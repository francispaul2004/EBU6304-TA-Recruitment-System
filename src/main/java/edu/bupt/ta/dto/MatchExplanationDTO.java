package edu.bupt.ta.dto;

import java.util.List;

public record MatchExplanationDTO(int score,
                                  List<String> matchedSkills,
                                  List<String> missingSkills,
                                  int currentWorkload,
                                  int projectedWorkload,
                                  String explanation) {
}
