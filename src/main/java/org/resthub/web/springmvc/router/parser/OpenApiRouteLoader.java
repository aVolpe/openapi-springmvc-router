package org.resthub.web.springmvc.router.parser;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.resthub.web.springmvc.router.HTTPRequestAdapter;
import org.resthub.web.springmvc.router.Router.Route;
import org.resthub.web.springmvc.router.exceptions.RouteFileParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

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

        try (InputStream ignored = data.getInputStream()) {
            ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolveFully(true);
            OpenAPI loaded = new OpenAPIV3Parser().read(
                    data.getURL().toString(),
                    null,
                    parseOptions
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
        route.accepts = getAcceptContentTypes(route, op, components);
        route.contentType = getContentType(route, op, components);
        route.staticArgs = getParams(op, components);
        route.compute();
        if (logger.isDebugEnabled()) {
            logger.debug("Adding [{}] with params [{}] and headers [{}]", route, route.accepts, route.staticArgs);
        }

        return route;
    }

    private Map<String, String> getParams(Operation op, Components components) {
        // we don't support static args
        return Collections.emptyMap();
    }

    private List<MediaType> getAcceptContentTypes(Route route, Operation op, Components components) {
        // TODO check how to get the accepts (it should iterate all responses?)
        return List.of(MediaType.ALL);
    }

    private List<MediaType> getContentType(Route route, Operation op, Components components) {
        if (op.getRequestBody() == null) return Collections.emptyList();
        if (op.getRequestBody().get$ref() != null) {
            var schemeRef = op.getRequestBody().get$ref();
            if (schemeRef.startsWith("#/components/requestBodies"))
                for (var schemeDef : components.getRequestBodies().entrySet()) {
                    if (schemeRef.endsWith("/" + schemeDef.getKey()))
                        return schemeDef.getValue().getContent().keySet().stream().map(HTTPRequestAdapter::resolveFormat).filter(Objects::nonNull).toList();
                }
            throw new OpenApiBuilderException(route, "Has ref, but the ref wasn't found in request bodies %s".formatted(schemeRef));
        }
        return op.getRequestBody().getContent().keySet().stream().map(HTTPRequestAdapter::resolveFormat).filter(Objects::nonNull).toList();
    }

    public static class OpenApiBuilderException extends RuntimeException {
        public OpenApiBuilderException(Route route, String description) {
            this(route, description, null);
        }

        public OpenApiBuilderException(Route route, String description, Throwable nested) {
            super(String.format("%s - building route: %s %s", description, route.getMethod(), route.getPath()), nested);
        }

        public OpenApiBuilderException(String detail) {
            super(detail);
        }

        public OpenApiBuilderException(String detail, Throwable nested) {
            super(detail, nested);
        }

    }
}
