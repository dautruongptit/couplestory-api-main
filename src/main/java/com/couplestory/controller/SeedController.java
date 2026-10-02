package com.couplestory.controller;

import com.couplestory.entity.*;
import com.couplestory.repository.*;
import com.couplestory.util.SlugUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Profile("dev")
@RestController
@RequestMapping("/api/public")
public class SeedController {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final StoryRepository storyRepo;
    private final PhotoRepository photoRepo;
    private final StoryEventRepository eventRepo;
    private final StoryMessageRepository msgRepo;
    private final PasswordEncoder passwordEncoder;

    public SeedController(UserRepository userRepo, RoleRepository roleRepo, StoryRepository storyRepo,
                          PhotoRepository photoRepo, StoryEventRepository eventRepo,
                          StoryMessageRepository msgRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.storyRepo = storyRepo;
        this.photoRepo = photoRepo;
        this.eventRepo = eventRepo;
        this.msgRepo = msgRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/seed")
    @Transactional
    public ResponseEntity<String> seedData() {
        String email = "user@couplestory.site";
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));
            user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("123"))
                .displayName("Demo User")
                .roles(Set.of(userRole))
                .build();
            user = userRepo.save(user);
        }

        List<Story> old = storyRepo.findByOwnerIdAndStatusNot(user.getId(), "DELETED");
        for (Story s : old) {
            s.setStatus("DELETED");
            storyRepo.save(s);
        }

        Story story = Story.builder()
            .ownerId(user.getId())
            .slug(SlugUtil.toSlug("Bảo Long An Nhiên " + System.currentTimeMillis()))
            .status("PUBLISHED")
            .planType("PLUS")
            .templateCode("eternal-love")
            .templateConfig("{\"theme_color\":\"#ff4d8d\",\"purpose\":\"confession\"}")
            .coupleName1("Bảo Long")
            .coupleName2("An Nhiên")
            .startDate(LocalDate.of(2021, 5, 20))
            .build();
        story = storyRepo.save(story);
        UUID storyId = story.getId();

        Photo cover = createPhoto(storyId, "photos/STORY/" + storyId + "/cover.jpg", 0, user.getId());
        Photo p1 = createPhoto(storyId, "photos/STORY/" + storyId + "/event1.jpg", 1, user.getId());
        Photo p2 = createPhoto(storyId, "photos/STORY/" + storyId + "/event2.jpg", 2, user.getId());

        story.setCoverPhotoId(cover.getId());
        storyRepo.save(story);

        eventRepo.save(StoryEvent.builder()
            .storyId(storyId)
            .title("Lần Đầu Chạm Mặt")
            .message("Tại quán cafe nhỏ trên phố cổ. Cơn mưa rào mùa hạ đã mang chúng mình đến với nhau.")
            .eventDate(LocalDate.of(2021, 5, 20))
            .photoId(p1.getId())
            .sortOrder(0)
            .build());

        eventRepo.save(StoryEvent.builder()
            .storyId(storyId)
            .title("Nụ Hôn Đầu Tiên")
            .message("Bên bờ hồ gió, mọi thứ như ngừng lại, chỉ còn nhịp đập của hai trái tim.")
            .eventDate(LocalDate.of(2021, 8, 15))
            .photoId(p2.getId())
            .sortOrder(1)
            .build());

        msgRepo.save(StoryMessage.builder()
            .storyId(storyId)
            .type("LOVE_LETTER")
            .content("Gửi An Nhiên của anh,\n\nTừ ngày có em, thế giới của anh trở nên rực rỡ và ấm áp hơn bao giờ hết.")
            .build());

        return ResponseEntity.ok("Seeded successfully! Story ID: " + storyId);
    }

    private Photo createPhoto(UUID storyId, String storageKey, int order, UUID uploadedBy) {
        Photo p = Photo.builder()
            .ownerType("STORY")
            .ownerId(storyId)
            .storageKey(storageKey)
            .filenameOriginal("seed-photo-" + order + ".jpg")
            .filenameStored(UUID.randomUUID() + ".jpg")
            .mimeType("image/jpeg")
            .sortOrder(order)
            .uploadedBy(uploadedBy)
            .build();
        return photoRepo.save(p);
    }
}
