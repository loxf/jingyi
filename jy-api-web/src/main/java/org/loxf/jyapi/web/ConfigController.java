package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.service.ConfigService;
import org.loxf.jyadmin.client.service.ProvinceAndCityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ConfigController {
    @Autowired
    private ConfigService configService;
    @Autowired
    private ProvinceAndCityService provinceAndCityService;
    @RequestMapping("/api/system/getConfig")
    @ResponseBody
    public BaseResult getConfig(String catalog, String configCode){
        return configService.queryConfig(catalog, configCode);
    }

    @RequestMapping("/api/system/getArea")
    @ResponseBody
    public BaseResult getArea(Integer type){
        return provinceAndCityService.queryAreaByTree(type);
    }
}
