package local.notion.pinnedpage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PinnedPageLink {
    private static final String PAGE_ID =
            "(?:[0-9a-f]{32}|[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})";
    private static final Pattern PIN_PATH = Pattern.compile(
            "/[a-z0-9._~%+-]+/(" + PAGE_ID + ")/?", Pattern.CASE_INSENSITIVE);

    private PinnedPageLink() {}

    static String fromShortcut(String source, String url) {
        if (!"widget".equals(source) || url == null) {
            return null;
        }

        URI address;
        try {
            address = new URI(url);
        } catch (URISyntaxException ignored) {
            return null;
        }
        if (!"https".equalsIgnoreCase(address.getScheme())
                || !"app.notion.com".equalsIgnoreCase(address.getHost())
                || address.getRawUserInfo() != null
                || address.getPort() != -1) {
            return null;
        }

        Matcher match = PIN_PATH.matcher(address.getRawPath());
        if (!match.matches()) {
            return null;
        }

        // Notion also accepts the page ID directly through its verified app link.
        StringBuilder result = new StringBuilder("https://www.notion.so/")
                .append(match.group(1).replace("-", ""));
        if (address.getRawQuery() != null) {
            result.append('?').append(address.getRawQuery());
        }
        if (address.getRawFragment() != null) {
            result.append('#').append(address.getRawFragment());
        }
        return result.toString();
    }
}
