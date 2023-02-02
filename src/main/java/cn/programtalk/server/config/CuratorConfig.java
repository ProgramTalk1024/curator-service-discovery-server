package cn.programtalk.server.config;

import cn.programtalk.server.pojo.InstanceMetadata;
import javax.ws.rs.ext.ContextResolver;
import lombok.SneakyThrows;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.server.contexts.GenericDiscoveryContext;
import org.apache.curator.x.discovery.server.rest.DiscoveryContext;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CuratorConfig {
    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("172.29.240.53:2181", new ExponentialBackoffRetry(1000, 3));
        curatorFramework.start();
        return curatorFramework;
    }

    @SneakyThrows
    @Bean
    ServiceDiscovery<InstanceMetadata> serviceDiscovery() {
        return ServiceDiscoveryBuilder.builder(InstanceMetadata.class)
                // zk路径
                .basePath("/base-path")
                // zk客户端
                .client(curatorFramework())
                // 是否监听实例
                .watchInstances(true)
                // 序列化
                .serializer(new JsonInstanceSerializer<>(InstanceMetadata.class))
                .build();
    }


    @Bean
    public DiscoveryContext<InstanceMetadata> discoveryContext() {
        return new GenericDiscoveryContext<>(serviceDiscovery(), new RoundRobinStrategy<>(), 30, InstanceMetadata.class);
    }

    @Bean
    public ContextResolver<DiscoveryContext<InstanceMetadata>> resolver() {
        return aClass -> discoveryContext();
    }
}
