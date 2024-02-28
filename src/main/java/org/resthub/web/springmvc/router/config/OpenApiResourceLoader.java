package org.resthub.web.springmvc.router.config;

import org.resthub.web.springmvc.router.exceptions.RouteFileParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OpenApiResourceLoader {

    private final List<Resource> routes;

    public OpenApiResourceLoader(String routerFiles, ApplicationContext context) {
        if (routerFiles == null) {
            this.routes = List.of();
            return;
        }

        try {
            List<Resource> fileResources = new ArrayList<>();
            for (String fileName : routerFiles.split(",")) {
                fileResources.addAll(Arrays.asList(context.getResources(fileName)));
            }
            this.routes = Collections.unmodifiableList(fileResources);
        } catch (IOException e) {
            throw new RouteFileParsingException("Could not read route configuration files", e);
        }
    }

    public List<Resource> getRoutes() {
        return routes;
    }
}
