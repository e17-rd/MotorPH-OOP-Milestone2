package com.motorph.payroll.model;

import com.motorph.payroll.util.Money;

import java.math.BigDecimal;

/**
 * An employee still inside the six-month probationary period.
 *
 * MILESTONE 1 FEEDBACK FIX -- ambiguous benefit rule.
 * The mentor flagged that calculateTotalBenefits() "permits either zero or
 * prorated allowances without a defined eligibility rule; the implementation
 * contract remains ambiguous."
 *
 * The team has now committed to ONE rule, stated here and nowhere else:
 *
 *   Rice subsidy    -- granted in full from day one (it is a basic subsistence
 *                      benefit and is not tied to regularisation).
 *   Phone allowance -- NOT granted during probation.
 *   Clothing allow. -- NOT granted during probation.
 *
 * There is no proration by month served. The rule is a flat entitlement test,
 * which keeps the payslip reproducible and removes the ambiguity.
 */
public class ProbationaryEmployee extends Employee {

    /** Probationary staff earn the same overtime premium as regular staff. */
    public static final double OVERTIME_MULTIPLIER = 1.25;

    public ProbationaryEmployee(String employeeId, String lastName, String firstName) {
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

    /** Rice subsidy only -- see the entitlement rule in the class comment. */
    @Override
    public BigDecimal calculateTotalBenefits() {
        return Money.round(getRiceSubsidy());
    }

    @Override
    public String getEmploymentType() {
        return "Probationary";
    }
}
