package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            List<Movie> movies = store.getAll();
            sendJson(ex, 200, movies);
        } else {
            sendError(ex, 405, "Метод не поддерживается");
        }
    }
}
