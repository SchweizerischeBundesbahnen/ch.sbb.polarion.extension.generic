package ch.sbb.polarion.extension.generic.rest;

import ch.sbb.polarion.extension.generic.rest.controller.info.ExtensionInfoApiController;
import ch.sbb.polarion.extension.generic.rest.controller.info.ExtensionInfoInternalController;
import ch.sbb.polarion.extension.generic.rest.controller.openapi.OpenApiController;
import ch.sbb.polarion.extension.generic.rest.controller.settings.NamedSettingsApiController;
import ch.sbb.polarion.extension.generic.rest.controller.settings.NamedSettingsInternalController;
import ch.sbb.polarion.extension.generic.rest.controller.swagger.SwaggerController;
import ch.sbb.polarion.extension.generic.rest.controller.swagger.SwaggerDefinitionController;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.BadRequestExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.DuplicateSettingNameExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.ForbiddenExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.IllegalArgumentExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.IllegalStateExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.InternalServerErrorExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.NotFoundExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.ObjectNotFoundExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.NotAuthorizedExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.exception.mapper.UncaughtExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import ch.sbb.polarion.extension.generic.rest.filter.CorsFilter;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenericRestApplication extends Application {

    @Override
    @NotNull
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.add(JacksonFeature.withoutExceptionMappers());
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
        return classes;
    }

    @NotNull
    protected Set<Object> getGenericControllerSingletons() {
        return Set.of();
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
                new DuplicateSettingNameExceptionMapper(),
                new ForbiddenExceptionMapper(),
                new IllegalArgumentExceptionMapper(),
                new IllegalStateExceptionMapper(),
                new InternalServerErrorExceptionMapper(),
                new NotAuthorizedExceptionMapper(),
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
        HashSet<Class<?>> genericControllerClasses = new HashSet<>(Arrays.asList(
                ExtensionInfoApiController.class,
                ExtensionInfoInternalController.class,
                OpenApiController.class,
                SwaggerController.class,
                SwaggerDefinitionController.class
        ));
        if (!NamedSettingsRegistry.INSTANCE.getAll().isEmpty()) {
            genericControllerClasses.add(NamedSettingsApiController.class);
            genericControllerClasses.add(NamedSettingsInternalController.class);
        }
        return genericControllerClasses;
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
