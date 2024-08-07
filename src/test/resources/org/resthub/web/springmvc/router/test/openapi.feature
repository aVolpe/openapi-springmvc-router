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

    When I send the HTTP request "POST" "/pet" with body:
      | name | Test alias  |
      | kind | DOG         |
    Then the server should send an HTTP response with status "201"

    When I send the HTTP request "POST" "/pet_form_post" with body content "application/x-www-form-urlencoded" and expect "application/json" with body:
      | name | Homero  |
      | id   | homer   |
    Then the server should send an HTTP response with status "201"
    Then the server should send an HTTP header with name "Location" and value "/pets/homer"

    When I send the HTTP clean request "GET" "/v3/api-docs"
    Then the server should send an HTTP response with status "200"

    When I send the HTTP clean request "GET" "/v3/api-docs/petstore"
    Then the server should send an HTTP response with status "200"

    When I send the HTTP clean request "GET" "/v3/api-docs/other-openapi"
    Then the server should send an HTTP response with status "200"
    Then the response is a valid open api

    When I send the HTTP request "GET" "/v3/api-docs/other-openapi" with query params:
      | name    | value           |
      | resolve | true            |
    Then the server should send an HTTP response with status "200"
    Then the response is a valid open api
