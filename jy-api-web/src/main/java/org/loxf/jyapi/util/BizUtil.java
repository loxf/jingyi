package org.loxf.jyapi.util;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class BizUtil {
    public static Map createWXKeyWord(String value, String color){
        Map map = new HashMap();
        map.put("value", value);
        if(StringUtils.isNotBlank(color)){
            color = "#000000";
        }
        map.put("color", color);
        return map;
    }

    /**
     * @param templateId 模板ID
     * @param openId 接收人
     * @param data 格式<String, Object>：{"User":{ "value":"黄先生", "color":"#173177"}}
     * @param detailUrl 详情地址，可以为空
     * @return
     */
    public static Map createWxMsgMap(String templateId, String openId, Map data, String detailUrl){
        Map result = new HashMap();
        result.put("touser", openId);
        result.put("template_id", templateId);
        result.put("url", detailUrl);
        result.put("topcolor", "#00868B");
        result.put("data", data);
        return result ;
    }
}
