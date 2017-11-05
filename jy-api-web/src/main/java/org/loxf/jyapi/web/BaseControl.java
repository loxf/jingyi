package org.loxf.jyapi.web;

import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseModel;

public class BaseControl<T extends BaseModel> {
    protected void initRangeDate(T t){
        if(StringUtils.isNotBlank(t.getRangDate())){
            String startAndEnd []= t.getRangDate().split("~");
            t.setStartDate(startAndEnd[0].trim());
            t.setEndDate(startAndEnd[1].trim());
        }
    }
}
