package org.resthub.web.springmvc.router;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

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
public abstract class RouterConfigurationSupport extends DelegatingWebMvcConfiguration {

    /**
     * Return a {@link RouterHandlerMapping} ordered at 0 for mapping
     * requests to controllers' actions mapped by routes.
     */
    @Bean
    public RouterHandlerMapping createRouterHandlerMapping(@Qualifier("mvcConversionService") FormattingConversionService conversionService,
                                                           @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

        RouterHandlerMapping handlerMapping = new RouterHandlerMapping();
        handlerMapping.setRouteFiles(listRouteFiles());
        handlerMapping.setAutoReloadEnabled(isHandlerMappingReloadEnabled());
        handlerMapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
        handlerMapping.setOrder(0);
        return handlerMapping;
    }

    /**
     * By default, route configuration files auto-reload is not enabled.
     * You can override this method to enable this feature.
     *
     * @see RouterHandlerMapping
     */
    protected boolean isHandlerMappingReloadEnabled() {
        return false;
    }

    /**
     * Return a {@link RequestMappingHandlerMapping} ordered at 3 for mapping
     * requests to annotated controllers.
     */
    @Bean
    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping(@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
                                                                     @Qualifier("mvcConversionService") FormattingConversionService conversionService,
                                                                     @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        handlerMapping.setOrder(3);
        handlerMapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
        handlerMapping.setContentNegotiationManager(mvcContentNegotiationManager());
        return handlerMapping;
    }

    /**
     * Return the ordered list of route configuration files to be loaded
     * by the Router at startup.
     */
    public abstract List<String> listRouteFiles();

}
