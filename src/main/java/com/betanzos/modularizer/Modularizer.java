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

import com.betanzos.modularizer.pojo.Artifact;
import com.betanzos.modularizer.pojo.Module;
import com.betanzos.modularizer.tda.Tree;
import com.betanzos.modularizer.tda.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author Eduardo Betanzos
 * @since 1.0
 */
class Modularizer {
    private Set<Artifact> artifactSet;
    private List<Artifact> artifactList;
    private List<File> jarFilesList;
    private int countModularized = 0;
    private int countErrorFounds = 0;

    private Compiler compiler;

    public Modularizer() {

    }

    /**
     * Inicia el proceso de modularización.
     *
     * @return {@code true} si el proceso terminó sin errores, {@code false} en caso contrario.
     *
     * @throws ParseException Si ocurrió algún error deserializando el archivo descriptor de modularización
     */
    public boolean start() throws ParseException {
        System.out.println();
        System.out.println("Starting modularization process...");
        System.out.println("--------------------------------------------------------------------");
        System.out.println();

        parseDescriptor();

        if (artifactSet.isEmpty()) {
            System.out.println("Empty descriptor.");
            return false;
        }

        File[] sourceJarFiles = Main.sourceDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (sourceJarFiles.length == 0) {
            System.out.println("There are no JAR files in source directory");
            return false;
        }

        jarFilesList = Arrays.asList(sourceJarFiles);

        processJars();

        return countErrorFounds == 0;
    }

    /**
     * @return Permite obtener la cantidad de archivos modularizados
     */
    public int getCountModularized() {
        return countModularized;
    }

    /**
     * @return Permite obtener la cantidad de errores no fatales encontrados durante el proceso de modularización.
     *         Estos errores son aquellos relacionados con la modularización de algún archivo JAR en particular pero
     *         que no impiden que se continúe el proceso con el resto de archivos.
     */
    public int getCountErrorFounds() {
        return countErrorFounds;
    }

    /**
     * Deserializa el archivo JSON descriptor de modularización.
     *
     * @implNote Para cargar los artefactos definidos en el descriptor se utiliza un {@link HashSet} para evitar los
     *           duplicados. Esto provoca que si un artefacto es definido múltiples veces solo se cargará el primero
     *           de ellos. Un artefacto se considera duplicado si se repite el {@code name}.
     *
     * @throws ParseException Si ocurre algún error que impida la deserialización.
     */
    private void parseDescriptor() throws ParseException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Se utiliza un LinkedHashSet para garantizar que
            Type type = mapper.getTypeFactory().constructCollectionType(LinkedHashSet.class, Artifact.class);
            artifactSet = mapper.readValue(Main.descriptorFile, mapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            throw new ParseException("[ERROR] Error parsing modularization descriptor file. " + e.getMessage(), e);
        }
    }

    /**
     * Procesa los archivos JAR encontrados en el directorio {@code sourceDir} para generar su correspondiente JAR
     * modularizado. La modularización solo se lleva a cabo con aquellos archivos para los cuales exista un entrada
     * correspondiente en el descriptor de modularización.<br/>
     * <br/>
     * Las entradas en descriptor de modularización serán analizadas para determinar el orden en el cual deben ser
     * procesados los JARs, teniendo en cuenta las dependencias entre ellos. Aquellos que menos niveles de dependencias
     * tengan serán procesados primero y los que más niveles dependencias tengan de último. Cuando hablamos de niveles de
     * dependencias nos referimos a que si el artefacto A depende de B y este a su vez depende de C, el primero en ser
     * modularizado será C, luego B y por último A.<br/>
     * <br/>
     * Es importante aclarar que las únicas dependencias que cuentan para los fines explicados arriba son aquellas que
     * hacen referncia a los módulos que deseamos crear (aquellos cuya definición está declarada en el descriptor de
     * modularización), nunca las que referencian a terceros módulos ya existentes.
     */
    private void processJars() {
        // Crear la instancia del compilador
        try {
            compiler = Compiler.getInstance();
            if (Main.jdkHome != null) {
                compiler.setJdkHome(Main.jdkHome);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
        System.out.println("[INFO] Using JDK_HOME: " + compiler.getJdkHome());
        System.out.println();

        // Antes de modularizar el JAR es necesario primero ordenar los artefactos de acuerdo a sus dependencias para
        // asegurarnos de que antes de modularizar un artefacto ya han sido modularizados todos aquellos de los que este
        // depende
        sortArtifacts();

        // Modularizar cada uno de los JARs
        artifactList.forEach(a -> {
             jarFilesList.stream()
                    .filter(file -> a.getName().equals(file.getName()))
                    .findFirst()
                    .ifPresent(file -> {
                        if (!modularizeJar(file, a)) {
                            countErrorFounds++;
                        }
                    });
        });
    }

    /**
     * Ordena artefactos definidos en el descriptor de modularización teniendo en cuenta las dependencias entre ellos.
     * Aquellos artefactos que menos niveles de dependencias tengan terminarán siendo primeros que aquellos que más niveles
     * dependencias tengan.<br/>
     * <br/>
     * Cuando hablamos de niveles de dependencias nos referimos a que si el artefacto A depende de B y este a su vez
     * depende de C, primero irá C, luego B y por último A.<br/>
     * <br/>
     * Es importante aclarar que las únicas dependencias que cuentan para los fines explicados arriba son aquellas que
     * hacen referncia a los módulos que deseamos crear (aquellos cuya definición está declarada en el descriptor de
     * modularización), nunca las que referencian a terceros módulos ya existentes.<br/>
     * <br/>
     * Los artefectos ordenados serán agregados a {@link Modularizer#artifactList}.
     *
     * @implNote Para lograr este orden se agregan cada unos de los artefactos contenidos en {@link Modularizer#artifactSet}
     *           en un árbol de dependencias, quedando en los niveles superiores aquellos que más niveles de dependencias
     *           tengan.
     */
    private void sortArtifacts() {
        BiPredicate<Artifact, Artifact> equalsArtifacByModuleNamePredicate = (a1, a2) -> {
            if (a1.getModule() != null && a2.getModule() != null) {
                return a1.getModule().getName().equals(a2.getModule().getName());
            }

            return false;
        };

        // Crear el árbol de dependencias con una raíz ficticia ya que no contiene datos. Esto
        // se hace para que todos los nodos con la misma profundidad en su decendencia se encuentren
        // al mismo nivel
        Tree<Artifact> dependenciesTree = new Tree(new TreeNode(new Artifact()));
        for (Artifact artifact : artifactSet) {
            // Busco si en el árbol ya existe un artefacto que defina el mismo módulo que el
            // artefacto analizado. Si esto ocurre es porque se trata del mismo artefacto.
            TreeNode<Artifact> artifactNode = dependenciesTree.findNodeByData(artifact, equalsArtifacByModuleNamePredicate);

            // Si no es parte del árbol de dependencias creo un nuevo nodo
            if (artifactNode == null) {
                artifactNode = new TreeNode(artifact);

                // Lo agrego como hijo de la raíz
                dependenciesTree.getRoot().getChildren().add(artifactNode);
            }

            // Esto es un truco para poder utilizar la referencia artifactNode dentro de expresiones lambda
            // puesto que al no ser final o efectivamente final me lanza error
            AtomicReference<TreeNode<Artifact>> artifactNodeReference = new AtomicReference<>(artifactNode);

            // Agregar como hijos los módulos de los que depende el artefacto actual y
            // que se encuentran definidos cómo módulos por otros artefactos
            Set<String> requiresModules = artifact.getModule().getRequiresModules();
            if (requiresModules != null) {
                for (String moduleName : requiresModules) {
                    artifactSet.stream()
                            // Busco si el módulo requerido es definido por alguno de los artefactos
                            .filter(a -> a.getModule().getName().equals(moduleName))
                            .findFirst()
                            // Si encuentro el artefacto que lo define...
                            .ifPresent(a -> {
                                // Busco si el artefacto ya existe en el árbol de dependencias
                                TreeNode<Artifact> requireModuleNode = dependenciesTree.findNodeByData(a, equalsArtifacByModuleNamePredicate);

                                if (requireModuleNode == null) {
                                    // Si no existe agrego el artefacto encontrado como hijo del artefacto actual
                                    artifactNodeReference.get().getChildren().add(new TreeNode<>(a));
                                } else {
                                    // Si ya existe en el árbol...
                                    int currentArtifactLevel = dependenciesTree.getNodeLevel(artifactNodeReference.get());
                                    int requiredArtifactLevel = dependenciesTree.getNodeLevel(requireModuleNode);

                                    // Si el nivel en que se encuentra el artefacto requerido es menor o igual
                                    // que el nivel donde se encuentra el artefacto actual, se lo quito al padre
                                    // y lo agrego como hijo del artefacto actual.
                                    // En caso contrario no es necesario hacerlo ya que si el nivel del requerido
                                    // es mayor, este será modularizado antes
                                    if (requiredArtifactLevel <= currentArtifactLevel) {
                                        dependenciesTree.removeSubtree(requireModuleNode);
                                        artifactNodeReference.get().getChildren().add(requireModuleNode);
                                    }
                                }
                            });

                }
            }
        }

        // Obtener el listado de ordenado de los artefactos según deben ser modularizados
        // Los primeros deben ser los que se hayan en el nivel más profundo
        int treeLevel = dependenciesTree.getTreeLevel();
        artifactList = new ArrayList<>();
        for (int i = treeLevel; i > 0; i--) {
            List<TreeNode<Artifact>> nodesAtLevel = dependenciesTree.getNodesAtLevel(i);
            for (TreeNode<Artifact> item : nodesAtLevel) {
                artifactList.add(item.getData());
            }
        }
    }

    /**
     * Modulariza el archivo JAR {@code file} de acuerdo con la definición de módulo especificada en el descriptor de
     * modularización.
     *
     * @param file Ruta al archivo JAR a modularizar.
     * @param artifact Objeto que contiene los datos de la entrada correspondiente al archivo JAR en el descriptor de
     *                 modularización.
     *
     * @return {@code true} si la modularización se completó satisfactoriamente, {@code false} en caso contrario.
     */
    private boolean modularizeJar(File file, Artifact artifact) {
        // Esto es necesario para poder utilizar la variable dentro de expresiones lambda
        // ya que se utilizamos directamente el tipo File no podremos hacerlo, ya que no
        // sería final o efectivamente final
        final AtomicReference<File> tempArtifactDir = new AtomicReference<>();

        try {
            JarFile jarFile = new JarFile(file);

            tempArtifactDir.set(new File(Main.destDir, file.getName() + "-temp"));
            if (!tempArtifactDir.get().mkdirs()) {
                throw new RuntimeException("Can not create temp dir '" + tempArtifactDir + "'.");
            }

            Set<String> nonEmptyPackages = new HashSet<>();

            // Validar que el jar no tenga al menos una definición de módulo
            jarFile.stream()
                    .filter(entry -> entry.getName().contains("module-info.class"))
                    .findFirst()
                    .ifPresent(entry -> {
                        throw new RuntimeException("JAR file contains al least one module definition.");
                    });

            // Extraer el contenido del archivo JAR
            jarFile.stream()
                    // Ordenar las entradas para que los directorios siempre aparezcan antes de los archivos que contiene
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .forEach(entry -> {
                        File entryOutFile = new File(tempArtifactDir.get() + File.separator + entry.getName().replaceAll("/", "\\" + File.separator));

                        if (!entry.isDirectory()) {
                            // Agregar el paquete que contiene la clase al conjunto de paquetes candidatos a exportar
                            if (entry.getName().endsWith(".class")) {
                                int lastSlashIndex = entry.getName().lastIndexOf('/');
                                nonEmptyPackages.add(entry.getName().substring(0, lastSlashIndex).replaceAll("/", "."));
                            }

                            // Escribir el archivo en el disco duro
                            try (OutputStream fos = new FileOutputStream(entryOutFile)) {
                                fos.write(jarFile.getInputStream(entry).readAllBytes());
                            } catch (IOException e) {
                                throw new RuntimeException(e.getMessage(), e);
                            }
                        } else {
                            // Crear todos los directorios que indique la entrada
                            entryOutFile.mkdirs();
                        }
                    });

            // Generar el archivo module-info.class
            byte[] moduleInfoData = null;
            try {
                moduleInfoData = generateModuleDescriptor(tempArtifactDir.get(), artifact.getModule(), nonEmptyPackages);
            } catch (IOException e) {
                throw new IOException("Error generating module descriptor. " + e.getMessage());
            }
            if (moduleInfoData == null) {
                throw new RuntimeException("Can not to compile module-info.java");
            }

            // Agregar el descriptor del módulo al JAR
            patchJar(file, moduleInfoData);

            System.out.println("[INFO] '" + file.getName() + "' modularized to module '" + artifact.getModule().getName() + "'");
            countModularized++;
            return true;
        } catch (IOException e) {
            System.out.println("[ERROR] I/O error modularizing JAR file '" + file.getName() + "'. " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error modularizing JAR file '" + file.getName() + "'. " + e.getMessage());
        } finally {
            if (tempArtifactDir.get() != null) {
                try {
                    recursiveRemove(tempArtifactDir.get());
                } catch (Exception e) {
                    System.out.println("[WARN] Error while remove temp dir '" + tempArtifactDir.get().getName() + "'. " + e.getMessage());
                }
            }
        }

        return false;
    }

    /**
     * Permite eliminar un archivo o directorio. Si {@code file} representa un directorio, se eliminarán recursivamente
     * todos los archivos y subdirectorios que este contenga. Si el archivo/directorio no existe, la llamada a este
     * método no tendrá efecto.
     *
     * @param file Archivo o directorio a eliminar.
     */
    private void recursiveRemove(File file) {
        try {
            if (file.isDirectory()) {
                String[] dirEntries = file.list();
                for (String entry : dirEntries) {
                    recursiveRemove(new File(file + File.separator + entry));
                }
            }

            Files.deleteIfExists(file.toPath());
        } catch (Exception e) {
            throw new RuntimeException("Can not remove " + (file.isFile() ? "file " : "directory ") + file);
        }
    }

    /**
     * Genera el descriptor del módulo {@code module} en el directorio definido por {code outputDir}.<br/>
     * <br/>
     * El proceso inicia creando la definición del descriptor del módulo, un archivo module-info.java. Para agregar
     * las directivas {@code exports} se utiliza la definición hecha en el descriptor de modularización. Si dicha
     * definición no fue hecha entonces se agregará una directiva {@code exports} para todos los paquetes del JAR
     * original que contengan al menos un archivo {code .class}. Para agregar las directivas {@code requires} se
     * utilizará la definición hecha en el descriptor de modularización y en caso de no existir no se agregará ninguna
     * directiva de este tipo.<br/>
     * <br/>
     * Una vez creado el descriptor del módulo (archivo module-info.java) este es compilado utilido el jdk sobre el
     * cual se está ejecutando este programa.<br/>
     * <br/>
     * El proceso de compilación puede fallar si los módulos de los cuales depende este módulo (según las directivas
     * {@code requires} definidas) no son visibles por el compilador. Por defecto se agrega al comando de compilación
     * el parámetro {@code --module-path <destDir>}, donde {@code destDir} es el directorio donde se depositarán los
     * nuevos JARs modularizados, el cual fue definido utilizando parámetro {@code --dest} de este programa. Esto nos
     * asegura que si el modulo que estamos modularizando actualmente depende de algún otro módulo que vamos a
     * modularizar, este sea visible para el compilador (esto requiere que primero se modularice el módulo del cual
     * se depende y luego se modularice este). Si adicionalmente el módulo a modularizar depende de otros ya existentes
     * se puede utilizar el parámetro {@code --module-path} al ejecutar la aplicación para agregar cualquier otro
     * directorio y/o archivos (este parámetro tiene la misma sintaxis del homónimo en {@code java}, {@code javac},
     * {@code jlink} y demás herramientas del JDK).
     *
     * @param outputDir Directorio en donde se debe generar el archivo module-info.java. Debe ser el directorio raíz en
     *                  el cual se extrajo el contenido del archivo JAR a modularizar.
     * @param module Objeto con la definición del módulo.
     * @param jarNonEmptyPackages Listado de los paquetes contenidos en el archivo JAR que al menos contiene una archivo
     *                            .class. Si {@code module.exportsPackages == null} se agregará una entrada del tipo
     *                            {@code exports package.name} para cada uno de los elementos de este listado.
     *
     * @return Cotenido del archivo module-info.class correspondiente al archivo module-info.java compilado.
     *
     * @throws IOException Si ocurre un error escribiendo el archivo module-info.java en el disco duro.
     */
    private byte[] generateModuleDescriptor(File outputDir, Module module, Set<String> jarNonEmptyPackages) throws IOException {
        // Crear el contenido del descriptor
        final StringBuilder builder = new StringBuilder("module ")
                .append(module.getName())
                .append(" {")
                .append((char) Character.LINE_SEPARATOR);

        if (module.getExportsPackages() != null) {
            module.getExportsPackages().forEach(p -> builder.append("    exports ")
                    .append(p)
                    .append(";")
                    .append((char) Character.LINE_SEPARATOR)
            );
        } else {
            // Si no se ha especificado la lista de paquetes a exportar se exportarán todos
            // aquellos paquetes que contengan archivos de clase
            jarNonEmptyPackages.forEach(p -> builder.append("    exports ")
                    .append(p)
                    .append(";")
                    .append((char) Character.LINE_SEPARATOR)
            );
        }

        if (module.getRequiresModules() != null) {
            module.getRequiresModules().forEach(m -> builder.append("    requires ")
                    .append(m)
                    .append(";")
                    .append((char) Character.LINE_SEPARATOR)
            );
        }

        builder.append("}");

        String moduleDescriptorPath = outputDir + File.separator + "module-info.java";

        // Escribir el descriptor en el disco duro
        try (OutputStream fos = new FileOutputStream(moduleDescriptorPath)) {
            fos.write(builder.toString().getBytes());
        } catch (IOException e) {
            throw e;
        }

        // Compilar el descriptor
        try {
            compiler.compileModuleDescriptor(outputDir.toString(), Main.destDir.toString() + (Main.modulePath != null ? File.pathSeparator + Main.modulePath : ""))
                    .ifPresent(System.out::println);
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }

        // Leer el descriptor compilado
        byte[] descriptorData = null;

        try (InputStream fis = new FileInputStream(outputDir + File.separator + "module-info.class")) {
            descriptorData = fis.readAllBytes();
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }

        return descriptorData;
    }

    /**
     * Agrega la entrada /module-info.class al archivo JAR cuya ruta es {@code jarFilePath}. El contenido de la entrada
     * será {@code moduleDescriptorData}.
     *
     * @param jarFilePath Archivo JAR a patchar
     * @param moduleDescriptorData Contenido de la entrada /module-info.class
     */
    public void patchJar(File jarFilePath, byte[] moduleDescriptorData) {
        try (
                OutputStream fos = new FileOutputStream(Main.destDir + File.separator + jarFilePath.getName() + "-mod.jar");
                JarOutputStream jos = new JarOutputStream(fos)
            ) {

            // Hago una copia exacta del JAR
            JarFile jar = new JarFile(jarFilePath);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                jos.putNextEntry(entry);

                if (!entry.isDirectory()) {
                    byte[] entryData = jar.getInputStream(entry).readAllBytes();
                    jos.write(entryData);
                    jos.flush();
                    jos.closeEntry();
                }
            }

            // Agrego el descriptor del módulo al JAR
            jos.putNextEntry(new JarEntry("module-info.class"));
            jos.write(moduleDescriptorData);
            jos.flush();
            jos.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException("Error pathing original jar file. " + e.getMessage(), e);
        }
    }
}