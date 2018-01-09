package com.metadata.service.api;

import com.metadata.service.domain.Action;

import java.util.stream.Stream;

public interface MatchService {
    Stream<Action> getActions(int batchId);
    int getSize(int batchId);
}
