package org.dbs.poc.unblu.outputpoint.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.dbs.poc.unblu.integration.domain.model.bot.BotAction;
import org.dbs.poc.unblu.outputpoint.unblu.BotActionExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consommateur Spring Kafka du topic {@code unblu.bot.actions}.
 *
 * <p>Pour chaque action reçue, délègue l'exécution à {@link BotActionExecutor}.
 * En cas d'erreur Unblu API après retry, parque le message dans {@code unblu.bot.actions.dlq}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotActionListener {

    private final ObjectMapper objectMapper;
    private final BotActionExecutor executor;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.bot-actions-dlq:unblu.bot.actions.dlq}")
    private String dlqTopic;

    @KafkaListener(
            topics = "${kafka.topic.bot-actions:unblu.bot.actions}",
            groupId = "${spring.kafka.consumer.group-id:unblu-outputpoint}",
            concurrency = "3"
    )
    public void onBotAction(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String json = record.value();
        BotAction action = null;

        try {
            action = objectMapper.readValue(json, BotAction.class);
            log.info("[BOT_ACTION] actionType={} correlationId={} dialogToken={}",
                    action.actionType(), action.correlationId(), action.dialogToken());

            executor.execute(action);
            ack.acknowledge();

        } catch (ApiException e) {
            log.error("[BOT_ACTION] Unblu API error — httpStatus={} actionType={} correlationId={} — sending to DLQ",
                    e.getCode(),
                    action != null ? action.actionType() : "unknown",
                    action != null ? action.correlationId() : "unknown", e);
            sendToDlq(json, e.getMessage());
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[BOT_ACTION] Processing error — sending to DLQ: {}", e.getMessage(), e);
            sendToDlq(json, e.getMessage());
            ack.acknowledge();
        }
    }

    private void sendToDlq(String originalPayload, String errorMessage) {
        try {
            kafkaTemplate.send(dlqTopic, originalPayload);
            log.warn("[BOT_ACTION] Message parked in DLQ: {}", dlqTopic);
        } catch (Exception e) {
            log.error("[BOT_ACTION] Failed to send to DLQ: {}", e.getMessage(), e);
        }
    }
}
