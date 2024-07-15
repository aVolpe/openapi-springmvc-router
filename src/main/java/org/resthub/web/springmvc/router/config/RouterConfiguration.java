package org.resthub.web.springmvc.router.config;

import org.resthub.web.springmvc.router.Router;
import org.resthub.web.springmvc.router.RouterHandlerMapping;
import org.resthub.web.springmvc.router.parser.ByLineRouterLoader;
import org.resthub.web.springmvc.router.support.OpenApiSpecController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.IOException;
import java.util.List;


/**
 * This class provides MVC Java config support for the {@link RouterHandlerMapping},
 * on top of the existing {@link WebMvcConfigurationSupport}.
 * <p>
 * Unlike {@link WebMvcConfigurationSupport}, you SHOULD NOT import it using
 * {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc} within an application
 * {@link org.springframework.context.annotation.Configuration @Configuration} class.
 * <p>
 * Extending this class and adding {@link org.springframework.context.annotation.Configuration @Configuration}
 * is enough. You should also implement the configureRouteFiles method to add the list of route
 * configuration files.
 * <p>
 * You can then instantiate your own beans and override {@link WebMvcConfigurationSupport} methods.
 *
 * <p>This class registers the following {@link org.springframework.web.servlet.HandlerMapping}s:</p>
 * <ul>
 *  <li>{@link RouterHandlerMapping}
 *  ordered at 0 for mapping requests to annotated controller methods.
 * 	<li>{@link org.springframework.web.servlet.HandlerMapping}
 * 	ordered at 1 to map URL paths directly to view names.
 * 	<li>{@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * 	ordered at 2 to map URL paths to controller bean names.
 * 	<li>{@link RequestMappingHandlerMapping}
 * 	ordered at 3 for mapping requests to annotated controller methods.
 * 	<li>{@link org.springframework.web.servlet.HandlerMapping}
 * 	ordered at {@code Integer.MAX_VALUE-1} to serve static resource requests.
 * 	<li>{@link org.springframework.web.servlet.HandlerMapping}
 * 	ordered at {@code Integer.MAX_VALUE} to forward requests to the default servlet.
 * </ul>
 *
 * @author Brian Clozel
 * @see WebMvcConfigurationSupport
 */
@Configuration
public class RouterConfiguration extends DelegatingWebMvcConfiguration implements ImportAware {

    private String annotationRoutes = null;
    private String apiDocsPath = null;

    /**
     * Return a {@link RouterHandlerMapping} ordered at 0 for mapping
     * requests to controllers' actions mapped by routes.
     */
    @Bean
    public RouterHandlerMapping openApiRouterHandlerMapping(
            Router router,
            @Qualifier("mvcConversionService") FormattingConversionService conversionService,
            @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

        RouterHandlerMapping handlerMapping = new RouterHandlerMapping(router);
        handlerMapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
        handlerMapping.setOrder(-2);
        return handlerMapping;
    }

    @Bean
    public Router openApiRouter(
            OpenApiResourceLoader routes
    ) throws IOException {
        return new Router(routes);
    }

    @Bean
    public OpenApiResourceLoader openApiRouterFiles(
            @Value("${openapi.router.routeFiles:}") String routerFiles,
            ApplicationContext applicationContext
    ) {
        var files = StringUtils.hasText(routerFiles) ? routerFiles : annotationRoutes;
        if (files == null)
            throw new IllegalArgumentException("Specify either property openapi.router.routeFiles or @EnableOpenApiRouter.config");
        return new OpenApiResourceLoader(files, applicationContext);
    }

    @Bean
    public Object apiDocsRoute(
            @Value("${openapi.router.specRoute:}") String specRoute,
            Router router,
            RouterHandlerMapping mapping,
            OpenApiResourceLoader routes) {
        var finalRoute = getApiDocsPath(specRoute);
        if (StringUtils.hasText(finalRoute)) {
            router.addRoutes(
                    ByLineRouterLoader.getRoute("GET", finalRoute, "apiDocsRoute.get", null, null, "RouterConfiguration", 1),
                    ByLineRouterLoader.getRoute("GET", "%s/{spec}".formatted(finalRoute), "apiDocsRoute.get", null, null, "RouterConfiguration", 1)
            );

            var toRet = new OpenApiSpecController(routes);
            mapping.addControllerToCache("apiDocsRoute", toRet);
            return toRet;
        }
        return null;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        var openApi = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableOpenApiRouter.class.getName()));
        if (openApi != null) {
            this.annotationRoutes = String.join(",", List.of(openApi.getStringArray("config")));
            this.apiDocsPath = openApi.getString("apiDocsPath");
        }
    }

    private String getApiDocsPath(String property) {
        if (StringUtils.hasText(property)) return property;
        return this.apiDocsPath;
    }

}
