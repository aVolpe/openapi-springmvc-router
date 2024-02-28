package org.resthub.web.springmvc.router.openapi;

import org.resthub.web.springmvc.router.config.OpenApiResourceLoader;
import org.resthub.web.springmvc.router.config.RouterConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arturo Volpe
 * @since 2022-11-10
 */
@Configuration
@ComponentScan(basePackages = "org.resthub.web.springmvc.router.controllers")
public class OpenApiWebAppConfig extends RouterConfiguration {

    @Override
    public OpenApiResourceLoader openApiRouterFiles(@Value("${test:test}") String routerFiles, ApplicationContext context) {
        return new OpenApiResourceLoader("classpath:petstore.yaml", context);
    }
}
