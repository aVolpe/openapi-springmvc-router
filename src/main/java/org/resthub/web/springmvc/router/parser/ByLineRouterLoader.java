package org.resthub.web.springmvc.router.parser;

import jregex.Matcher;
import jregex.Pattern;
import org.resthub.web.springmvc.router.HTTPRequestAdapter;
import org.resthub.web.springmvc.router.Router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.*;

/**
 * Parses the resource line by line
 */
public class ByLineRouterLoader {

    private static final Logger logger = LoggerFactory.getLogger(ByLineRouterLoader.class);
    static Pattern routePattern = new Pattern("^({method}GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|\\*)[(]?({headers}[^)]*)(\\))?\\s+({path}.*/[^\\s]*)\\s+({action}[^\\s(]+)({params}.+)?(\\s*)$");
    static Pattern paramPattern = new Pattern("\\s*([a-zA-Z_0-9]+)\\s*:\\s*'(.*)'\\s*");

    public List<Route> load(Resource resource) throws IOException {

        String fileAbsolutePath = resource.getURL().getPath();

        return parse(convertStreamToString(resource.getInputStream()), fileAbsolutePath);
    }

    List<Route> parse(String content, String fileAbsolutePath) {
        int lineNumber = 0;
        List<Route> toRet = new ArrayList<>();
        for (String line : content.split("\n")) {
            lineNumber++;
            line = line.trim().replaceAll("\\s+", " ");
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = routePattern.matcher(line);
            if (matcher.matches()) {

                String action = matcher.group("action");
                String method = matcher.group("method");
                String path = matcher.group("path");
                String params = matcher.group("params");
                String headers = matcher.group("headers");
                toRet.add(getRoute(method, path, action, params, headers, fileAbsolutePath, lineNumber));
            } else {
                logger.error("Invalid route definition in {}: {}", lineNumber, line);
            }
        }
        return toRet;
    }

    public Route getRoute(String method, String path, String action, String params, String headers) {
        return getRoute(method, path, action, params, headers, null, 0);
    }

    public Route getRoute(String method, String path, String action, String params, String headers, String sourceFile, int line) {
        Route route = new Route();
        route.method = method;
        route.path = path.replace("//", "/");
        route.action = action;
        route.routesFile = sourceFile;
        route.routesFileLine = line;
        route.formats = getAcceptContentTypes(headers);
        route.staticArgs = getParms(params);
        route.compute();
        if (logger.isDebugEnabled()) {
            logger.debug("Adding [{}] with params [{}] and headers [{}]", route, params, headers);
        }

        return route;
    }

    public List<MediaType> getAcceptContentTypes(String params) {
        if (params == null || params.length() < 1) {
            return Collections.emptyList();
        }
        params = params.trim();
        return Arrays.stream(params.split(",")).map(HTTPRequestAdapter::resolveFormat).toList();
    }

    public Map<String, String> getParms(String params) {
        if (params == null || params.length() < 1) {
            return Collections.emptyMap();
        }
        Map<String, String> toRet = new HashMap<>();
        params = params.substring(1, params.length() - 1);
        for (String param : params.split(",")) {
            Matcher matcher = paramPattern.matcher(param);
            if (matcher.matches()) {
                toRet.put(matcher.group(1), matcher.group(2));
            } else {
                logger.warn("Ignoring {} (static params must be specified as key:'value',...)", param);
            }
        }
        return toRet;
    }

    public Route buildRoute(String method, String path, String action) {
        return getRoute(method, path, action, null, null);
    }

    public Route buildRoute(String method, String path, String action, String params, String headers) {
        return getRoute(method, path, action, params, headers);
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
