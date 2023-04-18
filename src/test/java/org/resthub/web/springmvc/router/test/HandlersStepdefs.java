package org.resthub.web.springmvc.router.test;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.swagger.v3.core.util.Json;
import org.resthub.web.springmvc.router.HTTPRequestAdapter;
import org.resthub.web.springmvc.router.RouterHandlerMapping;
import org.resthub.web.springmvc.router.support.RouterHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


public class HandlersStepdefs {

    private AbstractRefreshableWebApplicationContext wac;
    private HandlerMapping hm;
    private HandlerAdapter ha;


    private String servletPath = "";
    private String contextPath = "";
    private List<HTTPParam> queryParams = new ArrayList<>();
    private List<HTTPHeader> headers = new ArrayList<>();
    private String body = null;

    private MockHttpServletRequest request;

    private String host = "example.org";

    private HandlerExecutionChain chain;

    @Given("^I have a web application with the config locations \"([^\"]*)\"$")
    public void I_have_a_web_applications_with_the_config_locations(String locations) throws Throwable {
        I_have_a_web_application_configured_locations_servletPath_contextPath(locations, "", "");
    }


    @Given("^I have a web application configured locations \"([^\"]*)\" servletPath \"([^\"]*)\" contextPath \"([^\"]*)\"$")
    public void I_have_a_web_application_configured_locations_servletPath_contextPath(String locations, String servletPath, String contextPath) throws Throwable {

        this.servletPath = servletPath;
        this.contextPath = contextPath;

        MockServletContext sc = new MockServletContext();
        sc.setContextPath(contextPath);

        this.wac = new XmlWebApplicationContext();
        this.wac.setServletContext(sc);
        this.wac.setConfigLocations(locations.split(","));
        this.wac.refresh();

        this.hm = this.wac.getBean(RouterHandlerMapping.class);
        this.ha = this.wac.getBean(RequestMappingHandlerAdapter.class);
    }

    @Given("^I have a web application with javaconfig in package \"([^\"]*)\"$")
    public void I_have_a_web_application_with_javaconfig_in_package(String scanPackage) throws Throwable {
        MockServletContext sc = new MockServletContext("");
        AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
        appContext.scan(scanPackage);
        appContext.setServletContext(sc);
        appContext.refresh();

        this.wac = appContext;

        this.hm = appContext.getBean(RouterHandlerMapping.class);
        this.ha = appContext.getBean(RequestMappingHandlerAdapter.class);
    }

    @Given("^I have a web application with javaconfig for openAPI in package \"([^\"]*)\"$")
    public void I_have_a_web_application_with_javaconfig_for_openAPI_in_package(String scanPackage) throws Throwable {
        MockServletContext sc = new MockServletContext("");
        AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
        appContext.scan(scanPackage);
        appContext.setServletContext(sc);
        appContext.refresh();

        this.wac = appContext;

        this.hm = appContext.getBean(RouterHandlerMapping.class);
        this.ha = appContext.getBean(RequestMappingHandlerAdapter.class);
    }

    @Given("^a current request \"([^\"]*)\" \"([^\"]*)\" with servlet path \"([^\"]*)\" and context path \"([^\"]*)\"$")
    public void a_current_request_with_servlet_path_and_context_path(String method, String url, String servletPath, String contextPath) throws Throwable {

        MockServletContext sc = new MockServletContext();
        sc.setContextPath(contextPath);

        int pathLength = 0;
        if (contextPath.length() > 0) {
            pathLength += contextPath.length();
        }

        if (servletPath.length() > 0) {
            pathLength += servletPath.length();
        }

        request = new MockHttpServletRequest(sc, method, url);
        request.setContextPath(contextPath);
        request.setServletPath(servletPath);
        request.addHeader("host", host);

        request.setPathInfo(url.substring(pathLength));

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        HTTPRequestAdapter.parseRequest(request);
    }


    @When("^I send the HTTP request \"([^\"]*)\" \"([^\"]*)\"$")
    public void I_send_the_HTTP_request(String method, String url) throws Throwable {

        int pathLength = 0;
        if (this.contextPath.length() > 0) {
            pathLength += this.contextPath.length();
        }

        if (this.servletPath.length() > 0) {
            pathLength += this.servletPath.length();
        }

        request = new MockHttpServletRequest(this.wac.getServletContext(), method, url);
        request.setContextPath(this.contextPath);
        request.setServletPath(this.servletPath);
        request.addHeader("host", host);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        for (HTTPHeader header : headers) {
            request.addHeader(header.name, header.value);
        }

        for (HTTPParam param : queryParams) {
            request.addParameter(param.name, param.value);
        }

        if (body != null) {
            request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            request.setContent(body.getBytes(StandardCharsets.UTF_8));
        }

        request.setPathInfo(url.substring(pathLength));
        chain = this.hm.getHandler(request);
    }

    @When("^I send the HTTP request \"([^\"]*)\" \"([^\"]*)\" with a null pathInfo$")
    public void I_send_the_HTTP_request_with_a_null_pathInfo(String method, String url) throws Throwable {

        request = new MockHttpServletRequest(this.wac.getServletContext(), method, url);
        request.setContextPath(this.contextPath);
        request.setServletPath(url.replaceFirst(this.contextPath, ""));
        request.addHeader("host", host);

        for (HTTPHeader header : headers) {
            request.addHeader(header.name, header.value);
        }

        for (HTTPParam param : queryParams) {
            request.addParameter(param.name, param.value);
        }

        request.setPathInfo(null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        chain = this.hm.getHandler(request);
    }


    @When("^I send the HTTP request \"([^\"]*)\" \"([^\"]*)\" to host \"([^\"]*)\"$")
    public void I_send_the_HTTP_request_to_host(String method, String url, String host) throws Throwable {

        this.host = host;
        I_send_the_HTTP_request(method, url);
    }

    @When("^I send the HTTP request \"([^\"]*)\" \"([^\"]*)\" with query params:$")
    public void I_send_the_HTTP_request_with_query_params(String method, String url, DataTable queryParams) throws Throwable {

        this.queryParams = queryParams.asMaps().stream().map(m -> new HTTPParam(m.get("name"), m.get("value"))).collect(Collectors.toList());
        I_send_the_HTTP_request(method, url);
    }

    @When("^I send the HTTP request \"([^\"]*)\" \"([^\"]*)\" with headers:$")
    public void I_send_the_HTTP_request_with_headers(String method, String url, DataTable headers) throws Throwable {

        this.headers = headers.asMaps().stream().map(m -> new HTTPHeader(m.get("name"), m.get("value"))).collect(Collectors.toList());
        I_send_the_HTTP_request(method, url);
    }

    @When("^I send the HTTP request \"([^\"]*)\" \"([^\"]*)\" with request:$")
    public void I_send_the_HTTP_request_with_request(String method, String url, DataTable headers) throws Throwable {

        if (headers.asMap().containsKey("body"))
            this.body = headers.asMap().get("body");
        var asMap = headers.asMap();
        this.headers = asMap
                .keySet()
                .stream()
                .filter(h -> h.startsWith("header:"))
                .map(k -> new HTTPHeader(k.substring(k.indexOf(":") + 1), asMap.get(k)))
                .toList();
        I_send_the_HTTP_request(method, url);
    }

    @When("^I send the HTTP request \"([^\"]*)\" \"([^\"]*)\" with body:$")
    public void I_send_the_HTTP_request_with_body(String method, String url, DataTable body) throws Throwable {

        this.body = Json.pretty(body.asMap());
        I_send_the_HTTP_request(method, url);
    }

    @Then("^no handler should be found$")
    public void no_handler_should_be_found() throws Throwable {

        assertThat(chain).isNull();
    }

    @Then("^the request should be handled by \"([^\"]*)\"$")
    public void the_request_should_be_handled_by(String controllerAction) throws Throwable {

        assertThat(chain).isNotNull();
        RouterHandler handler = (RouterHandler) chain.getHandler();

        assertThat(handler).isNotNull();
        assertThat(handler.getRoute()).isNotNull();
        assertThat(handler.getRoute().action).isNotNull().isEqualToIgnoringCase(controllerAction);
    }


    @Then("^the handler should raise a security exception$")
    public void the_handler_should_raise_a_security_exception() throws Throwable {

        assertThat(chain).isNotNull();
        RouterHandler handler = (RouterHandler) chain.getHandler();

        Exception securityException = null;

        try {
            ha.handle(request, new MockHttpServletResponse(), handler);
        } catch (Exception exc) {
            securityException = exc;
        }

        assertThat(securityException).isNotNull().isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Then("^the controller should respond with a ModelAndView containing:$")
    public void the_controller_should_respond_with_a_ModelAndView_containing(DataTable mavparams) throws Throwable {

        assertThat(chain).isNotNull();
        RouterHandler handler = (RouterHandler) chain.getHandler();

        ModelAndView mv = ha.handle(request, new MockHttpServletResponse(), handler);

        for (Map<String, String> param : mavparams.asMaps()) {
            if (param.isEmpty()) continue;
            assertThat(param.get("value")).isEqualTo(mv.getModel().get(param.get("key")).toString());
        }
    }

    @Then("^the server should send an HTTP response with status \"([^\"]*)\"$")
    public void the_server_should_send_an_HTTP_response_with_status(int status) throws Throwable {

        RouterHandler handler = null;
        MockHttpServletResponse response = new MockHttpServletResponse();

        if (chain != null) {
            handler = (RouterHandler) chain.getHandler();
        }
        assertThat(chain).withFailMessage("Can't find router in chain %s %s", this.request.getMethod(), this.request.getRequestURI()).isNotNull();

        HandlerInterceptor[] interceptors = chain.getInterceptors();

        for (HandlerInterceptor interceptor : interceptors) {
            interceptor.preHandle(request, response, handler);
        }

        ha.handle(request, response, handler);
        System.out.println(handler + " response: " + response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(status);
    }

    public static class HTTPHeader {
        public String name;
        public String value;

        public HTTPHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class HTTPParam {
        public String name;
        public String value;

        public HTTPParam(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class MaVParams {
        public String key;
        public String value;
    }

}
