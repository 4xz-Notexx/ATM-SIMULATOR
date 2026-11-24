import java.io.IOException;

/**
 * ServerMain
 * ----------
 * Entry point for the web frontend. Starts the HTTP server that serves the
 * frontend and exposes the existing ATM backend as a small JSON API.
 *
 * NOTE: This does NOT replace the console app. The console app still runs with:
 *       java -cp bin Main
 *
 * Run the web server from the project root:
 *       java -cp bin ServerMain [port]
 * Then open http://localhost:8080 in a browser.
 */
public class ServerMain {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid port '" + args[0] + "', using default 8080.");
            }
        }
        AtmServer server = new AtmServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
