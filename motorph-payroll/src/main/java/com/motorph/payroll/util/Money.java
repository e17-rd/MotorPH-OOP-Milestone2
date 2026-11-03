package com.motorph.payroll.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central money helper.
 *
 * MILESTONE 1 FEEDBACK FIX:
 * "Financial values use double throughout. This can introduce rounding
 *  discrepancies for money and should be addressed before implementation."
 *
 * Every peso amount in the system is a BigDecimal. Rounding happens at ONE
 * place only -- the boundaries defined here -- so that two different code
 * paths can never disagree about a centavo.
 */
public final class Money {

    /** All currency amounts are carried and reported to 2 decimal places. */
    public static final int SCALE = 2;

    /** Banker-free, predictable rounding for payroll: round half up. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal ZERO = of(0);

    private Money() {
        // utility class
    }

    /** Build a money value from a plain number. */
    public static BigDecimal of(double amount) {
        return BigDecimal.valueOf(amount).setScale(SCALE, ROUNDING);
    }

    /** Build a money value from a string literal (exact, no binary drift). */
    public static BigDecimal of(String amount) {
        return new BigDecimal(amount).setScale(SCALE, ROUNDING);
    }

    /**
     * Round a money value at a calculation boundary.
     * Call this when a computed amount becomes a reportable figure.
     */
    public static BigDecimal round(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(SCALE, ROUNDING);
    }

    /** Multiply money by a plain factor (hours, rate multiplier, percentage). */
    public static BigDecimal multiply(BigDecimal amount, double factor) {
        return round(amount.multiply(BigDecimal.valueOf(factor)));
    }

    /** Percentage of an amount, e.g. percentOf(salary, 3.0) for 3%. */
    public static BigDecimal percentOf(BigDecimal amount, double percent) {
        return round(amount.multiply(BigDecimal.valueOf(percent))
                           .divide(BigDecimal.valueOf(100), 10, ROUNDING));
    }

    /** Never let a computed deduction or pay figure go below zero. */
    public static BigDecimal atLeastZero(BigDecimal amount) {
        return amount.signum() < 0 ? ZERO : round(amount);
    }

    /** Cap a value at a maximum (used for the Pag-IBIG ceiling). */
    public static BigDecimal cap(BigDecimal amount, BigDecimal maximum) {
        return amount.compareTo(maximum) > 0 ? maximum : round(amount);
    }
}
