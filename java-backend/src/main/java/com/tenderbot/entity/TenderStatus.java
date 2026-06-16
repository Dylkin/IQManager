package com.tenderbot.entity;

public enum TenderStatus {
    NEW,
    PARSING,
    PARSED,
    SEARCHING_SUPPLIERS,
    SUPPLIERS_FOUND,
    DOWNLOADING_DOCUMENTS,
    DOCUMENTS_ANALYZED,
    MODEL_FOUND,
    EMAIL_SENT,
    ERROR,
    COMPLETED
}
