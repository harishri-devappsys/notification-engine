package com.valura.notification.repository;

import com.valura.notification.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientId(String recipientId);

    @Query("{ 'recipientId': ?0, 'channelType': ?1, 'contentHash': ?2, 'createdAt': { $gte: ?3 } }")
    List<Notification> findDuplicates(String recipientId, String channelType, String contentHash, Instant since);
}