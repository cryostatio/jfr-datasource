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
import java.util.ArrayList;
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
        given().when().get("/").then().statusCode(200).body(is(""));
    }

    @Order(2)
    @Test
    public void testGetCurrentEndpoint() {
        given().when().get("/current").then().statusCode(200).body(is(System.lineSeparator()));
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
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));
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
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

        expected = "Set: jmc.cpu.jfr" + System.lineSeparator();
        given().body("jmc.cpu.jfr").when().post("/set").then().statusCode(200).body(is(expected));

        expected = "jmc.cpu.jfr" + System.lineSeparator();
        given().when().get("/current").then().statusCode(200).body(is(expected));
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
        given().multiPart(jfrFile).when().post("/load").then().statusCode(200).body(is(expected));

        expected = "jmc.cpu.jfr" + System.lineSeparator();
        given().when().get("/current").then().statusCode(200).body(is(expected));
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
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

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
                                final List<Path> files = new ArrayList<>();
                                Path dir =
                                        Path.of(
                                                System.getProperty("java.io.tmpdir"),
                                                "jfr-file-uploads");
                                for (Path file : Files.list(dir).collect(Collectors.toList())) {
                                    files.add(file);
                                }
                                return files;
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
        given().when().get("/list").then().statusCode(200).body(is(expected));

        expected = "Set: jmc.cpu.jfr" + System.lineSeparator();
        given().body("jmc.cpu.jfr").when().post("/set").then().statusCode(200).body(is(expected));

        expected = "**jmc.cpu.jfr**" + System.lineSeparator();
        given().when().get("/list").then().statusCode(200).body(is(expected));
    }

    @Test
    public void testGetListEmpty() throws Exception {
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
                                final List<Path> files = new ArrayList<>();
                                Path dir =
                                        Path.of(
                                                System.getProperty("java.io.tmpdir"),
                                                "jfr-file-uploads");
                                for (Path file : Files.list(dir).collect(Collectors.toList())) {
                                    files.add(file);
                                }
                                return files;
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
        given().when().get("/list").then().statusCode(200).body(is(expected));
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

        String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator();
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

        expected = "Set: jmc.cpu.jfr" + System.lineSeparator();
        given().body("jmc.cpu.jfr").when().post("/set").then().statusCode(200).body(is(expected));

        expected = "jmc.cpu.jfr" + System.lineSeparator();
        given().when().get("/current").then().statusCode(200).body(is(expected));

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

        given().body("jmc.cpu.jfr").when().delete("/delete").then().statusCode(204).body(is(""));

        given().when().get("/current").then().statusCode(200).body(is(System.lineSeparator()));
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
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

        Mockito.when(fsService.pathOf(Mockito.anyString()))
                .thenReturn(Path.of(System.getProperty("java.io.tmpdir"), "jfr-file-uploads"));

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

        given().body("jmc.cpu.jfr").when().delete("/delete").then().statusCode(204).body(is(""));
    }

    @Test
    public void testDeleteFileNotExist() throws Exception {
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
        given().body("jmc.cpu.jfr").when().delete("/delete").then().statusCode(404).body(is(""));
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
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

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
        Mockito.when(fsService.pathOf(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(
                        Path.of(
                                System.getProperty("java.io.tmpdir"),
                                "jfr-file-uploads",
                                "jmc.cpu.jfr"));

        Mockito.when(fsService.deleteIfExists(Mockito.any(Path.class)))
                .thenThrow(new IOException());

        given().body("jmc.cpu.jfr").when().delete("/delete").then().statusCode(500).body(is(""));
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
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

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
                                final List<Path> files = new ArrayList<>();
                                Path dir =
                                        Path.of(
                                                System.getProperty("java.io.tmpdir"),
                                                "jfr-file-uploads");
                                for (Path file : Files.list(dir).collect(Collectors.toList())) {
                                    files.add(file);
                                }
                                return files;
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
        given().when().delete("/delete_all").then().statusCode(200).body(is(expected));
        given().when().delete("/delete_all").then().statusCode(200).body(is(""));
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
        given().multiPart(jfrFile).when().post("/upload").then().statusCode(200).body(is(expected));

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
                                final List<Path> files = new ArrayList<>();
                                Path dir =
                                        Path.of(
                                                System.getProperty("java.io.tmpdir"),
                                                "jfr-file-uploads");
                                for (Path file : Files.list(dir).collect(Collectors.toList())) {
                                    files.add(file);
                                }
                                return files;
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

        given().when().delete("/delete_all").then().statusCode(500).body(is(""));
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
