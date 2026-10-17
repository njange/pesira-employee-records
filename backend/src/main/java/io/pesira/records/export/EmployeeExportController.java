package io.pesira.records.export;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.IntFunction;

import io.pesira.records.employee.EmployeeResponse;
import io.pesira.records.employee.EmployeeService;
import io.pesira.records.employee.EmployeeStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeExportController {

    private final EmployeeService employees;
    private final EmployeeExporter exporter;

    public EmployeeExportController(EmployeeService employees, EmployeeExporter exporter) {
        this.employees = employees;
        this.exporter = exporter;
    }

    @GetMapping("/api/employees/export")
    public void export(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String department,
                       @RequestParam(required = false) EmployeeStatus status,
                       @RequestParam(required = false) String sort,
                       @RequestParam String format,
                       HttpServletResponse response) throws IOException {
        ExportFormat exportFormat = ExportFormat.parse(format);
        IntFunction<List<EmployeeResponse>> loadPage = page ->
                employees.list(search, department, status, sort, page, EmployeeExporter.PAGE_SIZE).content();
        // Loaded before any header is set, so a bad sort still gets a normal 400 response.
        List<EmployeeResponse> firstPage = loadPage.apply(0);

        response.setContentType(exportFormat.contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("employees-" + LocalDate.now() + "." + exportFormat.extension)
                .build()
                .toString());
        switch (exportFormat) {
            case CSV -> exporter.writeCsv(firstPage, loadPage, response.getOutputStream());
            case JSON -> exporter.writeJson(firstPage, loadPage, response.getOutputStream());
        }
    }
}
