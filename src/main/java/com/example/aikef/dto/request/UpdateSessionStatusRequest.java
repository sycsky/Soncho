package com.example.aikef.dto.request;

import com.example.aikef.model.enums.SessionAction;
import com.example.aikef.model.enums.SessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateSessionStatusRequest(
        @NotBlank String sessionId,
        @NotNull SessionStatus action) {
}
