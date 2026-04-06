package org.dbs.poc.unblu.livekit.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur Spring MVC gérant la redirection de la racine vers Swagger UI pour LiveKit.
 */
@Controller
public class LiveKitRootRedirectController {

    /**
     * Redirige la requête vers {@code /swagger-ui/index.html}.
     *
     * @return la chaîne de redirection Spring MVC
     */
    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }

    /**
     * Redirige la requête vers {@code /swagger-ui/index.html}.
     *
     * @return la chaîne de redirection Spring MVC
     */
    @GetMapping("/swagger")
    public String redirectToSwaggerAlias() {
        return "redirect:/swagger-ui/index.html";
    }
}
