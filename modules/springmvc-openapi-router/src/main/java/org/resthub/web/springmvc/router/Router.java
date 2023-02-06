package org.resthub.web.springmvc.router;

import jregex.Matcher;
import jregex.Pattern;
import org.apache.commons.io.FileUtils;
import org.resthub.web.springmvc.router.exceptions.NoHandlerFoundException;
import org.resthub.web.springmvc.router.exceptions.NoRouteFoundException;
import org.resthub.web.springmvc.router.exceptions.RouteFileParsingException;
import org.resthub.web.springmvc.router.parser.ByLineRouterLoader;
import org.resthub.web.springmvc.router.parser.OpenApiRouteLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>The router matches HTTP requests to action invocations.
 * <p>Courtesy of Play! Framework Router
 *
 * @author Play! Framework developers
 * @author Brian Clozel
 * @see org.resthub.web.springmvc.router.RouterHandlerMapping
 */
public class Router {

    /**
     * Pattern used to locate a method override instruction
     */
    static Pattern methodOverride = new Pattern("^.*x-http-method-override=({method}GET|PUT|POST|DELETE|PATCH).*$");

    /**
     * Timestamp the routes file was last loaded at.
     */
    private static long lastLoading = -1;
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    public static void clear() {
        routes.clear();
    }

    /**
     * Parse the routes file. This is called at startup.
     */
    public static void load(List<Resource> fileResources) throws IOException {
        routes.clear();
        for (Resource res : fileResources) {
            routes.addAll(parse(res));
        }

        lastLoading = System.currentTimeMillis();

        logger.info("Loaded routes: \n\t{}", routes.stream().map(Route::toFixedLengthString).collect(Collectors.joining("\n\t")));
    }


    /**
     * Add a route at the given position
     */
    public static void addRoute(int position, Route route) {
        if (position > routes.size()) {
            position = routes.size();
        }
        routes.add(position, route);
    }

    /**
     * Add a route
     */
    public static void addRoute(Route route) {
        routes.add(route);
    }

    /**
     * This is used internally when reading the route file. The order the routes
     * are added matters and we want the method to append the routes to the
     * list.
     */
    public static void appendRoute(Route route) {
        routes.add(route);
    }


    /**
     * Add a new route at the beginning of the route list
     */
    public static void prependRoute(Route route) {
        routes.add(0, route);
    }

    /**
     * Parse a route file.
     *
     * @param fileResource the file to read
     * @return all found routes
     */
    static List<Route> parse(Resource fileResource) throws IOException {

        String fileAbsolutePath = fileResource.getURL().getPath();

        List<String> openApiExtensions = Arrays.asList("yml", "yaml", "json");

        for (String ext : openApiExtensions) {
            if (fileAbsolutePath.endsWith(ext)) return new OpenApiRouteLoader().load(fileResource);
        }

        return new ByLineRouterLoader().load(fileResource);
    }


    public static void detectChanges(List<Resource> fileResources) throws IOException {

        boolean hasChanged = false;

        for (Resource res : fileResources) {
            if (FileUtils.isFileNewer(res.getFile(), lastLoading)) {
                hasChanged = true;
                break;
            }
        }

        if (hasChanged) {
            load(fileResources);
        }
    }

    public static List<Route> routes = new ArrayList<>(500);

    public static Route route(HTTPRequestAdapter request) {
        if (logger.isTraceEnabled()) {
            logger.trace("Route: {} - {}", request.path, request.querystring);
        }
        // request method may be overridden if a x-http-method-override parameter is given
        if (request.querystring != null && methodOverride.matches(request.querystring)) {
            Matcher matcher = methodOverride.matcher(request.querystring);
            if (matcher.matches()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("request method {} overridden to {} ", request.method, matcher.group("method"));
                }
                request.method = matcher.group("method");
            }
        }

        for (Route route : routes) {
            String format = request.format;
            String host = request.host;
            String path = request.contextPath != null ? request.path.replace(request.contextPath, "") : request.path;
            Map<String, String> args = route.matches(request.method, path, format, host);

            if (args != null) {
                request.routeArgs = args;
                request.action = route.action;
                if (args.containsKey("format")) {
                    request.setFormat(args.get("format"));
                }
                if (request.action.contains("{")) {
                    for (String arg : request.routeArgs.keySet()) {
                        request.action = request.action.replace("{" + arg + "}", request.routeArgs.get(arg));
                    }
                }
                return route;
            }
        }
        // Not found - if the request was a HEAD, let's see if we can find a corresponding GET
        if (request.method.equalsIgnoreCase("head")) {
            request.method = "GET";
            Route route = route(request);
            request.method = "HEAD";
            return route;
        }
        throw new NoRouteFoundException(request.method, request.path);
    }

    public static Map<String, String> route(String method, String path) {
        return route(method, path, null, null);
    }

    public static Map<String, String> route(String method, String path, String headers) {
        return route(method, path, headers, null);
    }

    public static Map<String, String> route(String method, String path, String headers, String host) {
        for (Route route : routes) {
            Map<String, String> args = route.matches(method, path, headers, host);
            if (args != null) {
                args.put("action", route.action);
                return args;
            }
        }
        return new HashMap<>(16);
    }

    public static ActionDefinition reverse(String action) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return reverse(action, new HashMap<>(16));
    }

    public static String getFullUrl(String action, Map<String, Object> args) {
        return HTTPRequestAdapter.getCurrent().getBase() + reverse(action, args);
    }

    public static String getFullUrl(String action) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return getFullUrl(action, new HashMap<>(16));
    }

    public static Collection<Route> resolveActions(String action) {

        List<Route> candidateRoutes = new ArrayList<Route>(3);

        for (Route route : routes) {
            if (route.actionPattern != null) {
                Matcher matcher = route.actionPattern.matcher(action);
                if (matcher.matches()) {
                    candidateRoutes.add(route);
                }
            }
        }

        return candidateRoutes;
    }

    public static ActionDefinition reverse(String action, Map<String, Object> args) {

        HTTPRequestAdapter currentRequest = HTTPRequestAdapter.getCurrent();

        Map<String, Object> argsbackup = new HashMap<>(args);
        for (Route route : routes) {
            if (route.actionPattern != null) {
                Matcher matcher = route.actionPattern.matcher(action);
                if (matcher.matches()) {
                    for (String group : route.actionArgs) {
                        String v = matcher.group(group);
                        if (v == null) {
                            continue;
                        }
                        args.put(group, v.toLowerCase());
                    }
                    List<String> inPathArgs = new ArrayList<>(16);
                    boolean allRequiredArgsAreHere = true;
                    // les noms de parametres matchent ils ?
                    for (Route.Arg arg : route.args) {
                        inPathArgs.add(arg.name);
                        Object value = args.get(arg.name);
                        if (value == null) {
                            // This is a hack for reverting on hostname that are a regex expression.
                            // See [#344] for more into. This is not optimal and should retough. However,
                            // it allows us to do things like {(.*)}.domain.com
                            String host = route.host.replaceAll("\\{", "").replaceAll("\\}", "");
                            if (host.equals(arg.name) || host.matches(arg.name)) {
                                args.put(arg.name, "");
                                value = "";
                            } else {
                                allRequiredArgsAreHere = false;
                                break;
                            }
                        } else {
                            if (value instanceof List<?>) {
                                @SuppressWarnings("unchecked")
                                List<Object> l = (List<Object>) value;
                                value = l.get(0);
                            }
                            if (!value.toString().startsWith(":") && !arg.constraint.matches(value.toString())) {
                                allRequiredArgsAreHere = false;
                                break;
                            }
                        }
                    }
                    // les parametres codes en dur dans la route matchent-ils ?
                    for (String staticKey : route.staticArgs.keySet()) {
                        if (staticKey.equals("format")) {
                            if (!currentRequest.format.equals(route.staticArgs.get("format"))) {
                                allRequiredArgsAreHere = false;
                                break;
                            }
                            continue; // format is a special key
                        }
                        if (!args.containsKey(staticKey) || (args.get(staticKey) == null)
                                || !args.get(staticKey).toString().equals(route.staticArgs.get(staticKey))) {
                            allRequiredArgsAreHere = false;
                            break;
                        }
                    }
                    if (allRequiredArgsAreHere) {
                        StringBuilder queryString = new StringBuilder();
                        String path = route.path;
                        //add contextPath and servletPath if set in the current request
                        if (currentRequest != null) {

                            if (!currentRequest.servletPath.isEmpty() && !currentRequest.servletPath.equals("/")) {
                                String servletPath = currentRequest.servletPath;
                                path = (servletPath.startsWith("/") ? servletPath : "/" + servletPath) + path;
                            }
                            if (!currentRequest.contextPath.isEmpty() && !currentRequest.contextPath.equals("/")) {
                                String contextPath = currentRequest.contextPath;
                                path = (contextPath.startsWith("/") ? contextPath : "/" + contextPath) + path;
                            }
                        }
                        String host = route.host;
                        if (path.endsWith("/?")) {
                            path = path.substring(0, path.length() - 2);
                        }
                        for (Map.Entry<String, Object> entry : args.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (inPathArgs.contains(key) && value != null) {
                                if (List.class.isAssignableFrom(value.getClass())) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> vals = (List<Object>) value;
                                    try {
                                        path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(vals.get(0).toString().replace("$", "\\$"), "utf-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        throw new RouteFileParsingException("RouteFile encoding exception", e);
                                    }
                                } else {
                                    try {
                                        path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString().replace("$", "\\$"), "utf-8"));
                                        host = host.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString().replace("$", "\\$"), "utf-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        throw new RouteFileParsingException("RouteFile encoding exception", e);
                                    }
                                }
                            } else if (route.staticArgs.containsKey(key)) {
                                // Do nothing -> The key is static
                            } else if (value != null) {
                                if (List.class.isAssignableFrom(value.getClass())) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> vals = (List<Object>) value;
                                    for (Object object : vals) {
                                        try {
                                            queryString.append(URLEncoder.encode(key, "utf-8"));
                                            queryString.append("=");
                                            if (object.toString().startsWith(":")) {
                                                queryString.append(object.toString());
                                            } else {
                                                queryString.append(URLEncoder.encode(object.toString() + "", "utf-8"));
                                            }
                                            queryString.append("&");
                                        } catch (UnsupportedEncodingException ex) {
                                        }
                                    }
//                                } else if (value.getClass().equals(Default.class)) {
//                                    // Skip defaults in queryString
                                } else {
                                    try {
                                        queryString.append(URLEncoder.encode(key, "utf-8"));
                                        queryString.append("=");
                                        if (value.toString().startsWith(":")) {
                                            queryString.append(value.toString());
                                        } else {
                                            queryString.append(URLEncoder.encode(value.toString() + "", "utf-8"));
                                        }
                                        queryString.append("&");
                                    } catch (UnsupportedEncodingException ex) {
                                    }
                                }
                            }
                        }
                        String qs = queryString.toString();
                        if (qs.endsWith("&")) {
                            qs = qs.substring(0, qs.length() - 1);
                        }
                        ActionDefinition actionDefinition = new ActionDefinition();
                        actionDefinition.url = qs.length() == 0 ? path : path + "?" + qs;
                        actionDefinition.method = route.method == null || route.method.equals("*") ? "GET" : route.method.toUpperCase();
                        actionDefinition.star = "*".equals(route.method);
                        actionDefinition.action = action;
                        actionDefinition.args = argsbackup;
                        actionDefinition.host = host;
                        return actionDefinition;
                    }
                }
            }
        }
        throw new NoHandlerFoundException(action, args);
    }

    public static class ActionDefinition {

        /**
         * The domain/host name.
         */
        public String host;
        /**
         * The HTTP method, e.g. "GET".
         */
        public String method;
        /**
         * @todo - what is this? does it include the domain?
         */
        public String url;
        /**
         * Whether the route contains an astericks *.
         */
        public boolean star;
        /**
         * @todo - what is this? does it include the class and package?
         */
        public String action;
        /**
         * @todo - are these the required args in the routing file, or the query
         * string in a request?
         */
        public Map<String, Object> args;

        public ActionDefinition add(String key, Object value) {
            args.put(key, value);
            return reverse(action, args);
        }

        public ActionDefinition remove(String key) {
            args.remove(key);
            return reverse(action, args);
        }

        public ActionDefinition addRef(String fragment) {
            url += "#" + fragment;
            return this;
        }

        @Override
        public String toString() {
            return url;
        }

        public void absolute() {
            HTTPRequestAdapter currentRequest = HTTPRequestAdapter.getCurrent();
            if (!url.startsWith("http")) {
                if (host == null || host.isEmpty()) {
                    url = currentRequest.getBase() + url;
                } else {
                    url = (currentRequest.secure ? "https://" : "http://") + host + url;
                }
            }
        }

        public ActionDefinition secure() {
            if (!url.contains("http://") && !url.contains("https://")) {
                absolute();
            }
            url = url.replace("http:", "https:");
            return this;
        }
    }

}
