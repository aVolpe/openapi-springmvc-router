package org.resthub.web.springmvc.router.parser;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import org.resthub.web.springmvc.router.HTTPRequestAdapter;
import org.resthub.web.springmvc.router.Route;
import org.resthub.web.springmvc.router.exceptions.RouteFileParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses an OpenApi file and produce the routes
 */
public class OpenApiRouteLoader {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiRouteLoader.class);

    public List<Route> load(Resource data) {

        try (InputStream fis = data.getInputStream()) {
            OpenAPI loaded = Yaml.mapper().readValue(
                    fis,
                    OpenAPI.class
            );
            List<Route> toRet = new ArrayList<>(loaded.getPaths().size() * 2);

            Paths paths = loaded.getPaths();
            paths.forEach((path, definition) -> {
                toRet.addAll(mapToRoute(path, definition, loaded.getComponents(), data.getDescription()));
            });

            return toRet;
        } catch (FileNotFoundException e) {
            throw new RouteFileParsingException(String.format("The specified def '%s' was not found ", data.getDescription()));
        } catch (IOException e) {
            throw new RouteFileParsingException(String.format("The specified def '%s' doesn't contain a valid definition ", data.getDescription()));
        }
    }

    private List<Route> mapToRoute(String path, PathItem definition, Components components, String resourceDesc) {

        return definition.readOperationsMap()
                .entrySet()
                .stream()
                .map(entry -> getRoute(
                        path,
                        entry.getKey(), entry.getValue(), components, resourceDesc))
                .collect(Collectors.toList());

    }

    private Route getRoute(String path, HttpMethod method, Operation op, Components components, String resourceDesc) {

        Route route = new Route();
        route.method = method.toString();
        route.path = path.replace("//", "/");
        route.action = Optional.ofNullable(op.getExtensions()).map(e -> e.get("x-action")).map(Objects::toString).orElse(op.getOperationId());
        route.routesFile = resourceDesc;
        route.routesFileLine = -1;
        route.formats = getAcceptContentTypes(op);
        route.staticArgs = getParams(op, components);
        route.operation = op;
        route.compute();
        if (logger.isDebugEnabled()) {
            logger.debug("Adding [{}] with params [{}] and headers [{}]", route, route.formats, route.staticArgs);
        }

        return route;
    }

    private Map<String, String> getParams(Operation op, Components components) {
        // we don't support static args
        return Collections.emptyMap();
    }

    private List<String> getAcceptContentTypes(Operation op) {
        if (op.getRequestBody() == null) return Collections.emptyList();
        return op.getRequestBody().getContent().keySet().stream().map(HTTPRequestAdapter::resolveFormat).filter(Objects::nonNull).toList();
    }
}
