package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
    private String error;
    private List<String> details;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, new ArrayList<>());
    }

    public static ErrorResponse of(String error, List<String> details) {
        return new ErrorResponse(error, details);
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}