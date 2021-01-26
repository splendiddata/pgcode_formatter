/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
 *
 * This program is free software: You may redistribute and/or modify under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at Client's option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, Client should obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;

/**
 * Checks if the format configuration does not contain any null value
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestFormatConfiguration {
    private static final Logger log = LogManager.getLogger(TestFormatConfiguration.class);

    private static String failedAt = null;
    private static Path projectDirectory;

    @BeforeAll
    public static void beforeAll() {
        log.info("Start TestFormatConfiguration");
        failedAt = null;
        Object mavenBaseDir = System.getProperties().get("basedir");
        if (mavenBaseDir == null) {
            projectDirectory = Paths.get(".").toAbsolutePath();
            /*
             * Find the directory that contains the src directory
             */
            for (projectDirectory = Paths.get(".").toAbsolutePath().getParent(); projectDirectory != null && !Files
                    .isDirectory(Paths.get(projectDirectory.toString(), "src")); projectDirectory = projectDirectory
                            .getParent()) {
                log.trace(() -> "finding projectDirectory = " + projectDirectory);
            }
        } else {
            projectDirectory = Paths.get(mavenBaseDir.toString()).toAbsolutePath();
        }
    }

    @AfterAll
    public static void afterAll() {
        if (failedAt == null) {
            log.info("finish TestFormatConfiguration - ok");
        } else {
            log.info("finish TestFormatConfiguration - failed at: " + failedAt);
        }
    }

    /**
     * Tests a default configuration
     */
    @Test
    public void testDefaultConfiguration() {
        Configuration config = FormatConfiguration.getEffectiveConfiguration();
        log.info("check  FormatConfiguration.getEffectiveConfiguration()");
        checkObject(config, "FormatConfiguration.effectiveConfiguration");
    }

    /**
     * Tests all configuration files it cn find in then src/test/resources/regression/config directory
     */
    @ParameterizedTest
    @MethodSource("getConfigFiles")
    public void regressionConfigurations(Path configPath) {
        FormatConfiguration config = new FormatConfiguration(configPath);
        log.info("check " + configPath);
        checkObject(config, "configPath: ");
    }

    /**
     * Recursively checks all if none of the getters of obj returns null
     *
     * @param obj
     *            The configuration object to check
     * @param path
     *            A couple of spaces to indicate the invocation level
     */
    private final static void checkObject(Object obj, String path) {
        Class<?> cls = obj.getClass();
        log.debug(() -> new StringBuilder().append(path).append(": check object ").append(cls.getName()));
        for (Method method : cls.getDeclaredMethods()) {
            if (method.getParameterCount() > 0
                    && (method.getName().startsWith("get") || method.getName().startsWith("is"))
                    && !method.getReturnType().isPrimitive()) {
                log.trace(() -> new StringBuilder().append(path).append(": method ").append(method.getName())
                        .append(" not tested because it needs arguments"));
                continue;
            }
            if (method.getReturnType().isPrimitive()) {
                log.trace(() -> new StringBuilder().append(path).append(": method ").append(method.getName())
                        .append(" not tested because return type ").append(method.getReturnType())
                        .append(" cannot be null"));
                continue;
            }
            String fldName = method.getName();
            if (fldName.startsWith("get")) {
                fldName = fldName.substring(3);
            } else if (fldName.startsWith("is")) {
                fldName = fldName.substring(2);
            } else {
                log.trace(() -> new StringBuilder().append(path).append(": method ").append(method.getName())
                        .append(" not tested because it is not a getter"));
                continue;
            }
            fldName = fldName.substring(0, 1).toLowerCase() + fldName.substring(1);
            String qualifiedFieldName = path + "." + fldName;
            if (log.isDebugEnabled()) {
                log.debug(new StringBuilder().append("Check ").append(qualifiedFieldName));
            }
            Object invocationResult;
            try {
                invocationResult = method.invoke(obj);
                if (invocationResult == null) {
                    failedAt = qualifiedFieldName;
                    log.error(qualifiedFieldName + " is null");
                }
                Assertions.assertNotNull(invocationResult, qualifiedFieldName);
                if (invocationResult.getClass().getName().startsWith("com.splendiddata")) {
                    checkObject(invocationResult, qualifiedFieldName);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                log.error(new StringBuilder().append(qualifiedFieldName).append(": check method ").append(cls.getName())
                        .append(".").append(method.getName()).append("() failed: ").append(e), e);
                Assertions.fail(
                        new StringBuilder().append(qualifiedFieldName).append(": check method ").append(cls.getName())
                                .append(".").append(method.getName()).append("() failed: ").append(e).toString(),
                        e);
            }
        }
    }

    /**
     * Returns all xml files in the src/test/resources/regression/config directory
     *
     * @return Stream&lt;Path&gt; All config files to test
     * @throws IOException
     *             when applicable
     */
    private static final Stream<Path> getConfigFiles() throws IOException {
        return Files.find(Paths.get(projectDirectory.toString(), "src/test/resources/regression/config"), 10,
                (file, attrs) -> attrs.isRegularFile() && file.getFileName().toString().endsWith(".xml"));
    }
}
