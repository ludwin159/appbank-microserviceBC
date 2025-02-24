package com.bank.appbank.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Function;

public class DateToPayUtil {
    public static Function<Integer, LocalDate> calculatePaymentDate = (payDay) -> {
        LocalDate today = LocalDate.now();
        YearMonth yearMonth = YearMonth.of(today.getYear(), today.getMonth());

        int lastDayOfMonth = yearMonth.lengthOfMonth();

        int finalDay = Integer.min(lastDayOfMonth, payDay);
        return LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), finalDay);
    };
}
