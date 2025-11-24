package org.uvhnael.fbadsbe2.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysisResponse {
    
    private List<String> keywords;
    private List<Topic> topics;
    private String competitorSummary;
    private String suggestions;
    private String aiSummary;
    private BigDecimal confidenceScore;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Topic {
        private String name;
        private String sentiment;
        private Integer volume;
    }
}
