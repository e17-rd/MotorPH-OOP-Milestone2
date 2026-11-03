package com.motorph.payroll;

import com.motorph.payroll.model.*;
import com.motorph.payroll.service.PayrollProcessor;
import com.motorph.payroll.util.Money;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end demonstration: attendance in, payslip out.
 *
 * This is the console equivalent of the GUI flow. The GUI builds the same
 * objects from its form fields and calls the same processor; it performs no
 * arithmetic of its own.
 */
public final class MotorPHPayrollDemo {

    public static void main(String[] args) {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        PayrollProcessor processor = new PayrollProcessor();

        Manager manager = buildManager();

        // POLYMORPHISM: one list, one loop, three different pay behaviours.
        List<Employee> payrollRun = List.of(
                buildRegular(),
                buildProbationary(),
                manager
        );

        for (Employee employee : payrollRun) {
            List<AttendanceRecord> attendance =
                    buildMonthAttendance(employee.getEmployeeId(), start, manager);
            PayrollRecord record =
                    processor.processEmployeePayroll(employee, attendance, start, end);
            System.out.println(Payslip.generatePayslipSummary(employee, record));
        }
    }

    private static RegularEmployee buildRegular() {
        RegularEmployee e = new RegularEmployee("10001", "Dela Cruz", "Juan");
        e.setPosition("Rank and File");
        e.setSssNumber("44-4506057-3");
        e.setPhilhealthNumber("820126853951");
        e.setTinNumber("442-605-657-000");
        e.setPagibigNumber("691295330870");
        e.setBasicSalary(Money.of("33000.00"));
        e.setRiceSubsidy(Money.of("1500.00"));
        e.setPhoneAllowance(Money.of("1000.00"));
        e.setClothingAllowance(Money.of("1000.00"));
        return e;
    }

    private static ProbationaryEmployee buildProbationary() {
        ProbationaryEmployee e = new ProbationaryEmployee("10002", "Reyes", "Maria");
        e.setPosition("Junior Associate");
        e.setSssNumber("44-4506057-4");
        e.setPhilhealthNumber("820126853952");
        e.setTinNumber("442-605-657-001");
        e.setPagibigNumber("691295330871");
        e.setBasicSalary(Money.of("22000.00"));
        e.setRiceSubsidy(Money.of("1500.00"));
        e.setPhoneAllowance(Money.of("1000.00"));
        e.setClothingAllowance(Money.of("1000.00"));
        return e;
    }

    private static Manager buildManager() {
        Manager m = new Manager("10003", "Santos", "Antonio");
        m.setPosition("Operations Manager");
        m.setSssNumber("44-4506057-5");
        m.setPhilhealthNumber("820126853953");
        m.setTinNumber("442-605-657-002");
        m.setPagibigNumber("691295330872");
        m.setBasicSalary(Money.of("33000.00"));
        m.setRiceSubsidy(Money.of("1500.00"));
        m.setPhoneAllowance(Money.of("1000.00"));
        m.setClothingAllowance(Money.of("1000.00"));
        m.setManagerialAllowance(Money.of("5000.00"));
        return m;
    }

    /** 20 working days of 8 hours, plus one 9-hour day approved for overtime. */
    private static List<AttendanceRecord> buildMonthAttendance(String employeeId,
                                                               LocalDate start,
                                                               Manager approver) {
        List<AttendanceRecord> list = new ArrayList<>();
        LocalDate day = start;
        int added = 0;
        while (added < 20) {
            if (day.getDayOfWeek().getValue() <= 5) {
                AttendanceRecord r = new AttendanceRecord(employeeId, day);
                r.setTimeIn(LocalTime.of(8, 0));
                r.setTimeOut(LocalTime.of(17, 0)); // 9 hours minus no break modelled
                approver.approveTimecard(r);
                list.add(r);
                added++;
            }
            day = day.plusDays(1);
        }
        return list;
    }
}
