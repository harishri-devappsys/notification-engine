package com.valura.notification.controller; // Corrected package name

import com.valura.notification.model.NotificationChannel;
import com.valura.notification.model.UserPreference;
import com.valura.notification.repository.UserPreferenceRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/setup")
public class SetupController { // Class definition in Java

    private final UserPreferenceRepository userPreferenceRepository;

    // Constructor injection
    public SetupController(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    @PostMapping("/init-user-preference")
    public ResponseEntity<UserPreference> initializeUserPreference(
            @RequestParam int userId, // int for Integer
            @RequestParam String fcmToken,
            @RequestParam String email) {

        userPreferenceRepository.deleteByUserId(userId);

        // Create a list of NotificationChannel objects
        List<NotificationChannel> notificationChannels = Arrays.asList(
                new NotificationChannel("firebase", fcmToken, true),
                new NotificationChannel("mail", email, true)
        );

        UserPreference preference = new UserPreference(
                userId,
                notificationChannels // Pass the list
        );

        UserPreference savedPreference = userPreferenceRepository.save(preference);
        return ResponseEntity.ok(savedPreference);
    }

    @GetMapping("/user-preference/{userId}")
    public ResponseEntity<UserPreference> getUserPreference(@PathVariable int userId) {
        // Assuming findAllByUserId returns a List<UserPreference>
        List<UserPreference> preferences = userPreferenceRepository.findAllByUserId(userId);

        if (!preferences.isEmpty()) { // Check if list is not empty
            return ResponseEntity.ok(preferences.get(0)); // Get the first element
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user-preference/{userId}")
    public ResponseEntity<Void> deleteUserPreference(@PathVariable int userId) {
        userPreferenceRepository.deleteByUserId(userId);
        return ResponseEntity.ok().build();
    }
}
