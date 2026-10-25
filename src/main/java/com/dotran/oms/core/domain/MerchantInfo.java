package com.dotran.oms.core.domain;

import com.dotran.oms.core.domain.id.MerchantId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MerchantInfo extends AggregateRoot<MerchantId> {

    private String code;
    private String name;
    private String status;
    private MerchantSetting settings;
    private Instant createdAt;
    private Instant updatedAt;
}
