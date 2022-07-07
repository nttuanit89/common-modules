package com.learning.ftp.common.crud.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class UserDegreeId implements Serializable {
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "degree_id")
  private UUID degreeId;

  public UserDegreeId() {}

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getDegreeId() {
    return degreeId;
  }

  public void setDegreeId(UUID degreeId) {
    this.degreeId = degreeId;
  }

  public UserDegreeId(UUID userId, UUID degreeId) {
    this.userId = userId;
    this.degreeId = degreeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserDegreeId that = (UserDegreeId) o;
    return Objects.equals(userId, that.userId) &&
        Objects.equals(degreeId, that.degreeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, degreeId);
  }
}
