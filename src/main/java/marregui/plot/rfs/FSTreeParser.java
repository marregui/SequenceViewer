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


import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Stack;

/**
 * Parses XML representing a file system tree structure
 *
 * @author marregui
 */
public class FSTreeParser extends DefaultHandler {
    public static final String NAME_ATTR = "name";
    public static final String PATH_ATTR = "path";
    private static final String SIZE_ATTR = "size";
    private static final String LAST_MODIFIED_ATTR = "lastModified";
    private static final String IS_FOLDER_ATTR = "isFolder";
    private static final String ENCODING = "UTF-8";
    private static final String ENTRY_TAG = "entry";
    private static final String CONTENTS_TAG = "contents";

    /**
     * @param url
     * @return An FSEntry representing the entire tree described in the XML file
     * @throws Exception
     */
    public static FSEntry parse(URL url) throws Exception {
        return parse(IOUtils.fetchFileContents(url.getPath()));
    }

    /**
     * @param fileContents
     * @return An FSEntry representing the entire tree described in the XML file
     * @throws Exception
     */
    public static FSEntry parse(String fileContents) throws Exception {
        InputSource source = new InputSource(new StringReader(fileContents));
        source.setEncoding(ENCODING);
        return new FSTreeParser().parse(source);
    }

    private XMLReader parser;
    private Stack<FSEntry> entryStack;
    private Stack<String> pathStack;

    private FSTreeParser() throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = parserFactory.newSAXParser();
        this.parser = saxParser.getXMLReader();
        this.parser.setContentHandler(this);
        this.parser.setErrorHandler(this);
        this.parser.setDTDHandler(this);
        this.parser.setEntityResolver(this);
        this.parser.setFeature("http://xml.org/sax/features/validation", true);
        this.parser.setFeature("http://xml.org/sax/features/namespaces", true);
        this.parser.setProperty("http://apache.org/xml/properties/input-buffer-size", Integer.valueOf(IOUtils.BUFFER_SIZE));
        this.entryStack = new Stack<FSEntry>();
        this.pathStack = new Stack<String>();
    }

    private FSEntry parse(InputSource source) throws Exception {
        this.parser.parse(source);
        return this.entryStack.isEmpty() ? null : this.entryStack.pop();
    }

    @Override
    public void startElement(String uri, String localName, String rawName, Attributes attr) {
        if (ENTRY_TAG.equals(rawName)) {
            String name = attr.getValue(NAME_ATTR);
            String path = attr.getValue(PATH_ATTR);
            long size = Long.valueOf(attr.getValue(SIZE_ATTR));
            long lastModified = Long.valueOf(attr.getValue(LAST_MODIFIED_ATTR)).longValue();
            boolean isFolder = Boolean.valueOf(attr.getValue(IS_FOLDER_ATTR)).booleanValue();
            if (isFolder) {
                if (false == this.pathStack.isEmpty()) {
                    path = String.format("%s%s%s", this.pathStack.peek(), File.separator, name);
                }
                this.pathStack.push(path);
            } else {
                path = this.pathStack.peek();
            }
            FSEntry entry = FSEntry.entry(name, path, size, lastModified, isFolder);
            if (false == this.entryStack.isEmpty()) {
                this.entryStack.peek().addFSEntry(entry);
            }
            if (isFolder) {
                this.entryStack.push(entry);
            }
        }
    }

    @Override
    public void characters(char[] chars, int start, int length) {
        // No - op
    }

    @Override
    public void endElement(String uri, String localName, String rawName) {
        if (CONTENTS_TAG.equals(rawName)) {
            if (this.entryStack.size() > 1) {
                this.entryStack.pop();
                this.pathStack.pop();
            }
        }
    }
}