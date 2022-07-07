package com.learning.common.crud.entity;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Setter
@ToString
@FieldNameConstants
@BatchSize(size = 1000)
public class Phone {

  @Id
  private UUID id;

  private String type;

  private String number;

  private String areaCode;

  @ManyToOne
  @JoinColumn(name = "OWNER_ID")
  @Exclude
  private Employee owner;
}
