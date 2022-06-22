package com.example;

import cn.authing.gateway.PermissionFilter;
import cn.authing.core.Authing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({ "cn.authing.gateway" })
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        Authing.setAppId("62a95682be15fc593002307b");
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder, PermissionFilter permissionFilter) {
        return builder.routes()
                .route(p -> p
                        .path("/**")
                        .filters(f -> f.filter(permissionFilter.apply(new
                                        PermissionFilter.Config())))
                        .uri("http://httpbin.org:80"))
                .build();
    }
}
