package org.resthub.web.springmvc.router.openapi;

import org.resthub.web.springmvc.router.RouterConfigurationSupport;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Arturo Volpe
 * @since 2022-11-10
 */
@Configuration
@ComponentScan(basePackages = "org.resthub.web.springmvc.router.controllers")
public class OpenApiWebAppConfig extends RouterConfigurationSupport {

    @Override
    public List<String> listRouteFiles() {

        List<String> routeFiles = new ArrayList<>();
        routeFiles.add("classpath:petstore.yaml");

        return routeFiles;
    }
}
