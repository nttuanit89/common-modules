package com.learning.common.crud.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Project {

  @Id
  private UUID id;

  private String name;

  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "projects")
  private List<Employee> employees;
}
