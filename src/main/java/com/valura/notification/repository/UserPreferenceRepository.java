package com.valura.notification.repository;

import com.valura.notification.model.UserPreference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserPreferenceRepository extends MongoRepository<UserPreference, String> {
    List<UserPreference> findAllByUserId(int userId);
    void deleteByUserId(int userId);
}