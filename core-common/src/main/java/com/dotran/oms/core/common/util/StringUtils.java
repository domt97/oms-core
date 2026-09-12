package com.dotran.oms.core.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {

    public static boolean isNullOrEmpty(String str) {
        return isNull(str) || isEmpty(str);
    }

    public static boolean isNull(String str) {
        return null == str;
    }

    public static boolean isEmpty(String str) {
        return str.isEmpty();
    }
}
