package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private BookingService bookingService;

    @Test
    void create_shouldReturnCreatedBooking() throws Exception {
        BookingDto request = new BookingDto(1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        BookingResponseDto response = BookingResponseDto.builder()
                .id(1L).status(BookingStatus.WAITING).build();
        when(bookingService.create(anyLong(), any())).thenReturn(response);

        mvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void create_withPastStartDate_shouldReturnBadRequest() throws Exception {
        BookingDto request = new BookingDto(1L, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2));

        mvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_forUnavailableItem_shouldReturnBadRequest() throws Exception {
        BookingDto request = new BookingDto(1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        when(bookingService.create(anyLong(), any())).thenThrow(new ValidationException("недоступна"));

        mvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approve_byNonOwner_shouldReturnForbidden() throws Exception {
        when(bookingService.approve(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(new ForbiddenException("не владелец"));

        mvc.perform(patch("/bookings/1")
                        .header(USER_ID_HEADER, 2L)
                        .param("approved", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_shouldReturnApprovedBooking() throws Exception {
        BookingResponseDto response = BookingResponseDto.builder()
                .id(1L).status(BookingStatus.APPROVED).build();
        when(bookingService.approve(1L, 1L, true)).thenReturn(response);

        mvc.perform(patch("/bookings/1")
                        .header(USER_ID_HEADER, 1L)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getById_withoutAccess_shouldReturnNotFound() throws Exception {
        when(bookingService.getById(anyLong(), anyLong())).thenThrow(new NotFoundException("нет доступа"));

        mvc.perform(get("/bookings/1").header(USER_ID_HEADER, 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllByBooker_shouldReturnList() throws Exception {
        when(bookingService.getAllByBooker(eq(1L), eq(BookingState.ALL)))
                .thenReturn(List.of(BookingResponseDto.builder().id(1L).status(BookingStatus.WAITING).build()));

        mvc.perform(get("/bookings").header(USER_ID_HEADER, 1L).param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllByOwner_shouldReturnList() throws Exception {
        when(bookingService.getAllByOwner(eq(1L), eq(BookingState.WAITING)))
                .thenReturn(List.of(BookingResponseDto.builder().id(1L).status(BookingStatus.WAITING).build()));

        mvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 1L).param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}