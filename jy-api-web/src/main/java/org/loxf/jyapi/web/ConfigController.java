package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ConfigController {
    @Autowired
    private ConfigService configService;
    @RequestMapping("/api/system/getConfig")
    @ResponseBody
    public BaseResult getConfig(String catalog, String configCode){
        return configService.queryConfig(catalog, configCode);
    }
}
