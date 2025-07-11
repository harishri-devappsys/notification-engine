package com.valura.notification.repository;

import com.valura.notification.model.NotificationFrequency;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotificationFrequencyRepository extends MongoRepository<NotificationFrequency, String> {
    NotificationFrequency findByUserIdAndChannelTypeAndDate(int userId, String channelType, LocalDate date);
    List<NotificationFrequency> findAllByUserIdAndDate(int userId, LocalDate date);
}