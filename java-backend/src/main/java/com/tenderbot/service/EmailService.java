package com.tenderbot.service;

import com.tenderbot.entity.*;
import com.tenderbot.repository.EmailLogRepository;
import com.tenderbot.repository.SupplierRepository;
import com.tenderbot.repository.TenderItemRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final TenderItemRepository tenderItemRepository;
    private final SupplierRepository supplierRepository;
    private final LoggingService loggingService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, EmailLogRepository emailLogRepository,
                        TenderItemRepository tenderItemRepository, SupplierRepository supplierRepository,
                        LoggingService loggingService) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.tenderItemRepository = tenderItemRepository;
        this.supplierRepository = supplierRepository;
        this.loggingService = loggingService;
    }

    @Transactional
    public void sendRequestToSupplier(Tender tender, TenderItem item) {
        loggingService.info(tender, "EMAIL", "Подготовка email для лота " + item.getLotNumber());
        Supplier supplier = findSupplierForItem(item);
        if (supplier == null || supplier.getEmail() == null || supplier.getEmail().isEmpty()) {
            loggingService.warning(tender, "EMAIL", "Email поставщика не найден для " + item.getSupplierSite());
            return;
        }

        String subject = buildEmailSubject(tender, item);
        String body = buildEmailBody(tender, item);

        EmailLog emailLog = new EmailLog();
        emailLog.setTenderItem(item);
        emailLog.setSupplierEmail(supplier.getEmail());
        emailLog.setSubject(subject);
        emailLog.setBody(body);
        emailLog.setStatus(EmailStatus.QUEUED);
        emailLog = emailLogRepository.save(emailLog);

        try {
            sendEmail(supplier.getEmail(), subject, body);
            emailLog.setStatus(EmailStatus.SENT);
            emailLog.setSentAt(LocalDateTime.now());
            emailLogRepository.save(emailLog);
            item.setStatus(ItemStatus.EMAIL_SENT);
            tenderItemRepository.save(item);
            loggingService.success(tender, "EMAIL", "Email отправлен на " + supplier.getEmail() + " для лота " + item.getLotNumber());
        } catch (Exception e) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            emailLogRepository.save(emailLog);
            loggingService.error(tender, "EMAIL", "Ошибка отправки email: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void sendBulkRequests(Tender tender) {
        loggingService.info(tender, "EMAIL", "Начало массовой отправки запросов поставщикам");
        tender.setStatus(TenderStatus.EMAIL_SENT);
        boolean anySent = false;
        for (TenderItem item : tender.getItems()) {
            if (item.getStatus() == ItemStatus.MODEL_MATCHED || item.getStatus() == ItemStatus.FOUND_ON_SUPPLIER) {
                sendRequestToSupplier(tender, item);
                anySent = true;
            }
        }
        if (anySent) {
            tender.setStatus(TenderStatus.COMPLETED);
            loggingService.success(tender, "EMAIL", "Все запросы отправлены");
        } else {
            loggingService.warning(tender, "EMAIL", "Нет товаров для отправки запросов");
        }
    }

    private void sendEmail(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }

    private Supplier findSupplierForItem(TenderItem item) {
        if (item.getSupplierSite() != null) {
            List<Supplier> suppliers = supplierRepository.findBySiteUrl(item.getSupplierSite());
            return suppliers.isEmpty() ? null : suppliers.get(0);
        }
        return null;
    }

    private String buildEmailSubject(Tender tender, TenderItem item) {
        return "Запрос счета: " + item.getDescription().substring(0, Math.min(50, item.getDescription().length())) + "...";
    }

    private String buildEmailBody(Tender tender, TenderItem item) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        StringBuilder b = new StringBuilder();
        b.append("<html><body style='font-family:Arial,sans-serif;'>")
         .append("<h2>Запрос на выставление счета</h2>")
         .append("<p>Уважаемые коллеги!</p>")
         .append("<p>Просим выставить счет на следующее оборудование/товар:</p>")
         .append("<table style='border-collapse:collapse;width:100%;margin:20px 0;'>")
         .append("<tr style='background:#f2f2f2'><th style='border:1px solid #ddd;padding:12px;text-align:left'>Параметр</th><th style='border:1px solid #ddd;padding:12px;text-align:left'>Значение</th></tr>")
         .append(row("Наименование", item.getDescription()));
        if (item.getFoundModelName() != null) b.append(row("Модель", item.getFoundModelName()));
        if (item.getFoundModelUrl() != null) b.append(row("Ссылка", "<a href='" + item.getFoundModelUrl() + "'>" + item.getFoundModelUrl() + "</a>"));
        b.append(row("Количество", (item.getQuantity() != null ? item.getQuantity() : "По спецификации") + " " + (item.getUnit() != null ? item.getUnit() : "шт")));
        if (item.getFoundModelPrice() != null) b.append(row("Цена", item.getFoundModelPrice() + " " + (item.getCurrency() != null ? item.getCurrency() : "BYN")));
        b.append(row("Тендер", tender.getTenderNumber()));
        if (tender.getOrganizer() != null) b.append(row("Заказчик", tender.getOrganizer()));
        b.append("</table>");
        if (item.getDocumentDescription() != null) {
            b.append("<h3>Доп. информация:</h3><p style='background:#f9f9f9;padding:15px;border-left:4px solid #007bff'>").append(esc(item.getDocumentDescription())).append("</p>");
        }
        b.append("<p>Просим указать: стоимость с НДС, срок поставки, условия оплаты, гарантию.</p>")
         .append("<p>С уважением,<br/>Автоматизированная система TenderBot</p>")
         .append("<hr><p style='font-size:12px;color:#666'>Дата: ").append(LocalDateTime.now().format(fmt)).append("</p>")
         .append("</body></html>");
        return b.toString();
    }

    private String row(String label, String value) {
        return "<tr><td style='border:1px solid #ddd;padding:12px'><b>" + esc(label) + "</b></td><td style='border:1px solid #ddd;padding:12px'>" + esc(value) + "</td></tr>";
    }

    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }

    @Transactional(readOnly = true)
    public List<EmailLog> getEmailLogs() {
        return emailLogRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
