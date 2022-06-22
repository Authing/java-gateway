# Authing Spring Cloud Gateway 示例

> 基于 Spring cloud gateway 2.2.10.RELEASE，Java 版本 >= 11

## 引入依赖

目前采用源码方式提供，请拷贝本仓库代码中的 cn.authing 包到工程里面。

同时添加以下依赖：

```groovy
implementation 'com.alibaba:fastjson:1.2.73'
```

## 初始化

在标注了 @SpringBootApplication 的 Application 类里面调用：

```java
Authing.setAppId("{authing app id}");
```

对该类增加标注

```java
@ComponentScan({ "cn.authing.gateway" })
```

## 设置路由

下面例子设置 path 的 pattern 为 /**，即所有请求都要执行过滤器。开发者需要根据业务自行设置路由规则。

```java
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
```

## 创建端点 <-> 权限点映射表

在项目根目录，创建 routePermission.json 文件。内容参考示例：

```json
{
  "/get": {
    "type": "role",
    "value": "dev"
  },
  "/post": {
    "type": "res",
    "value": "order:*"
  }
}
```

其中，属性名为端点路径（即 URI 里面的 path 部分），其值为权限点数据，分三种场景

1. 只需要认证，不需要角色或者资源信息。这种情况只需要将权限点数据留空即可。

```json
{
  "/get": { }
}
```

2. 需要某种角色。

```json
{
  "/get": {
    "type": "role",
    "value": "dev"
  }
}
```

> 如果端点要求用户具有某种角色之一，则用空格将角色隔开，如：

```json
{
  "/get": {
    "type": "role",
    "value": "dev mananger admin"
  }
}
```

上面例子中，只要用户拥有 dev、manager、admin 三种角色中的一种，过滤器就会放行。

3. 需要资源权限。

```json
{
  "/post": {
    "type": "res",
    "value": "order:*"
  }
}
```

type 为 res，value 的格式为 {资源名}:{action}，需要和 authing 控制台创建的资源名字匹配

## 鉴权成功

若鉴权成功，过滤器会在请求头里面插入

* x-user-id
* x-user-name

下游系统根据需要完成业务操作。

## 鉴权失败

鉴权失败场景：

* 用户认证失败。返回 http status code 401 unauthorized
* 角色或者资源鉴权失败。返回 http status code 403 forbidden

## 测试

1. 在 https://developer-beta.authing.cn/ams/auth-tool/index.html 填入自己应用相关信息，点击 Login，拷贝 ID Token
2. 访问业务服务时，在 Postman 里面配置 http header，带上 x-authing-app-id 和 x-authing-userpool-id。同时将步骤一获取的 ID Token 以 Authorization Bearer 的方式传入

## 私有化部署

在标注了 @SpringBootApplication 的 Application 类里面调用：

```java
// mycompany.com is your on premise host
Authing.setHost("mycompany.com");
```