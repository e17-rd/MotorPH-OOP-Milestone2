package com.motorph.payroll.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One day of attendance for one employee.
 *
 * MILESTONE 1 FEEDBACK FIX -- incomplete punches and overnight shifts.
 * The mentor flagged that "setTimeOut() requires timeOut after timeIn; the
 * model does not explain overnight shifts or missing punches."
 *
 * Both cases are now defined explicitly:
 *
 *   Missing punch   -- a record with a null timeIn or timeOut is INCOMPLETE.
 *                      It never throws; it simply contributes zero hours and
 *                      is reported by PayrollProcessor as an exception so the
 *                      payroll clerk can chase it.
 *   Overnight shift -- when timeOut is earlier in the clock than timeIn, the
 *                      shift is treated as crossing midnight and the duration
 *                      is measured to the following day. This is only allowed
 *                      when the record is explicitly flagged as a night shift,
 *                      so an ordinary typo is still caught as invalid.
 */
public class AttendanceRecord {

    /** Hours beyond this in a single day count as overtime. */
    public static final double REGULAR_HOURS_PER_DAY = 8.0;

    /** Grace period, in minutes, before an employee is considered late. */
    public static final int GRACE_PERIOD_MINUTES = 10;

    /** Standard MotorPH shift start. */
    public static final LocalTime STANDARD_START = LocalTime.of(8, 0);

    private final String employeeId;
    private final LocalDate date;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private boolean nightShift;
    private boolean approved;
    private String approvedBy;

    public AttendanceRecord(String employeeId, LocalDate date) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Attendance date is required.");
        }
        this.employeeId = employeeId;
        this.date = date;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(LocalTime timeIn) {
        this.timeIn = timeIn;
    }

    public LocalTime getTimeOut() {
        return timeOut;
    }

    /**
     * Records the time out.
     * A timeOut earlier than timeIn is rejected UNLESS this record has been
     * flagged as a night shift, in which case it legitimately crosses midnight.
     */
    public void setTimeOut(LocalTime timeOut) {
        if (timeOut != null && timeIn != null && timeOut.isBefore(timeIn) && !nightShift) {
            throw new IllegalArgumentException(
                    "Time out is before time in on " + date
                            + ". Flag this record as a night shift if the shift crosses midnight.");
        }
        this.timeOut = timeOut;
    }

    public boolean isNightShift() {
        return nightShift;
    }

    /** Marks this record as a shift that legitimately crosses midnight. */
    public void setNightShift(boolean nightShift) {
        this.nightShift = nightShift;
    }

    /** True only when both punches are present. */
    public boolean isComplete() {
        return timeIn != null && timeOut != null;
    }

    /** Total hours on the clock. Returns 0.0 for an incomplete record. */
    public double getHoursWorked() {
        if (!isComplete()) {
            return 0.0;
        }
        Duration duration = Duration.between(timeIn, timeOut);
        if (duration.isNegative() || duration.isZero()) {
            // Crosses midnight: measure forward to the next day.
            duration = duration.plusHours(24);
        }
        return duration.toMinutes() / 60.0;
    }

    /** Hours counted at the plain hourly rate, capped at the daily limit. */
    public double getRegularHours() {
        return Math.min(getHoursWorked(), REGULAR_HOURS_PER_DAY);
    }

    /**
     * Hours counted at the overtime premium.
     * Unapproved overtime is not paid -- a manager must approve the timecard.
     */
    public double getOvertimeHours() {
        double excess = getHoursWorked() - REGULAR_HOURS_PER_DAY;
        if (excess <= 0 || !approved) {
            return 0.0;
        }
        return excess;
    }

    /** True when the employee clocked in after the grace period. */
    public boolean isLate() {
        return timeIn != null && timeIn.isAfter(STANDARD_START.plusMinutes(GRACE_PERIOD_MINUTES));
    }

    public boolean isApproved() {
        return approved;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    /** Called by Manager.approveTimecard(). */
    void markApproved(String managerId) {
        this.approved = true;
        this.approvedBy = managerId;
    }

    @Override
    public String toString() {
        return date + " " + employeeId + " "
                + (isComplete() ? timeIn + "-" + timeOut : "INCOMPLETE");
    }
}
