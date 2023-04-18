package org.resthub.web.springmvc.router.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
public class MyTestController {

    public void simpleAction() {
    }

    public void additionalRouteFile() {
    }

    public void wildcardA() {
    }

    public void wildcardB() {
    }

    public void caseInsensitive() {
    }

    public void overrideMethod() {
    }

    public void paramAction(@PathVariable(value = "param") String param) {
    }

    public void httpAction(@PathVariable(value = "type") String type) {
    }

    public void regexNumberAction(@PathVariable(value = "number") int number) {
    }

    public void regexStringAction(@PathVariable(value = "string") String string) {
    }

    public void hostAction(@PathVariable(value = "host") String host) {
    }

    @ResponseBody
    public List<String> listPets() {
        return Arrays.asList("DOG", "CAT");
    }

    public ResponseEntity<Map<String, String>> createPet(@RequestBody Object body) throws URISyntaxException {
        return ResponseEntity.created(new URI("/"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Collections.singletonMap("data", body.toString()));
    }

    @ResponseBody
    public Map<String, String> showPetById(@PathVariable("petId") String id) {
        return Collections.singletonMap("name", id);
    }

    @ResponseBody
    public Map<String, String> updatePetById(@PathVariable("petId") String id, @RequestBody Object body) {
        return Map.of("id", id, "data", body.toString());
    }
}
