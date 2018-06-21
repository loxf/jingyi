package org.loxf.jyapi.web;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.constant.WeChatMessageConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.client.dto.OrderDto;
import org.loxf.jyadmin.client.service.AccountService;
import org.loxf.jyadmin.client.service.OrderService;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.WeixinPayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class WeixinController {
    private static Logger logger = LoggerFactory.getLogger(WeixinController.class);

    @Autowired
    private OrderService orderService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private JedisUtil jedisUtil;

    /**
     * 接入微信接口
     *
     * @param signature
     * @param timestamp
     * @param nonce
     * @param echostr
     * @return
     */
    @RequestMapping(value = "/api/weixin/api_access", method= RequestMethod.GET)
    @ResponseBody
    public String apiAccess(String signature, String timestamp, String nonce, String echostr) {
        // 1）将token、timestamp、nonce三个参数进行字典序排序
        String wxToken = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_TOKEN").getConfigValue();
        ArrayList<String> list = new ArrayList<String>();
        list.add(nonce);
        list.add(timestamp);
        list.add(wxToken);
        Collections.sort(list);
        //2）将三个参数字符串拼接成一个字符串进行sha1加密
        String ret = DigestUtils.shaHex(list.get(0) + list.get(1) + list.get(2));
        //3）开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
        if (ret.equals(signature)) {
            return echostr;
        }
        return "";
    }

    /**
     * 接入微信接口
     *
     * @return
     */
    @RequestMapping(value = "/api/weixin/api_access", method= RequestMethod.POST)
    public void wxAccess(HttpServletRequest request, HttpServletResponse response) {
        //读取参数
        StringBuffer notifyData = new StringBuffer();
        try {
            InputStream inputStream = request.getInputStream();
            String s;
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while ((s = in.readLine()) != null) {
                notifyData.append(s);
            }
            in.close();
            inputStream.close();
            logger.debug("接收到的微信POST消息：" + notifyData.toString());
            Map msgMap = WXPayUtil.xmlToMap(notifyData.toString());
            if(msgMap.containsKey("MsgType")) {
                if(msgMap.get("MsgType").equals(WeChatMessageConstant.MESSAGE_EVENT)) {
                    // 事件推送
                    String msgPic = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_COM, "WX_SUB_MSG_PIC",
                            "aguvLUu8kOua8acj1qnOjaHQjG9GhBb7ogQSlGUjreQ").getConfigValue();
                    if(msgMap.containsKey("Event") && msgMap.get("Event").equals(WeChatMessageConstant.MESSAGE_EVENT_SUBSCRIBE)) {
                        Map map = createMsgResp((String)msgMap.get("FromUserName"), System.currentTimeMillis(),
                                WeChatMessageConstant.MESSAGE_IMAGE, msgPic);
                        responseXml(map2Xmlstring(map, "Image"), response);
                        return;
                    }
                }
                /*else {
                    String text = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_COM, "WX_REPLY_MSG_TEXT",
                            "亲爱的会员，我们还在努力的构建智能客服系统，如果你有疑问，现在可以直接咨询班主任。").getConfigValue();
                    Map map = createMsgResp((String)msgMap.get("FromUserName"), System.currentTimeMillis(), WeChatMessageConstant.MESSAGE_TEXT, text);
                    responseXml(map2Xmlstring(map, null), response);
                    return;
                }*/
            }
        } catch (IOException e) {
            logger.error("微信POST消息异常", e);
        } catch (Exception e) {
            logger.error("微信POST消息异常", e);
        }
        responseXml("", response);
    }

    @RequestMapping("/api/weixin/payorder")
    public void payorder(HttpServletRequest request, HttpServletResponse response) {
        //读取参数
        StringBuffer notifyData = new StringBuffer();
        try {
            InputStream inputStream = request.getInputStream();
            String s;
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            while ((s = in.readLine()) != null) {
                notifyData.append(s);
            }
            logger.debug("微信支付原始报文：" + notifyData.toString());
            in.close();
            inputStream.close();
        } catch (IOException e) {
            logger.error("微信支付回调编码错误", e);
            responseXml(createResp(FAIL, "微信支付回调异常"), response);
            return;
        }
        //------------------------------
        //处理业务
        //------------------------------
        BaseResult<Map<String, String>> notifyMapResult = payNotifySign(notifyData.toString());
        String resXml = "";
        if (notifyMapResult.getCode() == BaseConstant.SUCCESS) {
            Map<String, String> notifyMap = notifyMapResult.getData();
            String out_trade_no = notifyMap.get("out_trade_no");// 交易订单ID
            String key = "WEIXIN_CALLBACK_" + out_trade_no;
            String transaction_id = notifyMap.get("transaction_id");// 微信支付订单
            String total_fee = notifyMap.get("total_fee");// 订单金额，单位为分
            if (jedisUtil.setnx(key, "true", 60) > 0) {
                try {
                    // 获取订单
                    BaseResult<OrderDto> orderDtoBaseResult = orderService.queryOrder(out_trade_no);
                    if (orderDtoBaseResult.getCode() == BaseConstant.FAILED || orderDtoBaseResult.getData() == null) {
                        resXml = createResp(FAIL, "获取商户订单失败");
                    } else {
                        OrderDto orderDto = orderDtoBaseResult.getData();
                        if (orderDto.getStatus() == 3) {
                            resXml = createResp(SUCCESS, "处理成功");
                        } else {
                            long orderMoney = orderDto.getOrderMoney().multiply(new BigDecimal(100)).longValue();
                            if (orderMoney != Long.parseLong(total_fee)) {
                                resXml = createResp(FAIL, "订单金额不一致");
                            } else {
                                // 调用第三方支付记录账单
                                BaseResult accountResult = accountService.reduceByThird(orderDto.getCustId(), orderDto.getOrderMoney(), orderDto.getBp(),
                                        orderDto.getOrderId(), orderDto.getOrderName());
                                if (accountResult.getCode() == BaseConstant.SUCCESS) {
                                    // 完成订单
                                    BaseResult completeOrderResult = orderService.completeOrder(out_trade_no, transaction_id, 3, "");
                                    /*if(completeOrderResult.getCode()==BaseConstant.SUCCESS && (orderDto.getOrderType()==3
                                            || orderDto.getOrderType()==1)){
                                        // 如果购买的是VIP，设置用户信息刷新标志
                                        jedisUtil.set("REFRESH_CUST_INFO_" + orderDto.getCustId(), "true", 60);
                                    }*/
                                    resXml = createResp(completeOrderResult.getCode() == BaseConstant.SUCCESS ? SUCCESS : FAIL,
                                            completeOrderResult.getMsg());
                                } else {
                                    resXml = createResp(FAIL, accountResult.getMsg());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("微信支付回调失败：", e);
                    resXml = createResp(FAIL, "微信支付回调异常");
                } finally {
                    jedisUtil.del(key);
                }
            } else {
                resXml = createResp(FAIL, "当前订单正在处理");
            }
        } else {
            resXml = createResp(FAIL, notifyMapResult.getMsg());
        }
        responseXml(resXml, response);
    }

    private void responseXml(String resXml, HttpServletResponse response) {
        try {
            logger.info("微信回调返回：" + resXml);
            BufferedOutputStream out = new BufferedOutputStream(
                    response.getOutputStream());
            out.write(resXml.getBytes());
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.error("返回微信支付回调失败", e);
        }
    }

    private static String FAIL = "FAIL";
    private static String SUCCESS = "SUCCESS";

    private String createResp(String code, String msg) {
        return "<xml>" + "<return_code><![CDATA[" + code + "]]></return_code>"
                + "<return_msg><![CDATA[" + msg + "]]></return_msg>" + "</xml> ";
    }

    private Map createMsgResp(String toUserName, long CreateTime, String MsgType, String ContentOrMediaId) {
        String wxId = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_COM, "WX_ID").getConfigValue();
        Map map = new HashMap();
        map.put("ToUserName", toUserName);
        map.put("FromUserName", wxId);
        map.put("CreateTime", (CreateTime/1000) + "");
        map.put("MsgType", MsgType);
        if (MsgType.equals(WeChatMessageConstant.MESSAGE_TEXT)) {
            map.put("Content", ContentOrMediaId);
        } else if (MsgType.equals(WeChatMessageConstant.MESSAGE_IMAGE) || MsgType.equals(WeChatMessageConstant.MESSAGE_VOICE)) {
            map.put("MediaId", ContentOrMediaId);
        } else {
            map.put("Content", ContentOrMediaId);
        }
        return map;
    }

    public static BaseResult<Map<String, String>> payNotifySign(String notifyData) {
        Map<String, String> notifyMap = null;  // 转换成map
        try {
            notifyMap = WXPayUtil.xmlToMap(notifyData);
            if (notifyMap.get("result_code").equals("SUCCESS")) {
                String env = notifyMap.get("device_info");
                WeixinPayConfig config = new WeixinPayConfig();
                WXPay wxpay = new WXPay(config);
                if (wxpay.isPayResultNotifySignatureValid(notifyMap)) {
                    // 签名正确
                    return new BaseResult<>(notifyMap);
                } else {
                    return new BaseResult<>(BaseConstant.FAILED, "签名校验失败:" + notifyMap.get("err_code"));
                }
            } else {
                return new BaseResult<>(BaseConstant.FAILED, "微信端返回err_code:" + notifyMap.get("err_code"));
            }
        } catch (Exception e) {
            logger.error("微信返回内容转MAP异常", e);
            return new BaseResult<>(BaseConstant.FAILED, "微信返回内容转MAP异常");
        }
    }
    /**
     * Map转换成Xml
     * @param map
     * @return
     */
    public static String map2Xmlstring(Map<String,Object> map, String type){
        StringBuffer sb = new StringBuffer("");
        sb.append("<xml>");

        Set<String> set = map.keySet();
        for(Iterator<String> it=set.iterator(); it.hasNext();){
            String key = it.next();
            Object value = map.get(key);
            if(type!=null && key.equals("MediaId")){
                sb.append("<").append(type).append(">");
            }
            sb.append("<").append(key).append(">");
            sb.append("<![CDATA[").append(value).append("]]>");
            sb.append("</").append(key).append(">");
            if(type!=null && key.equals("MediaId")){
                sb.append("</").append(type).append(">");
            }
        }
        sb.append("</xml>");
        return sb.toString();
    }
}
