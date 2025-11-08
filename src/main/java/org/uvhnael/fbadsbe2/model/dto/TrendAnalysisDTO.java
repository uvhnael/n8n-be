package org.uvhnael.fbadsbe2.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class TrendAnalysisDTO {
    private String analysisDate;
    private List<String> trendingKeywords;
    private List<String> trendingTopics;
}
