package com.learning.common.crud.entity;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Setter
@FieldNameConstants
@ToString
public class Employee {

  @Id
  private UUID id;

  private String firstName;

  private String lastName;

  private Double salary;

  private Instant start;

  private ZonedDateTime birthday;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name= "ADDRESS_ID")
  private Address address;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner")
  @BatchSize(size = 1000)
  private List<Phone> phones;

  @ManyToOne
  @JoinColumn(name = "MANAGER_ID")
  private Employee manager;

  @OneToMany(mappedBy = "manager")
  private List<Employee> employees;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "EMPLOYEE_PROJECT",
      joinColumns = {@JoinColumn(name = "EMPLOYEE_ID", referencedColumnName = "ID")},
      inverseJoinColumns = {@JoinColumn(name = "PROJECT_ID", referencedColumnName = "ID")})
  private List<Project> projects;
}
