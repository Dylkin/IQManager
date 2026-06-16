package com.tenderbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenderbot.entity.*;
import com.tenderbot.repository.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SupplierSearchService {

    private static final Logger log = LoggerFactory.getLogger(SupplierSearchService.class);

    private final SupplierRepository supplierRepository;
    private final TenderItemRepository tenderItemRepository;
    private final FoundModelRepository foundModelRepository;
    private final EquipmentCatalogItemRepository catalogItemRepository;
    private final EquipmentTypeRepository typeRepository;
    private final LoggingService loggingService;
    private final EquipmentCatalogService catalogService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${parser.supplier.timeout:30000}")
    private int timeout;

    @Value("${parser.user-agent}")
    private String userAgent;

    // Minimum normalized score to accept a candidate
    private static final double SCORE_THRESHOLD = 0.35;
    // Minimum score for an "exact" match
    private static final double EXACT_MATCH_THRESHOLD = 0.80;

    // Characteristic search priority for fallback (higher weight = more important)
    private static final List<String> FALLBACK_PRIORITY = List.of(
        "model", "maxTempC", "volumeL", "powerKW", "voltageV",
        "weightKg", "material", "dimensionsMm", "programsCount", "regulatorType"
    );

    public SupplierSearchService(SupplierRepository supplierRepository, TenderItemRepository tenderItemRepository,
                                 FoundModelRepository foundModelRepository,
                                 EquipmentCatalogItemRepository catalogItemRepository,
                                 EquipmentTypeRepository typeRepository,
                                 LoggingService loggingService,
                                 EquipmentCatalogService catalogService) {
        this.supplierRepository = supplierRepository;
        this.tenderItemRepository = tenderItemRepository;
        this.foundModelRepository = foundModelRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.typeRepository = typeRepository;
        this.loggingService = loggingService;
        this.catalogService = catalogService;
    }

    @Transactional
    public void searchSuppliersForItem(Tender tender, TenderItem item) {
        List<Supplier> activeSuppliers = getActiveSuppliers();
        if (activeSuppliers.isEmpty()) {
            loggingService.warning(tender, "SUPPLIER_SEARCH", "Нет активных поставщиков в базе");
            return;
        }
        item.setStatus(ItemStatus.PENDING);
        searchItemOnSuppliers(tender, item, activeSuppliers);
    }

    @Transactional
    public void searchSuppliersForItemByDescription(Tender tender, TenderItem item) {
        List<Supplier> activeSuppliers = getActiveSuppliers();
        if (activeSuppliers.isEmpty()) {
            loggingService.warning(tender, "SUPPLIER_SEARCH", "Нет активных поставщиков в базе");
            return;
        }
        item.setStatus(ItemStatus.PENDING);
        searchItemOnSuppliersByDescription(tender, item, activeSuppliers);
    }

    @Transactional
    public void searchSuppliersForTender(Tender tender) {
        loggingService.info(tender, "SUPPLIER_SEARCH", "Начало поиска на сайтах поставщиков");
        tender.setStatus(TenderStatus.SEARCHING_SUPPLIERS);

        for (TenderItem item : tender.getItems()) {
            item.setStatus(ItemStatus.PENDING);
            item.setFoundModelName(null);
            item.setFoundModelUrl(null);
            item.setFoundModelPrice(null);
            item.setSupplierSite(null);
            item.setMatchScore(null);
            tenderItemRepository.save(item);
        }

        List<Supplier> activeSuppliers = getActiveSuppliers();
        if (activeSuppliers.isEmpty()) {
            loggingService.warning(tender, "SUPPLIER_SEARCH", "Нет активных поставщиков в базе");
            return;
        }

        for (TenderItem item : tender.getItems()) {
            if (item.getStatus() != null && item.getStatus() != ItemStatus.PENDING) continue;
            searchItemOnSuppliers(tender, item, activeSuppliers);
        }

        boolean anyFound = tender.getItems().stream()
                .anyMatch(i -> i.getStatus() == ItemStatus.FOUND_ON_SUPPLIER || i.getStatus() == ItemStatus.MODEL_MATCHED);

        if (anyFound) {
            tender.setStatus(TenderStatus.SUPPLIERS_FOUND);
            loggingService.success(tender, "SUPPLIER_SEARCH", "Товары найдены на сайтах поставщиков");
        } else {
            tender.setStatus(TenderStatus.PARSED);
            loggingService.warning(tender, "SUPPLIER_SEARCH", "Товары не найдены на сайтах поставщиков");
        }
    }

    private List<Supplier> getActiveSuppliers() {
        return supplierRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();
    }

    private void searchItemOnSuppliers(Tender tender, TenderItem item, List<Supplier> suppliers) {
        String sourceDescription = item.getDocumentDescription();
        if (sourceDescription == null || sourceDescription.isBlank()) {
            sourceDescription = item.getDescription();
        }
        Map<String, Object> lotParams = ProductParameterExtractor.parseExtractedParams(item.getExtractedParams());
        String searchQuery = extractSearchQuery(sourceDescription, lotParams);
        loggingService.info(tender, "SUPPLIER_SEARCH", "Поиск по запросу '" + searchQuery + "' для лота " + item.getLotNumber());
        processSearchResults(tender, item, suppliers, searchQuery, sourceDescription, lotParams);
    }

    private void searchItemOnSuppliersByDescription(Tender tender, TenderItem item, List<Supplier> suppliers) {
        String sourceDescription = item.getDocumentDescription();
        if (sourceDescription == null || sourceDescription.isBlank()) {
            loggingService.info(tender, "SUPPLIER_SEARCH", "Поле 'Описание лота' пустое для лота " + item.getLotNumber() + ", поиск не выполнен");
            item.setStatus(ItemStatus.NOT_FOUND);
            tenderItemRepository.save(item);
            return;
        }
        Map<String, Object> lotParams = ProductParameterExtractor.parseExtractedParams(item.getExtractedParams());
        String searchQuery = extractSearchQuery(sourceDescription, lotParams);
        loggingService.info(tender, "SUPPLIER_SEARCH", "Поиск по описанию лота '" + searchQuery + "' для лота " + item.getLotNumber());
        processSearchResults(tender, item, suppliers, searchQuery, sourceDescription, lotParams);
    }

    private void processSearchResults(Tender tender, TenderItem item, List<Supplier> suppliers,
                                      String searchQuery, String sourceDescription, Map<String, Object> lotParams) {
        boolean hasExtractedParams = !lotParams.isEmpty();

        // ===== Stage 1: Exact catalog lookup =====
        List<EquipmentCatalogItem> catalogMatches = performCatalogExactLookup(lotParams);
        if (!catalogMatches.isEmpty()) {
            loggingService.info(tender, "SUPPLIER_SEARCH",
                "Stage 1: Найдено " + catalogMatches.size() + " точных совпадений в каталоге для лота " + item.getLotNumber());
        }

        // ===== Stage 2: Primary search with full query =====
        List<SupplierResult> allResults = searchOnAllSuppliers(suppliers, searchQuery, sourceDescription, lotParams, hasExtractedParams, catalogMatches);

        // ===== Stage 3: Fallback — iterate characteristics by priority =====
        if (allResults.isEmpty() || bestScore(allResults) < SCORE_THRESHOLD) {
            List<SupplierResult> fallbackResults = performFallbackSearch(suppliers, sourceDescription, lotParams, hasExtractedParams, catalogMatches);
            if (!fallbackResults.isEmpty()) {
                // Merge and re-rank
                allResults = mergeResults(allResults, fallbackResults);
            }
        }

        if (allResults.isEmpty() || bestScore(allResults) < SCORE_THRESHOLD * 0.5) {
            markNotFound(item);
            loggingService.info(tender, "SUPPLIER_SEARCH", "Ничего не найдено для лота " + item.getLotNumber());
            return;
        }

        // Sort by matchScore descending
        allResults.sort((a, b) -> Double.compare(b.result.getMatchScore(), a.result.getMatchScore()));

        // Clear old found models
        foundModelRepository.deleteByTenderItemId(item.getId());
        item.getFoundModels().clear();

        int topCount = Math.min(5, allResults.size());
        boolean exactMatch = allResults.get(0).result.getMatchScore() >= EXACT_MATCH_THRESHOLD;

        for (int i = 0; i < topCount; i++) {
            SupplierResult sr = allResults.get(i);
            SearchResult r = sr.result;
            FoundModel fm = new FoundModel(
                item, r.getProductName(), r.getProductUrl(), r.getPrice(),
                sr.supplier.getSiteUrl(), r.getMatchScore(), r.getSemanticScore(), r.getParametricScore(), i + 1
            );
            foundModelRepository.save(fm);
            item.getFoundModels().add(fm);
        }

        // Set best as primary
        SearchResult best = allResults.get(0).result;
        Supplier bestSupplier = allResults.get(0).supplier;
        item.setFoundModelName(best.getProductName());
        item.setFoundModelUrl(best.getProductUrl());
        item.setFoundModelPrice(best.getPrice());
        item.setSupplierSite(bestSupplier.getSiteUrl());
        item.setMatchScore(best.getMatchScore());
        item.setStatus(ItemStatus.FOUND_ON_SUPPLIER);
        tenderItemRepository.save(item);

        saveToCatalog(item, best, bestSupplier);

        if (exactMatch) {
            loggingService.success(tender, "SUPPLIER_SEARCH", "Точное совпадение на " + bestSupplier.getSiteUrl() + ": " + best.getProductName() + " (score=" + String.format("%.2f", best.getMatchScore()) + ")");
        } else {
            loggingService.success(tender, "SUPPLIER_SEARCH", "Топ-" + topCount + " вариантов найдено, лучший: " + best.getProductName() + " (score=" + String.format("%.2f", best.getMatchScore()) + ")");
        }
    }

    // ========== Stage 1: Catalog Exact Lookup ==========

    private List<EquipmentCatalogItem> performCatalogExactLookup(Map<String, Object> lotParams) {
        List<EquipmentCatalogItem> matches = new ArrayList<>();
        Object equipmentType = lotParams.get("equipmentType");
        Object model = lotParams.get("model");
        if (equipmentType == null) return matches;

        EquipmentType type = typeRepository.findByNameIgnoreCase(equipmentType.toString().trim()).orElse(null);
        if (type == null) return matches;

        if (model != null && !model.toString().isBlank()) {
            // Exact match by type + model
            catalogItemRepository.findByEquipmentTypeIdAndModelNameIgnoreCase(type.getId(), model.toString().trim())
                .ifPresent(matches::add);
            // Also partial match for model variants
            matches.addAll(catalogItemRepository.findByEquipmentTypeIdAndModelNameContainingIgnoreCase(type.getId(), model.toString().trim()));
        } else {
            // Without model, return all items of this type (for reference)
            matches.addAll(catalogItemRepository.findByEquipmentTypeId(type.getId()));
        }
        return matches.stream().distinct().toList();
    }

    // ========== Stage 2 & 3: Supplier Search ==========

    private List<SupplierResult> searchOnAllSuppliers(List<Supplier> suppliers, String searchQuery,
                                                       String sourceDescription, Map<String, Object> lotParams,
                                                       boolean hasExtractedParams, List<EquipmentCatalogItem> catalogMatches) {
        List<SupplierResult> allResults = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            try {
                SearchResult result = searchOnSupplier(supplier, searchQuery, sourceDescription, lotParams, hasExtractedParams, catalogMatches);
                if (result.isFound()) {
                    allResults.add(new SupplierResult(result, supplier));
                }
            } catch (Exception e) {
                log.warn("Search error on {}: {}", supplier.getSiteUrl(), e.getMessage());
            }
        }
        return allResults;
    }

    private List<SupplierResult> performFallbackSearch(List<Supplier> suppliers, String sourceDescription,
                                                        Map<String, Object> lotParams, boolean hasExtractedParams,
                                                        List<EquipmentCatalogItem> catalogMatches) {
        List<SupplierResult> bestFallback = new ArrayList<>();
        double bestFallbackScore = 0;

        for (String characteristic : FALLBACK_PRIORITY) {
            Object value = lotParams.get(characteristic);
            if (value == null || value.toString().isBlank()) continue;

            List<String> parts = new ArrayList<>();
            Object equipmentType = lotParams.get("equipmentType");
            if (equipmentType != null) parts.add(equipmentType.toString());
            parts.add(value.toString());
            String fallbackQuery = String.join(" ", parts);

            log.info("Stage 3 fallback: trying query '{}' (characteristic={})", fallbackQuery, characteristic);
            List<SupplierResult> results = searchOnAllSuppliers(suppliers, fallbackQuery, sourceDescription, lotParams, hasExtractedParams, catalogMatches);

            if (!results.isEmpty()) {
                double score = bestScore(results);
                log.info("Stage 3 fallback: query '{}' returned {} results, bestScore={}", fallbackQuery, results.size(), score);
                if (score > bestFallbackScore) {
                    bestFallbackScore = score;
                    bestFallback = results;
                }
                if (score >= SCORE_THRESHOLD) {
                    // Good enough — stop iterating
                    break;
                }
            }
        }
        return bestFallback;
    }

    private double bestScore(List<SupplierResult> results) {
        if (results.isEmpty()) return 0.0;
        return results.stream().mapToDouble(r -> r.result.getMatchScore()).max().orElse(0.0);
    }

    private List<SupplierResult> mergeResults(List<SupplierResult> primary, List<SupplierResult> fallback) {
        Map<String, SupplierResult> merged = new LinkedHashMap<>();
        for (SupplierResult sr : primary) {
            merged.put(key(sr), sr);
        }
        for (SupplierResult sr : fallback) {
            String key = key(sr);
            if (!merged.containsKey(key) || sr.result.getMatchScore() > merged.get(key).result.getMatchScore()) {
                merged.put(key, sr);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private String key(SupplierResult sr) {
        return sr.supplier.getSiteUrl() + "|" + sr.result.getProductName();
    }

    private void markNotFound(TenderItem item) {
        item.setMatchScore(null);
        item.setStatus(ItemStatus.NOT_FOUND);
        item.setFoundModelName(null);
        item.setFoundModelUrl(null);
        item.setFoundModelPrice(null);
        item.setSupplierSite(null);
        item.getFoundModels().clear();
        tenderItemRepository.save(item);
    }

    private SearchResult searchOnSupplier(Supplier supplier, String searchQuery, String originalDescription,
                                          Map<String, Object> lotParams, boolean hasExtractedParams,
                                          List<EquipmentCatalogItem> catalogMatches) throws IOException {
        String searchUrl = buildSearchUrl(supplier, searchQuery);
        log.info("Searching on {}: URL={}", supplier.getSiteUrl(), searchUrl);
        Document doc = SslUtils.createConnection(searchUrl).timeout(timeout).userAgent(userAgent).followRedirects(true).get();
        return parseSearchResults(doc, supplier, originalDescription, lotParams, hasExtractedParams, catalogMatches);
    }

    private String buildSearchUrl(Supplier supplier, String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if (supplier.getSiteUrl().contains("redhon.ru")) return "https://redhon.ru/search/?q=" + encodedQuery;
        if (supplier.getSiteUrl().contains("dia-m.ru")) return "https://www.dia-m.ru/search/?q=" + encodedQuery;
        if (supplier.getSearchUrlTemplate() != null) return supplier.getSearchUrlTemplate().replace("{query}", encodedQuery);
        return supplier.getSiteUrl() + "/search/?q=" + encodedQuery;
    }

    private SearchResult parseSearchResults(Document doc, Supplier supplier, String originalDescription,
                                              Map<String, Object> lotParams, boolean hasExtractedParams,
                                              List<EquipmentCatalogItem> catalogMatches) {
        String siteUrl = supplier.getSiteUrl();
        if (siteUrl.contains("redhon.ru")) return parseRedhonResults(doc, originalDescription, lotParams, hasExtractedParams, catalogMatches);
        if (siteUrl.contains("dia-m.ru")) return parseDiaMResults(doc, originalDescription, lotParams, hasExtractedParams, catalogMatches);
        return parseGenericResults(doc, supplier, originalDescription, lotParams, hasExtractedParams, catalogMatches);
    }

    private SearchResult parseRedhonResults(Document doc, String originalDescription,
                                              Map<String, Object> lotParams, boolean hasExtractedParams,
                                              List<EquipmentCatalogItem> catalogMatches) {
        // Redhon search results are <a class="dark_link font_mlg title"> inside .bordered.rounded3.item.colored_theme_hover_bg-block
        Elements products = doc.select("a.dark_link.font_mlg.title");
        // Filter out blog posts and category pages — keep only product pages (3+ path segments after /catalog/)
        products = new Elements(products.stream()
            .filter(el -> {
                String href = el.attr("href");
                if (!href.contains("/catalog/")) return false;
                String afterCatalog = href.substring(href.indexOf("/catalog/") + 9);
                // Remove query string
                int qIdx = afterCatalog.indexOf('?');
                if (qIdx >= 0) afterCatalog = afterCatalog.substring(0, qIdx);
                // Count non-empty segments after catalog/
                long segments = java.util.Arrays.stream(afterCatalog.split("/"))
                    .filter(s -> !s.isBlank()).count();
                return segments >= 3; // product pages have 3+ segments
            })
            .toList());
        if (products.isEmpty()) {
            products = doc.select(".product-item, .catalog-item, .product-card, [class*=product]");
            if (products.isEmpty()) products = doc.select(".item, .goods-item, .search-result-item");
        }
        log.info("Redhon parsed {} product links", products.size());
        return findBestCandidate(products, originalDescription, "https://redhon.ru", lotParams, hasExtractedParams, catalogMatches);
    }

    private SearchResult parseDiaMResults(Document doc, String originalDescription,
                                            Map<String, Object> lotParams, boolean hasExtractedParams,
                                            List<EquipmentCatalogItem> catalogMatches) {
        Elements products = doc.select("article.product-card, .product-card.collapse.js-product-card");
        if (products.isEmpty()) {
            products = doc.select(".product-item, .catalog-item, .product-card, [class*=product]");
        }
        if (products.isEmpty()) products = doc.select(".item, .goods-item, .search-result-item, .catalog-element");
        log.info("Dia-M parsed {} products", products.size());
        return findBestCandidate(products, originalDescription, "https://www.dia-m.ru", lotParams, hasExtractedParams, catalogMatches);
    }

    private SearchResult parseGenericResults(Document doc, Supplier supplier, String originalDescription,
                                               Map<String, Object> lotParams, boolean hasExtractedParams,
                                               List<EquipmentCatalogItem> catalogMatches) {
        String selector = supplier.getSearchSelectorProduct() != null ? supplier.getSearchSelectorProduct() : ".product-item, .item";
        return findBestCandidate(doc.select(selector), originalDescription, supplier.getSiteUrl(), lotParams, hasExtractedParams, catalogMatches);
    }

    private SearchResult findBestCandidate(Elements products, String originalDescription, String baseUrl,
                                             Map<String, Object> lotParams, boolean hasExtractedParams,
                                             List<EquipmentCatalogItem> catalogMatches) {
        SearchResult best = SearchResult.notFound();
        double bestScore = 0.0;
        int evaluated = 0;

        String normOriginal = SearchNormalizationUtils.normalize(originalDescription);

        for (Element product : products) {
            String name = extractProductName(product);
            if (name == null) continue;
            evaluated++;

            String normName = SearchNormalizationUtils.normalize(name);

            // Semantic similarity using normalized strings
            double semanticScore = SearchNormalizationUtils.normalizedSimilarity(normOriginal, normName);

            double parametricScore = 0.0;
            double matchScore = semanticScore;
            ProductParameterExtractor.ScoreResult discreteScore = null;

            if (hasExtractedParams) {
                Map<String, Object> prodParams = ProductParameterExtractor.extractFromTitle(name);
                discreteScore = ProductParameterExtractor.calculateDiscreteScore(lotParams, prodParams);
                parametricScore = discreteScore.normalizedScore();

                // Boost if catalog match
                if (!catalogMatches.isEmpty()) {
                    for (EquipmentCatalogItem cat : catalogMatches) {
                        String catModel = SearchNormalizationUtils.normalize(cat.getModelName());
                        if (normName.contains(catModel) || catModel.contains(normName)) {
                            parametricScore = Math.min(1.0, parametricScore + 0.15);
                            semanticScore = Math.min(1.0, semanticScore + 0.10);
                            break;
                        }
                    }
                }

                // Weighted: semantic 25%, parametric 75%
                matchScore = semanticScore * 0.25 + parametricScore * 0.75;
                log.debug("Candidate '{}' semantic={} parametric={} discrete={} combined={}",
                    name, String.format("%.2f", semanticScore), String.format("%.2f", parametricScore),
                    discreteScore != null ? String.format("%.3f", discreteScore.normalizedScore()) : "n/a",
                    String.format("%.2f", matchScore));
            }

            if (matchScore > bestScore) {
                bestScore = matchScore;
                best = new SearchResult(true, name, extractProductUrl(product, baseUrl), extractPrice(product),
                    semanticScore, matchScore, parametricScore, semanticScore);
            }
        }

        log.info("Evaluated {} valid products, bestScore={:.3f}, found={}", evaluated, bestScore, best.isFound());
        return best;
    }

    private String extractProductName(Element product) {
        // Direct element match for redhon.ru search results
        if (product.is("a.dark_link.font_mlg.title")) {
            String text = product.text().trim();
            if (text.length() > 3) return text;
        }
        String[] selectors = {".product-card__name", ".product-card__name a", "h2 a", "h3 a", "h4 a", ".product-name a", ".item-title a", "h2", "h3", "h4", ".product-name", ".item-title", "a[href*=product]", "a[href*=catalog]", ".name a", ".title a"};
        for (String selector : selectors) {
            Elements elements = product.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.first().text().trim();
                if (text.length() > 3) return text;
            }
        }
        return null;
    }

    private String extractProductUrl(Element product, String baseUrl) {
        // Direct link element (redhon search results)
        if (product.tagName().equals("a") && product.hasAttr("href")) {
            String href = product.attr("href");
            if (href.startsWith("http")) return href;
            return baseUrl + (href.startsWith("/") ? "" : "/") + href;
        }
        Elements links = product.select("a[href]");
        if (!links.isEmpty()) {
            String href = links.first().attr("href");
            if (href.startsWith("http")) return href;
            return baseUrl + (href.startsWith("/") ? "" : "/") + href;
        }
        return baseUrl;
    }

    private Double extractPrice(Element product) {
        for (Element priceEl : product.select(".product-model__price, .price, .product-price, .cost, [class*=price], [class*=cost]")) {
            String text = priceEl.text().replaceAll("[^\\d,.]", "").replace(",", ".");
            if (!text.isEmpty()) {
                try { return Double.parseDouble(text); } catch (NumberFormatException e) { /* ignore */ }
            }
        }
        return null;
    }

    private String extractSearchQuery(String description, Map<String, Object> lotParams) {
        // Build query from structured extracted params first
        Object equipmentType = lotParams.get("equipmentType");
        if (equipmentType == null || equipmentType.toString().isBlank()) {
            equipmentType = ProductParameterExtractor.detectEquipmentType(description.toLowerCase());
        }
        String et = equipmentType != null ? SearchNormalizationUtils.normalize(equipmentType.toString()) : null;

        List<String> queryParts = new ArrayList<>();
        if (et != null && !et.isBlank()) {
            queryParts.add(et);
        }

        Object model = lotParams.get("model");
        if (model != null && !model.toString().isBlank()) {
            queryParts.add(SearchNormalizationUtils.normalize(model.toString()));
        }

        addParam(queryParts, lotParams.get("volumeL"));
        addParam(queryParts, lotParams.get("maxTempC"));
        addParam(queryParts, lotParams.get("tempRange"));
        addParam(queryParts, lotParams.get("regulatorType"));
        addParam(queryParts, lotParams.get("accuracyPercent"));
        addParam(queryParts, lotParams.get("thermocoupleType"));
        addParam(queryParts, lotParams.get("heatUpTimeMin"));
        addParam(queryParts, lotParams.get("dimensionsMm"));
        addParam(queryParts, lotParams.get("weightKg"));
        addParam(queryParts, lotParams.get("powerKW"));
        addParam(queryParts, lotParams.get("voltageV"));
        addParam(queryParts, lotParams.get("material"));
        addParam(queryParts, lotParams.get("programsCount"));

        if (!queryParts.isEmpty()) {
            return String.join(" ", queryParts);
        }

        // Fallback: extract keywords from raw description
        String[] words = description.toLowerCase().split("\\s+");
        String[] stopWords = {
            "для", "и", "или", "в", "на", "с", "по", "из", "от", "до", "не", "без", "при", "об", "за", "под", "про", "через", "между", "над", "около",
            "мм", "кг", "л", "вт", "квт", "°c", "°с",
            "описание", "предмета", "предмет", "закупки", "закупка", "оказания", "услуг", "услуги", "работ", "работы", "поставки", "поставка", "изготовления", "производства",
            "предназначены", "предназначена", "термической", "термическая", "обработки", "обработка", "материалов", "материала", "камере", "камера", "рабочей", "рабочая", "температуре", "температура", "конструкции", "конструкция", "использованием", "использования",
            "технические", "техническая", "характеристики", "характеристика", "параметр", "параметры", "таблица", "значение", "значения", "номинальное", "номинальная",
            "питающей", "сети", "более", "менее", "должна", "должен", "соответствовать", "соответствие", "гост", "ту", "iso", "iec", "en"
        };
        List<String> keywords = new ArrayList<>();

        for (String word : words) {
            String clean = word.replaceAll("[^а-яa-z0-9-]", "").trim();
            if (clean.length() > 2 && !isStopWord(clean, stopWords)) {
                if (clean.matches("\\d+[xх]\\d+[xх]?\\d*")) continue;
                if (clean.matches("\\d{5,}")) continue;
                if (clean.matches("\\d{3,4}")) continue;
                if (clean.matches("[a-z]+") && clean.length() <= 4) continue;
                if (clean.matches("[a-zа-я]+\\d+[a-zа-я\\d-]*")) continue;
                if (!keywords.contains(clean)) {
                    keywords.add(clean);
                    if (keywords.size() >= 3) break;
                }
            }
        }

        // Ensure equipment type is present
        if (et != null && et.length() > 2) {
            boolean hasEt = false;
            for (String kw : keywords) {
                if (kw.contains(et) || et.contains(kw)) {
                    hasEt = true;
                    break;
                }
            }
            if (!hasEt) {
                keywords.add(0, et);
                if (keywords.size() > 3) keywords.remove(keywords.size() - 1);
            }
        }

        return String.join(" ", keywords);
    }

    private void addParam(List<String> queryParts, Object value) {
        if (value == null) return;
        String s = value.toString().trim();
        if (s.isBlank() || s.equals("null") || s.equals("-")) return;
        // Strip .0 from doubles
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        s = SearchNormalizationUtils.normalizeUnits(s);
        if (!queryParts.contains(s)) {
            queryParts.add(s);
        }
    }

    private boolean isStopWord(String word, String[] stopWords) {
        for (String stop : stopWords) if (stop.equals(word)) return true;
        return false;
    }

    private double calculateSimilarity(String str1, String str2) {
        return SearchNormalizationUtils.normalizedSimilarity(str1, str2);
    }

    private void saveToCatalog(TenderItem item, SearchResult best, Supplier supplier) {
        if (item.getExtractedParams() == null || item.getExtractedParams().isBlank()) return;
        try {
            JsonNode params = MAPPER.readTree(item.getExtractedParams());
            String equipmentType = null;
            if (params.has("equipmentType") && !params.get("equipmentType").isNull()) {
                JsonNode etNode = params.get("equipmentType");
                if (etNode.isObject() && etNode.has("value")) {
                    equipmentType = etNode.get("value").asText();
                } else {
                    equipmentType = etNode.asText();
                }
            }
            if (equipmentType == null || equipmentType.isBlank()) return;

            Map<String, String> specValues = new HashMap<>();
            params.fields().forEachRemaining(e -> {
                String key = e.getKey();
                if ("equipmentType".equals(key) || "model".equals(key) || e.getValue().isNull()) return;
                JsonNode node = e.getValue();
                String value;
                if (node.isObject() && node.has("value")) {
                    value = node.get("value").asText();
                } else {
                    value = node.asText();
                }
                specValues.put(key, value);
            });

            String modelName = best.getProductName();
            String manufacturerName = supplier.getName();
            if (manufacturerName == null || manufacturerName.isBlank()) {
                manufacturerName = supplier.getSiteUrl();
            }

            catalogService.saveModel(equipmentType, manufacturerName, modelName, specValues);
        } catch (Exception e) {
            log.warn("Failed to save model to catalog: {}", e.getMessage());
        }
    }

    private record SupplierResult(SearchResult result, Supplier supplier) {}

    public static class SearchResult {
        private final boolean found;
        private final String productName;
        private final String productUrl;
        private final Double price;
        private final double similarity;
        private final double matchScore;
        private final double parametricScore;
        private final double semanticScore;

        public SearchResult(boolean found, String productName, String productUrl, Double price, double similarity) {
            this(found, productName, productUrl, price, similarity, similarity, 0, 0);
        }

        public SearchResult(boolean found, String productName, String productUrl, Double price, double similarity, double matchScore) {
            this(found, productName, productUrl, price, similarity, matchScore, 0, 0);
        }

        public SearchResult(boolean found, String productName, String productUrl, Double price,
                            double similarity, double matchScore, double parametricScore, double semanticScore) {
            this.found = found; this.productName = productName; this.productUrl = productUrl; this.price = price;
            this.similarity = similarity; this.matchScore = matchScore;
            this.parametricScore = parametricScore; this.semanticScore = semanticScore;
        }

        public static SearchResult notFound() { return new SearchResult(false, null, null, null, 0, 0, 0, 0); }
        public boolean isFound() { return found; }
        public String getProductName() { return productName; }
        public String getProductUrl() { return productUrl; }
        public Double getPrice() { return price; }
        public double getSimilarity() { return similarity; }
        public double getMatchScore() { return matchScore; }
        public double getParametricScore() { return parametricScore; }
        public double getSemanticScore() { return semanticScore; }
    }
}
