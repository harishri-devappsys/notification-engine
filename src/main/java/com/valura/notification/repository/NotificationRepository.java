package com.valura.notification.repository;

import com.valura.notification.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserId(int userId);

    @Query("{ 'userId': ?0, 'contentHash': ?1, 'createdAt': { $gte: ?2 } }")
    List<Notification> findDuplicates(int userId, String contentHash, Instant since);
}