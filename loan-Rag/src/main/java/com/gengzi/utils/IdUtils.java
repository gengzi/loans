package com.gengzi.utils;

import cn.hutool.core.util.IdUtil;

public class IdUtils {

    public static String generate() {
        return IdUtil.simpleUUID();
    }
}
