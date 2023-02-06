package py.com.volpe.openapi.router.springboot;

import org.resthub.web.springmvc.router.RouterConfigurationSupport;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import py.com.volpe.openapi.router.spring.validator.OpenapiMethodValidationInterceptor;

import java.util.List;

@Configuration
public class OpenApiRoutesConfig extends RouterConfigurationSupport {

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new OpenapiMethodValidationInterceptor());
    }

    @Override
    public List<String> listRouteFiles() {
        return List.of("classpath:petstore.yaml");
    }
}
