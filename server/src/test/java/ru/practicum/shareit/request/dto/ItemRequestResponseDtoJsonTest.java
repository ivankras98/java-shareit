package ru.practicum.shareit.request.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestResponseDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestResponseDto> json;

    @Test
    void serialize_shouldIncludeNestedAnsweringItems() throws Exception {
        ItemRequestResponseDto dto = ItemRequestResponseDto.builder()
                .id(1L)
                .description("Нужна дрель")
                .created(LocalDateTime.of(2026, 9, 10, 12, 0, 0))
                .items(List.of(
                        ItemRequestResponseDto.ItemAnswerDto.builder()
                                .id(10L).name("Дрель").ownerId(5L).build()
                ))
                .build();

        var result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Нужна дрель");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].id").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.items[0].name").isEqualTo("Дрель");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(5);
    }

    @Test
    void serialize_withEmptyItemsList_shouldWriteEmptyArray() throws Exception {
        ItemRequestResponseDto dto = ItemRequestResponseDto.builder()
                .id(1L).description("Нужна дрель")
                .created(LocalDateTime.now())
                .items(List.of())
                .build();

        var result = json.write(dto);

        assertThat(result).extractingJsonPathArrayValue("$.items").isEmpty();
    }
}