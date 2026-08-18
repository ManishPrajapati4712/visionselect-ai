package com.visionselect.backend.video.repository;

import com.visionselect.backend.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    Optional<Video> findByStorageKey(String storageKey);
}
