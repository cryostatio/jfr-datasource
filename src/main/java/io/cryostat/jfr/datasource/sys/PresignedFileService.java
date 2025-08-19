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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PresignedFileService {

    @ConfigProperty(name = "cryostat.storage.base-uri")
    Optional<String> storageBase;

    @ConfigProperty(name = "cryostat.storage.auth-method")
    Optional<String> storageAuthMethod;

    @ConfigProperty(name = "cryostat.storage.auth")
    Optional<String> storageAuth;

    @ConfigProperty(name = "cryostat.storage.tls-version")
    String storageTlsVersion;

    @ConfigProperty(name = "cryostat.storage.ignore-tls")
    boolean storageTlsIgnore;

    @ConfigProperty(name = "cryostat.storage.verify-hostname")
    boolean storageHostnameVerify;

    @ConfigProperty(name = "cryostat.storage.tls.ca.path")
    Optional<java.nio.file.Path> storageCaPath;

    @ConfigProperty(name = "cryostat.storage.tls.cert.path")
    Optional<java.nio.file.Path> storageCertPath;

    @Inject FileSystemService fsService;
    @Inject Logger logger;

    Path presignDownloadFile;

    public void onStart(@Observes StartupEvent evt) throws IOException {
        this.presignDownloadFile = fsService.createTempFile();
    }

    public Path download(String path, String query)
            throws URISyntaxException, MalformedURLException, IOException {
        if (storageBase.isEmpty()) {
            throw new IllegalStateException();
        }

        UriBuilder uriBuilder =
                UriBuilder.newInstance()
                        .uri(new URI(storageBase.get()))
                        .path(path)
                        .replaceQuery(query);
        URI downloadUri = uriBuilder.build();
        logger.infov("Attempting to download presigned recording from {0}", downloadUri);
        HttpURLConnection httpConn = (HttpURLConnection) downloadUri.toURL().openConnection();
        httpConn.setRequestMethod("GET");
        if (httpConn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) httpConn;
            if (storageTlsIgnore) {
                try {
                    httpsConn.setSSLSocketFactory(
                            ignoreSslContext(storageTlsVersion).getSocketFactory());
                } catch (Exception e) {
                    logger.error(e);
                    throw new IllegalStateException(e);
                }
            } else if (storageCaPath.isPresent() || storageCertPath.isPresent()) {
                if (!(storageCaPath.isPresent() && storageCertPath.isPresent())) {
                    Exception e =
                            new IllegalStateException(
                                    String.format(
                                            "%s and %s must be both set or both unset",
                                            "cryostat.storage.tls.ca.path",
                                            "cryostat.storage.tls.cert.path"));
                    logger.error(e);
                    throw new IllegalStateException(e);
                }
                try {
                    httpsConn.setSSLSocketFactory(
                            trustSslCertContext(
                                            storageTlsVersion,
                                            storageCaPath.get(),
                                            storageCertPath.get())
                                    .getSocketFactory());
                } catch (Exception e) {
                    logger.error(e);
                    throw new IllegalStateException(e);
                }
            }
            if (!storageHostnameVerify) {
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            }
        }
        if (storageAuthMethod.isPresent() && storageAuth.isPresent()) {
            httpConn.setRequestProperty(
                    "Authorization",
                    String.format("%s %s", storageAuthMethod.get(), storageAuth.get()));
        }

        try (var stream = httpConn.getInputStream();
                var bis = new BufferedInputStream(stream)) {
            Files.copy(bis, presignDownloadFile, StandardCopyOption.REPLACE_EXISTING);
            logger.infov("Downloaded {0} to {1}", downloadUri, presignDownloadFile);
            return presignDownloadFile;
        } finally {
            httpConn.disconnect();
        }
    }

    private static SSLContext ignoreSslContext(String tlsVersion) throws Exception {
        SSLContext sslContext = SSLContext.getInstance(tlsVersion);
        sslContext.init(
                null, new X509TrustManager[] {new X509TrustAllManager()}, new SecureRandom());
        return sslContext;
    }

    private static SSLContext trustSslCertContext(
            String tlsVersion, java.nio.file.Path caPath, java.nio.file.Path certPath)
            throws IOException,
                    KeyStoreException,
                    KeyManagementException,
                    CertificateException,
                    NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream ca = new FileInputStream(caPath.toFile());
                InputStream cert = new FileInputStream(certPath.toFile()); ) {
            keyStore.load(null, null);
            keyStore.setCertificateEntry("storage-ca", certFactory.generateCertificate(ca));
            keyStore.setCertificateEntry("storage-tls", certFactory.generateCertificate(cert));

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslCtx = SSLContext.getInstance(tlsVersion);
            sslCtx.init(null, trustManagerFactory.getTrustManagers(), null);

            return sslCtx;
        }
    }

    private static final class X509TrustAllManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {}

        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    }
}
