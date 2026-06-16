package com.tenderbot.telegram;

import com.tenderbot.entity.*;
import com.tenderbot.entity.Tender;
import com.tenderbot.entity.TenderStatus;
import com.tenderbot.repository.ConfigRepository;
import com.tenderbot.repository.TelegramMessageRepository;
import com.tenderbot.repository.TenderRepository;
import com.tenderbot.service.DocumentService;
import com.tenderbot.service.SupplierSearchService;
import com.tenderbot.service.TenderParserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TenderTelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TenderTelegramBot.class);

    private final TelegramMessageRepository messageRepository;
    private final TenderParserService tenderParserService;
    private final TenderRepository tenderRepository;
    private final SupplierSearchService supplierSearchService;
    private final DocumentService documentService;

    private final ConfigRepository configRepository;

    private String channelId;

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"{}|\\^`\\[\\]]+");

    public TenderTelegramBot(TelegramMessageRepository messageRepository,
                             TenderParserService tenderParserService,
                             TenderRepository tenderRepository,
                             SupplierSearchService supplierSearchService,
                             DocumentService documentService,
                             ConfigRepository configRepository) {
        this.messageRepository = messageRepository;
        this.tenderParserService = tenderParserService;
        this.tenderRepository = tenderRepository;
        this.supplierSearchService = supplierSearchService;
        this.documentService = documentService;
        this.configRepository = configRepository;
    }

    @Override
    public String getBotUsername() {
        return configRepository.findByKey("telegram.bot.username")
                .map(com.tenderbot.entity.Config::getValue)
                .orElse("");
    }

    @Override
    public String getBotToken() {
        return configRepository.findByKey("telegram.bot.token")
                .map(com.tenderbot.entity.Config::getValue)
                .orElse("");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            log.debug("Update without message received, skipping");
            return;
        }
        Message message = update.getMessage();
        String text = message.getText();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();

        log.info("Received message {} from chat {}: {}", messageId, chatId, text != null ? text.substring(0, Math.min(text.length(), 50)) : "null");

        String configuredChannelId = getChannelId();
        String chatIdStr = String.valueOf(chatId);
        if (configuredChannelId != null && !configuredChannelId.isEmpty()
                && !configuredChannelId.equals(chatIdStr)
                && !chatIdStr.equals("-100" + configuredChannelId)) {
            log.debug("Message chat_id {} does not match channelId {} or -100{}", chatId, configuredChannelId, configuredChannelId);
            return;
        }

        if (messageRepository.findByMessageId(messageId).isPresent()) {
            log.debug("Message {} already exists in DB, skipping", messageId);
            return;
        }

        String extractedUrl = extractUrlFromMessage(message);

        TelegramMessage tm = new TelegramMessage();
        tm.setMessageId(messageId);
        tm.setChannelId(chatId);
        tm.setText(text);
        tm.setSender(message.getFrom() != null ? message.getFrom().getUserName() : null);
        tm.setHasLink(extractedUrl != null);
        tm.setExtractedUrl(extractedUrl);
        tm.setStatus(extractedUrl != null ? MessageStatus.PROCESSED : MessageStatus.SKIPPED);

        if (extractedUrl != null) {
            processTenderUrl(extractedUrl);
        }
        messageRepository.save(tm);
        log.info("Saved message {} from chat {} with status {}", messageId, chatId, tm.getStatus());
    }

    private String extractUrlFromMessage(Message message) {
        if (message.hasEntities()) {
            for (MessageEntity entity : message.getEntities()) {
                if ("text_link".equals(entity.getType()) || "url".equals(entity.getType())) {
                    String url = entity.getUrl();
                    if (url != null && !url.isEmpty()) {
                        return url;
                    }
                }
            }
        }
        String text = message.getText();
        if (text != null) {
            Matcher matcher = URL_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private void processTenderUrl(String url) {
        try {
            if (tenderParserService.isTenderAlreadyProcessed(url)) {
                log.info("Tender from URL already processed: {}", url);
                return;
            }
            Tender tender = tenderParserService.parseTenderFromUrl(url);
            if (tender != null && tender.getId() != null) {
                log.info("Parsed tender {}, starting async processing", tender.getId());
                processTenderAsync(tender.getId());
            }
        } catch (Exception e) {
            log.error("Error processing URL: {}", url, e);
        }
    }

    @Async
    public void processTenderAsync(Long tenderId) {
        try {
            Thread.sleep(1000);
            Tender tender = tenderRepository.findByIdWithItems(tenderId).orElse(null);
            if (tender == null) {
                log.warn("Tender {} not found for async processing", tenderId);
                return;
            }
            log.info("Starting supplier search for tender {}", tenderId);
            supplierSearchService.searchSuppliersForTender(tender);

            tender = tenderRepository.findByIdWithItems(tenderId).orElse(null);
            if (tender != null && tender.getStatus() == TenderStatus.SUPPLIERS_FOUND) {
                log.info("Suppliers found for tender {}, starting document download", tenderId);
                documentService.downloadAndAnalyzeDocuments(tender);
            } else {
                log.info("No suppliers found for tender {}, status: {}", tenderId,
                        tender != null ? tender.getStatus() : "null");
            }
        } catch (Exception e) {
            log.error("Error in async processing for tender {}", tenderId, e);
        }
    }

    public void sendNotification(String chatId, String message) {
        try {
            SendMessage sm = new SendMessage();
            sm.setChatId(chatId);
            sm.setText(message);
            execute(sm);
        } catch (TelegramApiException e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    public void sendErrorNotification(String errorMessage) {
        String configuredChannelId = getChannelId();
        if (configuredChannelId != null && !configuredChannelId.isEmpty()) sendNotification(configuredChannelId, "\u26A0\uFE0F Ошибка в TenderBot: " + errorMessage);
    }

    private String getChannelId() {
        return configRepository.findByKey("telegram.channel.id")
                .map(com.tenderbot.entity.Config::getValue)
                .orElse("");
    }
}
