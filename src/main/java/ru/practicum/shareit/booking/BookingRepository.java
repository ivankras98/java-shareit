package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBooker_IdOrderByStartDesc(Long bookerId);

    List<Booking> findByBooker_IdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    List<Booking> findByBooker_IdAndEndBeforeOrderByStartDesc(Long bookerId, LocalDateTime now);

    List<Booking> findByBooker_IdAndStartAfterOrderByStartDesc(Long bookerId, LocalDateTime now);

    List<Booking> findByBooker_IdAndStartBeforeAndEndAfterOrderByStartDesc(
            Long bookerId, LocalDateTime nowStart, LocalDateTime nowEnd);

    List<Booking> findByItem_Owner_IdOrderByStartDesc(Long ownerId);

    List<Booking> findByItem_Owner_IdAndStatusOrderByStartDesc(Long ownerId, BookingStatus status);

    List<Booking> findByItem_Owner_IdAndEndBeforeOrderByStartDesc(Long ownerId, LocalDateTime now);

    List<Booking> findByItem_Owner_IdAndStartAfterOrderByStartDesc(Long ownerId, LocalDateTime now);

    List<Booking> findByItem_Owner_IdAndStartBeforeAndEndAfterOrderByStartDesc(
            Long ownerId, LocalDateTime nowStart, LocalDateTime nowEnd);

    Optional<Booking> findFirstByItem_IdAndStatusAndStartBeforeOrderByStartDesc(
            Long itemId, BookingStatus status, LocalDateTime now);

    Optional<Booking> findFirstByItem_IdAndStatusAndStartAfterOrderByStartAsc(
            Long itemId, BookingStatus status, LocalDateTime now);

    boolean existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
            Long bookerId, Long itemId, BookingStatus status, LocalDateTime now);
}