package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
@Slf4j
public class ItemRequestController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final ItemRequestClient itemRequestClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(USER_ID_HEADER) Long userId,
                                         @Valid @RequestBody ItemRequestDto dto) {
        log.info("Creating item request {}, userId={}", dto, userId);
        return itemRequestClient.create(userId, dto);
    }

    @GetMapping
    public ResponseEntity<Object> getOwn(@RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Get own item requests, userId={}", userId);
        return itemRequestClient.getOwn(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getAll(@RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Get all item requests, userId={}", userId);
        return itemRequestClient.getAll(userId);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getById(@RequestHeader(USER_ID_HEADER) Long userId,
                                          @PathVariable Long requestId) {
        log.info("Get item request {}, userId={}", requestId, userId);
        return itemRequestClient.getById(userId, requestId);
    }
}