package ch.sbb.polarion.extension.generic.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
@SuppressWarnings("unused")
public class HtmlUtils {

    public static int getEnding(@NotNull String html, int fromPosition, String startTag, String endTag) {
        // We specify start position in HTML here, start tag (eg. <ol>), end tag (eg. </ol>) and method
        // searches for a position of closing tag taking into account possible nesting of the same tags. For example here:
        //
        // <ol>
        //   <li>item
        //     <ol>
        //       <li>another item</li>
        //     </ol>
        //   </li>
        // </ol>
        //
        // ...method should return index of last </ol> at its end, and not first </ol>

        int opened = 1;
        int marker = fromPosition;
        while (opened > 0) { // Looking a position in html when all opening tags will be closed
            marker++;
            int nextEnd = html.indexOf(endTag, marker);
            int nextStart = html.indexOf(startTag, marker);
            if (nextEnd < 0) {
                return html.length(); // Shouldn't happen. If opening tags more than closing, and we didn't find next closing, then something's wrong with HTML,
                // and we consider HTML's end is an end of the tag
            } else if (nextStart < 0 || nextEnd < nextStart) {
                opened--; // Closing tag found earlier than opening, thus decreasing counter
                marker = nextEnd;
            } else {
                opened++; // Opening tag found earlier than closing, thus increasing counter (nested list)
                marker = nextStart;
            }
        }

        return Math.min(html.length(), marker + endTag.length());
    }
}
