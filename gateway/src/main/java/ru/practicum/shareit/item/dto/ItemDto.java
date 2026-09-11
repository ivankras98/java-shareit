package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;

    @NotBlank(message = "не может быть пустым")
    private String name;

    @NotBlank(message = "не может быть пустым")
    private String description;

    @NotNull(message = "не может быть пустым")
    private Boolean available;

    private Long requestId;
}