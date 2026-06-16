package com.tenderbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:qwen2.5:3b}")
    private String model;

    @Value("${ollama.timeout:120000}")
    private int timeout;

    public OllamaClient() {
        this.restTemplate = new RestTemplate();
        // Force UTF-8 for text responses (Ollama may not set charset)
        this.restTemplate.setMessageConverters(List.of(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(StandardCharsets.UTF_8)));
    }

    public String chat(String systemPrompt, String userPrompt) {
        String url = baseUrl + "/api/chat";

        try {
            ObjectNode request = MAPPER.createObjectNode();
            request.put("model", model);
            request.put("stream", false);
            request.put("format", "json");

            ArrayNode messages = request.putArray("messages");
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);

            ObjectNode options = request.putObject("options");
            options.put("temperature", 0.2);
            options.put("num_ctx", 4096);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

            HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);

            log.debug("Calling Ollama API at {} with model {}", url, model);
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, entity, byte[].class);

            byte[] body = response.getBody();
            if (body == null) {
                log.warn("Ollama returned empty response");
                return null;
            }

            String responseText = new String(body, StandardCharsets.UTF_8);
            JsonNode jsonResponse = MAPPER.readTree(responseText);
            JsonNode messageNode = jsonResponse.get("message");
            if (messageNode != null && messageNode.has("content")) {
                return messageNode.get("content").asText();
            }

            log.warn("Ollama response missing content: {}", responseText);
            return null;
        } catch (Exception e) {
            log.warn("Ollama API call failed: {}", e.getMessage());
            return null;
        }
    }
}
