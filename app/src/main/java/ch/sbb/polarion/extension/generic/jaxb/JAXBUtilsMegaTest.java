package ch.sbb.polarion.extension.generic.jaxb;

import ch.sbb.polarion.extension.generic.exception.JAXBUnmarshalException;
import lombok.experimental.UtilityClass;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class JAXBUtilsMegaTest {

    public static <T> T deserialize(final Class<T> type, final String xml) throws JAXBException {
        if (xml == null) {
            return null;
        }
        try (final StringReader stringReader = new StringReader(xml)) {
            return deserialize(type, stringReader);
        }
    }

    public static <T> T deserialize(final Class<T> type, final InputStream inputStream) throws JAXBException, IOException {
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return deserialize(type, inputStreamReader);
        }
    }

    public static <T> T deserialize(final Class<T> type, final File source) throws JAXBException, IOException {
        try (final FileInputStream fileInputStream = new FileInputStream(source)) {
            return deserialize(type, fileInputStream);
        }
    }

    public static <T> T deserialize(final Class<T> type, final Reader reader) throws JAXBException {
        if (type == null || reader == null) {
            return null;
        }
        final Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        unmarshaller.setEventHandler(event -> {
            throw new JAXBUnmarshalException(event.getMessage(), event.getLinkedException());
        });
        final StreamSource streamSource = new StreamSource(reader);
        final JAXBElement<T> root = unmarshaller.unmarshal(streamSource, type);
        return root.getValue();
    }


    public static <T> String serialize(final T object) throws JAXBException, IOException {
        return serialize(object, true);
    }


    public static <T> String serialize(final T object, boolean xmlDeclaration) throws JAXBException, IOException {
        if (object == null) {
            return null;
        }

        final XmlType xmlType = object.getClass().getAnnotation(XmlType.class);
        String tagName = xmlType != null ? xmlType.name() : object.getClass().getSimpleName();


        final XmlSchema xmlSchema = object.getClass().getPackage().getAnnotation(XmlSchema.class);
        final String namespace = xmlSchema != null ? xmlSchema.namespace() : "";

        return serialize(object, new QName(namespace, tagName), xmlDeclaration);
    }

    @SuppressWarnings("unchecked")
    public static <T> String serialize(final T object, final QName qName, boolean xmlDeclaration) throws JAXBException, IOException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
        final JAXBIntrospector jaxbIntrospector = jaxbContext.createJAXBIntrospector();
        final Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, !xmlDeclaration);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        try (final StringWriter stringWriter = new StringWriter()) {

            if (jaxbIntrospector.getElementName(object) == null) {
                final JAXBElement<T> jaxbElement = new JAXBElement<>(qName, (Class<T>) object.getClass(), object);
                marshaller.marshal(jaxbElement, stringWriter);
            } else {
                marshaller.marshal(object, stringWriter);
            }

            stringWriter.flush();
            return stringWriter.toString();
        }
    }
}
