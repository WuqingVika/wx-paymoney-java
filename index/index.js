const app = getApp()
var MD5Util = require('../utils/md5.js');
Page({
  data: {

  },
  
  doLogin: function(e) {
    console.log(e.detail.errMsg)
    console.log(e.detail.userInfo)
    console.log(e.detail.rawData)
    //登录
    wx.login({
      success(res) {
        console.log("-------------------")
        //获取登录的临时凭证，调用后端获取微信的sessionKey，secret

        wx.request({
          url: 'http://后端请求地址/wxlogin?code=' + res.code,
          method: 'POST',
          success: function(result) {
            console.log(result);
            var timestamp = String(Date.parse(new Date()))
            var nonceStr = result.data.data.nonceStr;
            var prepayId = result.data.data.prepayId;
            var payDataA = "appId=小程序appId&nonceStr=" + nonceStr + "&package=prepay_id=" + prepayId + "&signType=MD5&timeStamp=" + timestamp;
            var payDataB = payDataA + "&key=app_key跟商户id关联的那个，不是appsecret";
            // 使用MD5加密算法计算加密字符串
            var paySign = MD5Util.MD5(payDataB).toUpperCase();
            wx.requestPayment({
              timeStamp: timestamp,
              nonceStr: nonceStr,
              package: 'prepay_id=' + prepayId,
              signType: 'MD5',
              paySign: paySign,
              success(res) {
                console.log("pay success!")
              },
              fail(res) {
                console.log("pay fail!")
              }
            })
          }
        })
      }
    })
  },
  onLoad: function() {
    console.log('wuqingvika')
    console.log('https://mp.weixin.qq.com/debug/wxadoc/dev/devtools/devtools.html')
  },
})