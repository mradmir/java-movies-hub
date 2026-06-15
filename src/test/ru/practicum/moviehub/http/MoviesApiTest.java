package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // Чистим хранилище перед каждым тестом — независимость тестов
    @BeforeEach
    void beforeEach() {
        MoviesStore.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) server.stop();
    }

    // ─── GET /movies ────────────────────────────────────────────────────────────

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode());
        assertJsonContentType(resp);
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
        assertEquals("[]", body);
    }

    @Test
    void getMovies_whenNotEmpty_returnsList() throws Exception {
        MoviesStore.add("Inception", 2010);
        MoviesStore.add("Dune", 2021);

        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode());
        Movie[] movies = gson.fromJson(resp.body(), Movie[].class);
        assertEquals(2, movies.length);
    }

    // ─── GET /movies?year=YYYY ──────────────────────────────────────────────────

    @Test
    void getMovies_filterByYear_returnsMatching() throws Exception {
        MoviesStore.add("Inception", 2010);
        MoviesStore.add("Tenet", 2020);
        MoviesStore.add("Interstellar", 2014);

        HttpResponse<String> resp = get("/movies?year=2010");

        assertEquals(200, resp.statusCode());
        Movie[] movies = gson.fromJson(resp.body(), Movie[].class);
        assertEquals(1, movies.length);
        assertEquals("Inception", movies[0].getTitle());
    }

    @Test
    void getMovies_filterByYear_noMatch_returnsEmptyArray() throws Exception {
        MoviesStore.add("Inception", 2010);

        HttpResponse<String> resp = get("/movies?year=1900");

        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body().trim());
    }

    @Test
    void getMovies_filterByYear_invalidYear_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies?year=abc");
        assertEquals(400, resp.statusCode());
    }

    // ─── POST /movies ────────────────────────────────────────────────────────────

    @Test
    void postMovie_validBody_returns201WithMovie() throws Exception {
        String body = "{\"title\":\"Inception\",\"year\":2010}";

        HttpResponse<String> resp = post("/movies", body);

        assertEquals(201, resp.statusCode());
        assertJsonContentType(resp);
        Movie created = gson.fromJson(resp.body(), Movie.class);
        assertEquals("Inception", created.getTitle());
        assertEquals(2010, created.getYear());
        assertTrue(created.getId() > 0, "id должен быть положительным");
    }

    @Test
    void postMovie_emptyBody_returns400() throws Exception {
        HttpResponse<String> resp = post("/movies", "");
        assertEquals(400, resp.statusCode());
    }

    @Test
    void postMovie_missingTitle_returns400() throws Exception {
        HttpResponse<String> resp = post("/movies", "{\"year\":2010}");
        assertEquals(400, resp.statusCode());
    }

    @Test
    void postMovie_missingYear_returns400() throws Exception {
        HttpResponse<String> resp = post("/movies", "{\"title\":\"Inception\"}");
        assertEquals(400, resp.statusCode());
    }

    // ─── GET /movies/{id} ───────────────────────────────────────────────────────

    @Test
    void getMovieById_exists_returns200() throws Exception {
        Movie added = MoviesStore.add("Dune", 2021);

        HttpResponse<String> resp = get("/movies/" + added.getId());

        assertEquals(200, resp.statusCode());
        assertJsonContentType(resp);
        Movie found = gson.fromJson(resp.body(), Movie.class);
        assertEquals(added.getId(), found.getId());
        assertEquals("Dune", found.getTitle());
    }

    @Test
    void getMovieById_notFound_returns404() throws Exception {
        HttpResponse<String> resp = get("/movies/9999");
        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMovieById_invalidId_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies/abc");
        assertEquals(400, resp.statusCode());
    }

    // ─── DELETE /movies/{id} ────────────────────────────────────────────────────

    @Test
    void deleteMovie_exists_returns204() throws Exception {
        Movie added = MoviesStore.add("Interstellar", 2014);

        HttpResponse<String> resp = delete("/movies/" + added.getId());

        assertEquals(204, resp.statusCode());
        // Проверяем, что фильм действительно удалён
        HttpResponse<String> getResp = get("/movies/" + added.getId());
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void deleteMovie_notFound_returns404() throws Exception {
        HttpResponse<String> resp = delete("/movies/9999");
        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteMovie_invalidId_returns400() throws Exception {
        HttpResponse<String> resp = delete("/movies/xyz");
        assertEquals(400, resp.statusCode());
    }

    // ─── Вспомогательные методы ─────────────────────────────────────────────────

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertJsonContentType(HttpResponse<String> resp) {
        String ct = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", ct,
                "Content-Type должен быть application/json; charset=UTF-8");
    }
}