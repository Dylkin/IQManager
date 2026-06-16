package com.tenderbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ParameterExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ParameterExtractionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, String> PARAM_LABELS = Map.ofEntries(
            Map.entry("equipmentType", "Тип оборудования"),
            Map.entry("model", "Модель/артикул"),
            Map.entry("manufacturer", "Производитель"),
            Map.entry("volumeL", "Объём камеры, л"),
            Map.entry("maxTempC", "Максимальная температура, °C"),
            Map.entry("tempRange", "Диапазон температур, °C"),
            Map.entry("regulatorType", "Тип терморегулятора"),
            Map.entry("accuracyPercent", "Точность, %"),
            Map.entry("thermocoupleType", "Тип термопары"),
            Map.entry("heatUpTimeMin", "Время нагрева, мин"),
            Map.entry("dimensionsMm", "Габариты, мм"),
            Map.entry("weightKg", "Вес, кг"),
            Map.entry("powerKW", "Мощность, кВт"),
            Map.entry("voltageV", "Напряжение, В"),
            Map.entry("material", "Материал камеры"),
            Map.entry("programsCount", "Количество программ"),
            Map.entry("hasExhaust", "Наличие вытяжки")
    );

    private final OllamaClient ollamaClient;
    private final EquipmentCatalogService catalogService;

    @Autowired
    public ParameterExtractionService(OllamaClient ollamaClient, EquipmentCatalogService catalogService) {
        this.ollamaClient = ollamaClient;
        this.catalogService = catalogService;
    }

    public String extractParameters(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = "Извлеки технические параметры из следующего текста тендера:\n\n" + text;

        try {
            String content = ollamaClient.chat(systemPrompt, userPrompt);
            log.info("LLM raw response: {}", content);
            if (content == null) {
                return null;
            }

            content = cleanJsonResponse(content);
            log.info("LLM cleaned response: {}", content);

            JsonNode jsonNode = MAPPER.readTree(content);
            if (!jsonNode.isObject()) {
                log.warn("LLM returned non-object JSON: {}", content);
                return null;
            }

            // Новый формат с массивом parameters
            if (jsonNode.has("parameters") && jsonNode.get("parameters").isArray()) {
                return extractParametersNewFormat(jsonNode, text);
            }

            // Обратная совместимость со старым плоским форматом
            return extractParametersLegacyFormat(jsonNode, text);
        } catch (Exception e) {
            log.warn("Parameter extraction failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractParametersNewFormat(JsonNode root, String text) {
        String equipmentType = getStringOrNull(root, "equipmentType");
        String model = getStringOrNull(root, "model");
        String manufacturer = getStringOrNull(root, "manufacturer");

        if (equipmentType == null || equipmentType.isBlank() || "null".equalsIgnoreCase(equipmentType)) {
            equipmentType = detectEquipmentTypeFallback(text);
            log.info("Fallback equipmentType detected: {}", equipmentType);
        }
        log.info("Extracted equipmentType: {}", equipmentType);

        ObjectNode result = MAPPER.createObjectNode();
        Map<String, String> labels = new HashMap<>();
        Map<String, String> specValues = new HashMap<>();

        if (equipmentType != null && !equipmentType.isBlank()) {
            result.set("equipmentType", wrap("Тип оборудования", MAPPER.valueToTree(equipmentType)));
            labels.put("equipmentType", "Тип оборудования");
        }
        if (model != null && !model.isBlank() && !"null".equalsIgnoreCase(model)) {
            result.set("model", wrap("Модель/артикул", MAPPER.valueToTree(model)));
            labels.put("model", "Модель/артикул");
        }
        if (manufacturer != null && !manufacturer.isBlank() && !"null".equalsIgnoreCase(manufacturer)) {
            result.set("manufacturer", wrap("Производитель", MAPPER.valueToTree(manufacturer)));
        }

        JsonNode params = root.get("parameters");
        if (params != null && params.isArray()) {
            for (JsonNode p : params) {
                if (!p.isObject()) continue;
                String key = getStringOrNull(p, "key");
                String label = getStringOrNull(p, "label");
                JsonNode valueNode = p.get("value");
                if (key == null || key.isBlank() || valueNode == null || valueNode.isNull()) continue;
                if (label == null || label.isBlank()) {
                    label = PARAM_LABELS.getOrDefault(key, humanizeKey(key));
                }
                result.set(key, wrap(label, valueNode));
                labels.put(key, label);
                specValues.put(key, valueNode.asText());
            }
        }

        if (equipmentType != null && catalogService != null) {
            catalogService.ensureTypeAndCharacteristics(equipmentType, labels);
            if (model != null && !model.isBlank()) {
                catalogService.saveDraftModel(equipmentType, model, specValues);
            } else {
                log.info("No model found in extracted params, skipping saveDraftModel");
            }
        }

        log.info("Successfully extracted parameters from text ({} chars)", text.length());
        return result.toString();
    }

    private String extractParametersLegacyFormat(JsonNode flatJson, String text) {
        String equipmentType = flatJson.has("equipmentType") && !flatJson.get("equipmentType").isNull()
                ? flatJson.get("equipmentType").asText() : null;
        if (equipmentType == null || equipmentType.isBlank() || "null".equalsIgnoreCase(equipmentType)) {
            equipmentType = detectEquipmentTypeFallback(text);
            log.info("Fallback equipmentType detected: {}", equipmentType);
        }
        log.info("Extracted equipmentType: {}", equipmentType);

        JsonNode wrapped = wrapWithLabels(flatJson);
        if (equipmentType != null && catalogService != null) {
            Map<String, String> labels = new HashMap<>();
            final String[] modelNameRef = { null };
            Map<String, String> specValues = new HashMap<>();
            flatJson.fields().forEachRemaining(e -> {
                if (e.getValue().isNull()) return;
                String key = e.getKey();
                labels.put(key, PARAM_LABELS.getOrDefault(key, key));
                if ("model".equals(key)) {
                    modelNameRef[0] = e.getValue().asText();
                } else if (!"equipmentType".equals(key)) {
                    specValues.put(key, e.getValue().asText());
                }
            });
            log.info("Calling ensureTypeAndCharacteristics for type: {}, model: {}", equipmentType, modelNameRef[0]);
            catalogService.ensureTypeAndCharacteristics(equipmentType, labels);
            if (modelNameRef[0] != null && !modelNameRef[0].isBlank()) {
                log.info("Calling saveDraftModel for type: {}, model: {}", equipmentType, modelNameRef[0]);
                catalogService.saveDraftModel(equipmentType, modelNameRef[0], specValues);
            } else {
                log.info("No model found in extracted params, skipping saveDraftModel");
            }
        } else {
            log.warn("No equipmentType extracted or catalogService is null");
        }
        log.info("Successfully extracted parameters from text ({} chars)", text.length());
        return wrapped.toString();
    }

    private String buildSystemPrompt() {
        return "Ты — инженер по извлечению технических параметров оборудования из текстов тендеров.\n" +
            "Извлеки из текста ВСЕ технические характеристики оборудования и верни результат СТРОГО в формате JSON.\n\n" +
            "КРИТИЧЕСКИ ВАЖНЫЕ ПРАВИЛА:\n" +
            "1. Поле equipmentType — ОБЯЗАТЕЛЬНОЕ. Определи тип оборудования из текста тендера.\n" +
            "   Примеры типов: муфельная печь, лабораторный анализатор, водяная баня, центрифуга, термостат, весы лабораторные, титратор, автоклав, инкубатор, УФ-камера.\n" +
            "   Если не уверен — используй наиболее близкий тип, но НЕ оставляй null.\n" +
            "2. Поля model и manufacturer — модель/артикул и производитель, если указаны в тексте. Иначе null.\n" +
            "3. Верни ТОЛЬКО JSON объект без markdown, без пояснений, без ```.\n" +
            "4. Не включай параметры, значение которых неизвестно (null).\n" +
            "5. Все значения в parameters должны быть строками, числами или boolean.\n" +
            "6. Ключ key пиши на английском в camelCase (например waterRange, resolution, power).\n" +
            "7. label — понятная русская подпись параметра.\n\n" +
            "Формат ответа:\n" +
            "{\n" +
            "  \"equipmentType\": \"тип оборудования\",\n" +
            "  \"model\": \"модель/артикул или null\",\n" +
            "  \"manufacturer\": \"производитель или null\",\n" +
            "  \"parameters\": [\n" +
            "    {\"key\": \"waterRange\", \"label\": \"Диапазон измерения содержания воды\", \"value\": \"от 10 мкг до 100 мг\"},\n" +
            "    {\"key\": \"resolution\", \"label\": \"Разрешающая способность\", \"value\": \"0,1 мкг\"},\n" +
            "    {\"key\": \"voltage\", \"label\": \"Напряжение питания\", \"value\": \"200-240 В, 50 Гц\"}\n" +
            "  ]\n" +
            "}\n\n" +
            "Если в тексте есть таблица технических характеристик — перенеси ВСЕ её строки в parameters.";
    }

    private String cleanJsonResponse(String content) {
        if (content == null) return null;
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    private JsonNode wrapWithLabels(JsonNode flatJson) {
        ObjectNode result = MAPPER.createObjectNode();
        flatJson.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isNull()) {
                return;
            }
            if ("additionalParams".equals(key)) {
                return;
            }
            String label = PARAM_LABELS.getOrDefault(key, key);
            result.set(key, wrap(label, value));
        });
        return result;
    }

    private ObjectNode wrap(String label, JsonNode value) {
        ObjectNode wrapped = MAPPER.createObjectNode();
        wrapped.put("label", label);
        wrapped.set("value", value);
        return wrapped;
    }

    private String getStringOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String s = value.asText().trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    private String humanizeKey(String key) {
        if (key == null || key.isBlank()) return key;
        StringBuilder sb = new StringBuilder();
        for (char c : key.toCharArray()) {
            if (Character.isUpperCase(c) && sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toLowerCase(c));
        }
        String result = sb.toString().replaceAll("[_\\-]+", " ").trim();
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    private String detectEquipmentTypeFallback(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        if (lower.contains("муфельн") || (lower.contains("печь") && lower.contains("муфель"))) return "Муфельная печь";
        if (lower.contains("печь") && lower.contains("лаборатор")) return "Лабораторная печь";
        if (lower.contains("печь")) return "Печь";
        if (lower.contains("спектрометр")) return "Спектрометр";
        if (lower.contains("анализатор") && lower.contains("газ")) return "Газовый анализатор";
        if (lower.contains("анализатор")) return "Анализатор";
        if (lower.contains("баня водяная") || lower.contains("водяная баня")) return "Водяная баня";
        if (lower.contains("центрифуга")) return "Центрифуга";
        if (lower.contains("термостат")) return "Термостат";
        if (lower.contains("весы") || lower.contains("весов")) return "Весы";
        if (lower.contains("титратор")) return "Титратор";
        if (lower.contains("автоклав")) return "Автоклав";
        if (lower.contains("инкубатор")) return "Инкубатор";
        if (lower.contains("ультрафиолет") || lower.contains("uv") || lower.contains("уф-")) return "УФ-камера";
        if (lower.contains("микроскоп")) return "Микроскоп";
        if (lower.contains("колонка") && lower.contains("хроматограф")) return "Хроматографическая колонка";
        if (lower.contains("хроматограф")) return "Хроматограф";
        return "Лабораторное оборудование";
    }
}
