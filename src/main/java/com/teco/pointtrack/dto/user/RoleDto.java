package com.teco.pointtrack.dto.user;

import com.teco.pointtrack.entity.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto {

    Long id;
    String slug;
    String displayName;
    String description;
    Boolean isSystem;

    public RoleDto(Role role) {
        if (role != null) {
            this.id = role.getId();
            this.slug = role.getSlug();
            this.displayName = role.getDisplayName();
            this.description = role.getDescription();
            this.isSystem = role.getIsSystem();
        }
    }
}
