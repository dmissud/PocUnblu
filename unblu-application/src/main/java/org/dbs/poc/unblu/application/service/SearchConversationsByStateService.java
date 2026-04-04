package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.domain.port.in.SearchConversationsByStateUseCase;
import org.dbs.poc.unblu.domain.port.in.query.SearchConversationsByStateQuery;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implémentation du use case {@link SearchConversationsByStateUseCase}.
 * Délègue la recherche filtrée au port secondaire {@link UnbluPort}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchConversationsByStateService implements SearchConversationsByStateUseCase {

    private final UnbluPort unbluPort;

    @Override
    public List<UnbluConversationSummary> searchByState(SearchConversationsByStateQuery query) {
        log.info("Recherche de conversations avec état: {}", query.state());
        List<UnbluConversationSummary> results = unbluPort.searchConversationsByState(query.state());
        log.info("Trouvé {} conversation(s) avec état: {}", results.size(), query.state());
        return results;
    }
}
