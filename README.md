# JarModularizer
This project is a lightweight command line java tool for make non-modular JAR files compatible with Java Platform Module System. Was inspired in the [ModiTect's moditect-maven-plugin](https://github.com/moditect/moditect) idea but nothing of it code was copied, studied or simply consulted for develop this project.

See brother project [PyJarModularizer](https://github.com/betanzos/py-jar-modularizer/).

## Usage
The way JarModularizer work is simple. It need a configuration JSON file, called [modularization descriptor](#modularization-descriptor-format), with the description of how each JAR file, called artifact, will should become in a java module and the source directory path, wich is where non-modular JARs files will be found.

### Prerequisites
* [JDK 9 or later](https://www.oracle.com/technetwork/java/javase/overview/index.html) because is requires by JarModularizer for do its work.
* [Maven](http://maven.apache.org/) as project management tool.

### Example
1 - Compile and package with Maven using the command `mvn clean package`.

2 - Execute de following command:
```
java -jar jar-modularizer-<version>-jar-with-dependencies.jar --descriptor <file-path> --source <dir-path>
```
Command above will deposite the new modular JAR copies in `--source/mods` directory with fallowing name pattern: original.jar-mod.jar (ej. for log4j-1.2.17.jar new file name will be log4j-1.2.17.jar-mod.jar).

#### Important!
Only that files wich name (including .jar extension) match with an entry in [modularization descriptor](#modularization-descriptor-format) will be processed.

## Getting help
If `--help` param is using, tool's help will be diplayed in the terminal.

## Modularization descriptor format
As was mentioned above, the modularization descriptor is a JSON file. Below show it format.

***Note:*** JSON format doesn't allow comments, so java-style inline comment texts are only for descriptive purposes.
```
[
    {
        "name": "log4j-1.2.17.jar",// artifact name
        "module": {// module entry
            "name": "log4j",// future module name
            "exportsPackages": [// (optional) list of artifact's packages to be exported by the future module. Default is all artifact non-empty packages
                "org.apache.log4j",
                "org.apache.log4j.net"
            ],
            "requiresModules": [// (optional) list of requires directive to be included in module-info.java. Dafault is none diretive
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

## Author
Eduardo Betanzos [@ebetanzosm](https://twitter.com/ebetanzosm)

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.