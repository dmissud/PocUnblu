package org.dbs.poc.unblu.livekit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiveKitStartRequest {
    @NotBlank(message = "L'identifiant de la personne Unblu est obligatoire")
    private String personId;
}
