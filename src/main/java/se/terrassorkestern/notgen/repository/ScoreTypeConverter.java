package se.terrassorkestern.notgen.repository;

import se.terrassorkestern.notgen.model.ScoreType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class ScoreTypeConverter implements AttributeConverter<ScoreType, String> {
    @Override
    public String convertToDatabaseColumn(ScoreType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public ScoreType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(ScoreType.values())
                .filter(c -> c.getCode().equals(dbData))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
