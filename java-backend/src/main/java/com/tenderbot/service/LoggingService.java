package com.tenderbot.service;

import com.tenderbot.dto.LogDto;
import com.tenderbot.entity.LogLevel;
import com.tenderbot.entity.ProcessingLog;
import com.tenderbot.entity.Tender;
import com.tenderbot.repository.ProcessingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoggingService {

    private final ProcessingLogRepository logRepository;

    public LoggingService(ProcessingLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional
    public void info(Tender tender, String step, String message) {
        saveLog(tender, step, message, LogLevel.INFO, null);
    }

    @Transactional
    public void info(Tender tender, String step, String message, String details) {
        saveLog(tender, step, message, LogLevel.INFO, details);
    }

    @Transactional
    public void debug(Tender tender, String step, String message) {
        saveLog(tender, step, message, LogLevel.DEBUG, null);
    }

    @Transactional
    public void success(Tender tender, String step, String message) {
        saveLog(tender, step, message, LogLevel.SUCCESS, null);
    }

    @Transactional
    public void warning(Tender tender, String step, String message) {
        saveLog(tender, step, message, LogLevel.WARNING, null);
    }

    @Transactional
    public void error(Tender tender, String step, String message, Throwable throwable) {
        String details = throwable != null ? throwable.getMessage() : null;
        saveLog(tender, step, message, LogLevel.ERROR, details);
    }

    @Transactional
    public void error(Tender tender, String step, String message) {
        saveLog(tender, step, message, LogLevel.ERROR, null);
    }

    private void saveLog(Tender tender, String step, String message, LogLevel level, String details) {
        try {
            ProcessingLog processingLog = new ProcessingLog();
            processingLog.setTender(tender);
            processingLog.setStep(step);
            processingLog.setMessage(message);
            processingLog.setLevel(level);
            processingLog.setDetails(details);
            logRepository.save(processingLog);

            String prefix = tender != null ? tender.getTenderNumber() : "N/A";
            switch (level) {
                case ERROR -> System.err.println("[" + step + "] " + prefix + ": " + message);
                case WARNING -> System.out.println("[WARN] [" + step + "] " + prefix + ": " + message);
                case SUCCESS -> System.out.println("[INFO] [" + step + "] " + prefix + ": " + message);
                default -> System.out.println("[INFO] [" + step + "] " + prefix + ": " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to save processing log: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<LogDto> getLogsByTenderId(Long tenderId) {
        return logRepository.findByTenderIdOrderByCreatedAtDesc(tenderId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LogDto> getRecentLogs() {
        return logRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    private LogDto toDto(ProcessingLog pl) {
        return new LogDto(
                pl.getId(),
                pl.getTender() != null ? pl.getTender().getId() : null,
                pl.getTender() != null ? pl.getTender().getTenderNumber() : null,
                pl.getStep(),
                pl.getMessage(),
                pl.getLevel(),
                pl.getDetails(),
                pl.getCreatedAt()
        );
    }
}
