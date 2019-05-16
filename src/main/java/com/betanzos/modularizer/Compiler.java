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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Eduardo Betanzos
 * @since 1.0
 */
final class Compiler {

    private static Compiler compiler;

    private String jdkHome;
    private String jdkBinDir;
    private static String javac = "javac";

    static {
        // Definir el nombre el archivo javac en dependencia del sistema operativo
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            javac = "javac.exe";
        }
    }

    private Compiler() {
        jdkHome = System.getProperty("java.home");
        buildJdkBinPath();
    }

    public static Compiler getInstance() {
        if (compiler == null) {
            compiler = new Compiler();
        }

        return compiler;
    }

    /**
     * Construye la ruta al directorio contenedor de los binarios del JDK
     */
    private void buildJdkBinPath() {
        jdkBinDir = new StringBuilder(jdkHome)
                .append(File.separator)
                .append("bin")
                .toString();
    }

    /**
     * Permite conocer si el {@code jdkHomePath} en un JAVA_HOME válido.<br/>
     * <br/>
     * Esto se hace coprobando únicamente que exista el archivo {@code jdkHomePath/bin/java} y que este
     * sea un archivo ejecutable. La comprobación de archivo ejecutable se basa en que el archivo sea
     * ejecutable y que la máquina virtual tenga permiso para ejecutarlo.
     *
     * @param jdkHomePath Ruta al directorio raíz del JDK
     *
     * @return {@code true} si existe el archivo {@code javac} dentro de la ruta {@code jdkHomePath/bin/}
     *         y este es un archivo ejecutable, {@code false} caso contrario.
     */
    public static boolean validateJdkHome(String jdkHomePath) {
        Path javacPath = Paths.get(jdkHomePath, "bin", javac);
        return Files.exists(javacPath) && Files.isExecutable(javacPath);
    }

    public Compiler setJdkHome(String jdkHome) {
        if (!validateJdkHome(jdkHome)) {
            throw new IllegalArgumentException("Invalid JDK_HOME '" + jdkHome + "'");
        }

        this.jdkHome = jdkHome;
        buildJdkBinPath();
        return this;
    }

    public String getJdkHome() {
        return jdkHome;
    }

    /**
     * Compila el descriptor del módulo (archivo module-info.java) cuyo directorio raíz es {@code tergetModuleDir}. Se
     * asume que el descriptor del módulo se encuentra en la misma raíz.
     *
     * @param targetModuleDir Directorio raíz del modulo
     * @param modulePath Valor a usar como {@code --module-path}
     *
     * @return Descripción de los errores de compilación en caso de producirse alguno, {@link Optional#empty()} si no
     *         hubo errores.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public Optional<String> compileModuleDescriptor(String targetModuleDir, String modulePath) throws InterruptedException, IOException {
        // Construir el comando de compilación
        List<String> commandList = new ArrayList<>(6);
        commandList.add(jdkBinDir + File.separator + javac);
        commandList.add("-d");
        commandList.add(targetModuleDir);

        if (modulePath != null) {
            commandList.add("--module-path");
            commandList.add(modulePath);
        }

        commandList.add(targetModuleDir + File.separator + "module-info.java");

        // Ejecutar el comando de compilación
        Process compilerProcess = new ProcessBuilder()
                .command(commandList)
                .start();
        compilerProcess.waitFor(5, TimeUnit.SECONDS);

        // Si hay error de compilación devuelvo la salida de la consola de compilación
        byte[] compileErros = compilerProcess.getErrorStream().readAllBytes();
        if (compileErros.length > 0) {
            return Optional.of("Command: " + getFullCommandStr(commandList) + "\n" + new String(compileErros));
        }

        return Optional.empty();
    }

    private String getFullCommandStr(List<String> command) {
        StringBuilder sb = new StringBuilder();

        boolean firstEntry = true;
        for (String cmdEntry : command) {
            if (!firstEntry) {
                sb.append(" ");
            }

            firstEntry = false;
            sb.append(cmdEntry);
        }

        return sb.toString();
    }
}
