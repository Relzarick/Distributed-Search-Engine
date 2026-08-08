package search;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SearchHandler implements HttpHandler {
    private final SearchService service;
    private final Logger logger = LoggerFactory.getLogger(SearchHandler.class);

    public SearchHandler(SearchService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getQuery());
            String searchQuery = queryParams.get("q");

            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                String error = "Bad Request: Missing or invalid query parameter.";
                sendResponses(exchange, 400, error.getBytes(StandardCharsets.UTF_8));
                return;
            }

            int size = checkParam(queryParams, "size");
            int offset = checkParam(queryParams, "offset");

            size = Math.min(100, size);
            String response = service.find(searchQuery, offset, size);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            sendResponses(exchange, 200, bytes);
        } catch (IOException e) {
            logger.error("SERVER ERROR: {}", e.getMessage());

            byte[] bytes = "Server Error".getBytes(StandardCharsets.UTF_8);
            sendResponses(exchange, 500, bytes);
        }
    }

    /**
     * Parses the raw query into a map.
     *
     * @param query Is the raw HTTP request
     */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();

        if (query == null || query.isEmpty())
            return params;

        String[] pairs = query.split("&");

        for (String pair : pairs) {
            int idx = pair.indexOf("=");

            if (idx > 0 && pair.length() > idx + 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            } else if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                params.put(key, "");
            }
        }
        return params;
    }

    private int checkParam(Map<String, String> queryParams, String param) {
        if (queryParams.containsKey(param)) {
            try {
                int output = Integer.parseInt(queryParams.get(param));

                return Math.max(output, 0);
            } catch (NumberFormatException e) {
                logger.warn("Invalid {} provided, defaulting to 0", param);
            }
        }

        return 0;
    }

    private void sendResponses(HttpExchange exchange, int code, byte[] bytes) throws IOException {
        exchange.sendResponseHeaders(code, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

}