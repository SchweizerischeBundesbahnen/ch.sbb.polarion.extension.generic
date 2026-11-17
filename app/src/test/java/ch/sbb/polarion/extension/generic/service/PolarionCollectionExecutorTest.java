package ch.sbb.polarion.extension.generic.service;

import ch.sbb.polarion.extension.generic.exception.CollectionExecutionException;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollection;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.ipi.CollectionThreadContext;
import com.polarion.portal.internal.shared.navigation.CollectionReferenceConverter;
import com.polarion.portal.shared.navigation.CollectionContext;
import com.polarion.subterra.base.data.identification.IObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PolarionCollectionExecutorTest {

    private static final String PROJECT = "drivepilot";
    private static final String COLLECTION = "elibrary/144~1234";

    private final InternalReadOnlyTransaction transaction = mock(InternalReadOnlyTransaction.class);
    private final IObjectId collectionObjectId = mock(IObjectId.class);
    private final CollectionReferenceConverter converter = mock(CollectionReferenceConverter.class);
    private final BaselineCollectionReference reference = mock(BaselineCollectionReference.class);
    private final IBaselineCollection collection = mock(IBaselineCollection.class);

    private MockedStatic<CollectionReferenceConverter> converterStatic;

    @BeforeEach
    void setUp() {
        converterStatic = mockStatic(CollectionReferenceConverter.class);
        converterStatic.when(CollectionReferenceConverter::getInstance).thenReturn(converter);

        // The collection is named as the address of the page names it, and Polarion's own converter reads it
        when(converter.relativeStringToReference(PROJECT, COLLECTION)).thenReturn(reference);
        when(converter.referenceToObjectId(reference)).thenReturn(collectionObjectId);

        BaselineCollection baselineCollection = mock(BaselineCollection.class);
        when(baselineCollection.getOldApi()).thenReturn(collection);
        when(reference.get(transaction)).thenReturn(baselineCollection);
    }

    @AfterEach
    void tearDown() {
        converterStatic.close();
    }

    @Test
    void runsOutsideAnyCollectionWhereNoneIsNamed() {
        assertEquals(Optional.empty(), PolarionCollectionExecutor.executeInCollection(PROJECT, null, transaction, CollectionThreadContext::getContextCollectionId));
        assertEquals(Optional.empty(), PolarionCollectionExecutor.executeInCollection(PROJECT, "", transaction, CollectionThreadContext::getContextCollectionId));
        verifyNoInteractions(converter);
    }

    @Test
    void runsInsideTheNamedCollectionAndLeavesItAfterwards() {
        Optional<IObjectId> seen = PolarionCollectionExecutor.executeInCollection(PROJECT, COLLECTION, transaction, CollectionThreadContext::getContextCollectionId);

        assertEquals(Optional.of(collectionObjectId), seen);
        assertTrue(CollectionThreadContext.getContextCollectionId().isEmpty());
    }

    @Test
    void leavesTheCollectionWhenTheCallFails() {
        assertThrows(CollectionExecutionException.class, () -> PolarionCollectionExecutor.executeInCollection(PROJECT, COLLECTION, transaction, this::failsWithCheckedException));
        assertThrows(IllegalStateException.class, () -> PolarionCollectionExecutor.executeInCollection(PROJECT, COLLECTION, transaction, this::failsWithRuntimeException));

        assertTrue(CollectionThreadContext.getContextCollectionId().isEmpty());
    }

    @Test
    void refusesACollectionWhichDoesNotExist() {
        // Ignoring it would read the document at HEAD, which is the wrong document and does not look wrong
        when(collection.isUnresolvable()).thenReturn(true);
        DocumentReference document = mock(DocumentReference.class);

        assertThrows(IllegalArgumentException.class, () -> PolarionCollectionExecutor.executeInCollection(PROJECT, COLLECTION, transaction, () -> "never"));
        assertThrows(IllegalArgumentException.class, () -> PolarionCollectionExecutor.resolveDocument(PROJECT, COLLECTION, document, transaction));
    }

    @Test
    void leavesTheDocumentAsItIsWhereNoCollectionIsNamed() {
        DocumentReference document = mock(DocumentReference.class);

        assertSame(document, PolarionCollectionExecutor.resolveDocument(PROJECT, null, document, transaction));
        assertSame(document, PolarionCollectionExecutor.resolveDocument(PROJECT, "", document, transaction));
        verifyNoInteractions(converter);
    }

    @Test
    void resolvesTheDocumentToTheRevisionTheCollectionHoldsItAt() {
        DocumentReference document = mock(DocumentReference.class);
        DocumentReference atCollectionRevision = mock(DocumentReference.class);
        CollectionContext collectionContext = mock(CollectionContext.class);
        when(collectionContext.resolveReference(document)).thenReturn(atCollectionRevision);

        try (MockedStatic<CollectionContext> contextStatic = mockStatic(CollectionContext.class)) {
            contextStatic.when(() -> CollectionContext.byReference(reference)).thenReturn(collectionContext);

            assertSame(atCollectionRevision, PolarionCollectionExecutor.resolveDocument(PROJECT, COLLECTION, document, transaction));
            // Polarion reads the revisions of the collection and of the ones upstream of it on initialization
            verify(collectionContext).initialize(transaction);
        }
    }

    @Test
    void looksTheCollectionUpOnceInsideItsContext() {
        DocumentReference document = mock(DocumentReference.class);
        CollectionContext collectionContext = mock(CollectionContext.class);

        try (MockedStatic<CollectionContext> contextStatic = mockStatic(CollectionContext.class)) {
            contextStatic.when(() -> CollectionContext.byReference(reference)).thenReturn(collectionContext);

            // A document read inside the collection which has just been opened
            PolarionCollectionExecutor.executeInCollection(PROJECT, COLLECTION, transaction,
                    () -> PolarionCollectionExecutor.resolveDocument(PROJECT, COLLECTION, document, transaction));
            verify(reference, times(1)).get(transaction);

            // Read on its own, as the language of the document is, the collection is still checked
            PolarionCollectionExecutor.resolveDocument(PROJECT, COLLECTION, document, transaction);
            verify(reference, times(2)).get(transaction);
        }
    }

    @Test
    void checksACollectionOtherThanTheOneItRunsIn() {
        BaselineCollectionReference other = mock(BaselineCollectionReference.class);
        IBaselineCollection missing = mock(IBaselineCollection.class);
        when(missing.isUnresolvable()).thenReturn(true);
        BaselineCollection otherCollection = mock(BaselineCollection.class);
        when(otherCollection.getOldApi()).thenReturn(missing);
        when(other.get(transaction)).thenReturn(otherCollection);
        when(converter.relativeStringToReference(PROJECT, "999")).thenReturn(other);
        IObjectId otherObjectId = mock(IObjectId.class);
        when(converter.referenceToObjectId(other)).thenReturn(otherObjectId);
        DocumentReference document = mock(DocumentReference.class);

        assertThrows(IllegalArgumentException.class, () -> PolarionCollectionExecutor.executeInCollection(PROJECT, COLLECTION, transaction,
                () -> PolarionCollectionExecutor.resolveDocument(PROJECT, "999", document, transaction)));
    }

    private String failsWithCheckedException() throws IOException {
        throw new IOException("io exception");
    }

    private String failsWithRuntimeException() {
        throw new IllegalStateException("illegal state");
    }

}
