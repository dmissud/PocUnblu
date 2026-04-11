package org.dbs.poc.unblu.integration.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.application.port.in.EnrichConversationUseCase;
import org.dbs.poc.unblu.integration.domain.model.UnbluMessageData;
import org.dbs.poc.unblu.integration.domain.model.UnbluParticipantData;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichConversationService implements EnrichConversationUseCase {

    private final IntegrationUnbluPort unbluPort;
    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public ConversationHistory enrichOne(String conversationId) {
        ConversationHistory history = conversationHistoryRepository
                .findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation introuvable: " + conversationId));

        List<UnbluParticipantData> participants = unbluPort.fetchConversationParticipants(conversationId);
        for (UnbluParticipantData p : participants) {
            history.registerParticipant(p.personId(), p.displayName(), resolveType(p.personSource()));
        }
        Map<String, String> nameByPersonId = participants.stream()
                .collect(Collectors.toMap(UnbluParticipantData::personId, UnbluParticipantData::displayName));

        List<UnbluMessageData> messages = unbluPort.fetchConversationMessages(conversationId);
        for (UnbluMessageData m : messages) {
            history.backfillMessage(m.messageId(), m.text(), m.senderPersonId(),
                    nameByPersonId.get(m.senderPersonId()), m.sentAt());
        }
        conversationHistoryRepository.save(history);
        log.info("Conversation {} enriched — {}p, {}m", conversationId, participants.size(), messages.size());
        return conversationHistoryRepository.findByConversationId(conversationId).orElse(history);
    }

    private ParticipantHistory.ParticipantType resolveType(String personSource) {
        return "USER_DB".equals(personSource) ? ParticipantHistory.ParticipantType.AGENT
                : ParticipantHistory.ParticipantType.VISITOR;
    }
}
