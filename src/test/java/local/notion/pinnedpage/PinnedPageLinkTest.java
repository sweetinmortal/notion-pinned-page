package local.notion.pinnedpage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PinnedPageLinkTest {
    @Test
    public void convertsDifferentPagesAcrossWorkspaces() {
        assertEquals("https://www.notion.so/0123456789abcdef0123456789abcdef",
                PinnedPageLink.fromShortcut("widget",
                        "https://app.notion.com/first-workspace/0123456789abcdef0123456789abcdef"));
        assertEquals("https://www.notion.so/55555555555555555555555555555555",
                PinnedPageLink.fromShortcut("widget",
                        "https://app.notion.com/second-workspace/55555555-5555-5555-5555-555555555555"));
    }

    @Test
    public void preservesRawQueryAndFragment() {
        assertEquals("https://www.notion.so/0123456789abcdef0123456789abcdef?v=abc%2Bdef#section",
                PinnedPageLink.fromShortcut("widget",
                        "https://app.notion.com/workspace/0123456789abcdef0123456789abcdef?v=abc%2Bdef#section"));
    }

    @Test
    public void leavesOtherSourcesAndMalformedAddressesUntouched() {
        assertNull(PinnedPageLink.fromShortcut(null, null));
        assertNull(PinnedPageLink.fromShortcut("browser",
                "https://app.notion.com/workspace/0123456789abcdef0123456789abcdef"));
        assertNull(PinnedPageLink.fromShortcut("widget",
                "https://app.notion.com.example.org/workspace/0123456789abcdef0123456789abcdef"));
        assertNull(PinnedPageLink.fromShortcut("widget",
                "https://other.example@app.notion.com/workspace/0123456789abcdef0123456789abcdef"));
        assertNull(PinnedPageLink.fromShortcut("widget",
                "https://app.notion.com:444/workspace/0123456789abcdef0123456789abcdef"));
        assertNull(PinnedPageLink.fromShortcut("widget", "https://app.notion.com/workspace"));
        assertNull(PinnedPageLink.fromShortcut("widget", "https://app.notion.com"));
    }
}
