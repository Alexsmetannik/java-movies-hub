package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private int idSequence = 0;

    public Movie add(String title, int year) {
        int id = ++idSequence;
        Movie movie = new Movie(id, title, year);
        movies.put(id, movie);
        return movie;
    }

    public List<Movie> getAll() {
        return movies.values().stream()
                .sorted(Comparator.comparingInt(Movie::getId))
                .collect(Collectors.toList());
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean exists(int id) {
        return movies.containsKey(id);
    }

    public boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> getByYear(int year) {
        return movies.values().stream()
                .filter(m -> m.getYear() == year)
                .sorted(Comparator.comparingInt(Movie::getId))
                .collect(Collectors.toList());
    }

    public void clear() {
        movies.clear();
        idSequence = 0;
    }
}