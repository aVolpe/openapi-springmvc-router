package org.resthub.web.springmvc.router.test;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.resthub.web.springmvc.router.HTTPRequestAdapter;
import org.resthub.web.springmvc.router.Router;
import org.resthub.web.springmvc.router.exceptions.NoHandlerFoundException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ReverseRoutingStepdefs {

    private HTTPRequestAdapter requestAdapter;

    private Router.ActionDefinition resolvedAction;

    private Exception thrownException;

    @Given("^an empty Router$")
    public void an_empty_Router() throws Throwable {
        // clear routes from the static Router
        Router.clear();
        // clear RequestContextHolder from previous tests
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "http://localhost/");
        request.addHeader("host", "localhost");
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @Given("^I have a route with method \"([^\"]*)\" path \"([^\"]*)\" action \"([^\"]*)\"$")
    public void I_have_a_route_with_method_url_action(String method, String path, String action) throws Throwable {
        Router.prependRoute(method, path, action);
    }

    @Given("^I have routes:$")
    public void I_have_routes(DataTable routes) throws Throwable {
        for (RouteItem item : routes
                .asMaps().stream().map(m -> new RouteItem(m.get("method"), m.get("path"), m.get("action"), m.get("params")))
                .collect(Collectors.toList())) {
            Router.addRoute(item.method, item.path, item.action, item.params, null);
        }
    }

    @Given("^the current request is processed within a context path \"([^\"]*)\" and servlet path \"([^\"]*)\"$")
    public void the_current_request_is_processed_within_a_context_path_and_servlet_path(String contextPath, String servletPath) throws Throwable {

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/reverse-routing");
        request.addHeader("host", "example.org");
        request.setContextPath(contextPath);
        request.setServletPath(servletPath);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        this.requestAdapter = HTTPRequestAdapter.parseRequest(request);
    }

    @When("^I try to reverse route \"([^\"]*)\" with params:$")
    public void I_try_to_reverse_route_with_params(String path, DataTable params) throws Throwable {
        Map<String, Object> routeParams = new HashMap<String, Object>();
        for (ParamItem param : params.asMaps().stream().map(ParamItem::new).collect(Collectors.toList())) {
            routeParams.put(param.key, param.value);
        }
        try {
            resolvedAction = Router.reverse(path, routeParams);
        } catch (Exception exc) {
            this.thrownException = exc;
        }
    }

    @When("^I try to reverse route \"([^\"]*)\"$")
    public void I_try_to_reverse_route(String action) throws Throwable {
        resolvedAction = Router.reverse(action);
    }

    @Then("^I should get an action with path \"([^\"]*)\"$")
    public void I_should_get_an_action_with_URL(String path) throws Throwable {
        assertThat(path).isEqualTo(resolvedAction.url);
    }

    @Then("^I should get an action with path \"([^\"]*)\" and host \"([^\"]*)\"$")
    public void I_should_get_an_action_with_path_and_host(String path, String host) throws Throwable {
        assertThat(path).isEqualTo(resolvedAction.url);
        assertThat(host).isEqualTo(resolvedAction.host);
    }

    @Then("^no action should match$")
    public void no_action_should_match() throws Throwable {
        assertThat(this.thrownException).isNotNull().isInstanceOf(NoHandlerFoundException.class);
    }

    public static class RouteItem {
        public String method;
        public String path;
        public String action;
        public String params;

        public RouteItem(String method, String path, String action, String params) {
            this.method = method;
            this.path = path;
            this.action = action;
            this.params = params;
        }
    }

    public static class ParamItem {
        public String key;
        public String value;

        public ParamItem(Map<String, String> params) {
            this.key = params.get("key");
            this.value = params.get("value");
        }
    }
}
