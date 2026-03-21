package com.teco.pointtrack.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTypeResponse {
    private Long id;
    private String email;
    private String displayName;
    private String gender;
    private String avatar;
    private String bio;
    private String onlineStatus; // ONLINE, OFFLINE
}
