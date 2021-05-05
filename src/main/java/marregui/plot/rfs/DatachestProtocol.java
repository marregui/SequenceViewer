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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Simple protocol that runs on top of HTTP. It supports the commands:
 * <ul>
 *     <li><b>list:</b> retrieves the tree structure of the remote data space in xml format</li>
 *     <li><b>get:</b> takes parameters 'path' and 'name' and retrieves the file or folder
 *                     from the remote data space saving it locally. In the case of retrieving
 *                     a folder, it comes in '.tgz' format</li>
 * </ul>
 *
 * @author marregui
 */
public abstract class DatachestProtocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatachestProtocol.class.getName());
    private static final String CHARSET = "UTF-8";
    private static final String LIST_COMMAND_URL = "/do?list";
    private static final String GET_FILE_COMMAND_URL_TPT = "/do?get&" + FSTreeParser.PATH_ATTR + "=%s&" + FSTreeParser.NAME_ATTR + "=%s";

    /**
     * @param serverUrl
     * @return the contents of the remote data space in the form of a file system tree structure
     * @throws Exception
     */
    public static FSEntry list(String serverUrl) throws Exception {
        LOGGER.debug("List command invoked");
        HttpClient client = new HttpClient(serverUrl, LIST_COMMAND_URL);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(IOUtils.BUFFER_SIZE);
        client.download(baos);
        baos.close();
        return FSTreeParser.parse(baos.toString(CHARSET));
    }

    /**
     * @param serverUrl
     * @param entry
     * @param downloadFolder
     * @return Downloads the entry from the remote data space into the download folder
     * @throws Exception
     */
    public static File get(String serverUrl, FSEntry entry, File downloadFolder) throws Exception {
        if (null == entry) {
            return null;
        }
        LOGGER.debug(String.format("Get command invoked: %s -> %s", entry.asString(), downloadFolder.getAbsolutePath()));
        String path = (entry.isFolder && false == entry.isRoot()) ? entry.parentFolder() : entry.path;
        HttpClient client = new HttpClient(serverUrl, String.format(GET_FILE_COMMAND_URL_TPT, path, entry.name));
        File dstFile = new File(downloadFolder, entry.isFolder ? String.format("%s.tgz", entry.name) : entry.name);
        FileOutputStream fos = new FileOutputStream(dstFile);
        client.download(fos);
        fos.close();
        LOGGER.debug(String.format("Downloaded file: %s", dstFile.getAbsolutePath()));
        return dstFile;
    }

    private static class HttpClient {
        private HttpURLConnection conn;

        private HttpClient(String serverUrlStr, String commandStr) throws MalformedURLException, IOException {
            String urlStr = String.format("%s%s", serverUrlStr, commandStr);
            LOGGER.debug(String.format("Connecting to: %s", urlStr));
            this.conn = (HttpURLConnection) new URL(urlStr).openConnection();
            this.conn.setRequestProperty("Content-Type", "application/octet-stream");
            this.conn.setReadTimeout(Integer.MAX_VALUE);
            this.conn.setRequestMethod("GET");
            this.conn.setUseCaches(false);
            this.conn.setDoInput(true);
            this.conn.setDoOutput(true);
            this.conn.connect();
            LOGGER.debug("Connected");
        }

        private void download(OutputStream out) throws IOException {
            LOGGER.debug("Starting download");
            long init = System.currentTimeMillis();
            long transferredBytes = IOUtils.readFromTo(this.conn.getInputStream(), out);
            LOGGER.debug(String.format(
                    "Download of %d bytes took %d millis",
                    Long.valueOf(transferredBytes),
                    Long.valueOf(System.currentTimeMillis() - init)));
        }
    }
}