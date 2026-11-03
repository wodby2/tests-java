package com.example.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AppTest {

	private static App app;
	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	@BeforeAll
	static void start() throws IOException {
		app = App.start(0);
	}

	@AfterAll
	static void stop() {
		app.stop(0);
	}

	private static HttpResponse<String> request(String method, String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
				.method(method, HttpRequest.BodyPublishers.noBody())
				.build();
		return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@ParameterizedTest(name = "{0} {1}")
	@CsvSource(delimiter = '|', textBlock = """
			GET  | /                  | 200 | Your Java app is running | text/html; charset=utf-8
			GET  | /assets/styles.css | 200 | :root                    | text/css; charset=utf-8
			GET  | /healthz           | 200 | ok                       | text/plain; charset=utf-8
			GET  | /missing           | 404 | 404 page not found       | text/plain; charset=utf-8
			GET  | /assets/../pom.xml | 404 | 404 page not found       | text/plain; charset=utf-8
			POST | /                  | 405 | Method Not Allowed       | text/plain; charset=utf-8
			""")
	void routes(String method, String path, int status, String body, String contentType) throws Exception {
		HttpResponse<String> response = request(method, path);
		assertEquals(status, response.statusCode());
		assertTrue(response.body().contains(body), response.body());
		assertEquals(contentType, response.headers().firstValue("Content-Type").orElse(""));
	}

	@Test
	void homeShowsTheJavaVersion() throws Exception {
		assertTrue(request("GET", "/").body().contains(Runtime.version().toString()));
	}

	@Test
	void statusIsJson() throws Exception {
		HttpResponse<String> response = request("GET", "/api/status");
		assertEquals(200, response.statusCode());
		assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
		assertTrue(response.body().contains("\"status\":\"ok\""), response.body());
		assertTrue(response.body().contains("\"runtime\":\"" + Runtime.version() + "\""), response.body());
	}

	@Test
	void headSendsNoBody() throws Exception {
		HttpResponse<String> response = request("HEAD", "/healthz");
		assertEquals(200, response.statusCode());
		assertEquals("", response.body());
		assertEquals("3", response.headers().firstValue("Content-Length").orElse(""));
	}
}
