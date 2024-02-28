package org.resthub.web.springmvc.router.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Arturo Volpe
 * @since 2024-02-28
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RouterConfiguration.class)
public @interface EnableOpenApiRouter {

    /**
     * Path to the open api file.
     * <p>
     * Use classpath: to load from resources
     */
    String[] config();

    /**
     * Set to null to disable
     */
    String apiDocsPath() default "/v3/api-docs";
}
