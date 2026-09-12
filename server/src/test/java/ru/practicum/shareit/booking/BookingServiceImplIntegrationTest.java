package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceImplIntegrationTest {

    @Autowired
    private BookingService bookingService;
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

    private Item createItem(User owner, boolean available) {
        Item item = new Item();
        item.setName("Дрель");
        item.setDescription("Ударная");
        item.setAvailable(available);
        item.setOwner(owner);
        return itemRepository.save(item);
    }

    @Test
    void create_shouldSaveBookingWithWaitingStatus() {
        User owner = createUser("Owner", "owner1@example.com");
        User booker = createUser("Booker", "booker1@example.com");
        Item item = createItem(owner, true);

        BookingDto dto = new BookingDto(item.getId(),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        BookingResponseDto result = bookingService.create(booker.getId(), dto);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void create_forUnavailableItem_shouldThrowValidation() {
        User owner = createUser("Owner", "owner2@example.com");
        User booker = createUser("Booker", "booker2@example.com");
        Item item = createItem(owner, false);

        BookingDto dto = new BookingDto(item.getId(),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        assertThrows(ValidationException.class, () -> bookingService.create(booker.getId(), dto));
    }

    @Test
    void create_byOwner_shouldThrowNotFound() {
        User owner = createUser("Owner", "owner3@example.com");
        Item item = createItem(owner, true);

        BookingDto dto = new BookingDto(item.getId(),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        assertThrows(NotFoundException.class, () -> bookingService.create(owner.getId(), dto));
    }

    @Test
    void approve_byNonOwner_shouldThrowForbidden() {
        User owner = createUser("Owner", "owner4@example.com");
        User booker = createUser("Booker", "booker4@example.com");
        User stranger = createUser("Stranger", "stranger4@example.com");
        Item item = createItem(owner, true);
        BookingResponseDto booking = bookingService.create(booker.getId(),
                new BookingDto(item.getId(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)));

        assertThrows(ru.practicum.shareit.exception.ForbiddenException.class,
                () -> bookingService.approve(stranger.getId(), booking.getId(), true));
    }

    @Test
    void approve_shouldChangeStatusToApproved() {
        User owner = createUser("Owner", "owner5@example.com");
        User booker = createUser("Booker", "booker5@example.com");
        Item item = createItem(owner, true);
        BookingResponseDto booking = bookingService.create(booker.getId(),
                new BookingDto(item.getId(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)));

        BookingResponseDto approved = bookingService.approve(owner.getId(), booking.getId(), true);

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approve_whenAlreadyDecided_shouldThrowValidation() {
        User owner = createUser("Owner", "owner6@example.com");
        User booker = createUser("Booker", "booker6@example.com");
        Item item = createItem(owner, true);
        BookingResponseDto booking = bookingService.create(booker.getId(),
                new BookingDto(item.getId(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)));
        bookingService.approve(owner.getId(), booking.getId(), true);

        assertThrows(ValidationException.class,
                () -> bookingService.approve(owner.getId(), booking.getId(), false));
    }

    @Test
    void getById_byStranger_shouldThrowNotFound() {
        User owner = createUser("Owner", "owner7@example.com");
        User booker = createUser("Booker", "booker7@example.com");
        User stranger = createUser("Stranger", "stranger7@example.com");
        Item item = createItem(owner, true);
        BookingResponseDto booking = bookingService.create(booker.getId(),
                new BookingDto(item.getId(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)));

        assertThrows(NotFoundException.class, () -> bookingService.getById(stranger.getId(), booking.getId()));
    }

    @Test
    void getAllByBooker_withWaitingState_shouldReturnOnlyWaiting() {
        User owner = createUser("Owner", "owner8@example.com");
        User booker = createUser("Booker", "booker8@example.com");
        Item item = createItem(owner, true);
        bookingService.create(booker.getId(),
                new BookingDto(item.getId(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)));

        List<BookingResponseDto> result = bookingService.getAllByBooker(booker.getId(), BookingState.WAITING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.WAITING);
    }
}