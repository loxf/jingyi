package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.client.dto.*;
import org.loxf.jyadmin.client.service.AccountService;
import org.loxf.jyadmin.client.service.ActiveService;
import org.loxf.jyadmin.client.service.OfferService;
import org.loxf.jyadmin.client.service.OrderService;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OfferService offerService;
    @Autowired
    private ActiveService activeService;
    @Autowired
    private AccountService accountService;

    /**
     * 订单确认初始化
     *
     * @param offerId 必填，购买对象（商品ID，活动ID）
     * @param type    活动：ACTIVE,课程：CLASS,套餐：OFFER,SVIP/VIP：VIP
     * @return
     */
    @RequestMapping("/api/order/init")
    @ResponseBody
    public BaseResult orderInit(HttpServletRequest request, String offerId, String type) {
        if (StringUtils.isBlank(offerId)) {
            return new BaseResult(BaseConstant.FAILED, "购买对象为空");
        }
        if (StringUtils.isBlank(type)) {
            return new BaseResult(BaseConstant.FAILED, "订单类型为空");
        }
        JSONObject result = new JSONObject();
        // 当前余额
        BaseResult<BigDecimal> balanceBaseResult = accountService.queryBalance(CookieUtil.getCustId(request));
        if(balanceBaseResult.getCode()==BaseConstant.FAILED){
            return balanceBaseResult;
        }
        result.put("balance", balanceBaseResult.getData().toPlainString());
        // 订单属性
        if(type.equals("ACTIVE")){// 活动才有订单属性
            setAttrList(result);
        }
        //商品列表
        return setOfferList(offerId, type, result);
    }

    private BaseResult setOfferList(String offerId, String type, JSONObject result){
        String[] objs = offerId.split(",");
        List<JSONObject> offerList = new ArrayList<>();
        for(String objId : objs) {
            if (type.equals("ACTIVE")) {
                ActiveDto activeDto = activeService.queryActive(objId).getData();
                if(activeDto==null){
                    return new BaseResult(BaseConstant.FAILED, "活动不存在");
                }
                offerList.add(settingOfferInfo(activeDto.getActiveId(), activeDto.getActiveName(),
                        activeDto.getPic(), activeDto.getPrice()));
            } else if (type.equals("VIP") || type.equals("OFFER") || type.equals("CLASS")) {
                OfferDto offerDto = offerService.queryOffer(objId).getData();
                if(offerDto==null){
                    return new BaseResult(BaseConstant.FAILED, "商品不存在");
                }
                JSONObject jsonObject = settingOfferInfo(offerDto.getOfferId(), offerDto.getOfferName(),
                        offerDto.getOfferPic(), offerDto.getSaleMoney());
                if(type.equals("VIP")){
                    String desc = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_PAY, "VIP_ORDER_DESC",
                            "畅享名师干货;分享即可获得奖学金").getConfigValue();
                    jsonObject.put("descList", desc.split(";"));
                }
                offerList.add(jsonObject);
            }
        }
        result.put("offerList", offerList);
        return new BaseResult(result);
    }

    private JSONObject settingOfferInfo(String offerId, String offerName, String pic, BigDecimal price){
        JSONObject offer = new JSONObject();
        offer.put("offerId", offerId);
        offer.put("offerName", offerName);
        offer.put("pic", pic);
        offer.put("price", price);
        return offer;
    }


    private void setAttrList(JSONObject result){
        List<OrderAttrDto> attrDtoList = new ArrayList<>();
        OrderAttrDto orderAttrDto = new OrderAttrDto();
        orderAttrDto.setAttrCode("ACTIVITY_USER_NAME");
        orderAttrDto.setAttrName("活动参与人");
        attrDtoList.add(orderAttrDto);
        OrderAttrDto orderAttrDto1 = new OrderAttrDto();
        orderAttrDto1.setAttrCode("ACTIVITY_CONTACT");
        orderAttrDto1.setAttrName("联系方式");
        attrDtoList.add(orderAttrDto1);
        result.put("attrList", attrDtoList);
    }
    /**
     * 创建订单
     *
     * @param objId     必填，购买对象（商品ID，活动ID）
     * @param orderType 必填，1 商品 3 VIP 5 活动
     * @param payType   必填，支付方式 1：微信支付 3：余额支付
     * @return
     */
    @RequestMapping("/api/createOrder")
    @ResponseBody
    public BaseResult createOrder(HttpServletRequest request, String objId, Integer orderType, Integer payType, List<OrderAttrDto> attrList) {
        if (StringUtils.isBlank(objId) || orderType == null
                || payType == null) {
            return new BaseResult(BaseConstant.FAILED, "关键参数缺失");
        }
        CustDto custDto = CookieUtil.getCust(request);
        String lv = custDto.getUserLevel();
        // 订单
        OrderDto orderDto = new OrderDto();
        String privi = "";
        BaseResult<Boolean> hasBuyOrder = orderService.hasBuy(custDto.getCustId(), orderType, objId);
        if (hasBuyOrder.getCode() == BaseConstant.FAILED) {
            return new BaseResult(BaseConstant.FAILED, hasBuyOrder.getMsg());
        }
        if (hasBuyOrder.getData()) {
            return new BaseResult(BaseConstant.FAILED, "当前商品已经购买过");
        }
        if (orderType.intValue() == 1) {
            OfferDto offerDto = offerService.queryOffer(objId).getData();
            if (offerDto == null) {
                return new BaseResult(BaseConstant.FAILED, "商品不存在");
            }
            orderDto.setOrderName(offerDto.getOfferName());
            privi = offerDto.getBuyPrivi();

        } else if (orderType.intValue() == 3) {
            OfferDto offerDto = offerService.queryOffer(objId).getData();
            if (offerDto == null) {
                return new BaseResult(BaseConstant.FAILED, "服务不存在");
            }
            // 服务价格单独处理
            if (lv.equals("VIP") && offerDto.getOfferName().equals("SVIP")) {
                // vip 升级 svip
                orderDto.setOrderMoney(new BigDecimal("400"));
                orderDto.setTotalMoney(new BigDecimal("400"));
            } else {
                orderDto.setOrderMoney(offerDto.getSaleMoney());
                orderDto.setTotalMoney(offerDto.getSaleMoney());
            }
            orderDto.setOrderName("升级" + offerDto.getOfferName());
        } else if (orderType.intValue() == 5) {
            ActiveDto activeDto = activeService.queryActive(objId).getData();
            if (activeDto == null) {
                return new BaseResult(BaseConstant.FAILED, "活动不存在");
            }
            orderDto.setOrderName(activeDto.getActiveName());
            privi = activeDto.getActivePrivi();
        }
        if (orderType.intValue() != 3) {
            if (StringUtils.isBlank(privi)) {
                return new BaseResult(BaseConstant.FAILED, "当前商品不能购买");
            } else {
                JSONObject priviJson = JSONObject.parseObject(privi);
                if (!priviJson.containsKey(lv)) {
                    return new BaseResult(BaseConstant.FAILED, "无权购买当前商品");
                } else {
                    String price = priviJson.getString(lv);
                    orderDto.setOrderMoney(new BigDecimal(price));
                    orderDto.setTotalMoney(new BigDecimal(price));
                }
            }
        }

        orderDto.setObjId(objId);
        orderDto.setOrderType(orderType);
        orderDto.setPayType(payType);
        orderDto.setCustId(custDto.getCustId());
        orderDto.setBp(BigDecimal.ZERO);
        orderDto.setDiscount(10L);
        return orderService.createOrder(orderDto, attrList);
    }
}
