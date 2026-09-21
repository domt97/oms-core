package com.dotran.oms.core.domain;

import com.dotran.oms.core.domain.id.TenantId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TenantInfo extends AggregateRoot<TenantId> {

    private String code;
    private String name;
    private String status;
    private TenantSetting settings;
    private Instant createdAt;
    private Instant updatedAt;
}
