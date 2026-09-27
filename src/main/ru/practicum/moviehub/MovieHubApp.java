package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

import static ru.practicum.moviehub.Variables.PORT;

public class MovieHubApp {
    public static void main(String[] args) {
        final MoviesServer server = new MoviesServer(new MoviesStore(), PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}