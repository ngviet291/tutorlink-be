package org.group3.tutorlink.features.payment.gateway;
import org.junit.jupiter.api.Test;
import org.group3.tutorlink.common.exception.AppException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class GatewayDiagnosticsTest {
    @Test void distinguishesConnectionFailuresWithoutLeakingMessages() {
        java.io.IOException[] causes = {new java.net.SocketTimeoutException("private"),
            new java.net.UnknownHostException("private"), new javax.net.ssl.SSLHandshakeException("private"),
            new java.net.ConnectException("private")};
        String[] codes = {"TIMEOUT", "DNS", "TLS", "CONNECTION"};
        for (int i=0; i<causes.length; i++) {
            var ex = GatewayHttp.requestFailure(new ResourceAccessException("private", causes[i]));
            assertEquals("[PAYMENT_"+codes[i]+"]", ex.getErrorCode().getMessage());
        }
    }
    @Test void preservesOnlyNumericProviderMetadata() {
        var ex = new RestClientResponseException("private",400,"private",null,
            "{\"resultCode\":13,\"message\":\"private\"}".getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8);
        assertEquals("[PAYMENT_HTTP_REJECTED] HTTP=400 resultCode=13",
            GatewayHttp.requestFailure(ex).getErrorCode().getMessage());
    }
    @Test void handlesEmptyAndMalformedResponses() {
        for (String body : new String[]{null,"","  "})
            assertEquals("[PAYMENT_EMPTY_RESPONSE]", assertThrows(AppException.class, () -> GatewayHttp.parse(body)).getMessage());
        for (String body : new String[]{"null","[]","<html>private</html>","42"})
            assertEquals("[PAYMENT_INVALID_JSON]", assertThrows(AppException.class, () -> GatewayHttp.parse(body)).getMessage());
        assertEquals(0,GatewayHttp.parse("{\"resultCode\":0}").get("resultCode"));
    }
}
