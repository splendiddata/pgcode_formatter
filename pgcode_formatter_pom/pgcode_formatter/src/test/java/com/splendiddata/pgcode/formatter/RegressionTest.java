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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.splendiddata.sqlparser.SqlParser;
import com.splendiddata.sqlparser.SqlParserErrorData;
import com.splendiddata.sqlparser.SqlParserErrorReporter;
import com.splendiddata.sqlparser.enums.NodeTag;
import com.splendiddata.sqlparser.structure.CreateFunctionStmt;
import com.splendiddata.sqlparser.structure.DefElem;
import com.splendiddata.sqlparser.structure.DoStmt;
import com.splendiddata.sqlparser.structure.Node;
import com.splendiddata.sqlparser.structure.Value;

/**
 * Walks the src/test/resources/regression/source and src/test/resources/regression/config directories and creates a
 * test case for every combination.
 * <p>
 * The output files are the target/test/regression/out directory.
 * </p>
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class RegressionTest {
    private static final Logger log = LogManager.getLogger(RegressionTest.class);

    private static boolean allOk;
    private static StringBuilder filesInError;
    private static Path projectDirectory;
    private static Path inputBaseDirectory;
    private static Path configDirectory;
    private static Path outputBaseDirectory;
    private static Path expectedBaseDirectory;

    /**
     * Tries to figure out the base path of the project. Different editors / running environments sometimes have
     * different opinions on the current directory. Further it decides on a couple paths that will be used in the tests
     * and it initialises the filesInError StringBuffer so that somewhat nicely formatted test results can be presented.
     * 
     * @throws IOException
     */
    @BeforeAll
    static void beforeAll() throws IOException {
        allOk = true;
        filesInError = new StringBuilder("Not all tests appear to be ok. Please check:");

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
        log.debug(() -> "projectDirectory = " + projectDirectory);

        inputBaseDirectory = Paths.get(projectDirectory.toString(), "src/test/resources/regression/source")
                .toAbsolutePath();
        log.debug(() -> "inputBaseDirectory = " + inputBaseDirectory);
        configDirectory = Paths.get(projectDirectory.toString(), "src/test/resources/regression/config")
                .toAbsolutePath();
        log.debug(() -> "configDirectory = " + configDirectory);
        outputBaseDirectory = Paths.get(projectDirectory.toString(), "target/test/regression/out").toAbsolutePath();
        log.debug(() -> "outputBaseDirectory = " + outputBaseDirectory);
        // Make sure the out directory is fresh, even if no clean has been specified in the maven build
        if (Files.isDirectory(outputBaseDirectory)) {
            Files.walk(outputBaseDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        expectedBaseDirectory = Paths.get(projectDirectory.toString(), "src/test/resources/regression/expected")
                .toAbsolutePath();
        log.debug(() -> "expectedBaseDirectory = " + expectedBaseDirectory);
    }

    /**
     * Prints the files that are different from the expected results
     */
    @AfterAll
    static void afterAll() {
        Assertions.assertTrue(allOk, filesInError.toString());
    }

    /**
     * Builds test cases from the src/test/resources/regression/source and src/test/resources/regression/config
     * directories
     *
     * @return Stream&lt;Case&gt; with for each source - config combination a test base
     */
    static Stream<TestCase> getCases() {
        log.debug("@>getCases()");
        List<Path> sourceFiles = Collections.emptyList();
        List<Path> configFiles = Collections.emptyList();
        TestFileVisitor fileVisitor = new TestFileVisitor();
        try {
            Files.walkFileTree(inputBaseDirectory, fileVisitor);
            sourceFiles = fileVisitor.getFiles();

            fileVisitor.clear();
            Files.walkFileTree(Paths.get(projectDirectory.toString(), "src/test/resources/regression/config"),
                    fileVisitor);
            configFiles = fileVisitor.getFiles();
        } catch (IOException e) {
            log.error(e, e);
        }

        List<TestCase> testCases = new ArrayList<>(sourceFiles.size() * configFiles.size());
        for (Path sourceFile : sourceFiles) {
            for (Path configFile : configFiles) {
                testCases.add(new TestCase(sourceFile, configFile));
            }
        }
        if (log.isDebugEnabled()) {
            if (log.isTraceEnabled()) {
                log.trace("@<getCases() = " + testCases);
            } else {
                log.debug("@<getCases() = " + testCases.size() + " test cases");
            }

        }

        return testCases.stream();
    }

    /**
     * Builds test cases from the src/test/resources/regression/source and src/main/resources/profiles directories
     *
     * @return Stream&lt;Case&gt; with for each source - config combination a test base
     */
    static Stream<TestCase> getProfileCases() {
        log.debug("@>getProfileCases()");
        List<Path> sourceFiles = Collections.emptyList();
        List<Path> configFiles = Collections.emptyList();
        TestFileVisitor fileVisitor = new TestFileVisitor();
        try {
            Files.walkFileTree(inputBaseDirectory, fileVisitor);
            sourceFiles = fileVisitor.getFiles();

            fileVisitor.clear();
            Files.walkFileTree(Paths.get(projectDirectory.toString(), "src/main/resources/profiles"), fileVisitor);
            configFiles = fileVisitor.getFiles();
        } catch (IOException e) {
            log.error(e, e);
        }

        List<TestCase> testCases = new ArrayList<>(sourceFiles.size() * configFiles.size());
        for (Path sourceFile : sourceFiles) {
            for (Path configFile : configFiles) {
                testCases.add(new TestCase(sourceFile, configFile));
            }
        }
        if (log.isDebugEnabled()) {
            if (log.isTraceEnabled()) {
                log.trace("@<getProfileCases() = " + testCases);
            } else {
                log.debug("@<getProfileCases() = " + testCases.size() + " test cases");
            }

        }

        return testCases.stream();
    }

    /**
     * Processes a combination of an input sql file with the specified configuration xml file
     *
     * @param testCase
     *            Containing pathnames for the input, configuration, expected and output file
     * @throws IOException
     *             from testCase.getExpectedFile() if it feels like it
     */
    @ParameterizedTest
    @MethodSource("getCases")
    void testSqlFileWithConfig(TestCase testCase) throws IOException {
        log.info("testSqlFileWithConfig(" + testCase + ")");

        String expected = "";
        boolean inError = false;
        try {
            String input = new String(Files.readAllBytes(testCase.getSourceFile()));
            String output = CodeFormatter
                    .toStringResults(new StringReader(input), new FormatConfiguration(testCase.getConfigFile()))
                    .collect(Collectors.joining());

            if (testCase.getExpectedFile().toFile().exists()) {
                expected = new String(Files.readAllBytes(testCase.getExpectedFile()));
            } else {
                log.info(testCase.getExpectedFile().toAbsolutePath() + " does not exist");
            }

            if (!output.equals(expected)) {
                log.trace("Expected source =\"\n" + expected + "\"\n");
                log.trace("output =\"\n" + output + "\"\n");
                allOk = false;
                inError = true;
                Path outputPath = testCase.getOutputFile();
                filesInError.append("\n    ").append(outputPath);
                Files.createDirectories(outputPath.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                    writer.append(output);
                }
            }

            checkQueries(input, output);
        } catch (IOException e) {
            log.error("Error while processing " + testCase, e);
            Assertions.fail(e.toString(), e);
        } catch (Exception e) {
            log.error("Error while processing " + testCase, e);
            allOk = false;
            filesInError.append("\n    ").append(testCase.getOutputFile());
            Assertions.fail(e.toString(), e);

        } catch (Error e) { // We do want a stack trace here!!!
            log.error("Error while processing " + testCase, e);
            Assertions.fail(e.toString(), e);
        }
        if (inError) {
            Assertions.fail(new StringBuilder().append("Regression: actual output differs from expected in ")
                    .append(testCase).append("\nPlease compare ").append(testCase.getExpectedFile()).append("\nwith ")
                    .append(testCase.getOutputFile()).toString());
        }
    }

    /**
     * Processes a combination of an input sql file with the profile (configuration xml) file
     *
     * @param testCase
     *            Containing pathnames for the input, configuration, expected and output file
     * @throws IOException
     *             from testCase.getExpectedFile() if it feels like it
     */
    @ParameterizedTest
    @MethodSource("getProfileCases")
    void testSqlFileWithProfile(TestCase testCase) throws IOException {
        log.info("testSqlFileWithProfile(" + testCase + ")");

        String expected = "";
        boolean inError = false;
        try {
            String input = new String(Files.readAllBytes(testCase.getSourceFile()));
            String output = CodeFormatter
                    .toStringResults(new StringReader(input), new FormatConfiguration(testCase.getConfigFile()))
                    .collect(Collectors.joining());

            if (testCase.getExpectedFile().toFile().exists()) {
                expected = new String(Files.readAllBytes(testCase.getExpectedFile()));
            } else {
                log.info(testCase.getExpectedFile().toAbsolutePath() + " does not exist");
            }

            if (!output.equals(expected)) {
                log.trace("Expected source =\"\n" + expected + "\"\n");
                log.trace("output =\"\n" + output + "\"\n");
                allOk = false;
                inError = true;
                Path outputPath = testCase.getOutputFile();
                filesInError.append("\n    ").append(outputPath);
                Files.createDirectories(outputPath.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                    writer.append(output);
                }
            }

            checkQueries(input, output);
        } catch (IOException e) {
            log.error("Error while processing " + testCase, e);
            Assertions.fail(e.toString(), e);
        } catch (Exception e) {
            log.error("Error while processing " + testCase, e);
            allOk = false;
            filesInError.append("\n    ").append(testCase.getOutputFile());
            Assertions.fail(e.toString(), e);

        } catch (Error e) { // We do want a stack trace here!!!
            log.error("Error while processing " + testCase, e);
            Assertions.fail(e.toString(), e);
        }
        if (inError) {
            Assertions.fail(new StringBuilder().append("Regression: actual output differs from expected in ")
                    .append(testCase).append("\nPlease compare ").append(testCase.getExpectedFile()).append("\nwith ")
                    .append(testCase.getOutputFile()).toString());
        }
    }

    /**
     * Checks if the parser can identify the same statements of the input and the output
     *
     * @param input
     *            The source before beautification
     * @param output
     *            The source after beautification
     * @throws IOException
     *             Never thrown
     */
    private void checkQueries(String input, String output) throws IOException {
        List<String> inputParseErrors = new ArrayList<>();
        List<String> outputParseErrors = new ArrayList<>();
        SqlParser inputParser = new com.splendiddata.sqlparser.SqlParser(new StringReader(input),
                new PgParserErrorReporter(inputParseErrors));
        SqlParser outputParser = new com.splendiddata.sqlparser.SqlParser(new StringReader(output),
                new PgParserErrorReporter(outputParseErrors));
        boolean parsingInputSucceeded = inputParser.parse();
        boolean parsingOutputSucceeded = outputParser.parse();
        Assertions.assertEquals(inputParseErrors, outputParseErrors,
                "Difference between inputParseErrors and outputParseErrors");
        if (parsingInputSucceeded && parsingOutputSucceeded && inputParser.getResult() != null
                && outputParser.getResult() != null) {
            Assertions.assertEquals(
                    inputParser.getResult().stream().map(stmt -> maskFunctionBody(stmt, inputParseErrors))
                            .map(stmt -> stmt.toString()).collect(Collectors.toList()),
                    outputParser.getResult().stream().map(stmt -> maskFunctionBody(stmt, outputParseErrors))
                            .map(stmt -> stmt.toString()).collect(Collectors.toList()),
                    "Difference between inputParser.getResult() and outputParser.getResult()");
        }
    }

    /**
     * The body of a function may be significantly beautified, so may need to be excluded from equality checking
     * <p>
     * For language SQL functions, the body is parsed and the parsed statements will be re-inserted using the toString()
     * method. Thus the body will be functionally the same as the original body, but in a uniform way and without
     * comment.
     * <p>
     * For language PLPGSQL functions, the body will be replaced by a literal so it compares the same. This is because
     * currently we have no functional PLPGSQL parser.
     * <p>
     * All other languages should be left untouched, so will be left untouched here
     *
     * @param stmt
     *            an sql statement that may be a create function statement
     * @param parseErrors
     *            May be appended to if the body sql statement(s) of a language SQL function have errors
     * @return Node the, potentially altered, stmt
     */
    @SuppressWarnings("unchecked")
    private Node maskFunctionBody(Node stmt, List<String> parseErrors) {
        List<DefElem> defElemList;
        switch (stmt.type) {
        case T_CreateFunctionStmt:
            defElemList = ((CreateFunctionStmt) stmt).options;
            break;
        case T_DoStmt:
            defElemList = ((DoStmt) stmt).args;
            break;
        default:
            return stmt;
        }
        String language = "plpgsql";
        for (DefElem defElem : defElemList) {
            if ("language".equals(defElem.defname)) {
                language = defElem.arg.toString();
            }
        }
        switch (language.toLowerCase()) {
        case "plpgsql":
            for (DefElem defElem : defElemList) {
                Value bodyContent;
                if ("as".equals(defElem.defname)) {
                    if (NodeTag.T_List.equals(defElem.arg.type)) {
                        bodyContent = ((List<Value>) defElem.arg).get(0);
                    } else {
                        bodyContent = (Value) defElem.arg;
                    }
                    bodyContent.val.str = "*** the plpgsql function body cannot be tested by an sql parser only, so is taken out of the comparison here ***";
                }
            }
            break;
        case "sql":
            for (DefElem defElem : defElemList) {
                if ("as".equals(defElem.defname)) {
                    SqlParser parser = new com.splendiddata.sqlparser.SqlParser(
                            new StringReader(((List<Value>) defElem.arg).get(0).val.str),
                            new PgParserErrorReporter(parseErrors));
                    try {
                        if (parser.parse()) {
                            if (parser.getResult() != null) {
                                ((List<Value>) defElem.arg).get(0).val.str = parser.getResult().stream()
                                        .map(sql -> sql.toString()).collect(Collectors.joining("\n"));
                            } else {
                                ((List<Value>) defElem.arg).get(0).val.str = "";
                            }
                        } else {
                            ((List<Value>) defElem.arg).get(0).val.str = "*** some statement with parser errors ***";
                        }
                    } catch (IOException e) {
                        Assertions.fail(e.toString(), e);
                    }
                }
            }
            break;
        default:
            break;
        }
        return stmt;
    }

    private static final class TestCase {
        private final Path sourceFile;
        private final Path configFile;
        private final Path relativeOutputPath;

        /**
         * Constructor
         *
         * @param sourceFile
         *            Some source file to test with
         * @param configFile
         *            Some config file to test with
         */
        public TestCase(Path sourceFile, Path configFile) {
            super();
            this.sourceFile = sourceFile;
            this.configFile = configFile;
            relativeOutputPath = Paths.get(configFile.getFileName().toString().replaceAll("\\.[^.]+$", ""),
                    inputBaseDirectory.relativize(sourceFile).toString());
        }

        /**
         * @return Path the sourceFile
         */
        public Path getSourceFile() {
            return sourceFile;
        }

        /**
         * @return Path the configFile
         */
        public Path getConfigFile() {
            return configFile;
        }

        /**
         * Returns the path to the output file that is to be created by the test
         *
         * @return Path The file into which the output is to be generated
         */
        public Path getOutputFile() {
            return Paths.get(outputBaseDirectory.toString(), relativeOutputPath.toString()).toAbsolutePath();
        }

        /**
         * Returns the path to the file that contains the source file formatted as expected given the settings in the
         * config file.
         * <p>
         * The file may not exist (yet)
         * </p>
         *
         * @return Path The file that should contain the already formatted file to check against
         */
        public Path getExpectedFile() throws IOException {
            return Paths.get(expectedBaseDirectory.toString(), relativeOutputPath.toString()).toAbsolutePath();
        }

        /**
         * @see java.lang.Object#toString()
         *
         * @return String describing the test case
         */
        public String toString() {
            return new StringBuilder().append("TestCase source:")
                    .append(inputBaseDirectory.relativize(sourceFile).toString()).append(" config:")
                    .append(configFile.getFileName()).toString();
        }
    }

    /**
     * FileVisitor that just adds all the files it can find to the files List&lt;Path&gt;
     */
    private static final class TestFileVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> files;

        /**
         * Constructor
         */
        public TestFileVisitor() {
            files = new ArrayList<>();
        }

        /**
         * Adds all simple files to the files list
         *
         * @param file
         *            Potential file to add
         * @param attrs
         *            To determine if this is a regular file
         * @return FileVisitResult.CONTINUE
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (attrs.isRegularFile() && !file.getFileName().toString().startsWith(".")) {
                files.add(file);
            }
            return FileVisitResult.CONTINUE;
        }

        /**
         * Returns all collected files
         *
         * @return List&lt;Path&gt; The collected files
         */
        public List<Path> getFiles() {
            return new ArrayList<Path>(files);
        }

        /**
         * Clears the list of collected files
         */
        public void clear() {
            files.clear();
        }
    }

    /**
     * Error reporter for the SqlParser. It just adds the error text to the provided errorsFound List&lt;String&gt;
     */
    private static final class PgParserErrorReporter implements SqlParserErrorReporter {
        private final List<String> errorsFound;

        /**
         * Constructor
         *
         * @param errorsFound
         *            A list that will be appended to when the parser detects an error
         */
        PgParserErrorReporter(List<String> errorsFound) {
            this.errorsFound = errorsFound;
        }

        /**
         * @see com.splendiddata.sqlparser.SqlParserErrorReporter#reportError(com.splendiddata.sqlparser.SqlParserErrorData)
         *      Appends the error text to the errorsFound list
         * @param err
         *            The error to be reported
         */
        @Override
        public void reportError(SqlParserErrorData err) {
            errorsFound.add(err.getErrorText());
        }

    }
}
