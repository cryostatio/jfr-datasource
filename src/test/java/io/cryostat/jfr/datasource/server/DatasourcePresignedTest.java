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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import io.cryostat.jfr.datasource.events.RecordingService;
import io.cryostat.jfr.datasource.sys.PresignedFileService;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class DatasourcePresignedTest {

    @InjectMock PresignedFileService presignedFileService;
    @InjectMock RecordingService recordingService;

    @AfterEach
    void cleanup() {
        given().when().delete("/delete_all");
    }

    @Test
    public void testDownloadPresignedFile() throws Exception {
        String path = "/some/path";
        String query = "my=query&foo=bar";
        String absPath = "/path/to/presigned.file";

        File file = Mockito.mock(File.class);
        Mockito.when(file.getAbsolutePath()).thenReturn(absPath);
        Path filePath = Mockito.mock(Path.class);
        Mockito.when(filePath.toFile()).thenReturn(file);
        Mockito.when(presignedFileService.download(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(filePath);
        Mockito.doNothing().when(recordingService).loadEvents(Mockito.anyString());

        given().multiPart("path", path)
                .multiPart("query", query)
                .when()
                .post("/load_presigned")
                .then()
                .statusCode(200);

        Mockito.verify(presignedFileService, Mockito.times(1)).download(path, query);
        Mockito.verify(recordingService, Mockito.times(1)).loadEvents(absPath);
    }

    @Test
    public void testDownloadPresignedFileFailure() throws Exception {
        String path = "/some/path";
        String query = "my=query&foo=bar";

        Mockito.when(presignedFileService.download(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new IOException());
        Mockito.doNothing().when(recordingService).loadEvents(Mockito.anyString());

        given().multiPart("path", path)
                .multiPart("query", query)
                .when()
                .post("/load_presigned")
                .then()
                .statusCode(500);

        Mockito.verify(presignedFileService, Mockito.times(1)).download(path, query);
    }
}
