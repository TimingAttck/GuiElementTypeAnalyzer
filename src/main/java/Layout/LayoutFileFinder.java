package Layout;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The LayoutFileFinder class finds and reads all Layout Files
 * inside a Android apk.
 */
public class LayoutFileFinder {

    private File apk;

    /**
     * @param apk The Android apk file
     */
    public LayoutFileFinder(File apk) {
        this.apk = apk;

        if (!apk.exists())
            throw new RuntimeException("Apk file does not exist");
    }

    /**
     * Reads and finds all Layout Files from an Android apk file.
     *
     * @return A Map from File Name to byte array of the layout file contents
     * @throws IOException
     */
    public Map<String, byte[]> getAllLayoutFilesFromApk() throws IOException {

        Map<String, byte[]> layoutFiles = new HashMap<>();

        ZipFile archive = new ZipFile(apk);

        Enumeration<? extends ZipEntry> zipEntries = archive.entries();
        while (zipEntries.hasMoreElements()) {

            ZipEntry entry = zipEntries.nextElement();
            String fileName = entry.getName();

            if (!fileName.startsWith("res/layout") && !fileName.startsWith("res/navigation"))
                continue;
            if (!fileName.endsWith(".xml"))
                continue;

            byte[] xmlFileByteArray = getBytesFromXML(archive, entry);
            layoutFiles.put(fileName, xmlFileByteArray);

        }

        return layoutFiles;
    }

    /**
     * Returns a byte array from a Layout File
     *
     * @param archive The APK as a ZipFile archive instance
     * @param entry A file of the APK
     * @return A byte array of the contents of the file
     * @throws IOException
     */
    private byte[] getBytesFromXML(ZipFile archive, ZipEntry entry) throws IOException {
        try (InputStream is = archive.getInputStream(entry)) {
            BufferedInputStream buffer = new BufferedInputStream(is);

            List<byte[]> chunks = new ArrayList<byte[]>();
            int bytesRead = 0;
            while (is.available() > 0) {
                byte[] nextChunk = new byte[is.available()];
                int chunkSize = buffer.read(nextChunk);
                if (chunkSize < 0)
                    break;
                chunks.add(nextChunk);
                bytesRead += chunkSize;
            }

            // Create the full array
            byte[] xml = new byte[bytesRead];
            int bytesCopied = 0;
            for (byte[] chunk : chunks) {
                int toCopy = Math.min(chunk.length, bytesRead - bytesCopied);
                System.arraycopy(chunk, 0, xml, bytesCopied, toCopy);
                bytesCopied += toCopy;
            }

            return xml;
        }
    }

}
