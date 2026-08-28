package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final BookingService bookingService;

    @PostMapping
    public BookingResponseDto create(@RequestHeader(USER_ID_HEADER) Long userId,
                                     @Valid @RequestBody BookingDto bookingDto) {
        return bookingService.create(userId, bookingDto);
    }

    @PatchMapping("/{bookingId}")
    public BookingResponseDto approve(@RequestHeader(USER_ID_HEADER) Long userId,
                                      @PathVariable Long bookingId,
                                      @RequestParam boolean approved) {
        return bookingService.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingResponseDto getById(@RequestHeader(USER_ID_HEADER) Long userId,
                                      @PathVariable Long bookingId) {
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping
    public List<BookingResponseDto> getAllByBooker(@RequestHeader(USER_ID_HEADER) Long userId,
                                                   @RequestParam(defaultValue = "ALL") BookingState state) {
        return bookingService.getAllByBooker(userId, state);
    }

    @GetMapping("/owner")
    public List<BookingResponseDto> getAllByOwner(@RequestHeader(USER_ID_HEADER) Long userId,
                                                  @RequestParam(defaultValue = "ALL") BookingState state) {
        return bookingService.getAllByOwner(userId, state);
    }
}