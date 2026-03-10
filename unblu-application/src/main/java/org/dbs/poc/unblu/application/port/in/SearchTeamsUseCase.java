package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.TeamInfo;

import java.util.List;

public interface SearchTeamsUseCase {
    List<TeamInfo> searchTeams();
}
