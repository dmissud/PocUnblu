package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.history.ConversationEventHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.exposition.rest.dto.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Mapper Camel pour les routes de consultation de l'historique des conversations.
 * Convertit les objets domaine en DTOs REST.
 */
@Component
public class ConversationHistoryQueryMapper {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ENDED = "ENDED";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private String toIso(Instant instant) {
        return instant != null ? ISO.format(instant) : null;
    }

    /**
     * Mappe la {@link ConversationHistoryPage} domaine en {@link ConversationHistoryPageResponse} REST.
     */
    public void mapPageToResponse(Exchange exchange) {
        ConversationHistoryPage page = exchange.getIn().getBody(ConversationHistoryPage.class);
        exchange.getIn().setBody(toPageResponse(page));
    }

    /**
     * Mappe le {@link ConversationHistory} domaine en {@link ConversationHistoryDetailResponse} REST.
     * Retourne un 404 si le corps est {@code null}.
     */
    public void mapDetailToResponse(Exchange exchange) {
        ConversationHistory history = exchange.getIn().getBody(ConversationHistory.class);
        if (history == null) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setBody(null);
            return;
        }
        exchange.getIn().setBody(toDetailResponse(history));
    }

    private ConversationHistoryPageResponse toPageResponse(ConversationHistoryPage page) {
        List<ConversationHistoryItemResponse> items = page.items().stream()
                .map(this::toItemResponse)
                .toList();
        return ConversationHistoryPageResponse.builder()
                .items(items)
                .totalItems(page.totalItems())
                .page(page.page())
                .size(page.size())
                .totalPages(page.totalPages())
                .build();
    }

    private ConversationHistoryItemResponse toItemResponse(ConversationHistory history) {
        return ConversationHistoryItemResponse.builder()
                .conversationId(history.conversationId())
                .topic(history.topic())
                .createdAt(toIso(history.startedAt()))
                .endedAt(toIso(history.endedAt()))
                .status(history.isEnded() ? STATUS_ENDED : STATUS_ACTIVE)
                .build();
    }

    private ConversationHistoryDetailResponse toDetailResponse(ConversationHistory history) {
        List<ConversationParticipantResponse> participants = history.participants().stream()
                .map(this::toParticipantResponse)
                .toList();

        List<ConversationEventResponse> events = history.events().stream()
                .sorted(Comparator.comparing(ConversationEventHistory::occurredAt))
                .map(this::toEventResponse)
                .toList();

        return ConversationHistoryDetailResponse.builder()
                .conversationId(history.conversationId())
                .topic(history.topic())
                .createdAt(toIso(history.startedAt()))
                .endedAt(toIso(history.endedAt()))
                .status(history.isEnded() ? STATUS_ENDED : STATUS_ACTIVE)
                .participants(participants)
                .events(events)
                .build();
    }

    private ConversationParticipantResponse toParticipantResponse(ParticipantHistory participant) {
        return ConversationParticipantResponse.builder()
                .personId(participant.personId())
                .displayName(participant.displayName())
                .participantType(participant.participantType().name())
                .build();
    }

    private ConversationEventResponse toEventResponse(ConversationEventHistory event) {
        return ConversationEventResponse.builder()
                .eventType(event.eventType().name())
                .occurredAt(toIso(event.occurredAt()))
                .messageText(event.messageText())
                .senderPersonId(event.senderPersonId())
                .senderDisplayName(event.senderDisplayName())
                .build();
    }
}
