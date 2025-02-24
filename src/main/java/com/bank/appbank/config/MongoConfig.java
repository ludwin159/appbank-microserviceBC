package com.bank.appbank.config;

import com.bank.appbank.utils.Converters;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;
import java.util.List;

@Configuration
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return "bank-products";
    }

    @Bean
    @Override
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = Arrays.asList(
                new Converters.TypeMovementReadConverter(),
                new Converters.TypeMovementWriteConverter(),
                new Converters.TypeClientReadingConverter(),
                new Converters.TypeClientWritingConverter()
        );
        return new MongoCustomConversions(converters);
    }

    @Bean
    @Override
    public MongoClient reactiveMongoClient() {
        return MongoClients.create(mongoUri);
    }
}
