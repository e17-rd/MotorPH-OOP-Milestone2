package com.motorph.payroll.model;

import com.motorph.payroll.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The finished result of one payroll run for one employee.
 *
 * MILESTONE 1 FEEDBACK FIX -- split responsibility.
 * The mentor flagged twice that "PayrollRecord is described both as a payroll
 * data record and as the payroll processor. Combining calculation
 * orchestration with record storage may make the class difficult to maintain",
 * and asked the team to "choose one owner for payroll orchestration."
 *
 * The team chose PayrollProcessor as the single owner of the calculation.
 * PayrollRecord is now a pure, immutable DATA HOLDER. It performs no payroll
 * arithmetic of its own -- every figure below is handed to it already computed.
 * The old processEmployeePayroll() method has been removed from this class.
 */
public final class PayrollRecord {

    private final Employee employee;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    private final double regularHours;
    private final double overtimeHours;

    private final BigDecimal basePay;
    private final BigDecimal overtimePay;
    private final BigDecimal grossPay;

    private final BigDecimal sssContribution;
    private final BigDecimal philhealthContribution;
    private final BigDecimal pagibigContribution;
    private final BigDecimal totalStatutoryDeductions;

    private final BigDecimal taxableIncome;
    private final BigDecimal withholdingTax;
    private final BigDecimal totalBenefits;
    private final BigDecimal netPay;

    private final List<String> exceptions;

    private PayrollRecord(Builder b) {
        this.employee = b.employee;
        this.periodStart = b.periodStart;
        this.periodEnd = b.periodEnd;
        this.regularHours = b.regularHours;
        this.overtimeHours = b.overtimeHours;
        this.basePay = b.basePay;
        this.overtimePay = b.overtimePay;
        this.grossPay = b.grossPay;
        this.sssContribution = b.sss;
        this.philhealthContribution = b.philhealth;
        this.pagibigContribution = b.pagibig;
        this.totalStatutoryDeductions = b.totalStatutory;
        this.taxableIncome = b.taxableIncome;
        this.withholdingTax = b.withholdingTax;
        this.totalBenefits = b.totalBenefits;
        this.netPay = b.netPay;
        this.exceptions = List.copyOf(b.exceptions);
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public double getRegularHours() {
        return regularHours;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public BigDecimal getBasePay() {
        return basePay;
    }

    public BigDecimal getOvertimePay() {
        return overtimePay;
    }

    /** Gross pay = base pay + overtime pay. Benefits are NOT included here. */
    public BigDecimal getGrossPay() {
        return grossPay;
    }

    public BigDecimal getSssContribution() {
        return sssContribution;
    }

    public BigDecimal getPhilhealthContribution() {
        return philhealthContribution;
    }

    public BigDecimal getPagibigContribution() {
        return pagibigContribution;
    }

    public BigDecimal getTotalStatutoryDeductions() {
        return totalStatutoryDeductions;
    }

    /** Taxable income = gross pay - statutory deductions. */
    public BigDecimal getTaxableIncome() {
        return taxableIncome;
    }

    public BigDecimal getWithholdingTax() {
        return withholdingTax;
    }

    /** Non-taxable allowances, added after tax. */
    public BigDecimal getTotalBenefits() {
        return totalBenefits;
    }

    /** Net pay = taxable income - withholding tax + benefits. */
    public BigDecimal getNetPay() {
        return netPay;
    }

    /** Total of every amount withheld. */
    public BigDecimal getTotalDeductions() {
        return Money.round(totalStatutoryDeductions.add(withholdingTax));
    }

    /** Problems found while reading attendance, e.g. missing punches. */
    public List<String> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    public boolean hasExceptions() {
        return !exceptions.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Only PayrollProcessor populates this builder. */
    public static final class Builder {
        private Employee employee;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private double regularHours;
        private double overtimeHours;
        private BigDecimal basePay = Money.ZERO;
        private BigDecimal overtimePay = Money.ZERO;
        private BigDecimal grossPay = Money.ZERO;
        private BigDecimal sss = Money.ZERO;
        private BigDecimal philhealth = Money.ZERO;
        private BigDecimal pagibig = Money.ZERO;
        private BigDecimal totalStatutory = Money.ZERO;
        private BigDecimal taxableIncome = Money.ZERO;
        private BigDecimal withholdingTax = Money.ZERO;
        private BigDecimal totalBenefits = Money.ZERO;
        private BigDecimal netPay = Money.ZERO;
        private final List<String> exceptions = new ArrayList<>();

        public Builder employee(Employee v) { this.employee = v; return this; }
        public Builder period(LocalDate start, LocalDate end) {
            this.periodStart = start; this.periodEnd = end; return this;
        }
        public Builder regularHours(double v) { this.regularHours = v; return this; }
        public Builder overtimeHours(double v) { this.overtimeHours = v; return this; }
        public Builder basePay(BigDecimal v) { this.basePay = v; return this; }
        public Builder overtimePay(BigDecimal v) { this.overtimePay = v; return this; }
        public Builder grossPay(BigDecimal v) { this.grossPay = v; return this; }
        public Builder sss(BigDecimal v) { this.sss = v; return this; }
        public Builder philhealth(BigDecimal v) { this.philhealth = v; return this; }
        public Builder pagibig(BigDecimal v) { this.pagibig = v; return this; }
        public Builder totalStatutory(BigDecimal v) { this.totalStatutory = v; return this; }
        public Builder taxableIncome(BigDecimal v) { this.taxableIncome = v; return this; }
        public Builder withholdingTax(BigDecimal v) { this.withholdingTax = v; return this; }
        public Builder totalBenefits(BigDecimal v) { this.totalBenefits = v; return this; }
        public Builder netPay(BigDecimal v) { this.netPay = v; return this; }
        public Builder addException(String v) { this.exceptions.add(v); return this; }

        public PayrollRecord build() {
            return new PayrollRecord(this);
        }
    }
}
