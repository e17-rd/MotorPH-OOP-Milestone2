package com.motorph.payroll.model;

import com.motorph.payroll.util.Money;

import java.math.BigDecimal;

/**
 * A managerial employee. Inherits the regular pay structure, then departs
 * from it in exactly two documented ways.
 *
 * MILESTONE 1 FEEDBACK FIX -- unclear Manager overrides.
 * The mentor asked the team to "document whether Manager overrides all
 * pay-related behavior that differs from RegularEmployee, not only
 * computeOvertimePay()."
 *
 * The complete list of managerial differences is:
 *
 *   1. computeOvertimePay()      -- OVERRIDDEN. Managers are overtime-exempt
 *                                   and always receive zero overtime pay,
 *                                   however many extra hours are logged.
 *   2. calculateTotalBenefits()  -- OVERRIDDEN. Managers receive the three
 *                                   standard allowances PLUS a managerial
 *                                   allowance.
 *
 * Everything else -- base pay, hourly rate, the gross pay formula, and every
 * statutory deduction -- is inherited from RegularEmployee UNCHANGED.
 * Manager adds two approval behaviours of its own that no other type has.
 */
public class Manager extends RegularEmployee {

    private BigDecimal managerialAllowance;

    public Manager(String employeeId, String lastName, String firstName) {
        super(employeeId, lastName, firstName);
        this.managerialAllowance = Money.ZERO;
    }

    /**
     * OVERRIDE 1 of 2: managers are exempt from overtime pay.
     * Returns zero regardless of the hours passed in.
     */
    @Override
    public BigDecimal computeOvertimePay(double overtimeHours) {
        return Money.ZERO;
    }

    /** OVERRIDE 2 of 2: standard allowances plus the managerial allowance. */
    @Override
    public BigDecimal calculateTotalBenefits() {
        return Money.round(sumOfAllowances().add(managerialAllowance));
    }

    @Override
    public String getEmploymentType() {
        return "Manager";
    }

    public BigDecimal getManagerialAllowance() {
        return managerialAllowance;
    }

    public void setManagerialAllowance(BigDecimal managerialAllowance) {
        if (managerialAllowance == null || managerialAllowance.signum() < 0) {
            throw new IllegalArgumentException("Managerial allowance cannot be negative.");
        }
        this.managerialAllowance = Money.round(managerialAllowance);
    }

    // ---- Manager-only responsibilities (from the MS1 CRC card) ----------

    /** Approves a subordinate's timecard before it enters payroll. */
    public boolean approveTimecard(AttendanceRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("No attendance record to approve.");
        }
        if (!record.isComplete()) {
            return false;
        }
        record.markApproved(getEmployeeId());
        return true;
    }

    /** Endorses a manual payroll adjustment for a subordinate. */
    public String endorsePayrollAdjustment(Employee subordinate, BigDecimal amount, String reason) {
        if (subordinate == null) {
            throw new IllegalArgumentException("Subordinate is required.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("An adjustment must state a reason.");
        }
        return "Adjustment of " + Money.round(amount) + " for "
                + subordinate.getEmployeeId() + " endorsed by " + getEmployeeId()
                + " (" + reason + ")";
    }
}
