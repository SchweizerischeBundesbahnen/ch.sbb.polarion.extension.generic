package ch.sbb.polarion.extension.generic.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;

import ch.sbb.polarion.extension.generic.rest.controller.NamedSettingsApiController;
import ch.sbb.polarion.extension.generic.rest.controller.NamedSettingsApiScopeAgnosticController;
import ch.sbb.polarion.extension.generic.rest.controller.NamedSettingsInternalController;
import ch.sbb.polarion.extension.generic.rest.exception.ForbiddenExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.IllegalStateExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.ObjectNotFoundExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.filter.CorsFilter;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.jetbrains.annotations.NotNull;

import ch.sbb.polarion.extension.generic.rest.controller.ExtensionInfoApiController;
import ch.sbb.polarion.extension.generic.rest.controller.ExtensionInfoInternalController;
import ch.sbb.polarion.extension.generic.rest.controller.SwaggerController;
import ch.sbb.polarion.extension.generic.rest.controller.SwaggerDefinitionController;
import ch.sbb.polarion.extension.generic.rest.exception.BadRequestExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.IllegalArgumentExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.InternalServerErrorExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.NotFoundExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.UncaughtExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;

public class GenericRestApplication extends Application {

    @Override
    @NotNull
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.addAll(getAllExceptionMapperSingletons());
        singletons.addAll(getAllFilterSingletons());
        singletons.addAll(getAllControllerSingletons());
        return singletons;
    }

    @Override
    @NotNull
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(getAllExceptionMapperClasses());
        classes.addAll(getAllFilterClasses());
        classes.addAll(getAllControllerClasses());
        classes.add(JacksonFeature.class);
        return classes;
    }

    @NotNull
    protected Set<Object> getGenericControllerSingletons() {
        HashSet<Object> genericControllerSingletons = new HashSet<>(Arrays.asList(
                new ExtensionInfoApiController(),
                new ExtensionInfoInternalController(),
                new SwaggerController(),
                new SwaggerDefinitionController()
        ));
        if (!NamedSettingsRegistry.INSTANCE.getAll().isEmpty()) {
            genericControllerSingletons.add(NamedSettingsRegistry.INSTANCE.isScopeAgnostic() ? new NamedSettingsApiScopeAgnosticController() : new NamedSettingsApiController());
            genericControllerSingletons.add(new NamedSettingsInternalController());
        }
        return genericControllerSingletons;
    }

    @NotNull
    protected Set<Object> getGenericFilterSingletons() {
        return Set.of(
                new AuthenticationFilter(),
                new CorsFilter(),
                new LogoutFilter()
        );
    }

    protected Set<Object> getGenericExceptionMapperSingletons() {
        return Set.of(
                new BadRequestExceptionMapper(),
                new ForbiddenExceptionMapper(),
                new IllegalArgumentExceptionMapper(),
                new IllegalStateExceptionMapper(),
                new InternalServerErrorExceptionMapper(),
                new NotFoundExceptionMapper(),
                new ObjectNotFoundExceptionMapper(),
                new UncaughtExceptionMapper()
        );
    }

    @NotNull
    protected Set<Object> getExtensionControllerSingletons() {
        return Set.of();
    }

    @NotNull
    protected Set<Object> getExtensionFilterSingletons() {
        return Set.of();
    }

    @NotNull
    protected Set<Object> getExtensionExceptionMapperSingletons() {
        return Set.of();
    }

    @NotNull
    protected Set<Class<?>> getGenericControllerClasses() {
        return Set.of();
    }

    @NotNull
    protected Set<Class<?>> getGenericFilterClasses() {
        return Set.of();
    }

    @NotNull
    protected Set<Class<?>> getGenericExceptionMapperClasses() {
        return Set.of();
    }

    @NotNull
    protected Set<Class<?>> getExtensionControllerClasses() {
        return Set.of();
    }

    @NotNull
    protected Set<Class<?>> getExtensionExceptionMapperClasses() {
        return Set.of();
    }

    @NotNull
    protected Set<Class<?>> getExtensionFilterClasses() {
        return Set.of();
    }

    @NotNull
    private Set<Object> getAllControllerSingletons() {
        return Stream.concat(getGenericControllerSingletons().stream(), getExtensionControllerSingletons().stream())
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<Object> getAllFilterSingletons() {
        return Stream.concat(getGenericFilterSingletons().stream(), getExtensionFilterSingletons().stream())
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<Object> getAllExceptionMapperSingletons() {
        return Stream.concat(getGenericExceptionMapperSingletons().stream(), getExtensionExceptionMapperSingletons().stream())
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<Class<?>> getAllControllerClasses() {
        return Stream.concat(getGenericControllerClasses().stream(), getExtensionControllerClasses().stream())
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<Class<?>> getAllFilterClasses() {
        return Stream.concat(getGenericFilterClasses().stream(), getExtensionFilterClasses().stream())
                .collect(Collectors.toSet());
    }

    @NotNull
    private Set<Class<?>> getAllExceptionMapperClasses() {
        return Stream.concat(getGenericExceptionMapperClasses().stream(), getExtensionExceptionMapperClasses().stream())
                .collect(Collectors.toSet());
    }
}
