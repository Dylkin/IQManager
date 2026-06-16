package com.tenderbot.controller;

import com.tenderbot.entity.TelegramMessage;
import com.tenderbot.entity.MessageStatus;
import com.tenderbot.repository.TelegramMessageRepository;
import com.tenderbot.repository.TenderRepository;
import com.tenderbot.service.TelegramFetchService;
import com.tenderbot.service.TenderParserService;
import com.tenderbot.telegram.TenderTelegramBot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private static final String STATUS_KEY = "status";

    private final TelegramFetchService telegramFetchService;
    private final TelegramMessageRepository telegramMessageRepository;
    private final TenderParserService tenderParserService;
    private final TenderTelegramBot tenderTelegramBot;
    private final TenderRepository tenderRepository;

    public TelegramController(TelegramFetchService telegramFetchService,
                              TelegramMessageRepository telegramMessageRepository,
                              TenderParserService tenderParserService,
                              TenderTelegramBot tenderTelegramBot,
                              TenderRepository tenderRepository) {
        this.telegramFetchService = telegramFetchService;
        this.telegramMessageRepository = telegramMessageRepository;
        this.tenderParserService = tenderParserService;
        this.tenderTelegramBot = tenderTelegramBot;
        this.tenderRepository = tenderRepository;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<TelegramMessage>> getMessages() {
        return ResponseEntity.ok(telegramMessageRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @PostMapping("/fetch-today")
    public ResponseEntity<List<TelegramMessage>> fetchTodayMessages() {
        List<TelegramMessage> messages = telegramFetchService.fetchTodayMessages();
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/reset-tender")
    public ResponseEntity<Map<String, String>> resetTender(@RequestBody Map<String, String> body) {
        Map<String, String> response = new HashMap<>();
        String url = body.get("url");
        if (url == null || url.isEmpty()) {
            response.put(STATUS_KEY, "URL required");
            return ResponseEntity.badRequest().body(response);
        }
        var tenderOpt = tenderRepository.findByTenderNumber(url);
        if (tenderOpt.isEmpty()) {
            // try find by url
            var all = tenderRepository.findAll();
            for (var t : all) {
                if (url.equals(t.getUrl())) {
                    tenderRepository.delete(t);
                    response.put(STATUS_KEY, "Tender deleted");
                    response.put("id", String.valueOf(t.getId()));
                    return ResponseEntity.ok(response);
                }
            }
            response.put(STATUS_KEY, "Tender not found");
            return ResponseEntity.notFound().build();
        }
        tenderRepository.delete(tenderOpt.get());
        response.put(STATUS_KEY, "Tender deleted");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-message/{messageId}")
    public ResponseEntity<Map<String, String>> processMessage(@PathVariable Integer messageId) {
        Map<String, String> response = new HashMap<>();
        Optional<TelegramMessage> msgOpt = telegramMessageRepository.findByMessageId(messageId);
        if (msgOpt.isEmpty()) {
            response.put(STATUS_KEY, "Message not found");
            return ResponseEntity.notFound().build();
        }
        TelegramMessage msg = msgOpt.get();
        if (msg.getExtractedUrl() == null || msg.getExtractedUrl().isEmpty()) {
            response.put(STATUS_KEY, "No URL in message");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            if (tenderParserService.isTenderAlreadyProcessed(msg.getExtractedUrl())) {
                response.put(STATUS_KEY, "Tender already processed");
                return ResponseEntity.ok(response);
            }
            var tender = tenderParserService.parseTenderFromUrl(msg.getExtractedUrl());
            if (tender != null && tender.getId() != null) {
                tenderTelegramBot.processTenderAsync(tender.getId());
                msg.setStatus(MessageStatus.PROCESSED);
                telegramMessageRepository.save(msg);
                response.put(STATUS_KEY, "Tender parsed and queued for supplier search");
                response.put("tenderId", String.valueOf(tender.getId()));
                response.put("url", msg.getExtractedUrl());
            } else {
                response.put(STATUS_KEY, "Failed to parse tender");
            }
        } catch (Exception e) {
            response.put(STATUS_KEY, "Error: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
