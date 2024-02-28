package org.resthub.web.springmvc.router.javaconfig;

import org.resthub.web.springmvc.router.config.OpenApiResourceLoader;
import org.resthub.web.springmvc.router.config.RouterConfiguration;
import org.resthub.web.springmvc.router.support.TeapotHandlerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
@ComponentScan(basePackages = "org.resthub.web.springmvc.router.controllers")
public class WebAppConfig extends RouterConfiguration {

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new TeapotHandlerInterceptor());
    }

    @Override
    public OpenApiResourceLoader openApiRouterFiles(@Value("${test:test}") String routerFiles, ApplicationContext context) {

        return new OpenApiResourceLoader("classpath:mappingroutes.conf", context);
    }

}
