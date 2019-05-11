# JarModularizer
Este proyecto es una herramineta ligera de línea de comandos de Java para hacer archivos JAR no modulares compatibles con el Java Platform Module System. Está inspirado en la idea del proyecto [moditect-maven-plugin de ModiTect](https://github.com/moditect/moditect) pero no se copió, estudió o consultó su código para el desarrollo de este proyecto.

## Uso
El modo en que JarModularizer trabaja es simple. Necesita de un archivo JSON de configuración, llamado [descriptor de modularización](#formato-del-descriptor-de-modularización), que contiene la descripción de cómo cada archivo JAR, llamado artefacto, será convertido en un módulo de java; y la ruta al directorio fuente, en el cual podremos encontrar los archivos JAR no modulares.

### Pre-requisitos
* [JDK 9 o superior](https://www.oracle.com/technetwork/java/javase/overview/index.html) ya que es requerido por JarModularizer para hacer su trabajo.
* [Maven](http://maven.apache.org/) como herramienta de gestión del proyecto.

### Ejemplo
1 - Compilar y empaquetar con Maven, usando el comando `mvn clean package`.

2 - Ejecutar el siguiente comando:
```
java -jar jar-modularizer-<version>-jar-with-dependencies.jar --descriptor <file-path> --source <dir-path>
```
El comando anterior depositará copias de los nuevos archivos JAR modulares en el directorio `--source/mods` con el siguiente patrón de nombre: original.jar-mod.jar (ej. for log4j-1.2.17.jar el nombre del nuevo archivó será log4j-1.2.17.jar-mod.jar).

#### ¡Importante!
Solo aquellos archivos cuyo nombre (incluida la extensión .jar) coincidan con una entrada en el [descriptor de modularización](#formato-del-descriptor-de-modularización) serán procesados.

## Obteniendo ayuda
Si se pasa el comando `--help`, la ayuda de la herramienta será mostrada en el terminal.

## Formato del descriptor de modularización
Como se mencionó arriba, el descriptor de modularización es un archivo JSON. Debajo el formato de este.

***Nota:*** El formato JSON no permite comentarios, por lo que los comentarios en línea tipo Java se muestra solo con fines descriptivos.
```
[
    {
        "name": "log4j-1.2.17.jar",// nombre del artefacto
        "module": {// module entry
            "name": "log4j",// nombre del futuro módulo
            "exportsPackages": [// (opcional) lista de los paquetes del artefacto a ser exportados por el futuro módulo. Por defecto son todos los paquetes no vacíos del artefacto
                "org.apache.log4j",
                "org.apache.log4j.net"
            ],
            "requiresModules": [// (opcional) lista de las directivas requires a incluir en el archivo module-info.java. Por defecto ninguna directiva será incluida
                "java.base",
                "java.desktop",
                "java.management",
                "java.naming",
                "java.sql",
                "java.xml"
            ]
        }
    },
    ...
]
```

## Autor
Eduardo Betanzos [@ebetanzosm](https://twitter.com/ebetanzosm)

## Licencia
Este proyecto está licenciado bajo la Licencia del  MIT - para más detalles ver el archivo [LICENSE](LICENSE).