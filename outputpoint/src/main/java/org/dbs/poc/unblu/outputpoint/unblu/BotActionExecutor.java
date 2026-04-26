package org.dbs.poc.unblu.outputpoint.unblu;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.bot.BotAction;
import org.dbs.poc.unblu.integration.domain.model.bot.BotActionType;
import org.springframework.stereotype.Component;

/**
 * Exécute les actions bot vers l'API Unblu.
 * Appelé par {@link BotActionListener} pour chaque message consommé.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotActionExecutor {

    private final ApiClient integrationUnbluApiClient;

    public void execute(BotAction action) throws ApiException {
        long start = System.currentTimeMillis();
        switch (action.actionType()) {
            case BotActionType.SET_NAMED_AREA -> executeSetNamedArea(action, start);
            case BotActionType.SEND_MESSAGE   -> executeSendMessage(action, start);
            case BotActionType.HAND_OFF       -> executeHandOff(action, start);
            default -> log.warn("[BOT_EXEC] unknown actionType={} correlationId={}",
                    action.actionType(), action.correlationId());
        }
    }

    private void executeSetNamedArea(BotAction action, long start) throws ApiException {
        String namedAreaId = action.payload().get("namedAreaId");
        ConversationsApi api = new ConversationsApi(integrationUnbluApiClient);
        NamedAreaData recipient = new NamedAreaData();
        recipient.setId(namedAreaId);
        api.conversationsSetRecipient(action.conversationId(), recipient, null);
        log.info("[BOT_EXEC] step=SET_NAMED_AREA correlationId={} conversationId={} namedAreaId={} status=OK durationMs={}",
                action.correlationId(), action.conversationId(), namedAreaId, System.currentTimeMillis() - start);
    }

    private void executeSendMessage(BotAction action, long start) throws ApiException {
        String text = action.payload().get("text");
        BotsApi api = new BotsApi(integrationUnbluApiClient);
        api.botsSendDialogMessage(
                new BotDialogPostMessage()
                        .dialogToken(action.dialogToken())
                        .messageData(new TextPostMessageData()
                                .type(EPostMessageType.TEXT)
                                .text(text)
                                .fallbackText(text))
        );
        log.info("[BOT_EXEC] step=SEND_MESSAGE correlationId={} dialogToken={} textLength={} status=OK durationMs={}",
                action.correlationId(), action.dialogToken(), text.length(), System.currentTimeMillis() - start);
    }

    private void executeHandOff(BotAction action, long start) throws ApiException {
        BotsApi api = new BotsApi(integrationUnbluApiClient);
        api.botsFinishDialog(new BotsFinishDialogBody()
                .dialogToken(action.dialogToken())
                .reason(EBotDialogFinishReason.HAND_OFF));
        log.info("[BOT_EXEC] step=HAND_OFF correlationId={} dialogToken={} status=OK durationMs={}",
                action.correlationId(), action.dialogToken(), System.currentTimeMillis() - start);
    }
}
