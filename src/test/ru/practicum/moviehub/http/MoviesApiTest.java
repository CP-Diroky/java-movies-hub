package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;
    private static HttpResponse<String> response;
    private static HttpResponse.BodyHandler<String> handler;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        client = HttpClient.newHttpClient();
        handler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        gson = new Gson();
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        server.getMoviesStore().cleanStrorage();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    //Проверка эндпоинта GET /movies в случае если список фильмов пустой
    @Test
    void GetMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .uri(URI.create(BASE + "/movies"))
                .build();
        response = client.send(request, handler);
        // если массив пустой, то должна вернуться строка "[]" длиной 2 символа
        Assertions.assertEquals(2, response.body().length());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта GET /movies в случае если в списке есть один фильм
    @Test
    void ShouldReturnMovie() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .uri(URI.create(BASE + "/movies"))
                .build();
        server.getMoviesStore().addMovie(1984, "Терминатор");
        response = client.send(request, handler);
        JsonElement jsonElement = JsonParser.parseString(response.body());
        JsonObject jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
        int year = jsonObject.get("year").getAsInt();
        String title = jsonObject.get("title").getAsString();
        long id = jsonObject.get("id").getAsLong();
        Movie movieFromResponse = new Movie(year, title, id);
        Assertions.assertEquals(new Movie(1984, "Терминатор", 1), movieFromResponse);
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта POST /movies при добавлении фильма
    @Test
    void ShouldAddMovie() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?title=Терминатор&year=1984"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта POST /movies при пустом заголовке
    @Test
    void ShouldThrowErrorWhenTitleIsEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?title=&year=1984"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта POST /movies при заголовке больше 100 символов
    @Test
    void ShouldThrowErrorWhenTitleIs100Symbols() throws Exception {
        String title = "Это название фильма состоит из большого количества символов для " +
                "тестирования ограничения." +
                "Ограничение жесткое! Нарушать его нельзя!";
        title = title.replaceAll("\\s", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?title=" + title + "&year=1984"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта POST /movies при неправильном вводе года
    @Test
    void ShouldThrowErrorWhenYearIsNotCorrect() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?title=Терминатор&year=1000"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(422, response.statusCode());
        request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?title=Терминатор&year=3000"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта POST /movies при неправильном вводе заголовка
    @Test
    void ShouldThrowErrorWhenContentTypeIsWrong() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?title=Терминатор&year=1984"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json; charset=UTF-812")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(415, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта POST /movies при некорректном JSON.
    @Test
    void ShouldThrowErrorWhenJSonIsWrong() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?title:Терминатор&year=1984"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта GET /movies/{id} на возвращение по id
    @Test
    void ShouldReturnMovieById() throws Exception {
        server.getMoviesStore().addMovie(1984, "Терминатор");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Movie movieFromResponse = gson.fromJson(response.body(), Movie.class);
        Assertions.assertEquals(new Movie(1984, "Терминатор", 1), movieFromResponse);
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта GET /movies/{id} в случае если id не найден
    @Test
    void ShouldReturn404WhenIdNotFound() throws Exception {
        server.getMoviesStore().addMovie(1984, "Терминатор");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertTrue(response.statusCode() == 404);
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта GET /movies/{id} в случае если id не число
    @Test
    void ShouldReturnErrorWhenIdIsNotNumber() throws Exception {
        server.getMoviesStore().addMovie(1984, "Терминатор");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertTrue(response.statusCode() == 400);
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта DELETE /movies/{id} по id
    @Test
    void ShouldDelete() throws Exception {
        server.getMoviesStore().addMovie(1984, "Терминатор");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertTrue(response.statusCode() == 204);
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта DELETE /movies/{id} в случае когда id не найден
    @Test
    void ShouldNotDeleteWhenIdNotFound() throws Exception {
        server.getMoviesStore().addMovie(1984, "Терминатор");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .DELETE()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertTrue(response.statusCode() == 404);
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта DELETE /movies/{id} в случае когда id не является числом
    @Test
    void ShouldNotDeleteWhenIdNotNumber() throws Exception {
        server.getMoviesStore().addMovie(1984, "Терминатор");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertTrue(response.statusCode() == 400);
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта GET /movies?year=YYYY
    @Test
    void ShouldShowMoviesByYear() throws Exception {
        server.getMoviesStore().addMovie(1984, "Терминатор");
        server.getMoviesStore().addMovie(1984, "Гремлины");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1984"))
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        List<Movie> movieList = List.of(new Movie(1984, "Терминатор", 1),
                new Movie(1984, "Гремлины", 2));
        Assertions.assertEquals(movieList, gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType()));
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта GET /movies?year=YYYY в случае если список пустой
    @Test
    void ShouldShowNoMoviesByYear() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1984"))
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(2, response.body().length());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //Проверка эндпоинта GET /movies?year=YYYY в случае если year не является числом
    @Test
    void ShouldThrowErrorWhenYearIsNotNumber() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }

    //При неподдерживаемом HTTP-методе возвращается 405 Method Not Allowed.
    @Test
    void ShouldThrowErrorWhenMethodNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .DELETE()
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        response = client.send(request, handler);
        Assertions.assertEquals(405, response.statusCode());
        Assertions.assertTrue(response.headers().map().get("Content-Type").contains("application/json; charset=UTF-8"));
    }


}