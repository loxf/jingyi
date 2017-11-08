package org.loxf.jyapi.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.EncryptionKeyUtil;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.loxf.jyadmin.client.dto.AdminDto;
import org.loxf.jyadmin.client.dto.CustDto;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil {
    public static String TOKEN_PREFIX = "JY_USER";
    public static String TOKEN_SPLIT = "#JY#";

    public static void setCookie(HttpServletResponse response, String cookieName, Object value) {
        Cookie cookie = new Cookie(cookieName, JSONObject.toJSONString(value));
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);
    }

    public static void rmCookie(HttpServletRequest request, HttpServletResponse response, String cookieName){
        Cookie[] cookies = request.getCookies();
        if (null!=cookies) {
            for(Cookie cookie : cookies){
                if(cookie.getName().equals(cookieName)){
                    cookie.setValue(null);
                    cookie.setMaxAge(0);// 立即销毁cookie
                    cookie.setPath("/");
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        Cookie cookie = null;
        if (null == cookies) {
            return null;
        }
        for (Cookie c : cookies) {
            if (cookieName.equalsIgnoreCase(c.getName())) {
                cookie = c;
                break;
            }
        }
        //cookie不存在,session为空
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    public static Object getCookieValue(HttpServletRequest request, String cookieName, Class clazz) {
        Cookie[] cookies = request.getCookies();
        Cookie cookie = null;
        if (null == cookies) {
            return null;
        }
        for (Cookie c : cookies) {
            if (cookieName.equalsIgnoreCase(c.getName())) {
                cookie = c;
                break;
            }
        }
        //cookie不存在,session为空
        if (cookie == null) {
            return null;
        }
        return JSON.parseObject(cookie.getValue(), clazz);
    }

    public static void setSession(HttpServletRequest request, String key, Object value){
        request.getSession().setAttribute(key, value);
    }

    public static Object getSession(HttpServletRequest request, String sessionName){
        Object sessionValue = request.getSession().getAttribute(sessionName);
        if(sessionValue==null){
            return null;
        }
        return sessionValue;
    }

    public static CustDto getCust(HttpServletRequest request){
        String token = getUserToken(request);
        if(StringUtils.isBlank(token)){
            return null;
        }
        String str = SpringApplicationContextUtil.getBean(JedisUtil.class).get(token);
        if(StringUtils.isBlank(str)){
            return null;
        }
        return JSON.parseObject(str, CustDto.class);
    }

    public static String getUserToken(HttpServletRequest request){
        Object sessionValue = getSession(request, BaseConstant.USER_COOKIE_NAME);
        if(sessionValue==null){
            return getCookieValue(request, BaseConstant.USER_COOKIE_NAME);
        }
        return sessionValue.toString();
    }

    public static String encrypt(String str) throws Exception {
        return EncryptionKeyUtil.encryption(str);
    }

    public static String decrypt(String str) throws Exception {
        return EncryptionKeyUtil.decryption(str);
    }
}
