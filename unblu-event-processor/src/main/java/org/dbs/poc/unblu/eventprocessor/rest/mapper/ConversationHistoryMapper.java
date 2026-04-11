package org.dbs.poc.unblu.eventprocessor.rest.mapper;

import org.dbs.poc.unblu.eventprocessor.rest.dto.*;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component("conversationHistoryResponseMapper")
public class ConversationHistoryMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public ConversationHistoryPageResponse toPageResponse(ConversationHistoryPage page) {
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

    public ConversationHistoryDetailResponse toDetailResponse(ConversationHistory history) {
        List<ConversationParticipantResponse> participants = history.participants().stream()
                .map(p -> ConversationParticipantResponse.builder()
                        .personId(p.personId())
                        .displayName(p.displayName())
                        .participantType(p.participantType().name())
                        .build())
                .toList();

        List<ConversationEventResponse> events = history.events().stream()
                .sorted(Comparator.comparing(e -> e.occurredAt()))
                .map(e -> ConversationEventResponse.builder()
                        .eventType(e.eventType().name())
                        .occurredAt(toIso(e.occurredAt()))
                        .messageText(e.messageText())
                        .senderPersonId(e.senderPersonId())
                        .senderDisplayName(e.senderDisplayName())
                        .build())
                .toList();

        return ConversationHistoryDetailResponse.builder()
                .conversationId(history.conversationId())
                .topic(history.topic())
                .createdAt(toIso(history.startedAt()))
                .endedAt(toIso(history.endedAt()))
                .status(history.isEnded() ? "ENDED" : "ACTIVE")
                .participants(participants)
                .events(events)
                .build();
    }

    private ConversationHistoryItemResponse toItemResponse(ConversationHistory history) {
        return ConversationHistoryItemResponse.builder()
                .conversationId(history.conversationId())
                .topic(history.topic())
                .createdAt(toIso(history.startedAt()))
                .endedAt(toIso(history.endedAt()))
                .status(history.isEnded() ? "ENDED" : "ACTIVE")
                .build();
    }

    private String toIso(Instant instant) {
        return instant != null ? ISO.format(instant) : null;
    }
}
