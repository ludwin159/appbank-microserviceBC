package com.bank.appbank.utils;

import com.bank.appbank.model.Client;
import com.bank.appbank.dto.MovementDto.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public class Converters {

    @WritingConverter
    public static class TypeMovementWriteConverter implements Converter<TypeMovement, String> {
        @Override
        public String convert(TypeMovement source) {
            return source.name();
        }
    }

    @ReadingConverter
    public static class TypeMovementReadConverter implements Converter<String, TypeMovement> {
        @Override
        public TypeMovement convert(String source) {
            return TypeMovement.valueOf(source);
        }
    }

    @WritingConverter
    public static class TypeClientWritingConverter implements Converter<Client.TypeClient, String> {
        @Override
        public String convert(Client.TypeClient source) {
            return source.name();
        }
    }

    @ReadingConverter
    public static class TypeClientReadingConverter implements Converter<String, Client.TypeClient> {
        @Override
        public Client.TypeClient convert(String source) {
            return Client.TypeClient.valueOf(source);
        }
    }
}
