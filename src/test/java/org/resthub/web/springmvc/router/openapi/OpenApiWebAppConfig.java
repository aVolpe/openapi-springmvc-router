package org.resthub.web.springmvc.router.openapi;

import org.resthub.web.springmvc.router.config.EnableOpenApiRouter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arturo Volpe
 * @since 2022-11-10
 */
@Configuration
@ComponentScan(basePackages = "org.resthub.web.springmvc.router.controllers")
@EnableOpenApiRouter(config = "classpath:petstore.yaml")
public class OpenApiWebAppConfig {

}
