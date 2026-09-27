package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

import static ru.practicum.moviehub.Variables.MOVIES_PATH;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(MOVIES_PATH, new MoviesHandler(store));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public MoviesServer() {
        this(new MoviesStore(), 8080);
    }

    public MoviesStore getStore() {
        return store;
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }
}