package com.couplestory.service;

import com.couplestory.entity.Device;
import com.couplestory.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserActivityService activityService;

    public DeviceService(DeviceRepository deviceRepository, UserActivityService activityService) {
        this.activityService = activityService;
        this.deviceRepository = deviceRepository;
    }

    /** Ghi lại mỗi lần đăng nhập (thiết bị, nền tảng, IP) vào DB. */
    public void recordLogin(UUID userId, String userAgent, String ip, String method) {
        String ua = userAgent == null ? "" : userAgent;
        Device d = Device.builder()
                .userId(userId)
                .name(parseBrowser(ua) + " trên " + parsePlatform(ua))
                .platform(parsePlatform(ua))
                .ipAddress(ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip)
                .userAgent(ua.length() > 500 ? ua.substring(0, 500) : ua)
                .loginMethod(method)
                .lastSeenAt(OffsetDateTime.now())
                .build();
        deviceRepository.save(d);
        activityService.record(userId, com.couplestory.entity.ActivityAction.LOGIN, "USER", userId,
                "GOOGLE".equals(method) ? "Đăng nhập bằng Google" : "Đăng nhập bằng mật khẩu");
    }

    private String parsePlatform(String ua) {
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "Unknown";
    }

    private String parseBrowser(String ua) {
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/")) return "Safari";
        return "Trình duyệt";
    }
}
