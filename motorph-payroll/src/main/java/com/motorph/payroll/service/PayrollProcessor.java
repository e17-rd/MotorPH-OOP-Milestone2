package com.motorph.payroll.service;

import com.motorph.payroll.model.AttendanceRecord;
import com.motorph.payroll.model.Employee;
import com.motorph.payroll.model.PayrollRecord;
import com.motorph.payroll.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * THE SINGLE OWNER OF PAYROLL ORCHESTRATION.
 *
 * MILESTONE 1 FEEDBACK FIX.
 * The mentor asked the team to "choose one owner for payroll orchestration and
 * update all three design sections accordingly", and to "write a single payroll
 * formula with unambiguous terms: gross wages, benefits, statutory deductions,
 * and net pay."
 *
 * That owner is this class. Employee objects know how to price THEIR OWN hours
 * and benefits; PayrollRecord stores the result; this processor is the only
 * place that decides the ORDER of operations.
 *
 * THE SINGLE PAYROLL FORMULA
 * --------------------------
 *   1. regularHours, overtimeHours  <- summed from the attendance records
 *   2. basePay                      <- employee.computeBasePay(regularHours)
 *   3. overtimePay                  <- employee.computeOvertimePay(overtimeHours)
 *   4. grossPay                     =  basePay + overtimePay        (benefits excluded)
 *   5. statutory                    =  SSS + PhilHealth + Pag-IBIG  (on monthly basic salary)
 *   6. taxableIncome                =  grossPay - statutory
 *   7. withholdingTax               =  tax table applied to taxableIncome
 *   8. benefits                     <- employee.calculateTotalBenefits()  (non-taxable)
 *   9. netPay                       =  taxableIncome - withholdingTax + benefits
 *
 * Overtime enters the computation at step 3 and nowhere else.
 * Benefits enter at step 8 and nowhere else -- they are never taxed and never
 * counted inside grossPay.
 */
public class PayrollProcessor {

    /**
     * Runs payroll for one employee over one period.
     *
     * POLYMORPHISM: the parameter is the Employee parent type. Whether a
     * Regular, Probationary or Manager object arrives, this method is
     * unchanged -- the overtime and benefit behaviour is selected at runtime.
     */
    public PayrollRecord processEmployeePayroll(Employee employee,
                                                List<AttendanceRecord> attendance,
                                                LocalDate periodStart,
                                                LocalDate periodEnd) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee is required.");
        }
        if (attendance == null) {
            throw new IllegalArgumentException("Attendance list is required (may be empty).");
        }
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Invalid payroll period.");
        }

        PayrollRecord.Builder record = PayrollRecord.builder()
                .employee(employee)
                .period(periodStart, periodEnd);

        // --- Step 1: total the attendance -------------------------------
        double regularHours = 0.0;
        double overtimeHours = 0.0;

        for (AttendanceRecord day : attendance) {
            if (day == null) {
                continue;
            }
            if (!day.getEmployeeId().equals(employee.getEmployeeId())) {
                continue;
            }
            if (day.getDate().isBefore(periodStart) || day.getDate().isAfter(periodEnd)) {
                continue;
            }
            if (!day.isComplete()) {
                record.addException("Incomplete attendance on " + day.getDate()
                        + " - missing time in or time out. Counted as zero hours.");
                continue;
            }
            if (day.getHoursWorked() > AttendanceRecord.REGULAR_HOURS_PER_DAY && !day.isApproved()) {
                record.addException("Unapproved overtime on " + day.getDate()
                        + " - excess hours not paid until a manager approves the timecard.");
            }
            regularHours += day.getRegularHours();
            overtimeHours += day.getOvertimeHours();
        }

        // --- Steps 2 to 4: gross pay ------------------------------------
        BigDecimal overtimePay = employee.computeOvertimePay(overtimeHours);
        BigDecimal grossPay = employee.calculateGrossPay(regularHours, overtimeHours);
        BigDecimal basePay = Money.round(grossPay.subtract(overtimePay));

        if (overtimeHours > 0 && overtimePay.signum() == 0) {
            record.addException(overtimeHours + " overtime hour(s) logged but "
                    + employee.getEmploymentType()
                    + " employees are overtime-exempt. No overtime pay added.");
        }

        // --- Step 5: statutory contributions ----------------------------
        BigDecimal basic = employee.getBasicSalary();
        BigDecimal sss = StatutoryDeductions.sss(basic);
        BigDecimal philhealth = StatutoryDeductions.philhealth(basic);
        BigDecimal pagibig = StatutoryDeductions.pagibig(basic);
        BigDecimal totalStatutory = Money.round(sss.add(philhealth).add(pagibig));

        // --- Steps 6 and 7: taxable income and withholding tax ----------
        BigDecimal taxableIncome = Money.atLeastZero(grossPay.subtract(totalStatutory));
        BigDecimal withholdingTax = StatutoryDeductions.withholdingTax(taxableIncome);

        // --- Steps 8 and 9: benefits and net pay ------------------------
        BigDecimal benefits = employee.calculateTotalBenefits();
        BigDecimal netPay = Money.atLeastZero(
                taxableIncome.subtract(withholdingTax).add(benefits));

        return record
                .regularHours(regularHours)
                .overtimeHours(overtimeHours)
                .basePay(basePay)
                .overtimePay(overtimePay)
                .grossPay(grossPay)
                .sss(sss)
                .philhealth(philhealth)
                .pagibig(pagibig)
                .totalStatutory(totalStatutory)
                .taxableIncome(taxableIncome)
                .withholdingTax(withholdingTax)
                .totalBenefits(benefits)
                .netPay(netPay)
                .build();
    }
}
