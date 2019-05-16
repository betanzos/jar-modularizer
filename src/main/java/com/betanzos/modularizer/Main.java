/**
 * MIT License
 *
 * Copyright (c) 2019 Eduardo E. Betanzos Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.betanzos.modularizer;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Access point to the application.
 *
 * @author Eduardo Betanzos
 * @since 1.0
 */
public final class Main {

    private static final String prodName = "JarModularizer";
    private static final String version = "1.0.1";
    private static final String helpText;

    // TODO Mover a un archivo aparte
    static File descriptorFile;
    static File sourceDir;
    static File destDir;
    static String modulePath;
    static String jdkHome;

    private static boolean showHelp;
    private static boolean showVersion;

    /**
     * Private for avoid class instantiation
     */
    private Main() {}

    static {
        helpText = new StringBuilder()
                .append("usage: java -jar jar-modularizer.jar --descriptor <path> --source <path>\n")
                .append("                                     [--dest <path>] [--module-path <path-group>] [--jdk-home <path>]\n")
                .append("                                     [--version] [--help, -h]\n")
                .append("\n")
                .append("Wellcome to ").append(prodName).append("!\n")
                .append("------------------------------------------\n")
                .append("Version: ").append(version).append("\n")
                .append("\n")
                .append("mandatory arguments:\n")
                .append(getParamHelpLine("--descriptor <path>", "Path to modularization descriptor file."))
                .append(getParamHelpLine("--source <path>", "Path to directory containing source JAR files."))
                .append("\n")
                .append("optional arguments:\n")
                .append(getParamHelpLine("--dest <path>", "Path to modularized JAR files destination directory. Will be created is not exist. Default is --source/mods."))
                .append(getParamHelpLine("--module-path <path-group>", "Path group of directories and/or files containing depending modules."))
                .append(getParamHelpLine("--jdk-home <path>", "Path to JDK root directory. Default is the result of call System.getProperty(\"java.home\")"))
                .append(getParamHelpLine("--version", "Display program version and exit."))
                .append(getParamHelpLine("--help, -h", "Display this help and exit."))
                .append("\n")
                .append("Copyright (c) 2019 Eduardo E. Betanzos Morales")
                .toString();
    }

    private static String getParamHelpLine(String param, String description) {
        return String.format("  %-27s %s%n", param, description);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println();
            System.out.println("Invalid execution");
            System.out.println();
            System.out.println("Run with --help or -h");
            return;
        }

        parseArgs(args);

        // Mostrar la ayuda y terminar
        if (showHelp) {
            showHelp();
            return;
        }

        // Mostrar la versión y terminar
        if (showVersion) {
            showVersion();
            return;
        }

        // Si se han pasado todos los parámetros obligatorios se inicia el proceso
        if (descriptorFile != null && sourceDir != null) {
            if (destDir == null) {
                destDir = new File(sourceDir, "mods");
            }

            Modularizer modularizer = new Modularizer();

            long startTime = System.currentTimeMillis();

            try {
                if (!modularizer.start()) {
                    // Si entra aquí significa que hubo errores durante el proceso, pero quizás algunos
                    // jars pudieron ser modularizados
                    System.out.println("--------------------------------------------------------------------");
                    System.out.println("  Process finish with some non fatal erros. Maybe some JAR files were modularized.");
                } else {
                    System.out.println();
                    System.out.println("--------------------------------------------------------------------");
                    System.out.println("  SUCCESSFUL!!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println();
                System.out.println("--------------------------------------------------------------------");
                System.out.println("  Process finish with ERROR :(");
            }

            long endTime = System.currentTimeMillis();

            System.out.println();
            System.out.printf("  %d JARs modularized in %s%n", modularizer.getCountModularized(), getDuration(endTime, startTime));
            System.out.printf("  %d errors found%n%n", modularizer.getCountErrorFounds());
        } else {
            System.out.println();
            System.out.println("Invalid execution. Mandatory params must be passed.");
            System.out.println();
            System.out.println("Run with --help or -h");
            return;
        }
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String item = args[i];

            if (item.equals("--help") || item.equals("-h")) {
                showHelp = true;
                break;
            } else if (item.equals("--version")) {
                showVersion = true;
            } else if (item.equals("--descriptor")) {
                descriptorFile = new File(args[++i]);

                if (!descriptorFile.exists()) {
                    System.out.println("[ERROR] Descriptor file not exist (" + descriptorFile + ")");
                    descriptorFile = null;
                    return;
                }

                if (!descriptorFile.isFile()) {
                    System.out.println("[ERROR] Descriptor is not a file (" + descriptorFile + ")");
                    descriptorFile = null;
                    return;
                }
            } else if (item.equals("--source")) {
                sourceDir = new File(args[++i]);

                if (!sourceDir.exists()) {
                    System.out.println("[ERROR] Source directory not exist (" + sourceDir + ")");
                    sourceDir = null;
                    return;
                }

                if (!sourceDir.isDirectory()) {
                    System.out.println("[ERROR] Source is not a directory (" + sourceDir + ")");
                    sourceDir = null;
                    return;
                }
            } else if (item.equals("--dest")) {
                destDir = new File(args[++i]);

                if (!sourceDir.isDirectory()) {
                    System.out.println("[ERROR] Destination is not a directory (" + destDir + ")");
                    destDir = null;
                    return;
                }
            } else if (item.equals("--module-path")) {
                modulePath = args[++i];
            } else if (item.equals("--jdk-home")) {
                jdkHome = args[++i];

                if (!Compiler.validateJdkHome(jdkHome)) {
                    System.out.println("[WARN] Invalid JDK_HOME '" + jdkHome + "'. Default will be used.");
                    jdkHome = null;
                }
            }
        }
    }

    private static void showHelp() {
        System.out.println(helpText);
    }

    private static void showVersion() {
        System.out.println("Version: " + version);
    }

    private static String getDuration(long startMillis, long endMillis) {
        String rawDuration = Duration.of(endMillis - startMillis, ChronoUnit.MILLIS).toString();

        return rawDuration
                .replaceAll("PT-", "")
                .replaceAll("H-", "h ")
                .replaceAll("M-", "m ")
                .replaceAll("S", "s");
    }
}
