package com.motorph.payroll.ui;

import com.motorph.payroll.model.*;
import com.motorph.payroll.service.PayrollProcessor;
import com.motorph.payroll.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MotorPH payroll GUI.
 *
 * MILESTONE 2 REQUIREMENT: "No GUI-side computation."
 *
 * This class does exactly three things:
 *   1. Reads the form fields and builds the correct Employee subclass.
 *   2. Builds the AttendanceRecord list for the period.
 *   3. Hands both to PayrollProcessor and displays what comes back.
 *
 * There is no payroll arithmetic anywhere in this file. Search it for a
 * multiplication or a tax bracket and you will not find one. Every peso
 * figure shown below is read from the PayrollRecord the processor returned.
 */
public class PayrollFrame extends JFrame {

    private final JTextField employeeIdField = new JTextField("10001", 10);
    private final JTextField lastNameField = new JTextField("Dela Cruz", 12);
    private final JTextField firstNameField = new JTextField("Juan", 12);
    private final JComboBox<String> employmentTypeBox =
            new JComboBox<>(new String[]{"Regular", "Probationary", "Manager"});
    private final JTextField positionField = new JTextField("Rank and File", 14);

    private final JTextField basicSalaryField = new JTextField("33000.00", 10);
    private final JTextField riceSubsidyField = new JTextField("1500.00", 10);
    private final JTextField phoneAllowanceField = new JTextField("1000.00", 10);
    private final JTextField clothingAllowanceField = new JTextField("1000.00", 10);
    private final JTextField managerialAllowanceField = new JTextField("5000.00", 10);

    private final JTextField daysWorkedField = new JTextField("20", 5);
    private final JTextField hoursPerDayField = new JTextField("9", 5);
    private final JCheckBox overtimeApprovedBox = new JCheckBox("Overtime approved by manager", true);

    private final JTextArea outputArea = new JTextArea(22, 52);
    private final JLabel statusLabel = new JLabel(" ");

    /** The backend. The GUI owns a reference to it and nothing else. */
    private final PayrollProcessor processor = new PayrollProcessor();

    public PayrollFrame() {
        super("MotorPH Payroll System - Milestone 2");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildInputPanel(), BorderLayout.WEST);
        add(buildOutputPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        employmentTypeBox.addActionListener(e -> syncManagerialField());
        syncManagerialField();

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Employee"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addRow(panel, c, row, "Employee ID:", employeeIdField);
        row = addRow(panel, c, row, "Last name:", lastNameField);
        row = addRow(panel, c, row, "First name:", firstNameField);
        row = addRow(panel, c, row, "Employment type:", employmentTypeBox);
        row = addRow(panel, c, row, "Position:", positionField);
        row = addRow(panel, c, row, "Basic salary (monthly):", basicSalaryField);
        row = addRow(panel, c, row, "Rice subsidy:", riceSubsidyField);
        row = addRow(panel, c, row, "Phone allowance:", phoneAllowanceField);
        row = addRow(panel, c, row, "Clothing allowance:", clothingAllowanceField);
        row = addRow(panel, c, row, "Managerial allowance:", managerialAllowanceField);
        row = addRow(panel, c, row, "Days worked:", daysWorkedField);
        row = addRow(panel, c, row, "Hours per day:", hoursPerDayField);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        panel.add(overtimeApprovedBox, c);
        return panel;
    }

    private int addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        panel.add(field, c);
        return row + 1;
    }

    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Payslip"));
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            outputArea.setText("");
            setStatus(" ", false);
        });

        JButton computeButton = new JButton("Compute Payroll");
        computeButton.addActionListener(e -> onComputePayroll());
        getRootPane().setDefaultButton(computeButton);

        buttons.add(clearButton);
        buttons.add(computeButton);

        statusLabel.setBorder(new EmptyBorder(0, 6, 0, 0));
        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private void syncManagerialField() {
        boolean isManager = "Manager".equals(employmentTypeBox.getSelectedItem());
        managerialAllowanceField.setEnabled(isManager);
    }

    /**
     * The compute button's event listener.
     * Builds objects, calls the processor, renders the result. No arithmetic.
     */
    private void onComputePayroll() {
        try {
            Employee employee = buildEmployeeFromForm();
            LocalDate start = LocalDate.now().withDayOfMonth(1);
            LocalDate end = start.plusMonths(1).minusDays(1);

            List<AttendanceRecord> attendance = buildAttendanceFromForm(employee.getEmployeeId(), start);

            // The one and only call into the payroll backend.
            PayrollRecord record = processor.processEmployeePayroll(employee, attendance, start, end);

            outputArea.setText(Payslip.generatePayslipSummary(employee, record));
            outputArea.setCaretPosition(0);

            if (record.hasExceptions()) {
                setStatus("Payroll computed with " + record.getExceptions().size()
                        + " note(s) - see the bottom of the payslip.", false);
            } else {
                setStatus("Payroll computed successfully.", false);
            }
        } catch (IllegalArgumentException ex) {
            setStatus(ex.getMessage(), true);
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            setStatus("Unexpected error: " + ex, true);
        }
    }

    /** Chooses the subclass from the form. This is where polymorphism starts. */
    private Employee buildEmployeeFromForm() {
        String id = required(employeeIdField.getText(), "Employee ID");
        String last = required(lastNameField.getText(), "Last name");
        String first = required(firstNameField.getText(), "First name");
        String type = String.valueOf(employmentTypeBox.getSelectedItem());

        Employee employee;
        switch (type) {
            case "Probationary" -> employee = new ProbationaryEmployee(id, last, first);
            case "Manager" -> employee = new Manager(id, last, first);
            default -> employee = new RegularEmployee(id, last, first);
        }

        employee.setPosition(positionField.getText().trim());
        employee.setBasicSalary(money(basicSalaryField.getText(), "Basic salary"));
        employee.setRiceSubsidy(money(riceSubsidyField.getText(), "Rice subsidy"));
        employee.setPhoneAllowance(money(phoneAllowanceField.getText(), "Phone allowance"));
        employee.setClothingAllowance(money(clothingAllowanceField.getText(), "Clothing allowance"));

        if (employee instanceof Manager manager) {
            manager.setManagerialAllowance(money(managerialAllowanceField.getText(), "Managerial allowance"));
        }
        return employee;
    }

    private List<AttendanceRecord> buildAttendanceFromForm(String employeeId, LocalDate start) {
        int days = integer(daysWorkedField.getText(), "Days worked");
        int hours = integer(hoursPerDayField.getText(), "Hours per day");
        if (days < 0 || days > 31) {
            throw new IllegalArgumentException("Days worked must be between 0 and 31.");
        }
        if (hours < 1 || hours > 16) {
            throw new IllegalArgumentException("Hours per day must be between 1 and 16.");
        }

        Manager approver = new Manager("APPROVER", "System", "Timekeeping");
        List<AttendanceRecord> list = new ArrayList<>();
        LocalDate day = start;
        int added = 0;
        while (added < days) {
            if (day.getDayOfWeek().getValue() <= 5) {
                AttendanceRecord record = new AttendanceRecord(employeeId, day);
                record.setTimeIn(LocalTime.of(8, 0));
                record.setTimeOut(LocalTime.of(8, 0).plusHours(hours));
                if (overtimeApprovedBox.isSelected()) {
                    approver.approveTimecard(record);
                }
                list.add(record);
                added++;
            }
            day = day.plusDays(1);
        }
        return list;
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setForeground(error ? new Color(0xB3261E) : new Color(0x1B5E20));
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private static BigDecimal money(String value, String label) {
        try {
            return Money.of(required(value, label));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a number, for example 33000.00");
        }
    }

    private static int integer(String value, String label) {
        try {
            return Integer.parseInt(required(value, label));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PayrollFrame().setVisible(true));
    }
}
