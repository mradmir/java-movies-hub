package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();      // /movies или /movies/5
        String query  = ex.getRequestURI().getQuery();     // year=2020 или null

        // /movies  (без id)
        if (path.equals("/movies")) {
            switch (method.toUpperCase()) {
                case "GET"  -> handleGetAll(ex, query);
                case "POST" -> handlePost(ex);
                default     -> sendJson(ex, 405,
                        gson.toJson(new ErrorResponse("Метод не поддерживается")));
            }
            return;
        }

        // /movies/{id}
        if (path.startsWith("/movies/")) {
            String idPart = path.substring("/movies/".length());
            try {
                int id = Integer.parseInt(idPart);
                switch (method.toUpperCase()) {
                    case "GET"    -> handleGetById(ex, id);
                    case "DELETE" -> handleDelete(ex, id);
                    default       -> sendJson(ex, 405,
                            gson.toJson(new ErrorResponse("Метод не поддерживается")));
                }
            } catch (NumberFormatException e) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный id")));
            }
            return;
        }

        sendJson(ex, 404, gson.toJson(new ErrorResponse("Маршрут не найден")));
    }

    // GET /movies  или  GET /movies?year=YYYY
    private void handleGetAll(HttpExchange ex, String query) throws IOException {
        if (query != null && query.startsWith("year=")) {
            try {
                int year = Integer.parseInt(query.substring("year=".length()));
                List<Movie> filtered = MoviesStore.getMoviesByYear(year);
                sendJson(ex, 200, gson.toJson(filtered));
            } catch (NumberFormatException e) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный год")));
            }
        } else {
            sendJson(ex, 200, gson.toJson(MoviesStore.getMovies()));
        }
    }

    // POST /movies  — тело: {"title":"...", "year":2020}
    private void handlePost(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (body.isEmpty()) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Тело запроса пустое")));
                return;
            }
            Movie incoming = gson.fromJson(body, Movie.class);
            if (incoming.getTitle() == null || incoming.getTitle().isBlank()) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Поле title обязательно")));
                return;
            }
            if (incoming.getYear() <= 0) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Поле year должно быть положительным")));
                return;
            }
            Movie created = MoviesStore.add(incoming.getTitle(), incoming.getYear());
            sendJson(ex, 201, gson.toJson(created));
        }
    }

    // GET /movies/{id}
    private void handleGetById(HttpExchange ex, int id) throws IOException {
        Optional<Movie> movie = MoviesStore.findById(id);
        if (movie.isPresent()) {
            sendJson(ex, 200, gson.toJson(movie.get()));
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
        }
    }

    // DELETE /movies/{id}
    private void handleDelete(HttpExchange ex, int id) throws IOException {
        boolean deleted = MoviesStore.delete(id);
        if (deleted) {
            sendNoContent(ex);
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
        }
    }
}