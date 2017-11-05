package org.loxf.jyapi.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;

public class LetvCloudV1 {
    private static Logger logger = LoggerFactory.getLogger(LetvCloudV1.class);
    private String userUnique;
    private String secretKey;
    private String restUrl = "http://api.letvcloud.com/open.php";
    public String format = "json";
    public String apiVersion = "2.0";

    // 定义视频状态常量
    public static final int ALL = 0;
    public static final int PLAY_OK = 10;
    public static final int FAILED = 20;
    public static final int WAIT = 30;

    public LetvCloudV1(String userUnique, String secretKey) {
        this.userUnique = userUnique;
        this.secretKey = secretKey;
    }

    /**
     * 视频上传初始化
     *
     * @return String
     * @throws Exception
     */
    public String videoUploadInit(String video_name, String client_ip, int file_size,int uploadtype) throws Exception {
        String api = "video.upload.init";
        HashMap<Object, Object> params = new HashMap<Object, Object>();

        params.put("video_name", video_name);
        params.put("uploadtype", uploadtype + "");
        if (client_ip.length() > 0) {
            params.put("client_ip", client_ip);
        }
        if (file_size > 0) {
            params.put("file_size", file_size + "");
        }
        return makeRequest(api, params);
    }

    /**
     * 视频上传 (web方式)
     *
     * @return String
     * @throws IOException
     */
    public String videoUpload(String video_file, String upload_url) throws IOException {
        File file = new File(video_file);
        return doUploadFile(file, upload_url);
    }

    /**
     * 视频断点续传
     *
     * @return String
     * @throws Exception
     */
    public String videoUploadResume(String token,int uploadtype) throws Exception {
        String api = "video.upload.resume";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("token", token);
        params.put("uploadtype", uploadtype+"");
        return makeRequest(api, params);
    }

    /**
     * 视频上传（Flash方式）
     *
     * @return String
     * @throws Exception
     */
    public String videoUploadFlash(String video_name, String js_callback, int flash_width, int flash_height,
            String client_ip) throws Exception {
        String api = "video.upload.flash";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_name", video_name);
        if (js_callback.length() > 0) {
            params.put("js_callback", js_callback);
        }
        if (flash_width > 0) {
            params.put("flash_width", flash_width + "");
        }
        if (flash_height > 0) {
            params.put("flash_height", flash_height + "");
        }
        if (client_ip.length() > 0) {
            params.put("client_ip", client_ip);
        }
        return makeRequest(api, params);
    }

    /**
     * 视频上传（Flash方式）
     *
     * @return String
     * @throws Exception
     */
    public String videoUploadFlash(String video_name, String js_callback, int flash_width, int flash_height)
            throws Exception {
        return videoUploadFlash(video_name, js_callback, flash_width, flash_height, "");
    }

    /**
     * 视频上传（Flash方式）
     *
     * @return String
     * @throws Exception
     */
    public String videoUploadFlash(String video_name, String js_callback, int flash_width) throws Exception {
        return videoUploadFlash(video_name, js_callback, flash_width, 0, "");
    }

    /**
     * 视频上传（Flash方式）
     *
     * @return String
     * @throws Exception
     */
    public String videoUploadFlash(String video_name, String js_callback) throws Exception {
        return videoUploadFlash(video_name, js_callback, 0, 0, "");
    }

    /**
     * 视频上传（Flash方式）
     *
     * @return String
     * @throws Exception
     */
    public String videoUploadFlash(String video_name) throws Exception {
        return videoUploadFlash(video_name, "", 0, 0, "");
    }

    /**
     * 视频信息更新
     *
     * @return String
     * @throws Exception
     */
    public String videoUpdate(int video_id, String video_name, String video_desc, String tag, int is_pay)
            throws Exception {
        String api = "video.update";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_id", video_id + "");
        if (video_name.length() > 0) {
            params.put("video_name", video_name);
        }
        if (video_desc.length() > 0) {
            params.put("video_desc", video_desc);
        }
        if (tag.length() > 0) {
            params.put("tag", tag);
        }
        if (is_pay == 0 || is_pay == 1) {
            params.put("is_pay", is_pay + "");
        }
        return makeRequest(api, params);
    }

    /**
     * 视频信息更新
     *
     * @return String
     * @throws Exception
     */
    public String videoUpdate(int video_id, String video_name, String video_desc, String tag) throws Exception {
        return videoUpdate(video_id, video_name, video_desc, tag, -1);
    }

    /**
     * 视频信息更新
     *
     * @return String
     * @throws Exception
     */
    public String videoUpdate(int video_id, String video_name, String video_desc) throws Exception {
        return videoUpdate(video_id, video_name, video_desc, "", -1);
    }

    /**
     * 视频信息更新
     *
     * @return String
     * @throws Exception
     */
    public String videoUpdate(int video_id, String video_name) throws Exception {
        return videoUpdate(video_id, video_name, "", "", -1);
    }

    /**
     * 视频信息更新
     *
     * @return String
     * @throws Exception
     */
    public String videoUpdate(int video_id) throws Exception {
        return videoUpdate(video_id, "", "", "", -1);
    }

    /**
     * 获取视频列表
     *
     * @return String
     * @throws Exception
     */
    public String videoList(int index, int size, int status) throws Exception {
        String api = "video.list";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        if (index > 0) {
            params.put("index", index + "");
        }
        if (size > 0) {
            params.put("size", size + "");
        }
        if (status == ALL || status == PLAY_OK || status == FAILED || status == WAIT) {
            params.put("status", status + "");
        }
        return makeRequest(api, params);
    }

    /**
     * 获取视频列表
     *
     * @return String
     * @throws Exception
     */
    public String videoList(int index, int size) throws Exception {
        return videoList(index, size, -1);
    }

    /**
     * 获取视频列表
     *
     * @return String
     * @throws Exception
     */
    public String videoList(int index) throws Exception {
        return videoList(index, 0, -1);
    }

    /**
     * 获取视频列表
     *
     * @return String
     * @throws Exception
     */
    public String videoList() throws Exception {
        return videoList(0, 0, -1);
    }

    /**
     * 获取单个视频信息
     *
     * @param videoid 视频id
     * @return
     * @throws Exception
     */
    public String videoGet(int videoid) throws Exception {
        String api = "video.get";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_id", videoid + "");
        return makeRequest(api, params);
    }

    /**
     * 删除视频
     *
     * @return String
     * @throws Exception
     */
    public String videoDel(int video_id) throws Exception {
        String api = "video.del";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_id", video_id + "");
        return makeRequest(api, params);
    }

    /**
     * 批量删除视频
     *
     * @return String
     * @throws Exception
     */
    public String videoDelBatch(String video_id_list) throws Exception {
        String api = "video.del.batch";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_id_list", video_id_list);
        return makeRequest(api, params);
    }

    /**
     * 视频暂停
     *
     * @return String
     * @throws Exception
     */
    public String videoPause(int video_id) throws Exception {
        String api = "video.pause";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_id", video_id + "");
        return makeRequest(api, params);
    }

    /**
     * 视频恢复
     *
     * @return String
     * @throws Exception
     */
    public String videoRestore(int video_id) throws Exception {
        String api = "video.restore";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_id", video_id + "");
        return makeRequest(api, params);
    }

    /**
     * 获取视频截图
     *
     * @return String
     * @throws Exception
     */
    public String imageGet(int video_id, String size) throws Exception {
        String api = "image.get";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("video_id", video_id + "");
        params.put("size", size);
        return makeRequest(api, params);
    }

    /**
     * 视频小时数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoHour(String date, int hour, int video_id, int index, int size) throws Exception {
        String api = "data.video.hour";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("date", date);
        if (hour >= 0 && hour <= 23) {
            params.put("hour", hour + "");
        }
        if (video_id > 0) {
            params.put("video_id", video_id + "");
        }
        if (index > 0) {
            params.put("index", index + "");
        }
        if (size > 0) {
            params.put("size", size + "");
        }
        return makeRequest(api, params);
    }

    /**
     * 视频小时数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoHour(String date, int hour, int video_id, int index) throws Exception {
        return dataVideoHour(date, hour, video_id, index, 0);
    }

    /**
     * 视频小时数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoHour(String date, int hour, int video_id) throws Exception {
        return dataVideoHour(date, hour, video_id, 0, 0);
    }

    /**
     * 视频小时数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoHour(String date, int hour) throws Exception {
        return dataVideoHour(date, hour, 0, 0, 0);
    }

    /**
     * 视频小时数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoHour(String date) throws Exception {
        return dataVideoHour(date, -1, 0, 0, 0);
    }

    /**
     * 视频天数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoDate(String start_date, String end_date, int video_id, int index, int size) throws Exception {
        String api = "data.video.date";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("start_date", start_date);
        params.put("end_date", end_date);
        if (video_id > 0) {
            params.put("video_id", video_id + "");
        }
        if (index > 0) {
            params.put("index", index + "");
        }
        if (size > 0) {
            params.put("size", size + "");
        }
        return makeRequest(api, params);
    }

    /**
     * 视频天数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoDate(String start_date, String end_date, int video_id, int index) throws Exception {
        return dataVideoDate(start_date, end_date, video_id, index, 0);
    }

    /**
     * 视频天数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoDate(String start_date, String end_date, int video_id) throws Exception {
        return dataVideoDate(start_date, end_date, video_id, 0, 0);
    }

    /**
     * 视频天数据
     *
     * @return String
     * @throws Exception
     */
    public String dataVideoDate(String start_date, String end_date) throws Exception {
        return dataVideoDate(start_date, end_date, 0, 0, 0);
    }

    /**
     * 所有数据
     *
     * @return String
     * @throws Exception
     */
    public String dataTotalDate(String start_date, String end_date, int index, int size) throws Exception {
        String api = "data.total.date";
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("start_date", start_date);
        params.put("end_date", end_date);
        if (index > 0) {
            params.put("index", index + "");
        }
        if (size > 0) {
            params.put("size", size + "");
        }
        return makeRequest(api, params);
    }

    /**
     * 所有数据
     *
     * @return String
     * @throws Exception
     */
    public String dataTotalDate(String start_date, String end_date, int index) throws Exception {
        return dataTotalDate(start_date, end_date, index, 0);
    }

    /**
     * 所有数据
     *
     * @return String
     * @throws Exception
     */
    public String dataTotalDate(String start_date, String end_date) throws Exception {
        return dataTotalDate(start_date, end_date, 0, 0);
    }

    /**
     * 获取视频播放接口
     *
     * @return String
     */
    public String videoGetPlayinterface(String uu, String vu, String type, String pu, int auto_play, int width,
            int height) {
        HashMap<Object, Object> params = new HashMap<Object, Object>();
        params.put("uu", uu);
        params.put("vu", vu);
        if (pu.length() > 0) {
            params.put("pu", pu);
        }
        if (auto_play != -1) {
            params.put("auto_play", auto_play + "");
        }
        if (width > 0) {
            params.put("width", width + "");
        } else {
            width = 800;
        }
        if (height > 0) {
            params.put("height", height + "");
        } else {
            height = 450;
        }
        String queryString = mapToQueryString(params);
        String jsonString = mapToJsonString(params);
        String response = "";
        if ("url".equalsIgnoreCase(type)) {
            response = "http://yuntv.letv.com/bcloud.html?" + queryString;
        }
        if ("js".equalsIgnoreCase(type)) {
            response =
                    "<script type=\"text/javascript\">var letvcloud_player_conf = "
                            + jsonString
                            + ";</script><script type=\"text/javascript\" src=\"http://yuntv.letv.com/bcloud.js\"></script>";
        }
        if ("flash".equalsIgnoreCase(type)) {
            response = "http://yuntv.letv.com/bcloud.swf?" + queryString;
        }
        if ("html".equalsIgnoreCase(type)) {
            response =
                    "<embed src=\"http://yuntv.letv.com/bcloud.swf\" allowFullScreen=\"true\" quality=\"high\" width=\""
                            + width + "\" height=\"" + height
                            + "\" align=\"middle\" allowScriptAccess=\"always\" flashvars=\"" + queryString
                            + "\" type=\"application/x-shockwave-flash\"></embed>";
        }
        return response;
    }

    /**
     * 获取视频播放接口
     *
     * @return String
     */
    public String videoGetPlayinterface(String uu, String vu, String type, String pu, int auto_play, int width) {
        return videoGetPlayinterface(uu, vu, type, pu, auto_play, width, 0);
    }

    /**
     * 获取视频播放接口
     *
     * @return String
     */
    public String videoGetPlayinterface(String uu, String vu, String type, String pu, int auto_play) {
        return videoGetPlayinterface(uu, vu, type, pu, auto_play, 0, 0);
    }

    /**
     * 获取视频播放接口
     *
     * @return String
     */
    public String videoGetPlayinterface(String uu, String vu, String type, String pu) {
        return videoGetPlayinterface(uu, vu, type, pu, -1, 0, 0);
    }

    /**
     * 获取视频播放接口
     *
     * @return String
     */
    public String videoGetPlayinterface(String uu, String vu, String type) {
        return videoGetPlayinterface(uu, vu, type, "", -1, 0, 0);
    }

    /**
     * 构造云视频Sign
     *
     * @param params 业务参数
     * @return string
     * @throws Exception
     */
    public String generateSign(HashMap<Object, Object> params) throws Exception {
        Object[] array = params.keySet().toArray();
        java.util.Arrays.sort(array);
        String keyStr = "";
        for (int i = 0; i < array.length; i++) {
            String key = array[i].toString();
            keyStr += key + params.get(key);
        }
        keyStr += this.secretKey;
        return MD5.md5(keyStr);
    }

    // 构造请求串
    public String makeRequest(String api, HashMap<Object, Object> params) throws Exception {
        params.put("user_unique", this.userUnique);
        // 获取时间戳
        Date date = new Date();
        long time = date.getTime();
        String ts = time + "";
        params.put("timestamp", ts);
        params.put("ver", this.apiVersion);
        params.put("format", this.format);
        params.put("api", api);
        // 签名
        params.put("sign", generateSign(params));
        // 构造请求URL
        String resurl = "";
        resurl += this.restUrl + "?" + mapToQueryString(params);
        System.out.println(resurl);
        return doGet(resurl);
    }

    // GET请求
    public String doGet(String url) throws IOException {
        //实例化get请求
        HttpGet request = new HttpGet(url);
        //实例化客户端
        CloseableHttpClient client = HttpClients.custom().build();
        //执行该请求,得到服务器端的响应内容
        CloseableHttpResponse response = client.execute(request);
        try {
            String result = null;
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                //把响应结果转成String
                result = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
            return result;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    // POST上传文件
    public String doUploadFile(File file, String url) throws IOException {
        if (!file.exists()) {
            return "file not exists";
        }
        HttpPost httpPost = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        // 上传的文件
        builder.addBinaryBody("filedata", file);

        try {
            HttpEntity httpEntity = builder.build();
            httpPost.setEntity(httpEntity);
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(httpPost);
            if (null == response || response.getStatusLine() == null) {
                return "fail";
            } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return "fail";
            } else {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            logger.error("上传失败", e);
        }
        return "fail";
    }

    // 将 map 中的参数及对应值转换为查询字符串
    private String mapToQueryString(HashMap<Object, Object> params) {
        Object[] array = params.keySet().toArray();

        java.util.Arrays.sort(array);
        String str = "";
        for (int i = 0; i < array.length; i++) {
            String key = array[i].toString();
            try {
                if (i != array.length - 1) {
                    str += key + "=" + URLEncoder.encode((String)params.get(key), "UTF-8") + "&";
                } else {
                    str += key + "=" + URLEncoder.encode((String)params.get(key), "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return str;
    }

    // 将 map 中的参数及对应值转换为json字符串
    private String mapToJsonString(HashMap<Object, Object> params) {
        Object[] array = params.keySet().toArray();

        java.util.Arrays.sort(array);
        String str = "";
        for (int i = 0; i < array.length; i++) {
            String key = array[i].toString();
            if (i != array.length - 1) {
                str += "\"" + key + "\"" + ":" + "\"" + params.get(key) + "\"" + ",";
            } else {
                str += "\"" + key + "\"" + ":" + "\"" + params.get(key) + "\"";
            }
        }
        str = "{" + str + "}";
        return str;
    }

    // MD5加密类
    private static class MD5 {
        private static char md5Chars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
                'f' };

        public static String md5(String str) throws Exception {
            MessageDigest md5 = getMD5Instance();
            md5.update(str.getBytes("UTF-8"));
            byte[] digest = md5.digest();
            char[] chars = toHexChars(digest);
            return new String(chars);
        }

        private static MessageDigest getMD5Instance() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ignored) {
                throw new RuntimeException(ignored);
            }
        }

        private static char[] toHexChars(byte[] digest) {
            char[] chars = new char[digest.length * 2];
            int i = 0;
            for (byte b : digest) {
                char c0 = md5Chars[(b & 0xf0) >> 4];
                chars[i++] = c0;
                char c1 = md5Chars[b & 0xf];
                chars[i++] = c1;
            }
            return chars;
        }
    }

}
