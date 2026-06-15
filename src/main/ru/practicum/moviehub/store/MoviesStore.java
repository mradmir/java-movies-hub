package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MoviesStore {
    private static final List<Movie> movies = new ArrayList<>();
    private static final AtomicInteger nextId = new AtomicInteger(1);

    public static List<Movie> getMovies() {
        return new ArrayList<>(movies);
    }

    public static List<Movie> getMoviesByYear(int year) {
        return movies.stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList());
    }

    public static Optional<Movie> findById(int id) {
        return movies.stream().filter(m -> m.getId() == id).findFirst();
    }

    public static Movie add(String title, int year) {
        Movie movie = new Movie(nextId.getAndIncrement(), title, year);
        movies.add(movie);
        return movie;
    }

    public static boolean delete(int id) {
        return movies.removeIf(m -> m.getId() == id);
    }

    public static void clear() {
        movies.clear();
        nextId.set(1);
    }
}