package org.loxf.jyapi.util;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class BizUtil {
    public static Map createWXKeyWord(String value, String color){
        Map map = new HashMap();
        map.put("value", value);
        if(StringUtils.isNotBlank(color)){
            color = "000000";
        }
        map.put("color", color);
        return map;
    }
}
