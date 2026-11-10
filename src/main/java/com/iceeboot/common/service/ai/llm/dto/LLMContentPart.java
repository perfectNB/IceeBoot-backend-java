package com.iceeboot.common.service.ai.llm.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LLMContentPart {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE_URL = "image_url";
    public static final String IMAGE_DETAIL_ORIGINAL = "original";

    private String type;
    private String text;
    private ImageUrl imageUrl;

    public static LLMContentPart text(String value) {
        return new LLMContentPart().setType(TYPE_TEXT).setText(value);
    }

    public static LLMContentPart imageUrl(String url, String detail) {
        return new LLMContentPart()
                .setType(TYPE_IMAGE_URL)
                .setImageUrl(new ImageUrl().setUrl(url).setDetail(detail));
    }

    @Data
    @Accessors(chain = true)
    public static class ImageUrl {
        private String url;
        private String detail;
    }
}
