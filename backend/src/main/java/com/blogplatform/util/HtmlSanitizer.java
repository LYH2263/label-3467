package com.blogplatform.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private static final Safelist SAFE_HTML = Safelist.relaxed()
            .addTags("section", "article", "figure", "figcaption", "pre", "code")
            .addAttributes(":all", "class", "style")
            .addProtocols("img", "src", "http", "https", "data");

    public String sanitize(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, SAFE_HTML);
    }
}
