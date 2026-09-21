package com.dotran.oms.core.domain.id;

import lombok.Getter;

@Getter
public class ProductImageId extends BaseId<Long> {

    public ProductImageId(Long value) {
        super(value);
    }

    public static ProductImageId of(Long value) {
        return new ProductImageId(value);
    }
}
