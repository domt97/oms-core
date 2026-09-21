package com.dotran.oms.core.domain.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SKU extends BaseId<String> {

    public static SKU of(String value) {
        return new SKU(value);
    }

    @JsonCreator
    public SKU(@JsonProperty("value") String value) {
        super(value);
    }
}
