package py.com.volpe.openapi.router.spring.validator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.resthub.web.springmvc.router.support.RouterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Handler interceptor that validates against an open api specification
 *
 * @author Arturo Volpe
 * @since 2023-02-06
 */
public class OpenapiMethodValidationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(OpenapiMethodValidationInterceptor.class);

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        if (handler instanceof RouterHandler rh) {
            if (rh.getRoute().operation != null) {
                logger.warn("Validating open api request with {}", rh.getRoute().operation);
            }
        }

        logger.warn(handler.getClass().toString());


        return true;
    }
}
