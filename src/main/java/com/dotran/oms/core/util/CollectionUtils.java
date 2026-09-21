package com.dotran.oms.core.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;

@UtilityClass
public class CollectionUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
