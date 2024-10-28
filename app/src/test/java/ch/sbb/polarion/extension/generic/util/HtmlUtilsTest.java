package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlUtilsTest {

    private static final String TAG_START = "<td>";
    private static final String TAG_END = "</td>";

    @Test
    void getEnding() {

        // no tag exist at all
        assertEquals(33, HtmlUtils.getEnding("<div><span>Some text</span></div>", 0, TAG_START, TAG_END));

        // only start tag exists
        assertEquals(37, HtmlUtils.getEnding("<td><div><span>Some text</span></div>", 0, TAG_START, TAG_END));

        // only end tag exists
        assertEquals(38, HtmlUtils.getEnding("<div><span>Some text</span></div></td>some text", 0, TAG_START, TAG_END));

        // only start tag exists + inner
        assertEquals(79, HtmlUtils.getEnding("some text<td><table><td><div><span>Some text</span></div></td></table>some text", 0, TAG_START, TAG_END));

        // only end tag exists + inner
        assertEquals(62, HtmlUtils.getEnding("<table><td><div><span>Some text</span></div></td></table></td>some text", 0, TAG_START, TAG_END));

        // ok
        assertEquals(71, HtmlUtils.getEnding("<div><td><table><td><div><span>Some text</span></div></td></table></td></div>", 5, TAG_START, TAG_END));

        // inner ok
        assertEquals(58, HtmlUtils.getEnding("<div><td><table><td><div><span>Some text</span></div></td></table></td></div>", 22, TAG_START, TAG_END));
    }
}
