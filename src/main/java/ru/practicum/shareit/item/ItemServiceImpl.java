package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = getUserOrThrow(userId);
        Item item = ItemMapper.toItem(itemDto, owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        getUserOrThrow(userId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не является владельцем вещи " + itemId);
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
        ItemDto dto = ItemMapper.toItemDto(item);
        dto.setComments(commentRepository.findByItem_Id(itemId).stream()
                .map(CommentMapper::toDto)
                .toList());
        if (item.getOwner().getId().equals(userId)) {
            fillBookingDates(dto, itemId);
        }
        return dto;
    }

    @Override
    public List<ItemDto> getAllByOwner(Long userId) {
        getUserOrThrow(userId);
        List<Item> items = itemRepository.findByOwner_Id(userId);
        List<Long> itemIds = items.stream().map(Item::getId).toList();

        // один запрос на все комментарии вместо запроса в цикле по каждой вещи
        Map<Long, List<Comment>> commentsByItemId = commentRepository.findByItem_IdIn(itemIds).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId()));

        // один запрос на все подтверждённые бронирования вместо двух запросов на каждую вещь
        Map<Long, List<Booking>> bookingsByItemId = bookingRepository
                .findByItem_IdInAndStatusOrderByStartAsc(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));

        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ItemDto dto = ItemMapper.toItemDto(item);
                    dto.setComments(commentsByItemId.getOrDefault(item.getId(), List.of()).stream()
                            .map(CommentMapper::toDto)
                            .toList());

                    List<Booking> itemBookings = bookingsByItemId.getOrDefault(item.getId(), List.of());
                    itemBookings.stream()
                            .filter(b -> b.getStart().isBefore(now))
                            .max(Comparator.comparing(Booking::getStart))
                            .ifPresent(b -> dto.setLastBooking(BookingMapper.toShortDto(b)));
                    itemBookings.stream()
                            .filter(b -> b.getStart().isAfter(now))
                            .min(Comparator.comparing(Booking::getStart))
                            .ifPresent(b -> dto.setNextBooking(BookingMapper.toShortDto(b)));

                    return dto;
                })
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = getUserOrThrow(userId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        boolean tookItem = bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                userId, itemId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!tookItem) {
            throw new ValidationException(
                    "Пользователь с id " + userId + " не брал вещь с id " + itemId
                            + " в аренду, либо аренда ещё не завершена");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        return CommentMapper.toDto(commentRepository.save(comment));
    }

    private void fillBookingDates(ItemDto dto, Long itemId) {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.findFirstByItem_IdAndStatusAndStartBeforeOrderByStartDesc(
                        itemId, BookingStatus.APPROVED, now)
                .ifPresent(b -> dto.setLastBooking(BookingMapper.toShortDto(b)));
        bookingRepository.findFirstByItem_IdAndStatusAndStartAfterOrderByStartAsc(
                        itemId, BookingStatus.APPROVED, now)
                .ifPresent(b -> dto.setNextBooking(BookingMapper.toShortDto(b)));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }
}