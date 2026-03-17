package com.revplay.musicplatform.common.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class MediaUrlResolver {

    public String toAbsoluteUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        if (isAbsoluteUrl(url)) {
            return url;
        }

        String normalizedPath = url.startsWith("/") ? url : "/" + url;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(normalizedPath)
                .toUriString();
    }

    private boolean isAbsoluteUrl(String url) {
        return url.startsWith("http://")
                || url.startsWith("https://")
                || url.startsWith("//");
    }
}
