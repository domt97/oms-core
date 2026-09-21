package com.dotran.oms.core.domain;

import lombok.experimental.SuperBuilder;

/*
 This is just a marker class to distinguish entities from the aggregate entity
 */
@SuperBuilder
public abstract class AggregateRoot<ID> extends BaseDomain<ID> {
}
