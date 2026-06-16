package com.tenderbot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "found_model_emails")
public class FoundModelEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_model_id")
    private FoundModel foundModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private EmailDirection direction;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body", length = 8000)
    private String body;

    @Column(name = "from_email")
    private String fromEmail;

    @Column(name = "to_email")
    private String toEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EmailStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "message_id")
    private String messageId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public FoundModelEmail() {}

    public FoundModelEmail(FoundModel foundModel, EmailDirection direction, String subject, String body,
                           String fromEmail, String toEmail, EmailStatus status) {
        this(foundModel, direction, subject, body, fromEmail, toEmail, status, null);
    }

    public FoundModelEmail(FoundModel foundModel, EmailDirection direction, String subject, String body,
                           String fromEmail, String toEmail, EmailStatus status, String messageId) {
        this.foundModel = foundModel;
        this.direction = direction;
        this.subject = subject;
        this.body = body;
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
        this.status = status;
        this.messageId = messageId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FoundModel getFoundModel() { return foundModel; }
    public void setFoundModel(FoundModel foundModel) { this.foundModel = foundModel; }

    public EmailDirection getDirection() { return direction; }
    public void setDirection(EmailDirection direction) { this.direction = direction; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }

    public EmailStatus getStatus() { return status; }
    public void setStatus(EmailStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
