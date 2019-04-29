package com.wq.wxpaymoneyjava.constant;

/**
 * Created by Hyman on 2017/2/27.
 */
public class Constant {

    public static final String  DOMAIN= "域名";

    public static final String APP_ID = "填appId";

    public static final String APP_SECRET = "appsecret 和appId混双获取openId用的";

    public static final String APP_KEY = "这个是给商户号设置的密钥（自己设置的32位）";

    public static final String MCH_ID = "商户号";  //商户号

    public static final String URL_UNIFIED_ORDER = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    public static final String URL_NOTIFY = "https://www.baidu.com/随便写只要能访问到的，正常写你公司的回调请求";//Constant.DOMAIN + "/hello";

    public static final String TIME_FORMAT = "yyyyMMddHHmmss";

    public static final int TIME_EXPIRE = 2;  //订单过期时间单位是day

}
