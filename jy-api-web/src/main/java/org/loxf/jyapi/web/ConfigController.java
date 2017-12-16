package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.client.dto.SystemLogDto;
import org.loxf.jyadmin.client.service.ConfigService;
import org.loxf.jyadmin.client.service.ProvinceAndCityService;
import org.loxf.jyadmin.client.service.SystemLogService;
import org.loxf.jyapi.thread.WxAccessTokenFreshJob;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyapi.util.IPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class ConfigController {
    @Autowired
    private ConfigService configService;
    @Autowired
    private ProvinceAndCityService provinceAndCityService;
    @Autowired
    private JedisUtil jedisUtil;
    @Autowired
    private SystemLogService systemLogService;

    @RequestMapping("/api/system/wxUrlConfig")
    @ResponseBody
    public BaseResult getConfig(String url) {
        String jsapiTicket = jedisUtil.get(BaseConstant.WX_JS_TICKET);
        if (StringUtils.isBlank(jsapiTicket)) {
            WxAccessTokenFreshJob.deal(jedisUtil);
            jsapiTicket = jedisUtil.get(BaseConstant.WX_JS_TICKET);
            if (StringUtils.isBlank(jsapiTicket)) {
                return new BaseResult(BaseConstant.FAILED, "获取TOKEN失败");
            }
        }
        Map info = WeixinUtil.signJsTicket(jsapiTicket, url);
        String appId = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_APPID").getConfigValue();
        info.put("appId", appId);
        return new BaseResult(info);
    }

    @RequestMapping("/api/system/getConfig")
    @ResponseBody
    public BaseResult getConfig(String catalog, String configCode) {
        return configService.queryConfig(catalog, configCode);
    }

    @RequestMapping("/api/system/getArea")
    @ResponseBody
    public BaseResult getArea(Integer type) {
        return provinceAndCityService.queryAreaByTree(type);
    }

    @RequestMapping("/api/system/log")
    @ResponseBody
    public BaseResult log(HttpServletRequest request, String osType, String page, String location) {
        try {
            systemLogService.log(createLog(CookieUtil.getCustId(request), IPUtil.getIpAddr(request),
                    osType, page, location));
        } finally {
            return new BaseResult();
        }
    }

    private SystemLogDto createLog(String custId, String ip, String os, String page, String position) {
        SystemLogDto log = new SystemLogDto();
        log.setCustId(custId);
        log.setIp(ip);
        log.setOs(os);
        log.setPage(page);
        log.setPosition(position);
        return log;
    }

}
