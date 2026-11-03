package com.motorph.payroll;

import com.motorph.payroll.model.*;
import com.motorph.payroll.service.PayrollProcessor;
import com.motorph.payroll.service.StatutoryDeductions;
import com.motorph.payroll.util.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Milestone 2 test suite.
 *
 * Each nested class maps to a specific Milestone 1 feedback point, so the
 * mentor can trace a correction straight to the test that proves it.
 */
class PayrollCalculationTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 31);

    private final PayrollProcessor processor = new PayrollProcessor();

    // ===================================================================
    //  FEEDBACK 1 -- overtime must be counted exactly once
    // ===================================================================
    @Nested
    @DisplayName("Overtime is counted exactly once")
    class OvertimeCountedOnce {

        @Test
        @DisplayName("Gross pay equals base pay plus overtime pay, never more")
        void grossPayIsBasePlusOvertimeExactlyOnce() {
            RegularEmployee employee = regular("33000.00");

            BigDecimal gross = employee.calculateGrossPay(160, 8);
            BigDecimal overtime = employee.computeOvertimePay(8);

            // hourly = 33000 / 168 = 196.43
            // base   = 196.43 x 160       = 31,428.80
            // OT     = 196.43 x 8 x 1.25  =  1,964.30
            assertEquals(Money.of("1964.30"), overtime);
            assertEquals(Money.of("33393.10"), gross);

            // The decisive check: gross minus overtime leaves the base pay
            // untouched. If overtime were added twice this would be short.
            assertEquals(Money.of("31428.80"), gross.subtract(overtime));
        }

        @Test
        @DisplayName("Zero overtime hours means gross pay equals base pay")
        void zeroOvertimeMeansGrossEqualsBase() {
            RegularEmployee employee = regular("33000.00");
            assertEquals(employee.calculateGrossPay(160, 0),
                         Money.of("31428.80"));
            assertEquals(Money.ZERO, employee.computeOvertimePay(0));
        }

        @Test
        @DisplayName("The processor reports base pay and overtime pay that sum to gross")
        void processorComponentsSumToGross() {
            RegularEmployee employee = regular("33000.00");
            PayrollRecord record = processor.processEmployeePayroll(
                    employee, approvedDays(employee.getEmployeeId(), 20, 9), START, END);

            assertEquals(record.getGrossPay(),
                         record.getBasePay().add(record.getOvertimePay()),
                         "Base + overtime must reconstruct gross exactly");
        }
    }

    // ===================================================================
    //  FEEDBACK 2 -- manager overtime exemption
    // ===================================================================
    @Nested
    @DisplayName("Manager overtime exemption")
    class ManagerExemption {

        @Test
        @DisplayName("A manager earns no overtime pay however many hours are logged")
        void managerEarnsNoOvertime() {
            Manager manager = manager("33000.00");

            assertEquals(Money.ZERO, manager.computeOvertimePay(8));
            assertEquals(Money.ZERO, manager.computeOvertimePay(40));

            // Gross for a manager is base pay only.
            assertEquals(Money.of("31428.80"), manager.calculateGrossPay(160, 8));
        }

        @Test
        @DisplayName("A regular employee on identical hours earns more gross than a manager")
        void regularOutEarnsManagerOnOvertime() {
            BigDecimal regularGross = regular("33000.00").calculateGrossPay(160, 8);
            BigDecimal managerGross = manager("33000.00").calculateGrossPay(160, 8);
            assertTrue(regularGross.compareTo(managerGross) > 0,
                       "Only the non-exempt employee should be paid for overtime");
        }

        @Test
        @DisplayName("Payroll flags the unpaid overtime hours for a manager")
        void payrollFlagsExemptOvertime() {
            Manager manager = manager("33000.00");
            PayrollRecord record = processor.processEmployeePayroll(
                    manager, approvedDays(manager.getEmployeeId(), 20, 9), START, END);

            assertTrue(record.getOvertimeHours() > 0, "Hours were logged");
            assertEquals(Money.ZERO, record.getOvertimePay(), "But none were paid");
            assertTrue(record.getExceptions().stream()
                            .anyMatch(n -> n.contains("overtime-exempt")),
                       "The payslip should explain why overtime was not paid");
        }
    }

    // ===================================================================
    //  FEEDBACK 3 -- polymorphism through an Employee reference
    // ===================================================================
    @Nested
    @DisplayName("Polymorphic dispatch through an Employee reference")
    class Polymorphism {

        @Test
        @DisplayName("One loop over Employee references produces three different results")
        void oneLoopThreeBehaviours() {
            List<Employee> staff = List.of(
                    regular("33000.00"),
                    probationary("33000.00"),
                    manager("33000.00"));

            List<BigDecimal> grossResults = new ArrayList<>();
            List<BigDecimal> benefitResults = new ArrayList<>();

            for (Employee e : staff) {          // no instanceof, no casting
                grossResults.add(e.calculateGrossPay(160, 8));
                benefitResults.add(e.calculateTotalBenefits());
            }

            // Regular and probationary are paid the same overtime...
            assertEquals(grossResults.get(0), grossResults.get(1));
            // ...the manager is not.
            assertNotEquals(grossResults.get(0), grossResults.get(2));

            // All three benefit entitlements differ from one another.
            assertEquals(Money.of("3500.00"), benefitResults.get(0));  // all allowances
            assertEquals(Money.of("1500.00"), benefitResults.get(1));  // rice only
            assertEquals(Money.of("8500.00"), benefitResults.get(2));  // all + managerial
        }

        @Test
        @DisplayName("The processor accepts any subclass without modification")
        void processorIsTypeAgnostic() {
            for (Employee e : List.of(regular("25000.00"),
                                      probationary("25000.00"),
                                      manager("25000.00"))) {
                PayrollRecord record = processor.processEmployeePayroll(
                        e, approvedDays(e.getEmployeeId(), 20, 8), START, END);
                assertNotNull(record);
                assertTrue(record.getNetPay().signum() > 0);
            }
        }

        @Test
        @DisplayName("Employment type is reported from the runtime object")
        void employmentTypeIsRuntimeResolved() {
            Employee e1 = regular("20000.00");
            Employee e2 = probationary("20000.00");
            Employee e3 = manager("20000.00");
            assertEquals("Regular", e1.getEmploymentType());
            assertEquals("Probationary", e2.getEmploymentType());
            assertEquals("Manager", e3.getEmploymentType());
        }
    }

    // ===================================================================
    //  FEEDBACK 4 -- the probationary benefit rule is explicit
    // ===================================================================
    @Nested
    @DisplayName("Probationary benefit entitlement")
    class ProbationaryBenefits {

        @Test
        @DisplayName("Probationary staff receive the rice subsidy only")
        void riceSubsidyOnly() {
            ProbationaryEmployee e = probationary("22000.00");
            assertEquals(Money.of("1500.00"), e.calculateTotalBenefits());
        }

        @Test
        @DisplayName("Regular staff receive all three allowances")
        void regularReceivesAll() {
            assertEquals(Money.of("3500.00"), regular("22000.00").calculateTotalBenefits());
        }
    }

    // ===================================================================
    //  FEEDBACK 5 -- gross income excludes benefits; one net pay formula
    // ===================================================================
    @Nested
    @DisplayName("Gross income excludes benefits")
    class GrossExcludesBenefits {

        @Test
        @DisplayName("Benefits are absent from gross pay and present in net pay")
        void benefitsEnterOnlyAfterTax() {
            RegularEmployee e = regular("33000.00");
            PayrollRecord r = processor.processEmployeePayroll(
                    e, approvedDays(e.getEmployeeId(), 20, 9), START, END);

            // Gross is base + overtime only.
            assertEquals(r.getBasePay().add(r.getOvertimePay()), r.getGrossPay());

            // Net rebuilds exactly from the documented formula.
            BigDecimal expectedNet = r.getTaxableIncome()
                    .subtract(r.getWithholdingTax())
                    .add(r.getTotalBenefits());
            assertEquals(expectedNet, r.getNetPay());
        }

        @Test
        @DisplayName("computeTotalCompensation is gross plus benefits, for reporting only")
        void totalCompensationIsExplicit() {
            RegularEmployee e = regular("33000.00");
            assertEquals(e.calculateGrossPay(160, 8).add(e.calculateTotalBenefits()),
                         e.computeTotalCompensation(160, 8));
        }
    }

    // ===================================================================
    //  FEEDBACK 6 -- a worked end-to-end gross-to-net example
    // ===================================================================
    @Nested
    @DisplayName("End-to-end attendance to payslip")
    class EndToEnd {

        @Test
        @DisplayName("Worked example: regular employee, 160 regular hours + 8 overtime")
        void workedGrossToNetExample() {
            RegularEmployee e = regular("33000.00");

            // 20 approved days of 8 hours = 160 regular hours, plus one
            // separate approved 8-hour overtime block.
            List<AttendanceRecord> attendance = approvedDays(e.getEmployeeId(), 20, 9);
            PayrollRecord r = processor.processEmployeePayroll(e, attendance, START, END);

            assertEquals(160.0, r.getRegularHours(), 0.001);
            assertEquals(20.0, r.getOvertimeHours(), 0.001);

            // Every figure below is reproduced by hand in the worksheet.
            assertEquals(Money.of("1125.00"), r.getSssContribution());
            assertEquals(Money.of("495.00"), r.getPhilhealthContribution());
            assertEquals(Money.of("100.00"), r.getPagibigContribution());
            assertEquals(Money.of("1720.00"), r.getTotalStatutoryDeductions());
            assertEquals(Money.of("3500.00"), r.getTotalBenefits());

            // The identity that must always hold.
            assertEquals(r.getGrossPay().subtract(r.getTotalStatutoryDeductions()),
                         r.getTaxableIncome());
            assertEquals(r.getTaxableIncome()
                            .subtract(r.getWithholdingTax())
                            .add(r.getTotalBenefits()),
                         r.getNetPay());
        }

        @Test
        @DisplayName("A payslip can be generated and contains the net pay figure")
        void payslipRenders() {
            RegularEmployee e = regular("33000.00");
            PayrollRecord r = processor.processEmployeePayroll(
                    e, approvedDays(e.getEmployeeId(), 20, 8), START, END);

            String slip = Payslip.generatePayslipSummary(e, r);
            assertTrue(slip.contains("NET PAY"));
            assertTrue(slip.contains("Regular"));
            assertTrue(slip.contains(e.getEmployeeId()));
        }
    }

    // ===================================================================
    //  FEEDBACK 7 -- attendance edge cases
    // ===================================================================
    @Nested
    @DisplayName("Attendance validation")
    class AttendanceValidation {

        @Test
        @DisplayName("An incomplete punch contributes zero hours and raises an exception note")
        void incompletePunchIsReported() {
            RegularEmployee e = regular("33000.00");
            List<AttendanceRecord> days = new ArrayList<>();

            AttendanceRecord missing = new AttendanceRecord(e.getEmployeeId(), START);
            missing.setTimeIn(LocalTime.of(8, 0));   // never clocked out
            days.add(missing);

            PayrollRecord r = processor.processEmployeePayroll(e, days, START, END);

            assertEquals(0.0, r.getRegularHours(), 0.001);
            assertTrue(r.hasExceptions());
            assertTrue(r.getExceptions().get(0).contains("Incomplete attendance"));
        }

        @Test
        @DisplayName("An overnight shift is measured across midnight when flagged")
        void overnightShiftIsMeasuredForward() {
            AttendanceRecord night = new AttendanceRecord("10001", START);
            night.setNightShift(true);
            night.setTimeIn(LocalTime.of(22, 0));
            night.setTimeOut(LocalTime.of(6, 0));    // next morning

            assertEquals(8.0, night.getHoursWorked(), 0.001);
        }

        @Test
        @DisplayName("A backwards time out is rejected when it is not a night shift")
        void backwardsTimeOutIsRejected() {
            AttendanceRecord typo = new AttendanceRecord("10001", START);
            typo.setTimeIn(LocalTime.of(8, 0));
            assertThrows(IllegalArgumentException.class,
                         () -> typo.setTimeOut(LocalTime.of(6, 0)));
        }

        @Test
        @DisplayName("Overtime is not paid until a manager approves the timecard")
        void unapprovedOvertimeIsNotPaid() {
            AttendanceRecord day = new AttendanceRecord("10001", START);
            day.setTimeIn(LocalTime.of(8, 0));
            day.setTimeOut(LocalTime.of(18, 0));     // 10 hours

            assertEquals(8.0, day.getRegularHours(), 0.001);
            assertEquals(0.0, day.getOvertimeHours(), 0.001);

            manager("33000.00").approveTimecard(day);
            assertEquals(2.0, day.getOvertimeHours(), 0.001);
        }
    }

    // ===================================================================
    //  FEEDBACK 8 -- money is BigDecimal, encapsulation holds
    // ===================================================================
    @Nested
    @DisplayName("Money precision and encapsulation")
    class MoneyAndEncapsulation {

        @Test
        @DisplayName("Every monetary figure carries exactly two decimal places")
        void scaleIsAlwaysTwo() {
            RegularEmployee e = regular("33000.00");
            PayrollRecord r = processor.processEmployeePayroll(
                    e, approvedDays(e.getEmployeeId(), 20, 9), START, END);

            for (BigDecimal amount : List.of(r.getBasePay(), r.getOvertimePay(),
                    r.getGrossPay(), r.getTotalStatutoryDeductions(),
                    r.getWithholdingTax(), r.getTotalBenefits(), r.getNetPay())) {
                assertEquals(2, amount.scale(), "Scale must be 2 for " + amount);
            }
        }

        @Test
        @DisplayName("Government identifiers are masked by default")
        void identifiersAreMasked() {
            RegularEmployee e = regular("33000.00");
            e.setTinNumber("442-605-657-000");
            assertTrue(e.getTinNumber().endsWith("-000"));
            assertTrue(e.getTinNumber().startsWith("*"));
            assertEquals("442-605-657-000", e.getTinNumberRaw());
        }

        @Test
        @DisplayName("Setters reject invalid payroll data")
        void settersValidate() {
            RegularEmployee e = regular("33000.00");
            assertThrows(IllegalArgumentException.class,
                         () -> e.setBasicSalary(Money.of("-1.00")));
            assertThrows(IllegalArgumentException.class, () -> e.setLastName("  "));
            assertThrows(IllegalArgumentException.class,
                         () -> e.calculateGrossPay(-1, 0));
        }

        @Test
        @DisplayName("Pag-IBIG never exceeds the statutory ceiling")
        void pagibigIsCapped() {
            assertEquals(StatutoryDeductions.PAGIBIG_CAP,
                         StatutoryDeductions.pagibig(Money.of("90000.00")));
        }

        @Test
        @DisplayName("No tax is withheld below the exemption threshold")
        void lowIncomeIsTaxFree() {
            assertEquals(Money.ZERO,
                         StatutoryDeductions.withholdingTax(Money.of("20000.00")));
        }
    }

    // ===================================================================
    //  Fixtures
    // ===================================================================

    private static RegularEmployee regular(String salary) {
        RegularEmployee e = new RegularEmployee("10001", "Dela Cruz", "Juan");
        applyStandardPay(e, salary);
        return e;
    }

    private static ProbationaryEmployee probationary(String salary) {
        ProbationaryEmployee e = new ProbationaryEmployee("10002", "Reyes", "Maria");
        applyStandardPay(e, salary);
        return e;
    }

    private static Manager manager(String salary) {
        Manager m = new Manager("10003", "Santos", "Antonio");
        applyStandardPay(m, salary);
        m.setManagerialAllowance(Money.of("5000.00"));
        return m;
    }

    private static void applyStandardPay(Employee e, String salary) {
        e.setBasicSalary(Money.of(salary));
        e.setRiceSubsidy(Money.of("1500.00"));
        e.setPhoneAllowance(Money.of("1000.00"));
        e.setClothingAllowance(Money.of("1000.00"));
    }

    /** n consecutive weekday records of the given length, all manager-approved. */
    private static List<AttendanceRecord> approvedDays(String employeeId, int days, int hours) {
        Manager approver = manager("50000.00");
        List<AttendanceRecord> list = new ArrayList<>();
        LocalDate day = START;
        int added = 0;
        while (added < days) {
            if (day.getDayOfWeek().getValue() <= 5) {
                AttendanceRecord r = new AttendanceRecord(employeeId, day);
                r.setTimeIn(LocalTime.of(8, 0));
                r.setTimeOut(LocalTime.of(8, 0).plusHours(hours));
                approver.approveTimecard(r);
                list.add(r);
                added++;
            }
            day = day.plusDays(1);
        }
        return list;
    }
}
