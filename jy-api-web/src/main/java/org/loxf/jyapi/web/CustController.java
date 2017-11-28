package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.IdGenerator;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.base.util.weixin.bean.UserAccessToken;
import org.loxf.jyadmin.base.util.weixin.bean.WXUserInfo;
import org.loxf.jyadmin.client.dto.AccountDto;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.AccountService;
import org.loxf.jyadmin.client.service.CustService;
import org.loxf.jyadmin.client.service.VerifyCodeService;
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
    public BaseResult myClassmate(Integer page, Integer size, Integer type) {
        return new BaseResult();
    }

    /**
     * 我的活动
     *
     * @return
     */
    @RequestMapping("/api/cust/activity")
    @ResponseBody
    public BaseResult myActivity(Integer page, Integer size) {
        return new BaseResult();
    }

    /**
     * 我的订单
     *
     * @return
     */
    @RequestMapping("/api/cust/myorder")
    @ResponseBody
    public BaseResult myorder() {
        return new BaseResult();
    }

    /**
     * 积分排行榜前十列表
     *
     * @return
     */
    @RequestMapping("/api/bp/rankingList")
    @ResponseBody
    public BaseResult rankingList() {
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
    public BaseResult bpDetail(Integer page, Integer size, Integer type) {
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
    public BaseResult watchRecordList(Integer page, Integer size) {
        return new BaseResult();
    }
}
