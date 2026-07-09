import bootstrap.AppSetup;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import db.Database;
import db.Repository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Server {
    static void main() {
        Repository db = new Database();

        try {
            if (!db.ifExists())
                AppSetup.run(db);

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/", new baseHandler());
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            IO.println("Server is running on http://localhost:8080");

        } catch (RuntimeException | IOException e) {
            System.err.println("IO Error can't start the server");
            e.printStackTrace();
        }

        // close db when jvm exits
    }

    private static class baseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = "testing".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

        }

    }
}