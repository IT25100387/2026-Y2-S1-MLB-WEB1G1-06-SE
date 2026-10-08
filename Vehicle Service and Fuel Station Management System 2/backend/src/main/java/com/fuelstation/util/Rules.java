package com.fuelstation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Rules {
    private Rules() {}
    public static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        String clean=InputValidation.text(value,label,SetLimits.forLabel(label),true,true);
        if(label.toLowerCase(Locale.ROOT).contains("reason")&&clean.length()<3)throw new IllegalArgumentException(label+" must have at least three characters");
        return clean;
    }
    public static double nonnegative(Double value, String label) {
        if (value == null || !Double.isFinite(value) || value < 0) throw new IllegalArgumentException(label + " must be a valid nonnegative number");
        InputValidation.decimal(BigDecimal.valueOf(value).toPlainString(),label,0,999999999.99,false);
        return money(value);
    }
    public static double positive(Double value, String label) {
        if (value == null || !Double.isFinite(value) || value <= 0 || money(value) <= 0) throw new IllegalArgumentException(label + " must be greater than zero");
        InputValidation.decimal(BigDecimal.valueOf(value).toPlainString(),label,.01,999999999.99,false);
        return money(value);
    }
    public static int quantity(Integer value, String label) {
        if (value == null || value < 1 || value > 9999) throw new IllegalArgumentException(label + " must be a whole number from 1 to 9999");
        return value;
    }
    public static double money(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid amount");
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    public static double amount(Double value) { return value == null ? 0 : value; }
    public static String paymentMethod(String raw) {
        String value = required(raw, "Payment method").toUpperCase(Locale.ROOT);
        return switch (value) {
            case "CASH" -> "CASH";
            case "CARD", "CREDIT CARD", "DEBIT CARD", "CREDIT / DEBIT CARD" -> "CARD";
            case "QR", "QR PAY", "WALLET" -> "QR";
            default -> throw new IllegalArgumentException("Select Cash, Card or QR");
        };
    }
    private static class SetLimits {
        static int forLabel(String label) {return switch(label.toLowerCase(Locale.ROOT)) {case "message", "reply", "mechanic notes", "problem description" -> 4000; default -> 255;};}
    }
}
