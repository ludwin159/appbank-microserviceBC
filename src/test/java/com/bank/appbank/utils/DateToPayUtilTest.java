package com.bank.appbank.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateToPayUtilTest {

    @Test
    void testCalculatePaymentDate_ValidDay() {
        LocalDate paymentDate = DateToPayUtil.calculatePaymentDate.apply(15);
        LocalDate expectedDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), 15);
        assertEquals(expectedDate, paymentDate);
    }

    @Test
    void testCalculatePaymentDate_ExceedsLastDayOfMonth() {
        LocalDate paymentDate = DateToPayUtil.calculatePaymentDate.apply(31);
        int lastDayOfMonth = LocalDate.now().lengthOfMonth();
        LocalDate expectedDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), lastDayOfMonth);
        assertEquals(expectedDate, paymentDate);
    }

    @Test
    void testCalculatePaymentDate_FebruaryNonLeapYear() {
        LocalDate paymentDate = DateToPayUtil.calculatePaymentDate.apply(30);
        LocalDate expectedDate = LocalDate.of(LocalDate.now().getYear(), 2, 28);
        assertEquals(expectedDate, paymentDate);
    }
}