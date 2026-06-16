package com.tenderbot.dto;

public record DashboardStatsDto(
    long totalTenders,
    long newTenders,
    long processingTenders,
    long completedTenders,
    long errorTenders,
    long totalEmailsSent,
    long pendingEmails,
    long totalItems,
    long itemsFound,
    long itemsNotFound
) {}
