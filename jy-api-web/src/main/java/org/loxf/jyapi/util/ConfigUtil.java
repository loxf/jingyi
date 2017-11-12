package org.loxf.jyapi.util;

import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.loxf.jyadmin.client.dto.ConfigDto;
import org.loxf.jyadmin.client.service.ConfigService;

public class ConfigUtil {
    public static ConfigDto getConfig(String catalog, String configCode){
        return SpringApplicationContextUtil.getBean(ConfigService.class).queryConfig(catalog, configCode).getData();
    }
}
