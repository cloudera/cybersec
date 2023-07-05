package com.cloudera.parserchains.queryservice.common.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;

@Slf4j
@UtilityClass
public class RequestUtil {

    public static ResponseEntity<String> proxyRequest(String url, HttpServletRequest request, byte[] body, boolean preservePath, RestTemplate restTemplate) {
        // Build the request headers
        HttpHeaders headers = getHttpHeaders(request);

        // Build the request parameters
        MultiValueMap<String, String> queryParams = getQueryParams(request);

        // Build the request URL with query parameters
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(preservePath ? url + request.getServletPath() : url)
                .queryParams(queryParams);


        // Create the request entity with headers and body (if present)
        RequestEntity<byte[]> requestEntity = new RequestEntity<>(body, headers, HttpMethod.resolve(request.getMethod()), uriBuilder.build().toUri());

        // Make the request to the external service
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.exchange(requestEntity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Error while proxying the request!", ex);
            // If an error occurs, return a custom error message
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error occurred while proxying the request!");
        }

        // Return the response from the external service as is
        return ResponseEntity.status(responseEntity.getStatusCode())
                .headers(responseEntity.getHeaders())
                .body(responseEntity.getBody());
    }

    public static MultiValueMap<String, String> getQueryParams(HttpServletRequest request) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            queryParams.put(paramName, Arrays.asList(paramValues));
        }
        return queryParams;
    }

    public static HttpHeaders getHttpHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.add(headerName, request.getHeader(headerName));
        }
        return headers;
    }

}
