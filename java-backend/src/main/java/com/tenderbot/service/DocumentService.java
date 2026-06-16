package com.tenderbot.service;

import com.tenderbot.entity.*;
import com.tenderbot.repository.TenderItemRepository;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final String LOG_CATEGORY_DOCUMENTS = "DOCUMENTS";
    private static final String LOG_CATEGORY_DOCUMENT_ANALYSIS = "DOCUMENT_ANALYSIS";
    private static final String EXT_DOCX = ".docx";
    private static final String EXT_XLSX = ".xlsx";

    private static final String MSG_SYMBOLS = " символов)";
    private static final String MSG_FOR_LOT = "Для лота ";

    private final TenderItemRepository tenderItemRepository;
    private final LoggingService loggingService;

    @Value("${document.storage.path:/tmp/tenderbot/documents}")
    private String documentStoragePath;

    @Value("${parser.goszakupki.timeout:30000}")
    private int timeout;

    @Value("${parser.user-agent}")
    private String userAgent;

    @Value("${ocr.python.path:python}")
    private String pythonPath;

    @Value("${ocr.script.path:scripts/ocr_pdf.py}")
    private String ocrScriptPath;

    private String resolvedOcrScriptPath;

    private static final Pattern MODEL_PATTERN = Pattern.compile("(модель|артикул|код|catalog|model|art\\.?|type)[\\s:]+([^\\n\\r]{3,50})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SPEC_PATTERN = Pattern.compile("(техническое задание|ТЗ|спецификация|описание|параметры|характеристики)[\\s:]*([^\\n\\r]{10,500})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public DocumentService(TenderItemRepository tenderItemRepository, LoggingService loggingService) {
        this.tenderItemRepository = tenderItemRepository;
        this.loggingService = loggingService;
    }

    @PostConstruct
    public void init() {
        try { Files.createDirectories(Paths.get(documentStoragePath)); }
        catch (IOException e) { log.error("Failed to create documents directory: {}", e.getMessage()); }
        resolveOcrScriptPath();
    }

    private void resolveOcrScriptPath() {
        Path configured = Paths.get(ocrScriptPath);
        if (Files.exists(configured)) {
            resolvedOcrScriptPath = configured.toAbsolutePath().toString();
            log.info("Using configured OCR script: {}", resolvedOcrScriptPath);
            return;
        }
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("ocr_pdf.py");
            if (is == null) {
                log.error("OCR script not found in classpath (ocr_pdf.py) and not on filesystem ({})", ocrScriptPath);
                return;
            }
            Path extracted = Paths.get(documentStoragePath, "ocr_pdf.py");
            Files.copy(is, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();
            resolvedOcrScriptPath = extracted.toAbsolutePath().toString();
            log.info("Extracted OCR script from classpath to: {}", resolvedOcrScriptPath);
        } catch (IOException e) {
            log.error("Failed to extract OCR script: {}", e.getMessage());
        }
    }

    @Transactional
    public void enrichLotDescriptions(Tender tender) {
        loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Начало скачивания документов для обогащения описаний лотов");
        try {
            Document doc = SslUtils.createConnection(tender.getUrl()).timeout(timeout).userAgent(userAgent).get();
            List<DocumentInfo> documents = findDocuments(doc, tender.getUrl());
            loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Найдено документов: " + documents.size());

            if (documents.isEmpty()) {
                loggingService.warning(tender, LOG_CATEGORY_DOCUMENTS, "Документы не найдены на странице тендера");
                return;
            }

            StringBuilder docNames = new StringBuilder();
            for (DocumentInfo d : documents) {
                if (!docNames.isEmpty()) docNames.append(", ");
                docNames.append(d.name);
            }
            loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Список документов: " + docNames);

            List<DocumentContent> documentContents = downloadDocuments(tender, documents);

            if (documentContents.isEmpty()) {
                loggingService.warning(tender, LOG_CATEGORY_DOCUMENTS, "Не удалось извлечь текст из документов");
                return;
            }

            int updatedCount = updateLotDescriptions(tender, documentContents);

            if (updatedCount > 0) {
                loggingService.success(tender, LOG_CATEGORY_DOCUMENTS, "Обновлено описаний лотов: " + updatedCount + " из " + tender.getItems().size());
            } else {
                loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Расширенные описания не найдены в документах");
            }
        } catch (IOException e) {
            loggingService.error(tender, LOG_CATEGORY_DOCUMENTS, "Ошибка доступа к странице тендера", e);
        }
    }

    private record DocumentContent(String fileName, String fileUrl, String localFilePath, String content) {}
    private record LotDescriptionResult(String text, String fileName, String fileUrl, String localFileName) {}

    private List<DocumentContent> downloadDocuments(Tender tender, List<DocumentInfo> documents) {
        List<DocumentContent> documentContents = new ArrayList<>();
        int downloadedCount = 0;
        for (DocumentInfo docInfo : documents) {
            DocumentContent content = downloadSingleDocument(tender, docInfo);
            if (content != null) {
                downloadedCount++;
                documentContents.add(content);
            }
        }
        loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Успешно обработано документов: " + downloadedCount + " из " + documents.size());
        return documentContents;
    }

    private DocumentContent downloadSingleDocument(Tender tender, DocumentInfo docInfo) {
        try {
            String downloadUrl = prepareDownloadUrl(docInfo.url);
            String filePath = downloadDocument(downloadUrl, docInfo.name, tender.getTenderNumber());
            if (filePath != null) {
                String content = extractTextFromDocument(filePath);
                loggingService.info(tender, LOG_CATEGORY_DOCUMENTS,
                        "Скачан и обработан документ: " + docInfo.name + " (извлечено " + content.length() + MSG_SYMBOLS);
                return new DocumentContent(docInfo.name, docInfo.url, filePath, content);
            } else {
                loggingService.warning(tender, LOG_CATEGORY_DOCUMENTS, "Не удалось скачать документ: " + docInfo.name);
            }
        } catch (Exception e) {
            loggingService.error(tender, LOG_CATEGORY_DOCUMENTS, "Ошибка обработки документа " + docInfo.name, e);
        }
        return null;
    }

    private int updateLotDescriptions(Tender tender, List<DocumentContent> documentContents) {
        int updatedCount = 0;
        for (TenderItem item : tender.getItems()) {
            String currentDesc = item.getDescription();
            if (currentDesc == null || currentDesc.isEmpty()) {
                loggingService.info(tender, LOG_CATEGORY_DOCUMENT_ANALYSIS, "Лот " + item.getLotNumber() + " пропущен (пустое описание)");
                continue;
            }

            loggingService.info(tender, LOG_CATEGORY_DOCUMENT_ANALYSIS,
                    "Поиск расширенного описания для лота " + item.getLotNumber() + " (предмет: " + currentDesc.substring(0, Math.min(currentDesc.length(), 60)) + ")...");

            LotDescriptionResult result = findLotDescriptionInDocuments(documentContents, currentDesc);
            if (result != null && result.text() != null && !result.text().isBlank()) {
                item.setDocumentDescription(result.text());
                item.setDocumentFileUrl(result.fileUrl());
                item.setDocumentFileName(result.localFileName());
                tenderItemRepository.save(item);
                String preview = result.text().length() > 200 ? result.text().substring(0, 200) + "..." : result.text();
                loggingService.success(tender, LOG_CATEGORY_DOCUMENT_ANALYSIS,
                        MSG_FOR_LOT + item.getLotNumber() + " найдено описание из документа '" + result.fileName() + "' (" + result.text().length() + MSG_SYMBOLS + ". Фрагмент: " + preview);
                updatedCount++;
            } else {
                loggingService.info(tender, LOG_CATEGORY_DOCUMENT_ANALYSIS,
                        MSG_FOR_LOT + item.getLotNumber() + " расширенное описание не найдено");
            }
        }
        return updatedCount;
    }

    @Transactional
    public void downloadAndAnalyzeDocuments(Tender tender) {
        loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Начало скачивания документов тендера");
        tender.setStatus(TenderStatus.DOWNLOADING_DOCUMENTS);

        try {
            Document doc = SslUtils.createConnection(tender.getUrl()).timeout(timeout).userAgent(userAgent).get();
            List<DocumentInfo> documents = findDocuments(doc, tender.getUrl());
            loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Найдено документов: " + documents.size());

            if (documents.isEmpty()) {
                loggingService.warning(tender, LOG_CATEGORY_DOCUMENTS, "Документы не найдены на странице тендера");
                return;
            }

            List<DocumentContent> documentContents = downloadAndAnalyzeSingleDocuments(tender, documents);
            if (!documentContents.isEmpty()) {
                updateItemsWithDescriptions(tender, documentContents);
            }

            tender.setStatus(TenderStatus.DOCUMENTS_ANALYZED);
            loggingService.success(tender, LOG_CATEGORY_DOCUMENTS, "Документы успешно проанализированы");
        } catch (IOException e) {
            loggingService.error(tender, LOG_CATEGORY_DOCUMENTS, "Ошибка доступа к странице тендера", e);
        }
    }

    private List<DocumentContent> downloadAndAnalyzeSingleDocuments(Tender tender, List<DocumentInfo> documents) {
        List<DocumentContent> documentContents = new ArrayList<>();
        int downloadedCount = 0;
        for (DocumentInfo docInfo : documents) {
            DocumentContent content = downloadAndAnalyzeSingleDocument(tender, docInfo);
            if (content != null) {
                downloadedCount++;
                documentContents.add(content);
            }
        }
        loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Успешно обработано документов: " + downloadedCount + " из " + documents.size());
        return documentContents;
    }

    private DocumentContent downloadAndAnalyzeSingleDocument(Tender tender, DocumentInfo docInfo) {
        try {
            String downloadUrl = prepareDownloadUrl(docInfo.url);
            String filePath = downloadDocument(downloadUrl, docInfo.name, tender.getTenderNumber());
            if (filePath != null) {
                String content = extractTextFromDocument(filePath);
                analyzeModelsAndSpecs(tender, content);
                loggingService.info(tender, LOG_CATEGORY_DOCUMENTS, "Документ проанализирован: " + docInfo.name + " (" + content.length() + MSG_SYMBOLS);
                return new DocumentContent(docInfo.name, docInfo.url, filePath, content);
            }
        } catch (Exception e) {
            loggingService.error(tender, LOG_CATEGORY_DOCUMENTS, "Ошибка обработки документа " + docInfo.name, e);
        }
        return null;
    }

    private void updateItemsWithDescriptions(Tender tender, List<DocumentContent> documentContents) {
        for (TenderItem item : tender.getItems()) {
            if (item.getStatus() == ItemStatus.FOUND_ON_SUPPLIER) {
                LotDescriptionResult result = findLotDescriptionInDocuments(documentContents, item.getDescription());
                if (result != null && result.text() != null && !result.text().isBlank()) {
                    item.setDocumentDescription(result.text());
                    item.setDocumentFileUrl(result.fileUrl());
                    item.setDocumentFileName(result.localFileName());
                    item.setStatus(ItemStatus.MODEL_MATCHED);
                    tenderItemRepository.save(item);
                    loggingService.success(tender, LOG_CATEGORY_DOCUMENT_ANALYSIS,
                            MSG_FOR_LOT + item.getLotNumber() + " найдено подробное описание из документа '" + result.fileName() + "' (" + result.text().length() + MSG_SYMBOLS);
                }
            }
        }
    }

    private List<DocumentInfo> findDocuments(Document doc, String baseUrl) {
        List<DocumentInfo> documents = new ArrayList<>();
        java.util.Set<String> processedUrls = new java.util.HashSet<>();

        // Находим все ссылки на реальные файлы и проверяем, находятся ли они в блоке "Документы"
        for (Element link : doc.select("a[href]")) {
            String href = link.attr("href");
            String text = link.text().trim();
            // Принимаем ссылку только если текст ссылки оканчивается на имя файла с известным расширением.
            // Это отфильтровывает общие документы портала ("Регламент", "Политика" и т.п.)
            if (!isDocumentFileByName(text)) continue;

            // Поднимаемся от ссылки вверх до 10 уровней, ищем предка с текстом "Документы" или "Приложения"
            Element parent = link.parent();
            int levels = 0;
            boolean inDocumentsSection = false;
            while (parent != null && levels < 10) {
                String parentText = parent.text().toLowerCase();
                if (parentText.contains("документы") || parentText.contains("приложения")) {
                    inDocumentsSection = true;
                    break;
                }
                parent = parent.parent();
                levels++;
            }

            if (inDocumentsSection) {
                String resolved = resolveUrl(href, baseUrl);
                if (processedUrls.add(resolved)) {
                    documents.add(new DocumentInfo(link.text().trim(), resolved));
                }
            }
        }

        return documents;
    }

    private boolean isDocumentFileByName(String text) {
        String lt = text.toLowerCase();
        for (String ext : new String[]{".pdf", ".doc", EXT_DOCX, ".xls", EXT_XLSX, ".rtf", ".zip", ".rar"})
            if (lt.endsWith(ext)) return true;
        return false;
    }

    private String resolveUrl(String href, String baseUrl) {
        if (href.startsWith("http")) return href;
        if (href.startsWith("//")) return "https:" + href;
        if (href.startsWith("/")) return baseUrl.replaceAll("(https?://[^/]+).*", "$1") + href;
        return baseUrl + "/" + href;
    }

    private String prepareDownloadUrl(String url) {
        // Для goszakupki.by ссылки типа /get-file/... возвращают JSON с метаданными,
        // а сам файл доступен по тому же URL с добавлением &download=1
        if (url.contains("goszakupki.by") && url.contains("/get-file/") && !url.contains("download=1")) {
            return url + (url.contains("?") ? "&download=1" : "?download=1");
        }
        return url;
    }

    private String downloadDocument(String url, String docName, String tenderNumber) {
        try (CloseableHttpClient client = SslUtils.createHttpClient()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", userAgent);
            return client.execute(request, response -> {
                if (response.getCode() == 200) {
                    byte[] content = EntityUtils.toByteArray(response.getEntity());
                    String ext = getFileExtension(url, docName);
                    String fileName = tenderNumber.replaceAll("[^\\w]", "_") + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
                    Path filePath = Paths.get(documentStoragePath, fileName);
                    Files.write(filePath, content);
                    return filePath.toString();
                }
                return null;
            });
        } catch (Exception e) { log.error("Failed to download: {}", e.getMessage()); }
        return null;
    }

    private String getFileExtension(String url, String name) {
        String combined = (url + " " + name).toLowerCase();
        for (String ext : new String[]{".pdf", EXT_DOCX, ".doc", EXT_XLSX, ".xls", ".rtf"})
            if (combined.contains(ext)) return ext;
        return ".bin";
    }

    private String extractTextFromDocument(String filePath) {
        try {
            String lp = filePath.toLowerCase();
            if (lp.endsWith(".pdf")) return extractPdfText(filePath);
            if (lp.endsWith(EXT_DOCX)) return extractDocxText(filePath);
            if (lp.endsWith(".doc")) return extractDocText(filePath);
            if (lp.endsWith(EXT_XLSX) || lp.endsWith(".xls")) return extractExcelText(filePath);
            if (lp.endsWith(".txt") || lp.endsWith(".rtf")) return Files.readString(Paths.get(filePath));
        } catch (Exception e) { log.error("Error extracting text: {}", e.getMessage()); }
        return "";
    }

    private String extractPdfText(String filePath) throws IOException {
        String text;
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            text = new PDFTextStripper().getText(document);
        }
        int textLen = text == null ? 0 : text.trim().length();
        if (textLen < 50) {
            log.info("PDF has little or no extractable text ({} chars), trying OCR for: {}", textLen, filePath);
            String ocrText = runOcrOnPdf(filePath);
            if (ocrText != null) {
                int ocrLen = ocrText.trim().length();
                if (ocrLen > textLen) {
                    log.info("OCR extracted {} chars from {}", ocrLen, filePath);
                    return ocrText;
                } else {
                    log.info("OCR returned {} chars, not better than direct extraction ({} chars)", ocrLen, textLen);
                }
            } else {
                log.warn("OCR produced no output for {}", filePath);
            }
        }
        return text;
    }

    private String runOcrOnPdf(String filePath) {
        try {
            if (resolvedOcrScriptPath == null) {
                log.error("OCR script not available, cannot process {}", filePath);
                return null;
            }
            ProcessBuilder pb = new ProcessBuilder(pythonPath, resolvedOcrScriptPath, filePath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            boolean finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("OCR process timed out for {}", filePath);
                return null;
            }
            if (process.exitValue() != 0) {
                log.error("OCR process exited with code {} for {}", process.exitValue(), filePath);
                return null;
            }
            return output.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("OCR interrupted for {}", filePath);
            return null;
        } catch (Exception e) {
            log.error("Failed to run OCR on {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    private String extractDocxText(String filePath) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath))) {
            StringBuilder sb = new StringBuilder();
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        sb.append(text).append("\n");
                    }
                } else if (element instanceof XWPFTable table) {
                    appendTableText(sb, table);
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
    }

    private void appendTableText(StringBuilder sb, XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows == null || rows.isEmpty()) return;

        int twoColumnRows = 0;
        int totalRows = 0;
        for (XWPFTableRow row : rows) {
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() >= 2) {
                totalRows++;
                if (!cells.get(0).getText().isBlank() && !cells.get(1).getText().isBlank()) {
                    twoColumnRows++;
                }
            }
        }
        boolean keyValue = totalRows > 0 && (double) twoColumnRows / totalRows >= 0.5;

        for (XWPFTableRow row : rows) {
            List<XWPFTableCell> cells = row.getTableCells();
            List<String> parts = new ArrayList<>();
            for (XWPFTableCell cell : cells) {
                parts.add(cell.getText().trim());
            }
            if (parts.stream().allMatch(String::isEmpty)) continue;

            if (keyValue && parts.size() >= 2) {
                sb.append(parts.get(0)).append(": ").append(parts.get(1)).append("\n");
            } else {
                sb.append(String.join(" | ", parts)).append("\n");
            }
        }
    }

    private String extractDocText(String filePath) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(new FileInputStream(filePath));
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String extractExcelText(String filePath) throws IOException {
        StringBuilder text = new StringBuilder();
        try (Workbook wb = WorkbookFactory.create(new File(filePath))) {
            for (Sheet sheet : wb) {
                for (Row row : sheet) {
                    for (Cell cell : row) text.append(getCellValue(cell)).append(" ");
                    text.append("\n");
                }
            }
        }
        return text.toString();
    }

    private String getCellValue(Cell cell) {
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> "";
            };
        } catch (Exception _) { return ""; }
    }

    private void analyzeModelsAndSpecs(Tender tender, String content) {
        if (content == null || content.isEmpty()) return;
        Matcher mm = MODEL_PATTERN.matcher(content);
        while (mm.find()) loggingService.info(tender, LOG_CATEGORY_DOCUMENT_ANALYSIS, "Найдена информация о модели: " + mm.group(2).trim());
        Matcher sm = SPEC_PATTERN.matcher(content);
        while (sm.find()) {
            String spec = sm.group(2).trim();
            if (spec.length() > 20) loggingService.info(tender, LOG_CATEGORY_DOCUMENT_ANALYSIS, "Найдено описание: " + spec.substring(0, Math.min(spec.length(), 200)));
        }
    }

    /**
     * Ищет в документах описание конкретного лота.
     * Логика: ищем фразу "Описание предмета закупки" (или вариации) рядом с названием предмета закупки.
     * Если найдено — возвращаем текст от этой фразы до конца секции/параграфа.
     */
    private LotDescriptionResult findLotDescriptionInDocuments(List<DocumentContent> docs, String itemDescription) {
        if (itemDescription == null || itemDescription.isEmpty()) return null;

        String normalizedDesc = normalizeDescription(itemDescription);
        List<String> keyWords = extractKeyWords(normalizedDesc);

        // Технические задания и спецификации имеют приоритет
        List<DocumentContent> ordered = new ArrayList<>(docs);
        ordered.sort((a, b) -> Boolean.compare(isTechnicalSpecDocument(b), isTechnicalSpecDocument(a)));

        LotDescriptionResult bestResult = null;
        int bestScore = -1;

        for (DocumentContent doc : ordered) {
            if (doc.content == null || doc.content.isEmpty()) continue;

            int docScore = scoreDocumentForLot(doc);

            SectionMatch match = findBestSectionMatch(doc, keyWords);
            if (match != null) {
                int totalScore = docScore + match.matches() * 10;
                if (totalScore > bestScore) {
                    bestScore = totalScore;
                    bestResult = createResult(doc, match);
                }
            } else {
                String fallback = findDescriptionByKeywordMatch(doc.content, keyWords);
                if (fallback != null) {
                    int totalScore = docScore + countKeywordMatches(fallback.toLowerCase(), keyWords) * 5;
                    if (totalScore > bestScore) {
                        bestScore = totalScore;
                        bestResult = createFallbackResult(doc, fallback);
                    }
                }
            }
        }

        // Если секции/абзацы не подошли, но документ релевантен (содержит ключевые слова),
        // берём первые 3000 символов текста — это помогает для OCR-текстов без разбиения на строки.
        if (bestResult == null) {
            for (DocumentContent doc : ordered) {
                if (doc.content == null || doc.content.isEmpty()) continue;
                if (hasAnyKeywordMatch(doc.content, keyWords)) {
                    String text = doc.content.length() > 3000 ? doc.content.substring(0, 3000) : doc.content;
                    return createFallbackResult(doc, text);
                }
            }
        }

        return bestResult;
    }

    private boolean isTechnicalSpecDocument(DocumentContent doc) {
        if (doc.fileName == null) return false;
        String lower = doc.fileName.toLowerCase();
        return lower.contains("техническое")
            || lower.contains("спецификация")
            || lower.contains("тз")
            || lower.contains("tz")
            || lower.contains("specification")
            || lower.contains("характеристики");
    }

    private int scoreDocumentForLot(DocumentContent doc) {
        int score = 0;
        if (isTechnicalSpecDocument(doc)) score += 100;
        String lower = doc.content.toLowerCase();
        if (lower.contains("техническое задание")) score += 30;
        if (lower.contains("технические характеристики")) score += 20;
        if (lower.contains("спецификация")) score += 15;
        return score;
    }

    private String normalizeDescription(String itemDescription) {
        return itemDescription.toLowerCase()
                .replaceAll("[^а-яa-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> extractKeyWords(String normalizedDesc) {
        String[] descWords = normalizedDesc.split("\\s+");
        List<String> keyWords = new ArrayList<>();
        for (String w : descWords) {
            if (w.length() > 3) keyWords.add(w);
        }
        return keyWords;
    }

    private SectionMatch findBestSectionMatch(DocumentContent doc, List<String> keyWords) {
        String content = doc.content;
        String lowerContent = content.toLowerCase();
        List<Integer> sectionPositions = findSectionPositions(lowerContent);

        SectionMatch bestMatch = null;
        int bestMatches = 0;

        for (int pos : sectionPositions) {
            int endPos = Math.min(pos + 3000, content.length());
            String sectionText = content.substring(pos, endPos);
            String lowerSection = sectionText.toLowerCase();

            int matches = countKeywordMatches(lowerSection, keyWords);

            if (matches >= 2 && matches > bestMatches) {
                bestMatches = matches;
                String extracted = extractCleanSectionText(sectionText);
                bestMatch = new SectionMatch(extracted, matches);
            }
        }
        return bestMatch;
    }

    private int countKeywordMatches(String text, List<String> keyWords) {
        if (text == null || text.isEmpty() || keyWords == null || keyWords.isEmpty()) return 0;
        Set<String> textStems = extractStems(text);
        int matches = 0;
        for (String kw : keyWords) {
            if (textStems.contains(stemRussian(kw))) matches++;
        }
        return matches;
    }

    private boolean hasAnyKeywordMatch(String text, List<String> keyWords) {
        return countKeywordMatches(text, keyWords) > 0;
    }

    private Set<String> extractStems(String text) {
        Set<String> stems = new HashSet<>();
        String[] words = text.toLowerCase().split("[^а-яa-z0-9]+");
        for (String w : words) {
            if (w.length() > 3) stems.add(stemRussian(w));
        }
        return stems;
    }

    /**
     * Простая эвристическая нормализация русских слов: удаляет типичные окончания.
     * Позволяет сопоставлять ключевые слова описания лота с разными словоформами в документах
     * (например, "центрифуга" ↔ "центрифуги", "лабораторная" ↔ "лабораторной").
     */
    private String stemRussian(String word) {
        if (word == null || word.length() <= 4) return word;
        String[] endings = {
            "ого", "его", "ому", "ему", "ыми", "ими", "ая", "яя", "ое", "ее", "ие", "ые",
            "ий", "ый", "ой", "ом", "ем", "ах", "ях", "ами", "ями", "ам", "ям", "ов", "ев",
            "ей", "ой", "ую", "юю", "ы", "и", "а", "я", "о", "е", "у", "ю"
        };
        for (String e : endings) {
            if (word.endsWith(e) && word.length() - e.length() >= 4) {
                return word.substring(0, word.length() - e.length());
            }
        }
        return word;
    }

    private LotDescriptionResult createResult(DocumentContent doc, SectionMatch match) {
        String localFileName = doc.localFilePath != null ? Paths.get(doc.localFilePath).getFileName().toString() : null;
        return new LotDescriptionResult(match.text(), doc.fileName, doc.fileUrl, localFileName);
    }

    private LotDescriptionResult createFallbackResult(DocumentContent doc, String fallback) {
        String localFileName = doc.localFilePath != null ? Paths.get(doc.localFilePath).getFileName().toString() : null;
        return new LotDescriptionResult(fallback, doc.fileName, doc.fileUrl, localFileName);
    }

    private record SectionMatch(String text, int matches) {}

    /**
     * Находит позиции секций "Описание предмета закупки" в тексте.
     */
    private List<Integer> findSectionPositions(String lowerContent) {
        List<Integer> positions = new ArrayList<>();
        String[] markers = {
            "описание предмета закупки",
            "описание объекта закупки",
            "предмет закупки",
            "описание лота",
            "наименование предмета закупки",
            "техническое задание",
            "техническая спецификация",
            "спецификация",
            "характеристики",
            "описание товаров",
            "сведения о предмете",
            "описание и количество",
            "технические требования",
            "ii. описание предмета"
        };
        for (String marker : markers) {
            int idx = 0;
            while ((idx = lowerContent.indexOf(marker, idx)) != -1) {
                positions.add(idx);
                idx += marker.length();
            }
        }
        return positions;
    }

    /**
     * Извлекает чистый текст секции, удаляя лишние переносы строк.
     */
    private String extractCleanSectionText(String sectionText) {
        // Находим конец секции — следующий крупный заголовок (римская цифра, "Раздел", "Договор" и т.п.)
        String[] lines = sectionText.split("\\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String lower = trimmed.toLowerCase();
            if (result.length() > 200 && isNextSectionHeading(lower)) {
                break;
            }

            if (!result.isEmpty()) result.append("\n");
            result.append(trimmed);
        }
        return result.toString().trim();
    }

    private boolean isNextSectionHeading(String lower) {
        return lower.matches("^[ivx]+\\.\\s+.*") // I. II. III.
            || lower.startsWith("раздел ")
            || lower.startsWith("часть (лот) №")
            || lower.matches("^(договор|контракт|приложение|протокол)\\b.*");
    }

    /**
     * Fallback поиск: ищет параграф с наибольшим количеством совпадающих ключевых слов.
     */
    private String findDescriptionByKeywordMatch(String content, List<String> keyWords) {
        String[] paragraphs = content.split("\\n+");
        String best = null;
        int bestMatches = 0;
        for (String para : paragraphs) {
            if (para.length() < 50 || para.length() > 3000) continue;
            String lp = para.toLowerCase();
            int matches = 0;
            for (String kw : keyWords) {
                if (lp.contains(kw)) matches++;
            }
            if (matches >= 3 && matches > bestMatches) {
                bestMatches = matches;
                best = para;
            }
        }
        return best;
    }

    private record DocumentInfo(String name, String url) {}
}
