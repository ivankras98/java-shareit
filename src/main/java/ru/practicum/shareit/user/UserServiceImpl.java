package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        if (userRepository.existsByEmailIgnoreCase(userDto.getEmail())) {
            throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }
        User user = UserMapper.toUser(userDto);
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto update(Long userId, UserDto userDto) {
        User user = getUserOrThrow(userId);

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(userDto.getEmail(), userId)) {
                throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
            }
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            user.setName(userDto.getName());
        }
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        getUserOrThrow(userId);
        userRepository.deleteById(userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }
}