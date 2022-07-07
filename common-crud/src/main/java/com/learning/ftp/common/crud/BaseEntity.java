package com.learning.ftp.common.crud;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@MappedSuperclass
public class BaseEntity implements Serializable {
  @CreatedDate protected Timestamp createdTime;
  @LastModifiedDate protected Timestamp updatedTime;

  @Version protected Long version;

  @CreatedBy protected String createdBy;
  @LastModifiedBy protected String updatedBy;

  public Timestamp getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(Timestamp createdTime) {
    this.createdTime = createdTime;
  }

  public Timestamp getUpdatedTime() {
    return updatedTime;
  }

  public void setUpdatedTime(Timestamp updatedTime) {
    this.updatedTime = updatedTime;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }
}
