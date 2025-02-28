package com.bank.appbank.service;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {
    Mono<Map<String, Object>> generateReportAverageBalanceDailyInPresentMonth(String idClient);
    Mono<Map<String, Object>> generateReportAllCommissionsByProductInRangeDate(String from, String to);
    Mono<Map<String, Object>> generateReportCompleteAndGeneralByProductInRangeDate(Instant from, Instant to);
    Mono<Map<String, Object>> reportLastTenMovementsCreditDebit(String idClient);
}
