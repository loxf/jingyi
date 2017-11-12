package org.loxf.jyapi.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.ConfigService;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class BaseInterceptor extends HandlerInterceptorAdapter {
    private static Logger logger = LoggerFactory.getLogger(BaseInterceptor.class);
    private static String [] excludeUrl = {"/api/login", "/api/loginByWx"};
    @Autowired
    private ConfigService configService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("basePic",
                configService.queryConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "PIC_SERVER_URL").getData().getConfigValue());
        response.setHeader("Access-Control-Allow-Origin","*");
        response.setHeader("Access-Control-Allow-Methods","POST,GET,OPTIONS");
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
        logger.error("系统异常", ex);
        this.writeResult(response, new BaseResult(BaseConstant.FAILED, "系统异常，请联系客服"));
    }

    private void writeResult(HttpServletResponse response, BaseResult baseResult) {
        PrintWriter writer = null;
        try {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
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
}
