package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.base.bean.Pager;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.constant.WxMsgTemplateConstant;
import org.loxf.jyadmin.base.util.DateUtils;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.loxf.jyadmin.client.dto.*;
import org.loxf.jyadmin.client.service.*;
import org.loxf.jyapi.util.BizUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;

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
    @Autowired
    private OrderService orderService;
    @Autowired
    private CustBpDetailService custBpDetailService;
    @Autowired
    private WatchRecordService watchRecordService;
    @Autowired
    private NoticeService noticeService;

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
                jsonObject.put("activeEndTime", DateUtils.formatHms(dto.getActiveEndTime()));
                jsonObject.put("activeStartTime", DateUtils.formatHms(dto.getActiveStartTime()));
                jsonObject.put("activeId", dto.getActiveId());
                jsonObject.put("activeName", dto.getActiveName());
                jsonObject.put("addr", dto.getAddr());
                jsonObject.put("pic", dto.getPic());
                // 0：无，1：免费 2：VIP免费 3：SVIP免费
                String activePrivi = dto.getActivePrivi();
                JSONObject priviJson = JSON.parseObject(activePrivi);
                if(priviJson.containsKey("NONE") && new BigDecimal(priviJson.get("NONE").toString()).compareTo(BigDecimal.ZERO)==0) {
                    jsonObject.put("vipFlag", 1);
                } else if(priviJson.containsKey("VIP") && new BigDecimal(priviJson.get("VIP").toString()).compareTo(BigDecimal.ZERO)==0) {
                    jsonObject.put("vipFlag", 2);
                } else if(priviJson.containsKey("SVIP") && new BigDecimal(priviJson.get("SVIP").toString()).compareTo(BigDecimal.ZERO)==0) {
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
    public PageResult myorder(HttpServletRequest request, Integer page, Integer size) {
        String custId = CookieUtil.getCustId(request);
        OrderDto custDto = new OrderDto();
        Pager pager = new Pager(page, size);
        custDto.setPager(pager);
        custDto.setCustId(custId);
        PageResult<OrderDto> pageResult = orderService.pager(custDto);
        return pageResult;
    }

    /**
     * 积分排行榜前十列表
     *
     * @return
     */
    @RequestMapping("/api/bp/rankingList")
    @ResponseBody
    public BaseResult<JSONObject> rankingList(HttpServletRequest request) {
        String custId = CookieUtil.getCustId(request);
        return accountService.queryBpRankingList(custId);
    }

    /**
     * 积分明细
     *
     * @param type 0:全部 1:收入 3:支出
     * @return
     */
    @RequestMapping("/api/cust/bpDetail")
    @ResponseBody
    public PageResult<CustBpDetailDto> bpDetail(HttpServletRequest request, Integer page, Integer size, Integer type) {
        String custId = CookieUtil.getCustId(request);
        CustBpDetailDto custBpDetailDto = new CustBpDetailDto();
        custBpDetailDto.setType(type);
        custBpDetailDto.setCustId(custId);
        custBpDetailDto.setPager(new Pager(page, size));
        return custBpDetailService.pager(custBpDetailDto);
    }

    /**
     * 绑定手机
     *
     * @param isChinese 1:国内用户 2:海外用户
     * @return
     */
    @RequestMapping("/api/cust/bindPhone")
    @ResponseBody
    public BaseResult bindPhone(HttpServletRequest request, HttpServletResponse response,
                                String realName, String email, String phone,
                                Integer isChinese, String verifyCode) {
        CustDto custDto = CookieUtil.getCust(request);
        if(custDto.getIsChinese()==null) {
            if(isChinese==1 && StringUtils.isBlank(phone)){
                return new BaseResult(BaseConstant.FAILED, "国内用户请填写手机号码");
            } else if(isChinese==2 && StringUtils.isBlank(email)){
                return new BaseResult(BaseConstant.FAILED, "海外用户请填写电子邮箱");
            }
            BaseResult verifyResult = verifyCodeService.verify(custDto.getCustId(), verifyCode);
            if(verifyResult.getCode()==BaseConstant.SUCCESS) {
                // 判断用户存在不
                BaseResult<CustDto> existsCustBaseResult = custService.queryCust(isChinese, StringUtils.isNotBlank(phone)?phone:email);
                if(existsCustBaseResult.getCode()==BaseConstant.SUCCESS && existsCustBaseResult.getData()!=null){
                    if(StringUtils.isNotBlank(existsCustBaseResult.getData().getOpenid())) {// 老用户没有openid 但是有电话
                        return new BaseResult(BaseConstant.FAILED, "当前联系方式已被绑定，请更换");
                    }
                }
                custDto.setIsChinese(isChinese);
                custDto.setEmail(email);
                custDto.setPhone(phone);
                custDto.setRealName(realName);
                if(isChinese==1) {
                    BaseResult<CustDto> custDtoBaseResult = custService.queryOldCust(phone);
                    if(custDtoBaseResult.getCode()==BaseConstant.SUCCESS){
                        // 存在老客户未绑定，获取老用户信息
                        CustDto oldCust = custDtoBaseResult.getData();
                        // 将新账号的信息复制到老账号
                        oldCust.setNickName(custDto.getNickName());
                        oldCust.setCountry(custDto.getCountry());
                        oldCust.setProvince(custDto.getProvince());
                        oldCust.setCity(custDto.getCity());
                        oldCust.setSex(custDto.getSex());
                        oldCust.setHeadImgUrl(custDto.getHeadImgUrl());
                        oldCust.setRealName(realName);
                        oldCust.setPrivilege(custDto.getPrivilege());
                        oldCust.setOpenid(custDto.getOpenid());
                        // 更新老用户信息
                        custService.updateOldCustInfo(oldCust);
                        // 获取临时新账户
                        JSONObject tmpAccount = accountService.queryAccount(custDto.getCustId()).getData();
                        // 合并新账户金额到老账户
                        if(new BigDecimal(tmpAccount.get("bp").toString()).compareTo(BigDecimal.ZERO)>0 ||
                                new BigDecimal(tmpAccount.get("balance").toString()).compareTo(BigDecimal.ZERO)>0) {
                            accountService.increase(oldCust.getCustId(), new BigDecimal(tmpAccount.get("balance").toString()),
                                    new BigDecimal(tmpAccount.get("bp").toString()), null, "老用户绑定合并", null);
                        }
                        // 删除临时客户账户数据
                        custService.delTmpCust(custDto.getCustId());
                        accountService.delAccount(custDto.getCustId());
                    } else {
                        custService.updateCust(custDto);
                    }
                } else {
                    custService.updateCust(custDto);
                }
                sendUserBindNotice(custDto.getOpenid(), custDto.getNickName(), (isChinese==1?phone:email));
                // 刷新缓存
                LoginController.setCustInfoSessionAndCookie(request, response, custService, jedisUtil,
                        custDto.getOpenid(), null);
                return new BaseResult();
            } else {
                return new BaseResult(BaseConstant.FAILED, "验证码错误");
            }
        }
        return new BaseResult(BaseConstant.FAILED, "用户已绑定，不能重复绑定");
    }

    /**
     * 用户绑定通知
     * @param openid
     * @param nickname
     * @param contact
     */
    public void sendUserBindNotice( String openid, String nickname, String contact){
        Map data = new HashMap();
        data.put("first", BizUtil.createWXKeyWord("亲爱的会员，您已使用" + contact + "绑定", null));
        data.put("keyword1", BizUtil.createWXKeyWord(nickname, "#FF3030"));
        data.put("keyword2", BizUtil.createWXKeyWord(DateUtils.formatHms(new Date()), null));
        data.put("remark", BizUtil.createWXKeyWord("若非本人操作，请联系班主任，谢谢。", null));
        noticeService.insert("WX", openid, BizUtil.createWxMsgMap(WxMsgTemplateConstant.BIND_USER,
                openid, data, BaseConstant.JYZX_INDEX_URL));
    }
    /**
     * 观看记录
     *
     * @return
     */
    @RequestMapping("/api/cust/watchRecordList")
    @ResponseBody
    public PageResult<WatchRecordDto> watchRecordList(HttpServletRequest request, Integer page, Integer size) {
        String custId = CookieUtil.getCustId(request);
        WatchRecordDto watchRecordDto = new WatchRecordDto();
        watchRecordDto.setPager(new Pager(page, size));
        watchRecordDto.setCustId(custId);

        return watchRecordService.pager(watchRecordDto);
    }
}
