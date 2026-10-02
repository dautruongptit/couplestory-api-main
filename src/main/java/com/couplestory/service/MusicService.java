package com.couplestory.service;

import com.couplestory.entity.MusicTrack;
import com.couplestory.entity.Story;
import com.couplestory.entity.StoryMusic;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.MusicTrackRepository;
import com.couplestory.repository.StoryMusicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Admin-managed music library; every active track is usable on every plan, the plan only caps how many per website. */
@Service
public class MusicService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "m4a", "ogg", "wav");

    private final MusicTrackRepository trackRepository;
    private final StoryMusicRepository storyMusicRepository;
    private final StoryAccessService storyAccessService;
    private final PlanLimitService planLimitService;
    private final StorageService storageService;

    public MusicService(MusicTrackRepository trackRepository, StoryMusicRepository storyMusicRepository,
                        StoryAccessService storyAccessService, PlanLimitService planLimitService,
                        StorageService storageService) {
        this.trackRepository = trackRepository;
        this.storyMusicRepository = storyMusicRepository;
        this.storyAccessService = storyAccessService;
        this.planLimitService = planLimitService;
        this.storageService = storageService;
    }

    public List<MusicTrack> activeLibrary() {
        return trackRepository.findByIsActiveTrueOrderByTitleAsc();
    }

    public List<MusicTrack> allTracks() {
        return trackRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public MusicTrack addTrack(MultipartFile file, String title, String artist, String licenseNote) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chưa chọn file nhạc.");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Tên bài hát là bắt buộc.");
        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file mp3, m4a, ogg, wav.");
        }
        String stored = "music_" + UUID.randomUUID() + "." + ext;
        storageService.store(file, stored);
        return trackRepository.save(MusicTrack.builder()
                .title(title.trim())
                .artist(artist == null || artist.isBlank() ? null : artist.trim())
                .licenseNote(licenseNote == null || licenseNote.isBlank() ? null : licenseNote.trim())
                .storedFile(stored)
                .build());
    }

    @Transactional
    public MusicTrack updateTrack(UUID id, String title, String artist, String licenseNote, Boolean isActive) {
        MusicTrack track = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found"));
        if (title != null && !title.isBlank()) track.setTitle(title.trim());
        if (artist != null) track.setArtist(artist.isBlank() ? null : artist.trim());
        if (licenseNote != null) track.setLicenseNote(licenseNote.isBlank() ? null : licenseNote.trim());
        if (isActive != null) track.setIsActive(isActive);
        return trackRepository.save(track);
    }

    /** Ordered playlist of a story; tracks switched off by an admin are skipped. */
    public List<MusicTrack> playlist(UUID storyId) {
        List<StoryMusic> rows = storyMusicRepository.findByStoryIdOrderBySortOrderAsc(storyId);
        Map<UUID, MusicTrack> tracks = trackRepository.findAllById(rows.stream().map(StoryMusic::getTrackId).toList())
                .stream().collect(Collectors.toMap(MusicTrack::getId, Function.identity()));
        List<MusicTrack> result = new ArrayList<>();
        for (StoryMusic row : rows) {
            MusicTrack track = tracks.get(row.getTrackId());
            if (track != null && Boolean.TRUE.equals(track.getIsActive())) result.add(track);
        }
        return result;
    }

    public List<MusicTrack> ownedPlaylist(UUID storyId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return playlist(storyId);
    }

    @Transactional
    public List<MusicTrack> setPlaylist(UUID storyId, List<UUID> trackIds, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(storyId, userId);
        Set<UUID> unique = new HashSet<>(trackIds);
        if (unique.size() != trackIds.size()) throw new IllegalArgumentException("Mỗi bài chỉ được chọn một lần.");

        int max = planLimitService.plan(story.getOwnerId()).getMaxMusicTracks();
        if (trackIds.size() > max) {
            throw new IllegalArgumentException("Gói của bạn cho tối đa " + max + " bài nhạc mỗi website. Vui lòng nâng cấp gói để thêm.");
        }
        Map<UUID, MusicTrack> tracks = trackRepository.findAllById(trackIds)
                .stream().collect(Collectors.toMap(MusicTrack::getId, Function.identity()));
        for (UUID id : trackIds) {
            MusicTrack track = tracks.get(id);
            if (track == null || !Boolean.TRUE.equals(track.getIsActive())) {
                throw new IllegalArgumentException("Bài nhạc không tồn tại hoặc đã bị gỡ.");
            }
        }

        storyMusicRepository.deleteByStoryId(storyId);
        storyMusicRepository.flush();
        for (int i = 0; i < trackIds.size(); i++) {
            storyMusicRepository.save(StoryMusic.builder().storyId(storyId).trackId(trackIds.get(i)).sortOrder(i).build());
        }
        return trackIds.stream().map(tracks::get).toList();
    }
}
