import bootstrap.AppSetup;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import db.Database;
import db.Repository;

void main() {

    Repository db = new Database();

    try {
        if (!db.ifExists())
            AppSetup.run(db);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new baseHandler());
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        IO.println("Server is running on http://localhost:8080");

        db.fetch(); // testing
    } catch (AppSetup.AppSetupException e) {
        IO.println(e.getMessage());
    } catch (IOException e) {
        IO.println("IO Error can't start the server: " + e.getMessage());
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