package com.old.silence.data.commons.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * @author murrayZhang
 */
public class AuditableVo {

    private String createdBy;
    private Instant createdDate;
    private String updatedBy;
    private Instant updatedDate;

    Optional<String> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    Optional<Instant> getCreatedDate() {
       return Optional.ofNullable(createdDate);
    }

    Optional<String> getUpdatedBy() {
        return Optional.ofNullable(updatedBy);
    }

    Optional<Instant> getUpdatedDate() {
        return Optional.ofNullable(updatedDate);
    }
}
