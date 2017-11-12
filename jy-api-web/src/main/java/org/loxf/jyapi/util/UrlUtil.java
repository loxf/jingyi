package org.loxf.jyapi.util;

import org.apache.commons.lang3.StringUtils;

public class UrlUtil {
    public static void main(String [] args ){
        System.out.println(inputURL("http://lsakj.com?recommend=12325&id=ackdjs2", "recommend", "asgas"));
    }
    public static String inputURL(String url,String paramName,String paramValue){
        //参数和参数名为空的话就返回原来的URL
        if(StringUtils.isBlank(paramValue) || StringUtils.isBlank(paramName)){
            return url;
        }
        //先很据# ? 将URL拆分成一个String数组
        String a = "";
        String b = "";
        String c = "";

        String[] abcArray = url.split("[?]");
        a = abcArray[0];
        if (abcArray.length > 1) {
            String bc = abcArray[1];
            String[] bcArray = bc.split("#");
            b = bcArray[0];
            if (bcArray.length > 1) {
                c = bcArray[1];
            }
        }
        if (StringUtils.isBlank(b)) {
            return url;
        }

        // 用&拆p, p1=1&p2=2 ，{p1=1,p2=2}
        String[] bArray = b.split("&");
        String newb = "";
        boolean found = false;
        for (int i = 0; i < bArray.length; i++) {
            String bi = bArray[i];
            if (StringUtils.isBlank(bi))
                continue;
            String key = "";
            String value = "";

            String[] biArray = bi.split("="); // {p1,1}
            key = biArray[0];
            if (biArray.length > 1)
                value = biArray[1];

            if (key.equals(paramName)) {
                found = true;
                if(StringUtils.isNotBlank(newb)){
                    newb += "&";
                }
                if (StringUtils.isNotBlank(paramValue)) {
                    newb += key + "=" + paramValue;
                } else {
                    continue;
                }
            } else {
                if(StringUtils.isNotBlank(newb)){
                    newb += "&";
                }
                newb += key + "=" + value;
            }
        }
        // 如果没找到，加上
        if (!found && StringUtils.isNotBlank(paramValue)) {
            if(StringUtils.isNotBlank(newb)){
                newb += "&";
            }
            newb += paramName + "=" + paramValue;
        }
        if (StringUtils.isNotBlank(newb))
            a = a + "?" + newb;
        if (StringUtils.isNotBlank(c))
            a = a + "#" + c;
        return a;
    }
}
