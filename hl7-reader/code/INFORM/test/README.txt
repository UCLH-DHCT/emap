https://stackoverflow.com/questions/52373469/how-to-launch-junit-5-platform-from-the-command-line-without-maven-gradle


Sure, use the ConsoleLauncher.

The ConsoleLauncher is a command-line Java application that lets you launch the JUnit Platform from the console. For example, it can be used to run JUnit Vintage and JUnit Jupiter tests and print test execution results to the console.

An executable *junit-platform-console-standalone-<version>.jar* with all dependencies included is published in the central Maven repository under the junit-platform-console-standalone directory. You can run the standalone ConsoleLauncher as shown below.
java -jar junit-platform-console-standalone-<version>.jar <Options>
For details about the options consult https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher please.

Tailored to your example and using JUnit Platform version 1.3.1, the commands could look like those:

$ mkdir out
$ javac -d out Student.java StudentSortSearch.java
$ javac -d out -cp out:junit-platform-console-standalone-1.3.1.jar TestClass.java
$ java -jar junit-platform-console-standalone-1.3.1.jar --class-path out --scan-class-path
╷
├─ JUnit Jupiter ✔
│  └─ TestClass ✔
│     └─ test() ✔
└─ JUnit Vintage ✔

Test run finished after 67 ms
...
