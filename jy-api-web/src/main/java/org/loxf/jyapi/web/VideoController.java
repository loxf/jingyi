package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.dto.WatchRecordDto;
import org.loxf.jyadmin.client.service.WatchRecordService;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class VideoController {
    @Autowired
    WatchRecordService watchRecordService;

    /**
     * 视频观看记录
     * @param videoId 视频ID
     * @return
     */
    @RequestMapping("/api/video/watch")
    @ResponseBody
    public BaseResult watchRecord(HttpServletRequest request, String watchId, String offerId, String videoId){
        String custId = CookieUtil.getCustId(request);
        WatchRecordDto watchRecordDto = new WatchRecordDto();
        watchRecordDto.setCustId(custId);
        watchRecordDto.setWatchId(watchId);
        watchRecordDto.setVideoId(videoId);
        watchRecordDto.setOfferId(offerId);
        return watchRecordService.watch(watchRecordDto);
    }
}
