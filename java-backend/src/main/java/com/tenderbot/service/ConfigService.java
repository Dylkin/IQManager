package com.tenderbot.service;

import com.tenderbot.dto.ConfigDto;
import com.tenderbot.entity.Config;
import com.tenderbot.entity.ConfigGroup;
import com.tenderbot.repository.ConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfigService {

    private final ConfigRepository configRepository;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<Config> DEFAULT_CONFIGS = Arrays.asList(
        // Telegram
        new Config("telegram.bot.token", "", "Токен Telegram бота", ConfigGroup.TELEGRAM, true),
        new Config("telegram.bot.username", "", "Username Telegram бота (@botname)", ConfigGroup.TELEGRAM),
        new Config("telegram.channel.id", "", "ID канала для мониторинга", ConfigGroup.TELEGRAM),
        // Email
        new Config("spring.mail.host", "smtp.gmail.com", "SMTP/IMAP сервер", ConfigGroup.EMAIL),
        new Config("spring.mail.port", "587", "Порт SMTP", ConfigGroup.EMAIL),
        new Config("spring.mail.username", "", "Email отправителя", ConfigGroup.EMAIL),
        new Config("spring.mail.password", "", "Пароль / App Password для SMTP", ConfigGroup.EMAIL, true),
        new Config("spring.mail.smtp.port", "587", "Порт SMTP", ConfigGroup.EMAIL),
        new Config("spring.mail.imap.port", "993", "Порт IMAP", ConfigGroup.EMAIL),
        // Parser
        new Config("parser.goszakupki.base-url", "https://goszakupki.by", "Базовый URL портала госзакупок", ConfigGroup.PARSER),
        new Config("parser.goszakupki.timeout", "30000", "Таймаут HTTP запросов (мс)", ConfigGroup.PARSER),
        new Config("parser.user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36", "User-Agent для парсера", ConfigGroup.PARSER),
        new Config("parser.supplier.timeout", "30000", "Таймаут запросов к поставщикам (мс)", ConfigGroup.PARSER),
        // Scheduler
        new Config("scheduler.telegram-poll-interval", "60000", "Интервал опроса Telegram (мс)", ConfigGroup.PARSER),
        new Config("scheduler.tender-process-interval", "120000", "Интервал обработки тендеров (мс)", ConfigGroup.PARSER),
        // Documents
        new Config("document.storage.path", "/tmp/tenderbot/documents", "Путь хранения документов тендеров", ConfigGroup.GENERAL),
        // Suppliers
        new Config("supplier.sites", "redhon.ru,dia-m.ru", "Список сайтов поставщиков через запятую", ConfigGroup.SUPPLIER)
    );

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaults() {
        for (Config def : DEFAULT_CONFIGS) {
            if (!configRepository.existsByKey(def.getKey())) {
                configRepository.save(def);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ConfigDto> getAll() {
        return configRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConfigDto> getByGroup(ConfigGroup group) {
        return configRepository.findByGroup(group).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConfigDto getById(Long id) {
        return configRepository.findById(id)
            .map(this::toDto)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public ConfigDto getByKey(String key) {
        return configRepository.findByKey(key)
            .map(this::toDto)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public String getString(String key, String defaultValue) {
        return configRepository.findByKey(key)
            .map(Config::getValue)
            .filter(v -> v != null && !v.isBlank())
            .orElse(defaultValue);
    }

    @Transactional
    public ConfigDto update(Long id, ConfigDto dto) {
        Config config = configRepository.findById(id).orElseThrow();
        if (Boolean.FALSE.equals(config.getIsEditable())) {
            throw new IllegalStateException("Config is not editable: " + config.getKey());
        }
        config.setValue(dto.getValue());
        if (dto.getDescription() != null) config.setDescription(dto.getDescription());
        if (dto.getGroup() != null) config.setGroup(dto.getGroup());
        if (dto.getIsSecret() != null) config.setIsSecret(dto.getIsSecret());
        return toDto(configRepository.save(config));
    }

    @Transactional
    public ConfigDto updateByKey(String key, String value) {
        Config config = configRepository.findByKey(key)
            .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));
        if (Boolean.FALSE.equals(config.getIsEditable())) {
            throw new IllegalStateException("Config is not editable: " + key);
        }
        config.setValue(value);
        return toDto(configRepository.save(config));
    }

    @Transactional
    public ConfigDto create(ConfigDto dto) {
        if (configRepository.existsByKey(dto.getKey())) {
            throw new IllegalArgumentException("Config already exists: " + dto.getKey());
        }
        Config config = new Config();
        config.setKey(dto.getKey());
        config.setValue(dto.getValue());
        config.setDescription(dto.getDescription());
        config.setGroup(dto.getGroup() != null ? dto.getGroup() : ConfigGroup.GENERAL);
        config.setIsSecret(dto.getIsSecret() != null ? dto.getIsSecret() : false);
        config.setIsEditable(true);
        return toDto(configRepository.save(config));
    }

    @Transactional
    public void delete(Long id) {
        Config config = configRepository.findById(id).orElseThrow();
        if (Boolean.FALSE.equals(config.getIsEditable())) {
            throw new IllegalStateException("Cannot delete non-editable config: " + config.getKey());
        }
        configRepository.deleteById(id);
    }

    private ConfigDto toDto(Config config) {
        ConfigDto dto = new ConfigDto();
        dto.setId(config.getId());
        dto.setKey(config.getKey());
        dto.setValue(config.getValue());
        dto.setDescription(config.getDescription());
        dto.setGroup(config.getGroup());
        dto.setIsSecret(config.getIsSecret());
        dto.setIsEditable(config.getIsEditable());
        dto.setCreatedAt(config.getCreatedAt() != null ? config.getCreatedAt().format(DTF) : null);
        dto.setUpdatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().format(DTF) : null);
        return dto;
    }
}
