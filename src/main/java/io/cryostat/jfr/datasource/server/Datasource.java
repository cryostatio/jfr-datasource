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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.util.Pair;

import io.cryostat.jfr.datasource.events.RecordingService;
import io.cryostat.jfr.datasource.sys.FileSystemService;

import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("")
public class Datasource {

    private static final String UNSET_FILE = "";
    private volatile String loadedFile = UNSET_FILE;

    @ConfigProperty(name = "quarkus.http.body.uploads-directory")
    String jfrDir;

    @ConfigProperty(name = "io.cryostat.jfr-datasource.timeout", defaultValue = "29000")
    String timeoutMs;

    @Inject RecordingService recordingService;

    @Inject FileSystemService fsService;

    @Inject Logger logger;

    @GET
    @Path("/")
    public void healthCheck() {}

    @Path("/search")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public String search(JsonObject body) {
        try {
            if (body != null && !body.isEmpty()) {
                logger.debug(body.toString());
                return recordingService.search(new Search(body));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("Error: invalid search body", e);
        }
        throw new BadRequestException("Error: invalid search body");
    }

    @Path("/query")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public String query(JsonObject body) {
        try {
            if (body != null && !body.isEmpty()) {
                logger.info(body.toString());
                Query query = new Query(body);
                return (recordingService.query(query));
            }
        } catch (Exception e) {
            throw new BadRequestException("Error: invalid query body", e);
        }

        throw new BadRequestException("Error: invalid query body");
    }

    @Path("/annotations")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String annotations() {
        return recordingService.annotations();
    }

    @Path("/set")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String set(String file) {
        String filePath = jfrDir + File.separator + file;

        return setFile(filePath, file, new StringBuilder());
    }

    @Path("/upload")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String upload(
            @RestForm(FileUpload.ALL) List<FileUpload> files,
            @QueryParam("overwrite") @DefaultValue("false") boolean overwrite)
            throws IOException {
        final StringBuilder responseBuilder = new StringBuilder();

        uploadFiles(files, responseBuilder, overwrite);
        return responseBuilder.toString();
    }

    @Path("/load")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String load(
            @RestForm(FileUpload.ALL) List<FileUpload> files,
            @QueryParam("overwrite") @DefaultValue("false") boolean overwrite)
            throws IOException {
        final StringBuilder responseBuilder = new StringBuilder();

        String lastFile = uploadFiles(files, responseBuilder, overwrite);
        String filePath = jfrDir + File.separator + lastFile;

        return setFile(filePath, lastFile, responseBuilder);
    }

    @Path("/list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String list() {
        try {
            StringBuilder responseBuilder = new StringBuilder();
            for (String filename : listFiles()) {
                if (filename.equals(loadedFile)) {
                    filename = String.format("**%s**", filename);
                }
                responseBuilder.append(filename);
                responseBuilder.append(System.lineSeparator());
            }
            return responseBuilder.toString();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new InternalServerErrorException(e);
        }
    }

    @Path("/current")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String current() {
        logger.infov("Current: {0}", loadedFile);
        return loadedFile + System.lineSeparator();
    }

    @Path("/delete_all")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String deleteAll() {
        final StringBuilder stringBuilder = new StringBuilder();
        try {
            List<String> deletedFiles = deleteAllFiles();
            for (String deletedFile : deletedFiles) {
                stringBuilder.append("Deleted: " + deletedFile);
                stringBuilder.append(System.lineSeparator());
            }
            setLoadedFile(UNSET_FILE);
            return (stringBuilder.toString());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new InternalServerErrorException(e);
        }
    }

    @Path("/delete")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public void delete(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new BadRequestException();
        } else {
            try {
                deleteFile(fileName);
                if (fileName.equals(loadedFile)) {
                    setLoadedFile(UNSET_FILE);
                }
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new NotFoundException();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new InternalServerErrorException();
            }
        }
    }

    private List<String> listFiles() throws IOException {
        final List<String> files = new ArrayList<>();
        java.nio.file.Path dir = fsService.pathOf(jfrDir);
        if (fsService.exists(dir) && fsService.isDirectory(dir)) {
            for (java.nio.file.Path f : fsService.list(dir)) {
                if (fsService.isRegularFile(f)) {
                    files.add(f.getFileName().toString());
                }
            }
        }
        return files;
    }

    private String uploadFiles(
            List<FileUpload> uploads, StringBuilder responseBuilder, boolean overwrite)
            throws IOException {
        String lastFile = "";
        for (FileUpload fileUpload : uploads) {
            Pair<String, java.nio.file.Path> pairHelper = uploadHelper(fileUpload);
            String uploadedFile = pairHelper.left;
            java.nio.file.Path source = pairHelper.right;
            lastFile = uploadedFile;
            java.nio.file.Path dest = source.resolveSibling(fileUpload.fileName());

            if (fsService.exists(dest)) {
                if (overwrite) {
                    logger.infov("{0} exists and will be overwritten.", fileUpload.fileName());
                } else {
                    int attempts = 0;
                    while (fsService.exists(dest) && attempts < 10) {
                        dest =
                                source.resolveSibling(
                                        UUID.randomUUID().toString() + '-' + fileUpload.fileName());
                        attempts++;
                    }
                }
            }

            try {
                if (overwrite) {
                    fsService.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    fsService.move(source, dest);
                }
                logUploadedFile(dest.getFileName().toString(), responseBuilder);
                lastFile = dest.getFileName().toString();
            } catch (IOException e) {
                logUploadedFile(uploadedFile, responseBuilder);
            }
        }

        return lastFile;
    }

    private Pair<String, java.nio.file.Path> uploadHelper(FileUpload fileUpload)
            throws IOException {
        java.nio.file.Path source = fileUpload.filePath();
        long timeout = TimeUnit.MILLISECONDS.toNanos(Long.parseLong(timeoutMs));
        String uploadedFile = source.getFileName().toString();
        long start = System.nanoTime();
        long now = start;
        long elapsed = 0;

        logger.infov(
                "Received request for {0} ({1} bytes)", fileUpload.fileName(), fileUpload.size());

        if (IOToolkit.isCompressedFile(source.toFile())) {
            source = decompress(source);
            now = System.nanoTime();
            elapsed = now - start;
            logger.infov(
                    "{0} was compressed. Decompressed size: {1} bytes. Decompression took {2}ms",
                    fileUpload.fileName(),
                    source.toFile().length(),
                    TimeUnit.NANOSECONDS.toMillis(elapsed));
        }

        now = System.nanoTime();
        elapsed = now - start;
        if (elapsed > timeout) {
            throw new ServerErrorException(504);
        }
        return new Pair<>(uploadedFile, source);
    }

    private java.nio.file.Path decompress(java.nio.file.Path source) throws IOException {
        java.nio.file.Path tmp = Files.createTempFile(null, null);
        try (var stream = IOToolkit.openUncompressedStream(source.toFile())) {
            Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp;
        } finally {
            Files.deleteIfExists(source);
        }
    }

    private void logUploadedFile(String file, StringBuilder responseBuilder) {
        responseBuilder.append("Uploaded: " + file);
        responseBuilder.append(System.lineSeparator());
        logger.infov("Uploaded: {0}", file);
    }

    private void setLoadedFile(String filename) {
        this.loadedFile = filename;
    }

    private String setFile(String absolutePath, String filename, StringBuilder responseBuilder) {
        try {
            recordingService.loadEvents(absolutePath);
            responseBuilder.append("Set: " + filename);
            responseBuilder.append(System.lineSeparator());
            setLoadedFile(filename);
            return (responseBuilder.toString());
        } catch (IOException e) {
            throw new NotFoundException(e);
        }
    }

    private List<String> deleteAllFiles() throws IOException {
        final List<String> deleteFiles = new ArrayList<>();
        java.nio.file.Path dir = fsService.pathOf(jfrDir);
        if (fsService.exists(dir) && fsService.isDirectory(dir)) {
            for (java.nio.file.Path f : fsService.list(dir)) {
                if (fsService.isRegularFile(f)) {
                    fsService.delete(f);
                    deleteFiles.add(f.getFileName().toString());
                    logger.infov("Deleted: {0}", f.getFileSystem().toString());
                }
            }
            setLoadedFile(UNSET_FILE);
        }
        return deleteFiles;
    }

    private void deleteFile(String filename) throws IOException {
        java.nio.file.Path dir = fsService.pathOf(jfrDir);

        if (fsService.exists(dir) && fsService.isDirectory(dir)) {
            if (fsService.deleteIfExists(
                    fsService.pathOf(dir.toAbsolutePath().toString(), filename))) {
                logger.infov("Deleted: {0}", filename);
                if (filename.equals(loadedFile)) {
                    setLoadedFile(UNSET_FILE);
                }
            } else {
                throw new FileNotFoundException(filename + " does not exist");
            }
        } else {
            throw new FileNotFoundException(filename + " does not exist");
        }
    }
}
