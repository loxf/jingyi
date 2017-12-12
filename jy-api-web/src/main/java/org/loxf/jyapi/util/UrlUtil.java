package org.loxf.jyapi.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UrlUtil {
    public static void main(String[] args) {
        // System.out.println(inputURL("http://lsakj.com?recommend=12325&id=ackdjs2", "recommend", "asgas"));
        System.out.println(JSONObject.toJSONString(URLRequest("http://lsakj.com?recommend=12325&id=ackdjs2")));
    }

    /**
     * 去掉url中的路径，留下请求参数部分
     *
     * @param strURL url地址
     * @return url请求参数部分
     */
    private static String TruncateUrlPage(String strURL) {
        String strAllParam = null;
        String[] arrSplit = null;

        strURL = strURL.trim().toLowerCase();

        arrSplit = strURL.split("[?]");
        if (strURL.length() > 1) {
            if (arrSplit.length > 1) {
                if (arrSplit[1] != null) {
                    strAllParam = arrSplit[1];
                }
            }
        }
        return strAllParam;
    }

    /**
     * 解析出url参数中的键值对
     * 如 "index.jsp?Action=del&id=123"，解析出Action:del,id:123存入map中
     *
     * @param URL url地址
     * @return url请求参数部分
     */
    public static Map<String, String> URLRequest(String URL) {
        Map<String, String> mapRequest = new HashMap<String, String>();

        String[] arrSplit = null;

        String strUrlParam = TruncateUrlPage(URL);
        if (strUrlParam == null) {
            return mapRequest;
        }
        //每个键值为一组 www.2cto.com
        arrSplit = strUrlParam.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual = null;
            arrSplitEqual = strSplit.split("[=]");

            //解析出键值
            if (arrSplitEqual.length > 1) {
                //正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);

            } else {
                if (arrSplitEqual[0] != "") {
                    //只有参数没有值，不加入
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapRequest;
    }

    public static String inputURL(String url, String paramName, String paramValue) {
        //参数和参数名为空的话就返回原来的URL
        if (StringUtils.isBlank(paramValue) || StringUtils.isBlank(paramName)) {
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
                if (StringUtils.isNotBlank(newb)) {
                    newb += "&";
                }
                if (StringUtils.isNotBlank(paramValue)) {
                    newb += key + "=" + paramValue;
                } else {
                    continue;
                }
            } else {
                if (StringUtils.isNotBlank(newb)) {
                    newb += "&";
                }
                newb += key + "=" + value;
            }
        }
        // 如果没找到，加上
        if (!found && StringUtils.isNotBlank(paramValue)) {
            if (StringUtils.isNotBlank(newb)) {
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
