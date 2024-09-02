package ch.sbb.polarion.extension.generic.configuration;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.util.ContextUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("unused")
public abstract class ConfigurationStatusProvider {

    @SneakyThrows
    public static Collection<ConfigurationStatus> getAllStatuses(String scope) {
        Context context = new Context(scope);
        Set<Class<? extends ConfigurationStatusProvider>> subTypes = ContextUtils.findSubTypes(ConfigurationStatusProvider.class);
        Collection<ConfigurationStatus> statuses = new TreeSet<>();
        for (Class<? extends ConfigurationStatusProvider> subType : subTypes) {
            ConfigurationStatusProvider provider = subType.getConstructor().newInstance();
            statuses.addAll(provider.getStatuses(context));
        }
        return statuses;
    }

    public @NotNull Collection<ConfigurationStatus> getStatuses(@NotNull Context context) {
        return List.of(getStatus(context));
    }

    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        throw new IllegalStateException("Either must be implemented by a subclass or shouldn't be called");
    }

    protected @NotNull ConfigurationStatus getConfigurationStatus(@NotNull String name, @Nullable String content, @NotNull String regex) {
        return getConfigurationStatus(name, content, regex, new ConfigurationStatus(name, Status.WARNING, "Not configured"));
    }

    protected @NotNull ConfigurationStatus getConfigurationStatus(@NotNull String name, @Nullable String content, @NotNull String regex, @NotNull ConfigurationStatus notOkStatus) {
        if (content != null && contains(content, regex)) {
            return new ConfigurationStatus(name, Status.OK);
        } else {
            return notOkStatus;
        }
    }

    private static boolean contains(@NotNull String input, @NotNull String regex) {
        return RegexMatcher.get(regex).anyMatch(input);
    }

    @Getter
    @Builder
    public static class Context {
        private String scope;
    }
}
