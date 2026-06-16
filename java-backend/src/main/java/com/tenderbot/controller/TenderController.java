package com.tenderbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenderbot.dto.AddFoundModelRequest;
import com.tenderbot.dto.FoundModelDto;
import com.tenderbot.dto.TenderDto;
import com.tenderbot.dto.TenderItemDto;
import com.tenderbot.dto.UpdateItemDocumentDescriptionRequest;
import com.tenderbot.dto.UpdatePricingRequest;
import com.tenderbot.entity.*;
import com.tenderbot.repository.*;
import com.tenderbot.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenders")

public class TenderController {

    private final TenderRepository tenderRepository;
    private final TenderItemRepository tenderItemRepository;
    private final FoundModelRepository foundModelRepository;
    private final FoundModelEmailRepository foundModelEmailRepository;
    private final EmailLogRepository emailLogRepository;
    private final TenderOrchestrationService orchestrationService;
    private final LoggingService loggingService;
    private final SupplierSearchService supplierSearchService;
    private final ParameterExtractionService parameterExtractionService;
    private final NbrbExchangeRateService exchangeRateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TenderController(TenderRepository tenderRepository, TenderItemRepository tenderItemRepository,
                            FoundModelRepository foundModelRepository,
                            FoundModelEmailRepository foundModelEmailRepository,
                            EmailLogRepository emailLogRepository,
                            TenderOrchestrationService orchestrationService, LoggingService loggingService,
                            SupplierSearchService supplierSearchService,
                            ParameterExtractionService parameterExtractionService,
                            NbrbExchangeRateService exchangeRateService) {
        this.tenderRepository = tenderRepository;
        this.tenderItemRepository = tenderItemRepository;
        this.foundModelRepository = foundModelRepository;
        this.foundModelEmailRepository = foundModelEmailRepository;
        this.emailLogRepository = emailLogRepository;
        this.orchestrationService = orchestrationService;
        this.loggingService = loggingService;
        this.supplierSearchService = supplierSearchService;
        this.parameterExtractionService = parameterExtractionService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    public List<TenderDto> getAllTenders() {
        return tenderRepository.findAllOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenderDto> getTenderById(@PathVariable Long id) {
        return tenderRepository.findById(id).map(this::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    public List<TenderItemDto> getTenderItems(@PathVariable Long id) {
        return tenderItemRepository.findByTenderId(id).stream().map(this::toItemDto).toList();
    }

    @GetMapping("/{id}/logs")
    public List<com.tenderbot.dto.LogDto> getTenderLogs(@PathVariable Long id) {
        return loggingService.getLogsByTenderId(id);
    }

    @PostMapping("/process-url")
    public ResponseEntity<TenderDto> processUrl(@RequestBody String url) {
        return ResponseEntity.ok(toDto(orchestrationService.processNewUrl(url)));
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<Void> reprocessTender(@PathVariable Long id) {
        orchestrationService.reprocessTender(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteTender(@PathVariable Long id) {
        var tenderOpt = tenderRepository.findByIdWithItems(id);
        if (tenderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Tender tender = tenderOpt.get();

        List<Long> itemIds = tender.getItems().stream()
                .map(TenderItem::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<Long> foundModelIds = tender.getItems().stream()
                .flatMap(item -> item.getFoundModels().stream())
                .map(FoundModel::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (!foundModelIds.isEmpty()) {
            foundModelEmailRepository.deleteByFoundModelIdIn(foundModelIds);
        }
        if (!itemIds.isEmpty()) {
            emailLogRepository.deleteByTenderItemIdIn(itemIds);
        }

        tenderRepository.delete(tender);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{status}")
    public List<TenderDto> getTendersByStatus(@PathVariable String status) {
        try {
            return tenderRepository.findByStatus(TenderStatus.valueOf(status.toUpperCase())).stream().map(this::toDto).toList();
        } catch (IllegalArgumentException e) { return List.of(); }
    }

    @PutMapping("/{tenderId}/items/{itemId}/document-description")
    public ResponseEntity<TenderItemDto> updateItemDocumentDescription(
            @PathVariable Long tenderId,
            @PathVariable Long itemId,
            @RequestBody UpdateItemDocumentDescriptionRequest request) {
        return tenderItemRepository.findByIdWithFoundModels(itemId)
                .map(item -> {
                    item.setDocumentDescription(request.documentDescription());
                    TenderItem saved = tenderItemRepository.save(item);
                    return ResponseEntity.ok(toItemDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{tenderId}/items/{itemId}/search-suppliers")
    public ResponseEntity<TenderItemDto> searchSuppliersForItem(
            @PathVariable Long tenderId,
            @PathVariable Long itemId) {
        return tenderRepository.findById(tenderId)
                .flatMap(tender -> tenderItemRepository.findByIdWithFoundModels(itemId).map(item -> {
                    supplierSearchService.searchSuppliersForItem(tender, item);
                    return ResponseEntity.ok(toItemDto(item));
                }))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{tenderId}/items/{itemId}/search-by-description")
    public ResponseEntity<TenderItemDto> searchSuppliersByDescription(
            @PathVariable Long tenderId,
            @PathVariable Long itemId) {
        return tenderRepository.findById(tenderId)
                .flatMap(tender -> tenderItemRepository.findByIdWithFoundModels(itemId).map(item -> {
                    supplierSearchService.searchSuppliersForItemByDescription(tender, item);
                    return ResponseEntity.ok(toItemDto(item));
                }))
                .orElse(ResponseEntity.notFound().build());
    }

    private TenderDto toDto(Tender tender) {
        return new TenderDto(
                tender.getId(), tender.getTenderNumber(), tender.getTitle(), tender.getUrl(),
                tender.getOrganizer(), tender.getPublishDate(), tender.getDeadlineDate(),
                tender.getStatus(), tender.getTotalAmount(), tender.getCurrency(),
                tender.getCreatedAt(), tender.getUpdatedAt(), null
        );
    }

    @PostMapping("/{tenderId}/items/{itemId}/extract-params")
    public ResponseEntity<TenderItemDto> extractParameters(
            @PathVariable Long tenderId,
            @PathVariable Long itemId) {
        return tenderRepository.findById(tenderId)
                .flatMap(tender -> tenderItemRepository.findByIdWithFoundModels(itemId).map(item -> {
                    String text = item.getDocumentDescription();
                    if (text == null || text.isBlank()) {
                        text = item.getDescription();
                    }
                    String params = parameterExtractionService.extractParameters(text);
                    if (params != null) {
                        item.setExtractedParams(params);
                        tenderItemRepository.save(item);
                    }
                    return ResponseEntity.ok(toItemDto(item));
                }))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{tenderId}/items/{itemId}/extracted-params")
    public ResponseEntity<TenderItemDto> updateExtractedParams(
            @PathVariable Long tenderId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> params) {
        java.util.Optional<TenderItem> itemOpt = tenderItemRepository.findByIdWithFoundModels(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TenderItem item = itemOpt.get();
        try {
            String json = objectMapper.writeValueAsString(params);
            item.setExtractedParams(json);
            TenderItem saved = tenderItemRepository.save(item);
            return ResponseEntity.ok(toItemDto(saved));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{tenderId}/items/{itemId}/extracted-params/{key}")
    public ResponseEntity<TenderItemDto> deleteExtractedParam(
            @PathVariable Long tenderId,
            @PathVariable Long itemId,
            @PathVariable String key) {
        java.util.Optional<TenderItem> itemOpt = tenderItemRepository.findByIdWithFoundModels(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TenderItem item = itemOpt.get();
        try {
            Map<String, Object> current = objectMapper.readValue(
                    item.getExtractedParams() != null ? item.getExtractedParams() : "{}",
                    new TypeReference<>() {});
            current.remove(key);
            String json = objectMapper.writeValueAsString(current);
            item.setExtractedParams(json.isEmpty() || json.equals("{}") ? null : json);
            TenderItem saved = tenderItemRepository.save(item);
            return ResponseEntity.ok(toItemDto(saved));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{tenderId}/items/{itemId}/found-models/{modelId}")
    public ResponseEntity<TenderItemDto> deleteFoundModel(
            @PathVariable Long tenderId,
            @PathVariable Long itemId,
            @PathVariable Long modelId) {
        if (!tenderRepository.existsById(tenderId)) {
            return ResponseEntity.notFound().build();
        }
        java.util.Optional<TenderItem> itemOpt = tenderItemRepository.findByIdWithFoundModels(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TenderItem item = itemOpt.get();
        java.util.Optional<FoundModel> modelOpt = item.getFoundModels().stream()
                .filter(m -> m.getId().equals(modelId))
                .findFirst();
        if (modelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        FoundModel model = modelOpt.get();
        boolean wasSelected = model.getProductName() != null
                && model.getProductName().equals(item.getFoundModelName());
        item.getFoundModels().remove(model);
        foundModelRepository.delete(model);
        if (wasSelected || item.getFoundModels().isEmpty()) {
            item.setFoundModelName(null);
            item.setFoundModelUrl(null);
            item.setFoundModelPrice(null);
            item.setSupplierSite(null);
            item.setMatchScore(null);
            if (item.getFoundModels().isEmpty()) {
                item.setStatus(ItemStatus.NOT_FOUND);
            }
        }
        TenderItem saved = tenderItemRepository.save(item);
        return ResponseEntity.ok(toItemDto(saved));
    }

    @PostMapping("/{tenderId}/items/{itemId}/add-found-model")
    public ResponseEntity<TenderItemDto> addFoundModel(
            @PathVariable Long tenderId,
            @PathVariable Long itemId,
            @RequestBody AddFoundModelRequest request) {
        if (request.productUrl() == null || request.productUrl().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (!tenderRepository.existsById(tenderId)) {
            return ResponseEntity.notFound().build();
        }
        java.util.Optional<TenderItem> itemOpt = tenderItemRepository.findByIdWithFoundModels(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TenderItem item = itemOpt.get();

        String productName = request.productName();
        if (productName == null || productName.isBlank()) {
            try {
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(request.productUrl())
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();
                productName = doc.title();
                if (productName == null || productName.isBlank()) {
                    productName = request.productUrl();
                }
            } catch (Exception e) {
                productName = request.productUrl();
            }
        }

        String supplierSite;
        try {
            supplierSite = new java.net.URI(request.productUrl()).getHost();
        } catch (Exception e) {
            supplierSite = request.productUrl();
        }

        int rank = item.getFoundModels() != null ? item.getFoundModels().size() + 1 : 1;
        FoundModel model = new FoundModel(item, productName, request.productUrl(), request.price(), supplierSite, 1.0, 1.0, 1.0, rank);
        item.getFoundModels().add(model);
        foundModelRepository.save(model);

        // If no primary model set, make this one primary
        if (item.getFoundModelName() == null || item.getFoundModelName().isBlank()) {
            item.setFoundModelName(productName);
            item.setFoundModelUrl(request.productUrl());
            item.setFoundModelPrice(request.price());
            item.setSupplierSite(supplierSite);
            item.setMatchScore(1.0);
            item.setStatus(ItemStatus.FOUND_ON_SUPPLIER);
        }

        TenderItem saved = tenderItemRepository.save(item);
        return ResponseEntity.ok(toItemDto(saved));
    }

    @PostMapping("/{tenderId}/items/{itemId}/pricing")
    public ResponseEntity<TenderItemDto> updatePricing(
            @PathVariable Long tenderId,
            @PathVariable Long itemId,
            @RequestBody UpdatePricingRequest request) {
        if (!tenderRepository.existsById(tenderId)) {
            return ResponseEntity.notFound().build();
        }
        java.util.Optional<TenderItem> itemOpt = tenderItemRepository.findByIdWithFoundModels(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TenderItem item = itemOpt.get();
        item.setDeliveryCostByn(request.deliveryCostByn());
        item.setMarkupPercent(request.markupPercent());
        TenderItem saved = tenderItemRepository.save(item);
        return ResponseEntity.ok(toItemDto(saved));
    }

    @PostMapping("/{tenderId}/items/{itemId}/select-found-model/{modelId}")
    public ResponseEntity<TenderItemDto> selectFoundModel(
            @PathVariable Long tenderId,
            @PathVariable Long itemId,
            @PathVariable Long modelId) {
        if (!tenderRepository.existsById(tenderId)) {
            return ResponseEntity.notFound().build();
        }
        java.util.Optional<TenderItem> itemOpt = tenderItemRepository.findByIdWithFoundModels(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TenderItem item = itemOpt.get();
        java.util.Optional<FoundModel> modelOpt = item.getFoundModels().stream()
                .filter(m -> m.getId().equals(modelId))
                .findFirst();
        if (modelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        FoundModel model = modelOpt.get();
        item.setFoundModelName(model.getProductName());
        item.setFoundModelUrl(model.getProductUrl());
        item.setFoundModelPrice(model.getPrice());
        item.setSupplierSite(model.getSupplierSite());
        item.setMatchScore(model.getMatchScore());
        item.setStatus(ItemStatus.FOUND_ON_SUPPLIER);
        TenderItem saved = tenderItemRepository.save(item);
        return ResponseEntity.ok(toItemDto(saved));
    }

    private TenderItemDto toItemDto(TenderItem item) {
        var rate = exchangeRateService.getRubRate();
        double ratePerRub = rate.ratePerOneRub();

        List<FoundModelDto> foundModels = item.getFoundModels() != null
            ? item.getFoundModels().stream()
                .map(fm -> {
                    Double priceByn = fm.getPrice() != null ? exchangeRateService.convertRubToByn(fm.getPrice()) : null;
                    return new FoundModelDto(
                        fm.getId(), fm.getProductName(), fm.getProductUrl(), fm.getPrice(), priceByn, ratePerRub,
                        fm.getSupplierSite(), fm.getMatchScore(), fm.getSemanticScore(),
                        fm.getParametricScore(), fm.getRankPosition());
                })
                .toList()
            : List.of();

        Double primaryPriceByn = item.getFoundModelPrice() != null
            ? exchangeRateService.convertRubToByn(item.getFoundModelPrice())
            : null;
        Double finalPriceByn = item.getFoundModelPrice() != null
            ? exchangeRateService.calculateFinalByn(item.getFoundModelPrice(), item.getMarkupPercent(), item.getDeliveryCostByn())
            : null;

        return new TenderItemDto(
                item.getId(), item.getLotNumber(), item.getDescription(), item.getOriginalDescription(),
                item.getQuantity(), item.getUnit(), item.getEstimatedPrice(), item.getCurrency(), item.getOkpd2Code(),
                item.getFoundModelName(), item.getFoundModelUrl(), item.getFoundModelPrice(),
                primaryPriceByn, ratePerRub,
                item.getDeliveryCostByn(), item.getMarkupPercent(), finalPriceByn,
                item.getSupplierSite(), item.getStatus(), item.getDocumentDescription(), item.getDocumentFileUrl(),
                item.getDocumentFileName(), item.getExtractedParams(), item.getMatchScore(), foundModels
        );
    }
}
