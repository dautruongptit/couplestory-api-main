package com.couplestory.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data @AllArgsConstructor
public class JwtResponse {
    private String id;
    private String email;
    private String name;
    private String plan;
    private List<String> roles;
}
