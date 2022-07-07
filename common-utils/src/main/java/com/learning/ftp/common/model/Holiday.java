package com.learning.ftp.common.model;

public class Holiday {

  private final long startDate;

  private final long endDate;

  public Holiday(long startDate, long endDate) {
    if (startDate > endDate) {
      throw new IllegalArgumentException("startDate is greater than endDate");
    }
    this.startDate = startDate;
    this.endDate = endDate;
  }

  /**
   * Get start date of this holiday
   * @return the number of milliseconds since the epoch of 1970-01-01T00:00:00Z
   */
  public long getStartDate() {
    return startDate;
  }

  /**
   * Get end date of this holiday
   * @return the number of milliseconds since the epoch of 1970-01-01T00:00:00Z
   */
  public long getEndDate() {
    return endDate;
  }
}
