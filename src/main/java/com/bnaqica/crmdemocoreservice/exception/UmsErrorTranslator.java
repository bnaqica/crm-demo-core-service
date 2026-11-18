package com.bnaqica.crmdemocoreservice.exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/** Turns UMS error responses into {@link UmsClientException}, so no REST client type leaks upwards. */
public final class UmsErrorTranslator {

    private static final int MAX_BODY = 500;

    private UmsErrorTranslator() {}

    public static void translate(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readNBytes(MAX_BODY), StandardCharsets.UTF_8);
        int status = response.getStatusCode().value();
        throw new UmsClientException(
                status,
                "UMS " + request.getMethod() + " " + request.getURI().getPath() + " returned " + status + ": " + body);
    }
}
