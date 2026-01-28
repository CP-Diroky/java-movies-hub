package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    private Map<Long, Movie> movies;
    private long id = 1;

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public void addMovie(int year, String title) {
        movies.put(id, new Movie(year, title, id));
        id++;
    }

    public void removeMovie(long id) {
        movies.remove(id);
    }

    public Movie getMovie(long id) {
        return movies.get(id);
    }

    public Map<Long, Movie> getMovies() {
        return movies;
    }

    public long getId() {
        return id;
    }

    public void cleanStrorage() {
        movies.clear();
        id = 1;
    }
}