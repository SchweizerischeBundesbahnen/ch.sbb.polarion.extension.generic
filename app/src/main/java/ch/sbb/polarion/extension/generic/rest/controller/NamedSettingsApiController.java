package ch.sbb.polarion.extension.generic.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.settings.Revision;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.Path;
import java.util.Collection;
import java.util.List;

@Secured
@Path("/api")
public class NamedSettingsApiController extends NamedSettingsInternalController {

    @Override
    public Collection<String> readFeaturesList() {
        return polarionService.callPrivileged(super::readFeaturesList);
    }

    @Override
    public Collection<SettingName> readSettingNames(String feature, String scope) {
        return polarionService.callPrivileged(() -> super.readSettingNames(feature, scope));
    }

    @Override
    public SettingsModel readSetting(String feature, String name, String scope, String revision) {
        return polarionService.callPrivileged(() -> super.readSetting(feature, name, scope, revision));
    }

    @Override
    public void saveSetting(String feature, String name, String scope, final String content) {
        polarionService.callPrivileged(() -> super.saveSetting(feature, name, scope, content));
    }

    @Override
    public void renameSetting(String feature, String name, String scope, final String newName) {
        polarionService.callPrivileged(() -> super.renameSetting(feature, name, scope, newName));
    }

    @Override
    public void deleteSetting(String feature, String name, String scope) {
        polarionService.callPrivileged(() -> super.deleteSetting(feature, name, scope));
    }

    @Override
    public @NotNull List<Revision> readRevisionsList(String feature, String name, String scope) {
        return polarionService.callPrivileged(() -> super.readRevisionsList(feature, name, scope));
    }

    @Override
    public SettingsModel getDefaultValues(String feature) {
        return polarionService.callPrivileged(() -> super.getDefaultValues(feature));
    }
}
