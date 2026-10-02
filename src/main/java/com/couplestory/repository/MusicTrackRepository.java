package com.couplestory.repository;

import com.couplestory.entity.MusicTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MusicTrackRepository extends JpaRepository<MusicTrack, UUID> {

    List<MusicTrack> findByIsActiveTrueOrderByTitleAsc();

    List<MusicTrack> findAllByOrderByCreatedAtDesc();
}
