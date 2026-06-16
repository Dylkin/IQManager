package com.tenderbot.repository;

import com.tenderbot.entity.TelegramMessage;
import com.tenderbot.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {
    Optional<TelegramMessage> findByMessageId(Integer messageId);
    List<TelegramMessage> findByStatus(MessageStatus status);
    List<TelegramMessage> findTop50ByOrderByCreatedAtDesc();

    @Query("SELECT tm FROM TelegramMessage tm WHERE tm.channelId = :channelId AND tm.createdAt >= :startOfDay AND tm.createdAt < :endOfDay ORDER BY tm.createdAt DESC")
    List<TelegramMessage> findByChannelIdAndDate(
        @Param("channelId") Long channelId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("SELECT tm FROM TelegramMessage tm WHERE tm.channelId = :channelId ORDER BY tm.createdAt DESC")
    List<TelegramMessage> findByChannelId(@Param("channelId") Long channelId);

    boolean existsByMessageId(Integer messageId);
}
