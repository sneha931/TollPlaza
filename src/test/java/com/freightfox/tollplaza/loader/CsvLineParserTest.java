package com.freightfox.tollplaza.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class CsvLineParserTest {

	@Test
	void splitsPlainFields() {
		assertThat(CsvLineParser.parse("74.99,23.41,Dhamnod Toll Plaza,Madhya Pradesh"))
				.containsExactly("74.99", "23.41", "Dhamnod Toll Plaza", "Madhya Pradesh");
	}

	@Test
	void keepsCommasInsideQuotedField() {
		assertThat(CsvLineParser.parse("70.34917,22.550447,\"Soyal, Dhrol Toll Plaza\",Gujarat"))
				.containsExactly("70.34917", "22.550447", "Soyal, Dhrol Toll Plaza", "Gujarat");
	}

	@Test
	void unescapesDoubledQuotes() {
		assertThat(CsvLineParser.parse("1,2,\"The \"\"Big\"\" Plaza\",X"))
				.containsExactly("1", "2", "The \"Big\" Plaza", "X");
	}

	@Test
	void keepsEmptyTrailingField() {
		assertThat(CsvLineParser.parse("1,2,Name,")).containsExactly("1", "2", "Name", "");
	}

	@Test
	void csvRowMapsLongitudeFirstColumnOrder() {
		TollPlazaCsvLoader.CsvRow row = TollPlazaCsvLoader.CsvRow.from(List.of("77.5", "12.9", "X Plaza", "Karnataka"));
		assertThat(row.latitude()).isEqualTo(12.9);
		assertThat(row.longitude()).isEqualTo(77.5);
	}

	@Test
	void csvRowRejectsNonNumericCoordinates() {
		assertThatThrownBy(() -> TollPlazaCsvLoader.CsvRow.from(List.of("abc", "12.9", "X", "S")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void csvRowRejectsBlankName() {
		assertThatThrownBy(() -> TollPlazaCsvLoader.CsvRow.from(List.of("77", "12", " ", "S")))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
