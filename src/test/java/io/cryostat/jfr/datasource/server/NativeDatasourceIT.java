package io.cryostat.jfr.datasource.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import io.quarkus.test.junit.NativeImageTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

    @Test
    public void testGet() throws Exception {
        given().when().get("/").then().statusCode(200).body(is(""));
    }

    @Test
    public void testPostUpload() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));
    }

    @Test
    public void testPostSet() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

        expected = "Set: jmc.cpu.jfr" + System.lineSeparator();
        given().body("jmc.cpu.jfr").when().post("/set").then().statusCode(200).body(is(expected));
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
        given().multiPart(jfrFile).when().post("/load").then().statusCode(200).body(is(expected));
    }

    @Test
    public void testGetList() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

        expected = "jmc.cpu.jfr" + System.lineSeparator();
        given().when().get("/list").then().statusCode(200).body(is(expected));
    }

    @Test
    public void testGetListEmpty() throws Exception {
        String expected = "";
        given().when().get("/list").then().statusCode(200).body(is(expected));
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
        given().multiPart(jfrFile).when().post("/load").then().statusCode(200).body(is(expected));

        File outputFile = new File("src/test/resources/search.output.txt");
        assertTrue(outputFile.exists());

        expected = new String(Files.readAllBytes(outputFile.toPath()));
        given().when().get("/search").then().statusCode(200).body(is(expected));
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
        given().multiPart(jfrFile).when().post("/load").then().statusCode(200).body(is(expected));

        File inputFile = new File("src/test/resources/query.timeseries.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/query.timeseries.output.txt");

        assertTrue(outputFile.exists());
        expected = new String(Files.readAllBytes(outputFile.toPath()));
        given().body(input).when().post("/query").then().statusCode(200).body(is(expected));
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
        given().multiPart(jfrFile).when().post("/load").then().statusCode(200).body(is(expected));

        File inputFile = new File("src/test/resources/query.table.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/query.table.output.txt");

        assertTrue(outputFile.exists());
        expected = new String(Files.readAllBytes(outputFile.toPath()));
        given().body(input).when().post("/query").then().statusCode(200).body(is(expected));
    }

    @Test
    public void testDeleteFileExist() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

        given().body("jmc.cpu.jfr").when().delete("/delete").then().statusCode(204).body(is(""));
    }

    @Test
    public void testDeleteFileNotExist() throws Exception {
        given().body("jmc.cpu.jfr").when().delete("/delete").then().statusCode(404).body(is(""));
    }

    @Test
    public void testDeleteAllFiles() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

        expected = "Deleted: jmc.cpu.jfr" + System.lineSeparator();
        given().when().delete("/delete_all").then().statusCode(200).body(is(expected));
        given().when().delete("/delete_all").then().statusCode(200).body(is(""));
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
