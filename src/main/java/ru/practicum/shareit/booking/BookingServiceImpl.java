package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingResponseDto create(Long userId, BookingDto bookingDto) {
        User booker = getUserOrThrow(userId);
        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь с id " + bookingDto.getItemId() + " не найдена"));

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new ValidationException("Вещь с id " + item.getId() + " недоступна для бронирования");
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может забронировать собственную вещь");
        }
        if (!bookingDto.getStart().isBefore(bookingDto.getEnd())) {
            throw new ValidationException("Дата начала должна быть раньше даты окончания");
        }

        Booking booking = new Booking();
        booking.setStart(bookingDto.getStart());
        booking.setEnd(bookingDto.getEnd());
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        return BookingMapper.toResponseDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponseDto approve(Long userId, Long bookingId, boolean approved) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Пользователь с id " + userId + " не является владельцем вещи");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Статус бронирования уже определён: " + booking.getStatus());
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toResponseDto(bookingRepository.save(booking));
    }

    @Override
    public BookingResponseDto getById(Long userId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Пользователь с id " + userId + " не имеет доступа к этому бронированию");
        }
        return BookingMapper.toResponseDto(booking);
    }

    @Override
    public List<BookingResponseDto> getAllByBooker(Long userId, BookingState state) {
        getUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case CURRENT -> bookingRepository.findByBooker_IdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case PAST -> bookingRepository.findByBooker_IdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookingRepository.findByBooker_IdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
            case ALL -> bookingRepository.findByBooker_IdOrderByStartDesc(userId);
        };
        return bookings.stream().map(BookingMapper::toResponseDto).toList();
    }

    @Override
    public List<BookingResponseDto> getAllByOwner(Long userId, BookingState state) {
        getUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case CURRENT -> bookingRepository.findByItem_Owner_IdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookingRepository.findByItem_Owner_IdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findByItem_Owner_IdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
            case ALL -> bookingRepository.findByItem_Owner_IdOrderByStartDesc(userId);
        };
        return bookings.stream().map(BookingMapper::toResponseDto).toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id " + bookingId + " не найдено"));
    }
}