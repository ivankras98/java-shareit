package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void create_shouldSaveUserAndReturnDtoWithId() {
        UserDto userDto = new UserDto(null, "Anna", "anna@example.com");

        UserDto saved = userService.create(userDto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Anna");
        assertThat(saved.getEmail()).isEqualTo("anna@example.com");
    }

    @Test
    void create_withDuplicateEmail_shouldThrowConflict() {
        userService.create(new UserDto(null, "Anna", "duplicate@example.com"));

        UserDto duplicate = new UserDto(null, "Boris", "duplicate@example.com");

        assertThrows(ConflictException.class, () -> userService.create(duplicate));
    }

    @Test
    void update_shouldChangeOnlyProvidedFields() {
        UserDto created = userService.create(new UserDto(null, "Anna", "anna2@example.com"));

        UserDto update = new UserDto(null, "Anna Updated", null);
        UserDto updated = userService.update(created.getId(), update);

        assertThat(updated.getName()).isEqualTo("Anna Updated");
        assertThat(updated.getEmail()).isEqualTo("anna2@example.com");
    }

    @Test
    void getById_withUnknownId_shouldThrowNotFound() {
        assertThrows(NotFoundException.class, () -> userService.getById(999L));
    }

    @Test
    void getAll_shouldReturnAllCreatedUsers() {
        userService.create(new UserDto(null, "Anna", "anna3@example.com"));
        userService.create(new UserDto(null, "Boris", "boris3@example.com"));

        List<UserDto> all = userService.getAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void delete_shouldRemoveUser() {
        UserDto created = userService.create(new UserDto(null, "Anna", "anna4@example.com"));

        userService.delete(created.getId());

        assertThrows(NotFoundException.class, () -> userService.getById(created.getId()));
    }
}