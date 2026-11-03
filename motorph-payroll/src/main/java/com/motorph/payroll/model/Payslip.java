package com.motorph.payroll.model;

import java.time.format.DateTimeFormatter;

/**
 * Formats a finished PayrollRecord for display.
 *
 * Presentation only. It reads figures and lays them out; it never computes a
 * peso amount of its own. This keeps the GUI free of payroll logic, which is
 * the Milestone 2 requirement "No GUI-side computation".
 */
public final class Payslip {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Payslip() {
    }

    /**
     * POLYMORPHISM IN THE OUTPUT LAYER.
     * The Employee reference is used without any instanceof check; the
     * employment type and benefit figures come from the runtime object.
     */
    public static String generatePayslipSummary(Employee employee, PayrollRecord record) {
        if (employee == null || record == null) {
            throw new IllegalArgumentException("Employee and payroll record are required.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("        MotorPH PAYSLIP\n");
        sb.append("========================================\n");
        sb.append(line("Employee ID", employee.getEmployeeId()));
        sb.append(line("Name", employee.getFullName()));
        sb.append(line("Position", String.valueOf(employee.getPosition())));
        sb.append(line("Employment Type", employee.getEmploymentType()));
        sb.append(line("Period", record.getPeriodStart().format(DATE)
                + " to " + record.getPeriodEnd().format(DATE)));
        sb.append("----------------------------------------\n");
        sb.append("EARNINGS\n");
        sb.append(amount("Regular hours", record.getRegularHours()));
        sb.append(line("Base pay", peso(record.getBasePay())));
        sb.append(amount("Overtime hours", record.getOvertimeHours()));
        sb.append(line("Overtime pay", peso(record.getOvertimePay())));
        sb.append(line("GROSS PAY", peso(record.getGrossPay())));
        sb.append("----------------------------------------\n");
        sb.append("DEDUCTIONS\n");
        sb.append(line("SSS", peso(record.getSssContribution())));
        sb.append(line("PhilHealth", peso(record.getPhilhealthContribution())));
        sb.append(line("Pag-IBIG", peso(record.getPagibigContribution())));
        sb.append(line("Total statutory", peso(record.getTotalStatutoryDeductions())));
        sb.append(line("Taxable income", peso(record.getTaxableIncome())));
        sb.append(line("Withholding tax", peso(record.getWithholdingTax())));
        sb.append(line("TOTAL DEDUCTIONS", peso(record.getTotalDeductions())));
        sb.append("----------------------------------------\n");
        sb.append("BENEFITS (non-taxable)\n");
        sb.append(line("Total benefits", peso(record.getTotalBenefits())));
        sb.append("----------------------------------------\n");
        sb.append(line("NET PAY", peso(record.getNetPay())));
        sb.append("========================================\n");

        if (record.hasExceptions()) {
            sb.append("NOTES\n");
            for (String note : record.getExceptions()) {
                sb.append("  - ").append(note).append("\n");
            }
            sb.append("========================================\n");
        }
        return sb.toString();
    }

    private static String line(String label, String value) {
        return String.format("%-20s %s%n", label + ":", value);
    }

    private static String amount(String label, double value) {
        return String.format("%-20s %.2f%n", label + ":", value);
    }

    private static String peso(java.math.BigDecimal value) {
        return String.format("PHP %,.2f", value);
    }
}
