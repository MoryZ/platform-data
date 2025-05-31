package com.old.silence.data.commons.domain;

import org.springframework.lang.Nullable;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;


/**
 * @author MurrayZhang
 */
public abstract class AbstractAuditable<ID extends Serializable> implements Serializable {

    private static final long serialVersionUID = -8981379469380861749L;

    @Nullable
    @TableId(type = IdType.ASSIGN_ID)
    private ID id;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String createdBy;

    @TableField(value = "CREATED_DATE", updateStrategy = FieldStrategy.NEVER)
    private Instant createdDate;

    @TableField(value = "UPDATED_BY")
    private String updatedBy;

    @Version
    @TableField(value = "UPDATED_DATE")
    private Instant updatedDate;

    @Nullable
    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }


    public Optional<String> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Optional<Instant> getCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Optional<String> getUpdatedBy() {
        return Optional.ofNullable(updatedBy);
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Optional<Instant> getUpdatedDate() {
        return Optional.ofNullable(updatedDate);
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public int hashCode() {
        int hashCode = 17;
        ID id = getId();
        hashCode += null == id ? 0 : id.hashCode() * 31;
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractAuditable<?> other = (AbstractAuditable<?>) obj;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public String toString() {
        return String.format("Entity of type %s with id: %s", this.getClass().getName(), getId());
    }
}
