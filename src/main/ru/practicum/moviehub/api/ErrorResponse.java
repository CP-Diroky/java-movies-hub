package ru.practicum.moviehub.api;

public class ErrorResponse {
    private String message;

    public ErrorResponse(String message) {
        this.message = message;
        System.out.println(message);
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "message='" + message + '\'' +
                '}';
    }
}