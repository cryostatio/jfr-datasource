package com.redhat.jfr.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DatasourceTest {

  @AfterEach
  public void afterEachDatasourceTest() {
    File directory = new File("file-uploads");
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

    String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator() + "Set: jmc.cpu.jfr" + System.lineSeparator();
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

    String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator() + "Set: jmc.cpu.jfr" + System.lineSeparator();
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

    String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator() + "Set: jmc.cpu.jfr" + System.lineSeparator();
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

    String expected = "Uploaded: jmc.cpu.jfr" + System.lineSeparator() + "Set: jmc.cpu.jfr" + System.lineSeparator();
    given().multiPart(jfrFile).when().post("/load").then().statusCode(200).body(is(expected));

    File inputFile = new File("src/test/resources/query.table.input.txt");
    assertTrue(inputFile.exists());
    String input = new String(Files.readAllBytes(inputFile.toPath()));

    File outputFile = new File("src/test/resources/query.table.output.txt");

    assertTrue(outputFile.exists());
    expected = new String(Files.readAllBytes(outputFile.toPath()));
    given().body(input).when().post("/query").then().statusCode(200).body(is(expected));
  }

}