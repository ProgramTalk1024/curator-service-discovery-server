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
