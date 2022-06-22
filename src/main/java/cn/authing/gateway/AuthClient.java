package cn.authing.gateway;

import cn.authing.core.Authing;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import cn.authing.core.BaseResponse;
import cn.authing.core.UserInfo;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthClient {

    public UserInfo auth(String appId, String userPoolId, String authorization) {
        if (!StringUtils.hasLength(appId) || !StringUtils.hasLength(userPoolId) || !StringUtils.hasLength(authorization)) {
            return null;
        }

        authorization = authorization.replace("Bearer ", "");
        String url = "https://core." + Authing.getHost() + "/api/v2/users/me";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", authorization)
                .header("x-authing-app-id", appId)
                .header("x-authing-userpool-id", userPoolId)
                .build();
        try {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            BaseResponse<UserInfo> resp = JSON.parseObject(res.body(), new TypeReference<>() {
            });
            if (resp == null || resp.getCode() != 200) {
                return null;
            } else {
                return resp.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
