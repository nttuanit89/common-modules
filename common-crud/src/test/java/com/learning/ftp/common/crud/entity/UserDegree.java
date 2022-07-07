package com.learning.ftp.common.crud.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name = "user_degree")
public class UserDegree {
  @EmbeddedId
  private UserDegreeId id;

  @ManyToOne (fetch = FetchType.LAZY)
  @MapsId("userId")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("degreeId")
  private Degree degree;

  public UserDegree() {}

  public UserDegree(User user, Degree degree) {
    this.user = user;
    this.degree = degree;
    this.id = new UserDegreeId(user.getId(), degree.getId());
  }

  public UserDegreeId getId() {
    return id;
  }

  public void setId(UserDegreeId id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Degree getDegree() {
    return degree;
  }

  public void setDegree(Degree degree) {
    this.degree = degree;
  }
}
