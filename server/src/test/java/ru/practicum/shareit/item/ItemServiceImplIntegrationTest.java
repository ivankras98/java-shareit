package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemService itemService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Test
    void create_shouldSaveItemWithOwner() {
        User owner = createUser("Owner", "owner1@example.com");
        ItemDto itemDto = ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).build();

        ItemDto saved = itemService.create(owner.getId(), itemDto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Дрель");
    }

    @Test
    void create_withUnknownOwner_shouldThrowNotFound() {
        ItemDto itemDto = ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).build();

        assertThrows(NotFoundException.class, () -> itemService.create(999L, itemDto));
    }

    @Test
    void update_byNonOwner_shouldThrowNotFound() {
        User owner = createUser("Owner", "owner2@example.com");
        User stranger = createUser("Stranger", "stranger2@example.com");
        ItemDto created = itemService.create(owner.getId(),
                ItemDto.builder().name("Дрель").description("Ударная").available(true).build());

        ItemDto update = ItemDto.builder().name("Новое имя").build();

        assertThrows(NotFoundException.class, () -> itemService.update(stranger.getId(), created.getId(), update));
    }

    @Test
    void search_shouldReturnOnlyAvailableMatchingItems() {
        User owner = createUser("Owner", "owner3@example.com");
        itemService.create(owner.getId(),
                ItemDto.builder().name("Дрель Bosch").description("Мощная").available(true).build());
        itemService.create(owner.getId(),
                ItemDto.builder().name("Пила").description("Дрель не подходит").available(false).build());

        List<ItemDto> found = itemService.search("дрель");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Дрель Bosch");
    }

    @Test
    void search_withBlankText_shouldReturnEmptyList() {
        assertThat(itemService.search("")).isEmpty();
    }

    @Test
    void getById_byOwner_shouldIncludeBookingDates() {
        User owner = createUser("Owner", "owner4@example.com");
        User booker = createUser("Booker", "booker4@example.com");
        ItemDto item = itemService.create(owner.getId(),
                ItemDto.builder().name("Дрель").description("Ударная").available(true).build());

        Booking pastBooking = new Booking();
        pastBooking.setStart(LocalDateTime.now().minusDays(5));
        pastBooking.setEnd(LocalDateTime.now().minusDays(3));
        pastBooking.setStatus(BookingStatus.APPROVED);
        pastBooking.setBooker(booker);
        pastBooking.setItem(itemFromDto(item, owner));
        bookingRepository.save(pastBooking);

        ItemDto result = itemService.getById(owner.getId(), item.getId());

        assertThat(result.getLastBooking()).isNotNull();
        assertThat(result.getLastBooking().getBookerId()).isEqualTo(booker.getId());
    }

    @Test
    void addComment_withoutCompletedBooking_shouldThrowValidation() {
        User owner = createUser("Owner", "owner5@example.com");
        User stranger = createUser("Stranger", "stranger5@example.com");
        ItemDto item = itemService.create(owner.getId(),
                ItemDto.builder().name("Дрель").description("Ударная").available(true).build());

        CommentDto commentDto = CommentDto.builder().text("Отличная вещь").build();

        assertThrows(ValidationException.class,
                () -> itemService.addComment(stranger.getId(), item.getId(), commentDto));
    }

    @Test
    void addComment_afterCompletedBooking_shouldSaveComment() {
        User owner = createUser("Owner", "owner6@example.com");
        User booker = createUser("Booker", "booker6@example.com");
        ItemDto item = itemService.create(owner.getId(),
                ItemDto.builder().name("Дрель").description("Ударная").available(true).build());

        Booking pastBooking = new Booking();
        pastBooking.setStart(LocalDateTime.now().minusDays(5));
        pastBooking.setEnd(LocalDateTime.now().minusDays(3));
        pastBooking.setStatus(BookingStatus.APPROVED);
        pastBooking.setBooker(booker);
        pastBooking.setItem(itemFromDto(item, owner));
        bookingRepository.save(pastBooking);

        CommentDto commentDto = CommentDto.builder().text("Отличная вещь").build();
        CommentDto saved = itemService.addComment(booker.getId(), item.getId(), commentDto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuthorName()).isEqualTo("Booker");
    }

    private ru.practicum.shareit.item.model.Item itemFromDto(ItemDto dto, User owner) {
        ru.practicum.shareit.item.model.Item item = new ru.practicum.shareit.item.model.Item();
        item.setId(dto.getId());
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setAvailable(dto.getAvailable());
        item.setOwner(owner);
        return item;
    }
}