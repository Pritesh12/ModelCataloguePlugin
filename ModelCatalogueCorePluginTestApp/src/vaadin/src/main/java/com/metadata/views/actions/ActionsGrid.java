package com.metadata.views.actions;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.metadata.service.domain.Action;
import com.metadata.service.domain.Dest;
import com.metadata.service.domain.Source;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.themes.ValoTheme;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

@UIScope
class ActionsGrid extends Grid<Action> {

    enum RunState {
        YES, NO, MAYBE
    }

    //all run-states must be tracked independently of the actual items
    private final Map<Action, RunState> runStates = new WeakHashMap<>();

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

        addComponentColumn(StartButtonGroup::new)
            .setCaption("Run?")
            .setWidth(240)
            .setId("run");

        setSizeFull();

        setColumnOrder("dataModel1", "dataModel2", "matchScore", "run");

        //style-generator conditionally assigns css-styles to rows
        //all rows with runState Yes get 'yellow-row', all those with 'No' get
        //'gray-row'
        setStyleGenerator(item -> {
            final RunState runState = runStates.computeIfAbsent(item, action -> RunState.MAYBE);

            switch (runState){
                case YES:
                    return "yellow-row";
                case NO:
                    return "gray-row";
                default:
                    return "";
            }
        });
    }

    private final Collection<RunState> ALL_RUN_STATES = ImmutableList.copyOf(RunState.values());

    class StartButtonGroup extends RadioButtonGroup<RunState> {
        StartButtonGroup(Action action){
           super("", ALL_RUN_STATES);
           addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
           addValueChangeListener(e -> {
               //when a different option is selected ( Yes, No, Maybe ),
               //it is put into the runStates and the grid is asked to
               //update the item, which will trigger the styleGenerator
               //for that row
               runStates.put(action, e.getValue());
               ActionsGrid.this.getDataProvider().refreshItem(action);
           });

           //default runState is 'maybe'
           setValue(runStates.computeIfAbsent(action, a -> RunState.MAYBE));
        }
    }
}
