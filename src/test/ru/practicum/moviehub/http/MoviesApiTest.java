package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.practicum.moviehub.Constants.*;

public class MoviesApiTest {
    private static final int PORT = 8080;
    private static final String BASE = "http://localhost:" + PORT;
    private static final int CURRENT_YEAR = java.time.Year.now().getValue();
    private static final int MAX_YEAR = CURRENT_YEAR + 1;
    private static final Gson gson = new Gson();
    private static MoviesServer server;
    private static HttpClient client;

    private static HttpResponse<String> send(String method, String url, String body, String contentType)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5));

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }

        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void assertErrorResponse(HttpResponse<String> resp, int expectedStatus) {
        assertEquals(expectedStatus, resp.statusCode(), "Неверный статус ответа");
        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
                "Ошибка должна приходить с Content-Type application/json; charset=UTF-8");

        JsonObject error = gson.fromJson(resp.body().trim(), JsonObject.class);
        assertNotNull(error, "Тело ошибки должно быть JSON-объектом");
        assertNotNull(error.get("error"), "В теле ошибки должно быть поле error");
        assertFalse(error.get("error").getAsString().isEmpty(),
                "Поле error не должно быть пустым");
    }

    private static void assertValidationError(HttpResponse<String> resp) {
        assertErrorResponse(resp, SC_UNPROCESSABLE_ENTITY);
        JsonObject error = gson.fromJson(resp.body().trim(), JsonObject.class);
        assertNotNull(error.getAsJsonArray("details"), "У 422 должен быть массив details");
        assertFalse(error.getAsJsonArray("details").isEmpty(),
                "Массив details не должен быть пустым");
        assertEquals("Ошибка валидации", error.get("error").getAsString());
    }

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = send("GET", BASE + MOVIES_PATH, null, null);

        assertEquals(SC_OK, resp.statusCode());
        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty(), "Ожидается пустой массив");
    }

    @Test
    void getMovies_whenHasItems_returnsAll() throws Exception {
        server.getStore().add("Матрица", 1999);
        server.getStore().add("Начало", 2010);

        HttpResponse<String> resp = send("GET", BASE + MOVIES_PATH, null, null);

        assertEquals(SC_OK, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertEquals("Матрица", movies.get(0).getTitle());
        assertEquals(1999, movies.get(0).getYear());
        assertEquals("Начало", movies.get(1).getTitle());
    }

    @Test
    void getMovies_whenPathIsNotMovies_returns404() throws Exception {
        HttpResponse<String> resp = send("GET", BASE + "/movies123", null, null);
        assertErrorResponse(resp, SC_NOT_FOUND);
    }

    @Test
    void postMovies_whenValid_returns201AndCreatedMovie() throws Exception {
        String json = "{\"title\":\"Интерстеллар\",\"year\":2014}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertEquals(SC_CREATED, resp.statusCode());
        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""));
        Movie created = gson.fromJson(resp.body().trim(), Movie.class);
        assertTrue(created.getId() > 0, "id должен быть присвоен");
        assertEquals("Интерстеллар", created.getTitle());
        assertEquals(2014, created.getYear());
    }

    @Test
    void postMovies_whenEmptyTitle_returns422() throws Exception {
        String json = "{\"title\":\"\",\"year\":2014}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertValidationError(resp);
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "a".repeat(MAX_TITLE_LENGTH + 1);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2014}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertValidationError(resp);
    }

    @Test
    void postMovies_whenTitleExactly100_returns201() throws Exception {
        String title = "a".repeat(MAX_TITLE_LENGTH);
        String json = "{\"title\":\"" + title + "\",\"year\":2014}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertEquals(SC_CREATED, resp.statusCode());
    }

    @Test
    void postMovies_whenTitle101_returns422() throws Exception {
        String title = "a".repeat(MAX_TITLE_LENGTH + 1);
        String json = "{\"title\":\"" + title + "\",\"year\":2014}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertValidationError(resp);
    }

    @Test
    void postMovies_whenTitleHasLeadingSpaceOfExact100_returns201() throws Exception {
        String title = " " + "a".repeat(MAX_TITLE_LENGTH - 1);
        String json = "{\"title\":\"" + title + "\",\"year\":2014}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertEquals(SC_CREATED, resp.statusCode(), "После trim длина = 100, должно быть 201");
    }

    @Test
    void postMovies_whenYearAtLowerBound_returns201() throws Exception {
        String json = "{\"title\":\"Рождённый в 1888\",\"year\":" + MIN_YEAR + "}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertEquals(SC_CREATED, resp.statusCode());
    }

    @Test
    void postMovies_whenYearBelowLowerBound_returns422() throws Exception {
        String json = "{\"title\":\"Слишком старый\",\"year\":" + (MIN_YEAR - 1) + "}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertValidationError(resp);
    }

    @Test
    void postMovies_whenYearAtUpperBound_returns201() throws Exception {
        String json = "{\"title\":\"Почти будущий\",\"year\":" + MAX_YEAR + "}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertEquals(SC_CREATED, resp.statusCode());
    }

    @Test
    void postMovies_whenYearAboveUpperBound_returns422() throws Exception {
        String json = "{\"title\":\"Слишком будущий\",\"year\":" + (MAX_YEAR + 1) + "}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertValidationError(resp);
    }

    @Test
    void postMovies_whenYearTooSmall_returns422() throws Exception {
        String json = "{\"title\":\"Старый фильм\",\"year\":1800}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertValidationError(resp);
    }

    @Test
    void postMovies_whenYearTooBig_returns422() throws Exception {
        String json = "{\"title\":\"Будущий фильм\",\"year\":9999}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertValidationError(resp);
    }

    @Test
    void postMovies_whenWrongContentType_returns415() throws Exception {
        String json = "{\"title\":\"Интерстеллар\",\"year\":2014}";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, "text/plain");

        assertErrorResponse(resp, SC_UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void postMovies_whenBrokenJson_returns422() throws Exception {
        String json = "{not a json at all";

        HttpResponse<String> resp = send("POST", BASE + MOVIES_PATH, json, CT_JSON);

        assertErrorResponse(resp, SC_UNPROCESSABLE_ENTITY);
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie saved = server.getStore().add("Матрица", 1999);

        HttpResponse<String> resp = send("GET", BASE + "/movies/" + saved.getId(), null,
                null);

        assertEquals(SC_OK, resp.statusCode());
        Movie found = gson.fromJson(resp.body().trim(), Movie.class);
        assertEquals(saved.getId(), found.getId());
        assertEquals("Матрица", found.getTitle());
        assertEquals(1999, found.getYear());
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpResponse<String> resp = send("GET", BASE + "/movies/9999", null, null);

        assertErrorResponse(resp, SC_NOT_FOUND);
    }

    @Test
    void getMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpResponse<String> resp = send("GET", BASE + "/movies/abc", null, null);

        assertErrorResponse(resp, SC_BAD_REQUEST);
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie saved = server.getStore().add("Матрица", 1999);

        HttpResponse<String> resp = send("DELETE", BASE + "/movies/" + saved.getId(), null,
                null);

        assertEquals(SC_NO_CONTENT, resp.statusCode());
        assertEquals(0, server.getStore().getAll().size(), "Фильм должен быть удалён");
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpResponse<String> resp = send("DELETE", BASE + "/movies/9999", null, null);

        assertErrorResponse(resp, SC_NOT_FOUND);
    }

    @Test
    void deleteMovie_whenIdNotNumber_returns400() throws Exception {
        HttpResponse<String> resp = send("DELETE", BASE + "/movies/abc", null, null);

        assertErrorResponse(resp, SC_BAD_REQUEST);
    }

    @Test
    void getMoviesByYear_whenMatches_returnsFiltered() throws Exception {
        server.getStore().add("Матрица", 1999);
        server.getStore().add("Начало", 2010);
        server.getStore().add("Матрица: Перезагрузка", 1999);

        HttpResponse<String> resp = send("GET", BASE + "/movies?year=1999", null, null);

        assertEquals(SC_OK, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 1999));
    }

    @Test
    void getMoviesByYear_whenNoMatches_returnsEmptyArray() throws Exception {
        server.getStore().add("Матрица", 1999);

        HttpResponse<String> resp = send("GET", BASE + "/movies?year=1900", null, null);

        assertEquals(SC_OK, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMoviesByYear_whenNotNumber_returns400() throws Exception {
        HttpResponse<String> resp = send("GET", BASE + "/movies?year=abc", null, null);

        assertErrorResponse(resp, SC_BAD_REQUEST);
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpResponse<String> resp = send("PUT", BASE + MOVIES_PATH, "{}", CT_JSON);

        assertErrorResponse(resp, SC_METHOD_NOT_ALLOWED);
    }
}