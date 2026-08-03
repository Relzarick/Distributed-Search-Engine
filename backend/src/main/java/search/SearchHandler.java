package search;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SearchHandler implements HttpHandler {
    private final SearchService service;
    private final Logger logger = LoggerFactory.getLogger(SearchHandler.class);

    public SearchHandler(SearchService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String rawQuery = parseQuery(exchange.getRequestURI().getQuery());

            if (rawQuery == null) {
                String error = "Bad Request: Missing or invalid query parameter.";
                sendResponses(exchange, 400, error.getBytes(StandardCharsets.UTF_8));
                return;
            }

            String response = service.find(rawQuery);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            sendResponses(exchange, 200, bytes);
        } catch (IOException e) {
            logger.error("SERVER ERROR: {}", e.getMessage());

            byte[] bytes = "Server Error".getBytes(StandardCharsets.UTF_8);
            sendResponses(exchange, 500, bytes);
        }
    }

    private String parseQuery(String query) {
        if (query == null || !query.startsWith("q="))
            return null;

        return URLDecoder.decode(query.substring(2), StandardCharsets.UTF_8);
    }

    private void sendResponses(HttpExchange exchange, int code, byte[] bytes) throws IOException {
        exchange.sendResponseHeaders(code, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

}