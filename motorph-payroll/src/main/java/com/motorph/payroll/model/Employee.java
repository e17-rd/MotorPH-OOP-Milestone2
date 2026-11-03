package com.motorph.payroll.model;

import com.motorph.payroll.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abstract parent for every MotorPH employee type.
 *
 * ENCAPSULATION  - all sensitive payroll and government fields are private and
 *                  reachable only through validated accessors.
 * ABSTRACTION    - defines the payroll contract every employee type must honour.
 * INHERITANCE    - RegularEmployee, ProbationaryEmployee and Manager extend this.
 * POLYMORPHISM   - computeBasePay, computeOvertimePay and calculateTotalBenefits
 *                  are resolved at runtime through an Employee reference.
 *
 * MILESTONE 1 FEEDBACK FIX -- the overtime double-count.
 * The mentor flagged that RegularEmployee.calculateGrossPay() already included
 * overtime while computeOvertimePay() computed it a second time.
 *
 * Resolution: calculateGrossPay() is now FINAL and is the single, shared
 * definition of gross pay:
 *
 *         grossPay = computeBasePay(regularHours) + computeOvertimePay(overtimeHours)
 *
 * No subclass may redefine that sum, so overtime can be counted exactly once.
 * Subclasses vary ONLY the two parts, which is where the polymorphism lives.
 */
public abstract class Employee {

    // ---- Identity -------------------------------------------------------
    private final String employeeId;
    private String lastName;
    private String firstName;
    private LocalDate birthday;

    // ---- Protected government identifiers (encapsulated) ----------------
    private String sssNumber;
    private String philhealthNumber;
    private String tinNumber;
    private String pagibigNumber;

    // ---- Employment ------------------------------------------------------
    private String position;
    private String supervisor;

    // ---- Compensation ----------------------------------------------------
    private BigDecimal basicSalary;      // monthly
    private BigDecimal riceSubsidy;      // monthly benefit
    private BigDecimal phoneAllowance;   // monthly benefit
    private BigDecimal clothingAllowance;// monthly benefit

    /** Standard MotorPH working month used to derive the hourly rate. */
    public static final int WORKING_DAYS_PER_MONTH = 21;
    public static final int WORKING_HOURS_PER_DAY = 8;

    protected Employee(String employeeId, String lastName, String firstName) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        this.employeeId = employeeId;
        setLastName(lastName);
        setFirstName(firstName);
        this.basicSalary = Money.ZERO;
        this.riceSubsidy = Money.ZERO;
        this.phoneAllowance = Money.ZERO;
        this.clothingAllowance = Money.ZERO;
    }

    // =====================================================================
    //  THE PAYROLL CONTRACT
    // =====================================================================

    /**
     * The single definition of gross pay for every employee type.
     * Declared final so overtime is added exactly once, system-wide.
     *
     * Gross pay deliberately EXCLUDES benefits. Benefits are non-taxable and
     * are added after deductions by PayrollProcessor. See calculateTotalBenefits().
     */
    public final BigDecimal calculateGrossPay(double regularHours, double overtimeHours) {
        if (regularHours < 0 || overtimeHours < 0) {
            throw new IllegalArgumentException("Hours cannot be negative.");
        }
        BigDecimal base = computeBasePay(regularHours);
        BigDecimal overtime = computeOvertimePay(overtimeHours);
        return Money.round(base.add(overtime));
    }

    /** Pay for hours worked up to the regular limit. Varies per employee type. */
    protected abstract BigDecimal computeBasePay(double regularHours);

    /**
     * Pay for hours beyond the regular limit.
     * Manager overrides this to return zero (overtime-exempt).
     */
    public abstract BigDecimal computeOvertimePay(double overtimeHours);

    /**
     * Total non-taxable monthly benefits this employee is entitled to.
     * Probationary employees receive a reduced, explicitly-defined share.
     */
    public abstract BigDecimal calculateTotalBenefits();

    /** Human-readable employment classification, shown on the payslip. */
    public abstract String getEmploymentType();

    /**
     * MILESTONE 1 FEEDBACK FIX -- ambiguous computeTotalCompensation().
     * The mentor asked whether gross income includes benefits. It does not.
     * This method is the explicit "gross pay plus benefits" figure and is used
     * for reporting only; it is never fed back into a deduction calculation.
     */
    public final BigDecimal computeTotalCompensation(double regularHours, double overtimeHours) {
        return Money.round(calculateGrossPay(regularHours, overtimeHours)
                .add(calculateTotalBenefits()));
    }

    /** Derived hourly rate: monthly basic over the standard 21 x 8 month. */
    public BigDecimal getHourlyRate() {
        return Money.round(basicSalary.divide(
                BigDecimal.valueOf((long) WORKING_DAYS_PER_MONTH * WORKING_HOURS_PER_DAY),
                10, Money.ROUNDING));
    }

    // =====================================================================
    //  VALIDATED ACCESSORS (encapsulation)
    // =====================================================================

    public String getEmployeeId() {
        return employeeId;
    }

    public String getLastName() {
        return lastName;
    }

    public final void setLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required.");
        }
        this.lastName = lastName.trim();
    }

    public String getFirstName() {
        return firstName;
    }

    public final void setFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required.");
        }
        this.firstName = firstName.trim();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        if (birthday != null && birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birthday cannot be in the future.");
        }
        this.birthday = birthday;
    }

    /** Government numbers are returned masked unless explicitly unmasked. */
    public String getSssNumber() {
        return mask(sssNumber);
    }

    public String getSssNumberRaw() {
        return sssNumber;
    }

    public void setSssNumber(String sssNumber) {
        this.sssNumber = requireIdentifier(sssNumber, "SSS number");
    }

    public String getPhilhealthNumber() {
        return mask(philhealthNumber);
    }

    public String getPhilhealthNumberRaw() {
        return philhealthNumber;
    }

    public void setPhilhealthNumber(String philhealthNumber) {
        this.philhealthNumber = requireIdentifier(philhealthNumber, "PhilHealth number");
    }

    public String getTinNumber() {
        return mask(tinNumber);
    }

    public String getTinNumberRaw() {
        return tinNumber;
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = requireIdentifier(tinNumber, "TIN");
    }

    public String getPagibigNumber() {
        return mask(pagibigNumber);
    }

    public String getPagibigNumberRaw() {
        return pagibigNumber;
    }

    public void setPagibigNumber(String pagibigNumber) {
        this.pagibigNumber = requireIdentifier(pagibigNumber, "Pag-IBIG number");
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = requireNonNegative(basicSalary, "Basic salary");
    }

    public BigDecimal getRiceSubsidy() {
        return riceSubsidy;
    }

    public void setRiceSubsidy(BigDecimal riceSubsidy) {
        this.riceSubsidy = requireNonNegative(riceSubsidy, "Rice subsidy");
    }

    public BigDecimal getPhoneAllowance() {
        return phoneAllowance;
    }

    public void setPhoneAllowance(BigDecimal phoneAllowance) {
        this.phoneAllowance = requireNonNegative(phoneAllowance, "Phone allowance");
    }

    public BigDecimal getClothingAllowance() {
        return clothingAllowance;
    }

    public void setClothingAllowance(BigDecimal clothingAllowance) {
        this.clothingAllowance = requireNonNegative(clothingAllowance, "Clothing allowance");
    }

    /** Sum of the three monthly allowances, before any entitlement rule. */
    protected BigDecimal sumOfAllowances() {
        return Money.round(riceSubsidy.add(phoneAllowance).add(clothingAllowance));
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private static BigDecimal requireNonNegative(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(label + " cannot be negative.");
        }
        return Money.round(value);
    }

    private static String requireIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    /** Shows only the last four characters of a government identifier. */
    private static String mask(String value) {
        if (value == null || value.length() <= 4) {
            return value;
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }

    @Override
    public String toString() {
        return getEmploymentType() + " " + employeeId + " - " + getFullName();
    }
}
