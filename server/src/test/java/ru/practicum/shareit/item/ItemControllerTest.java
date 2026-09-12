package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ItemService itemService;

    @Test
    void create_shouldReturnCreatedItem() throws Exception {
        ItemDto request = ItemDto.builder().name("Дрель").description("Ударная").available(true).build();
        ItemDto response = ItemDto.builder().id(1L).name("Дрель").description("Ударная").available(true).build();
        when(itemService.create(anyLong(), any())).thenReturn(response);

        mvc.perform(post("/items")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Дрель"));
    }

    @Test
    void create_withBlankName_shouldReturnBadRequest() throws Exception {
        ItemDto request = ItemDto.builder().name("").description("Ударная").available(true).build();

        mvc.perform(post("/items")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_byNonOwner_shouldReturnNotFound() throws Exception {
        ItemDto request = ItemDto.builder().name("Новое имя").build();
        when(itemService.update(anyLong(), anyLong(), any()))
                .thenThrow(new NotFoundException("не владелец"));

        mvc.perform(patch("/items/1")
                        .header(USER_ID_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturnItem() throws Exception {
        ItemDto response = ItemDto.builder().id(1L).name("Дрель").description("Ударная").available(true).build();
        when(itemService.getById(1L, 1L)).thenReturn(response);

        mvc.perform(get("/items/1").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Дрель"));
    }

    @Test
    void getAllByOwner_shouldReturnList() throws Exception {
        when(itemService.getAllByOwner(1L)).thenReturn(List.of(
                ItemDto.builder().id(1L).name("Дрель").description("Ударная").available(true).build()));

        mvc.perform(get("/items").header(USER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void search_shouldReturnMatchingItems() throws Exception {
        when(itemService.search("дрель")).thenReturn(List.of(
                ItemDto.builder().id(1L).name("Дрель").description("Ударная").available(true).build()));

        mvc.perform(get("/items/search").param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Дрель"));
    }

    @Test
    void addComment_withoutBooking_shouldReturnBadRequest() throws Exception {
        CommentDto request = CommentDto.builder().text("Отлично").build();
        when(itemService.addComment(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("не брал в аренду"));

        mvc.perform(post("/items/1/comment")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addComment_withBlankText_shouldReturnBadRequest() throws Exception {
        CommentDto request = CommentDto.builder().text("").build();

        mvc.perform(post("/items/1/comment")
                        .header(USER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}