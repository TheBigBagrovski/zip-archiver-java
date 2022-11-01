package code;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tests {

    private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;
    /**
     * absolute paths to testing source files
     */
    private static final String basePath = System.getProperty("user.dir");
    private static final String pathToInputs = basePath + "\\src\\test\\resources\\input\\";
    private static final String ch = pathToInputs + "ch";
    private static final String dir = pathToInputs + "dir";
    private static final String audio = pathToInputs + "audio.mp3";
    private static final String file3 = pathToInputs + "file3.txt";
    private static final String pic1 = pathToInputs + "pic1.png";
    private static final String pic3 = pathToInputs + "pic3.png";
    private static final String video = pathToInputs + "video.mp4";
    private static final String wrongFileName1 = pathToInputs + "vide/o.mp4";
    private static final String wrongFileName2 = pathToInputs + "vide-o.mp4";
    private static final String pathToOutputs = basePath + "\\src\\test\\resources\\output\\";
    private static final String wrongPath1 = "/path/";
    private static final String wrongPath2 = "D:/Path";
    private static final String wrongArchName1 = "na/me";
    private static final String wrongArchName2 = "na:me";
    private static String[] args;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private boolean checkArchive(String zipFile, String[] expected) throws IOException {
        if (!new File(zipFile).exists()) return false;
        boolean ok = false;
        ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(
                zipFile), StandardOpenOption.READ));
        ZipEntry entry;
        for (String file : expected) {
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(file)) {
                    ok = true;
                    break;
                }
            }
            if (ok) {
                ok = false;
            } else {
                zis.close();
                return false;
            }
        }
        zis.close();
        return true;
    }

    private boolean checkUnzipped(String[] expected) {
        boolean ok = false;
        File folder = new File("src/test/resources/output/unzipped");
        File[] filesInFolder = folder.listFiles();
        for (String str : expected) {
            String correctStr = str.substring(str.lastIndexOf("\\") + 1);
            if (filesInFolder != null) {
                for (File file : filesInFolder) {
                    String fileName = file.getName();
                    if (fileName.equals(correctStr)) {
                        ok = true;
                        break;
                    }
                }
            }
            if (ok) {
                ok = false;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean compareStr(String exp, String act) {
        for (int i = 0; i < exp.length(); i++) {
            if (exp.charAt(i) != act.charAt(i)) return false;
        }
        return true;
    }

    @Test
    public void testZipSpecifiedFiles() throws IOException {
        args = new String[]{pic1, video, "-p", pathToOutputs, "test1"};
        Main.main(args);
        String[] expected = {"pic1.png", "video.mp4"};
        assertTrue(checkArchive(pathToOutputs + "test1.zip", expected));
    }

    @Test
    public void testZipMoreSpecifiedFiles() throws IOException {
        args = new String[]{ch, dir, audio, file3, pic3, pic1, video, "-p", pathToOutputs, "test2"};
        Main.main(args);
        String[] expected = {"ch\\a\\b/", "ch\\a\\text.txt", "dir\\file2.txt", "dir\\indir\\file1.txt",
                "dir\\pic2.png", "audio.mp3", "file3.txt", "pic3.png", "pic1.png", "video.mp4"};
        assertTrue(checkArchive(pathToOutputs + "test2.zip", expected));
    }

    @Test
    public void testZipWrongPath() {
        args = new String[]{ch, dir, audio, file3, pic3, pic1, video, "-p", wrongPath1, "test3"};
        Main.main(args);
        String expected = "Archive name set: test3.zip\r\nException while archiving\nPath not found: /path/\\\r\n";
        assertTrue(compareStr(expected, errContent.toString()));
        args = new String[]{ch, dir, audio, file3, pic3, pic1, video, "-p", wrongPath2, "test3"};
        Main.main(args);
        expected = "Archive name set: test3.zip\r\nException while archiving\nPath not found: /path/\\\r\nArchive name set: test3.zip\r\nException while archiving\nPath not found: D:/Path\\\r\n";
        assertTrue(compareStr(expected, errContent.toString()));
    }

    @Test
    public void testZipWrongArchiveName() {
        args = new String[]{ch, dir, audio, file3, pic3, pic1, video, "-p", pathToOutputs, wrongArchName1};
        Main.main(args);
        String expected = "Exception while archiving\nInvalid archive name: na/me.zip\r\n";
        assertTrue(compareStr(expected, errContent.toString()));
        args = new String[]{ch, dir, audio, file3, pic3, pic1, video, "-p", pathToOutputs, wrongArchName2};
        Main.main(args);
        expected = "Exception while archiving\nInvalid archive name: na/me.zip\r\nException while archiving\nInvalid archive name: na:me.zip\r\n";
        assertTrue(compareStr(expected, errContent.toString()));
    }

    @Test
    public void testZipWrongFileNames() {
        args = new String[]{wrongFileName1, wrongFileName2, "-p", pathToOutputs, "test3"};
        Main.main(args);
        String expected = """
                Archive name set: test3.zip\r
                Path set: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\output\\\r
                Searching file: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\input\\vide/o.mp4\r
                Exception while archiving
                Exception while searching files: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\input\\vide/o.mp4
                No such file: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\input\\vide/o.mp4\r
                """;
        assertTrue(compareStr(expected, errContent.toString()));
    }

    @Test
    public void testZipNoNameProvided() {
        args = new String[]{pic1, video, "-p", pathToOutputs};
        Main.main(args);
        String expected = """
                Exception while archiving
                Invalid archive name: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\output\\.zip\r
                """;
        assertTrue(compareStr(expected, errContent.toString()));
    }

    @Test
    public void testZipNoPathProvided() {
        args = new String[]{pic1, video, "-p", "name"};
        Main.main(args);
        String str = """
                Archive name set: name.zip\r
                Path set: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\input\\video.mp4\\\r
                Searching file: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\input\\pic1.png\r
                Total file size: 2 kB\r
                Exception while archiving
                Exception while adding files to zip:
                D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\input\\video.mp4\\name.zip (Системе не удается найти указанный путь)\r
                """;
        byte[] bytes = str.getBytes();
        String expected = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(compareStr(expected, errContent.toString()));
    }

    @Test
    public void testUnzip() {
        args = new String[]{"-u", "-p", pathToOutputs + "unzipped\\", "src/test/resources/output/test2.zip"};
        Main.main(args);
        String[] expected = {ch, dir, audio, file3, pic1, pic3, video};
        assertTrue(checkUnzipped(expected));
    }

    @Test
    public void testUnzipNoNameProvided() {
        args = new String[]{"-u", "-p", pathToOutputs};
        Main.main(args);
        String expected = """
                Exception while archiving
                Invalid archive name: D:\\Projects\\Java\\zip-archiver-java\\src\\test\\resources\\output\\.zip\r
                """;
        assertTrue(compareStr(expected, errContent.toString()));
    }

    @Test
    public void testUnzipNoPathProvided() {
        args = new String[]{"-u", "-p", "test2.zip"};
        Main.main(args);
        String expected = """
                Archive name: test2.zip\r
                Exception while archiving
                Wrong input: no path provided\r
                """;
        assertTrue(compareStr(expected, errContent.toString()));
    }

}
