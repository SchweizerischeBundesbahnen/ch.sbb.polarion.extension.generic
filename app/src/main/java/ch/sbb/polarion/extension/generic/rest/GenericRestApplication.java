package ch.sbb.polarion.extension.generic.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import ch.sbb.polarion.extension.generic.rest.controller.NamedSettingsApiController;
import ch.sbb.polarion.extension.generic.rest.controller.NamedSettingsApiScopeAgnosticController;
import ch.sbb.polarion.extension.generic.rest.controller.NamedSettingsInternalController;
import ch.sbb.polarion.extension.generic.rest.exception.ForbiddenExceptionMapper;
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
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(getExceptionMappers());
        classes.addAll(getFilters());
        classes.addAll(getControllerClasses());
        classes.add(JacksonFeature.class);
        return classes;
    }

    @NotNull
    protected Set<Class<?>> getExceptionMappers() {
        return new HashSet<>();
    }

    @NotNull
    protected Set<Class<?>> getFilters() {
        return new HashSet<>();
    }

    @NotNull
    protected Set<Class<?>> getControllerClasses() {
        return new HashSet<>();
    }

    @Override
    @NotNull
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.addAll(getExceptionMapperSingletons());
        singletons.addAll(getFilterSingletons());
        singletons.addAll(getControllerSingletons());
        return singletons;
    }

    @NotNull
    protected Set<Object> getExceptionMapperSingletons() {
        return new HashSet<>(Arrays.asList(
                new BadRequestExceptionMapper(),
                new ForbiddenExceptionMapper(),
                new IllegalArgumentExceptionMapper(),
                new InternalServerErrorExceptionMapper(),
                new NotFoundExceptionMapper(),
                new ObjectNotFoundExceptionMapper(),
                new UncaughtExceptionMapper()
        ));
    }

    @NotNull
    protected Set<Object> getFilterSingletons() {
        return new HashSet<>(Arrays.asList(
                new AuthenticationFilter(),
                new CorsFilter(),
                new LogoutFilter()
        ));
    }

    @NotNull
    protected Set<Object> getControllerSingletons() {
        HashSet<Object> controllerSingletons = new HashSet<>(Arrays.asList(
                new ExtensionInfoApiController(),
                new ExtensionInfoInternalController(),
                new SwaggerController(),
                new SwaggerDefinitionController()
        ));
        if (!NamedSettingsRegistry.INSTANCE.getAll().isEmpty()) {
            controllerSingletons.add(NamedSettingsRegistry.INSTANCE.isScopeAgnostic() ? new NamedSettingsApiScopeAgnosticController() : new NamedSettingsApiController());
            controllerSingletons.add(new NamedSettingsInternalController());
        }
        return controllerSingletons;
    }
}
