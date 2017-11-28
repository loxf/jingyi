package org.loxf.jyapi.util;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.loxf.jyadmin.client.dto.ConfigDto;
import org.loxf.jyadmin.client.service.ConfigService;

public class ConfigUtil {
    public static ConfigDto getConfig(String catalog, String configCode){
        return SpringApplicationContextUtil.getBean(ConfigService.class).queryConfig(catalog, configCode).getData();
    }
    public static ConfigDto getConfig(String catalog, String configCode, String defaultValue){
        BaseResult<ConfigDto> baseResult = SpringApplicationContextUtil.getBean(ConfigService.class).queryConfig(catalog, configCode);
        if(baseResult.getCode()== BaseConstant.FAILED || baseResult.getData()==null){
            ConfigDto configDto = new ConfigDto();
            configDto.setConfigValue(defaultValue);
            return configDto;
        }
        return baseResult.getData();
    }
}
