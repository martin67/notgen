package se.terrassorkestern.notgen.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.terrassorkestern.notgen.model.NgFileType;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class NgFileTypeConverter implements AttributeConverter<NgFileType, String> {
    @Override
    public String convertToDatabaseColumn(NgFileType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public NgFileType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(NgFileType.values())
                .filter(c -> c.getCode().equals(dbData))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
