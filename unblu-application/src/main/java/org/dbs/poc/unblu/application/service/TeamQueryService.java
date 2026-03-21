package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SearchTeamsUseCase;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamQueryService implements SearchTeamsUseCase {

    private final UnbluPort unbluPort;

    @Override
    public List<TeamInfo> searchTeams() {
        log.info("Récupération des équipes Unblu");
        return unbluPort.searchTeams();
    }
}
