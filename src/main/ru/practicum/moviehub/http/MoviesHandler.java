package ru.practicum.moviehub.http;
import com.sun.net.httpserver.HttpExchange;


import java.io.IOException;

import com.google.gson.Gson;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesHandler extends BaseHttpHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            String json = gson.toJson(MoviesStore.getMovies());
            sendJson(ex, 200, json);
        }
    }
}
