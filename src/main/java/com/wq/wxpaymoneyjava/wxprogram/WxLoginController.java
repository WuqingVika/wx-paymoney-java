package com.wq.wxpaymoneyjava.wxprogram;

import com.wq.wxpaymoneyjava.constant.Constant;
import com.wq.wxpaymoneyjava.pojo.PayInfo;
import com.wq.wxpaymoneyjava.pojo.PayInitParam;
import com.wq.wxpaymoneyjava.pojo.WXSessionModel;
import com.wq.wxpaymoneyjava.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.wq.wxpaymoneyjava.constant.Constant.APP_ID;
import static com.wq.wxpaymoneyjava.constant.Constant.APP_SECRET;

@RestController
public class WxLoginController {
    @Autowired
    private HttpServletRequest request;//注入请求  也可以写在方法里 这里直接作为全局变量来使用了
    private static Logger log = LoggerFactory.getLogger(WxLoginController.class);

    @PostMapping("/wxlogin")
    public JSONResult getOpenIdInfo(String code/*, HttpServletRequest request*/) {
        log.info("后端接收到的code:{}", code);
        //https://api.weixin.qq.com/sns/jscode2session?appid=APPID&secret=SECRET&js_code=JSCODE&grant_type=authorization_code
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        Map<String, String> param = new HashMap<>();
        param.put("appid", APP_ID);
        param.put("secret", APP_SECRET);
        param.put("js_code", code);
        param.put("grant_type", "authorization_code");
        String wxResult = HttpClientUtil.doGet(url, param);
        WXSessionModel model = JsonUtils.jsonToPojo(wxResult, WXSessionModel.class);
        String clientIP = IpUtils.getIpAddr(request);//ipv4格式
        log.info("clientIp:{}", clientIP);
        String randomNonceStr = RandomUtils.generateMixString(32);
        //拿到微信返回的prepay_id很重要！！
        String prepayId = unifiedOrder(model.getOpenid(), clientIP, randomNonceStr);
        if (StringUtils.isNotBlank(prepayId)) {
            //拿到以后返回给前端
            PayInitParam payInitParam = new PayInitParam();
            payInitParam.setPrepayId(prepayId);
            payInitParam.setNonceStr(randomNonceStr);
            payInitParam.setOpenid(model.getOpenid());
            payInitParam.setSession_key(model.getSession_key());
            return JSONResult.ok(payInitParam);
        } else {
            return JSONResult.errorMsg("拿不到prepayId请确认相关参数是否正常");
        }

    }

    /**
     * 调用统一下单接口
     *
     * @param openId
     */
    private String unifiedOrder(String openId, String clientIP, String randomNonceStr) {

        try {
            String url = Constant.URL_UNIFIED_ORDER;
            PayInfo payInfo = createPayInfo(openId, clientIP, randomNonceStr);
            String md5 = getSign(payInfo);
            payInfo.setSign(md5);
            log.info("md5 value: {}", md5);
            String xml = CommonUtil.payInfoToXML(payInfo);
            xml = xml.replace("\n", "")
                    .replace("__", "_")
                    .replace("<![CDATA[1]]>", "1");
            log.info("最后请求的入参xml====={}", xml);//这个可以直接拿到微信校验工具里去校验。一定要校验通过
            StringBuffer buffer = HttpUtil.httpsRequest(url, "POST", xml);
            log.info("unifiedOrder request return body: \n" + buffer.toString());
            // System.out.println("unifiedOrder request return body: \n" + buffer.toString());
            Map<String, String> result = CommonUtil.parseXml(buffer.toString());
            String return_code = result.get("return_code");
            if (StringUtils.isNotBlank(return_code) && return_code.equals("SUCCESS")) {
                String return_msg = result.get("return_msg");
                if (StringUtils.isNotBlank(return_msg) && !return_msg.equals("OK")) {
                    log.error("统一下单错误！");
                    // System.out.println("统一下单错误！");
                    return "";
                }
                String prepay_Id = result.get("prepay_id");
                return prepay_Id;

            } else {
                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }


    /**
     * 创建订单信息
     *
     * @param openId
     * @param clientIP
     * @param randomNonceStr
     * @return
     */
    private PayInfo createPayInfo(String openId, String clientIP, String randomNonceStr) {
        Date date = new Date();
        String timeStart = TimeUtils.getFormatTime(date, Constant.TIME_FORMAT);
        String timeExpire = TimeUtils.getFormatTime(TimeUtils.addDay(date, Constant.TIME_EXPIRE), Constant.TIME_FORMAT);
        String randomOrderId = CommonUtil.getRandomOrderId();
        PayInfo payInfo = new PayInfo();
        payInfo.setAppid(APP_ID);
        payInfo.setMch_id(Constant.MCH_ID);
        payInfo.setDevice_info("WEB");
        payInfo.setNonce_str(randomNonceStr);
        payInfo.setSign_type("MD5");  //默认即为MD5
        payInfo.setBody("可爱过头的小吴庆");//长度好像有限制
        payInfo.setAttach("wuqingvikateam");
        payInfo.setOut_trade_no(randomOrderId);//这个是商家自己的订单id这里我就随机生成
        payInfo.setTotal_fee(1);//单位是分，即0.01元
        payInfo.setSpbill_create_ip(clientIP);//客户端下单的ip这个需要获取下 我这里测试为了方便写死了
        payInfo.setTime_start(timeStart);
        payInfo.setTime_expire(timeExpire);
        payInfo.setNotify_url(Constant.URL_NOTIFY);//支付成功回调地址
        payInfo.setTrade_type("JSAPI");//这里固定
        payInfo.setLimit_pay("no_credit");//固定
        payInfo.setOpenid(openId);
        return payInfo;
    }


    private String getSign(PayInfo payInfo) throws Exception {
        StringBuffer sb = new StringBuffer();
        //这里踩了坑  这些参数需要排序。你可以写个排序方法按Ascii码值 网上找一大把，这里我就把排序写死了
        //但是以后说不准微信还要加参啥的 ，所以最好写个方法排序下。
        sb.append("appid=" + payInfo.getAppid())
                .append("&attach=" + payInfo.getAttach())
                .append("&body=" + payInfo.getBody())
                .append("&device_info=" + payInfo.getDevice_info())
                .append("&limit_pay=" + payInfo.getLimit_pay())
                .append("&mch_id=" + payInfo.getMch_id())//
                .append("&nonce_str=" + payInfo.getNonce_str())
                .append("&notify_url=" + payInfo.getNotify_url())
                .append("&openid=" + payInfo.getOpenid())
                .append("&out_trade_no=" + payInfo.getOut_trade_no())
                .append("&sign_type=" + payInfo.getSign_type())
                .append("&spbill_create_ip=" + payInfo.getSpbill_create_ip())
                .append("&time_expire=" + payInfo.getTime_expire())
                .append("&time_start=" + payInfo.getTime_start())
                .append("&total_fee=" + payInfo.getTotal_fee())
                .append("&trade_type=" + payInfo.getTrade_type())
                .append("&key=" + Constant.APP_KEY);
        log.info("排序后的拼接参数：{}", sb.toString());
        return CommonUtil.getMD5(sb.toString()).toUpperCase();
    }


}
