package org.dbs.poc.unblu.livekit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveKitStartResponse {
    private String conversationId;
    private String joinUrl;
}
