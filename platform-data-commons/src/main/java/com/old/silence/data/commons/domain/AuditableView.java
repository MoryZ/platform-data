package com.old.silence.data.commons.domain;

import java.time.Instant;

/**
 * @author moryzang
 */
public interface AuditableView {

    String getCreatedBy();

    Instant getCreatedDate();

    String getUpdatedBy();

    Instant getUpdatedDate();
}
