package com.example.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The application: routes on the JDK's built-in HTTP server, with no framework. Each request runs on its own
 * virtual thread.
 */
public final class App {

	private static final System.Logger LOG = System.getLogger(App.class.getName());

	/** Static files served under /assets/, by name, with their content types. */
	private static final Map<String, String> ASSETS = Map.of("styles.css", "text/css; charset=utf-8");

	private final HttpServer server;
	private final ExecutorService executor;
	private final String indexPage;

	private App(HttpServer server, ExecutorService executor) {
		this.server = server;
		this.executor = executor;
		this.indexPage = resource("web/index.html").replace("{{javaVersion}}", html(Runtime.version().toString()));
	}

	/** Starts the server on all interfaces; port 0 picks a free port. */
	public static App start(int port) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
		App app = new App(server, executor);
		server.createContext("/", app::handle);
		server.setExecutor(executor);
		server.start();
		return app;
	}

	/** The port the server listens on. */
	public int port() {
		return server.getAddress().getPort();
	}

	/** Stops accepting connections and waits up to the given seconds for requests in progress. */
	public void stop(int delaySeconds) {
		server.stop(delaySeconds);
		executor.close();
	}

	private void handle(HttpExchange exchange) throws IOException {
		long started = System.nanoTime();
		String method = exchange.getRequestMethod();
		String path = exchange.getRequestURI().getPath();
		try (exchange) {
			route(exchange, method, path);
		} catch (RuntimeException e) {
			LOG.log(System.Logger.Level.ERROR, "request failed", e);
			throw e;
		} finally {
			LOG.log(System.Logger.Level.INFO, "request method={0} path={1} status={2} duration={3}ms", method, path,
					String.valueOf(exchange.getResponseCode()), String.valueOf((System.nanoTime() - started) / 1_000_000));
		}
	}

	private void route(HttpExchange exchange, String method, String path) throws IOException {
		String asset = path.startsWith("/assets/") ? path.substring("/assets/".length()) : null;
		boolean known = path.equals("/") || path.equals("/api/status") || path.equals("/healthz")
				|| (asset != null && ASSETS.containsKey(asset));
		if (!known) {
			send(exchange, 404, "text/plain; charset=utf-8", "404 page not found\n");
			return;
		}
		// HEAD gets the same headers as GET, without a body.
		if (!method.equals("GET") && !method.equals("HEAD")) {
			exchange.getResponseHeaders().set("Allow", "GET, HEAD");
			send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed\n");
			return;
		}
		switch (path) {
			case "/" -> send(exchange, 200, "text/html; charset=utf-8", indexPage);
			case "/api/status" -> send(exchange, 200, "application/json; charset=utf-8",
					"{\"status\":\"ok\",\"runtime\":\"" + Runtime.version() + "\",\"server\":\"jdk.httpserver\"}\n");
			case "/healthz" -> send(exchange, 200, "text/plain; charset=utf-8", "ok\n");
			default -> send(exchange, 200, ASSETS.get(asset), resource("web/assets/" + asset));
		}
	}

	private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", contentType);
		boolean head = exchange.getRequestMethod().equals("HEAD");
		// -1 sends no body; HEAD still announces the length GET would send.
		if (head) {
			exchange.getResponseHeaders().set("Content-Length", String.valueOf(bytes.length));
		}
		exchange.sendResponseHeaders(status, head ? -1 : bytes.length);
		if (!head) {
			exchange.getResponseBody().write(bytes);
		}
	}

	private static String resource(String name) {
		try (InputStream in = App.class.getClassLoader().getResourceAsStream(name)) {
			if (in == null) {
				throw new IllegalStateException("missing resource " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String html(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
