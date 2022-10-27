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
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
//TODO() описания функций в javadoc стиле, рефакторинг, тесты (задавать аргументы командной строки в настройках)
//TODO() zip: довести доу ма что есть, проверить ошибки в разных случаях, -p, выводить общий размер файлов и размер
// архива в stderr
//todo() unzip
//todo() slipProtect

/**
 * Options:
 * -u - unzip file in files instead of zipping
 * -p - unzip archive in selected directory / create zip in selected directory
 * -a - zip all files in current directory
 * -: -a -p (userFileNames) (userPath) userArchiveName
 * u: -p userFileNames (userPath) userArchiveName
 */
public class Main {

    public static final Character[] INVALID_WINDOWS_SPECIFIC_CHARS = {':', '/', '\\', '"', '*', '<', '>', '?', '|'};
    public static final Character[] INVALID_UNIX_SPECIFIC_CHARS = {'\000'};

    @Option(name = "-u")
    private boolean u;
    @Option(name = "-p")
    private boolean p;
    @Option(name = "-a")
    private boolean a;
    @Argument(required = true)
    private List<String> userInputs;

    private String userPath = "";
    private String userArchiveName;

    public static void main(String[] args) {
        new Main().launch(args);
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
            setUserArchiveName();
            setUserPath();
            zipper();
        } catch (IOException e) {
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

    private void setUserArchiveName() {
        if (!isArchiveNameValid()) throw new IllegalArgumentException("Invalid archive name: " +
                userInputs.get(userInputs.size() - 1)  + ".zip");
        userArchiveName = userInputs.get(userInputs.size() - 1) + ".zip";
        userInputs.remove(userInputs.size() - 1);
        System.err.println("Archive name set: " + userArchiveName);
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
        String str = userInputs.get(userInputs.size() - 1);
        if (str == null || str.isEmpty() || str.length() > 255) {
            return false;
        }
        for (Character ch : getInvalidCharsByOS()) {
            if (str.contains(ch.toString())) return false;
        }
        return true;
    }

    private void setUserPath() {
        if (p) {
            userPath = userInputs.get(userInputs.size() - 1);
            if(userPath.charAt(userPath.length()-1)!='\\') userPath += '\\';
            Path absPath = Paths.get(userPath);
            /*String filePath = new File("").getAbsolutePath();
            filePath = filePath.concat(userPath);
            Path relPath = Paths.get(filePath);*/
            if (!Files.exists(absPath) /*&& !Files.exists(relPath)*/) throw new IllegalArgumentException("Path not found: " + userPath);
            userInputs.remove(userInputs.size() - 1);
            System.err.println("Path set: " + userPath);
        }
    }

    private void zipper() throws IOException {
        if (!u) {
            List<File> filesToZip = new ArrayList<>();
            List<String> fileNamesToZip = new ArrayList<>();
            if (a) {
                if(!userInputs.isEmpty()) throw new IllegalArgumentException("Wrong input: -a with files");
                getFilesInCurrentDir(filesToZip);
            } else {
                getExistingFiles(filesToZip);
            }
            for (File file : filesToZip) {
                generateFileList(fileNamesToZip, file);
            }
            zip(fileNamesToZip);
        }
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
            throw new IOException("Exception during searching files", e);
        }
    }

    private void getExistingFiles(List<File> filesToZip) throws IOException {
        for (String userInput : userInputs) {
            filesToZip.add(getExistingFile(userInput));
        }
    }

    private void zip(List<String> filesToZip) throws IOException {
        byte[] bytes;
        int length;
        ZipEntry zipEntry;
        FileInputStream fis;
        try (FileOutputStream fos = new FileOutputStream(userPath + userArchiveName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String fileToZip : filesToZip) {
                if (fileToZip.startsWith(".\\")) fileToZip = fileToZip.substring(2); // removes "./" from file name
                if (fileToZip.equals(userArchiveName)) continue; // skip .zip itself when -a
                System.err.println("Zipping file: " + fileToZip);
                zipEntry = new ZipEntry(fileToZip);
                System.err.println(zipEntry);
                zos.putNextEntry(zipEntry);
                if (new File(fileToZip).isDirectory()) continue; // for empty folder
                fis = new FileInputStream(fileToZip);
                bytes = new byte[1024];
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
                fis.close();
            }
        } catch (IOException e) {
            throw new IOException("Exception while adding files to zip", e);
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
        else throw new IOException("Exception while getting empty folder");
    }
}