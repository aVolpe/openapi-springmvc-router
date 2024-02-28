package org.resthub.web.springmvc.router;

import jakarta.servlet.http.HttpServletRequest;
import org.resthub.web.springmvc.router.exceptions.NoRouteFoundException;
import org.resthub.web.springmvc.router.support.RouterHandlerResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * Implementation of the {@link org.springframework.web.servlet.HandlerMapping}
 * interface that maps handlers based on HTTP routes defined in a route
 * configuration file.
 *
 * <p> RouterHandlerMapping is not the default HandlerMapping registered in
 * {@link org.springframework.web.servlet.DispatcherServlet} in SpringMVC. You
 * need to declare and configure it in your DispatcherServlet context, by adding
 * a RouterHandlerMapping bean explicitly. RouterHandlerMapping needs the name
 * of the route configuration file (available in the application classpath); it
 * also allows for registering custom interceptors:
 *
 * <pre class="code"> &lt;bean
 * class="org.resthub.web.springmvc.router.RouterHandlerMapping"&gt;
 * &lt;property name="routeFiles"&gt;
 * &lt;list&gt;
 *   &lt;value&gt;bindingroutes.conf&lt;/value&gt;
 *   &lt;value&gt;addroutes.conf&lt;/value&gt;
 * &lt;/list&gt;
 * &lt;/property&gt; &lt;property
 * name="interceptors" &gt; ... &lt;/property&gt; &lt;/bean&gt;
 * </pre>
 *
 * <p> Annotated controllers should be marked with the {@link Controller}
 * stereotype at the type level. This is not strictly necessary because the
 * methodeInvoker will try to map the Controller.invoker anyway using the
 * current ApplicationContext. The {@link RequestMapping} is not taken into
 * account here.
 *
 * <p> RouterHandlerMapping loads routes configuration from a file for route
 * configuration syntax (the Router implementation is adapted from Play!
 * Framework {@link http://www.playframework.org/documentation/1.0.3/routes#syntax}).
 * <p>
 * Example:
 *
 * <pre class="code"> GET /home PageController.showPage(id:'home') GET
 * /page/{id} PageController.showPage POST /customer/{<[0-9]+>customerid}
 * CustomerController.createCustomer
 * </pre> <p> The {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter} is responsible for choosing and
 * invoking the right controller method, as mapped by this HandlerMapping.
 *
 * @author Brian Clozel
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping
 */
public class RouterHandlerMapping extends AbstractHandlerMapping {

    private static final Logger logger = LoggerFactory.getLogger(RouterHandlerMapping.class);
    private final Router router;
    private final RouterHandlerResolver methodResolver;

    public RouterHandlerMapping(Router router) {
        this.router = router;
        this.methodResolver = new RouterHandlerResolver();
    }

    /**
     * Inits Routes from route configuration file
     */
    @Override
    protected void initApplicationContext() throws BeansException {

        super.initApplicationContext();

        // Scan beans for Controllers
        this.methodResolver.setCachedControllers(getApplicationContext().getBeansWithAnnotation(Controller.class));
    }

    public void addControllerToCache(String key, Object controller) {
        this.methodResolver.addToCache(key, controller);
    }

    /**
     * Resolves a HandlerMethod (of type RouterHandler) given the current HTTP
     * request, using the Router instance.
     *
     * @param request the HTTP Servlet request
     * @return a RouterHandler, containing matching route + wrapped request
     */
    @Override
    protected Object getHandlerInternal(HttpServletRequest request)
            throws Exception {

        HandlerMethod handler;

        try {
            // Adapt HTTPServletRequest for Router
            HTTPRequestAdapter rq = HTTPRequestAdapter.parseRequest(request);
            // Route request and resolve format
            Router.Route route = router.route(rq);
            logger.debug("Looking up handler method for path {} ({} {} {})", route.path, route.method, route.path, route.action);
            handler = this.methodResolver.resolveHandler(route, rq.action, rq);
            // Add resolved route arguments to the request
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, rq.routeArgs);
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, route.pattern.toString());

        } catch (NoRouteFoundException nrfe) {
            handler = null;
            logger.trace("no route found for method[{}] and path[{}]", nrfe.method, nrfe.path);
        }

        return handler;
    }
}
