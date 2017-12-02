package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.service.HtmlInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HtmlController {
    @Autowired
    private HtmlInfoService htmlInfoService;

    /**
     * 获取富文本内容
     *
     * @param htmlId 富文本ID
     * @return
     */
    @RequestMapping("/api/html/getHtml")
    @ResponseBody
    public BaseResult getHtml(String htmlId) {
        return htmlInfoService.getHtml(htmlId);
    }
}
