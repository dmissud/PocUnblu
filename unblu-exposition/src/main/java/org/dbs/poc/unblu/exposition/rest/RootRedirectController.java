package org.dbs.poc.unblu.exposition.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur Spring MVC gérant les redirections de la racine de l'application.
 * Redirige {@code /} vers l'UI Angular et {@code /swagger} vers la Swagger UI.
 */
@Controller
public class RootRedirectController {

    /**
     * Redirige la requête vers {@code /index.html} (Angular SPA).
     *
     * @return la chaîne de redirection Spring MVC
     */
    @GetMapping("/")
    public String redirectToIndex() {
        return "redirect:/index.html";
    }

    /**
     * Redirige la requête vers {@code /swagger-ui.html}.
     *
     * @return la chaîne de redirection Spring MVC
     */
    @GetMapping("/swagger")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui.html";
    }
}
