package com.example.app;

import java.io.IOException;

/** Starts the HTTP server and stops it gracefully on SIGTERM or Ctrl+C. */
public final class Main {

	static {
		// One line per log record, for container logs. It must be set before the first logger is created.
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tFT%1$tT.%1$tLZ %4$s %5$s%6$s%n");
	}

	private static final System.Logger LOG = System.getLogger(Main.class.getName());

	private Main() {
	}

	public static void main(String[] args) throws IOException {
		int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
		App app = App.start(port);
		LOG.log(System.Logger.Level.INFO, "server starting address=:{0,number,#}", app.port());

		// The server's thread keeps the JVM running. On shutdown, requests in progress get up to 10 seconds to
		// finish. The JDK's logging shuts down in its own hook at the same time, so the hook does not log.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> app.stop(10)));
	}
}
