package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = getUserOrThrow(userId);
        Item item = ItemMapper.toItem(itemDto, owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }
    @Override
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = getUserOrThrow(userId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        boolean tookItem = bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                userId, itemId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!tookItem) {
            throw new ValidationException(
                    "Пользователь с id " + userId + " не брал вещь с id " + itemId + " в аренду, либо аренда ещё не завершена");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
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
        return itemRepository.findByOwner_Id(userId).stream()
                .map(item -> {
                    ItemDto dto = ItemMapper.toItemDto(item);
                    dto.setComments(commentRepository.findByItem_Id(item.getId()).stream()
                            .map(CommentMapper::toDto)
                            .toList());
                    fillBookingDates(dto, item.getId());
                    return dto;
                })
                .toList();
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

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }
}