package com.tenderbot.controller;

import com.tenderbot.dto.DashboardStatsDto;
import com.tenderbot.entity.*;
import com.tenderbot.repository.*;
import com.tenderbot.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")

public class DashboardController {

    private final TenderRepository tenderRepository;
    private final TenderItemRepository tenderItemRepository;
    private final EmailLogRepository emailLogRepository;
    private final LoggingService loggingService;
    private final EmailService emailService;

    public DashboardController(TenderRepository tenderRepository, TenderItemRepository tenderItemRepository,
                               EmailLogRepository emailLogRepository, LoggingService loggingService,
                               EmailService emailService) {
        this.tenderRepository = tenderRepository;
        this.tenderItemRepository = tenderItemRepository;
        this.emailLogRepository = emailLogRepository;
        this.loggingService = loggingService;
        this.emailService = emailService;
    }

    @GetMapping("/stats")
    public DashboardStatsDto getStats() {
        long total = tenderRepository.count();
        long completed = tenderRepository.countByStatus(TenderStatus.COMPLETED);
        long processing = tenderRepository.countByStatus(TenderStatus.PARSING) + tenderRepository.countByStatus(TenderStatus.SEARCHING_SUPPLIERS) + tenderRepository.countByStatus(TenderStatus.DOWNLOADING_DOCUMENTS);
        long errors = tenderRepository.countByStatus(TenderStatus.ERROR);
        long emailsSent = emailLogRepository.findByStatus(EmailStatus.SENT).size();
        long pendingEmails = emailLogRepository.findByStatus(EmailStatus.QUEUED).size();
        List<TenderItem> allItems = tenderItemRepository.findAll();
        long itemsFound = allItems.stream().filter(i -> i.getStatus() == ItemStatus.FOUND_ON_SUPPLIER || i.getStatus() == ItemStatus.MODEL_MATCHED).count();
        long itemsNotFound = allItems.stream().filter(i -> i.getStatus() == ItemStatus.NOT_FOUND).count();
        return new DashboardStatsDto(total, tenderRepository.countByStatus(TenderStatus.NEW), processing, completed, errors, emailsSent, pendingEmails, allItems.size(), itemsFound, itemsNotFound);
    }

    @GetMapping("/logs")
    public List<com.tenderbot.dto.LogDto> getRecentLogs() {
        return loggingService.getRecentLogs();
    }

    @GetMapping("/emails")
    public ResponseEntity<List<EmailLog>> getEmailLogs() {
        return ResponseEntity.ok(emailService.getEmailLogs());
    }

    @GetMapping("/status-distribution")
    public Map<String, Long> getStatusDistribution() {
        return tenderRepository.findAll().stream().collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));
    }
}
