package com.learning.ftp.common.crud.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class EmployeeWithIdOnly {
  UUID id;
}
