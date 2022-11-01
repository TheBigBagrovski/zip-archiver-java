package code;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
//TODO() тесты
//TODO() описания функций в javadoc стиле, рефакторинг, тесты

/**
 * Options:
 * -u - unzip file in files instead of zipping
 * -p - unzip archive in selected directory / create zip in selected directory
 * -a - zip all files in current directory
 * -: -a -p (userFileNames) (userPath) userArchiveName
 * -u: -p (userFileNames) (userPath) userArchiveName
 */
public class Main {

    public static final Character[] INVALID_WINDOWS_SPECIFIC_CHARS = {':', '/', '\\', '"', '*', '<', '>', '?', '|'};
    public static final Character[] INVALID_UNIX_SPECIFIC_CHARS = {'\000'};

    @SuppressWarnings("unused")
    @Option(name = "-u")
    private boolean u;
    @SuppressWarnings("unused")
    @Option(name = "-p")
    private boolean p;
    @SuppressWarnings("unused")
    @Option(name = "-a")
    private boolean a;
    @SuppressWarnings("unused")
    @Argument(required = true)
    private List<String> userInput;

    private String userPath = "";
    private String userArchiveName;

    public static void main(String[] args) {
        new Main().launch(args);
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    private void launch(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println("Exception while parsing arguments\n" + e.getMessage());
            printFail();
            return;
        }
        try {
            if (!u) {
                setUserArchiveName();
                System.err.println("Archive name set: " + userArchiveName);
                if (p) {
                    setUserPath();
                    System.err.println("Path set: " + userPath);
                } else {
                    System.err.println("Archive will be created in current working directory");
                }
                List<File> filesToZip = new ArrayList<>();
                if (a) {
                    if (!userInput.isEmpty()) throw new IllegalArgumentException("Wrong input: -a with files");
                    getFilesInCurrentDir(filesToZip);
                } else {
                    if (userInput.isEmpty()) throw new IllegalArgumentException("Wrong input: no files provided");
                    getExistingFilesFromUserInput(filesToZip);
                }
                zipper(filesToZip);
            } else {
                if (a) throw new IllegalArgumentException("Flag -a when unzipping");
                setArchiveToUnzipName();
                System.err.println("Archive name: " + userArchiveName);
                File destDir;
                if (p) {
                    setUserPath();
                    destDir = new File(userPath);
                    System.err.println("Archive will be unpacked in directory: " + userPath);
                } else {
                    destDir = new File(System.getProperty("user.dir"));
                    System.err.println("Archive will be unpacked in current working directory");
                }
                File archive = getExistingFile(userArchiveName);
                unzip(destDir, archive);
            }
        } catch (Exception e) {
            System.err.println("Exception while archiving\n" + e.getMessage());
            printFail();
            return;
        }
        printSuccess();
    }

    private void printFail() {
        System.out.println("Process finished with errors");
    }

    private void printSuccess() {
        System.out.println("Process finished successfully");
    }

    private void setArchiveToUnzipName() {
        userArchiveName = userInput.get(userInput.size() - 1);
        userInput.remove(userInput.size() - 1);
    }

    private void setUserArchiveName() {
        if (!isArchiveNameValid()) throw new IllegalArgumentException("Invalid archive name: " +
                userInput.get(userInput.size() - 1) + ".zip");
        userArchiveName = userInput.get(userInput.size() - 1) + ".zip";
        userInput.remove(userInput.size() - 1);
    }

    private Character[] getInvalidCharsByOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return INVALID_WINDOWS_SPECIFIC_CHARS;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            return INVALID_UNIX_SPECIFIC_CHARS;
        } else {
            return new Character[]{};
        }
    }

    private boolean isArchiveNameValid() {
        String str = userInput.get(userInput.size() - 1);
        if (str == null || str.isEmpty() || str.length() > 255) {
            return false;
        }
        for (Character ch : getInvalidCharsByOS()) {
            if (str.contains(ch.toString())) return false;
        }
        return true;
    }

    private void setUserPath() {
        userPath = userInput.get(userInput.size() - 1);
        if (userPath.charAt(userPath.length() - 1) != '\\') userPath += '\\';
        if (!Files.exists(Paths.get(userPath))) throw new IllegalArgumentException("Path not found: " + userPath);
        userInput.remove(userInput.size() - 1);
    }

    private void zipper(List<File> filesToZip) throws IOException, IllegalArgumentException {
        System.err.println("Total file size: " + getFilesSize(filesToZip) / 1024 + " kB");
        List<String> absPaths = new ArrayList<>();
        List<String> correctNames = new ArrayList<>();
        getListsOfNames(filesToZip, absPaths, correctNames);
        zip(absPaths, correctNames);
        System.err.println("Archive size: " + new File(userPath + userArchiveName).length() / 1024 + " kB");
    }

    private void getFilesInCurrentDir(List<File> filesToZip) throws IOException {
        File dir = new File("./");
        File[] filesInCurrentDir = dir.listFiles();
        if (filesInCurrentDir != null) {
            filesToZip.addAll(List.of(filesInCurrentDir));
        } else throw new IOException("Exception while getting files in current directory");
    }

    private File getExistingFile(String userInput) throws IOException {
        try {
            System.err.println("Searching file: " + userInput);
            File file = new File(userInput);
            if (!file.exists()) throw new NoSuchFileException("No such file: " + userInput);
            else return file;
        } catch (IOException e) {
            throw new IOException("Exception while searching files: " + userInput + "\n" + e.getMessage(), e);
        }
    }

    private void getExistingFilesFromUserInput(List<File> filesToZip) throws IOException {
        for (String userInput : userInput) {
            filesToZip.add(getExistingFile(userInput));
        }
    }

    private void getListsOfNames(List<File> listOfFiles, List<String> absPaths, List<String> correctNames) throws IOException {
        for (File file : listOfFiles) {
            String common = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf('\\') + 1);
            List<String> temp = new ArrayList<>();
            generateFileList(temp, file);// returns abs paths for files (including those in folders)
            absPaths.addAll(temp);
            for (String absPath : temp) {
                String str = absPath.replace(common, "");
                if (str.startsWith(".\\")) str = str.substring(2); // removes "./" from file name
                correctNames.add(str);
            }
        }
    }

    private void zip(List<String> absPaths, List<String> correctNames) throws IOException {
        byte[] bytes;
        int length;
        ZipEntry zipEntry;
        FileInputStream fis;
        try (FileOutputStream fos = new FileOutputStream(userPath + userArchiveName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (int i = 0; i < correctNames.size(); i++) {
                String fileName = correctNames.get(i);
                String absPath = absPaths.get(i);
                if (fileName.equals(userArchiveName)) continue; // skip .zip itself when -a
                System.err.println("Zipping file: " + fileName);
                zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                if (new File(absPath).isDirectory()) continue; // if folder => empty folder => nothing to write
                fis = new FileInputStream(absPath);
                bytes = new byte[1024];
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
        } catch (IOException e) {
            throw new IOException("Exception while adding files to zip:\n" + e.getMessage(), e);
        }
    }

    private void generateFileList(List<String> fileNamesToZip, File node) throws IOException {
        if (node.isFile()) {
            fileNamesToZip.add(node.toString());
        }
        if (node.isDirectory()) {
            if (isDirectoryEmpty(node)) {
                fileNamesToZip.add(node + "/");
                return;
            }
            String[] subNote = node.list();
            if (subNote != null) {
                for (String filename : subNote) {
                    generateFileList(fileNamesToZip, new File(node, filename));
                }
            }
        }
    }

    private boolean isDirectoryEmpty(File directory) throws IOException {
        String[] files = directory.list();
        if (files != null) return files.length == 0;
        else throw new IOException("Exception while getting empty folder: " + directory);
    }

    private long getFilesSize(List<File> files) throws IOException {
        long sum = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                sum += Files.walk(Path.of(file.getAbsolutePath()))
                        .filter(p -> p.toFile().isFile())
                        .mapToLong(p -> p.toFile().length())
                        .sum();
            } else {
                sum += file.length();
            }
        }
        return sum;
    }

    private void unzip(File destDir, File archive) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parent);
                    }
                    FileOutputStream fos = new FileOutputStream(newFile);
                    System.err.println("Unpacking file: " + zipEntry);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            throw new IOException("Exception while unzipping:\n" + e.getMessage(), e);
        }
    }

}
