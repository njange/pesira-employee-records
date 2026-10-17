package io.pesira.records.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.IntFunction;

import io.pesira.records.employee.EmployeeResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingConsumer;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

// Streams rows to the client one page at a time. Each page is loaded in its own short
// transaction, so loaded entities are released between pages and memory stays flat.
@Component
class EmployeeExporter {

    static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 10_000 / PAGE_SIZE;

    private final JsonMapper jsonMapper;

    EmployeeExporter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    void writeCsv(List<EmployeeResponse> firstPage, IntFunction<List<EmployeeResponse>> loadPage,
                  OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        CsvWriter csv = new CsvWriter(writer);
        csv.writeRow("id", "firstName", "lastName", "email", "department", "jobTitle", "salary", "hireDate",
                "status", "createdAt", "updatedAt");
        forEachEmployee(firstPage, loadPage, e -> csv.writeRow(e.id(), e.firstName(), e.lastName(), e.email(),
                e.department(), e.jobTitle(), e.salary(), e.hireDate(), e.status().name(), e.createdAt(),
                e.updatedAt()));
        writer.flush();
    }

    void writeJson(List<EmployeeResponse> firstPage, IntFunction<List<EmployeeResponse>> loadPage,
                   OutputStream out) {
        try (JsonGenerator json = jsonMapper.createGenerator(out)) {
            json.writeStartArray();
            forEachEmployee(firstPage, loadPage, employee -> jsonMapper.writeValue(json, employee));
            json.writeEndArray();
        }
    }

    // Stops after a short page (no more rows) or at the 10,000-row cap.
    private static void forEachEmployee(List<EmployeeResponse> firstPage,
                                        IntFunction<List<EmployeeResponse>> loadPage,
                                        ThrowingConsumer<EmployeeResponse> action) {
        List<EmployeeResponse> page = firstPage;
        for (int next = 1; ; next++) {
            page.forEach(action);
            if (page.size() < PAGE_SIZE || next == MAX_PAGES) {
                return;
            }
            page = loadPage.apply(next);
        }
    }
}
