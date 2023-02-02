>  Spring boot整合service-discovery-server



# 版本说明
Spring boot: 2.7.8

Curator: 5.0.4

JDK: 17

# 特别说明
Spring boot版本不能使用`3.x`以上,我使用`3.0.2`测试,不通过,目前还没有研究具体原因,暂时使用`2.7.8`!
* `Curator Service Discovery Server` 的依赖 `curator-x-discovery-server` 是基于JAX-RS实现的,所以`Spring boot`的web依赖要使用`pring-boot-starter-jersey`.
* `curator-x-discovery-server`已经提供了Restful api的代码, 我们无需自己写,只需要配置即可.



# 项目说明
## Maven依赖
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.8</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>cn.programtalk</groupId>
    <artifactId>curator-service-discovery-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>curator-service-discovery-server</name>
    <description>curator-service-discovery-server</description>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jersey</artifactId>
        </dependency>

        <!-- curator相关依赖 -->
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-recipes</artifactId>
            <version>5.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-x-discovery-server</artifactId>
            <version>5.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-test</artifactId>
            <version>5.4.0</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```
## 服务发现服务器配置
需要初始化几个Bean.

```java
package cn.programtalk.config;

import lombok.SneakyThrows;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.server.contexts.StringDiscoveryContext;
import org.apache.curator.x.discovery.server.rest.DiscoveryContext;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ext.ContextResolver;

/**
 * Curator配置类
 * @author programtalk.cn
 */
@Configuration
public class CuratorConfig {

    // zk服务器连接地址,172.29.240.53:2181,172.29.240.53:2182
    @Value("${cn.programtalk.zk-servers}")
    public String zkServers;

    // 服务发现在Zk中的根路径
    @Value("${cn.programtalk.base-path}")
    public String basePath;

    /**
     * CuratorFramework客户端
     * @return
     */
    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(zkServers, new ExponentialBackoffRetry(1000, 3));
        curatorFramework.start();
        return curatorFramework;
    }

    /**
     * ServiceDiscovery实例
     * @return
     */
    @SneakyThrows
    @Bean
    ServiceDiscovery<String> serviceDiscovery() {
        return ServiceDiscoveryBuilder.builder(String.class)
                // zk路径
                .basePath(basePath)
                // zk客户端
                .client(curatorFramework())
                // 是否监听实例
                .watchInstances(true)
                // 序列化
                .serializer(new JsonInstanceSerializer<>(String.class))
                .build();
    }

    /**
     *
     * @return
     */
    @Bean
    public DiscoveryContext<String> discoveryContext() {
        return new StringDiscoveryContext(serviceDiscovery(), new RoundRobinStrategy<>(), 30);
    }

    @Bean
    public ContextResolver<DiscoveryContext<String>> resolver() {
        return aClass -> discoveryContext();
    }
}
```
上面这个类,主要配置了`CuratorFramework客户端(用于连接Zookeeper)`,`ServiceDiscovery(服务发现配置)`,`DiscoveryContext(服务发现上下文)`等.

## API配置
curator已经实现了Restful Api,他是个抽象类,名字是`DiscoveryResource`,使用者需要自己创建类并继承`DiscoveryResource`.
```java
package cn.programtalk.api;

import org.apache.curator.x.discovery.server.rest.DiscoveryContext;
import org.apache.curator.x.discovery.server.rest.DiscoveryResource;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;

/**
 * 自定义MyResource,继承DiscoveryResource,DiscoveryResource是Curator提供的服务发现服务器的Restful Api.
 */
@Path("/")
@Component
public class MyResource extends DiscoveryResource<String> {
    public MyResource(@Context ContextResolver<DiscoveryContext<String>> resolver) {
        super(resolver.getContext(DiscoveryContext.class));
    }
}
```



## 注册API类

上面创建的`MyResource`类需要注册到`Jersey`中.
```java
package cn.programtalk.config;

import cn.programtalk.api.MyResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

/**
 * Jersey配置类
 *
 * @author programtalk.cn
 */
@Configuration
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        // 将自定义的Api类注册
        register(MyResource.class);
    }
}
```
## application.yaml
```yaml
cn:
  programtalk:
    # zookeeper服务信息
    zk-servers: 172.29.240.53:2181
    # 服务发现的根路径(zookeeper中)
    base-path: '/service-discovery'
```



# API测试

api请求数据结构请看官方说明：https://git-wip-us.apache.org/repos/asf?p=curator.git;a=blob_plain;f=curator-x-discovery-server/README.txt;hb=HEAD



## putService

注册或者修改服务

![putService](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021923115.png)



请求数据如下：

```http
PUT /v1/service/product-service/product-instance-id2 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 224

{
    "name": "product-service",
    "id": "product-instance-id2",
    "address": "10.20.30.40",
    "port": 1234,
    "payload": "产品服务payload",
    "registrationTimeUTC": 1325129459728,
    "serviceType": "STATIC"
}
```



看下Zk里的节点的信息

![image-20230202192433063](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021924143.png)



可以看到`product-service`服务里有一个实例`product-instance-id1`

我再添加几个实例(修改id即可)。

![image-20230202192702930](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021927008.png)

总共添加了三个实例。

用同样的方法再添加另一个订单服务（`order-service`）

```http
PUT /v1/service/order-service/order-instance-id1 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 220

{
    "name": "order-service",
    "id": "order-instance-id1",
    "address": "10.20.30.40",
    "port": 1234,
    "payload": "订单服务payload",
    "registrationTimeUTC": 1325129459728,
    "serviceType": "STATIC"
}
```

再查看下zk节点情况

![image-20230202192925593](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021929676.png)

也创建了三个实例。

## removeService

删除服务下的实例



测试删除`order-service`的`order-instance-id1`实例

![removeService](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021931868.png)



```http
DELETE /v1/service/order-service/order-instance-id1 HTTP/1.1
Host: localhost:8080
```

查看订单服务信息

![image-20230202193153385](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021931462.png)

确实已经被删除掉。

## get

获取服务下的单个实例



![get](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021932466.png)



```http
GET /v1/service/order-service/order-instance-id2 HTTP/1.1
Host: localhost:8080
```



## getAllNames

查询所有服务名



![getAllNames](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021933646.png)



```http
GET /v1/service HTTP/1.1
Host: localhost:8080
```



## getAll

查询服务下的所有实例

![getAll](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021934190.png)



```http
GET /v1/service/order-service HTTP/1.1
Host: localhost:8080
```



## getAny

随机获取某个服务下的某个实例



![getAny](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202302021935455.png)



```http
GET /v1/anyservice/order-service HTTP/1.1
Host: localhost:8080
```



# 项目地址

github：https://github.com/ProgramTalk1024/curator-service-discovery-server.githttps://itlab1024.com/archives/208.html)



# 运行说明

如果想运行该项目，首先去application.yaml中修改zk的地址。
