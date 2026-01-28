package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private int year;
    private String title;
    private long id;

    public Movie(int year, String title, long id) {
        this.year = year;
        this.title = title;
        this.id = id;
    }

    public Movie(int year, String title) {
        this.year = year;
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return year == movie.year && id == movie.id && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, title, id);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "year=" + year +
                ", title='" + title + '\'' +
                ", id=" + id +
                '}';
    }
}