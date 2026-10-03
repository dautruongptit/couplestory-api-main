package com.couplestory.controller;

import com.couplestory.entity.User;
import com.couplestory.entity.Device;
import com.couplestory.repository.UserRepository;
import com.couplestory.repository.DeviceRepository;
import com.couplestory.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        if (body.containsKey("name")) {
            user.setDisplayName(body.get("name"));
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @GetMapping("/devices")
    public ResponseEntity<List<Device>> getDevices(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<Device> devices = deviceRepository.findByUserIdOrderByLastSeenAtDesc(userDetails.getId());
        return ResponseEntity.ok(devices);
    }
}
