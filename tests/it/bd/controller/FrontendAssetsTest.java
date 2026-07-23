package it.bd.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendAssetsTest {
    @Test
    void declaresAnAvailableFavicon() throws Exception {
        String index = resource("/static/index.html");
        String favicon = resource("/static/favicon.svg");

        assertTrue(index.contains("href=\"/favicon.svg\""));
        assertTrue(favicon.contains("<svg"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = FrontendAssetsTest.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Risorsa non trovata: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
