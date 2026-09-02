package com.jobreadiness.copilot.careerprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSkillDto {
    private UUID skillId;
    private String name;
    private String category;
    private String selfAssessedLevel;
}
