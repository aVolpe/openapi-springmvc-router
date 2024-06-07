# OpenAPI Router for Spring MVC

[![Release](https://jitpack.io/v/avolpe/openapi-springmvc-router.svg)](https://jitpack.io/#avolpe/openapi-springmvc-router)

This module allows you to define the routes for your Spring MVC application using an OpenAPI definition. It parses the OpenAPI documentation and uses the `operationId` to map to a controller operation. This provides a centralized way to manage your routes and makes it easier to refactor URLs.

## Usage

### Add the Dependency

First, you need to add the dependency to your project. If you're using Gradle, add the following to your `build.gradle` file:

```groovy
repositories {
    ...
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation 'com.github.avolpe:openapi-springmvc-router:2.2.1'
}
```

If you're using Maven, add this to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.github.avolpe</groupId>
        <artifactId>openapi-springmvc-router</artifactId>
        <version>2.2.1</version>
    </dependency>
</dependencies>
```

### Configuration

To configure this module, import the config in your main class

```java
@Configuration
@EnableOpenApiRouter(config="classpath:openapi.yml")
public class MyApplication {
}
```

### Example Code

Here's an example of how you can use this module in your Spring MVC application:

```yaml
paths:
  /pets:
    get:
      operationId: myController.listPets
      responses:
        '200':
          content:
            application/json:
              schema:
                type: object
```

```java
@Controller
public class MyController {

    public List<Pet> listPets() {
        // Your code here
    }
}
```

### Properties

The following properties can be used to configure the module:

```properties
openapi.router.routeFiles=classpath:openapi.yml
openapi.router.specRoute=/v3/api-docs
```
