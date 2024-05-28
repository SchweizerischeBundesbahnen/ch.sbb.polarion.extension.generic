package ch.sbb.polarion.extension.generic;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.polarion.alm.ui.server.forms.extensions.FormExtensionContribution;

@SuppressWarnings("unused")
public abstract class GenericModule extends AbstractModule {

    protected abstract FormExtensionContribution getFormExtensionContribution();

    @Override
    protected void configure() {
        Multibinder.newSetBinder(this.binder(), FormExtensionContribution.class)
                .addBinding()
                .toInstance(getFormExtensionContribution());
    }
}
