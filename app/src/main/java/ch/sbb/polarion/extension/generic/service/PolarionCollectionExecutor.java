package ch.sbb.polarion.extension.generic.service;

import ch.sbb.polarion.extension.generic.exception.CollectionExecutionException;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.tracker.model.ipi.CollectionThreadContext;
import com.polarion.portal.internal.shared.navigation.CollectionReferenceConverter;
import com.polarion.portal.shared.navigation.CollectionContext;
import com.polarion.subterra.base.data.identification.IObjectId;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

/**
 * Reads and renders a document the way Polarion shows it when it is opened from a collection.
 * <p>
 * Such a document differs from the same document opened on its own in two ways, and Polarion decides them apart:
 * </p>
 * <ul>
 * <li>The revision of the document is the one the collection, or a collection upstream of it, holds it at. The address
 * of the page does not name it; Polarion looks it up in {@link CollectionContext}, and so does
 * {@link #resolveDocument}.</li>
 * <li>A work item the document links to is shown at the revision the collection holds the document of that item at, and
 * one whose document is not part of the collection is not shown at all. Polarion decides that by
 * {@link CollectionThreadContext}, which its own {@code CollectionAwareRPC} opens around a request carrying the
 * {@code X-polarion-collection} header, and which {@code ServerFieldRichTextRenderer} and
 * {@code LinkedWorkItemsRendererImpl} read. A request of an extension is not such a request, so {@link #executeInCollection} opens it.</li>
 * </ul>
 * <p>
 * The collection is named the way Polarion names one relative to a project: its id, preceded by its project and a
 * slash where that is not the project of the document ({@code drivepilot/3}), and followed by {@code ~revision} where
 * the collection itself is opened at a revision. Polarion's own {@link CollectionReferenceConverter} reads it, so that
 * none of these forms is parsed here. The address of the page carries the same name between {@code /collection/} and
 * {@code /wiki/}, except that it doubles the slash, which the caller has to undo before it names the collection.
 * </p>
 */
@UtilityClass
public class PolarionCollectionExecutor {

    public <T> T executeInCollection(@Nullable String projectId, @Nullable String collection, @NotNull ReadOnlyTransaction transaction, @NotNull Callable<T> callable) {
        if (collection == null || collection.isEmpty()) {
            return callCallable(callable);
        }
        BaselineCollectionReference reference = reference(projectId, collection);
        ensureExists(reference, collection, transaction);
        try (CollectionThreadContext ignored = new CollectionThreadContext(objectId(reference))) {
            return callCallable(callable);
        }
    }

    /**
     * @return the document at the revision the collection holds it at, or as it was given where no collection is named
     * or the collection takes the document at HEAD
     */
    public @NotNull DocumentReference resolveDocument(@Nullable String projectId, @Nullable String collection, @NotNull DocumentReference document, @NotNull ReadOnlyTransaction transaction) {
        if (collection == null || collection.isEmpty()) {
            return document;
        }
        BaselineCollectionReference reference = reference(projectId, collection);
        // Inside executeInCollection() of the same collection it was looked up already
        if (CollectionThreadContext.getContextCollectionId().filter(objectId(reference)::equals).isEmpty()) {
            ensureExists(reference, collection, transaction);
        }
        CollectionContext collectionContext = CollectionContext.byReference(reference);
        collectionContext.initialize((InternalReadOnlyTransaction) transaction);
        return collectionContext.resolveReference(document);
    }

    private @NotNull BaselineCollectionReference reference(@Nullable String projectId, @NotNull String collection) {
        return CollectionReferenceConverter.getInstance().relativeStringToReference(projectId, collection);
    }

    private @NotNull IObjectId objectId(@NotNull BaselineCollectionReference reference) {
        return CollectionReferenceConverter.getInstance().referenceToObjectId(reference);
    }

    /**
     * A collection which does not exist is refused rather than ignored: the document would be read at HEAD, which
     * is the wrong document and does not look wrong.
     */
    private void ensureExists(@NotNull BaselineCollectionReference reference, @NotNull String collection, @NotNull ReadOnlyTransaction transaction) {
        if (reference.get(transaction).getOldApi().isUnresolvable()) {
            throw new IllegalArgumentException("Collection '%s' does not exist".formatted(collection));
        }
    }

    private <T> T callCallable(@NotNull Callable<T> callable) {
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CollectionExecutionException("Error during callable execution", e);
        }
    }

}
