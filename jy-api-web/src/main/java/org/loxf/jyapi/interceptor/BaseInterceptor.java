package org.loxf.jyapi.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.constant.BaseConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class BaseInterceptor extends HandlerInterceptorAdapter {
    private static Logger logger = LoggerFactory.getLogger(BaseInterceptor.class);
    private static String [] excludeUrl = {"/static/*", "/admin/login.html", "/"};
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /*if(needFilter(request.getRequestURI())){
            if(!hasLogin(request, response)){
                response.sendRedirect("/");
                return false;
            }
        }*/
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

    private boolean hasLogin(HttpServletRequest request, HttpServletResponse response){
        String token = CookieUtil.getAdminToken(request);
        if(StringUtils.isBlank(token)){
            return false;
        }
        try {
            String tmp = CookieUtil.decrypt(token);
            String tokenARR[] = tmp.split(CookieUtil.TOKEN_SPLIT);
            if(tokenARR.length!=3 || !tokenARR[0].equals(CookieUtil.TOKEN_PREFIX)){
                return false;
            } else {
                long startTime = Long.parseLong(tokenARR[2]);
                if(System.currentTimeMillis()-startTime>24*60*60*1000){
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("BaseInterceptor exception ", e);
            return false;
        }
        return true;
    }
}
