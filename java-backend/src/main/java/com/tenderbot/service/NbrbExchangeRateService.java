package com.tenderbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class NbrbExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(NbrbExchangeRateService.class);
    private static final String API_URL = "https://api.nbrb.by/exrates/rates/RUB?parammode=2";
    private static final long CACHE_TTL_MINUTES = 60;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private CachedRate cachedRate;

    public ExchangeRate getRubRate() {
        if (cachedRate != null && cachedRate.isValid()) {
            return cachedRate.rate();
        }
        try {
            String response = restTemplate.getForObject(API_URL, String.class);
            if (response == null || response.isBlank()) {
                log.warn("Empty response from NBRB API");
                return fallbackRate();
            }
            JsonNode node = mapper.readTree(response);
            double officialRate = node.path("Cur_OfficialRate").asDouble();
            double scale = node.path("Cur_Scale").asDouble(100.0);
            String dateStr = node.path("Date").asText();
            var rate = new ExchangeRate(officialRate, scale, dateStr);
            cachedRate = new CachedRate(rate, LocalDateTime.now());
            log.info("Fetched NBRB RUB rate: scale={}, rate={}, 1 RUB = {} BYN", scale, officialRate, rate.ratePerOneRub());
            return rate;
        } catch (Exception e) {
            log.warn("Failed to fetch NBRB exchange rate: {}. Using fallback.", e.getMessage());
            return fallbackRate();
        }
    }

    public double convertRubToByn(double rubAmount) {
        if (rubAmount <= 0 || Double.isNaN(rubAmount)) return 0.0;
        ExchangeRate rate = getRubRate();
        return rubAmount * rate.ratePerOneRub();
    }

    public double calculateFinalByn(double rubAmount, Double markupPercent, Double deliveryCostByn) {
        if (rubAmount <= 0 || Double.isNaN(rubAmount)) return 0.0;
        double baseByn = convertRubToByn(rubAmount);
        double markup = markupPercent != null && !Double.isNaN(markupPercent) ? markupPercent : 0.0;
        double delivery = deliveryCostByn != null && !Double.isNaN(deliveryCostByn) ? deliveryCostByn : 0.0;
        return baseByn * (1.0 + markup / 100.0) + delivery;
    }

    private ExchangeRate fallbackRate() {
        if (cachedRate != null) {
            return cachedRate.rate();
        }
        // Fallback approximate rate: 100 RUB ≈ 3.8 BYN
        return new ExchangeRate(3.8, 100.0, LocalDateTime.now().toString());
    }

    public record ExchangeRate(double officialRate, double scale, String date) {
        public double ratePerOneRub() {
            return scale > 0 ? officialRate / scale : 0.0;
        }
    }

    private record CachedRate(ExchangeRate rate, LocalDateTime fetchedAt) {
        boolean isValid() {
            return fetchedAt != null && fetchedAt.plusMinutes(CACHE_TTL_MINUTES).isAfter(LocalDateTime.now());
        }
    }
}
