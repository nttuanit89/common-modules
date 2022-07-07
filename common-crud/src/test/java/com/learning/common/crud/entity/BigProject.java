package com.learning.common.crud.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
public class BigProject extends Project {

  private Double budget;
}
