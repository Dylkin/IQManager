package com.tenderbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductParameterExtractor {

    private static final Logger log = LoggerFactory.getLogger(ProductParameterExtractor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Regex patterns for common equipment parameters in Russian product titles
    private static final Pattern VOLUME_PATTERN = Pattern.compile("(\\d+[,.]?\\d*)\\s*л(?![а-яa-z])");
    private static final Pattern TEMP_PATTERN = Pattern.compile("(?:до\\s+)?(\\d+)\\s*°?[cс](?![а-яa-z])");
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+[,.]?\\d*)\\s*кг(?![а-яa-z])");
    private static final Pattern POWER_PATTERN = Pattern.compile("(\\d+[,.]?\\d*)\\s*кВт(?![а-яa-z])");
    private static final Pattern VOLTAGE_PATTERN = Pattern.compile("(\\d+)\\s*В(?![а-яa-z])");
    private static final Pattern DIMENSIONS_PATTERN = Pattern.compile("(\\d+[xх]\\d+[xх]\\d+)");
    private static final Pattern PROGRAMS_PATTERN = Pattern.compile("(\\d+)\\s*программ");

    public static Map<String, Object> extractFromTitle(String title) {
        Map<String, Object> params = new HashMap<>();
        if (title == null) return params;
        String lower = title.toLowerCase();

        // Volume (л)
        Matcher vol = VOLUME_PATTERN.matcher(title);
        if (vol.find()) {
            params.put("volumeL", parseDouble(vol.group(1)));
        }

        // Temperature (°C)
        Matcher temp = TEMP_PATTERN.matcher(title);
        int maxTemp = 0;
        while (temp.find()) {
            int t = parseInt(temp.group(1));
            if (t > maxTemp) maxTemp = t;
        }
        if (maxTemp > 0) {
            params.put("maxTempC", maxTemp);
        }

        // Weight (кг)
        Matcher weight = WEIGHT_PATTERN.matcher(title);
        if (weight.find()) {
            params.put("weightKg", parseDouble(weight.group(1)));
        }

        // Power (кВт)
        Matcher power = POWER_PATTERN.matcher(title);
        if (power.find()) {
            params.put("powerKW", parseDouble(power.group(1)));
        }

        // Voltage (В)
        Matcher voltage = VOLTAGE_PATTERN.matcher(title);
        if (voltage.find()) {
            params.put("voltageV", parseInt(voltage.group(1)));
        }

        // Dimensions (mm)
        Matcher dim = DIMENSIONS_PATTERN.matcher(title);
        if (dim.find()) {
            params.put("dimensionsMm", dim.group(1).replace('х', 'x'));
        }

        // Programs count
        Matcher prog = PROGRAMS_PATTERN.matcher(title);
        if (prog.find()) {
            params.put("programsCount", parseInt(prog.group(1)));
        }

        // Exhaust / вытяжка
        if (lower.contains("вытяжка")) {
            params.put("hasExhaust", true);
        }

        // Equipment type heuristics
        params.put("equipmentType", detectEquipmentType(lower));

        return params;
    }

    public static Map<String, Object> parseExtractedParams(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            JsonNode node = MAPPER.readTree(json);
            Map<String, Object> params = new HashMap<>();
            extractWrappedField(node, params, "equipmentType", JsonNode::asText);
            extractWrappedField(node, params, "model", JsonNode::asText);
            extractWrappedField(node, params, "volumeL", JsonNode::asDouble);
            extractWrappedField(node, params, "maxTempC", JsonNode::asInt);
            extractWrappedField(node, params, "tempRange", JsonNode::asText);
            extractWrappedField(node, params, "regulatorType", JsonNode::asText);
            extractWrappedField(node, params, "accuracyPercent", JsonNode::asDouble);
            extractWrappedField(node, params, "thermocoupleType", JsonNode::asText);
            extractWrappedField(node, params, "heatUpTimeMin", JsonNode::asInt);
            extractWrappedField(node, params, "dimensionsMm", JsonNode::asText);
            extractWrappedField(node, params, "weightKg", JsonNode::asDouble);
            extractWrappedField(node, params, "powerKW", JsonNode::asDouble);
            extractWrappedField(node, params, "voltageV", JsonNode::asInt);
            extractWrappedField(node, params, "material", JsonNode::asText);
            extractWrappedField(node, params, "programsCount", JsonNode::asInt);
            JsonNode hasExhaustNode = resolveFieldNode(node, "hasExhaust");
            if (hasExhaustNode != null) {
                params.put("hasExhaust", hasExhaustNode.asBoolean());
            }
            return params;
        } catch (Exception e) {
            log.warn("Failed to parse extracted params JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public static double calculateParametricScore(Map<String, Object> lotParams, Map<String, Object> productParams) {
        return calculateDiscreteScore(lotParams, productParams).normalizedScore();
    }

    /**
     * Discrete scoring: exact match = +10 points, close match = +5 points.
     * Each characteristic has a weight. Total = sum(points * weight).
     * normalizedScore = total / maxPossible where maxPossible = sum(10 * weight).
     */
    public static ScoreResult calculateDiscreteScore(Map<String, Object> lotParams, Map<String, Object> productParams) {
        double totalScore = 0.0;
        double totalWeight = 0.0;

        // Equipment type match (weight 2.0)
        Object lotType = lotParams.get("equipmentType");
        Object prodType = productParams.get("equipmentType");
        if (lotType != null && prodType != null) {
            boolean exact = SearchNormalizationUtils.exactMatch(lotType.toString(), prodType.toString());
            boolean close = !exact && SearchNormalizationUtils.containsIgnoreCaseNormalized(lotType.toString(), prodType.toString());
            totalScore += scorePoints(exact, close) * 2.0;
            totalWeight += 2.0;
        }

        // Model name match (weight 3.0) — highest priority
        Object lotModel = lotParams.get("model");
        Object prodModel = productParams.get("model");
        if (lotModel != null && prodModel != null) {
            boolean exact = SearchNormalizationUtils.exactMatch(lotModel.toString(), prodModel.toString());
            boolean close = !exact && SearchNormalizationUtils.containsIgnoreCaseNormalized(lotModel.toString(), prodModel.toString());
            totalScore += scorePoints(exact, close) * 3.0;
            totalWeight += 3.0;
        }

        // Max temperature (weight 3.0) — critical if lower than required
        Integer lotTemp = getInt(lotParams, "maxTempC");
        Integer prodTemp = getInt(productParams, "maxTempC");
        if (lotTemp != null && prodTemp != null) {
            if (prodTemp < lotTemp) {
                totalScore += 0; // below requirement = zero
            } else {
                boolean exact = prodTemp.equals(lotTemp);
                boolean close = !exact && numericCloseness((double) lotTemp, (double) prodTemp) >= 0.7;
                totalScore += scorePoints(exact, close) * 3.0;
            }
            totalWeight += 3.0;
        }

        // Volume comparison (weight 2.5)
        Double lotVol = getDouble(lotParams, "volumeL");
        Double prodVol = getDouble(productParams, "volumeL");
        if (lotVol != null && prodVol != null) {
            boolean exact = lotVol.equals(prodVol);
            boolean close = !exact && numericClosenessRelaxed(lotVol, prodVol) >= 0.7;
            totalScore += scorePoints(exact, close) * 2.5;
            totalWeight += 2.5;
        }

        // Weight comparison (weight 1.0)
        Double lotWeightKg = getDouble(lotParams, "weightKg");
        Double prodWeightKg = getDouble(productParams, "weightKg");
        if (lotWeightKg != null && prodWeightKg != null) {
            boolean exact = lotWeightKg.equals(prodWeightKg);
            boolean close = !exact && numericClosenessRelaxed(lotWeightKg, prodWeightKg) >= 0.7;
            totalScore += scorePoints(exact, close) * 1.0;
            totalWeight += 1.0;
        }

        // Programs count comparison (weight 0.5)
        Integer lotProg = getInt(lotParams, "programsCount");
        Integer prodProg = getInt(productParams, "programsCount");
        if (lotProg != null && prodProg != null) {
            boolean exact = lotProg.equals(prodProg);
            boolean close = !exact && numericClosenessRelaxed((double) lotProg, (double) prodProg) >= 0.7;
            totalScore += scorePoints(exact, close) * 0.5;
            totalWeight += 0.5;
        }

        // Voltage comparison (weight 1.0)
        Double lotVoltage = getDouble(lotParams, "voltageV");
        Double prodVoltage = getDouble(productParams, "voltageV");
        if (lotVoltage != null && prodVoltage != null) {
            boolean exact = lotVoltage.equals(prodVoltage);
            boolean close = !exact && numericCloseness(lotVoltage, prodVoltage) >= 0.7;
            totalScore += scorePoints(exact, close) * 1.0;
            totalWeight += 1.0;
        }

        // Power comparison (weight 1.5)
        Double lotPower = getDouble(lotParams, "powerKW");
        Double prodPower = getDouble(productParams, "powerKW");
        if (lotPower != null && prodPower != null) {
            boolean exact = lotPower.equals(prodPower);
            boolean close = !exact && numericClosenessRelaxed(lotPower, prodPower) >= 0.7;
            totalScore += scorePoints(exact, close) * 1.5;
            totalWeight += 1.5;
        }

        // Exhaust comparison (weight 1.0)
        Boolean lotExhaust = (Boolean) lotParams.get("hasExhaust");
        Boolean prodExhaust = (Boolean) productParams.get("hasExhaust");
        if (lotExhaust != null) {
            boolean exact = Boolean.TRUE.equals(lotExhaust) == Boolean.TRUE.equals(prodExhaust);
            totalScore += (exact ? 10.0 : 0.0) * 1.0;
            totalWeight += 1.0;
        }

        // Dimensions comparison (weight 1.0)
        String lotDim = (String) lotParams.get("dimensionsMm");
        String prodDim = (String) productParams.get("dimensionsMm");
        if (lotDim != null && prodDim != null) {
            boolean exact = SearchNormalizationUtils.toKey(lotDim).equals(SearchNormalizationUtils.toKey(prodDim));
            totalScore += (exact ? 10.0 : 0.0) * 1.0;
            totalWeight += 1.0;
        }

        // Material comparison (weight 1.0)
        String lotMaterial = (String) lotParams.get("material");
        String prodMaterial = (String) productParams.get("material");
        if (lotMaterial != null && prodMaterial != null) {
            boolean exact = SearchNormalizationUtils.exactMatch(lotMaterial, prodMaterial);
            boolean close = !exact && SearchNormalizationUtils.containsIgnoreCaseNormalized(lotMaterial, prodMaterial);
            totalScore += scorePoints(exact, close) * 1.0;
            totalWeight += 1.0;
        }

        double maxPossible = totalWeight * 10.0;
        double normalized = maxPossible > 0 ? totalScore / maxPossible : 0.0;
        return new ScoreResult(totalScore, maxPossible, normalized, totalWeight);
    }

    private static double scorePoints(boolean exact, boolean close) {
        if (exact) return 10.0;
        if (close) return 5.0;
        return 0.0;
    }

    public record ScoreResult(double totalScore, double maxPossible, double normalizedScore, double totalWeight) {
        public boolean passesThreshold(double threshold) {
            return normalizedScore >= threshold;
        }
    }

    private static double numericCloseness(double expected, double actual) {
        if (expected == 0) return actual == 0 ? 1.0 : 0.0;
        double ratio = actual / expected;
        if (ratio >= 0.95 && ratio <= 1.05) return 1.0;
        if (ratio >= 0.8 && ratio <= 1.2) return 0.7;
        if (ratio >= 0.5 && ratio <= 1.5) return 0.3;
        return 0.0;
    }

    private static double numericClosenessRelaxed(double expected, double actual) {
        if (expected == 0) return actual == 0 ? 1.0 : 0.0;
        double ratio = actual / expected;
        if (ratio >= 0.9 && ratio <= 1.1) return 1.0;
        if (ratio >= 0.7 && ratio <= 1.5) return 0.7;
        if (ratio >= 0.5 && ratio <= 2.0) return 0.4;
        return 0.0;
    }

    public static String detectEquipmentType(String lower) {
        if (lower.contains("печь") || lower.contains("печи")) return "печь";
        if (lower.contains("анализатор")) return "анализатор";
        if (lower.contains("спектрофотометр")) return "спектрофотометр";
        if (lower.contains("хроматограф")) return "хроматограф";
        if (lower.contains("баланс") || lower.contains("весы")) return "весы";
        if (lower.contains("термостат")) return "термостат";
        if (lower.contains("центрифуга")) return "центрифуга";
        if (lower.contains("микроскоп")) return "микроскоп";
        if (lower.contains("автоклав")) return "автоклав";
        if (lower.contains("шкаф")) return "шкаф";
        return null;
    }

    private static Double getDouble(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble(((String) v).replace(",", ".")); } catch (Exception e) { return null; }
        }
        return null;
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (Exception e) { return null; }
        }
        return null;
    }

    private static double parseDouble(String s) {
        return Double.parseDouble(s.replace(",", "."));
    }

    private static int parseInt(String s) {
        return Integer.parseInt(s);
    }

    private static JsonNode resolveFieldNode(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        JsonNode fieldNode = node.get(field);
        if (fieldNode.isObject() && fieldNode.has("value") && !fieldNode.get("value").isNull()) {
            return fieldNode.get("value");
        }
        return fieldNode;
    }

    private static void extractWrappedField(JsonNode node, Map<String, Object> params, String field, java.util.function.Function<JsonNode, Object> extractor) {
        JsonNode valueNode = resolveFieldNode(node, field);
        if (valueNode != null) {
            try {
                params.put(field, extractor.apply(valueNode));
            } catch (Exception e) {
                // ignore malformed field
            }
        }
    }

    private static void extractField(JsonNode node, Map<String, Object> params, String field, java.util.function.Function<JsonNode, Object> extractor) {
        if (node.has(field) && !node.get(field).isNull()) {
            try {
                params.put(field, extractor.apply(node.get(field)));
            } catch (Exception e) {
                // ignore malformed field
            }
        }
    }
}
