package org.dbs.poc.unblu.livekit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiveKitStartRequest {
    @NotBlank(message = "L'identifiant client est obligatoire")
    private String clientId;
}
