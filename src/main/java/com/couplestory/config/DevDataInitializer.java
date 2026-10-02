package com.couplestory.config;

import com.couplestory.entity.Plan;
import com.couplestory.entity.Role;
import com.couplestory.entity.Template;
import com.couplestory.repository.PlanRepository;
import com.couplestory.repository.RoleRepository;
import com.couplestory.repository.TemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("dev")
@Component
public class DevDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final PlanRepository planRepo;
    private final TemplateRepository templateRepo;

    public DevDataInitializer(RoleRepository roleRepo, PlanRepository planRepo, TemplateRepository templateRepo) {
        this.roleRepo = roleRepo;
        this.planRepo = planRepo;
        this.templateRepo = templateRepo;
    }

    @Override
    public void run(String... args) {
        if (roleRepo.findByName("USER").isEmpty()) {
            roleRepo.save(Role.builder().name("USER").build());
        }
        if (roleRepo.findByName("ADMIN").isEmpty()) {
            roleRepo.save(Role.builder().name("ADMIN").build());
        }

        seedTemplates();

        if (planRepo.findByCode("FREE").isEmpty()) {
            planRepo.saveAll(List.of(
                Plan.builder()
                    .code("FREE").name("FREE").price(0L).sortOrder(0)
                    .description("Bắt đầu miễn phí: tạo website tình yêu đầu tiên của hai bạn.")
                    .features(List.of("Tài khoản trọn đời","Website tồn tại 1 tháng","3 website đồng thời","10 ảnh / website","Có watermark CoupleStory"))
                    .maxPhotos(10).maxStories(3).maxTotalStories(10)
                    .websiteDurationDays(30).maxMusicTracks(1).maxPhotosPerEvent(1)
                    .allowCollaborator(false).allowCustomDomain(false).allowPassword(false).showWatermark(true)
                    .isActive(true).isFeatured(false)
                    .build(),
                Plan.builder()
                    .code("PLUS").name("PLUS").price(49000L).sortOrder(1)
                    .description("Thêm ảnh, thêm nhạc, bỏ watermark và bảo vệ trang bằng mật khẩu.")
                    .features(List.of("Website tồn tại 6 tháng","10 website đồng thời","100 ảnh / website","Không watermark","Đặt mật khẩu cho trang"))
                    .maxPhotos(100).maxStories(10).maxTotalStories(50)
                    .websiteDurationDays(180).maxMusicTracks(5).maxPhotosPerEvent(5)
                    .allowCollaborator(false).allowCustomDomain(false).allowPassword(true).showWatermark(false)
                    .isActive(true).isFeatured(false)
                    .build(),
                Plan.builder()
                    .code("COUPLE").name("COUPLE").price(69000L).sortOrder(2)
                    .description("Cả hai cùng sở hữu và viết chung kỷ niệm.")
                    .features(List.of("Website tồn tại 1 năm","Mời Partner cùng chỉnh sửa","500 ảnh / website","Lời nhắn cho nhau","Chế độ riêng tư"))
                    .maxPhotos(500).maxStories(2147483647).maxTotalStories(null)
                    .websiteDurationDays(365).maxMusicTracks(10).maxPhotosPerEvent(5)
                    .allowCollaborator(true).allowCustomDomain(false).allowPassword(true).showWatermark(false)
                    .isActive(true).isFeatured(true)
                    .build(),
                Plan.builder()
                    .code("PREMIUM").name("PREMIUM").price(119000L).sortOrder(3)
                    .description("Website vĩnh viễn, toàn bộ mẫu cao cấp, không cần gia hạn.")
                    .features(List.of("Website tồn tại vĩnh viễn","Toàn bộ mẫu cao cấp","1.000+ ảnh / website","20 bài nhạc nền","Toàn bộ quyền lợi gói COUPLE"))
                    .maxPhotos(1000).maxStories(2147483647).maxTotalStories(null)
                    .websiteDurationDays(null).maxMusicTracks(20).maxPhotosPerEvent(5)
                    .allowCollaborator(true).allowCustomDomain(false).allowPassword(true).showWatermark(false)
                    .isActive(true).isFeatured(false)
                    .build()
            ));
        }
    }

    private void seedTemplates() {
        if (templateRepo.findByCode("minimal-couple").isPresent()) return;

        templateRepo.saveAll(List.of(
            Template.builder().code("minimal-couple").name("Minimal Couple").type("LOVE_STORY").packageCode("FREE")
                .description("Phong cách tối giản, tập trung vào hình ảnh và khoảng trắng.").sortOrder(1).isActive(true).build(),
            Template.builder().code("romantic-anniversary").name("Romantic Anniversary").type("LOVE_STORY").packageCode("FREE")
                .description("Lãng mạn mơ mộng với hiệu ứng trái tim bay.").sortOrder(2).isActive(true).build(),
            Template.builder().code("memory-wall").name("Romantic Memory Wall").type("LOVE_STORY").packageCode("FREE")
                .description("Bức tường kỷ niệm với ảnh polaroid.").sortOrder(3).isActive(true).build(),
            Template.builder().code("autumn-paris").name("Autumn Paris Romance").type("LOVE_STORY").packageCode("PLUS")
                .description("Mùa thu Paris lãng mạn.").sortOrder(4).isActive(true).build(),
            Template.builder().code("sunset-horizon").name("Sunset Horizon").type("LOVE_STORY").packageCode("PLUS")
                .description("Hoàng hôn trên đường chân trời.").sortOrder(5).isActive(true).build(),
            Template.builder().code("sweet-polaroid").name("Sweet Polaroid Story").type("LOVE_STORY").packageCode("PLUS")
                .description("Câu chuyện qua những tấm polaroid.").sortOrder(6).isActive(true).build(),
            Template.builder().code("anniversary-journey").name("Anniversary Journey").type("LOVE_STORY").packageCode("COUPLE")
                .description("Hành trình tình yêu theo từng cột mốc.").sortOrder(7).isActive(true).build(),
            Template.builder().code("royal-wedding").name("Royal Wedding Memoir").type("LOVE_STORY").packageCode("COUPLE")
                .description("Hồi ký đám cưới hoàng gia.").sortOrder(8).isActive(true).build(),
            Template.builder().code("eternal-love").name("Eternal Love").type("LOVE_STORY").packageCode("PREMIUM")
                .description("Sang trọng, lãng mạn như một cuốn tạp chí cưới.").sortOrder(9).isActive(true).build(),
            Template.builder().code("wedding-cinematic").name("Wedding Cinematic").type("LOVE_CARD").packageCode("PREMIUM")
                .description("Đám cưới điện ảnh nền tối, vàng kim.").sortOrder(20).isActive(false).build(),
            Template.builder().code("wedding-garden-bloom").name("Wedding Garden Bloom").type("LOVE_CARD").packageCode("PREMIUM")
                .description("Đám cưới vườn hoa sáng, xanh sage và hồng đất.").sortOrder(21).isActive(false).build(),
            Template.builder().code("birthday-neon-party").name("Birthday Neon Party").type("LOVE_CARD").packageCode("PREMIUM")
                .description("Sinh nhật neon Y2K nền tối.").sortOrder(22).isActive(false).build(),
            Template.builder().code("birthday-soft-yume").name("Birthday Soft Yume").type("LOVE_CARD").packageCode("FREE")
                .description("Sinh nhật pastel mộng mơ.").sortOrder(23).isActive(false).build(),
            Template.builder().code("confession-typewriter").name("Confession Typewriter").type("LOVE_CARD").packageCode("FREE")
                .description("Tỏ tình bằng lá thư đánh máy.").sortOrder(24).isActive(false).build(),
            Template.builder().code("confession-midnight-bloom").name("Confession Midnight Bloom").type("LOVE_CARD").packageCode("PREMIUM")
                .description("Tỏ tình đêm tím huyền ảo.").sortOrder(25).isActive(false).build()
        ));
    }
}
