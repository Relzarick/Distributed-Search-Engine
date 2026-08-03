import bootstrap.AppSetup;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import indexer.InvertedIndexer;
import indexer.tokenizer.StemTokenization;
import mongo.Database;
import mongo.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.SearchHandler;
import search.SearchService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        try {
            Repository db = new Database();
            InvertedIndexer indexer = new InvertedIndexer(new StemTokenization());

            if (!db.ifExists())
                AppSetup.run(db, indexer);

            SearchService search = new SearchService(db, indexer);

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            start(server, new SearchHandler(search));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop(0);

                try {
                    db.close();
                    logger.info("Database closed.");
                } catch (Exception e) {
                    logger.error("Error while closing database: {}", e.getMessage());
                }
            }));

        } catch (RuntimeException | IOException e) {
            logger.error("IO Error can't start the server.");
        }
    }

    private static void start(HttpServer server, HttpHandler handler) {
        server.createContext("/search", handler);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        logger.info("Server is running on http://wretch:8080");
    }

}