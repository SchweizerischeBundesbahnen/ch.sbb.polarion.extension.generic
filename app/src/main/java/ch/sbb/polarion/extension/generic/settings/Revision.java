package ch.sbb.polarion.extension.generic.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.polarion.core.util.logging.Logger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Used to keep and transfer information about particular revision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Revision implements Comparable<Revision> {

    private static final Logger logger = Logger.getLogger((Object) Revision.class);

    private String name;
    private String date;
    private String author;
    private String baseline;
    private String description;

    @Override
    public int compareTo(@NotNull Revision that) {

        long thisRevision = 0L;
        long thatRevision = 0L;

        try {
            thisRevision = Long.parseLong(name);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected revision name found: " + name);
        }
        try {
            thatRevision = Long.parseLong(that.name);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected revision name found: " + name);
        }

        return Long.compare(thatRevision, thisRevision);
    }
}
