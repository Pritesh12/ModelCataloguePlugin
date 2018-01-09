package com.metadata.views.actions;

import com.google.inject.Inject;

import com.metadata.service.api.MatchService;
import com.metadata.service.domain.Action;
import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.guice.annotation.UIScope;

import java.util.stream.Stream;

@UIScope
public class ActionDataProvider extends AbstractBackEndDataProvider<Action, Integer> {

    @Inject
    private MatchService matchService;

    private int batchId;

    @Override
    protected Stream<Action> fetchFromBackEnd(Query<Action, Integer> query) {
        return matchService.getActions(batchId);
    }

    @Override
    protected int sizeInBackEnd(Query<Action, Integer> query) {
        return matchService.getSize(batchId);
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }
}
