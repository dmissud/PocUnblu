package org.dbs.poc.unblu.engine.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.integration.domain.model.ClientContext;
import org.dbs.poc.unblu.integration.domain.model.bot.BotAction;
import org.dbs.poc.unblu.integration.domain.model.bot.BotActionType;
import org.dbs.poc.unblu.integration.domain.model.bot.BotCommand;
import org.dbs.poc.unblu.integration.domain.port.out.ClientContextPort;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationSummaryPort;
import org.dbs.poc.unblu.integration.domain.port.out.NamedAreaResolverPort;
import org.dbs.poc.unblu.integration.domain.port.out.ResourceUrlPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Route Camel orchestrant les 3 appels systèmes externes pour un {@code BOT_DIALOG_OPENED}.
 *
 * <p>Séquence :
 * <ol>
 *   <li>CRM → résolution contexte client ({@link ClientContextPort})</li>
 *   <li>LLM → génération résumé ({@link ConversationSummaryPort})</li>
 *   <li>Moteur de règles → résolution named area ({@link NamedAreaResolverPort})</li>
 *   <li>Service documentaire → URL contextuelle ({@link ResourceUrlPort})</li>
 * </ol>
 *
 * <p>Publie ensuite 2 ou 3 actions sur {@code unblu.bot.actions} :
 * {@code SET_NAMED_AREA} (si résolu), {@code SEND_MESSAGE}, {@code HAND_OFF}.
 */
@Slf4j
@Component
public class BotDialogProcessorRoute extends RouteBuilder {

    static final String ROUTE_ID = "bot-dialog-processor";
    static final String DIRECT_ENTRY = BotCommandConsumerRoute.DIRECT_BOT_DIALOG_PROCESSOR;
    private static final String DIRECT_BOT_ACTION_PUBLISHER = "direct:bot-action-publisher";

    private final ObjectMapper objectMapper;
    private final ClientContextPort clientContextPort;
    private final ConversationSummaryPort summaryPort;
    private final NamedAreaResolverPort namedAreaResolverPort;
    private final ResourceUrlPort resourceUrlPort;

    @Value("${kafka.topic.bot-actions:unblu.bot.actions}")
    private String actionsTopic;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public BotDialogProcessorRoute(ObjectMapper objectMapper,
                                   ClientContextPort clientContextPort,
                                   ConversationSummaryPort summaryPort,
                                   NamedAreaResolverPort namedAreaResolverPort,
                                   ResourceUrlPort resourceUrlPort) {
        this.objectMapper = objectMapper;
        this.clientContextPort = clientContextPort;
        this.summaryPort = summaryPort;
        this.namedAreaResolverPort = namedAreaResolverPort;
        this.resourceUrlPort = resourceUrlPort;
    }

    @Override
    public void configure() {
        configurePublisherRoute();
        configureProcessorRoute();
    }

    private void configurePublisherRoute() {
        from(DIRECT_BOT_ACTION_PUBLISHER)
                .routeId("bot-action-publisher")
                .process(exchange -> {
                    BotAction action = exchange.getIn().getBody(BotAction.class);
                    exchange.getIn().setBody(objectMapper.writeValueAsString(action));
                    exchange.getIn().setHeader("kafka.KEY", action.dialogToken());
                })
                .to("kafka:" + actionsTopic + "?brokers=" + bootstrapServers)
                .log(LoggingLevel.INFO, "bot-action-publisher",
                        "Action published — actionType=${header.CamelBotActionType} topic=" + actionsTopic);
    }

    private void configureProcessorRoute() {
        from(DIRECT_ENTRY)
                .routeId(ROUTE_ID)
                .process(exchange -> {
                    BotCommand command = exchange.getIn().getBody(BotCommand.class);
                    long start = System.currentTimeMillis();
                    log.info("[BOT_PROC] step=START correlationId={} conversationId={}",
                            command.correlationId(), command.conversationId());

                    // --- Étape 1 : CRM ---
                    long t1 = System.currentTimeMillis();
                    ClientContext clientContext = clientContextPort.resolveClientContext(command.conversationId());
                    log.info("[BOT_PROC] step=CLIENT_CONTEXT correlationId={} segment={} language={} durationMs={}",
                            command.correlationId(), clientContext.segment(), clientContext.language(),
                            System.currentTimeMillis() - t1);

                    // --- Étape 2 : LLM ---
                    long t2 = System.currentTimeMillis();
                    String summary = summaryPort.generateSummary(command.conversationId());
                    log.info("[BOT_PROC] step=SUMMARY_GENERATED correlationId={} summaryLength={} durationMs={}",
                            command.correlationId(), summary != null ? summary.length() : 0,
                            System.currentTimeMillis() - t2);

                    // --- Étape 3 : Named area ---
                    long t3 = System.currentTimeMillis();
                    String namedAreaId = namedAreaResolverPort.resolveNamedAreaId(clientContext);
                    log.info("[BOT_PROC] step=NAMED_AREA_RESOLVED correlationId={} namedAreaId={} durationMs={}",
                            command.correlationId(), namedAreaId != null ? namedAreaId : "null (skipped)",
                            System.currentTimeMillis() - t3);

                    // --- Étape 4 : URL ---
                    long t4 = System.currentTimeMillis();
                    String resourceUrl = resourceUrlPort.computeResourceUrl(clientContext);
                    log.info("[BOT_PROC] step=URL_COMPUTED correlationId={} url={} durationMs={}",
                            command.correlationId(), resourceUrl, System.currentTimeMillis() - t4);

                    // Stocke les résultats dans les propriétés de l'exchange pour la suite
                    exchange.setProperty("botCommand", command);
                    exchange.setProperty("clientContext", clientContext);
                    exchange.setProperty("summary", summary);
                    exchange.setProperty("namedAreaId", namedAreaId);
                    exchange.setProperty("resourceUrl", resourceUrl);
                    exchange.setProperty("processingStartMs", start);
                })
                // Publie SET_NAMED_AREA si un named area a été résolu
                .choice()
                    .when(exchange -> exchange.getProperty("namedAreaId") != null)
                        .process(exchange -> {
                            BotCommand cmd = exchange.getProperty("botCommand", BotCommand.class);
                            String namedAreaId = exchange.getProperty("namedAreaId", String.class);
                            exchange.getIn().setBody(new BotAction(
                                    BotActionType.SET_NAMED_AREA,
                                    cmd.correlationId(),
                                    cmd.dialogToken(),
                                    cmd.conversationId(),
                                    Map.of("namedAreaId", namedAreaId)
                            ));
                        })
                        .to(DIRECT_BOT_ACTION_PUBLISHER)
                .end()
                // Publie SEND_MESSAGE
                .process(exchange -> {
                    BotCommand cmd = exchange.getProperty("botCommand", BotCommand.class);
                    String summary = exchange.getProperty("summary", String.class);
                    String resourceUrl = exchange.getProperty("resourceUrl", String.class);
                    String text = summary + "\n\nVotre espace personnel : " + resourceUrl;
                    exchange.getIn().setBody(new BotAction(
                            BotActionType.SEND_MESSAGE,
                            cmd.correlationId(),
                            cmd.dialogToken(),
                            cmd.conversationId(),
                            Map.of("text", text)
                    ));
                })
                .to(DIRECT_BOT_ACTION_PUBLISHER)
                // Publie HAND_OFF
                .process(exchange -> {
                    BotCommand cmd = exchange.getProperty("botCommand", BotCommand.class);
                    exchange.getIn().setBody(new BotAction(
                            BotActionType.HAND_OFF,
                            cmd.correlationId(),
                            cmd.dialogToken(),
                            cmd.conversationId(),
                            Map.of("reason", "HAND_OFF")
                    ));
                })
                .to(DIRECT_BOT_ACTION_PUBLISHER)
                .process(exchange -> {
                    BotCommand cmd = exchange.getProperty("botCommand", BotCommand.class);
                    long start = exchange.getProperty("processingStartMs", Long.class);
                    log.info("[BOT_PROC] step=DONE correlationId={} conversationId={} totalDurationMs={}",
                            cmd.correlationId(), cmd.conversationId(), System.currentTimeMillis() - start);
                });
    }
}
