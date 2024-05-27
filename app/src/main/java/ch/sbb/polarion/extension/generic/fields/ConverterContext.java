package ch.sbb.polarion.extension.generic.fields;

import com.polarion.subterra.base.data.identification.IContextId;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ConverterContext {

    private IContextId contextId;

    private Map<String, Map<String, String>> enumsMapping;

    @SuppressWarnings("rawtypes")
    private Class preferredReturnType;

}
