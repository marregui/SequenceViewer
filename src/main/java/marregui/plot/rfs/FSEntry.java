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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Node that represents a node in the file system (file or directory)
 *
 * @author marregui
 */
public class FSEntry implements Comparable<FSEntry> {
    private static final String ROOT = "Root";

    /**
     * Factory method
     *
     * @param name
     * @param path
     * @param size
     * @param lastModified
     * @return The entry representing this file
     */
    public static FSEntry file(String name, String path, long size, long lastModified) {
        return new FSEntry(name, path, size, lastModified, false);
    }

    /**
     * Factory method
     *
     * @param name
     * @param lastModified
     * @return The entry representing this folder (size is 0, and path is '.')
     */
    public static FSEntry folder(String name, long lastModified) {
        return new FSEntry(name, ".", 0, lastModified, true);
    }

    /**
     * Factory method
     *
     * @param path
     * @param lastModified
     * @return The entry representing the root folder (size is 0, and name is 'Root')
     */
    public static FSEntry rootFolder(String path, long lastModified) {
        return new FSEntry(ROOT, path, 0, lastModified, true);
    }

    /**
     * Factory method
     *
     * @param name
     * @param path
     * @param size
     * @param lastModified
     * @param isFolder
     * @return The entry
     */
    public static FSEntry entry(String name, String path, long size, long lastModified, boolean isFolder) {
        return new FSEntry(name, path, size, lastModified, isFolder);
    }

    public final String path, name;
    public final long size;
    public final long lastModified;
    public final boolean isFolder;
    private List<FSEntry> contents;

    private FSEntry(String name, String path, long size, long lastModified, boolean isFolder) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.isFolder = isFolder;
        if (this.isFolder) {
            this.contents = new ArrayList<FSEntry>();
        }
    }

    /**
     * @return Path of the parent folder
     */
    public String parentFolder() {
        StringBuilder sb = new StringBuilder(this.path);
        sb.setLength(sb.length() - (this.name.length() + 1));
        return sb.toString();
    }

    /**
     * @param name
     * @return Whether the name matches the name used by the root node
     */
    public static boolean isRoot(String name) {
        return ROOT.equals(name);
    }

    /**
     * @return Whether the node is the root node
     */
    public boolean isRoot() {
        return isRoot(this.name);
    }

    /**
     * @return Unmodifiable list of FSEntry contained by this node
     * (empty in the case of a file - zero or more elements in the case of a folder)
     */
    public List<FSEntry> contents() {
        return Collections.unmodifiableList(this.contents);
    }

    /**
     * Adds the entry to this entry if it is a folder
     *
     * @param entry
     */
    public void addFSEntry(FSEntry entry) {
        if (this.isFolder) {
            this.contents.add(entry);
        }
    }

    /**
     * Prints out recursively
     */
    public void traversePrint() {
        System.out.println(asString());
        if (this.isFolder) {
            for (FSEntry entry : this.contents) {
                entry.traversePrint();
            }
        }
    }

    /**
     * @return String representation of this node
     */
    public String asString() {
        StringBuilder sb = new StringBuilder();
        if (this.isFolder) {
            sb.append(this.path).append(" (*)");
        } else {
            sb.append(this.path).append(File.separator).append(this.name)
                    .append(String.format(" - %s", IOUtils.hummanReadableSize(this.size)))
                    .append(" - last modified on '").append(new Date(this.lastModified).toString());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.name);
        if (this.isFolder) {
            if (this.contents.isEmpty()) {
                sb.append(" (Empty)");
            }
        } else {
            sb.append(" (").append(Long.valueOf(this.size)).append(" bytes)");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (null != o && o instanceof FSEntry) {
            FSEntry that = (FSEntry) o;
            boolean equals = this.name.equals(that.name)
                    && this.path.equals(that.path)
                    && this.isFolder == that.isFolder
                    && this.lastModified == that.lastModified
                    && this.size == that.size;
            if (equals && this.isFolder) {
                equals = (this.contents.size() == that.contents.size());
                if (equals) {
                    Collections.sort(this.contents);
                    Collections.sort(that.contents);
                    for (int i = 0; i < this.contents().size() && equals; i++) {
                        equals = this.contents.get(i).equals(that.contents.get(i));
                    }
                }
            }
            return equals;
        }
        return false;
    }

    @Override
    public int compareTo(FSEntry that) {
        int comp = this.name.compareTo(that.name);
        if (0 == comp) {
            comp = (int) (this.lastModified - that.lastModified);
        }
        return comp;
    }
}