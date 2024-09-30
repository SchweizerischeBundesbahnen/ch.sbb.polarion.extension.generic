package ch.sbb.polarion.extension.generic.util;

import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObject;
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
public class PObjectListStub<T extends IPObject> extends ArrayList<T> implements IPObjectList<T> {

    public PObjectListStub() {
    }

    public PObjectListStub(@NotNull Collection<? extends T> c) {
        super(c);
    }

    @Override
    public IDataService getDataService() {
        return mock(IDataService.class);
    }

    @Override
    public void resolveAll() {
        // do nothing
    }

    @Override
    public void resolveFirst(int n) {
        // do nothing
    }

    @Override
    public void resolve(int first, int length) {
        // do nothing
    }

    @Override
    public List<SubterraURI> getUrisList() {
        return new ArrayList<>();
    }
}
