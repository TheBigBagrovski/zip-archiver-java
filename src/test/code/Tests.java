package code;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tests {
    /**
     * absolute paths to testing source files
     */
    String basePath = System.getProperty("user.dir");
    String pathToInputs = basePath + "\\src\\test\\resources\\input\\";
    String ch = pathToInputs + "ch";
    String dir = pathToInputs + "dir";
    String audio = pathToInputs + "audio.mp3";
    String file3 = pathToInputs + "file3.txt";
    String pic1 = pathToInputs + "pic1.png";
    String pic3 = pathToInputs + "pic3.png";
    String video = pathToInputs + "video.mp4";
    String pathToOutputs = basePath + "\\src\\test\\resources\\output\\";
    String[] args;

    private boolean checkArchive(String zipFile, String[] files) throws IOException {
        if (!new File(zipFile).exists()) return false;
        boolean ok = false;
        ZipInputStream zip = new ZipInputStream(Files.newInputStream(Paths.get(
                pathToOutputs + "/test1.zip"), StandardOpenOption.READ));
        for (String file : files) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                System.err.println(file);
                System.err.println(entry.getName());
                if (entry.getName().equals(file)) {
                    ok = true;
                    break;
                }
            }
            if(ok) {
               ok = false;
            } else {
                zip.close();
                return false;
            }
        }
        zip.close();
        return true;
    }

    @Test
    public void testZippingSpecifiedFiles() throws IOException {
        args = new String[]{pic1, video, "-p", pathToOutputs, "test1"};
        Main.main(args);
        //assertTrue(checkArchive(pathToOutputs + "test1.zip", new String[]{pic1, video}));
    }

    @Test
    public void testZippingMoreSpecifiedFiles() throws IOException {
        args = new String[]{ch, dir, audio, file3, pic3, pic1, video, "-p", pathToOutputs, "test2"};
        Main.main(args);
        assertTrue(checkArchive(pathToOutputs + "test1.zip", new String[]{ch, dir, audio, file3, pic3, pic1, video}));
    }
}
