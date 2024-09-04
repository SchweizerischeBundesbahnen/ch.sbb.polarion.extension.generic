package ch.sbb.polarion.extension.generic.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.polarion.core.util.logging.Logger;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Revision details")
public class Revision implements Comparable<Revision> {

    private static final Logger logger = Logger.getLogger((Object) Revision.class);

    @Schema(description = "The name of the revision")
    private String name;

    @Schema(description = "The date of the revision")
    private String date;

    @Schema(description = "The author of the revision")
    private String author;

    @Schema(description = "The baseline of the revision")
    private String baseline;

    @Schema(description = "The description of the revision")
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
