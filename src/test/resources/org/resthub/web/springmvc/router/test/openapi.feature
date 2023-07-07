Feature: java config support with openapi
  As a developer coding a application
  I want to configure beans using java config and a openAPI definition as source of truth
  In order to setup my application

  Scenario: Parsing open API definition
    Given I have a web application with javaconfig for openAPI in package "org.resthub.web.springmvc.router.openapi"
    When I send the HTTP request "GET" "/pets"
    Then the server should send an HTTP response with status "200"

    When I send the HTTP request "GET" "/pets/1234"
    Then the server should send an HTTP response with status "200"

    When I send the HTTP request "GET" "/pets/1234" with headers:
      | Accept | */*  |
    Then the server should send an HTTP response with status "200"

    When I send the HTTP request "POST" "/pets" with body:
      | name | HOMERO  |
      | kind | DOG     |
    Then the server should send an HTTP response with status "201"

    When I send the HTTP request "POST" "/pets" with request:
      | body          | { "name": "HOMERON", "kind": "DOG" }  |
      | header:accept | */*                                   |
    Then the server should send an HTTP response with status "201"

    When I send the HTTP request "PUT" "/pets/1234" with request:
      | body          | { "name": "HOMERON", "kind": "DOG" }  |
      | header:accept | */*                                   |
    Then the server should send an HTTP response with status "200"

    When I send the HTTP request "POST" "/pets" with body:
      | name | Test alias  |
      | kind | DOG         |
    Then the server should send an HTTP response with status "201"
