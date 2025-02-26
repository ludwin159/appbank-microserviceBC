package com.bank.appbank.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Numbers {
    public static double redondear(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
