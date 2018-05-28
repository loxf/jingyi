package org.loxf.jyapi.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.exception.BizException;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyapi.exception.NotLoginException;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyapi.util.JyDomainUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class BaseInterceptor extends HandlerInterceptorAdapter {
    private static Logger logger = LoggerFactory.getLogger(BaseInterceptor.class);
    private static String [] excludeUrl = {"/api/weixin/*", "/api/login", "/api/loginByWx", "/api/loginByXcx", "/api/loginByXcxTmpToken", "/api/cancelPay", "/api/share/shareInfoXcx"};
    @Value("#{configProperties['SYSTEM.DEBUG']}")
    private Boolean debug;
    @Value("#{configProperties['JYZX.ENV']}")
    private String env;
    private static String basePic = null ;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            setResponse(request, response);
        } catch (Exception e){
            logger.error("拦截器设置response失败", e);
        }
        logger.info("请求路径:{}, sessionId:{}", request.getRequestURI(), request.getSession().getId());
        // 判断用户是否登录系统
        if(needFilter(request.getRequestURI())) {
            if (!hasLogin(request, response)) {
                this.writeResult(response, new BaseResult(BaseConstant.NOT_LOGIN, "未登录"));
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
        if(ex instanceof NotLoginException){
            this.writeResult(response, new BaseResult(BaseConstant.NOT_LOGIN, ex.getMessage()));
        } else if(ex instanceof BizException){
            logger.error("业务异常", ex);
            this.writeResult(response, new BaseResult(BaseConstant.FAILED, ex.getMessage()));
        } else {
            logger.error("系统异常", ex);
            this.writeResult(response, new BaseResult(BaseConstant.FAILED, "系统异常，请联系客服"));
        }
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
            if(debug!=null && debug){
                token = request.getParameter(BaseConstant.USER_COOKIE_NAME);
                logger.warn("调试模式获取的token:" + token);
                if(StringUtils.isBlank(token)){
                    return false;
                } else {
                    // 反写session cookie
                    CookieUtil.setSession(request, BaseConstant.USER_COOKIE_NAME, token);
                    CookieUtil.setCookie(response, BaseConstant.USER_COOKIE_NAME, token, JyDomainUtil.getDomain(request.getRequestURL().toString()));
                }
            } else {
                return false;
            }
        }
        try {
            // 用户已经登录
            String tmp = CookieUtil.decrypt(token);
            String tokenARR[] = tmp.split(CookieUtil.TOKEN_SPLIT);
            if(tokenARR.length!=4 || !tokenARR[0].equals(env)){
                return false;
            } else {
                long startTime = Long.parseLong(tokenARR[2]);
                // 24H有效期
                if(System.currentTimeMillis()-startTime>24*60*60*1000){
                    return false;
                }
                String custInfo = SpringApplicationContextUtil.getBean(JedisUtil.class).get(token);
                if(StringUtils.isBlank(custInfo)){
                    return false;
                }
                CustDto custDto = JSON.parseObject(custInfo, CustDto.class);
                if(!tokenARR[1].equals(custDto.getCustId())){
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

    private void setResponse(HttpServletRequest request, HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin","*");
        response.setHeader("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers","Origin, X-Requested-With, Content-Type, Accept, Cookie");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Content-type", "application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        CookieUtil.setCookie(response, "PIC_SERVER_URL",
                ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "PIC_SERVER_URL").getConfigValue(),
            JyDomainUtil.getDomain(request.getRequestURL().toString()));
    }

}
