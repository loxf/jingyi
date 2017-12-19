package org.loxf.jyapi.web;

import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.base.bean.Pager;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.DateUtils;
import org.loxf.jyadmin.client.dto.*;
import org.loxf.jyadmin.client.service.*;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private CustSignService custSignService;
    @Autowired
    private FriendLinkService friendLinkService;
    @Autowired
    private IndexRecommendService recommendService;
    @Autowired
    private OfferService offerService;
    @Autowired
    private ActiveService activeService;
    @Autowired
    private OfferCatalogService offerCatalogService;
    @Autowired
    private AgentInfoService agentInfoService;

    @RequestMapping("/api/index/init")
    @ResponseBody
    public BaseResult initIndex(HttpServletRequest request){
        JSONObject result = new JSONObject();
        String custId = CookieUtil.getCustId(request);
        // 友商链接
        FriendLinkDto friendLinkDto = new FriendLinkDto();
        friendLinkDto.setStatus(1);
        PageResult<FriendLinkDto> friendLinkDtoPageResult = friendLinkService.queryAllLink(friendLinkDto, 1, 100);
        if(friendLinkDtoPageResult.getCode()== BaseConstant.SUCCESS){
            result.put("friendLink", friendLinkDtoPageResult.getData());
        }
        // 是否签到
        BaseResult<Boolean> custSignResult = custSignService.hasSign(custId, DateUtils.format(new Date()));
        result.put("isSign", custSignResult.getCode()==BaseConstant.SUCCESS && custSignResult.getData());
        // 推荐
        BaseResult<List<IndexRecommendDto>> recommendResult = recommendService.selectShow();
        if(recommendResult.getCode()==BaseConstant.SUCCESS && recommendResult.getData()!=null){
            result.put("recommend", recommendResult.getData());
        }
        return new BaseResult(result);
    }

    /**
     * 签到时间，当前为0
     * @param signType
     * @return
     */
    @RequestMapping("/api/sign")
    @ResponseBody
    public BaseResult initIndex(HttpServletRequest request, Integer signType){
        if(signType==null){
            return new BaseResult(BaseConstant.FAILED, "签到失败，签到类型不为0");
        }
        String signDate = DateUtils.format(new Date());
        String custId = CookieUtil.getCustId(request);
        BaseResult<Boolean> custSignResult = custSignService.sign(custId, signDate);
        if(custSignResult.getCode()==BaseConstant.SUCCESS && custSignResult.getData()) {
            return new BaseResult();
        } else {
            return new BaseResult(BaseConstant.FAILED, custSignResult.getMsg());
        }
    }

    /**
     * @param request
     * @param province
     * @param city
     * @param realName
     * @param phone
     * @param email
     * @param type
     * @return
     */
    @RequestMapping("/api/cust/beAgent")
    @ResponseBody
    public BaseResult beAgent(HttpServletRequest request, String province, String city, String realName, String phone, String email, Integer type){
        if(type==null || type>3 || type<1 || StringUtils.isBlank(realName) || StringUtils.isBlank(phone)){
            return new BaseResult(BaseConstant.FAILED, "参数不正确");
        }
        CustDto custDto = CookieUtil.getCust(request);
        AgentInfoDto dto = new AgentInfoDto();
        BeanUtils.copyProperties(custDto, dto);
        dto.setProvince(province);
        dto.setCity(city);
        dto.setRealName(realName);
        dto.setPhone(phone);
        dto.setEmail(email);
        dto.setType(type);
        return agentInfoService.addAgent(dto);
    }

    /**
     * @param catalogId
     * @param filter 筛选，NONE(免费)/VIP/SVIP /CLASS(商品)/OFFER(套餐)，默认空
     * @param sortType
     * @param page
     * @param size
     * @return
     */
    @RequestMapping("/api/offer/list")
    @ResponseBody
    public PageResult<JSONObject> offerList(String catalogId, String filter, String sortType, Integer page, Integer size){
        if(page==null){
            page = 1;
        }
        if(size==null){
            page = 10;
        }
        OfferDto offerDto = new OfferDto();
        Pager pager = new Pager(page, size);
        offerDto.setPager(pager);
        offerDto.setCatalogId(catalogId);
        if(StringUtils.isNotBlank(filter)) {
            if(filter.equals("NONE") || filter.equals("VIP") || filter.equals("SVIP")) {
                JSONObject filterJson = new JSONObject();
                filterJson.put(filter, "0");
                String filterStr = filterJson.toJSONString();
                offerDto.setBuyPrivi(filterStr.substring(1, filterStr.length()-1));
            } else if(filter.equals("CLASS") || filter.equals("OFFER") ){
                offerDto.setOfferType(filter);
            }
        }
        if(StringUtils.isNotBlank(sortType) && sortType.equals("HOT")){
            offerDto.setSortType("HOT");
        }
        PageResult<OfferDto> offerDtoPageResult = offerService.pager(offerDto, 2);
        List<JSONObject> list = new ArrayList<>();
        if(offerDtoPageResult.getTotal()>0){
            for(OfferDto tmp : offerDtoPageResult.getData()){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("createdAt", tmp.getCreatedAt());
                jsonObject.put("offerId", tmp.getOfferId());
                jsonObject.put("offerName", tmp.getOfferName());
                jsonObject.put("offerPic", tmp.getOfferPic());
                jsonObject.put("offerType", tmp.getOfferType());
                jsonObject.put("playTime", tmp.getPlayTime());
                String metaData = tmp.getMetaData();
                if(StringUtils.isNotBlank(metaData)){
                    JSONObject metaJson = JSON.parseObject(metaData);
                    String teacherStr = "";
                    if(metaJson.containsKey("TEACHER")){
                        JSONArray teachers = metaJson.getJSONArray("TEACHER");
                        for(Object o : teachers){
                            if(StringUtils.isNotBlank(teacherStr)){
                                teacherStr += ", ";
                            }
                            teacherStr += ((JSONObject) o ).get("name");
                        }
                    }
                    jsonObject.put("teachers", teacherStr);
                }
                String buyPrivi = tmp.getBuyPrivi();
                if(StringUtils.isNotBlank(buyPrivi)){
                    JSONObject buyPriviJson = JSON.parseObject(buyPrivi);
                    if(buyPriviJson.containsKey("NONE")&&buyPriviJson.getDouble("NONE")==0d){
                        jsonObject.put("freeType", "NONE");
                    } else if(buyPriviJson.containsKey("VIP")&&buyPriviJson.getDouble("VIP")==0d){
                        jsonObject.put("freeType", "VIP");
                    } else if(buyPriviJson.containsKey("SVIP")&&buyPriviJson.getDouble("SVIP")==0d){
                        jsonObject.put("freeType", "SVIP");
                    } else {
                        jsonObject.put("freeType", "");
                    }

                }
                jsonObject.put("price", tmp.getSaleMoney().toPlainString());
                list.add(jsonObject);
            }
        }

        return new PageResult<JSONObject> (offerDtoPageResult.getTotalPage(), offerDtoPageResult.getCurrentPage(),
                offerDtoPageResult.getTotal(), list);
    }

    /**
     * @return
     */
    @RequestMapping("/api/active/list")
    @ResponseBody
    public PageResult activeList(Integer page, Integer size){
        if (page == null) {
            page=1;
        }
        if (size == null) {
            size=10;
        }
        ActiveDto activeDto = new ActiveDto();
        Pager pager = new Pager(page, size);
        activeDto.setPager(pager);
        activeDto.setStatus(1);
        activeDto.setStartDate(DateUtils.format(new Date()));
        PageResult<ActiveDto> pageResult = activeService.pager(activeDto);
        List<JSONObject> result = new ArrayList<>();
        if(pageResult.getTotal()>0) {
            List<ActiveDto> list = pageResult.getData();
            for(ActiveDto dto : list){
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
                result.add(jsonObject);
            }
        }
        return new PageResult(pageResult.getTotalPage(), pageResult.getCurrentPage(), pageResult.getTotal(), result);
    }

    /**
     * 课程分类接口
     * @return
     */
    @RequestMapping("/api/offer/catalog")
    @ResponseBody
    public PageResult offerCatalogList(Integer size){
        if(size==null){
            size = 10;
        }
        OfferCatalogDto dto = new OfferCatalogDto();
        dto.setPager(new Pager(1, size));
        PageResult<OfferCatalogDto> pageResult = offerCatalogService.pager(dto);
        List<JSONObject> list = new ArrayList<>();
        if(pageResult.getTotal()>0){
            for(OfferCatalogDto tmp : pageResult.getData()) {
                JSONObject json = new JSONObject();
                json.put("catalogId", tmp.getCatalogId());
                json.put("catalogName", tmp.getCatalogName());
                json.put("pic", tmp.getPic());
                list.add(json);
            }
            return new PageResult(pageResult.getTotalPage(), pageResult.getCurrentPage(), pageResult.getTotal() ,list);
        }
        return pageResult;
    }
}
