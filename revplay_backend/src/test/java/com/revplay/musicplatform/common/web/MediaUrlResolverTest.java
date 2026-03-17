package com.revplay.musicplatform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Tag("unit")
class MediaUrlResolverTest {

    private final MediaUrlResolver resolver = new MediaUrlResolver();

    @Test
    @DisplayName("toAbsoluteUrl keeps absolute urls unchanged")
    void toAbsoluteUrlKeepsAbsoluteUrls() {
        assertThat(resolver.toAbsoluteUrl("https://cdn.example.com/image.png"))
                .isEqualTo("https://cdn.example.com/image.png");
    }

    @Test
    @DisplayName("toAbsoluteUrl expands relative app paths using current request context")
    void toAbsoluteUrlExpandsRelativeAppPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("ec2-1-2-3-4.ap-south-1.compute.amazonaws.com");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            assertThat(resolver.toAbsoluteUrl("/api/v1/files/images/cover.png"))
                    .isEqualTo("http://ec2-1-2-3-4.ap-south-1.compute.amazonaws.com:8080/api/v1/files/images/cover.png");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
