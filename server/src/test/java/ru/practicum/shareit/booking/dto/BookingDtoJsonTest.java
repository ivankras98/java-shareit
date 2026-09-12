package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Test
    void serialize_shouldWriteDatesInIsoFormat() throws Exception {
        BookingDto dto = new BookingDto(1L,
                LocalDateTime.of(2026, 9, 15, 10, 0, 0),
                LocalDateTime.of(2026, 9, 16, 10, 0, 0));

        var result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2026-09-15T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2026-09-16T10:00:00");
        assertThat(result).extractingJsonPathNumberValue("$.itemId").isEqualTo(1);
    }

    @Test
    void deserialize_shouldParseDatesCorrectly() throws Exception {
        String content = """
                {
                  "itemId": 1,
                  "start": "2026-09-15T10:00:00",
                  "end": "2026-09-16T10:00:00"
                }
                """;

        BookingDto result = json.parse(content).getObject();

        assertThat(result.getItemId()).isEqualTo(1L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2026, 9, 15, 10, 0, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2026, 9, 16, 10, 0, 0));
    }
}