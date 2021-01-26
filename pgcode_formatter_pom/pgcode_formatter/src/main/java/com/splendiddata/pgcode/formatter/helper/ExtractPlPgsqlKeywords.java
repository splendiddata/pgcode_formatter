/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
 *
 * This program is free software: You may redistribute and/or modify under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at Client's option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, Client should
 * obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.splendiddata.pgcode.formatter.internal.Dicts;

/**
 * Extracts plpgsql keywords from PostgreSQL header files.
 * <p>
 * For the code formatter, keywords may have a special meaning. PostgreSQL has a well defined set of keywords that can
 * be queried with its pg_get_keywords() function. But for plpgsql no such function exists in the database. So they have
 * to come from another place. Luckily PostgreSQL is an open source database, and all plpgsql specific keywords are
 * registered in two header files: src/main/postgres/pl/plpgsql/src/pl_reserved_kwlist.h and
 * src/main/postgres/pl/plpgsql/src/pl_unreserved_kwlist.h. This program extracts the keywords from these header files
 * and writes them into a file that can be picked up by the {@link Dicts} class.
 * <p>
 * When a new version of PostgreSQL is released, please download the source and replace the two header files in the
 * src/main/postgres directories of this project by the newer versions from the PostgreSQL source.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ExtractPlPgsqlKeywords {

    /**
     * Path to the copy of the pl_reserved_kwlist.h file from the PostgreSQL source.
     * <p>
     * Please overwrite that file with the newest version when a new version of PostgreSQL arrives.
     */
    private static final String RESERVED_KW_LIST = "src/main/postgres/pl/plpgsql/src/pl_reserved_kwlist.h";

    /**
     * Path to the copy of the pl_unreserved_kwlist.h file from the PostgreSQL source.
     * <p>
     * Please overwrite that file with the newest version when a new version of PostgreSQL arrives.
     */
    private static final String UNRESERVED_KW_LIST = "src/main/postgres/pl/plpgsql/src/pl_unreserved_kwlist.h";

    /**
     * This is where the extracted keywords will land
     */
    private static final String OUTPUT_PATH = "target/classes/" + Dicts.PLPGSQL_KEYWORDS_PATH;

    /**
     * Pattern to extract the keywords from the header files
     */
    private static final Pattern PG_KEYWORD_PATTERN = Pattern.compile("PG_KEYWORD\\(\"(\\w+)\", \\w+\\)");

    /**
     * Main entry point of the program
     *
     * @param args
     *            not used
     */
    public static void main(String[] args) {
        Path baseDir = getBaseDir();
        Set<String> keywords = new TreeSet<>();

        Path path = Paths.get(baseDir.toString(), RESERVED_KW_LIST);
        try {
            readPostgresKeywords(path, keywords);
            path = Paths.get(baseDir.toString(), UNRESERVED_KW_LIST);
            readPostgresKeywords(path, keywords);
            path = Paths.get(baseDir.toString(), OUTPUT_PATH);
            writeKeywords(path, keywords);
            System.out.println(new StringBuilder().append("Created ").append(path).append(" with ")
                    .append(keywords.size()).append(" keywords").toString());
        } catch (IOException e) {
            System.err.println(ExtractPlPgsqlKeywords.class.getName() + " failed to process file " + path + ":\n" + e);
            e.printStackTrace(System.err);
        }
    }

    /**
     * Figure out the base directory of this project. Source and target files will be relative to this directory
     *
     * @return Path The project directory
     */
    private static Path getBaseDir() {
        Path projectDirectory;
        Object mavenBaseDir = System.getProperties().get("basedir");
        if (mavenBaseDir == null) {
            projectDirectory = Paths.get(".").toAbsolutePath();
            /*
             * May be in the parent directory
             */
            if (Files.isDirectory(Paths.get(projectDirectory.toString(), "pgcode_formatter"))) {
                projectDirectory = Paths.get(projectDirectory.toString(), "pgcode_formatter").toAbsolutePath();
            }
        } else {
            projectDirectory = Paths.get(mavenBaseDir.toString()).toAbsolutePath();
        }
        return projectDirectory;
    }

    /**
     * Extracts keywords from a header file
     * 
     * @param inputPath
     *            Full path to the header file that is to be read now
     * @param keywords
     *            Set of keywords to which the extracted keywords are to be added
     * @throws IOException
     *             when something is wrong with the header file
     */
    private static void readPostgresKeywords(Path inputPath, Set<String> keywords) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(inputPath)) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                Matcher matcher = PG_KEYWORD_PATTERN.matcher(line);
                if (matcher.matches()) {
                    keywords.add(matcher.group(1));
                }
            }
        }
    }

    /**
     * Writes the extracted keywords to the file specified by outputPath
     * <p>
     * Directories are created if needed
     *
     * @param outputPath
     *            points to the file that is to be written.
     * @param keywords
     *            The set of keywords to write into the file
     * @throws IOException
     *             When something is wrong while creating the necessary directories or while writing the output
     */
    private static void writeKeywords(Path outputPath, Set<String> keywords) throws IOException {
        Files.createDirectories(outputPath.getParent());
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            for (String keyword : keywords) {
                out.println(keyword);
            }
        }
    }
}
