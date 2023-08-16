/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.jfr.datasource.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import io.cryostat.jfr.datasource.sys.FileSystemService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@TestMethodOrder(OrderAnnotation.class)
@QuarkusTest
public class DatasourceTest {

    @InjectSpy FileSystemService fsService;

    @ConfigProperty(name = "quarkus.http.body.uploads-directory")
    String jfrDir;

    @AfterEach
    public void afterEachDatasourceTest() {
        File directory = Path.of(jfrDir).toFile();
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
    @Order(1)
    public void testGet() throws Exception {
        given().when().get("/").then().statusCode(200).body(is("")).headers(Collections.emptyMap());
    }

    @Test
    @Order(2)
    public void testGetCurrent() {
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(System.lineSeparator()))
                .header("content-type", is("text/plain"));
    }

    @Test
    @Order(3)
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
                .header("content-type", is("text/plain"));
    }

    @Test
    @Order(4)
    public void testPostUploadWithOverwrite() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        // Should return the same filename
        given().queryParam("overwrite", Arrays.asList("true"))
                .multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        // There should only be 1 file when /list
        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/list")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));
    }

    @Test
    @Order(5)
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
                .header("content-type", is("text/plain"));

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/list")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        expected = "Set: recording.jfr" + System.lineSeparator();
        given().body("recording.jfr")
                .when()
                .post("/set")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        expected = "**recording.jfr**" + System.lineSeparator();
        given().when()
                .get("/list")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));
    }

    @Test
    @Order(6)
    public void testGetListEmpty() throws Exception {
        String expected = "";
        given().when()
                .get("/list")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));
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
                .header("content-type", is("text/plain"));

        expected = "Set: recording.jfr" + System.lineSeparator();
        given().body("recording.jfr")
                .when()
                .post("/set")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));
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
                .header("content-type", is("text/plain"));

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));
    }

    @Test
    public void testPostLoadWithOverwrite() throws Exception {
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
                .header("content-type", is("text/plain"));

        given().queryParam("overwrite", Arrays.asList("true"))
                .multiPart(jfrFile)
                .when()
                .post("/load")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));
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
                .header("content-type", is("text/plain"));

        expected = "recording.jfr" + System.lineSeparator();
        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        given().body("recording.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(204)
                .body(is(""))
                .header("content-type", is("text/plain"));

        given().when()
                .get("/current")
                .then()
                .statusCode(200)
                .body(is(System.lineSeparator()))
                .header("content-type", is("text/plain"));
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
                .header("content-type", is("text/plain"));

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
                .body(is(expected.trim()))
                .header("content-type", is("application/json"));
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
                .header("content-type", is("text/plain"));

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
                .header("content-type", is("application/json"));
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
                .header("content-type", is("text/plain"));

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
                .header("content-type", is("application/json"));
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
                .header("content-type", is("text/plain"));

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
                .header("content-type", is("application/json"));
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
                .header("content-type", is("text/plain"));

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
                .header("content-type", is("application/json"));
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
                .header("content-type", is("text/plain"));

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
                .header("content-type", is("application/json"));
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
                .header("content-type", is("text/plain"));

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
                .header("content-type", is("application/json"));
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
                .header("content-type", is("text/plain"));

        given().body("recording.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(204)
                .body(is(""))
                .header("content-type", is("text/plain"));
    }

    @Test
    public void testDeleteFileNotExist() throws Exception {
        given().body("recording.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(404)
                .body(is(""))
                .header("content-type", is("text/plain"));
    }

    @Test
    public void testDeleteFileIOFail() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        doThrow(new IOException()).when(fsService).deleteIfExists(Mockito.any(Path.class));

        given().body("recording.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(500)
                .body(is(""))
                .header("content-type", is("text/plain"));
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
                .header("content-type", is("text/plain"));

        expected = "Deleted: recording.jfr" + System.lineSeparator();
        given().when()
                .delete("/delete_all")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        given().when()
                .delete("/delete_all")
                .then()
                .statusCode(200)
                .body(is(""))
                .header("content-type", is("text/plain"));
    }

    @Test
    public void testDeleteAllIOFail() throws Exception {
        File jfrFile = new File("src/test/resources/recording.jfr");
        assertTrue(jfrFile.exists());

        String expected = "Uploaded: recording.jfr" + System.lineSeparator();
        given().multiPart(jfrFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(expected))
                .header("content-type", is("text/plain"));

        doThrow(new IOException()).when(fsService).delete(Mockito.any(Path.class));

        given().when()
                .delete("/delete_all")
                .then()
                .statusCode(500)
                .body(is(""))
                .header("content-type", is("text/plain"));
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
        given().when().get("/annotations").then().statusCode(405);
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
