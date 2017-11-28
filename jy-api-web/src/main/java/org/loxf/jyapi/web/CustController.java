package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.base.bean.Pager;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.DateUtils;
import org.loxf.jyadmin.base.util.IdGenerator;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.base.util.weixin.bean.UserAccessToken;
import org.loxf.jyadmin.base.util.weixin.bean.WXUserInfo;
import org.loxf.jyadmin.client.dto.AccountDto;
import org.loxf.jyadmin.client.dto.ActiveCustListDto;
import org.loxf.jyadmin.client.dto.ActiveDto;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.*;
import org.loxf.jyapi.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.rmi.rmic.iiop.IDLGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CustController {
    private static Logger logger = LoggerFactory.getLogger(CustController.class);

    @Autowired
    private JedisUtil jedisUtil;
    @Autowired
    private CustService custService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private ActiveCustService activeCustService;
    @Autowired
    private VerifyCodeService verifyCodeService;

    /**
     * 初始化接口
     *
     * @return
     */
    @RequestMapping("/api/cust/init")
    @ResponseBody
    public BaseResult init(HttpServletRequest request) {
        CustDto custDto = CookieUtil.getCust(request);
        BaseResult<JSONObject> accountDtoBaseResult = accountService.queryAccount(custDto.getCustId());
        JSONObject jsonObject = accountDtoBaseResult.getData();
        jsonObject.put("email", custDto.getEmail());
        jsonObject.put("isChinese", custDto.getIsChinese());
        jsonObject.put("nickName", custDto.getNickName());
        jsonObject.put("phone", custDto.getPhone());
        jsonObject.put("pic", custDto.getHeadImgUrl());
        return accountDtoBaseResult;
    }

    /**
     * 我的同学
     *
     * @param type 1:直接同学 2：间接同学
     * @return
     */
    @RequestMapping("/api/cust/myClassmate")
    @ResponseBody
    public PageResult myClassmate(HttpServletRequest request, Integer page, Integer size, Integer type) {
        String custId = CookieUtil.getCustId(request);
        return custService.queryChildList(type, custId, page, size);
    }

    /**
     * 我的活动
     *
     * @return
     */
    @RequestMapping("/api/cust/activity")
    @ResponseBody
    public PageResult myActivity(HttpServletRequest request, Integer page, Integer size) {
        String custId = CookieUtil.getCustId(request);
        if (page == null) {
            page=1;
        }
        if (size == null) {
            size=10;
        }
        PageResult<ActiveCustListDto> pageResult = activeCustService.pager(custId, page, size);
        List<JSONObject> result = new ArrayList<>();
        if(pageResult.getTotal()>0) {
            List<ActiveCustListDto> list = pageResult.getData();
            for(ActiveCustListDto dto : list){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("activeEndTime", DateUtils.format(dto.getActiveEndTime()));
                jsonObject.put("activeStartTime", DateUtils.format(dto.getActiveStartTime()));
                jsonObject.put("activeId", dto.getActiveId());
                jsonObject.put("activeName", dto.getActiveName());
                jsonObject.put("addr", dto.getAddr());
                jsonObject.put("pic", dto.getPic());
                // 0：无，1：免费 2：VIP免费 3：SVIP免费
                String activePrivi = dto.getActivePrivi();
                JSONObject priviJson = JSON.parseObject(activePrivi);
                if(priviJson.containsKey("NONE") && priviJson.getIntValue("NONE")==0) {
                    jsonObject.put("vipFlag", 1);
                } else if(priviJson.containsKey("VIP") && priviJson.getIntValue("VIP")==0) {
                    jsonObject.put("vipFlag", 2);
                } else if(priviJson.containsKey("SVIP") && priviJson.getIntValue("SVIP")==0) {
                    jsonObject.put("vipFlag", 3);
                } else {
                    jsonObject.put("vipFlag", 0);
                }
                // 活动状态 0:进行中 1:即将开始 2:报名中 3:已经结束
                long startTime = dto.getActiveStartTime().getTime();
                int status = 0;
                if(startTime-System.currentTimeMillis()>48*60*60*1000){
                    // 48小时前叫报名中
                    status = 2;
                } else if(startTime-System.currentTimeMillis()<48*60*60*1000 && startTime-System.currentTimeMillis()>0){
                    status = 1;
                } else if(dto.getActiveEndTime().getTime()-System.currentTimeMillis()<0){
                    status = 3;
                }
                jsonObject.put("activeStatus", status);
                jsonObject.put("activeTicketNo", dto.getActiveTicketNo());
                result.add(jsonObject);
            }
        }
        return new PageResult(pageResult.getTotalPage(), pageResult.getCurrentPage(), pageResult.getTotal(), result);
    }

    /**
     * 我的订单
     *
     * @return
     */
    @RequestMapping("/api/cust/myorder")
    @ResponseBody
    public BaseResult myorder(HttpServletRequest request) {
        return new BaseResult();
    }

    /**
     * 积分排行榜前十列表
     *
     * @return
     */
    @RequestMapping("/api/bp/rankingList")
    @ResponseBody
    public BaseResult rankingList(HttpServletRequest request) {
        return new BaseResult();
    }

    /**
     * 积分明细
     *
     * @param type 0:全部 1:收入 3:支出
     * @return
     */
    @RequestMapping("/api/cust/bpDetail")
    @ResponseBody
    public BaseResult bpDetail(HttpServletRequest request, Integer page, Integer size, Integer type) {
        return new BaseResult();
    }

    /**
     * 绑定手机
     *
     * @param isChinese 1:国内用户 2:海外用户
     * @return
     */
    @RequestMapping("/api/cust/bindPhone")
    @ResponseBody
    public BaseResult bindPhone(HttpServletRequest request, String email, String phone, Integer isChinese, String verifyCode) {
        CustDto custDto = CookieUtil.getCust(request);
        if(custDto.getIsChinese()==null) {
            String target ;
            if(isChinese==1){
                target = phone;
            } else {
                target = email;
            }
            BaseResult verifyResult = verifyCodeService.verify(custDto.getCustId(), verifyCode);
            if(verifyResult.getCode()==BaseConstant.SUCCESS) {
                custDto.setIsChinese(isChinese);
                custDto.setEmail(email);
                custDto.setPhone(phone);
                custService.updateCust(custDto);
                return new BaseResult();
            } else {
                return new BaseResult(BaseConstant.FAILED, "验证码错误");
            }
        }
        return new BaseResult(BaseConstant.FAILED, "用户已绑定，不能重复绑定");
    }

    /**
     * 观看记录
     *
     * @return
     */
    @RequestMapping("/api/cust/watchRecordList")
    @ResponseBody
    public BaseResult watchRecordList(HttpServletRequest request, Integer page, Integer size) {
        return new BaseResult();
    }
}
