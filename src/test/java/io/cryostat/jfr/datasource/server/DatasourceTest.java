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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.cryostat.jfr.datasource.sys.FileSystemService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@TestMethodOrder(OrderAnnotation.class)
@QuarkusTest
public class DatasourceTest {
    @InjectMock FileSystemService fsService;

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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
    }

    @Test
    public void testPostLoad() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });
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
    }

    @Test
    public void testGetList() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String target = invocation.getArgument(0);
                                return Path.of(target);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.list(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<List<Path>>() {
                            @Override
                            public List<Path> answer(InvocationOnMock invocation)
                                    throws IOException {
                                Path dir = invocation.getArgument(0);
                                return Files.list(dir).collect(Collectors.toList());
                            }
                        });
        Mockito.when(fsService.isRegularFile(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isRegularFile(target);
                            }
                        });
        ;

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

        expected = "**jmc.cpu.jfr**" + System.lineSeparator();
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
        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String target = invocation.getArgument(0);
                                return Path.of(target);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.list(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<List<Path>>() {
                            @Override
                            public List<Path> answer(InvocationOnMock invocation)
                                    throws IOException {
                                Path dir = invocation.getArgument(0);
                                return Files.list(dir).collect(Collectors.toList());
                            }
                        });
        Mockito.when(fsService.isRegularFile(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isRegularFile(target);
                            }
                        });

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
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.pathOf(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(
                        Path.of(
                                System.getProperty("java.io.tmpdir"),
                                "jfr-file-uploads",
                                "jmc.cpu.jfr"));

        Mockito.when(fsService.deleteIfExists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.deleteIfExists(target);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class))).thenReturn(false);
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class))).thenReturn(false);
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class))).thenReturn(false);
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String target = invocation.getArgument(0);
                                return Path.of(target);
                            }
                        });

        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.pathOf(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String dir = invocation.getArgument(0);
                                String fileName = invocation.getArgument(1);
                                return Path.of(dir, fileName);
                            }
                        });

        Mockito.when(fsService.deleteIfExists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.deleteIfExists(target);
                            }
                        });

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
        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String target = invocation.getArgument(0);
                                return Path.of(target);
                            }
                        });

        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.pathOf(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String dir = invocation.getArgument(0);
                                String fileName = invocation.getArgument(1);
                                return Path.of(dir, fileName);
                            }
                        });

        Mockito.when(fsService.deleteIfExists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.deleteIfExists(target);
                            }
                        });
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
    public void testDeleteFileIOFail() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String target = invocation.getArgument(0);
                                return Path.of(target);
                            }
                        });

        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.pathOf(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String dir = invocation.getArgument(0);
                                String fileName = invocation.getArgument(1);
                                return Path.of(dir, fileName);
                            }
                        });

        Mockito.when(fsService.deleteIfExists(Mockito.any(Path.class)))
                .thenThrow(new IOException());

        given().body("jmc.cpu.jfr")
                .when()
                .delete("/delete")
                .then()
                .statusCode(500)
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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String target = invocation.getArgument(0);
                                return Path.of(target);
                            }
                        });

        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.list(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<List<Path>>() {
                            @Override
                            public List<Path> answer(InvocationOnMock invocation)
                                    throws IOException {
                                Path dir = invocation.getArgument(0);
                                return Files.list(dir).collect(Collectors.toList());
                            }
                        });
        Mockito.when(fsService.isRegularFile(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isRegularFile(target);
                            }
                        });
        doAnswer(
                        new Answer<Object>() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                Files.delete(target);
                                return null;
                            }
                        })
                .when(fsService)
                .delete(Mockito.any(Path.class));

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
    public void testDeleteAllIOFail() throws Exception {
        File jfrFile = new File("src/test/resources/jmc.cpu.jfr");
        assertTrue(jfrFile.exists());

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                String uploadedFileName = invocation.getArgument(0);
                                return Path.of(uploadedFileName);
                            }
                        });
        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.move(Mockito.any(Path.class), Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Path>() {
                            @Override
                            public Path answer(InvocationOnMock invocation) throws IOException {
                                Path source = invocation.getArgument(0);
                                Path dest = invocation.getArgument(1);
                                return Files.move(source, dest);
                            }
                        });

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

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenReturn(Path.of(System.getProperty("java.io.tmpdir"), "jfr-file-uploads"));

        Mockito.when(fsService.exists(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.exists(target);
                            }
                        });
        Mockito.when(fsService.isDirectory(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isDirectory(target);
                            }
                        });
        Mockito.when(fsService.list(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<List<Path>>() {
                            @Override
                            public List<Path> answer(InvocationOnMock invocation)
                                    throws IOException {
                                Path dir = invocation.getArgument(0);
                                return Files.list(dir).collect(Collectors.toList());
                            }
                        });
        Mockito.when(fsService.isRegularFile(Mockito.any(Path.class)))
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws IOException {
                                Path target = invocation.getArgument(0);
                                return Files.isRegularFile(target);
                            }
                        });
        doThrow(new IOException()).when(fsService).delete(Mockito.any(Path.class));

        given().when()
                .delete("/delete_all")
                .then()
                .statusCode(500)
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
