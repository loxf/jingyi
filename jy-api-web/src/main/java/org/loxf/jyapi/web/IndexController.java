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
        if(friendLinkDtoPageResult.getCode()== BaseConstant.SUCCESS && friendLinkDtoPageResult.getData()!=null){
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
        OfferDto offerDto = new OfferDto();
        Pager pager = new Pager(page, size);
        offerDto.setPager(pager);
        offerDto.setCatalogId(catalogId);
        if(StringUtils.isNotBlank(filter)) {
            if(filter.equals("NONE") || filter.equals("VIP") || filter.equals("SVIP")) {
                offerDto.setBuyPrivi("\""+ filter + "\"");
            } else if(filter.equals("CLASS") || filter.equals("OFFER") ){
                offerDto.setOfferType(filter);
            }
        }
        if(StringUtils.isNotBlank(sortType) && sortType.equals("HOT")){
            //TODO 热门怎么处理？
        }
        PageResult<OfferDto> offerDtoPageResult = offerService.pager(offerDto);
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
    public BaseResult activeList(Integer page, Integer size){
        return new BaseResult();
    }

    /**
     * 课程分类接口
     * @return
     */
    @RequestMapping("/api/offer/catalog")
    @ResponseBody
    public BaseResult offerCatalogList(int size){
        return new BaseResult();
    }
}
