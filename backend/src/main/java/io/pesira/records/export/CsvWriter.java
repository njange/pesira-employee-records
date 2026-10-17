package io.pesira.records.export;

import java.io.IOException;
import java.io.Writer;

// Writes RFC 4180 rows. Text cells that a spreadsheet would run as a formula get a leading
// apostrophe, which blocks CSV formula injection.
class CsvWriter {

    private final Writer out;

    CsvWriter(Writer out) {
        this.out = out;
    }

    void writeRow(Object... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(format(cells[i]));
        }
        out.write("\r\n");
    }

    static String format(Object cell) {
        if (cell == null) {
            return "";
        }
        String value = cell.toString();
        if (cell instanceof String && startsLikeFormula(value)) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static boolean startsLikeFormula(String value) {
        return !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0;
    }
}
