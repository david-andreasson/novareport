package com.novareport.reporter_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(
        @Value("${webclient.timeout.connect:5}") long connectTimeoutSeconds,
        @Value("${webclient.timeout.read:10}") long readTimeoutSeconds
    ) {
        TcpClient tcpClient = TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(connectTimeoutSeconds).toMillis());

        HttpClient httpClient = HttpClient.from(tcpClient)
            .responseTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build())
            .build();
    }
}
