package ch.sbb.polarion.extension.generic.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.ws.rs.InternalServerErrorException;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ManifestUtils {

    @SneakyThrows
    public static Attributes getManifestAttributes() {
        Enumeration<URL> resources = VersionUtils.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        if (resources.hasMoreElements()) {
            try (InputStream inputStream = resources.nextElement().openStream()) {
                Manifest manifest = new Manifest(inputStream);
                return manifest.getMainAttributes();
            }
        }

        throw new InternalServerErrorException("Manifest information could not be found or read");
    }
}
