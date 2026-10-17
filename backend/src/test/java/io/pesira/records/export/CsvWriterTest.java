package io.pesira.records.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CsvWriterTest {

    @Test
    void leavesPlainValuesAlone() {
        assertThat(CsvWriter.format("Amina")).isEqualTo("Amina");
        assertThat(CsvWriter.format(null)).isEmpty();
    }

    @Test
    void quotesFieldsWithCommaQuoteOrNewline() {
        assertThat(CsvWriter.format("Otieno, Amina")).isEqualTo("\"Otieno, Amina\"");
        assertThat(CsvWriter.format("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvWriter.format("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }

    @Test
    void prefixesTextThatLooksLikeAFormula() {
        assertThat(CsvWriter.format("=SUM(A1:A9)")).isEqualTo("'=SUM(A1:A9)");
        assertThat(CsvWriter.format("+254700")).isEqualTo("'+254700");
        assertThat(CsvWriter.format("-1")).isEqualTo("'-1");
        assertThat(CsvWriter.format("@cmd")).isEqualTo("'@cmd");
        assertThat(CsvWriter.format("\tx")).isEqualTo("'\tx");
        assertThat(CsvWriter.format("\rx")).isEqualTo("\"'\rx\"");
    }

    @Test
    void prefixesAndQuotesTogether() {
        assertThat(CsvWriter.format("=HYPERLINK(\"http://x\",\"y\")"))
                .isEqualTo("\"'=HYPERLINK(\"\"http://x\"\",\"\"y\"\")\"");
    }

    @Test
    void leavesNonTextCellsUnprefixed() {
        assertThat(CsvWriter.format(new BigDecimal("-5.00"))).isEqualTo("-5.00");
    }

    @Test
    void writesCommaSeparatedRowsEndingInCrlf() throws IOException {
        StringWriter out = new StringWriter();
        CsvWriter csv = new CsvWriter(out);

        csv.writeRow("id", "name");
        csv.writeRow(1L, "Otieno, Amina");

        assertThat(out.toString()).isEqualTo("id,name\r\n1,\"Otieno, Amina\"\r\n");
    }
}
