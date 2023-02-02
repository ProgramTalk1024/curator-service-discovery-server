package cn.programtalk.server.config;

import cn.programtalk.server.api.MyResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        register(MyResource.class);
    }
}
