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
package io.cryostat.jfr.datasource.sys;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileSystemService {

    public boolean isDirectory(Path path, LinkOption... linkOptions) {
        return Files.isDirectory(path, linkOptions);
    }

    public boolean isRegularFile(Path path, LinkOption... linkOptions) {
        return Files.isRegularFile(path, linkOptions);
    }

    public List<String> listDirectoryChildren(Path path) throws IOException {
        return Files.list(path).map(p -> p.getFileName().toString()).collect(Collectors.toList());
    }

    public boolean deleteIfExists(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    public void delete(Path path) throws IOException {
        Files.delete(path);
    }

    public boolean exists(Path path, LinkOption... linkOptions) {
        return Files.exists(path, linkOptions);
    }

    public Path pathOf(String first, String... more) {
        return Path.of(first, more);
    }

    public Path move(Path source, Path target, CopyOption... options) throws IOException {
        return Files.move(source, target, options);
    }

    public List<Path> list(Path dir) throws IOException {
        return Files.list(dir).collect(Collectors.toList());
    }
}
