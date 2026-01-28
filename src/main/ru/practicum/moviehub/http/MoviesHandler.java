package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private MoviesStore moviesStore;
    private Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /*
    Данный класс обрабатывает все возможные запросы, были сделаны два метода:
        handleByRoot() - обрабатывает запросы, у которых путь указан в виде /movies или /movies?
        handleById() - обрабатывает запросы, у которых в пути указан id искомого фильма: /movies/{id}
        В методе handle() проверяется путь запроса и вызывается один из методов, описанных выше
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/movies")) {
            handleByRoot(ex);
        } else if (path.startsWith("/movies/")) {
            handleById(ex);
        } else {
            sendJson(ex, 405, "Method not allowed");
        }
    }


    public void handleByRoot(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        List<ErrorResponse> errors = new ArrayList<>();
        String response;
        String query = ex.getRequestURI().getQuery();
        // Получение всех фильмов
        if (method.equalsIgnoreCase("GET") && ex.getRequestURI().getQuery() == null) {
            if (!ex.getRequestHeaders().get("Content-Type").contains("application/json; charset=UTF-8")) {
                response = "Unsupported Media Type";
                sendJson(ex, 415, response);
            } else {
                response = gson.toJson(moviesStore.getMovies().values());
                sendJson(ex, 200, response);
            }
            // Добавление фильма
        } else if (method.equalsIgnoreCase("POST") && query != null) {
            // запросы могут отправляться в двух форматах, сделаем реализацию для обоих:
            //http://localhost:8080/movies?title=name&year=1990
            //http://localhost:8080/movies?year=1990&title=name
            if (!query.contains("&") || !query.contains("title=") || !query.contains("year=") ||
                    query.contains("title= &") || query.contains("title=&") || query.contains("year= &") ||
                    query.contains("year=&")) {
                response = "Неверный формат";
                sendJson(ex, 422, response);
            } else {
                try {
                    String title = query.split("&")[0].split("=")[1];
                    int year = Integer.parseInt(query.split("&")[1].split("=")[1]);
                    if (title.length() > 100) {
                        errors.add(new ErrorResponse("В названии должно быть меньше 100 букв"));
                    }
                    if (title.isBlank()) {
                        errors.add(new ErrorResponse("Название не должно быть пустым"));
                    }
                    if (year < 1888) {
                        errors.add(new ErrorResponse("Год выпуска не должен быть меньше 1888"));
                    }
                    if (year > LocalDate.now().getYear() + 1) {
                        errors.add(new ErrorResponse("Год выпуска не должен быть больше следующего года"));
                    }
                    if (!errors.isEmpty()) {
                        response = gson.toJson(errors);
                        sendJson(ex, 422, response);
                    } else if (!ex.getRequestHeaders().get("Content-Type")
                            .contains("application/json; charset=UTF-8")) {
                        response = "Unsupported Media Type";
                        sendJson(ex, 415, response);
                    } else {
                        moviesStore.addMovie(year, title);
                        response = gson.toJson(moviesStore.getMovie(moviesStore.getId() - 1));
                        sendJson(ex, 201, response);
                    }
                } catch (Exception e) {
                    try {
                        String title = query.split("&")[1].split("=")[1];
                        int year = Integer.parseInt(query.split("&")[0].split("=")[1]);
                        if (title.length() > 100) {
                            errors.add(new ErrorResponse("В названии должно быть меньше 100 букв"));
                        }
                        if (title.isBlank()) {
                            errors.add(new ErrorResponse("Название не должно быть пустым"));
                        }
                        if (year < 1888) {
                            errors.add(new ErrorResponse("Год выпуска не должен быть меньше 1888"));
                        }
                        if (year > LocalDate.now().getYear() + 1) {
                            errors.add(new ErrorResponse("Год выпуска не должен быть больше следующего года"));
                        }
                        if (!errors.isEmpty()) {
                            sendJson(ex, 422, gson.toJson(errors));
                        } else if (!ex.getRequestHeaders().get("Content-Type")
                                .contains("application/json; charset=UTF-8")) {
                            response = "Unsupported Media Type";
                            sendJson(ex, 415, response);
                        } else {
                            moviesStore.addMovie(year, title);
                            response = gson.toJson(moviesStore.getMovie(moviesStore.getId() - 1));
                            sendJson(ex, 201, response);
                        }
                    } catch (Exception exc) {
                        response = "Неверный формат данных";
                        sendJson(ex, 422, response);
                    }
                }
            } // Фильтрация по году выпуска
        } else if (method.equalsIgnoreCase("GET") && query != null && query.startsWith("year=")) {
            try {
                int year = Integer.parseInt(query.split("=")[1]);
                if (year < 1888) {
                    errors.add(new ErrorResponse("Год выпуска не должен быть меньше 1888"));
                }
                if (year > LocalDate.now().getYear()) {
                    errors.add(new ErrorResponse("Год выпуска не должен быть больше следующего года"));
                }
                if (!errors.isEmpty()) {
                    sendJson(ex, 422, gson.toJson(errors));
                } else if (!ex.getRequestHeaders().get("Content-Type").contains("application/json; charset=UTF-8")) {
                    response = "Unsupported Media Type";
                    sendJson(ex, 415, response);
                } else {
                    List<Movie> movieList = moviesStore.getMovies().values().stream()
                            .filter(movie -> movie.getYear() == year).toList();
                    response = gson.toJson(movieList);
                    sendJson(ex, 200, response);
                }
            } catch (NumberFormatException e) {
                response = "Некорректный параметр запроса — 'year'";
                sendJson(ex, 400, response);
            }
        } else {
            sendJson(ex, 405, "Method not allowed");
        }
    }


    public void handleById(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        ErrorResponse error;
        String response;
        // Получение фильма по идентификатору
        if (method.equalsIgnoreCase("GET")) {
            try {
                long id = Long.parseLong(ex.getRequestURI().getPath().split("/")[2]);
                if (moviesStore.getMovie(id) == null) {
                    error = new ErrorResponse("Фильм не найден");
                    sendJson(ex, 404, error.getMessage());
                } else if (!ex.getRequestHeaders().get("Content-Type").contains("application/json; charset=UTF-8")) {
                    response = "Unsupported Media Type";
                    sendJson(ex, 415, response);
                } else {
                    response = gson.toJson(moviesStore.getMovie(id));
                    sendJson(ex, 200, response);
                }
            } catch (NumberFormatException e) {
                error = new ErrorResponse("Некорректный ID");
                sendJson(ex, 400, error.getMessage());
            } // Удаление фильма
        } else if (method.equalsIgnoreCase("DELETE")) {
            try {
                long id = Long.parseLong(ex.getRequestURI().getPath().split("/")[2]);
                if (moviesStore.getMovie(id) == null) {
                    error = new ErrorResponse("Фильм не найден");
                    sendJson(ex, 404, error.getMessage());
                } else if (!ex.getRequestHeaders().get("Content-Type").contains("application/json; charset=UTF-8")) {
                    response = "Unsupported Media Type";
                    sendJson(ex, 415, response);
                } else {
                    moviesStore.removeMovie(id);
                    sendNoContent(ex);
                }
            } catch (NumberFormatException e) {
                error = new ErrorResponse("Некорректный ID");
                sendJson(ex, 400, error.getMessage());
            }
        } else {
            sendJson(ex, 405, "Method not allowed");
        }
    }
}

