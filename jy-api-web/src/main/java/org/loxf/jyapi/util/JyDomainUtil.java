package org.loxf.jyapi.util;

/**
 * @author hongjia.lhj
 */
public class JyDomainUtil {
    public static String getDomain(String url){
        if(url.indexOf("dev.jingyizaixian.com")>-1){
            return "jingyizaixian.com";
        } else if(url.indexOf("test.jingyizaixian.com")>-1){
            return "test.jingyizaixian.com";
        } else if(url.indexOf("www.jingyizaixian.com")>-1){
            return "www.jingyizaixian.com";
        } else {
            return "www.jingyizaixian.com";
        }
    }
}
