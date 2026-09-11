package ru.practicum.shareit.request;

import lombok.experimental.UtilityClass;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.util.List;

@UtilityClass
public class ItemRequestMapper {

    public ItemRequestResponseDto toResponseDto(ItemRequest request, List<Item> answers) {
        return ItemRequestResponseDto.builder()
                .id(request.getId())
                .description(request.getDescription())
                .created(request.getCreated())
                .items(answers.stream()
                        .map(item -> ItemRequestResponseDto.ItemAnswerDto.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .ownerId(item.getOwner().getId())
                                .build())
                        .toList())
                .build();
    }
}