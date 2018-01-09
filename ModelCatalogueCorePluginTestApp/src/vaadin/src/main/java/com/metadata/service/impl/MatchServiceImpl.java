package com.metadata.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metadata.service.api.MatchService;
import com.metadata.service.domain.Action;
import com.metadata.service.domain.Actions;
import com.vaadin.server.VaadinSession;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Stream;

@Singleton
public class MatchServiceImpl implements MatchService{

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public Stream<Action> getActions(int batchId) {
        final Actions actions = load(batchId);

        return actions != null
            ? actions.getList().stream()
            : Stream.empty();
    }

    @Override
    public int getSize(int batchId) {
        Actions actions = load(batchId);

        return actions != null ? actions.getList().size() : 0;
    }

    private Actions load(Integer batchId) {
        try {
            URL url = new URL("http://localhost:8080/api/modelCatalogue/core/batch/" + batchId + "/actions/?max=200&offset=100");

            final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

            //pass on cookie
            final String cookie = VaadinSession.getCurrent().getSession().getId();

            urlConnection.setRequestProperty("Cookie", "JSESSIONID=" + cookie);

            urlConnection.connect();

            return objectMapper.readValue(
                    urlConnection.getInputStream(),
                    Actions.class
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
