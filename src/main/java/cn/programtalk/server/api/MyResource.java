package cn.programtalk.server.api;

import cn.programtalk.server.pojo.InstanceMetadata;
import org.apache.curator.x.discovery.server.rest.DiscoveryContext;
import org.apache.curator.x.discovery.server.rest.DiscoveryResource;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;

@Path("/")
@Component
public class MyResource extends DiscoveryResource<InstanceMetadata> {
    public MyResource(@Context ContextResolver<DiscoveryContext<InstanceMetadata>> resolver) {
        // note: this may not work with all JAX-RS implementations
        super(resolver.getContext(DiscoveryContext.class));
    }
}