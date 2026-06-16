package com.tenderbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenderbot.entity.*;
import com.tenderbot.entity.Tender;
import com.tenderbot.repository.ConfigRepository;
import com.tenderbot.repository.TelegramMessageRepository;
import com.tenderbot.telegram.TenderTelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramFetchService {

    private static final Logger log = LoggerFactory.getLogger(TelegramFetchService.class);
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"{}|\\^`\\[\\]]+");
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final ConfigRepository configRepository;
    private final TelegramMessageRepository telegramMessageRepository;
    private final TenderParserService tenderParserService;
    private final TenderTelegramBot tenderTelegramBot;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TelegramFetchService(ConfigRepository configRepository,
                                TelegramMessageRepository telegramMessageRepository,
                                TenderParserService tenderParserService,
                                TenderTelegramBot tenderTelegramBot) {
        this.configRepository = configRepository;
        this.telegramMessageRepository = telegramMessageRepository;
        this.tenderParserService = tenderParserService;
        this.tenderTelegramBot = tenderTelegramBot;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    private static final int FETCH_LOOKBACK_DAYS = 7;

    @Transactional
    public List<TelegramMessage> fetchTodayMessages() {
        String token = getConfigValue("telegram.bot.token");
        String channelIdStr = getConfigValue("telegram.channel.id");

        if (token == null || token.isEmpty() || token.equals("test")) {
            log.warn("Telegram bot token not configured");
            return List.of();
        }
        if (channelIdStr == null || channelIdStr.isEmpty()) {
            log.warn("Telegram channel ID not configured");
            return List.of();
        }

        Long channelId;
        try {
            channelId = Long.parseLong(channelIdStr);
        } catch (NumberFormatException e) {
            log.error("Invalid channel ID format: {}", channelIdStr);
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate sinceDate = today.minusDays(FETCH_LOOKBACK_DAYS - 1);
        log.info("Fetching messages from channel {} (API lookback: {} days)",
                channelId, FETCH_LOOKBACK_DAYS);

        // Fetch from Telegram API recent messages not yet consumed by the polling bot
        List<TelegramMessage> newMessages = fetchFromTelegramApi(token, channelId, sinceDate);

        // Read all saved messages from DB for this channel
        List<TelegramMessage> dbMessages = telegramMessageRepository.findByChannelId(channelId);

        List<TelegramMessage> result = new ArrayList<>(dbMessages);
        // Add new messages that are not already in DB
        for (TelegramMessage msg : newMessages) {
            boolean exists = dbMessages.stream().anyMatch(m -> m.getMessageId().equals(msg.getMessageId()));
            if (!exists) {
                result.add(msg);
            }
        }

        log.info("Total messages for channel {}: {} ({} new from API, {} from DB)",
                channelId, result.size(), newMessages.size(), dbMessages.size());

        return result;
    }

    private List<TelegramMessage> fetchFromTelegramApi(String token, Long channelId, LocalDate sinceDate) {
        List<TelegramMessage> messages = new ArrayList<>();

        try {
            String url = TELEGRAM_API_BASE + token + "/getUpdates?limit=100";
            log.info("Telegram API request: {}/getUpdates?limit=100", TELEGRAM_API_BASE + token.substring(0, Math.min(token.length(), 8)) + "...");
            String response = restTemplate.getForObject(url, String.class);
            log.debug("Telegram API raw response: {}", response);

            if (response == null) {
                log.warn("Empty response from Telegram API");
                return messages;
            }

            JsonNode root = objectMapper.readTree(response);
            if (!root.path("ok").asBoolean()) {
                log.error("Telegram API error: {}", root.path("description").asText());
                return messages;
            }

            JsonNode updates = root.path("result");
            if (!updates.isArray()) {
                log.warn("Telegram API result is not an array");
                return messages;
            }

            log.info("Telegram API returned {} updates", updates.size());

            for (JsonNode update : updates) {
                JsonNode messageNode = update.path("channel_post");
                if (messageNode.isMissingNode()) {
                    messageNode = update.path("message");
                }
                if (messageNode.isMissingNode()) {
                    log.debug("Update without channel_post/message skipped");
                    continue;
                }

                Long msgChatId = messageNode.path("chat").path("id").asLong();
                log.debug("Checking message chat_id={} against channelId={}", msgChatId, channelId);
                // Support both plain channel ID and -100 prefixed supergroup/channel ID
                if (!msgChatId.equals(channelId) && !msgChatId.toString().equals("-100" + channelId)) {
                    log.debug("Message chat_id {} does not match channelId {} or -100{}", msgChatId, channelId, channelId);
                    continue;
                }

                // Check date
                long dateUnix = messageNode.path("date").asLong();
                LocalDateTime msgDate = LocalDateTime.ofEpochSecond(dateUnix, 0, ZoneOffset.UTC);
                if (msgDate.toLocalDate().isBefore(sinceDate)) {
                    log.debug("Message date {} is before window start {}", msgDate.toLocalDate(), sinceDate);
                    continue;
                }

                Integer messageId = messageNode.path("message_id").asInt();
                String text = messageNode.path("text").asText();
                String sender = messageNode.path("from").path("username").asText();

                if (text == null || text.isEmpty()) {
                    log.debug("Message {} has empty text, skipping", messageId);
                    continue;
                }

                // Extract URL from entities or text
                String extractedUrl = extractUrlFromMessage(messageNode, text);

                // Check if already exists
                if (telegramMessageRepository.existsByMessageId(messageId)) {
                    log.debug("Message {} already exists in DB, skipping", messageId);
                    continue;
                }

                TelegramMessage telegramMessage = new TelegramMessage();
                telegramMessage.setMessageId(messageId);
                telegramMessage.setChannelId(channelId);
                telegramMessage.setText(text);
                telegramMessage.setSender(sender);
                telegramMessage.setHasLink(extractedUrl != null);
                telegramMessage.setExtractedUrl(extractedUrl);
                telegramMessage.setStatus(extractedUrl != null ? MessageStatus.NEW : MessageStatus.SKIPPED);

                telegramMessageRepository.save(telegramMessage);
                messages.add(telegramMessage);

                log.info("Saved message {} from chat {} with date {}", messageId, msgChatId, msgDate);
                if (extractedUrl != null) {
                    log.info("Found tender link in message {}: {}", messageId, extractedUrl);
                    try {
                        if (!tenderParserService.isTenderAlreadyProcessed(extractedUrl)) {
                            Tender tender = tenderParserService.parseTenderFromUrl(extractedUrl);
                            if (tender != null && tender.getId() != null) {
                                log.info("Parsed tender {} from URL {}, starting async processing", tender.getId(), extractedUrl);
                                tenderTelegramBot.processTenderAsync(tender.getId());
                            }
                        } else {
                            log.info("Tender from URL {} already processed", extractedUrl);
                        }
                    } catch (Exception e) {
                        log.error("Error parsing tender from URL {}: {}", extractedUrl, e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error fetching from Telegram API", e);
        }

        log.info("fetchFromTelegramApi completed with {} new messages", messages.size());
        return messages;
    }

    private String getConfigValue(String key) {
        return configRepository.findByKey(key)
            .map(Config::getValue)
            .orElse("");
    }

    private String extractUrlFromMessage(JsonNode messageNode, String text) {
        JsonNode entities = messageNode.path("entities");
        if (entities.isArray()) {
            for (JsonNode entity : entities) {
                String type = entity.path("type").asText();
                if ("text_link".equals(type) || "url".equals(type)) {
                    String url = entity.path("url").asText();
                    if (url != null && !url.isEmpty()) {
                        return url;
                    }
                }
            }
        }
        if (text != null) {
            Matcher matcher = URL_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }
}
