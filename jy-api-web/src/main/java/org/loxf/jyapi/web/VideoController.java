package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class VideoController {

    /**
     * 视频观看记录
     * @param videoId 视频ID
     * @return
     */
    @RequestMapping("/api/video/watch")
    @ResponseBody
    public BaseResult watchRecord(String videoId){
        return new BaseResult();
    }
}
