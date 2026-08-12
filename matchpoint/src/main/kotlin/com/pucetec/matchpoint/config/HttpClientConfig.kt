package com.pucetec.matchpoint.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class HttpClientConfig(
    @Value("\${services.users.timeout-millis}") private val timeoutMillis: Long
) {
    @Bean
    fun restClientBuilder(): RestClient.Builder {
        val timeout = Duration.ofMillis(timeoutMillis)
        val httpClient = HttpClient.newBuilder().connectTimeout(timeout).build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(timeout)
        return RestClient.builder().requestFactory(requestFactory)
    }
}
