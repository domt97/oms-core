package com.dotran.oms.core.common.valueobject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PriceRange {

    private final Money from;
    private final Money to;
}
