/*
 * Copyright The Cryostat Authors
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.cryostat.jfr.datasource.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
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
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
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
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
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

        expected = "Set: recording.jfr" + System.lineSeparator();
        given().body("recording.jfr")
                .when()
                .post("/set")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostLoad() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testGetList() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
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

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/list")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        expected = "Set: recording.jfr" + System.lineSeparator();
        given().body("recording.jfr")
                .when()
                .post("/set")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        expected = "**recording.jfr**" + System.lineSeparator();
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
    public void testGetCurrentAfterSettingAndAfterDeleting() throws IOException {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"))
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));

        given().body("recording.jfr")
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
    public void testPostSearchEvents() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        File inputFile = new File("src/test/resources/searches/search.events.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/searches/search.events.output.txt");
        assertTrue(outputFile.exists());
        expected = new String(Files.readAllBytes(outputFile.toPath()));

        given().body(input)
                .when()
                .post("/search")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("application/json"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostSearchTarget() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        File inputFile = new File("src/test/resources/searches/search.target.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/searches/search.target.output.txt");
        assertTrue(outputFile.exists());
        expected = new String(Files.readAllBytes(outputFile.toPath()));

        given().body(input)
                .when()
                .post("/search")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("application/json"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostQueryTimeseries() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        File inputFile = new File("src/test/resources/queries/query.timeseries.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/queries/query.timeseries.output.txt");
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
    public void testPostQueryTimeseriesWithParams() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        File inputFile = new File("src/test/resources/queries/query.timeseries.params.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/queries/query.timeseries.params.output.txt");
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
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        File inputFile = new File("src/test/resources/queries/query.table.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile = new File("src/test/resources/queries/query.table.output.txt");

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
    public void testPostQueryRecordingDuration() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        File inputFile = new File("src/test/resources/queries/query.recording_duration.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile =
                new File("src/test/resources/queries/query.recording_duration.output.txt");
        assertTrue(inputFile.exists());
        String output = new String(Files.readAllBytes(outputFile.toPath()));

        given().body(input)
                .when()
                .post("query")
                .then()
                .statusCode(200)
                .body(is(output))
                .header("content-type", is("application/json"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testPostQueryRecordingStartTime() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected =
                "Uploaded: recording.jfr"
                        + System.lineSeparator()
                        + "Set: recording.jfr"
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

        File inputFile =
                new File("src/test/resources/queries/query.recording_start_time.input.txt");
        assertTrue(inputFile.exists());
        String input = new String(Files.readAllBytes(inputFile.toPath()));

        File outputFile =
                new File("src/test/resources/queries/query.recording_start_time.output.txt");
        assertTrue(inputFile.exists());
        String output = new String(Files.readAllBytes(outputFile.toPath()));

        given().body(input)
                .when()
                .post("query")
                .then()
                .statusCode(200)
                .body(is(output))
                .header("content-type", is("application/json"))
                .header("Access-Control-Allow-Methods", is("POST"))
                .header("Access-Control-Allow-Origin", is("*"))
                .header("Access-Control-Allow-Headers", is("accept, content-type"));
    }

    @Test
    public void testDeleteFileExist() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
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

        given().body("recording.jfr")
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
        given().body("recording.jfr")
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
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
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

        expected = "Deleted: recording.jfr" + System.lineSeparator();
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
        given().when().get("/search").then().statusCode(405);
        given().body("{targets: [], range: { from: '', to: ''}}")
                .header("content-type", "application/json")
                .when()
                .get("/query")
                .then()
                .statusCode(405);
        given().when().post("/annotations").then().statusCode(405);
        given().body("recording.jfr").when().get("/set").then().statusCode(405);

        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        given().multiPart(jfrFile).when().get("/upload").then().statusCode(405);
        given().multiPart(jfrFile).when().get("/load").then().statusCode(405);
        given().when().post("/list").then().statusCode(405);
        given().when().post("/current").then().statusCode(405);
        given().when().post("/delete_all").then().statusCode(405);
        given().body("recording.jfr").when().post("/delete").then().statusCode(405);
    }
}
