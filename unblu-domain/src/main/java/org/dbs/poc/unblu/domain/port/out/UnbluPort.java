package org.dbs.poc.unblu.domain.port.out;

import org.dbs.poc.unblu.domain.model.*;

import java.util.List;


/**
 * Port secondaire vers la plateforme Unblu.
 * Regroupe toutes les opérations Unblu exposées au domaine : conversations, personnes, équipes, zones nommées et bots.
 */
public interface UnbluPort {

    /**
     * Retourne la liste de toutes les conversations présentes dans Unblu.
     * Utilisé pour le scan complet lors de la synchronisation avec la base de données.
     *
     * @return liste des résumés de conversations (jamais {@code null}, peut être vide)
     */
    List<UnbluConversationSummary> listAllConversations();
    /**
     * Creates a new conversation in Unblu and returns its info.
     */
    UnbluConversationInfo createConversation(ConversationContext context);

    /**
     * Searches for persons in Unblu.
     * @param sourceId optional filter by source ID
     * @param personSource optional filter by person source (USER_DB or VIRTUAL)
     */
    List<PersonInfo> searchPersons(String sourceId, PersonSource personSource);

    /**
     * Récupère une personne Unblu directement par sa source et son sourceId.
     * Utilise l'endpoint GET /persons/getBySource, plus fiable que la recherche pour les personnes VIRTUAL.
     *
     * @param personSource le type de source (VIRTUAL ou USER_DB)
     * @param sourceId     l'identifiant source de la personne
     * @return la personne trouvée
     * @throws RuntimeException si la personne n'existe pas ou en cas d'erreur API
     */
    PersonInfo getPersonBySource(PersonSource personSource, String sourceId);

    /**
     * Retourne la liste de toutes les équipes (teams) Unblu.
     *
     * @return liste des équipes
     */
    List<TeamInfo> searchTeams();

    /**
     * Retourne la liste de toutes les zones nommées (named areas) Unblu.
     *
     * @return liste des zones nommées
     */
    List<NamedAreaInfo> searchNamedAreas();

    /**
     * Search for agents who have a specific named area in their queue filter configuration.
     * @param namedAreaId the ID of the named area
     * @return list of agents (PersonInfo) who have this named area in their queue filter
     */
    List<PersonInfo> searchAgentsByNamedArea(String namedAreaId);

    /**
     * Creates a direct conversation between a VIRTUAL person and a USER_DB agent.
     * @param virtualPerson the VIRTUAL participant
     * @param agentPerson   the USER_DB agent participant
     * @param subject       the conversation subject
     */
    UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject);

    /**
     * Sends the summary as a message in the conversation, on behalf of the virtual person.
     */
    void addSummaryToConversation(String conversationId, String summary);

    /**
     * Crée un bot dans Unblu et retourne son identifiant de personne.
     *
     * @param name        nom du bot
     * @param description description du bot
     * @return identifiant de la personne bot créée ({@code botPersonId})
     */
    String createBot(String name, String description);

    /**
     * Récupère tous les messages d'une conversation depuis l'historique Unblu.
     * Parcourt toutes les pages via pagination.
     *
     * @param conversationId identifiant Unblu de la conversation
     * @return liste ordonnée des messages (jamais {@code null}, peut être vide)
     */
    List<UnbluMessageData> fetchConversationMessages(String conversationId);

    /**
     * Récupère les participants d'une conversation depuis l'historique Unblu.
     *
     * @param conversationId identifiant Unblu de la conversation
     * @return liste des participants (jamais {@code null}, peut être vide)
     */
    List<UnbluParticipantData> fetchConversationParticipants(String conversationId);

    /**
     * Recherche les conversations Unblu filtrées par état.
     *
     * @param state l'état recherché (INACTIVE, ACTIVE, ENDED, ONBOARDING, OFFBOARDING)
     * @return liste des résumés de conversations correspondantes (jamais {@code null}, peut être vide)
     */
    List<UnbluConversationSummary> searchConversationsByState(String state);
}
