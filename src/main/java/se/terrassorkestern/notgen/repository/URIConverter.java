package se.terrassorkestern.notgen.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.net.URI;

@Converter(autoApply = true)
public class URIConverter implements AttributeConverter<URI, String> {
    @Override
    public String convertToDatabaseColumn(URI attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public URI convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return URI.create(dbData);
    }
}