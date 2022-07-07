package com.learning.ftp.common.crud.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "user")
public class User {
  @Id
  private UUID id;

  private String name;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<UserDegree> degrees = new HashSet<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addDegree(Degree d) {
    UserDegree ud = new UserDegree(this, d);
    if (degrees.contains(ud)) {
      return;
    }
    degrees.add(ud);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(id, user.id);
  }

  public Set<UserDegree> getDegrees() {
    return degrees;
  }

  public void setDegrees(Set<UserDegree> degrees) {
    this.degrees = degrees;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
