package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.exception.BizException;
import org.loxf.jyadmin.base.util.IdGenerator;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.base.util.weixin.bean.UserAccessToken;
import org.loxf.jyadmin.base.util.weixin.bean.WXUserInfo;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.CustService;
import org.loxf.jyapi.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

@Controller
public class LoginController {
    private static Logger logger = LoggerFactory.getLogger(LoginController.class);
    private static String defaultHeaderImg = "https://www.jingyizaixian.com/imageServer/SYSTEM/default_header.png";

    @Autowired
    private JedisUtil jedisUtil;
    @Autowired
    private CustService custService;
    @Value("#{configProperties['SYSTEM.DEBUG']}")
    private Boolean debug;
    @Value("#{configProperties['JYZX.INDEX.URL']}")
    private String JYZX_INDEX_URL;

    @RequestMapping("/api/login")
    public void login(HttpServletRequest request, HttpServletResponse response, String targetUrl) {
        // 获取登录随机code，五分钟失效
        String code = IdGenerator.generate("JYL");
        String appId = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_APPID").getConfigValue();
        String indexUrl = JYZX_INDEX_URL;
        String loginUrl = WeixinUtil.getLoginUrl(appId, targetUrl, code, indexUrl);
        try {
            if ("JY123456QWE".equals(request.getParameter("XDebug"))) {
                loginUrl = String.format(indexUrl + BaseConstant.LOGIN_URL, URLEncoder.encode(targetUrl, "utf-8")) + "&state=" + code + "&XDebug=IYUTERESGBXVCMSWB";
                if ("L".equals(request.getParameter("LOCATION"))) {
                    loginUrl = loginUrl.replaceAll(indexUrl, "http://127.0.0.1:8081");
                }
            }
            jedisUtil.set(code, "0", 5 * 60);
            logger.info("登录：" + loginUrl);
            response.sendRedirect(loginUrl);
        } catch (IOException e) {
            logger.error("登录失败", e);
            throw new RuntimeException("登录失败", e);
        }
    }

    /**
     * 登录
     *
     * @param targetUrl 跳转URL
     * @param code      微信授权CODE
     * @param state     业务系统自行定义的校验码
     * @return
     */
    @RequestMapping("/api/loginByWx")
    public void login(HttpServletRequest request, HttpServletResponse response, String targetUrl, String code, String state) {
        // 检验用户登录随机码
        if (StringUtils.isBlank(state)) {
            logger.info("登录校验码为空");
            throw new BizException("登录校验码为空");
        }
        String validNbr = jedisUtil.get(state);
        if (StringUtils.isNotBlank(validNbr)) {
            if (3 - Integer.parseInt(validNbr) <= 0) {
                logger.info("登录校验码失效:" + state);
                throw new BizException("登录校验码失效:" + state);
            } else {
                // 设置可用次数
                jedisUtil.set(state, (Integer.parseInt(validNbr)+1)+"");
                // 登录成功
                if("qa".equalsIgnoreCase(jedisUtil.getNameSpace())) {
                    // 设置图片服务器地址
                    CookieUtil.setCookie(response, "PIC_SERVER_URL",
                            ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "PIC_SERVER_URL").getConfigValue(),
                            "test.jingyizaixian.com");
                } else if("online".equalsIgnoreCase(jedisUtil.getNameSpace())){
                    // 设置图片服务器地址
                    CookieUtil.setCookie(response, "PIC_SERVER_URL",
                            ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME,"PIC_SERVER_URL").getConfigValue(),
                            "www.jingyizaixian.com");
                }
                // 请求用户信息
                String appId = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_APPID").getConfigValue();
                String appSecret = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_APPSECRET").getConfigValue();
                UserAccessToken userAccessToken = WeixinUtil.queryUserAccessToken(appId, appSecret, code);
                if (debug != null && debug && "IYUTERESGBXVCMSWB".equals(request.getParameter("XDebug"))) {
                    userAccessToken = testUserAccessToken();
                }
                if (userAccessToken != null) {
                    // 获取用户登录token成功 拉取用户信息
                    WXUserInfo wxUserInfo = WeixinUtil.queryUserInfo(userAccessToken.getAccess_token(), userAccessToken.getOpenid());
                    if (debug != null && debug && "IYUTERESGBXVCMSWB".equals(request.getParameter("XDebug"))) {
                        wxUserInfo = testWXUserInfo();
                    }
                    if (wxUserInfo != null) {
                        Map<String, String> paramMap = UrlUtil.URLRequest(targetUrl);
                        CustDto custDto = settingUser(request, response, paramMap.get("recommend"), userAccessToken, wxUserInfo);
                        try {
                            response.sendRedirect(targetUrl);
                        } catch (IOException e) {
                            logger.error("登录后跳转页面失败", e);
                        }
                    }
                } else {
                    logger.error("用户accessToken为空");
                }
            }
        } else {
            logger.error("登录校验码不存在:" + state);
        }
    }

    @RequestMapping("/api/getUserInfo")
    @ResponseBody
    public BaseResult<CustDto> getUserInfo(HttpServletRequest request, HttpServletResponse response) {
        String custId = CookieUtil.getCustId(request);
        CustDto custDto = custService.queryCustByCustId(custId).getData();
        /*String flag = jedisUtil.get("REFRESH_CUST_INFO_") + custDto.getCustId();
        if (StringUtils.isNotBlank(flag) && Boolean.valueOf(flag)) {
        }*/
        return new BaseResult<>(custDto);
    }

    private UserAccessToken testUserAccessToken() {
        UserAccessToken accessToken = new UserAccessToken();
        accessToken.setAccess_token("asglujwlkrntjkfnvuifd233465gdfkjgds");
        accessToken.setExpires_in("7200");
        accessToken.setOpenid("LAKSJDFOI25JSDJF");
        accessToken.setRefresh_token("aslkughjlafshnguiriuhiu4thddfgoudjh");
        accessToken.setScope("snsapi_userinfo");
        return accessToken;
    }

    private WXUserInfo testWXUserInfo() {
        WXUserInfo wxUserInfo = new WXUserInfo();
        wxUserInfo.setCity("广州");
        wxUserInfo.setProvince("广东");
        wxUserInfo.setCountry("中国");
        wxUserInfo.setNickname("FACE-TEST");
        wxUserInfo.setHeadimgurl("http://wx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTIayV0CQ8xZWGDnUSkfTwSMKX32hIC4Tf6RibiatgYfePby8J6B0ic5iczicPY6KDE4VAnXpGEoibk0n70A/0");
        wxUserInfo.setOpenid("LAKSJDFOI25JSDJF");
        wxUserInfo.setSex(1);
        wxUserInfo.setUnionid("ijdxjnfnjrkt35kjwefs89");
        return wxUserInfo;
    }

    private CustDto settingUser(HttpServletRequest request, HttpServletResponse response, String recommend,
                                UserAccessToken userAccessToken, WXUserInfo wxUserInfo) {
        //处理微信用户信息
        CustDto custDto = new CustDto();
        BeanUtils.copyProperties(wxUserInfo, custDto);
        if (StringUtils.isBlank(wxUserInfo.getHeadimgurl())) {
            custDto.setHeadImgUrl(defaultHeaderImg);
        } else {
            custDto.setHeadImgUrl(wxUserInfo.getHeadimgurl());
        }
        custDto.setNickName(wxUserInfo.getNickname());
        BaseResult<CustDto> baseResult = custService.queryCustByOpenId(wxUserInfo.getOpenid());
        // 计算token失效时间
        int expireSecond = Integer.valueOf(userAccessToken.getExpires_in());
        userAccessToken.setExpires_in((expireSecond * 1000 + System.currentTimeMillis()) + "");
        if (baseResult.getCode() == BaseConstant.SUCCESS && baseResult.getData() != null) {
            custService.refreshCustByOpenId(custDto, userAccessToken);
        } else {
            if (StringUtils.isNotBlank(recommend)) {
                custDto.setRecommend(recommend.toUpperCase());
            }
            BaseResult<String> custBaseResult = custService.addCust(custDto, userAccessToken);
            custDto.setCustId(custBaseResult.getData());
        }

        return setCustInfoSessionAndCookie(request, response, custService, jedisUtil, wxUserInfo.getOpenid(), expireSecond);
    }


    public static CustDto setCustInfoSessionAndCookie(HttpServletRequest request, HttpServletResponse response,
                                                      CustService custService, JedisUtil jedisUtil,
                                                      String openid, Integer expireSecond) {
        // 获取最新的cust信息
        CustDto custInfo = custService.queryCustByOpenId(openid).getData();
        // 生成 系统TOKEN
        String tmp = jedisUtil.getNameSpace() + CookieUtil.TOKEN_SPLIT + custInfo.getCustId()
                + CookieUtil.TOKEN_SPLIT + System.currentTimeMillis();
        try {
            String token = CookieUtil.encrypt(tmp);
            // 设置cookie session token
            CookieUtil.setSession(request, BaseConstant.USER_COOKIE_NAME, token);
            if (expireSecond != null) {
                jedisUtil.set(token, JSON.toJSONString(custInfo), expireSecond);
            }
            CookieUtil.setCookie(response, BaseConstant.USER_COOKIE_NAME, token);
            CookieUtil.setCookie(response, BaseConstant.JY_CUST_ID, custInfo.getCustId());
        } catch (Exception e) {
            logger.error("TOKEN加密失败", e);
        }
        return custInfo;
    }
}
