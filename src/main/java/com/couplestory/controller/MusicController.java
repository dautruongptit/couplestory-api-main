package com.couplestory.controller;

import com.couplestory.entity.MusicTrack;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.MusicService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class MusicController {
    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/api/music")
    public ResponseEntity<List<MusicTrack>> library() {
        return ResponseEntity.ok(musicService.activeLibrary());
    }

    @GetMapping("/api/stories/{storyId}/music")
    public ResponseEntity<List<MusicTrack>> playlist(@PathVariable UUID storyId,
                                                     @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(musicService.ownedPlaylist(storyId, user.getId()));
    }

    @PutMapping("/api/stories/{storyId}/music")
    public ResponseEntity<List<MusicTrack>> setPlaylist(@PathVariable UUID storyId,
                                                        @RequestBody List<UUID> trackIds,
                                                        @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(musicService.setPlaylist(storyId, trackIds, user.getId()));
    }

    @GetMapping("/api/admin/music")
    public ResponseEntity<List<MusicTrack>> adminList() {
        return ResponseEntity.ok(musicService.allTracks());
    }

    @PostMapping("/api/admin/music")
    public ResponseEntity<MusicTrack> adminAdd(@RequestParam("file") MultipartFile file,
                                               @RequestParam("title") String title,
                                               @RequestParam(value = "artist", required = false) String artist,
                                               @RequestParam(value = "licenseNote", required = false) String licenseNote) {
        return ResponseEntity.ok(musicService.addTrack(file, title, artist, licenseNote));
    }

    @PutMapping("/api/admin/music/{id}")
    public ResponseEntity<MusicTrack> adminUpdate(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(musicService.updateTrack(
                id,
                (String) body.get("title"),
                (String) body.get("artist"),
                (String) body.get("licenseNote"),
                (Boolean) body.get("isActive")));
    }
}
