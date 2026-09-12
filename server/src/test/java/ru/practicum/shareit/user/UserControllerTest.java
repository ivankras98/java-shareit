package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void create_shouldReturnCreatedUser() throws Exception {
        UserDto request = new UserDto(null, "Anna", "anna@example.com");
        UserDto response = new UserDto(1L, "Anna", "anna@example.com");
        when(userService.create(any())).thenReturn(response);

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.email").value("anna@example.com"));
    }

    @Test
    void create_withBlankName_shouldReturnBadRequest() throws Exception {
        UserDto request = new UserDto(null, "", "anna@example.com");

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withDuplicateEmail_shouldReturnConflict() throws Exception {
        UserDto request = new UserDto(null, "Anna", "anna@example.com");
        when(userService.create(any())).thenThrow(new ConflictException("уже существует"));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_shouldReturnUser() throws Exception {
        when(userService.getById(1L)).thenReturn(new UserDto(1L, "Anna", "anna@example.com"));

        mvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Anna"));
    }

    @Test
    void getById_withUnknownId_shouldReturnNotFound() throws Exception {
        when(userService.getById(999L)).thenThrow(new NotFoundException("не найден"));

        mvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_shouldReturnListOfUsers() throws Exception {
        when(userService.getAll()).thenReturn(List.of(
                new UserDto(1L, "Anna", "anna@example.com"),
                new UserDto(2L, "Boris", "boris@example.com")
        ));

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void delete_shouldCallServiceAndReturnOk() throws Exception {
        mvc.perform(delete("/users/1"))
                .andExpect(status().isOk());

        verify(userService).delete(1L);
    }
}