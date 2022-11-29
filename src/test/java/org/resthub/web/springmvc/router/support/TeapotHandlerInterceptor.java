package org.resthub.web.springmvc.router.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class TeapotHandlerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (request.getParameter("teapot").equals("true")) {
            // I'm a teapot
            response.sendError(418);
            return false;
        }
        return true;
    }
}
