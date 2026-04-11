package org.dbs.poc.unblu.webhook.entrypoint.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebhookRootRedirectController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/swagger")
    public String redirectToSwaggerAlias() {
        return "redirect:/swagger-ui/index.html";
    }
}
