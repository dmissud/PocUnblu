package org.dbs.poc.unblu.exposition.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * Configuration Spring MVC pour la couche d'exposition.
 * Enregistre les gestionnaires de ressources statiques et un filtre CORS permissif
 * sur les routes {@code /api/*} pour le développement local.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Expose les ressources Swagger UI statiques depuis le classpath sous {@code /swagger/**}.
     *
     * @param registry le registre Spring des gestionnaires de ressources
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger/**")
                .addResourceLocations("classpath:/static/swagger/");
    }

    /**
     * Enregistre le filtre CORS {@link SimpleCorsFilter} sur les routes {@code /api/*}
     * avec une priorité haute (ordre {@code -100}).
     *
     * @return le bean d'enregistrement du filtre CORS
     */
    @Bean
    public FilterRegistrationBean<SimpleCorsFilter> corsFilter() {
        FilterRegistrationBean<SimpleCorsFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SimpleCorsFilter());
        bean.addUrlPatterns("/api/*");
        bean.setOrder(-100);
        return bean;
    }

    private static class SimpleCorsFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "*");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");

            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                httpResponse.setStatus(HttpServletResponse.SC_OK);
                return;
            }

            chain.doFilter(request, response);
        }
    }
}
