package com.metadata.views.actions;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import com.metadata.service.domain.Action;
import com.metadata.service.domain.Dest;
import com.metadata.service.domain.Source;
import com.metadata.service.domain.State;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.vaadin.dialogs.ConfirmDialog;

import java.util.Set;

@UIScope
public class ActionsGrid extends Grid<Action> {

    @Inject
    ActionsGrid(ActionDataProvider dataProvider){
        setDataProvider(dataProvider);

        addComponentColumn(action -> {
            final Source dest = action.getMatchParameters().getSource();

            return new Link(
                dest.getSourceClassifiedName(),
                new ExternalResource("#!actions/" + dest.getSourceId())
            );
        })
        .setCaption("Data Model 1")
        .setId("dataModel1");

        addComponentColumn(action -> {
            final Dest dest = action.getMatchParameters().getDest();

            return new Link(
                    dest.getDestClassifiedName(),
                    new ExternalResource("#!actions/" + dest.getDestId())
            );
        })
            .setCaption("Data Model 2")
            .setId("dataModel2");

        addComponentColumn(action -> new Label("" + action.getMatchParameters().getMatch()))
            .setCaption("Match")
            .setId("matchScore");

        addComponentColumn(StartActionButton::new)
            .setCaption("start")
            .setId("start");

        addComponentColumn(e -> new Label(e.getState().toString()))
                .setCaption("state")
                .setId("state");

        setSizeFull();

        setColumnOrder("dataModel1", "dataModel2", "matchScore");
    }

    private final Set<State> startableStates = ImmutableSet.of(
        State.DISMISSED,
        State.FAILED,
        State.UNDECIDED
    );

    class StartActionButton extends Button{
        StartActionButton(Action action){
            super((VaadinIcons.START_COG));
            addStyleName(ValoTheme.BUTTON_BORDERLESS);

            //setEnabled(startableStates.contains(action.getState()));

            addClickListener(event -> ConfirmDialog.show(
                UI.getCurrent(),
                "start action" + action.getId() + "?",
                confirmDialog -> {
                    if(confirmDialog.isConfirmed()){
                        action.setState(State.PERFORMED);
                        ActionsGrid.this.getDataProvider().refreshAll();
                    }
                }
            ));
        }
    }
}
