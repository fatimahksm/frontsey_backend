package com.dbwb.platform.menu;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuImportCsvParserTest {

    @Test
    void parsesSimpleRowsByHeaderName() {
        String csv = """
                Category,Name,Price
                Coffee,Espresso,3.00
                Bakery,Croissant,3.50
                """;

        List<MenuImportCsvParser.RawRow> rows = parse(csv);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("category")).isEqualTo("Coffee");
        assertThat(rows.get(0).get("name")).isEqualTo("Espresso");
        assertThat(rows.get(0).get("price")).isEqualTo("3.00");
        assertThat(rows.get(1).rowNumber()).isEqualTo(2);
    }

    @Test
    void headerMatchingIsCaseInsensitive() {
        String csv = """
                CATEGORY,name,PrIcE
                Coffee,Espresso,3.00
                """;

        List<MenuImportCsvParser.RawRow> rows = parse(csv);

        assertThat(rows.get(0).get("category")).isEqualTo("Coffee");
        assertThat(rows.get(0).get("name")).isEqualTo("Espresso");
    }

    @Test
    void blankCellsAreTreatedAsAbsent() {
        String csv = """
                Category,Name,Description
                Coffee,Espresso,
                """;

        List<MenuImportCsvParser.RawRow> rows = parse(csv);

        assertThat(rows.get(0).get("description")).isNull();
    }

    @Test
    void handlesQuotedFieldsWithEmbeddedCommas() {
        String csv = """
                Category,Name,Ingredients
                Coffee,Cappuccino,"Espresso, Milk, Foam"
                """;

        List<MenuImportCsvParser.RawRow> rows = parse(csv);

        assertThat(rows.get(0).get("ingredients")).isEqualTo("Espresso, Milk, Foam");
    }

    @Test
    void handlesEscapedQuotesInsideQuotedFields() {
        String csv = """
                Category,Name,Description
                Bakery,Croissant,"The ""Original"" recipe"
                """;

        List<MenuImportCsvParser.RawRow> rows = parse(csv);

        assertThat(rows.get(0).get("description")).isEqualTo("The \"Original\" recipe");
    }

    @Test
    void blankLinesBetweenRowsAreSkipped() {
        String csv = "Category,Name,Price\nCoffee,Espresso,3.00\n\nBakery,Croissant,3.50\n";

        List<MenuImportCsvParser.RawRow> rows = parse(csv);

        assertThat(rows).hasSize(2);
        // Row numbers only count non-blank data rows - the blank line contributes nothing to count.
        assertThat(rows.get(1).get("name")).isEqualTo("Croissant");
    }

    @Test
    void emptyFileIsRejected() {
        assertThatThrownBy(() -> parse(""))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("empty");
    }

    private List<MenuImportCsvParser.RawRow> parse(String csv) {
        return MenuImportCsvParser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }
}
