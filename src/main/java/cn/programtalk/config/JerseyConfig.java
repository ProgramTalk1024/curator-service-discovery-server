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
