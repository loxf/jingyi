package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.client.constant.NewsType;
import org.loxf.jyadmin.client.dto.NewsDto;
import org.loxf.jyadmin.client.service.NewsService;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class NewsController {
    @Autowired
    private NewsService newsService;

    @RequestMapping("/api/news/pager")
    @ResponseBody
    public PageResult<NewsDto> newsList(HttpServletRequest request, NewsDto newsDto){
        String custId = CookieUtil.getCustId(request);
        newsDto.setStatus(1);// 已发布的
        return newsService.getNewsListByCustId(newsDto, custId);
    }

    @RequestMapping("/api/news/detail")
    @ResponseBody
    public BaseResult<NewsDto> newsDetail(HttpServletRequest request, String titleId){
        return newsService.getNewsDetail(titleId);
    }

    @RequestMapping("/api/news/viewRecord")
    @ResponseBody
    public BaseResult<String> viewRecord(HttpServletRequest request, String titleId){
        String custId = CookieUtil.getCustId(request);
        return newsService.addViewRecord(titleId, custId);
    }
}
