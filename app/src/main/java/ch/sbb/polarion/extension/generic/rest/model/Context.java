package ch.sbb.polarion.extension.generic.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Context {
    private String extensionContext;

    public String getBaseUrl() {
        return "/polarion/" + extensionContext;
    }

    public String getRestUrl() {
        return "/polarion/" + extensionContext + "/rest";
    }

    public String getSwaggerUiUrl() {
        return getRestUrl() + "/swagger";
    }
}
