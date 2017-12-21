package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.client.dto.*;
import org.loxf.jyadmin.client.service.*;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyapi.util.IPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private CustService custService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OfferService offerService;
    @Autowired
    private ActiveService activeService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private JedisUtil jedisUtil;

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
        String custId = CookieUtil.getCustId(request);
        CustDto custDto = custService.queryCustByCustId(custId).getData();
        // 当前余额
        BaseResult<JSONObject> balanceBaseResult = accountService.queryAccount(custDto.getCustId());
        if (balanceBaseResult.getCode() == BaseConstant.FAILED) {
            return balanceBaseResult;
        }
        JSONObject result = balanceBaseResult.getData();
        // 订单属性
        if (type.equals("ACTIVE")) {// 活动才有订单属性
            setAttrList(result);
        }
        //商品列表
        return setOfferList(custDto.getUserLevel(), offerId, type, result);
    }

    private BaseResult setOfferList(String userLevel, String offerId, String type, JSONObject result) {
        String[] objs = offerId.split(",");
        List<JSONObject> offerList = new ArrayList<>();
        for (String objId : objs) {
            if (type.equals("ACTIVE")) {
                ActiveDto activeDto = activeService.queryActive(objId).getData();
                if (activeDto == null) {
                    return new BaseResult(BaseConstant.FAILED, "活动不存在");
                }

                offerList.add(settingOfferInfo(activeDto.getActiveId(), activeDto.getActiveName(),
                        activeDto.getPic(), calculatePrice(userLevel, activeDto.getActivePrivi(), activeDto.getPrice()),
                        getMaxBp(activeDto.getMetaData())));
            } else if (type.equals("VIP") || type.equals("OFFER") || type.equals("CLASS")) {
                OfferDto offerDto = offerService.queryOffer(objId).getData();
                if (offerDto == null) {
                    return new BaseResult(BaseConstant.FAILED, "商品不存在");
                }
                JSONObject jsonObject = settingOfferInfo(offerDto.getOfferId(), offerDto.getOfferName(),
                        offerDto.getOfferPic(), calculatePrice(userLevel, offerDto.getBuyPrivi(), offerDto.getSaleMoney()),
                        getMaxBp(offerDto.getMetaData()));
                if (type.equals("VIP")) {
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

    private int getMaxBp(String metaData) {
        int maxBp = 0;
        if (StringUtils.isNotBlank(metaData)) {
            JSONObject jsonObject = JSONObject.parseObject(metaData);
            if (jsonObject.containsKey("MAXBP")) {
                maxBp = jsonObject.getIntValue("MAXBP");
            }
        }
        return maxBp;
    }

    private BigDecimal calculatePrice(String userLevel, String priviStr, BigDecimal salePrice) {
        if (StringUtils.isBlank(priviStr)) {
            return salePrice;
        } else {
            JSONObject priviJson = JSONObject.parseObject(priviStr);
            if (priviJson.containsKey(userLevel)) {
                return new BigDecimal(priviJson.getString(userLevel));
            } else {
                return salePrice;
            }
        }
    }

    private JSONObject settingOfferInfo(String offerId, String offerName, String pic, BigDecimal price, int maxBp) {
        JSONObject offer = new JSONObject();
        offer.put("offerId", offerId);
        offer.put("offerName", offerName);
        offer.put("pic", pic);
        offer.put("price", price);
        offer.put("maxBp", maxBp);
        return offer;
    }


    private void setAttrList(JSONObject result) {
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
     * @param paramOrder#objId 必填，购买对象（商品ID，活动ID） orderType 必填，1 商品 3 VIP 5 活动 payType   必填，支付方式 1：微信支付 3：余额支付
     * @return
     */
    @RequestMapping("/api/createOrder")
    @ResponseBody
    public BaseResult createOrder(HttpServletRequest request, OrderDto paramOrder,String attrListStr) {
        if (StringUtils.isBlank(paramOrder.getObjId()) || paramOrder.getOrderType() == null
                || paramOrder.getPayType() == null) {
            return new BaseResult(BaseConstant.FAILED, "关键参数缺失");
        }
        if(StringUtils.isNotBlank(attrListStr)){
            paramOrder.setAttrList(JSON.parseArray(attrListStr, OrderAttrDto.class));
        }
        String custId = CookieUtil.getCustId(request);
        CustDto custDto = custService.queryCustByCustId(custId).getData();
        String lv = custDto.getUserLevel();
        // 订单
        OrderDto orderDto = new OrderDto();
        String privi = "";
        String metaDataStr = "";
        int maxBpUser = 0;
        BaseResult<Boolean> hasBuyOrder = orderService.hasBuy(custDto.getCustId(), paramOrder.getOrderType(), paramOrder.getObjId());
        if (hasBuyOrder.getCode() == BaseConstant.FAILED) {
            return new BaseResult(BaseConstant.FAILED, hasBuyOrder.getMsg());
        }
        if (paramOrder.getOrderType() == 1) {
            OfferDto offerDto = offerService.queryOffer(paramOrder.getObjId()).getData();
            if (offerDto == null) {
                return new BaseResult(BaseConstant.FAILED, "商品不存在");
            }
            orderDto.setOrderName(offerDto.getOfferName());
            privi = offerDto.getBuyPrivi();
            metaDataStr = offerDto.getMetaData();
        } else if (paramOrder.getOrderType() == 3) {
            OfferDto offerDto = offerService.queryOffer(paramOrder.getObjId()).getData();
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
        } else if (paramOrder.getOrderType() == 5) {
            ActiveDto activeDto = activeService.queryActive(paramOrder.getObjId()).getData();
            if (activeDto == null) {
                return new BaseResult(BaseConstant.FAILED, "活动不存在");
            }
            orderDto.setOrderName(activeDto.getActiveName());
            privi = activeDto.getActivePrivi();
            metaDataStr = activeDto.getMetaData();
        }
        if (paramOrder.getOrderType() != 3) {
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
            if (StringUtils.isNotBlank(metaDataStr)) {
                JSONObject metaDataJson = JSONObject.parseObject(metaDataStr);
                maxBpUser = metaDataJson.getIntValue("MAXBP");
            }
        }
        if (paramOrder.getBp() != null) {
            if (paramOrder.getBp().compareTo(new BigDecimal(maxBpUser)) > 0) {
                return new BaseResult(BaseConstant.FAILED, "当前商品最多使用" + maxBpUser + "积分");
            } else {
                // 减去抵扣的积分
                orderDto.setOrderMoney(orderDto.getOrderMoney().subtract((paramOrder.getBp().divide(BigDecimal.TEN))));
            }
        }
        if (orderDto.getOrderMoney().compareTo(BigDecimal.ZERO) <= 0) {
            return new BaseResult(BaseConstant.FAILED, "实际付款金额不能小于0");
        }
        orderDto.setObjId(paramOrder.getObjId());
        orderDto.setOrderType(paramOrder.getOrderType());
        orderDto.setPayType(paramOrder.getPayType());
        orderDto.setCustId(custDto.getCustId());
        orderDto.setBp(paramOrder.getBp());
        orderDto.setDiscount(10L);
        String ip = IPUtil.getIpAddr(request);
        return orderService.createOrder(custDto.getOpenid(), ip, orderDto, paramOrder.getAttrList());
    }

    /**
     * 创建订单
     *
     * @param orderId
     * @param password
     * @return
     */
    @RequestMapping("/api/order/pay")
    @ResponseBody
    public BaseResult payOrder(HttpServletRequest request, String orderId, String password) {
        String custId = CookieUtil.getCustId(request);
        String key = "PAY_ORDER_" + orderId;
        if (jedisUtil.setnx(key, "true", 60) > 0) {
            try {
                BaseResult<OrderDto> baseResult = orderService.queryOrder(orderId);
                if (baseResult.getCode() == BaseConstant.FAILED) {
                    return baseResult;
                }
                OrderDto orderDto = baseResult.getData();
                if (orderDto == null) {
                    return new BaseResult(BaseConstant.FAILED, "订单不存在");
                }
                if (orderDto.getStatus() == 3) {
                    return new BaseResult(BaseConstant.SUCCESS, "订单处理成功");
                }
                BaseResult<Boolean> payBaseResult = accountService.reduce(custId, password, orderDto.getOrderMoney(),
                        orderDto.getBp(), orderId, orderDto.getOrderName());
                if (payBaseResult.getCode() == BaseConstant.SUCCESS && payBaseResult.getData()){
                    // 支付成功
                    return orderService.completeOrder(orderId, null, 3, "支付成功");
                } else {
                    return payBaseResult;
                }
            } catch (Exception e) {
                logger.error("支付失败：", e);
                return new BaseResult(BaseConstant.FAILED, "支付异常，请联系管理员");
            } finally {
                jedisUtil.del(key);
            }
        } else {
            return new BaseResult(BaseConstant.FAILED, "订单处理中");
        }
    }
}
