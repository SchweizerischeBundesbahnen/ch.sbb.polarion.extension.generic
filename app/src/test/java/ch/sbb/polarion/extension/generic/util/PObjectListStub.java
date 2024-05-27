package ch.sbb.polarion.extension.generic.util;

import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.SubterraURI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Used in unit tests for stubbing instances of {@link IPObjectList}
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PObjectListStub extends ArrayList implements IPObjectList {

    public PObjectListStub() {
    }

    public PObjectListStub(@NotNull Collection c) {
        super(c);
    }

    @Override
    public IDataService getDataService() {
        return mock(IDataService.class);
    }

    @Override
    public void resolveAll() {

    }

    @Override
    public void resolveFirst(int i) {

    }

    @Override
    public void resolve(int i, int i1) {

    }

    @Override
    public List<SubterraURI> getUrisList() {
        return new ArrayList<>();
    }
}
