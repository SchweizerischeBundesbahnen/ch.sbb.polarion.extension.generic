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
        return new HashSet<>(Arrays.asList(
                BadRequestExceptionMapper.class,
                ForbiddenExceptionMapper.class,
                IllegalArgumentExceptionMapper.class,
                InternalServerErrorExceptionMapper.class,
                NotFoundExceptionMapper.class,
                ObjectNotFoundExceptionMapper.class,
                UncaughtExceptionMapper.class
        ));
    }

    @NotNull
    protected Set<Class<?>> getFilters() {
        return new HashSet<>(Arrays.asList(
                AuthenticationFilter.class,
                CorsFilter.class,
                LogoutFilter.class
        ));
    }

    @NotNull
    protected Set<Class<?>> getControllerClasses() {
        HashSet<Class<?>> controllerClasses = new HashSet<>(Arrays.asList(
                ExtensionInfoApiController.class,
                ExtensionInfoInternalController.class,
                SwaggerController.class,
                SwaggerDefinitionController.class
        ));
        if (!NamedSettingsRegistry.INSTANCE.getAll().isEmpty()) {
            controllerClasses.add(NamedSettingsRegistry.INSTANCE.isScopeAgnostic() ? NamedSettingsApiScopeAgnosticController.class : NamedSettingsApiController.class);
            controllerClasses.add(NamedSettingsInternalController.class);
        }
        return controllerClasses;
    }
}
