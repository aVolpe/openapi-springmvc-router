package org.resthub.web.springmvc.router.config;

import org.resthub.web.springmvc.router.Router;
import org.resthub.web.springmvc.router.RouterHandlerMapping;
import org.resthub.web.springmvc.router.parser.ByLineRouterLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
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
public class RouterConfiguration extends DelegatingWebMvcConfiguration {

    /**
     * Return a {@link RouterHandlerMapping} ordered at 0 for mapping
     * requests to controllers' actions mapped by routes.
     */
    @Bean
    public RouterHandlerMapping openApiRouterHandlerMapping(
            @Value("${openapi.router.specRoute:/v3/api-docs}") String specRoute,
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
            @Value("${openapi.router.routeFiles:openapi.yml}") String routerFiles,
            ApplicationContext applicationContext
    ) {
        return new OpenApiResourceLoader(routerFiles, applicationContext);
    }

    @Bean
    public Object apiDocsRoute(
            @Value("${openapi.router.specRoute:/v3/api-docs}") String specRoute,
            Router router,
            RouterHandlerMapping mapping,
            OpenApiResourceLoader routes) {
        if (StringUtils.hasText(specRoute)) {
            router.addRoute(
                    ByLineRouterLoader.getRoute("GET", specRoute, "apiDocsRoute.get", null, null, "RouterConfiguration", 1)
            );

            var toRet = new OpenApiSpecController(routes);
            mapping.addControllerToCache("apiDocsRoute", toRet);
            return toRet;
        }
        return null;
    }

    private static class OpenApiSpecController {

        private final List<Resource> routes;

        public OpenApiSpecController(OpenApiResourceLoader routes) {
            this.routes = routes.getRoutes();
        }

        public Resource get() {
            if (this.routes.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "openApiSpec not found");
            return this.routes.get(0);
        }
    }
}
