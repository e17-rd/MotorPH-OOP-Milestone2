# MotorPH Payroll System — Milestone 2 (OOP Refactor)

MO-IT110 Object-Oriented Programming · S2101 · Group 21

Refactor of the CP2 MotorPH payroll application into an object-oriented
backend. All payroll arithmetic lives in the backend; the GUI only collects
input and displays results.

## Structure

```
src/main/java/com/motorph/payroll/
├── model/
│   ├── Employee.java              abstract parent — the payroll contract
│   ├── RegularEmployee.java       rank-and-file: 1.25x overtime, all allowances
│   ├── ProbationaryEmployee.java  1.25x overtime, rice subsidy only
│   ├── Manager.java               overtime-exempt, + managerial allowance
│   ├── AttendanceRecord.java      one day; handles missing punches & night shifts
│   ├── PayrollRecord.java         immutable result holder (no calculation)
│   └── Payslip.java               formatting only
├── service/
│   ├── PayrollProcessor.java      THE single owner of payroll orchestration
│   └── StatutoryDeductions.java   SSS / PhilHealth / Pag-IBIG / withholding tax
├── ui/
│   └── PayrollFrame.java          Swing GUI — input and display only, no arithmetic
├── util/
│   └── Money.java                 BigDecimal helper; one rounding policy
└── MotorPHPayrollDemo.java        end-to-end attendance → payslip run
```

## Running the GUI

```bash
mvn compile
java -cp target/classes com.motorph.payroll.ui.PayrollFrame
```

The GUI builds the Employee subclass from the form, hands it to
`PayrollProcessor`, and prints what comes back. It performs no payroll
arithmetic of its own.

## The single payroll formula

```
1. regularHours, overtimeHours  ← summed from attendance records
2. basePay       = employee.computeBasePay(regularHours)
3. overtimePay   = employee.computeOvertimePay(overtimeHours)
4. grossPay      = basePay + overtimePay            ← benefits NOT included
5. statutory     = SSS + PhilHealth + Pag-IBIG      ← on monthly basic salary
6. taxableIncome = grossPay − statutory
7. withholdingTax= tax table applied to taxableIncome
8. benefits      = employee.calculateTotalBenefits()  ← non-taxable
9. netPay        = taxableIncome − withholdingTax + benefits
```

Overtime enters at step 3 and nowhere else. Benefits enter at step 8 and are
never taxed and never folded into gross pay.

## How the Milestone 1 feedback was addressed

| MS1 finding | Fix |
|---|---|
| `calculateGrossPay()` and `computeOvertimePay()` could double-count overtime | `calculateGrossPay()` is now `final` in `Employee` and is the only definition of the sum. Subclasses override only the parts. |
| `PayrollRecord` was both data holder and processor | `PayrollProcessor` owns orchestration. `PayrollRecord` is an immutable data holder with no arithmetic. |
| Unclear whether gross income includes benefits | It does not. Documented in `Employee.calculateGrossPay()` and `PayrollProcessor`. |
| Probationary benefit rule ambiguous (zero vs prorated) | One rule committed: rice subsidy in full, no phone or clothing allowance, no proration. |
| Manager overrides not fully documented | `Manager` lists both overrides explicitly: `computeOvertimePay()` (exempt) and `calculateTotalBenefits()` (+ managerial allowance). Everything else inherited unchanged. |
| `double` used for money | All currency is `BigDecimal` via `Money`, scale 2, HALF_UP, rounded at defined boundaries. |
| Overnight shifts and missing punches undefined | `AttendanceRecord` defines both: incomplete records contribute zero hours and raise an exception note; night shifts must be flagged and are measured across midnight. |
| No processing sequence shown | The nine-step formula above, mirrored in `PayrollProcessor`. |

## Build and test

```bash
mvn test        # runs the JUnit 5 suite
mvn compile exec:java -Dexec.mainClass=com.motorph.payroll.MotorPHPayrollDemo
```

## Worked example — Regular employee, ₱33,000 monthly basic

160 regular hours + 8 approved overtime hours.

| Line | Amount (PHP) |
|---|---|
| Hourly rate (33,000 ÷ 168) | 196.43 |
| Base pay (196.43 × 160) | 31,428.80 |
| Overtime pay (196.43 × 8 × 1.25) | 1,964.30 |
| **Gross pay** | **33,393.10** |
| SSS | 1,125.00 |
| PhilHealth (3% ÷ 2) | 495.00 |
| Pag-IBIG (2%, capped) | 100.00 |
| Total statutory | 1,720.00 |
| Taxable income | 31,673.10 |
| Withholding tax (20% over 20,833) | 2,168.02 |
| Benefits (rice + phone + clothing) | 3,500.00 |
| **Net pay** | **33,005.08** |

Same hours, Manager (overtime-exempt): gross 31,428.80 → net 36,433.64
Same hours, Probationary at ₱22,000: gross 22,261.50 → net 22,339.80

## Note on statutory rates

The SSS brackets, PhilHealth rate and tax table in `StatutoryDeductions` follow
the standard tables used in the MotorPH course data. Verify them against the
project's own reference sheet before the Terminal Assessment — they are policy
values, and changing one must never require changing the payroll flow.
