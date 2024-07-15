package org.resthub.web.springmvc.router.support;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.resthub.web.springmvc.router.config.OpenApiResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class OpenApiSpecController {

    private final List<Resource> routes;
    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecController.class);

    public OpenApiSpecController(OpenApiResourceLoader routes) {
        this.routes = routes.getRoutes();
    }

    @ResponseBody
    public Resource get(
            @PathVariable(value = "spec", required = false) String spec,
            @RequestParam(name = "resolve", required = false, defaultValue = "false") boolean resolve
    ) {
        System.out.println("Checking for spec: " + spec);
        if (this.routes.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "openApiSpec not found");
        if (!StringUtils.hasText(spec)) return checkAndResolve(resolve, this.routes.get(0));
        for (var r : this.routes) {
            System.out.println("Found: " + r.getFilename());
            if (r.getFilename() != null && r.getFilename().contains(spec)) return checkAndResolve(resolve, r);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "openApiSpec '%s' not found".formatted(spec));
    }

    public Resource checkAndResolve(boolean shouldResolve, Resource toResolve) {
        if (!shouldResolve) return toResolve;

        // to fix a problem with spring ubber jar, we first copy all resources to a local folder and then resolve
        // the spec
        try {
            var tempDir = Files.createTempDirectory("openapi");
            File toResolveFile = null;
            for (var r : this.routes) {
                if (r.getFilename() == null) {
                    logger.warn("The resource '%s' has no filename, skipping".formatted(r.toString()));
                    continue;
                }
                var tempFile = tempDir.resolve(r.getFilename());
                if (Objects.equals(r.getFilename(), toResolve.getFilename())) toResolveFile = tempFile.toFile();
                Files.copy(r.getInputStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            if (toResolveFile == null) {
                logger.warn("The resource '%s' was not found in the temp directory, returning unresolved open api".formatted(toResolve.getFilename()));
                return toResolve;
            }

            var resolved = resolve(toResolveFile);

            var asStr = Yaml.mapper()
                    .writeValueAsString(resolved);

            return new ByteArrayResource(asStr.getBytes(), toResolve.getFilename());
        } catch (IOException e) {
            logger.warn("Error creating temp directory for openapi spec, returing unresolved open api", e);
            return toResolve;
        }
    }

    private OpenAPI resolve(File toResolveFile) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        return new OpenAPIV3Parser().read(
                toResolveFile.getAbsolutePath(),
                null,
                parseOptions
        );
    }
}
