package cn.authing.gateway;

import cn.authing.core.UserInfo;
import cn.authing.permission.PermissionUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Component
public class PermissionFilter extends AbstractGatewayFilterFactory<PermissionFilter.Config> {

    private Map<String, Object> routePermission;

    public PermissionFilter() {
        super(Config.class);

        String path = "routePermission.json";
        try {
            String s = Files.readString(Paths.get(path));
            routePermission = JSON.parseObject(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (routePermission == null) {
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            Object permissionData = routePermission.get(path);
            if (permissionData == null) {
                // no permission control required
                return chain.filter(exchange);
            }

            String appId = request.getHeaders().getFirst("x-authing-app-id");
            String userPoolId = request.getHeaders().getFirst("x-authing-userpool-id");
            String authorization = request.getHeaders().getFirst("Authorization");
            UserInfo userInfo = new AuthClient().auth(appId, userPoolId, authorization);
            if (userInfo == null) {
                return error(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "Not logged");
            }

            Map<String, String> permissionMap = (Map<String, String>) permissionData;
            String type = permissionMap.get("type");
            if ("role".equals(type)) {
                String value = permissionMap.get("value");
                if (!PermissionUtil.hasRole(userInfo, value)) {
                    return error(exchange.getResponse(), HttpStatus.FORBIDDEN, "No permission");
                }
            } else if ("res".equals(type)) {
                String value = permissionMap.get("value");
                if (!PermissionUtil.canAccessResource(userInfo, value)) {
                    return error(exchange.getResponse(), HttpStatus.FORBIDDEN, "No permission");
                }
            }

            ServerWebExchange newExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.add("x-user-id", userInfo.getId());
                        headers.add("x-user-name", userInfo.getUsername());
                    })).build();
            return chain.filter(newExchange);
        };
    }

    public static class Config {
    }

    private Mono<Void> error(ServerHttpResponse response, HttpStatus code, String text) {
        response.setStatusCode(code);
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }
}
