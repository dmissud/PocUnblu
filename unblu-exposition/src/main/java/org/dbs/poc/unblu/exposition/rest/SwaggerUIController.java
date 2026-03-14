package org.dbs.poc.unblu.exposition.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to redirect to Swagger UI with Camel OpenAPI documentation
 */
@Controller
public class SwaggerUIController {

    @GetMapping("/swagger-ui")
    public String redirectToSwaggerUI() {
        return "redirect:/swagger/index.html";
    }
}
