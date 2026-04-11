package org.dbs.poc.unblu.engine.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EventProcessorRootRedirectController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/swagger")
    public String redirectToSwaggerAlias() {
        return "redirect:/swagger-ui/index.html";
    }
}
