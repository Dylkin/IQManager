package com.tenderbot.service;

import com.tenderbot.dto.FoundModelEmailDto;
import com.tenderbot.entity.*;
import com.tenderbot.repository.FoundModelEmailRepository;
import com.tenderbot.repository.FoundModelRepository;
import com.tenderbot.repository.SupplierRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FoundModelEmailService {

    private static final Pattern FOUND_MODEL_MARKER = Pattern.compile("\\[FM-(\\d+)\\]");
    private static final String APP_MARKER = "[TenderBot]";

    private final ConfigService configService;
    private final FoundModelEmailRepository foundModelEmailRepository;
    private final FoundModelRepository foundModelRepository;
    private final SupplierRepository supplierRepository;
    private final LoggingService loggingService;

    public FoundModelEmailService(ConfigService configService,
                                  FoundModelEmailRepository foundModelEmailRepository,
                                  FoundModelRepository foundModelRepository,
                                  SupplierRepository supplierRepository,
                                  LoggingService loggingService) {
        this.configService = configService;
        this.foundModelEmailRepository = foundModelEmailRepository;
        this.foundModelRepository = foundModelRepository;
        this.supplierRepository = supplierRepository;
        this.loggingService = loggingService;
    }

    @Transactional(readOnly = true)
    public List<FoundModelEmailDto> getEmailsForFoundModel(Long foundModelId) {
        return foundModelEmailRepository.findByFoundModelIdOrderByCreatedAtDesc(foundModelId).stream()
                .map(FoundModelEmailDto::fromEntity)
                .toList();
    }

    @Transactional
    public FoundModelEmailDto sendEmail(Long foundModelId, String toEmail, String subject, String body, Tender tender, TenderItem item) {
        FoundModel foundModel = foundModelRepository.findById(foundModelId)
                .orElseThrow(() -> new IllegalArgumentException("Found model not found: " + foundModelId));

        String fromEmail = getConfig("spring.mail.username", "");
        String mailPassword = getConfig("spring.mail.password", "");
        String mailHost = getConfig("spring.mail.host", "smtp.gmail.com");
        int smtpPort = parsePort(getConfig("spring.mail.smtp.port", getConfig("spring.mail.port", "587")), 587);

        String finalSubject = APP_MARKER + " [FM-" + foundModelId + "] " + subject;
        String finalBody = buildHtmlBody(body, foundModel, item, tender);

        FoundModelEmail email = new FoundModelEmail(
                foundModel, EmailDirection.OUT, finalSubject, finalBody, fromEmail, toEmail, EmailStatus.QUEUED
        );
        email = foundModelEmailRepository.save(email);

        if (fromEmail.isBlank()) {
            email.setStatus(EmailStatus.FAILED);
            email.setErrorMessage("Email отправителя не настроен. Заполните параметр spring.mail.username в разделе Переменные окружения → Email.");
            email = foundModelEmailRepository.save(email);
            loggingService.error(tender, "EMAIL", "Email отправителя не настроен (spring.mail.username)", null);
            return FoundModelEmailDto.fromEntity(email);
        }

        if (toEmail == null || toEmail.isBlank()) {
            email.setStatus(EmailStatus.FAILED);
            email.setErrorMessage("Email получателя не указан.");
            email = foundModelEmailRepository.save(email);
            loggingService.error(tender, "EMAIL", "Email получателя не указан для модели " + foundModel.getProductName(), null);
            return FoundModelEmailDto.fromEntity(email);
        }

        try {
            new InternetAddress(fromEmail).validate();
            new InternetAddress(toEmail).validate();

            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(mailHost);
            sender.setPort(smtpPort);
            sender.setUsername(fromEmail);
            sender.setPassword(mailPassword);

            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            if (smtpPort == 587) {
                props.put("mail.smtp.starttls.enable", "true");
            } else if (smtpPort == 465 || smtpPort == 25) {
                props.put("mail.smtp.ssl.enable", "true");
            }

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(finalSubject);
            helper.setText(finalBody, true);
            sender.send(message);

            email.setStatus(EmailStatus.SENT);
            email.setMessageId(message.getMessageID());
            email = foundModelEmailRepository.save(email);
            loggingService.success(tender, "EMAIL", "Email отправлен поставщику " + toEmail + " по модели " + foundModel.getProductName());
        } catch (Exception e) {
            email.setStatus(EmailStatus.FAILED);
            email.setErrorMessage(e.getMessage());
            email = foundModelEmailRepository.save(email);
            loggingService.error(tender, "EMAIL", "Ошибка отправки email: " + e.getMessage(), e);
        }

        return FoundModelEmailDto.fromEntity(email);
    }

    @Transactional
    public int fetchIncomingEmails() {
        String fromEmail = getConfig("spring.mail.username", "");
        String mailPassword = getConfig("spring.mail.password", "");
        String mailHost = getConfig("spring.mail.host", "smtp.gmail.com");
        int imapPort = parsePort(getConfig("spring.mail.imap.port", "993"), 993);

        if (fromEmail.isBlank() || mailPassword.isBlank()) {
            loggingService.warning(null, "EMAIL", "IMAP не настроен: отсутствует username/password");
            return 0;
        }

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", mailHost);
        props.setProperty("mail.imaps.port", String.valueOf(imapPort));
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.ssl.trust", "*");

        Session session = Session.getInstance(props, null);
        int savedCount = 0;

        try (Store store = session.getStore("imaps")) {
            store.connect(mailHost, fromEmail, mailPassword);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            for (Message message : messages) {
                try {
                    String subject = message.getSubject();
                    if (subject == null || !subject.contains(APP_MARKER)) {
                        continue;
                    }
                    String messageId = getMessageId(message);
                    if (messageId != null && foundModelEmailRepository.existsByMessageId(messageId)) {
                        continue;
                    }
                    Matcher matcher = FOUND_MODEL_MARKER.matcher(subject);
                    if (!matcher.find()) {
                        continue;
                    }
                    Long foundModelId = Long.valueOf(matcher.group(1));
                    Optional<FoundModel> foundModelOpt = foundModelRepository.findById(foundModelId);
                    if (foundModelOpt.isEmpty()) {
                        continue;
                    }
                    FoundModel foundModel = foundModelOpt.get();
                    String from = Arrays.stream(message.getFrom()).findFirst().map(addr -> addr.toString()).orElse("");
                    String body = extractText(message);

                    FoundModelEmail email = new FoundModelEmail(
                            foundModel, EmailDirection.IN, subject, body, from, fromEmail, EmailStatus.RECEIVED, messageId
                    );
                    foundModelEmailRepository.save(email);
                    savedCount++;
                } catch (Exception e) {
                    loggingService.warning(null, "EMAIL", "Ошибка чтения входящего письма: " + e.getMessage());
                }
            }
            inbox.close(false);
        } catch (Exception e) {
            loggingService.error(null, "EMAIL", "Ошибка подключения к IMAP: " + e.getMessage(), e);
        }

        return savedCount;
    }

    @Transactional(readOnly = true)
    public String resolveSupplierEmail(FoundModel foundModel) {
        if (foundModel == null || foundModel.getSupplierSite() == null) {
            return null;
        }
        String site = foundModel.getSupplierSite().trim().toLowerCase();
        return supplierRepository.findAll().stream()
                .filter(s -> s.getSiteUrl() != null && normalizeHost(s.getSiteUrl()).equals(normalizeHost(site)))
                .findFirst()
                .map(Supplier::getEmail)
                .orElse(null);
    }

    private String getConfig(String key, String defaultValue) {
        String value = configService.getString(key, defaultValue);
        return value != null ? value : defaultValue;
    }

    private int parsePort(String value, int defaultPort) {
        if (value == null || value.isBlank()) return defaultPort;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }

    private String normalizeHost(String site) {
        String url = site.trim().toLowerCase();
        url = url.replace("http://", "").replace("https://", "");
        url = url.replace("www.", "");
        int idx = url.indexOf('/');
        if (idx != -1) url = url.substring(0, idx);
        return url;
    }

    private String buildHtmlBody(String body, FoundModel foundModel, TenderItem item, Tender tender) {
        StringBuilder b = new StringBuilder();
        b.append("<html><body style='font-family:Arial,sans-serif;'>")
         .append("<p>").append(esc(body).replace("\n", "<br>")).append("</p>")
         .append("<hr><h3>Информация о запрашиваемой модели</h3>")
         .append("<table style='border-collapse:collapse;width:100%;margin:20px 0;'>");
        if (foundModel.getProductUrl() != null) b.append(rawRow("Ссылка", "<a href='" + foundModel.getProductUrl() + "'>" + foundModel.getProductUrl() + "</a>"));
        if (item != null && item.getQuantity() != null) b.append(row("Количество", item.getQuantity() + " " + (item.getUnit() != null ? item.getUnit() : "шт")));
        b.append("</table>")
         .append("<p><b>Если данная модель не доступна, предложите, пожалуйста, аналог.</b></p>")
         .append("</body></html>");
        return b.toString();
    }

    private String row(String label, String value) {
        return "<tr><td style='border:1px solid #ddd;padding:12px'><b>" + esc(label) + "</b></td><td style='border:1px solid #ddd;padding:12px'>" + esc(value) + "</td></tr>";
    }

    private String rawRow(String label, String rawHtml) {
        return "<tr><td style='border:1px solid #ddd;padding:12px'><b>" + esc(label) + "</b></td><td style='border:1px solid #ddd;padding:12px'>" + rawHtml + "</td></tr>";
    }

    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] headers = message.getHeader("Message-ID");
        return headers != null && headers.length > 0 ? headers[0] : null;
    }

    private String extractText(Message message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                    sb.append(part.getContent().toString());
                }
            }
            return sb.toString();
        }
        return "";
    }
}
