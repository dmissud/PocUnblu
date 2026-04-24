package org.dbs.poc.unblu.jobs.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de redirection pour le module Jobs.
 * Redirige la racine et /swagger vers l'interface Swagger UI.
 */
@Controller
public class JobsRootRedirectController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/swagger")
    public String redirectToSwaggerAlias() {
        return "redirect:/swagger-ui/index.html";
    }
}

// Made with Bob
