package com.jobreadiness.copilot.job.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdAnalysisMessage implements Serializable {
    private UUID jobId;
    private UUID userId;
    private String rawJdText;
}
