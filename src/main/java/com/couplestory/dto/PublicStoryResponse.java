package com.couplestory.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PublicStoryResponse {
    private String id;
    private String slug;
    private String templateCode;
    private Object templateConfig;
    private String coupleName1;
    private String coupleName2;
    private String title;
    private String shortQuote;
    private String startDate;
    private String coverPhotoUrl;
    private List<TimelineEventDto> events;
    private List<PhotoDto> gallery;
    private MessageDto loveLetter;
    private MessageDto finalMessage;
    private List<MomentDto> moments;

    @Data @Builder
    public static class TimelineEventDto {
        private String id;
        private String title;
        private String message;
        private String location;
        private String eventDate;
        private String photoUrl;
        private Integer order;
    }

    @Data @Builder
    public static class PhotoDto {
        private String id;
        private String url;
        private String thumbnailUrl;
        private Integer order;
    }

    @Data @Builder
    public static class MessageDto {
        private String id;
        private String heading;
        private String content;
        private String signature;
    }

    @Data @Builder
    public static class MomentDto {
        private String id;
        private String title;
        private String description;
        private String photoUrl;
        private Integer order;
    }
}
