package com.learning.ftp.common.crud.entity;

import lombok.Data;

import java.time.Instant;

@Data
public class EmployeePeriod {

  private Instant start;
  private Instant end;
}
