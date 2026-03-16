package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.NamedAreaInfo;

import java.util.List;

public interface SearchNamedAreasUseCase {
    List<NamedAreaInfo> searchNamedAreas();
}
