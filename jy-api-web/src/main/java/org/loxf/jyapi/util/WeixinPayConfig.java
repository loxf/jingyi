package org.loxf.jyapi.util;

import com.github.wxpay.sdk.WXPayConfig;
import org.apache.http.ssl.SSLContexts;
import org.loxf.jyadmin.base.constant.BaseConstant;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

public class WeixinPayConfig implements WXPayConfig {
    private static SSLContext sslContextWeixin;

    public WeixinPayConfig() throws Exception {
        initCert();
    }

    public void initCert() throws Exception {
        String WEIXIN_APICLIENT_CERT = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WEIXIN_APICLIENT_CERT").getConfigValue();
        File file = new File(WEIXIN_APICLIENT_CERT);
        InputStream certStream = new FileInputStream(file);
        this.certData = new byte[(int) file.length()];
        certStream.read(this.certData);
        certStream.close();
    }

    public static SSLContext queryWeixinSSL() throws Exception {
        if (sslContextWeixin == null) {
            String WX_MCHID = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_MCHID").getConfigValue();
            String WEIXIN_APICLIENT_CERT = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WEIXIN_APICLIENT_CERT").getConfigValue();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream instream = new FileInputStream(new File(WEIXIN_APICLIENT_CERT));
            keyStore.load(instream, WX_MCHID.toCharArray());
            instream.close();
            // Trust own CA and all self-signed certs
            sslContextWeixin = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, WX_MCHID.toCharArray())
                    .build();
        }
        return sslContextWeixin;
    }

    private byte[] certData;

    @Override
    public String getAppID() {
        String WX_APPID = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_APPID").getConfigValue();
        return WX_APPID;
    }

    @Override
    public String getMchID() {
        String WX_MCHID = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_MCHID").getConfigValue();
        return WX_MCHID;
    }

    @Override
    public String getKey() {
        String WX_MCH_KEY = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_RUNTIME, "WX_MCH_KEY").getConfigValue();
        return WX_MCH_KEY;
    }

    @Override
    public InputStream getCertStream() {
        return null;
    }

    @Override
    public int getHttpConnectTimeoutMs() {
        return 0;
    }

    @Override
    public int getHttpReadTimeoutMs() {
        return 0;
    }
}
