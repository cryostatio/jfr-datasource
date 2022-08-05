package io.cryostat.jfr.datasource.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import io.quarkus.test.junit.NativeImageTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
@NativeImageTest
public class NativeDatasourceIT {

    @AfterEach
    public void afterEachDatasourceTest() {
        File directory = new File(System.getProperty("java.io.tmpdir"), "jfr-file-uploads");
        if (directory.exists() && directory.isDirectory()) {
            for (File f : directory.listFiles()) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }
        directory.delete();
    }

    @Order(1)
    @Test
    public void testGet() throws Exception {
        given().when().get("/").then().statusCode(200).body(is("")).headers(Collections.emptyMap());
    }

    @Order(2)
    @Test
    public void testGetCurrent() {
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(System.lineSeparator()))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostUpload() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostSet() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        expected = "Set: jmc.cpu.jfr" + System.lineSeparator();
        given().body("jmc.cpu.jfr")
                .when()
                .post("/set")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostLoad() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: jmc.cpu.jfr"
                        + System.lineSeparator()
                        + "Set: jmc.cpu.jfr"
                        + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/load")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testGetList() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        expected = "jmc.cpu.jfr" + System.lineSeparator();
        given().when()
                .get("/list")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testGetListEmpty() throws Exception {
        String expected = "";
        given().when()
                .get("/list")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testGetCurrentAfterSettingAndAfterDeleting() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: jmc.cpu.jfr"
                        + System.lineSeparator()
                        + "Set: jmc.cpu.jfr"
                        + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/load")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        expected = "jmc.cpu.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        given().body("jmc.cpu.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(204)
                .body(is(""))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("DELETE"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(System.lineSeparator()))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testGetSearch() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: jmc.cpu.jfr"
                        + System.lineSeparator()
                        + "Set: jmc.cpu.jfr"
                        + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/load")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        File outputFile = new File("src/test/resources/search.output.txt");
        assertTrue(outputFile.exists());

        expected = new String(Files.readAllBytes(outputFile.toPath()));
        given().when()
                .get("/search")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("application/json"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostQueryTimeseries() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: jmc.cpu.jfr"
                        + System.lineSeparator()
                        + "Set: jmc.cpu.jfr"
                        + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/load")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        File inputFile = new File("src/test/resources/query.timeseries.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/query.timeseries.output.txt");

        assertTrue(outputFile.exists());
        expected = new String(Files.readAllBytes(outputFile.toPath()));
        given().body(input)
                .when()
                .post("/query")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("application/json"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostQueryTable() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: jmc.cpu.jfr"
                        + System.lineSeparator()
                        + "Set: jmc.cpu.jfr"
                        + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/load")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        File inputFile = new File("src/test/resources/query.table.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/query.table.output.txt");

        assertTrue(outputFile.exists());
        expected = new String(Files.readAllBytes(outputFile.toPath()));
        given().body(input)
                .when()
                .post("/query")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("application/json"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testDeleteFileExist() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        given().body("jmc.cpu.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(204)
                .body(is(""))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("DELETE"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testDeleteFileNotExist() throws Exception {
        given().body("jmc.cpu.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(404)
                .body(is(""))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("DELETE"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testDeleteAllFiles() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        expected = "Deleted: jmc.cpu.jfr" + System.lineSeparator();
        given().when()
                .delete("/delete_all")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("DELETE"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
        given().when()
                .delete("/delete_all")
                .then()
                .statusCode(200)
                .body(is(""))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("DELETE"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testNotAllowedMethods() {
        given().when().post("/").then().statusCode(405);
        given().when().post("/search").then().statusCode(405);
        given().body("{targets: [], range: { from: '', to: ''}}")
                .header("content-type", "application/json")
                .when()
                .get("/query")
                .then()
                .statusCode(405);
        given().when().post("/annotations").then().statusCode(405);
        given().body("jmc.cpu.jfr").when().get("/set").then().statusCode(405);

        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        given().multiPart(jfrFile).when().get("/upload").then().statusCode(405);
        given().multiPart(jfrFile).when().get("/load").then().statusCode(405);
        given().when().post("/list").then().statusCode(405);
        given().when().post("/current").then().statusCode(405);
        given().when().post("/delete_all").then().statusCode(405);
        given().body("jmc.cpu.jfr").when().post("/delete").then().statusCode(405);
    }
}
