package uk.ac.ucl.rits.inform.interchange;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;


/**
 * A collection of files where the access is monitored. So, for example, all resources associated with a class
 * can be checked if they have been accessed in a set of tests.
 */
public class FileStoreWithMonitoredAccess implements Iterable<MonitoredFile> {

    private final List<MonitoredFile> files;

    /**
     * A repository of file paths that have a particular extension, each of which has an access-count.
     * @param additionalClass Additional class to add the resources for on top of FileStoreWithMonitoredAccess
     * @throws URISyntaxException If the folder path does not exist in the file system
     */
    public FileStoreWithMonitoredAccess(Class additionalClass) throws URISyntaxException, IOException {
        files = new ArrayList<>();
        updateFilesFromClassResources(getClass());
        updateFilesFromClassResources(additionalClass);
    }

    /**
     * Access a filename within the store and increment the access count.
     * @param fileName Name of the file
     * @return fileName
     * @throws IOException If the file is not in the store
     */
    public String get(String fileName) throws IOException {

        for (MonitoredFile file : files) {
            if (file.getFilePathString().contains(fileName)) {
                file.incrementAccessCount();
                return fileName;
            }
        }

        throw new IOException("Failed to find " + fileName + " in the list of message files");
    }

    /**
     * Update the resource file paths from a class
     * @param rootClass Class
     * @throws URISyntaxException if the resource path cannot be found
     */
    public void updateFilesFromClassResources(Class rootClass) throws URISyntaxException, IOException {

        if (rootClass == null){
            return;
        }

        getResourcePaths(rootClass).forEach(p -> this.files.add(new MonitoredFile(p)));
    }

    @Override
    public Iterator<MonitoredFile> iterator() {
        return files.iterator();
    }

    public Stream<MonitoredFile> stream() {
        return files.stream();
    }

    /**
     * Get the paths to all resources for this class
     */
    public static Set<Path> getResourcePaths(Class rootClass) throws URISyntaxException, IOException {

        var pathsSet = new HashSet<Path>();
        var src = rootClass.getProtectionDomain().getCodeSource();
        var path = new File(src.getLocation().toURI());

        if (path.isDirectory()){
            addPathsFromDirectory(path, pathsSet);
        }
        else if (path.isFile() && path.getName().toLowerCase().endsWith(".jar")) {
            addPathsFromJar(path, pathsSet) ;
        }

        return pathsSet;
    }

    /**
     * Get the paths to resources contained within a jar file
     */
    private static void addPathsFromJar(File file, Set<Path> paths) throws IOException {

        var jarFileEntries = new JarFile(file).entries();
        var templateString = file.toURI() + "!/%s";

        while (jarFileEntries.hasMoreElements()) {

            var entry = jarFileEntries.nextElement();
            var entryName = entry.getName();

            if (!entry.isDirectory()) {
                var url = new URL("jar", "", String.format(templateString, entryName));
                paths.add(Path.of(url.getPath()));
            }
        }
    }

    /**
     * Recursively add all the file paths down from a particular path
     */
    private static void addPathsFromDirectory(File path, Set<Path> pathsSet){
        File[] items = path.listFiles();

        if (items == null){
            return;
        }

        for (var item: items) {
            if (item.isDirectory()) {
                addPathsFromDirectory(item, pathsSet);
            }
            else if (item.isFile()) {
                pathsSet.add(item.toPath());
            }
        }
    }
}
