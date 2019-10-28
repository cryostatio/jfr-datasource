package com.redhat.jfr.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DatasourceTest {
  @Test
  public void testGet() throws Exception {
    given().when().get("/").then().statusCode(200).body(is(""));
  }
}