package com.motorph.payroll.service;

import com.motorph.payroll.util.Money;

import java.math.BigDecimal;

/**
 * Philippine statutory contribution and withholding tax rules.
 *
 * Kept in one class so that every bracket, rate and ceiling the payroll relies
 * on is visible in a single place and can be checked against the published
 * tables. PayrollProcessor is the only caller.
 *
 * NOTE FOR THE TEAM: these brackets follow the standard tables used in the
 * MotorPH course data. Verify each one against the project's own reference
 * sheet before the Terminal Assessment -- the rates are policy, not logic,
 * and changing them must never require changing the payroll flow.
 */
public final class StatutoryDeductions {

    private StatutoryDeductions() {
    }

    // =====================================================================
    //  SSS -- bracket table on monthly basic salary
    // =====================================================================

    /** Lower bound of each salary bracket. */
    private static final double[] SSS_SALARY_FLOOR = {
            0, 3250, 3750, 4250, 4750, 5250, 5750, 6250, 6750, 7250, 7750,
            8250, 8750, 9250, 9750, 10250, 10750, 11250, 11750, 12250, 12750,
            13250, 13750, 14250, 14750, 15250, 15750, 16250, 16750, 17250,
            17750, 18250, 18750, 19250, 19750, 20250, 20750, 21250, 21750,
            22250, 22750, 23250, 23750, 24250, 24750
    };

    /** Employee contribution for the matching bracket. */
    private static final double[] SSS_CONTRIBUTION = {
            135.00, 157.50, 180.00, 202.50, 225.00, 247.50, 270.00, 292.50,
            315.00, 337.50, 360.00, 382.50, 405.00, 427.50, 450.00, 472.50,
            495.00, 517.50, 540.00, 562.50, 585.00, 607.50, 630.00, 652.50,
            675.00, 697.50, 720.00, 742.50, 765.00, 787.50, 810.00, 832.50,
            855.00, 877.50, 900.00, 922.50, 945.00, 967.50, 990.00, 1012.50,
            1035.00, 1057.50, 1080.00, 1102.50, 1125.00
    };

    /** Employee SSS contribution for a monthly basic salary. */
    public static BigDecimal sss(BigDecimal monthlyBasicSalary) {
        double salary = monthlyBasicSalary.doubleValue();
        BigDecimal contribution = Money.of(SSS_CONTRIBUTION[0]);
        for (int i = 0; i < SSS_SALARY_FLOOR.length; i++) {
            if (salary >= SSS_SALARY_FLOOR[i]) {
                contribution = Money.of(SSS_CONTRIBUTION[i]);
            } else {
                break;
            }
        }
        return contribution;
    }

    // =====================================================================
    //  PhilHealth -- 3% premium, shared equally by employer and employee
    // =====================================================================

    public static final double PHILHEALTH_PREMIUM_RATE = 3.0;
    public static final BigDecimal PHILHEALTH_SALARY_FLOOR = Money.of("10000.00");
    public static final BigDecimal PHILHEALTH_SALARY_CEILING = Money.of("100000.00");

    /** Employee share of the PhilHealth premium (half of the 3%). */
    public static BigDecimal philhealth(BigDecimal monthlyBasicSalary) {
        BigDecimal creditable = monthlyBasicSalary;
        if (creditable.compareTo(PHILHEALTH_SALARY_FLOOR) < 0) {
            creditable = PHILHEALTH_SALARY_FLOOR;
        } else if (creditable.compareTo(PHILHEALTH_SALARY_CEILING) > 0) {
            creditable = PHILHEALTH_SALARY_CEILING;
        }
        BigDecimal premium = Money.percentOf(creditable, PHILHEALTH_PREMIUM_RATE);
        return Money.round(premium.divide(BigDecimal.valueOf(2), Money.SCALE, Money.ROUNDING));
    }

    // =====================================================================
    //  Pag-IBIG -- 1% or 2% of monthly basic, capped
    // =====================================================================

    public static final BigDecimal PAGIBIG_CAP = Money.of("100.00");
    private static final BigDecimal PAGIBIG_LOWER_TIER_LIMIT = Money.of("1500.00");

    public static BigDecimal pagibig(BigDecimal monthlyBasicSalary) {
        if (monthlyBasicSalary.signum() <= 0) {
            return Money.ZERO;
        }
        double rate = monthlyBasicSalary.compareTo(PAGIBIG_LOWER_TIER_LIMIT) <= 0 ? 1.0 : 2.0;
        return Money.cap(Money.percentOf(monthlyBasicSalary, rate), PAGIBIG_CAP);
    }

    // =====================================================================
    //  Withholding tax -- monthly TRAIN brackets on taxable income
    // =====================================================================

    /**
     * Monthly withholding tax on taxable income
     * (gross pay less the three statutory contributions).
     */
    public static BigDecimal withholdingTax(BigDecimal taxableIncome) {
        double income = taxableIncome.doubleValue();
        if (income <= 20833) {
            return Money.ZERO;
        }
        if (income <= 33333) {
            return Money.of(0.20 * (income - 20833));
        }
        if (income <= 66667) {
            return Money.of(2500 + 0.25 * (income - 33333));
        }
        if (income <= 166667) {
            return Money.of(10833 + 0.30 * (income - 66667));
        }
        if (income <= 666667) {
            return Money.of(40833.33 + 0.32 * (income - 166667));
        }
        return Money.of(200833.33 + 0.35 * (income - 666667));
    }
}
