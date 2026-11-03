package com.motorph.payroll.model;

import com.motorph.payroll.util.Money;

import java.math.BigDecimal;

/**
 * A regularised, rank-and-file MotorPH employee.
 *
 * Entitlements:
 *   - Base pay at the full hourly rate.
 *   - Overtime at 1.25x the hourly rate.
 *   - 100% of the monthly rice, phone and clothing allowances.
 */
public class RegularEmployee extends Employee {

    /** Overtime premium for rank-and-file staff. */
    public static final double OVERTIME_MULTIPLIER = 1.25;

    public RegularEmployee(String employeeId, String lastName, String firstName) {
        super(employeeId, lastName, firstName);
    }

    @Override
    protected BigDecimal computeBasePay(double regularHours) {
        return Money.multiply(getHourlyRate(), regularHours);
    }

    @Override
    public BigDecimal computeOvertimePay(double overtimeHours) {
        if (overtimeHours <= 0) {
            return Money.ZERO;
        }
        return Money.multiply(getHourlyRate(), overtimeHours * OVERTIME_MULTIPLIER);
    }

    @Override
    public BigDecimal calculateTotalBenefits() {
        return sumOfAllowances();
    }

    @Override
    public String getEmploymentType() {
        return "Regular";
    }
}
