package com.tenderbot.service;

import com.tenderbot.entity.*;
import com.tenderbot.repository.TenderItemRepository;
import com.tenderbot.repository.TenderRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TenderParserService {

    private final TenderRepository tenderRepository;
    private final TenderItemRepository tenderItemRepository;
    private final LoggingService loggingService;

    @Value("${parser.goszakupki.base-url}")
    private String baseUrl;

    @Value("${parser.goszakupki.timeout:30000}")
    private int timeout;

    @Value("${parser.user-agent}")
    private String userAgent;

    private static final Pattern TENDER_NUMBER_PATTERN = Pattern.compile("\\d{2}\\.\\d{3}/\\d+");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public TenderParserService(TenderRepository tenderRepository, TenderItemRepository tenderItemRepository, LoggingService loggingService) {
        this.tenderRepository = tenderRepository;
        this.tenderItemRepository = tenderItemRepository;
        this.loggingService = loggingService;
    }

    @Transactional
    public Tender parseTenderFromUrl(String url) {
        System.out.println("[INFO] Starting to parse tender from URL: " + url);

        Tender tender = new Tender();
        tender.setUrl(url);
        tender.setStatus(TenderStatus.PARSING);
        tender = tenderRepository.save(tender);

        try {
            loggingService.info(tender, "PARSER", "Начало парсинга тендера: " + url);

            Document doc = SslUtils.createConnection(url)
                    .timeout(timeout)
                    .userAgent(userAgent)
                    .followRedirects(true)
                    .get();

            String tenderNumber = extractTenderNumber(doc, url);
            tender.setTenderNumber(tenderNumber);

            String title = extractTitle(doc);
            tender.setTitle(title);
            loggingService.info(tender, "PARSER", "Название тендера: " + title);

            String organizer = extractOrganizer(doc);
            tender.setOrganizer(organizer);
            loggingService.info(tender, "PARSER", "Заказчик: " + organizer);

            tender.setPublishDate(extractPublishDate(doc));
            tender.setDeadlineDate(extractDeadlineDate(doc));
            tender.setTotalAmount(extractTotalAmount(doc));
            tender.setCurrency("BYN");

            List<TenderItem> items = parseItems(doc, tender);
            tender.setItems(items);
            loggingService.info(tender, "PARSER", "Найдено лотов: " + items.size());

            tender.setStatus(TenderStatus.PARSED);
            tender = tenderRepository.save(tender);

            loggingService.success(tender, "PARSER", "Тендер успешно распарсен");
            return tender;

        } catch (IOException e) {
            tender.setStatus(TenderStatus.ERROR);
            tenderRepository.save(tender);
            loggingService.error(tender, "PARSER", "Ошибка подключения к сайту: " + e.getMessage(), e);
            return tender;
        } catch (Exception e) {
            tender.setStatus(TenderStatus.ERROR);
            tenderRepository.save(tender);
            loggingService.error(tender, "PARSER", "Ошибка парсинга: " + e.getMessage(), e);
            return tender;
        }
    }

    private String extractTenderNumber(Document doc, String url) {
        Elements numberElements = doc.select("td:contains(Номер извещения) + td, .tender-number, span:matchesOwn(\\d{2}\\.\\d{3}/\\d+)");
        if (!numberElements.isEmpty()) {
            String text = numberElements.first().text().trim();
            Matcher matcher = TENDER_NUMBER_PATTERN.matcher(text);
            if (matcher.find()) return matcher.group();
        }
        Matcher matcher = TENDER_NUMBER_PATTERN.matcher(url);
        if (matcher.find()) return matcher.group();
        return "UNKNOWN_" + System.currentTimeMillis();
    }

    private String extractTitle(Document doc) {
        Elements titleElements = doc.select("h1, .tender-title, td:contains(Наименование закупки) + td, .procurement-subject");
        if (!titleElements.isEmpty()) return titleElements.first().text().trim();
        String metaTitle = doc.select("title").text();
        if (!metaTitle.isEmpty()) return metaTitle.replace(" - Госзакупки", "").trim();
        return "Без названия";
    }

    private String extractOrganizer(Document doc) {
        Elements orgElements = doc.select("td:contains(Организатор) + td, td:contains(Заказчик) + td, .organizer-name");
        if (!orgElements.isEmpty()) return orgElements.first().text().trim();
        return "Не указан";
    }

    private LocalDateTime extractPublishDate(Document doc) {
        try {
            Elements dateElements = doc.select("td:contains(Дата публикации) + td, td:contains(Размещено) + td");
            if (!dateElements.isEmpty()) return LocalDateTime.parse(dateElements.first().text().trim(), DATE_FORMATTER);
        } catch (Exception e) { /* ignore */ }
        return LocalDateTime.now();
    }

    private LocalDateTime extractDeadlineDate(Document doc) {
        try {
            Elements dateElements = doc.select("td:contains(Срок подачи) + td, td:contains(Прием заявок) + td");
            if (!dateElements.isEmpty()) return LocalDateTime.parse(dateElements.first().text().trim(), DATE_FORMATTER);
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private Double extractTotalAmount(Document doc) {
        try {
            Elements amountElements = doc.select("td:contains(Начальная цена) + td, td:contains(Общая сумма) + td, .total-price");
            if (!amountElements.isEmpty()) return Double.parseDouble(amountElements.first().text().replaceAll("[^\\d,.]", "").replace(",", "."));
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private List<TenderItem> parseItems(Document doc, Tender tender) {
        List<TenderItem> items = new ArrayList<>();
        try {
            // Находим таблицу «Информация о лотах» по характерным заголовкам
            Element lotTable = findLotTable(doc);

            if (lotTable != null) {
                // Сначала пробуем строки с классом lot-row
                Elements rows = lotTable.select("tr.lot-row");
                // Если не найдены — берём все tr, пропуская заголовок
                if (rows.isEmpty()) {
                    rows = lotTable.select("tr");
                }

                for (Element row : rows) {
                    Elements cells = row.select("td, th");
                    if (cells.size() < 3) continue;

                    // Пропускаем заголовочные строки (все ячейки — th или содержат "Предмет закупки")
                    if (isHeaderRow(cells)) continue;

                    // Описание — обычно вторая колонка (после № лота)
                    String description = extractLotDescription(cells);
                    if (description == null || description.isEmpty()) continue;

                    // Количество/цена — обычно третья колонка
                    QuantityPrice qp = extractQuantityPrice(cells);

                    TenderItem item = new TenderItem();
                    item.setTender(tender);
                    item.setLotNumber(extractLotNumber(cells));
                    item.setDescription(description);
                    item.setOriginalDescription(description);
                    item.setQuantity(qp.quantity);
                    item.setUnit(qp.unit);
                    item.setEstimatedPrice(qp.price);
                    item.setCurrency("BYN");
                    item.setOkpd2Code(extractOkpd2(doc, description));
                    item.setStatus(ItemStatus.PENDING);
                    items.add(item);
                }
            }

            // Fallback: если таблица лотов не найдена — пробуем общий блок
            if (items.isEmpty()) {
                for (Element block : doc.select("div:contains(Предмет закупки), div:contains(Описание закупки)")) {
                    String text = block.text();
                    if (text.length() > 20 && !text.contains("Срок поставки") && !text.contains("Место поставки")) {
                        TenderItem item = new TenderItem();
                        item.setTender(tender);
                        item.setLotNumber("1");
                        item.setDescription(text.substring(0, Math.min(text.length(), 1000)));
                        item.setStatus(ItemStatus.PENDING);
                        items.add(item);
                        break; // только первый подходящий блок
                    }
                }
            }
        } catch (Exception e) {
            loggingService.error(tender, "PARSER_ITEMS", "Ошибка парсинга лотов: " + e.getMessage(), e);
        }

        if (!items.isEmpty()) tenderItemRepository.saveAll(items);
        return items;
    }

    private boolean isHeaderRow(Elements cells) {
        for (Element cell : cells) {
            String text = cell.text().trim().toLowerCase();
            if (text.contains("предмет закупки") || text.contains("количество") || text.contains("№ лота")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ищет таблицу «Информация о лотах» по заголовкам колонок.
     */
    private Element findLotTable(Document doc) {
        for (Element table : doc.select("table")) {
            String headerText = table.text();
            if (headerText.contains("Предмет закупки") && headerText.contains("Количество")) {
                return table;
            }
        }
        // Альтернативные селекторы
        Elements tables = doc.select("table:has(th:contains(Предмет закупки))");
        if (!tables.isEmpty()) return tables.first();
        tables = doc.select("table:has(td:contains(Предмет закупки))");
        if (!tables.isEmpty()) return tables.first();
        tables = doc.select(".lots-table, .lot-table");
        if (!tables.isEmpty()) return tables.first();
        return null;
    }

    /**
     * Извлекает описание предмета закупки из ячеек строки.
     * Ищет ячейку с кириллическим текстом длиной > 15 символов, которая не является
     * чисто числовой, датой, статусом или деталью (Срок поставки и т.п.).
     */
    private String extractLotDescription(Elements cells) {
        for (int i = 0; i < cells.size(); i++) {
            // Пропускаем первую колонку (обычно № лота)
            if (i == 0) continue;

            String text = cells.get(i).text().trim();
            // Фильтруем короткие, числовые, даты, статусы
            if (text.length() < 10) continue;
            if (text.matches("\\d+([,.]\\d+)?")) continue;
            if (text.matches("\\d{2}\\.\\d{2}\\.\\d{4}.*")) continue;
            if (text.contains("Срок поставки")) continue;
            if (text.contains("Место поставки")) continue;
            if (text.contains("Источник финансирования")) continue;
            if (text.contains("Порядок оплаты")) continue;
            if (text.contains("Позиция №")) continue;
            if (text.contains("Код ОКРБ")) continue;

            // Должен содержать кириллицу — это описание товара
            if (text.matches(".*[а-яА-Я].*")) {
                return text;
            }
        }
        return null;
    }

    /**
     * Разбирает ячейку вида "1 штук(шт.), 471.31 BYN" на количество, единицу и цену.
     */
    private QuantityPrice extractQuantityPrice(Elements cells) {
        QuantityPrice qp = new QuantityPrice();
        for (int i = 0; i < cells.size(); i++) {
            // Обычно количество/цена идёт после описания — ищем по шаблону
            String text = cells.get(i).text().trim();
            // Пропускаем ячейки без чисел или с чисто текстовым описанием
            if (!text.matches(".*\\d+.*")) continue;
            if (text.matches(".*[а-яА-Я].*") && !text.contains("шт") && !text.contains("BYN")) {
                // возможно, это описание, а не количество
                continue;
            }

            // Количество: число перед единицей измерения
            Matcher qm = Pattern.compile("(\\d+(?:[,.]\\d+)?)\\s*(шт|штук|компл|упак|кг|л|м|м2|м3|т|пар|ед|шт\\.)", Pattern.CASE_INSENSITIVE).matcher(text);
            if (qm.find()) {
                try {
                    qp.quantity = Double.parseDouble(qm.group(1).replace(",", "."));
                } catch (NumberFormatException ignored) {}
                qp.unit = qm.group(2).toLowerCase().replace(".", "");
            }

            // Цена: число перед BYN
            Matcher pm = Pattern.compile("(\\d+(?:[\\s\\d]*(?:[,.]\\d+)?))\\s*BYN").matcher(text);
            if (pm.find()) {
                try {
                    String priceStr = pm.group(1).replace(" ", "").replace(",", ".");
                    qp.price = Double.parseDouble(priceStr);
                } catch (NumberFormatException ignored) {}
            }

            // Если нашли хотя бы количество — считаем, что эта ячейка подходит
            if (qp.quantity != null || qp.price != null) break;
        }

        if (qp.unit == null) qp.unit = "шт";
        return qp;
    }

    private String extractLotNumber(Elements cells) {
        for (Element cell : cells) {
            String text = cell.text().trim();
            if (text.matches("\\d+")) return text;
        }
        return "1";
    }

    /**
     * Ищет код ОКРБ/ОКПД2 в документе по описанию лота.
     * Сначала ищет в раскрытой детали лота рядом с описанием, затем в общих блоках.
     */
    private String extractOkpd2(Document doc, String description) {
        // Ищем в строках, содержащих "Код ОКРБ" или "Код предмета закупки по ОКРБ"
        for (Element el : doc.select("*:matchesOwn(Код ОКРБ:.*|Код предмета закупки по ОКРБ:)")) {
            String text = el.text();
            Matcher m = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3})").matcher(text);
            if (m.find()) return m.group(1);
        }
        // Ищем по шаблону во всём документе
        Matcher m = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3})").matcher(doc.text());
        if (m.find()) return m.group(1);
        return null;
    }

    private static class QuantityPrice {
        Double quantity;
        String unit = "шт";
        Double price;
    }

    @Transactional(readOnly = true)
    public boolean isTenderAlreadyProcessed(String url) {
        return tenderRepository.findAll().stream().anyMatch(t -> url.equals(t.getUrl()));
    }
}
