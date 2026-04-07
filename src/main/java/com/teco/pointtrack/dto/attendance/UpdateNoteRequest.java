package com.teco.pointtrack.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Body cho PATCH /attendance/{id}/note.
 */
@Getter
@NoArgsConstructor
public class UpdateNoteRequest {

    @NotBlank(message = "Ghi chú không được để trống")
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;
}
