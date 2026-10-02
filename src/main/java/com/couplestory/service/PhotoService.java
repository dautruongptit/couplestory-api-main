package com.couplestory.service;

import com.couplestory.entity.Photo;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.PhotoRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PhotoService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^[a-zA-Z0-9]{1,5}$");

    private final PhotoRepository photoRepository;
    private final StorageService storageService;
    private final StoryAccessService storyAccessService;
    private final PlanLimitService planLimitService;

    public PhotoService(PhotoRepository photoRepository, StorageService storageService,
                        StoryAccessService storyAccessService, PlanLimitService planLimitService) {
        this.photoRepository = photoRepository;
        this.storageService = storageService;
        this.storyAccessService = storyAccessService;
        this.planLimitService = planLimitService;
    }

    public List<Photo> getPhotos(String ownerType, UUID storyId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return photoRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, storyId);
    }

    @Transactional
    public Photo uploadPhoto(String ownerType, UUID storyId, MultipartFile file, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);

        var plan = planLimitService.plan(userId);
        if (plan.getMaxPhotos() != null
                && photoRepository.countByOwnerTypeAndOwnerId(ownerType, storyId) >= plan.getMaxPhotos()) {
            throw new IllegalArgumentException("Gói " + plan.getCode() + " cho tối đa " + plan.getMaxPhotos()
                    + " ảnh mỗi website. Vui lòng nâng cấp gói để thêm ảnh.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractSafeExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        String thumbnailFilename = "thumb_" + storedFilename;
        long originalSize = file.getSize();

        try {
            byte[] originalBytes = file.getBytes();
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            int width = originalImage != null ? originalImage.getWidth() : 0;
            int height = originalImage != null ? originalImage.getHeight() : 0;

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(1920, 1080)
                    .outputQuality(0.85)
                    .toOutputStream(os);
            byte[] processedBytes = os.toByteArray();

            String storageKey = "photos/STORY/" + storyId + "/" + storedFilename;
            String thumbnailKey = "photos/STORY/" + storyId + "/" + thumbnailFilename;

            storageService.store(new ByteArrayInputStream(processedBytes), storedFilename, file.getContentType());

            ByteArrayOutputStream thumbOs = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(processedBytes))
                    .size(400, 400)
                    .outputQuality(0.8)
                    .toOutputStream(thumbOs);
            storageService.store(new ByteArrayInputStream(thumbOs.toByteArray()), thumbnailFilename, file.getContentType());

            long count = photoRepository.countByOwnerTypeAndOwnerId(ownerType, storyId);

            Photo photo = Photo.builder()
                    .ownerType(ownerType)
                    .ownerId(storyId)
                    .storageKey(storageKey)
                    .thumbnailKey(thumbnailKey)
                    .filenameOriginal(originalFilename != null ? originalFilename : "unknown.jpg")
                    .filenameStored(storedFilename)
                    .mimeType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
                    .sizeBytes(processedBytes.length)
                    .originalSizeBytes(originalSize)
                    .width(width)
                    .height(height)
                    .sortOrder((int) count)
                    .uploadedBy(userId)
                    .build();

            return photoRepository.save(photo);
        } catch (IOException e) {
            throw new RuntimeException("Could not process image", e);
        }
    }

    private String extractSafeExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String candidate = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (SAFE_EXTENSION.matcher(candidate).matches() && ALLOWED_EXTENSIONS.contains(candidate)) {
                return candidate;
            }
        }
        return "jpg";
    }

    @Transactional
    public void deletePhoto(UUID storyId, UUID photoId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
        if (!photo.getOwnerId().equals(storyId)) {
            throw new ForbiddenOperationException("Photo does not belong to this story");
        }
        storageService.delete(photo.getFilenameStored());
        storageService.delete("thumb_" + photo.getFilenameStored());
        photoRepository.delete(photo);
    }

    @Transactional
    public void reorderPhotos(String ownerType, UUID storyId, List<UUID> photoIds, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        for (int i = 0; i < photoIds.size(); i++) {
            Photo photo = photoRepository.findById(photoIds.get(i))
                    .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
            if (!ownerType.equals(photo.getOwnerType()) || !photo.getOwnerId().equals(storyId)) {
                throw new ForbiddenOperationException("Photo does not belong to this story");
            }
            photo.setSortOrder(i);
            photoRepository.save(photo);
        }
    }
}
