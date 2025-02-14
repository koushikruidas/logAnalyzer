package com.poinciana.loganalyzer.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {
    @Value("${elasticsearch.username}")
    private String elasticsearchUsername;
    @Value("${elasticsearch.password}")
    private String elasticsearchPassword;
    @Value("${elasticsearch.host}")
    private String elasticsearchHost;
    @Bean
    public ElasticsearchClient elasticsearchClient() {

        // 1. Credentials Provider
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword)); // Your credentials

        // 2. Rest Client Builder with Authentication
        RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchHost))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();

        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /**
     * This bean is to conver Epoch time to LocalDateTime.
     * As elasticSearch saves time as Epoch time.
     * */
    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(List.of(
                /**
                 * Below method can be replaced with Lamda expression.
                 * But still keeping it as anonymous method for easy understanding.
                 * */
                new Converter<Long, LocalDateTime>() {
                    @Override
                    public LocalDateTime convert(Long source) {
                        return source == null ? null :
                                LocalDateTime.ofInstant(Instant.ofEpochMilli(source), ZoneId.systemDefault());
                    }
                }
        ));
    }
}