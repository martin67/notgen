package se.terrassorkestern.notgen.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.terrassorkestern.notgen.model.LinkType;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class MediaLinkTypeConverter implements AttributeConverter<LinkType, String> {
    @Override
    public String convertToDatabaseColumn(LinkType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public LinkType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(LinkType.values())
                .filter(c -> c.getCode().equals(dbData))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
