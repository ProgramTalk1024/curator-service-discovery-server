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