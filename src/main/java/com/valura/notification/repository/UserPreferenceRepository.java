package com.valura.notification.repository;

import com.valura.notification.model.UserPreference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // Changed from List to Optional for single preference lookup

@Repository
public interface UserPreferenceRepository extends MongoRepository<UserPreference, String> {
    Optional<UserPreference> findByRecipientId(String recipientId); // Changed from findAllByUserId to findByRecipientId
    void deleteByRecipientId(String recipientId); // Changed from deleteByUserId to deleteByRecipientId
}