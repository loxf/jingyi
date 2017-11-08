package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.IdGenerator;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.base.util.weixin.bean.UserAccessToken;
import org.loxf.jyadmin.base.util.weixin.bean.WXUserInfo;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.CustService;
import org.loxf.jyapi.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class LoginController {
    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private JedisUtil jedisUtil;
    @Autowired
    private CustService custService;

    /**
     * 登录
     * @param targetUrl 跳转URL
     * @param code 微信授权CODE
     * @param state 业务系统自行定义的校验码
     *
     * @return
     */
    @RequestMapping("/api/login")
    public void login(HttpServletRequest request, HttpServletResponse response, String targetUrl, String code, String state){
        // 检验用户登录随机码
        if(jedisUtil.exists(state)) {
            String expireTime = jedisUtil.get(state);
            if(System.currentTimeMillis() - Long.parseLong(expireTime)>0) {
                // TODO 跳转错误页面：登录校验码失效
                logger.info("登录校验码失效");
            } else {
                // 登录成功
                // 请求用户信息
                UserAccessToken userAccessToken = WeixinUtil.queryUserAccessToken(code);
                // TODO 注释DEBUG
                if("IYUTERESGBXVCMSWB".equals(request.getParameter("XDebug"))) {
                    userAccessToken = testUserAccessToken();
                }
                if(userAccessToken!=null){
                    // 获取用户登录token成功 拉取用户信息
                    WXUserInfo wxUserInfo = WeixinUtil.queryUserInfo(userAccessToken.getAccess_token(), userAccessToken.getOpenid());
                    // TODO 注释DEBUG
                    if("IYUTERESGBXVCMSWB".equals(request.getParameter("XDebug"))) {
                        wxUserInfo = testWXUserInfo();
                    }
                    if(wxUserInfo!=null) {
                        settingUser(request, response, userAccessToken, wxUserInfo);
                        try {
                            response.sendRedirect(targetUrl);
                        } catch (IOException e) {
                            logger.error("登录后跳转页面失败", e);
                        }
                    }
                }
            }
            jedisUtil.del(state);
        } else {
            // TODO 跳转错误页面：登录校验码不存在
            logger.info("登录校验码不存在");
        }
    }
    private UserAccessToken testUserAccessToken(){
        UserAccessToken accessToken = new UserAccessToken();
        accessToken.setAccess_token("asglujwlkrntjkfnvuifd233465gdfkjgds");
        accessToken.setExpires_in("7200");
        accessToken.setOpenid("LAKSJDFOI25JSDJF");
        accessToken.setRefresh_token("aslkughjlafshnguiriuhiu4thddfgoudjh");
        accessToken.setScope("snsapi_userinfo");
        return accessToken;
    }

    private WXUserInfo testWXUserInfo(){
        WXUserInfo wxUserInfo = new WXUserInfo();
        wxUserInfo.setCity("广州");
        wxUserInfo.setProvince("广东");
        wxUserInfo.setCountry("中国");
        wxUserInfo.setNickname("FACE");
        wxUserInfo.setHeadimgurl("http://");
        wxUserInfo.setOpenid("LAKSJDFOI25JSDJF");
        wxUserInfo.setSex(1);
        wxUserInfo.setUnionid("ijdxjnfnjrkt35kjwefs89");
        return wxUserInfo;
    }

    private void settingUser(HttpServletRequest request, HttpServletResponse response,
                            UserAccessToken userAccessToken, WXUserInfo wxUserInfo) {
        //处理微信用户信息
        CustDto custDto = new CustDto();
        BeanUtils.copyProperties(wxUserInfo, custDto);
        custDto.setHeadImgUrl(wxUserInfo.getHeadimgurl());
        custDto.setNickName(wxUserInfo.getNickname());
        BaseResult<CustDto> baseResult = custService.queryCustByOpenId(wxUserInfo.getOpenid());
        // 计算token失效时间
        int expireSecond = Integer.valueOf(userAccessToken.getExpires_in());
        userAccessToken.setExpires_in((expireSecond *1000 + System.currentTimeMillis()) + "");
        if (baseResult.getCode() == BaseConstant.SUCCESS) {
            custService.refreshCustByOpenId(custDto, userAccessToken);
        } else {
            String recommend = request.getParameter("recommend");
            custDto.setRecommend(recommend);
            custService.addCust(custDto, userAccessToken);
        }
        // 生成 系统TOKEN
        String tmp = CookieUtil.TOKEN_PREFIX + CookieUtil.TOKEN_SPLIT + custDto.getOpenid()
                + CookieUtil.TOKEN_SPLIT + System.currentTimeMillis();
        try {
            String token = CookieUtil.encrypt(tmp);
            // 设置cookie session token
            CookieUtil.setSession(request, BaseConstant.USER_COOKIE_NAME, token);
            CookieUtil.setCookie(response, BaseConstant.USER_COOKIE_NAME, token);
            jedisUtil.set(token, JSON.toJSONString(custDto), expireSecond);
        } catch (Exception e) {
            logger.error("TOKEN加密失败", e);
        }
    }
}
