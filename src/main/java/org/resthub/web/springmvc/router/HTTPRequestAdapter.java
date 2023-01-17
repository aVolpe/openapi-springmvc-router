package org.resthub.web.springmvc.router;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Adapter class for HTTP class defined in Play! Framework Maps
 * HTTPServletRequest to HTTP.Request and HTTP.Header
 *
 * @author Brian Clozel
 * @see org.resthub.web.springmvc.router.Router
 */
public class HTTPRequestAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HTTPRequestAdapter.class);

    /**
     * Server host
     */
    public String host;
    /**
     * Request path
     */
    public String path;
    /**
     * Context path
     */
    public String contextPath;
    /**
     * Servlet path
     */
    public String servletPath;
    /**
     * QueryString
     */
    public String querystring;
    /**
     * Full url
     */
    public String url;
    /**
     * HTTP method
     */
    public String method;
    /**
     * Server domain
     */
    public String domain;
    /**
     * Client address
     */
    public String remoteAddress;
    /**
     * Request content-type
     */
    public String contentType;
    /**
     * Controller to invoke
     */
    public String controller;
    /**
     * Action method name
     */
    public String actionMethod;
    /**
     * HTTP port
     */
    public Integer port;
    /**
     * HTTP Headers
     */
    public Map<String, HTTPRequestAdapter.Header> headers = new HashMap<String, HTTPRequestAdapter.Header>();
    /**
     * Additinal HTTP params extracted from route
     */
    public Map<String, String> routeArgs;
    /**
     * Format (html,xml,json,text)
     */
    public String format = null;
    /**
     * Full action (ex: Application.index)
     */
    public String action;
    /**
     * The really invoker Java methid
     */
    public transient Method invokedMethod;
    /**
     * The invoked controller class
     */
    public transient Class controllerClass;
    /**
     * Free space to store your request specific data
     */
    public Map<String, Object> args = new HashMap<String, Object>();
    /**
     * When the request has been received
     */
    public Date date = new Date();
    /**
     * is HTTPS ?
     */
    public boolean secure = false;

    public HTTPRequestAdapter() {

        this.headers = new HashMap<String, Header>();
    }

    public void setFormat(String _format) {

        this.format = _format;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getQueryString() {
        return querystring;
    }

    public void setQueryString(String queryString) {
        this.querystring = queryString;
    }

    /**
     * Get the request base (ex: http://localhost:9000
     *
     * @return the request base of the url (protocol, host and port)
     */
    public String getBase() {
        if (port == 80 || port == 443) {
            return String.format("%s://%s", secure ? "https" : "http", domain).intern();
        }
        return String.format("%s://%s:%s", secure ? "https" : "http", domain,
                port).intern();
    }

    public static HTTPRequestAdapter parseRequest(
            HttpServletRequest httpServletRequest) {
        HTTPRequestAdapter request = new HTTPRequestAdapter();

        request.method = httpServletRequest.getMethod().intern();
        request.path = httpServletRequest.getPathInfo() != null ? httpServletRequest.getPathInfo() : httpServletRequest.getServletPath();
        request.servletPath = httpServletRequest.getServletPath() != null ? httpServletRequest.getServletPath() : "";
        request.contextPath = httpServletRequest.getContextPath() != null ? httpServletRequest.getContextPath() : "";
        request.setQueryString(httpServletRequest.getQueryString() == null ? ""
                : httpServletRequest.getQueryString());

        logger.trace("contextPath: {}  servletPath: {}", request.contextPath, request.servletPath);
        logger.trace("request.path: {}, request.queryString: {}", request.path, request.getQueryString());

        if (httpServletRequest.getHeader("Content-Type") != null) {
            request.contentType = httpServletRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            request.contentType = "text/html";
        }

        if (httpServletRequest.getHeader("X-HTTP-Method-Override") != null) {
            request.method = httpServletRequest.getHeader(
                    "X-HTTP-Method-Override").intern();
        }

        request.setSecure(httpServletRequest.isSecure());

        request.url = httpServletRequest.getRequestURI();
        request.host = httpServletRequest.getHeader("host");
        if (request.host != null && request.host.contains(":")) {
            request.port = Integer.parseInt(request.host.split(":")[1]);
            request.domain = request.host.split(":")[0];
        } else {
            request.port = 80;
            request.domain = request.host;
        }

        request.remoteAddress = httpServletRequest.getRemoteAddr();

        Enumeration<String> headersNames = httpServletRequest.getHeaderNames();
        while (headersNames.hasMoreElements()) {
            HTTPRequestAdapter.Header hd = new Header();
            hd.name = headersNames.nextElement();
            hd.values = new ArrayList<>();
            Enumeration<String> enumValues = httpServletRequest.getHeaders(hd.name);
            while (enumValues.hasMoreElements()) {
                String value = enumValues.nextElement();
                hd.values.add(value);
            }
            request.headers.put(hd.name.toLowerCase(), hd);
        }

        request.format = resolveFormat(request.headers.getOrDefault("accept", new Header()).value());

        return request;
    }

	public static HTTPRequestAdapter getCurrent() {
		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		Assert.notNull(requestAttributes, "Could not find current request via RequestContextHolder");
		HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		Assert.state(servletRequest != null, "Could not find current HttpServletRequest");
		return HTTPRequestAdapter.parseRequest(servletRequest);
	}

    /**
     * Automatically resolve request format from the Accept header (in this
     * order : html > xml > json > text)
     */
    public static String resolveFormat(String accept) {

        if (accept == null) {
            return null;
        }

        if (accept.contains("application/xhtml")
                || accept.contains("text/html")
                || accept.startsWith("*/*")) {
            return "html";
        }

        if (accept.contains("application/xml")
                || accept.contains("text/xml")) {
            return "xml";
        }

        if (accept.contains("text/plain")) {
            return "txt";
        }

        if (accept.contains("application/json")
                || accept.contains("text/javascript")) {
            return "json";
        }

        return null;
    }

    public static class Header {

        public String name;
        public List<String> values;

        public Header() {
        }

        /**
         * First value
         *
         * @return The first value
         */
        public String value() {
            if (values == null || values.isEmpty()) return null;
            return values.get(0);
        }
    }
}
