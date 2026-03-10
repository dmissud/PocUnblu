package org.dbs.poc.unblu.infrastructure.adapter.unblu;


import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Component;

@Component
public class UnbluResilientRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // We wrap the base unblu rest adapter with a circuit breaker
        from("direct:unblu-adapter-resilient")
            .routeId("unblu-resilient-wrapper")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(3000).end()
                .to("direct:unblu-adapter")
            .onFallback()
                .log("⚠️ L'API Unblu est injoignable ou a expiré. Déclenchement du Fallback.")
                .process(exchange -> {
                    ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                    ctx.setUnbluConversationId("OFFLINE-PENDING");
                    ctx.setUnbluJoinUrl("Le service de chat est temporairement indisponible.");
                    exchange.getIn().setBody(ctx);
                })
            .end();
    }
}
