/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package marregui.plot.rfs;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * IO Utilities
 *
 * @author marregui
 */
public class IOUtils {
    public static final int BUFFER_SIZE = 1024 * 64;
    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class.getName());

    /**
     * @param rootFolderPath
     * @return A tree structure representing the file system under the rootFolderPath
     * @throws IOException
     */
    public static FSEntry list(String rootFolderPath) throws IOException {
        File folder = new File(rootFolderPath);
        LOGGER.debug(String.format("Listing folder: %s", folder.getAbsolutePath()));
        return list(folder, FSEntry.rootFolder(rootFolderPath, folder.lastModified()), rootFolderPath);
    }

    private static FSEntry list(File folder, FSEntry parentFolder, String rootFolderPath) throws IOException {
        if (false == checkFolderAccess(folder)) {
            throw new IOException(String.format("Cannot access: %s", folder.getAbsolutePath()));
        }
        for (File f : folder.listFiles()) {
            if (checkFolderAccess(f)) {
                FSEntry folderEntry = FSEntry.folder(f.getName(), f.lastModified());
                parentFolder.addFSEntry(folderEntry);
                list(f, folderEntry, rootFolderPath);
            } else if (checkFileAccess(f)) {
                String path = folder.getAbsolutePath().substring(rootFolderPath.length() + 1);
                parentFolder.addFSEntry(FSEntry.file(f.getName(), path, f.length(), f.lastModified()));
            } else {
                throw new IOException(String.format("Cannot access: %s", f.getAbsolutePath()));
            }
        }
        return parentFolder;
    }

    /**
     * @param folder
     * @return Is folder, exists, can be read and executed
     */
    public static boolean checkFolderAccess(File folder) {
        return folder.exists() && folder.isDirectory() && folder.canExecute() && folder.canRead();
    }

    /**
     * @param file
     * @return Is file, exists and can be read
     */
    public static boolean checkFileAccess(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }

    /**
     * @param e
     * @return The entire stack trace as a String
     */
    public static String exceptionAsString(Exception e) {
        ByteArrayOutputStream ebytes = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(ebytes);
        e.printStackTrace(ps);
        ps.close();
        return ebytes.toString();
    }

    /**
     * Takes the folder specified by 'prefix/folderName' (where prefix is the absolute
     * path of the parent folder) and performs a "tar -cf - | gzip > folderName.tgz"
     *
     * @param prefix
     * @param folderName
     * @param out
     * @throws IOException
     */
    public static void tgz(String prefix, String folderName, OutputStream out) throws IOException {
        String tgzFolderPath = String.format("%s%s%s", prefix, File.separator, folderName);
        File tgzFolder = new File(tgzFolderPath);
        if (false == IOUtils.checkFolderAccess(tgzFolder)) {
            throw new IOException(String.format("Cannot access folder: %s", tgzFolderPath));
        }
        LOGGER.debug(String.format("Tarballing folder: %s", tgzFolderPath));
        TarArchiveOutputStream tgzos = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)));
        tgzos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        tgzos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        tgzos.putArchiveEntry(new TarArchiveEntry(tgzFolder, folderName));
        tgzos.closeArchiveEntry();
        tgz(prefix, tgzFolder, tgzos);
        tgzos.flush();
        tgzos.close();
    }

    private static void tgz(String preffix, File folder, TarArchiveOutputStream tgzos) throws IOException {
        if (false == checkFolderAccess(folder)) {
            throw new IOException(String.format("Cannot access: %s", folder.getAbsolutePath()));
        }
        for (File f : folder.listFiles()) {
            String path = f.getAbsolutePath();
            path = path.substring(path.indexOf(preffix) + preffix.length() + 1);
            tgzos.putArchiveEntry(new TarArchiveEntry(f, path));
            LOGGER.debug(String.format("Adding: %s", path));
            if (checkFolderAccess(f)) {
                tgzos.closeArchiveEntry();
                tgz(preffix, f, tgzos);
            } else if (checkFileAccess(f)) {
                final int bufferSize = 1024 * 4;
                byte[] buffer = new byte[bufferSize];
                int bread = 0;
                FileInputStream in = new FileInputStream(f);
                while (-1 != (bread = in.read(buffer, 0, bufferSize))) {
                    tgzos.write(buffer, 0, bread);
                }
                tgzos.flush();
                tgzos.closeArchiveEntry();
                in.close();
            } else {
                throw new IOException(String.format("Cannot access: %s", f.getAbsolutePath()));
            }
        }
    }

    /**
     * Moves bytes until exhaustion from in to out using an internal buffer
     *
     * @param in
     * @param out
     * @return The amount of bytes transferred
     * @throws IOException
     */
    public static long readFromTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long transferredBytes = 0;
        int bread = 0;
        while (-1 != (bread = in.read(buffer, 0, BUFFER_SIZE))) {
            out.write(buffer, 0, bread);
            out.flush();
            transferredBytes += bread;
        }
        return transferredBytes;
    }

    /**
     * @param size
     * @return A nicely formatted String representing the size
     */
    public static String hummanReadableSize(long size) {
        final int unit = 1024;
        if (size < unit) {
            return String.format("%d bytes", Long.valueOf(size));
        }
        int exp = (int) (Math.log(size) / Math.log(unit));
        return String.format(
                "%d bytes (%.1f %cB)",
                Long.valueOf(size),
                Double.valueOf(size / Math.pow(unit, exp)),
                "KMGTPE".charAt(exp - 1));
    }

    /**
     * @param fileName
     * @return The contents of the file as a String
     * @throws IOException
     */
    public static String fetchFileContents(String fileName) throws IOException {
        File file = new File(fileName);
        int fileSize = (int) file.length(); // Cannot exceed, 4Gb which is fine
        byte[] fileBytes = new byte[fileSize];
        FileInputStream fis = new FileInputStream(file);
        int readBytes = fis.read(fileBytes, 0, fileSize);
        fis.close();
        if (readBytes != fileSize) {
            throw new IOException("Failed to read the contents of the file");
        }
        return new String(fileBytes);
    }
}