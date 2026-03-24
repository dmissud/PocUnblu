package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.EnrichConversationUseCase;
import org.dbs.poc.unblu.domain.model.UnbluMessageData;
import org.dbs.poc.unblu.domain.model.UnbluParticipantData;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation du cas d'utilisation d'enrichissement à la demande d'une conversation.
 *
 * <p>Récupère depuis Unblu les participants et les messages de la conversation,
 * les persiste en base de données et retourne l'entité mise à jour.
 *
 * <p>L'opération est idempotente : les participants et messages déjà présents
 * sont remplacés par les données fraîches remontées depuis Unblu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichConversationService implements EnrichConversationUseCase {

    private final UnbluPort unbluPort;
    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public ConversationHistory enrichOne(String conversationId) {
        ConversationHistory history = conversationHistoryRepository
                .findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversation introuvable en base : " + conversationId));

        List<UnbluParticipantData> participants = unbluPort.fetchConversationParticipants(conversationId);
        for (UnbluParticipantData p : participants) {
            history.registerParticipant(p.personId(), p.displayName(), resolveParticipantType(p.personSource()));
        }

        // Index personId → displayName pour résoudre le nom de l'émetteur sur chaque message
        Map<String, String> displayNameByPersonId = participants.stream()
                .collect(Collectors.toMap(UnbluParticipantData::personId, UnbluParticipantData::displayName));

        List<UnbluMessageData> messages = unbluPort.fetchConversationMessages(conversationId);
        for (UnbluMessageData m : messages) {
            String senderDisplayName = displayNameByPersonId.get(m.senderPersonId());
            history.backfillMessage(m.messageId(), m.text(), m.senderPersonId(), senderDisplayName, m.sentAt());
        }

        conversationHistoryRepository.save(history);
        log.info("Conversation {} enrichie depuis Unblu — {} participant(s), {} message(s)",
                conversationId, participants.size(), messages.size());

        return conversationHistoryRepository.findByConversationId(conversationId)
                .orElse(history);
    }

    private ParticipantHistory.ParticipantType resolveParticipantType(String personSource) {
        if ("USER_DB".equals(personSource)) {
            return ParticipantHistory.ParticipantType.AGENT;
        }
        return ParticipantHistory.ParticipantType.VISITOR;
    }
}
