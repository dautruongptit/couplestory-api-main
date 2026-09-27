package com.couplestory.controller;

import com.couplestory.entity.Photo;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.PhotoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/stories/{storyId}/photos")
public class PhotoController {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public ResponseEntity<List<Photo>> getPhotos(
            @PathVariable UUID storyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(photoService.getPhotos("STORY", storyId, userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<Photo> uploadPhoto(
            @PathVariable UUID storyId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(photoService.uploadPhoto("STORY", storyId, file, userDetails.getId()));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable UUID storyId,
            @PathVariable UUID photoId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        photoService.deletePhoto(storyId, photoId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/order")
    public ResponseEntity<Void> reorderPhotos(
            @PathVariable UUID storyId,
            @RequestBody List<UUID> photoIds,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        photoService.reorderPhotos("STORY", storyId, photoIds, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
