package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.MovieRequest;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;

import static ru.practicum.moviehub.Constants.*;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    private static Integer tryParseInt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            try {
                result.put(
                        URLDecoder.decode(key, StandardCharsets.UTF_8),
                        URLDecoder.decode(value, StandardCharsets.UTF_8)
                );
            } catch (IllegalArgumentException e) {
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            if (!path.equals(MOVIES_PATH) && !path.startsWith(MOVIES_PATH + "/")) {
                sendError(ex, SC_NOT_FOUND, "Ресурс не найден");
                return;
            }

            String idPart = path.equals(MOVIES_PATH)
                    ? null
                    : path.substring(MOVIES_PATH.length() + 1);

            if (idPart != null && idPart.isEmpty()) {
                sendError(ex, SC_NOT_FOUND, "Ресурс не найден");
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                if (idPart == null) {
                    doGetAll(ex);
                } else {
                    doGetById(ex, idPart);
                }
            } else if (method.equalsIgnoreCase("POST") && idPart == null) {
                doPost(ex);
            } else if (method.equalsIgnoreCase("DELETE") && idPart != null) {
                doDelete(ex, idPart);
            } else {
                sendError(ex, SC_METHOD_NOT_ALLOWED, "Метод не поддерживается");
            }
        } catch (IllegalArgumentException e) {
            sendError(ex, SC_BAD_REQUEST, "Некорректный параметр запроса");
        } catch (Exception e) {
            sendError(ex, SC_INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
        }
    }

    private void doGetAll(HttpExchange ex) throws IOException {
        Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());

        if (query.containsKey("year")) {
            String yearRaw = query.get("year");
            Integer year = tryParseInt(yearRaw);
            if (year == null) {
                sendError(ex, SC_BAD_REQUEST, "Некорректный параметр запроса — 'year'");
                return;
            }
            sendJson(ex, SC_OK, store.getByYear(year));
            return;
        }

        sendJson(ex, SC_OK, store.getAll());
    }

    private void doGetById(HttpExchange ex, String idPart) throws IOException {
        Integer id = tryParseInt(idPart);
        if (id == null) {
            sendError(ex, SC_BAD_REQUEST, "Некорректный ID");
            return;
        }

        Optional<Movie> movie = store.getById(id);
        if (movie.isEmpty()) {
            sendError(ex, SC_NOT_FOUND, "Фильм не найден");
            return;
        }

        sendJson(ex, SC_OK, movie.get());
    }

    private void doPost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            sendError(ex, SC_UNSUPPORTED_MEDIA_TYPE, "Неподдерживаемый Content-Type");
            return;
        }

        String body = readBody(ex);
        MovieRequest request;
        try {
            request = fromJson(body, MovieRequest.class);
        } catch (JsonSyntaxException e) {
            sendError(ex, SC_UNPROCESSABLE_ENTITY, "Некорректный JSON");
            return;
        }
        if (request == null) {
            sendError(ex, SC_UNPROCESSABLE_ENTITY, "Некорректный JSON");
            return;
        }

        List<String> details = new ArrayList<>();

        String title = request.getTitle() == null ? null : request.getTitle().trim();
        if (title == null || title.trim().isEmpty()) {
            details.add("Название не должно быть пустым");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            details.add("Название не должно быть длиннее " + MAX_TITLE_LENGTH + " символов");
        }

        int maxYear = Year.now().getValue() + 1;
        if (request.getYear() == null) {
            details.add("Год должен быть между " + MIN_YEAR + " и " + maxYear);
        } else if (request.getYear() < MIN_YEAR || request.getYear() > maxYear) {
            details.add("Год должен быть между " + MIN_YEAR + " и " + maxYear);
        }

        if (!details.isEmpty()) {
            sendValidationError(ex, details);
            return;
        }

        Movie created = store.add(title, request.getYear());
        sendJson(ex, SC_CREATED, created);
    }

    private void doDelete(HttpExchange ex, String idPart) throws IOException {
        Integer id = tryParseInt(idPart);
        if (id == null) {
            sendError(ex, SC_BAD_REQUEST, "Некорректный ID");
            return;
        }

        if (!store.delete(id)) {
            sendError(ex, SC_NOT_FOUND, "Фильм не найден");
            return;
        }

        sendNoContent(ex);
    }
}
