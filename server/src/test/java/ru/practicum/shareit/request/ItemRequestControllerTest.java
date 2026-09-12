package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ItemRequestService itemRequestService;

    @Test
    void create_shouldReturnCreatedRequest() throws Exception {
        ItemRequestDto request = new ItemRequestDto("Нужна дрель");
        ItemRequestResponseDto response = ItemRequestResponseDto.builder()
                .id(1L).description("Нужна дрель").created(LocalDateTime.now()).items(List.of()).build();
        when(itemRequestService.create(anyLong(), any())).thenReturn(response);

        mvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Нужна дрель"));
    }

    @Test
    void create_withBlankDescription_shouldReturnBadRequest() throws Exception {
        ItemRequestDto request = new ItemRequestDto("");

        mvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOwn_shouldReturnList() throws Exception {
        when(itemRequestService.getOwn(1L)).thenReturn(List.of(
                ItemRequestResponseDto.builder().id(1L).description("Нужна дрель").items(List.of()).build()));

        mvc.perform(get("/requests").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_shouldReturnList() throws Exception {
        when(itemRequestService.getAll(1L)).thenReturn(List.of(
                ItemRequestResponseDto.builder().id(2L).description("Нужна пила").items(List.of()).build()));

        mvc.perform(get("/requests/all").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Нужна пила"));
    }

    @Test
    void getById_shouldReturnRequest() throws Exception {
        when(itemRequestService.getById(1L, 5L)).thenReturn(
                ItemRequestResponseDto.builder().id(5L).description("Нужна дрель").items(List.of()).build());

        mvc.perform(get("/requests/5").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getById_withUnknownId_shouldReturnNotFound() throws Exception {
        when(itemRequestService.getById(anyLong(), anyLong())).thenThrow(new NotFoundException("не найден"));

        mvc.perform(get("/requests/999").header(USER_ID_HEADER, 1L))
                .andExpect(status().isNotFound());
    }
}