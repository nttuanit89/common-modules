package com.learning.ftp.common.crud.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Address {

  @Id
  private UUID id;

  private String street;

  private String city;

  private String province;
}
