package org.loxf.jyapi.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Random;

public class BaseInterceptor extends HandlerInterceptorAdapter {
    private static Logger logger = LoggerFactory.getLogger(BaseInterceptor.class);
    private static String [] excludeUrl = {"/api/login"};
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断用户是否登录系统
        if(needFilter(request.getRequestURI())) {
            if (!hasLogin(request, response)) {
                // 未登录系统跳转到登录
                // 获取登录随机code，五分钟失效
                String code = getRandomCharAndNumr(8);
                String loginUrl = WeixinUtil.getLoginUrl(request.getRequestURL().toString(), code);
                // TODO 注释DEBUG
                if("JY123456QWE".equals(request.getParameter("XDebug"))){
                    loginUrl = String.format(BaseConstant.LOGIN_URL, URLEncoder.encode(request.getRequestURL().toString() + "?" + request.getQueryString(), "utf-8") ) + "&state=" + code + "&XDebug=IYUTERESGBXVCMSWB";
                    loginUrl = loginUrl.replaceAll("https://www.jingyizaixian.com", "http://127.0.0.1:8081");
                }
                SpringApplicationContextUtil.getBean(JedisUtil.class).set(code, (System.currentTimeMillis()+ 5 * 60 * 1000) + "", 5 * 60);
                response.sendRedirect(loginUrl);
                return false;
            }
        }
        return super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (null == ex) {
            return;
        }
        logger.error("系统异常", ex);
        this.writeResult(response, new BaseResult(BaseConstant.FAILED, "系统异常，请联系客服"));
    }

    private void writeResult(HttpServletResponse response, BaseResult baseResult) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.write(JSON.toJSONString(baseResult));
            writer.flush();
        } catch (Exception e) {
            logger.info("BaseInterceptor exception{}", baseResult,e);
        } finally {
            if (null != writer) {
                writer.close();
            }
        }
    }

    private boolean hasLogin(HttpServletRequest request, HttpServletResponse response){
        // 获取用户token
        String token = CookieUtil.getUserToken(request);
        if(StringUtils.isBlank(token)){
            return false;
        }
        try {
            // 用户已经登录
            String tmp = CookieUtil.decrypt(token);
            String tokenARR[] = tmp.split(CookieUtil.TOKEN_SPLIT);
            if(tokenARR.length!=3 || !tokenARR[0].equals(CookieUtil.TOKEN_PREFIX)){
                return false;
            } else {
                long startTime = Long.parseLong(tokenARR[2]);
                // 2H有效期
                if(System.currentTimeMillis()-startTime>2*60*60*1000){
                    return false;
                }
                String custInfo = SpringApplicationContextUtil.getBean(JedisUtil.class).get(token);
                if(StringUtils.isBlank(custInfo)){
                    return false;
                }
                CustDto custDto = JSON.parseObject(custInfo, CustDto.class);
                if(!tokenARR[1].equals(custDto.getOpenid())){
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("BaseInterceptor exception ", e);
            return false;
        }
        return true;
    }

    private boolean needFilter(String url){
        boolean needFilter = true;
        for(String u : excludeUrl){
            if(u.endsWith("*")){
                // 模糊匹配
                u = u.replaceAll("\\*", "");
                needFilter = !url.startsWith(u);
            } else {
                // 精确匹配
                needFilter = !url.equals(u);
            }
            if(!needFilter){
                break;
            }
        }
        return needFilter;
    }
    /**
     * 获取随机字母数字组合
     *
     * @param length
     *            字符串长度
     * @return
     */
    private static String getRandomCharAndNumr(Integer length) {
        String str = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            boolean b = random.nextBoolean();
            if (b) { // 字符串
                // int choice = random.nextBoolean() ? 65 : 97; 取得65大写字母还是97小写字母
                str += (char) (65 + random.nextInt(26));// 取得大写字母
            } else { // 数字
                str += String.valueOf(random.nextInt(10));
            }
        }
        return str;
    }
}
