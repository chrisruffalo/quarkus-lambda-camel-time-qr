package io.github.chrisruffalo.qlctqr;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QrRestTest {

    @Test
    public void simpleDefault() {
        final Response response = RestAssured.given()
                .get("/test/time.png")
                .then()
                .statusCode(200)
                .extract().response();
    }

    @Test
    public void simpleNoFormat() {
        final Response response = RestAssured.given()
                .get("/test/time")
                .then()
                .statusCode(200)
                .extract().response();
    }

    @Test
    public void noMatchingUrl() {
        final Response response = RestAssured.given()
                .get("/test/tiime")
                .then()
                .statusCode(404)
                .extract().response();
    }

    @Test
    public void simpleAscii() {
        final Response response = RestAssured.given()
                .get("/test/time.ascii")
                .then()
                .statusCode(200)
                .extract().response();
    }

    @Test
    public void invertAscii() {
        final Response response = RestAssured.given()
                .get("/test/time.ascii?invert=true")
                .then()
                .statusCode(200)
                .extract().response();
    }

    @Test
    public void paramFormatAscii() {
        final Response response = RestAssured.given()
                .get("/test/time?format=ascii&invert=true")
                .then()
                .statusCode(200)
                .extract().response();
    }

    @Test
    public void simpleHtml() {
        final Response response = RestAssured.given()
                .get("/test/time.html")
                .then()
                .statusCode(200)
                .extract().response();
    }

}
