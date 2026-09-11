package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {

    @NotNull(message = "не может быть пустым")
    private Long itemId;

    @NotNull(message = "не может быть пустым")
    @FutureOrPresent(message = "дата начала должна быть в настоящем или будущем")
    private LocalDateTime start;

    @NotNull(message = "не может быть пустым")
    @Future(message = "дата окончания должна быть в будущем")
    private LocalDateTime end;
}