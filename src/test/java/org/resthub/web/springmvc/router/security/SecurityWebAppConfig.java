package org.resthub.web.springmvc.router.security;

import org.resthub.web.springmvc.router.config.OpenApiResourceLoader;
import org.resthub.web.springmvc.router.config.RouterConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Java config replacement for the former {@code securityContext.xml}, which relied on the
 * {@code <global-method-security>} XML namespace removed in Spring Security 7.
 * <p>
 * Checks that routed actions still go through method security interceptors: the router resolves
 * the controller bean, and the handler adapter must invoke it through the security proxy.
 */
@Configuration
@ComponentScan(basePackages = "org.resthub.web.springmvc.router.controllers")
@EnableMethodSecurity(securedEnabled = true)
public class SecurityWebAppConfig extends RouterConfiguration {

    @Override
    public OpenApiResourceLoader openApiRouterFiles(@Value("${test:test}") String routerFiles, ApplicationContext context) {

        return new OpenApiResourceLoader("classpath:bindingroutes.conf", context);
    }

}
