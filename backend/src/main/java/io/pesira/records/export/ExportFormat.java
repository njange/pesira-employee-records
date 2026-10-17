package io.pesira.records.export;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

enum ExportFormat {
    CSV("text/csv; charset=UTF-8", "csv"),
    JSON("application/json", "json");

    final String contentType;
    final String extension;

    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    static ExportFormat parse(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "csv" -> CSV;
            case "json" -> JSON;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format must be csv or json");
        };
    }
}
