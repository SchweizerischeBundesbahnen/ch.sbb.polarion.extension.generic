package ch.sbb.polarion.extension.generic.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Roles available to grant in a given scope")
public class RolesInfo {

    @Schema(description = "Roles defined for the whole repository", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> globalRoles;

    @Schema(description = "Roles defined for the project of the scope; empty in global scope", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> projectRoles;
}
