package org.resthub.web.springmvc.router;

import io.swagger.v3.oas.models.Operation;
import jregex.Matcher;
import jregex.Pattern;
import jregex.REFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Arturo Volpe
 * @since 2023-02-06
 */
public class Route {

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    public String getAction() {
        return action;
    }

    public String getHost() {
        return host;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<Arg> getArgs() {
        return args;
    }

    public Map<String, String> getStaticArgs() {
        return staticArgs;
    }


    /**
     * HTTP method, e.g. "GET".
     */
    public String method;
    public String path;
    public String action;
    Pattern actionPattern;
    List<String> actionArgs = new ArrayList<String>(3);
    Pattern pattern;
    Pattern hostPattern;
    List<Arg> args = new ArrayList<Arg>(3);
    public Map<String, String> staticArgs = new HashMap<String, String>(3);
    public List<String> formats = new ArrayList<>(1);
    String host;
    Arg hostArg = null;
    public int routesFileLine;
    public String routesFile;
    static Pattern customRegexPattern = new Pattern("\\{([a-zA-Z_0-9]+)\\}");
    static Pattern argsPattern = new Pattern("\\{<([^>]+)>([a-zA-Z_0-9]+)\\}");
    public Operation operation;

    public void compute() {
        this.host = "";
        this.hostPattern = new Pattern(".*");


        // URL pattern
        // Is there is a host argument, append it.
        if (!path.startsWith("/")) {
            String p = this.path;
            this.path = p.substring(p.indexOf("/"));
            this.host = p.substring(0, p.indexOf("/"));
            String pattern = host.replaceAll("\\.", "\\\\.").replaceAll("\\{.*\\}", "(.*)");

            if (logger.isTraceEnabled()) {
                logger.trace("pattern [{}]", pattern);
                logger.trace("host [{}]", host);
            }

            Matcher m = new Pattern(pattern).matcher(host);
            this.hostPattern = new Pattern(pattern);

            if (m.matches()) {
                if (this.host.contains("{")) {
                    String name = m.group(1).replace("{", "").replace("}", "");
                    hostArg = new Arg();
                    hostArg.name = name;
                    if (logger.isTraceEnabled()) {
                        logger.trace("hostArg name [{}]", name);
                    }
                    // The default value contains the route version of the host ie {client}.bla.com
                    // It is temporary and it indicates it is an url route.
                    // TODO Check that default value is actually used for other cases.
                    hostArg.defaultValue = host;
                    hostArg.constraint = new Pattern(".*");

                    if (logger.isTraceEnabled()) {
                        logger.trace("adding hostArg [{}]", hostArg);
                    }

                    args.add(hostArg);
                }
            }
        }
        String patternString = path;
        patternString = customRegexPattern.replacer("\\{<[^/]+>$1\\}").replace(patternString);
        Matcher matcher = argsPattern.matcher(patternString);
        while (matcher.find()) {
            Arg arg = new Arg();
            arg.name = matcher.group(2);
            arg.constraint = new Pattern(matcher.group(1));
            args.add(arg);
        }

        patternString = argsPattern.replacer("({$2}$1)").replace(patternString);
        this.pattern = new Pattern(patternString);
        // Action pattern
        patternString = action;
        patternString = patternString.replace(".", "[.]");
        for (Arg arg : args) {
            if (patternString.contains("{" + arg.name + "}")) {
                patternString = patternString.replace("{" + arg.name + "}", "({" + arg.name + "}" + arg.constraint.toString() + ")");
                actionArgs.add(arg.name);
            }
        }
        actionPattern = new Pattern(patternString, REFlags.IGNORE_CASE);
    }


    // TODO: Add args names

    private boolean contains(String accept) {
        boolean contains = (accept == null);
        if (accept != null) {
            if (this.formats.isEmpty()) {
                return true;
            }
            for (String format : this.formats) {
                contains = format.startsWith(accept);
                if (contains) {
                    break;
                }
            }
        }
        return contains;
    }

    public Map<String, String> matches(String method, String path) {
        return matches(method, path, null, null);
    }

    public Map<String, String> matches(String method, String path, String accept) {
        return matches(method, path, accept, null);
    }

    /**
     * Check if the parts of a HTTP request equal this Route.
     *
     * @param method GET/POST/etc.
     * @param path   Part after domain and before query-string. Starts with a
     *               "/".
     * @param accept Format, e.g. html.
     * @param domain the domain.
     * @return ???
     */
    public Map<String, String> matches(String method, String path, String accept, String domain) {
        // If method is HEAD and we have a GET
        if (method == null || this.method.equals("*") || method.equalsIgnoreCase(this.method) || (method.equalsIgnoreCase("head") && ("get").equalsIgnoreCase(this.method))) {

            Matcher matcher = pattern.matcher(path);

            boolean hostMatches = (domain == null);
            if (domain != null) {
                Matcher hostMatcher = hostPattern.matcher(domain);
                hostMatches = hostMatcher.matches();
            }
            // Extract the host variable
            if (matcher.matches() && contains(accept) && hostMatches) {

                Map<String, String> localArgs = new HashMap<>();
                for (Arg arg : args) {
                    // FIXME: Careful with the arguments that are not matching as they are part of the hostname
                    // Defaultvalue indicates it is a one of these urls. This is a trick and should be changed.
                    if (arg.defaultValue == null) {
                        localArgs.put(arg.name, matcher.group(arg.name));
                    }
                }
                if (hostArg != null && domain != null) {
                    // Parse the hostname and get only the part we are interested in
                    String routeValue = hostArg.defaultValue.replaceAll("\\{.*}", "");
                    domain = domain.replace(routeValue, "");
                    localArgs.put(hostArg.name, domain);
                }
                localArgs.putAll(staticArgs);
                return localArgs;
            }
        }
        return null;
    }

    public static class Arg {

        String name;
        Pattern constraint;
        String defaultValue;
        Boolean optional = false;

        public String getName() {
            return name;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    @Override
    public String toString() {
        return method + " " + path + " -> " + action;
    }

    public String toFixedLengthString() {
        return String.format("%-8s%-50s%-25s", method, path, action);
    }
}
