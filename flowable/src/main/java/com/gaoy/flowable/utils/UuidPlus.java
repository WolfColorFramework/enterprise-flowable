package com.gaoy.flowable.utils;

import java.util.UUID;

public class UuidPlus {

    //去除UUID中的横岗
    public static String getUUIDPlus() {
        String src = UUID.randomUUID().toString();
        return src.substring(0, 8) + src.substring(9, 13) + src.substring(14, 18) + src.substring(19, 23) + src.substring(24, 36);
    }

}
