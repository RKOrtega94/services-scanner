package org.springdoc.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/api-docs")
public class OpenApiController {

    @GetMapping
    public void docs() {
    }
}

