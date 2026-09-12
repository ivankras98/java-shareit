package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceImplIntegrationTest {

    @Autowired
    private ItemRequestService itemRequestService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Test
    void create_shouldSaveRequestWithEmptyItemsList() {
        User requestor = createUser("Anna", "anna1@example.com");

        ItemRequestResponseDto result = itemRequestService.create(requestor.getId(),
                new ItemRequestDto("Нужна дрель"));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void create_withUnknownUser_shouldThrowNotFound() {
        assertThrows(NotFoundException.class,
                () -> itemRequestService.create(999L, new ItemRequestDto("Нужна дрель")));
    }

    @Test
    void getOwn_shouldIncludeAnsweringItems() {
        User requestor = createUser("Anna", "anna2@example.com");
        User responder = createUser("Boris", "boris2@example.com");
        ItemRequestResponseDto request = itemRequestService.create(requestor.getId(),
                new ItemRequestDto("Нужна дрель"));

        Item item = new Item();
        item.setName("Дрель");
        item.setDescription("Ударная");
        item.setAvailable(true);
        item.setOwner(responder);
        item.setRequestId(request.getId());
        itemRepository.save(item);

        List<ItemRequestResponseDto> own = itemRequestService.getOwn(requestor.getId());

        assertThat(own).hasSize(1);
        assertThat(own.get(0).getItems()).hasSize(1);
        assertThat(own.get(0).getItems().get(0).getOwnerId()).isEqualTo(responder.getId());
    }

    @Test
    void getAll_shouldNotIncludeOwnRequests() {
        User requestor = createUser("Anna", "anna3@example.com");
        User other = createUser("Boris", "boris3@example.com");
        itemRequestService.create(requestor.getId(), new ItemRequestDto("Нужна дрель"));

        List<ItemRequestResponseDto> allForOther = itemRequestService.getAll(other.getId());
        List<ItemRequestResponseDto> allForRequestor = itemRequestService.getAll(requestor.getId());

        assertThat(allForOther).isNotEmpty();
        assertThat(allForRequestor).noneMatch(r -> r.getDescription().equals("Нужна дрель"));
    }

    @Test
    void getById_withUnknownId_shouldThrowNotFound() {
        User user = createUser("Anna", "anna4@example.com");

        assertThrows(NotFoundException.class, () -> itemRequestService.getById(user.getId(), 999L));
    }
}