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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

/**
 * Main class for the code formatter. Formats a single (plpg)sql file according to provided (or default) settings.
 * <p>
 * If a configuration file is provided, then settings from that file are used. For any missing configuration option,
 * default options will be used.
 * </p>
 * <p>
 * If an input file is specified, then that file is used as input. Otherwise the input is expected to come from stdin
 * </p>
 * <p>
 * If an output file is specified, then the output will be written to that file. Otherwise stdout will be used
 * </p>
 * <p>
 * If the option "-S" is specified, then the provided configuration file will be stored in the user preferences. This
 * will be used in future calls when configuration file is not provided
 * </p>
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public final class PlPgSqlCodeFormatterMain {
    private static final Logger log = LogManager.getLogger(PlPgSqlCodeFormatterMain.class,
            StringFormatterMessageFactory.INSTANCE);

    /**
     * Command line option --help
     */
    public static final String OPTION_HELP = "help";
    
    /**
     * Command line option --config
     */
    public static final String OPTION_CONFIG = "config";
    
    /**
     * Command line option --input
     */
    public static final String OPTION_INPUT = "input";
    
    /**
     * Command line option --output
     */
    public static final String OPTION_OUTPUT = "output";
    
    private static final String OPTION_STORE_CONFIG = "store-config";

    private static final Preferences PREFS = Preferences.userNodeForPackage(PlPgSqlCodeFormatterMain.class);
    private static final String PREF_CONFIG_PROFILE_NAME = "config_profile_name";
    private static final String PREF_CONFIG_PATH = "config_path";
    private static final String PREF_CONFIG_XML_CONTENT = "config_xml_content";

    private static Path configPath;
    private static String configFileContent;
    private static String configXmlString;
    private static InputStream in = System.in;
    private static OutputStream out = System.out;

    private static Map<String, String> configProfiles;

    /**
     * The effective configuration to use for the lifetime of the JVM.
     * This configuration is supposed to be read-only and several static
     * references to it may exist. Please don't change!
     */
    public static FormatConfiguration config = null;

    /**
     * Available configuration profiles.
     */
    static {
        configProfiles = Map.ofEntries(Map.entry("compact", "/compact.xml"), Map.entry("profile1", "/profile1.xml"),
                Map.entry("profile2", "/profile2.xml"), Map.entry("profile3", "/profile3.xml"),
                Map.entry("profile4", "/profile4.xml"));
    }

    /**
     * No instances
     *
     * @throws UnsupportedOperationException
     *             in all cases
     */
    private PlPgSqlCodeFormatterMain() {
        throw new UnsupportedOperationException("No instances for " + PlPgSqlCodeFormatterMain.class.getName());
    }

    /**
     * Main entry point for the command line interface
     *
     * @param args
     *            As configured in {@link #interpretCommandLine(String[])}
     */
    public static void main(String[] args) {
        log.info("@>main(" + Arrays.asList(args).stream().collect(Collectors.joining(" ")) + ")");

        if (interpretCommandLine(args)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                if (configPath != null) {
                    if (configXmlString == null) {
                        // Use provided config file
                        config = new FormatConfiguration(configPath);
                    } else {
                        // Use profile config file
                        config = new FormatConfiguration(configXmlString, configPath.toString());
                    }
                } else {
                    // Use config file from user preferences
                    config = new FormatConfiguration(configFileContent, null);
                }

                // Clear reference to string
                configXmlString = null;
                CodeFormatter.toStringResults(reader, config).forEach(result -> {
                    try {
                        writer.append(result);
                    } catch (IOException e) {
                        log.error(e, e);
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                log.error(e, e);
                System.out.println(e);
            }
        }
        log.info("@<main()");
    }

    /**
     * Interprets the command line options
     *
     * @param args
     *            as defined within this method
     * @return boolean true if ok
     */
    private static boolean interpretCommandLine(String[] args) {

        CommandLine commandLine;

        Options options = new Options();
        options.addOption(Option.builder("?").longOpt(OPTION_HELP).desc("Help on the commandline options").build());
        options.addOption(Option.builder("c").longOpt(OPTION_CONFIG).hasArg().desc(
                "configuration.xml file that tells about how to format the sources. An example can be found in /com/splendiddata/plpgsql/code/formatter/DefaultConfig.xml in the jar")
                .build());
        options.addOption(Option.builder("i").longOpt(OPTION_INPUT).hasArg()
                .desc("The input (plpg)sql source file. If not provided, stdin will be used").build());
        options.addOption(Option.builder("o").longOpt(OPTION_OUTPUT).hasArg()
                .desc("The formatted output (plpg)sql source file. If not provided, stdout will be used").build());
        options.addOption(Option.builder("S").longOpt(OPTION_STORE_CONFIG)
                .desc("The provided configuration file, if any, will be stored in user preferences. "
                        + "This will be used in future calls when configuration file is not provided")
                .build());

        try {
            CommandLineParser parser = new DefaultParser();
            commandLine = parser.parse(options, args);

            if (commandLine.hasOption(OPTION_HELP) || commandLine.getOptions().length == 0) {
                printCommandLineUsage(PlPgSqlCodeFormatterMain.class.getSimpleName(), options, true, null);
                return false;
            }

            if (commandLine.hasOption(OPTION_INPUT)) {
                in = Files.newInputStream(Paths.get(commandLine.getOptionValue(OPTION_INPUT)));
            } else {
                log.error("Input file is missing. Please provide an input file and try again");
                printCommandLineUsage(PlPgSqlCodeFormatterMain.class.getSimpleName(), options, true,
                        "Input file is missing. Please provide an input file and try again");
                return false;
            }

            if (commandLine.hasOption(OPTION_CONFIG)) {
                String optionValue = commandLine.getOptionValue(OPTION_CONFIG);
                configPath = getConfigPath(optionValue, commandLine);
                if (log.isDebugEnabled()) {
                    log.debug(PlPgSqlCodeFormatterMain.class + ": trying to format "
                            + commandLine.getOptionValue(OPTION_INPUT) + " using config file " + configPath);
                }

            } else {
                configFileContent = PREFS.get(PREF_CONFIG_XML_CONTENT, null);
                if (configFileContent == null) {
                    log.warn("no configuration found in user preferences.");
                }
                if (log.isDebugEnabled()) {
                    log.debug(PREFS.get(PREF_CONFIG_PROFILE_NAME, null) != null ? PlPgSqlCodeFormatterMain.class
                            + ": trying to format " + commandLine.getOptionValue(OPTION_INPUT)
                            + " using config profile " + PREFS.get(PREF_CONFIG_PROFILE_NAME, null) : "");
                }
            }

            if (commandLine.hasOption(OPTION_OUTPUT)) {
                Path outputPath = Paths.get(commandLine.getOptionValue(OPTION_OUTPUT)).toAbsolutePath();
                Files.createDirectories(outputPath.getParent());
                out = Files.newOutputStream(outputPath);
            }
        } catch (IOException e) {
            log.error("interpretCommandLine(" + Arrays.asList(args).stream().collect(Collectors.joining(" ")) + ")", e);
            System.out.println(e);
            return false;
        } catch (BackingStoreException e) {
            log.warn("A problem occurred while trying to modify user preferences");
            printCommandLineUsage(PlPgSqlCodeFormatterMain.class.getSimpleName(), options, true,
                    "A problem occurred while trying to modify user preferences");
            return false;
        } catch (Exception e) {
            log.error("commandline parse error", e);
            System.out.println(e);
            printCommandLineUsage(PlPgSqlCodeFormatterMain.class.getSimpleName(), options, true, null);
            return false;
        }
        return true;
    }

    /**
     * Returns the Path to the provided config xml file or null when a profile name is provided instead. When a profile
     * name is provided, the config file content configFileContent will be set. In addition, the config info will be
     * stored in the user preferences when the option "-S" is provided.
     *
     * @param optionValue
     *            The provided config option value
     * @param commandLine
     * @return The Path to the provided config xml file or null when a profile name is provided instead
     * @throws IOException
     *             if an I/O error occurs
     * @throws BackingStoreException
     *             A preferences operation could not completed.
     */
    private static Path getConfigPath(String optionValue, CommandLine commandLine)
            throws IOException, BackingStoreException {
        Path result = null;
        Path profilesDirectory = Paths.get("profiles");
        StringBuilder contentBuilder = new StringBuilder();
        String profileName = "";
        switch (optionValue) {
        case "profile1":
        case "profile2":
        case "profile3":
        case "profile4":
        case "compact":
            result = Paths.get(profilesDirectory.toString() + configProfiles.get(optionValue));
            InputStream configXmlStream = PlPgSqlCodeFormatterMain.class.getClassLoader()
                    .getResourceAsStream(result.toString());
            configXmlString = new String(configXmlStream.readAllBytes(), StandardCharsets.UTF_8);
            contentBuilder.append(configXmlString);
            profileName = optionValue;

            clearPreferences(commandLine.hasOption(OPTION_STORE_CONFIG));
            if (commandLine.hasOption(OPTION_STORE_CONFIG)) {
                PREFS.put(PREF_CONFIG_PROFILE_NAME, profileName);
            }
            break;
        default:
            result = Paths.get(optionValue);
            if (!Files.exists(result)) {
                throw new NoSuchFileException(result.toString());
            }

            try (Stream<String> stream = Files.lines(result, StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            }
            clearPreferences(commandLine.hasOption(OPTION_STORE_CONFIG));
            break;
        }

        if (commandLine.hasOption(OPTION_STORE_CONFIG)) {
            PREFS.put(PREF_CONFIG_PATH, result.toString());
            PREFS.put(PREF_CONFIG_XML_CONTENT, contentBuilder.toString());
            PREFS.flush();
        }

        return result;
    }

    /**
     * Clears User Preferences
     *
     * @param clearPreferences
     *            Indicates whether user prefences should be cleared.
     * @throws BackingStoreException
     *             A preferences operation could not completed.
     */
    private static void clearPreferences(boolean clearPreferences) throws BackingStoreException {
        if (clearPreferences) {
            PREFS.clear();
            PREFS.flush();
        }
    }

    /**
     * Prints the command line usage
     * 
     * @param applicationName
     *            main class name
     * @param options Of type Options
     * @param message Additional text that has to be printed
     */
    private static void printCommandLineUsage(String applicationName, Options options, boolean printOptions,
            String message) {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter writer = new PrintWriter(stringWriter)) {
            writer.println();
            final HelpFormatter usageFormatter = new HelpFormatter();
            if (message != null) {
                usageFormatter.printWrapped(writer, 80, message);
            }
            usageFormatter.printUsage(writer, 80, applicationName, options);
            if (printOptions) {
                usageFormatter.printOptions(writer, 80, options, 4, 2);
            }
            writer.flush();
            log.info(stringWriter.toString());
            System.out.println(stringWriter.toString());
        } catch (IOException e) {
            log.error("printCommandLineUsage()->failed", e);
        }
    }
}
