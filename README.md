# scan_and_mark

## run on windows:
* install Java Version >= 17 : [OpenJDK](https://openjdk.org/) - latest open-source JDK
* download latest release .jar from GitHub
* place the .jar into desired working directory

## run on linux: 
* install Java Version >= 17 : [OpenJDK](https://openjdk.org/) - latest open-source JDK
* download latest release .jar from GitHub
* execute:  sudo apt update
            sudo apt install openjfx
* execute SAM with: java -Djava.library.path=<path_to_jni_for_javafx> --module-path <path_to_openjfk_lib_folder> --add-modules ALL-MODULE-PATH -jar <name_of_jar>
for example: java -Djava.library.path=/usr/lib/x86_64-linux-gnu/jni/ --module-path /usr/share/openjfx/lib --add-modules ALL-MODULE-PATH -jar SAM_Release1.jar

## compile and run:
* tested with Java>=17.
* you need to have JavaFx installed, e.g. run `apt install openjfx`
* `mvn package`
* `java --module-path /usr/share/openjfx/lib --add-modules ALL-MODULE-PATH -jar target/SAM-1.0-SNAPSHOT-jar-with-dependencies.jar`
