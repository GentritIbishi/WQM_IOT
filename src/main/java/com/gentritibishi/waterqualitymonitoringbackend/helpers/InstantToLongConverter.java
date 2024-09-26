package com.gentritibishi.waterqualitymonitoringbackend.helpers;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.Instant;

@ReadingConverter
public class InstantToLongConverter implements Converter<Instant, Long> {
    @Override
    public Long convert(Instant source) {
        return source.toEpochMilli();
    }
}
