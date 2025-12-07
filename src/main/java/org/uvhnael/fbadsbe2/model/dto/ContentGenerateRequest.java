package org.uvhnael.fbadsbe2.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class ContentGenerateRequest {
    private String contentType;
    private String platform;
    private Long basedOnTrendAnalysisId;
    private List<String> keywords;
    private String tone;
    private String length;
    private Boolean includeHashtags;
    private Boolean includeCTA;
}
