package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static ru.practicum.moviehub.Variables.*;

public abstract class BaseHttpHandler implements HttpHandler {
    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, body.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    protected void sendJson(HttpExchange ex, int status, Object obj) throws IOException {
        sendJson(ex, status, gson.toJson(obj));
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(SC_NO_CONTENT, -1);
        ex.close();
    }

    protected void sendError(HttpExchange ex, int status, String error) throws IOException {
        sendJson(ex, status, gson.toJson(ErrorResponse.of(error)));
    }

    protected void sendError(HttpExchange ex, int status, String error, java.util.List<String> details) throws IOException {
        sendJson(ex, status, gson.toJson(ErrorResponse.of(error, details)));
    }

    protected void sendValidationError(HttpExchange ex, List<String> details) throws IOException {
        sendError(ex, SC_UNPROCESSABLE_ENTITY, "Ошибка валидации", details);
    }

    protected String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    protected <T> T fromJson(String body, Class<T> clazz) throws JsonSyntaxException {
        return gson.fromJson(body, clazz);
    }
}