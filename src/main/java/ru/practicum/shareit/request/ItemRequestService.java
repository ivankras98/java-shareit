package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestResponseDto create(Long userId, ItemRequestDto dto);

    List<ItemRequestResponseDto> getOwn(Long userId);

    List<ItemRequestResponseDto> getAll(Long userId);

    ItemRequestResponseDto getById(Long userId, Long requestId);
}