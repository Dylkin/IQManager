package com.tenderbot.service;

import com.tenderbot.entity.*;
import com.tenderbot.repository.TenderRepository;
import com.tenderbot.telegram.TenderTelegramBot;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenderOrchestrationService {

    private final TenderRepository tenderRepository;
    private final TenderParserService parserService;
    private final SupplierSearchService supplierSearchService;
    private final DocumentService documentService;
    private final EmailService emailService;
    private final LoggingService loggingService;
    private final ParameterExtractionService parameterExtractionService;
    private final TenderTelegramBot telegramBot;
    private final TenderOrchestrationService self;

    public TenderOrchestrationService(TenderRepository tenderRepository, TenderParserService parserService,
                                       SupplierSearchService supplierSearchService, DocumentService documentService,
                                       EmailService emailService, LoggingService loggingService,
                                       ParameterExtractionService parameterExtractionService,
                                       TenderTelegramBot telegramBot,
                                       @Lazy TenderOrchestrationService self) {
        this.tenderRepository = tenderRepository;
        this.parserService = parserService;
        this.supplierSearchService = supplierSearchService;
        this.documentService = documentService;
        this.emailService = emailService;
        this.loggingService = loggingService;
        this.parameterExtractionService = parameterExtractionService;
        this.telegramBot = telegramBot;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${scheduler.tender-process-interval:120000}")
    public void processPendingTenders() {
        for (Tender tender : tenderRepository.findByStatus(TenderStatus.NEW)) {
            try { self.processNewTender(tender); }
            catch (Exception e) { handleError(tender, e); }
        }
        for (Tender tender : tenderRepository.findByStatus(TenderStatus.PARSED)) {
            try { self.processParsedTender(tender); }
            catch (Exception e) { handleError(tender, e); }
        }
        for (Tender tender : tenderRepository.findByStatus(TenderStatus.SUPPLIERS_FOUND)) {
            try { self.processSupplierFoundTender(tender); }
            catch (Exception e) { handleError(tender, e); }
        }
        for (Tender tender : tenderRepository.findByStatus(TenderStatus.DOCUMENTS_ANALYZED)) {
            try { self.processDocumentAnalyzedTender(tender); }
            catch (Exception e) { handleError(tender, e); }
        }
    }

    @Transactional
    public void processNewTender(Tender tender) {
        Tender t = tenderRepository.findByIdWithItems(tender.getId()).orElseThrow(() -> new RuntimeException("Tender not found"));
        loggingService.info(t, "ORCHESTRATION", "Начало обработки нового тендера");
        loggingService.info(t, "ORCHESTRATION", "Этап 1/4: Загрузка документов и обогащение описаний лотов");
        documentService.enrichLotDescriptions(t);
        extractParametersForItems(t);
        loggingService.info(t, "ORCHESTRATION", "Этап 2/4: Поиск товаров на сайтах поставщиков");
        supplierSearchService.searchSuppliersForTender(t);
    }

    @Transactional
    public void processParsedTender(Tender tender) {
        Tender t = tenderRepository.findByIdWithItems(tender.getId()).orElseThrow(() -> new RuntimeException("Tender not found"));
        loggingService.info(t, "ORCHESTRATION", "Начало обработки распарсенного тендера");
        loggingService.info(t, "ORCHESTRATION", "Этап 1/4: Загрузка документов и обогащение описаний лотов");
        documentService.enrichLotDescriptions(t);
        extractParametersForItems(t);
        loggingService.info(t, "ORCHESTRATION", "Этап 2/4: Поиск товаров на сайтах поставщиков");
        supplierSearchService.searchSuppliersForTender(t);
    }

    private void extractParametersForItems(Tender tender) {
        for (TenderItem item : tender.getItems()) {
            if (item.getExtractedParams() != null && !item.getExtractedParams().isBlank() && !"{}".equals(item.getExtractedParams().trim())) continue;
            String text = item.getDocumentDescription();
            if (text == null || text.isBlank()) text = item.getDescription();
            if (text == null || text.isBlank()) continue;
            try {
                String params = parameterExtractionService.extractParameters(text);
                if (params != null) {
                    item.setExtractedParams(params);
                    loggingService.info(tender, "ORCHESTRATION", "Извлечены параметры для лота " + item.getLotNumber());
                }
            } catch (Exception e) {
                loggingService.warning(tender, "ORCHESTRATION", "Не удалось извлечь параметры для лота " + item.getLotNumber() + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public void processSupplierFoundTender(Tender tender) {
        Tender t = tenderRepository.findByIdWithItems(tender.getId()).orElseThrow(() -> new RuntimeException("Tender not found"));
        loggingService.info(t, "ORCHESTRATION", "Начало загрузки и анализа документов");
        documentService.downloadAndAnalyzeDocuments(t);
    }

    @Transactional
    public void processDocumentAnalyzedTender(Tender tender) {
        Tender t = tenderRepository.findByIdWithItems(tender.getId()).orElseThrow(() -> new RuntimeException("Tender not found"));
        loggingService.info(t, "ORCHESTRATION", "Начало отправки запросов поставщикам");
        emailService.sendBulkRequests(t);
    }

    @Transactional
    public void reprocessTender(Long tenderId) {
        Tender tender = tenderRepository.findById(tenderId).orElseThrow(() -> new RuntimeException("Tender not found"));
        tender.setStatus(TenderStatus.PARSED);
        tenderRepository.save(tender);
        for (TenderItem item : tender.getItems()) {
            item.setStatus(ItemStatus.PENDING);
            item.setFoundModelName(null);
            item.setFoundModelUrl(null);
            item.setFoundModelPrice(null);
            item.setSupplierSite(null);
            item.setDocumentDescription(null);
            item.setExtractedParams(null);
        }
        loggingService.info(tender, "ORCHESTRATION", "Тендер поставлен в очередь на повторную обработку");
    }

    @Transactional
    public Tender processNewUrl(String url) {
        if (parserService.isTenderAlreadyProcessed(url)) throw new RuntimeException("Tender from this URL already processed");
        return parserService.parseTenderFromUrl(url);
    }

    private void handleError(Tender tender, Exception e) {
        tender.setStatus(TenderStatus.ERROR);
        tenderRepository.save(tender);
        loggingService.error(tender, "ORCHESTRATION", "Критическая ошибка: " + e.getMessage(), e);
        try {
            telegramBot.sendErrorNotification("Ошибка обработки тендера " + tender.getTenderNumber() + ": " + e.getMessage());
        } catch (Exception ex) { System.err.println("Failed to send notification: " + ex.getMessage()); }
    }
}
