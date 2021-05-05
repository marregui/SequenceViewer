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

package marregui.plot;


import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Loads images from a local folder 'images' if found, otherwise from a remote url
 * configurable in the configuration properties file by the key 'datachest.images.folder.url'
 *
 * @author marregui
 */
public class ImageUtils {
    private static final String IMAGES_URL =
            "https://github.com/marregui/SequencesViewer/blob/main/src/main/resources/images";
    private static final Map<String, Image> IMAGE_MAP = new HashMap<>();


    /**
     * @param imageName name of the image
     * @return The image matching the name. It looks first for it in the local
     * file system
     */
    public static Image loadImage(String imageName) {
        Image image = IMAGE_MAP.get(imageName);
        try {
            if (null == image) {
                URL url = new URL(String.format("%s/%s", IMAGES_URL, imageName));
                image = Toolkit.getDefaultToolkit().getImage(url);
                IMAGE_MAP.put(imageName, image);
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
        return image;
    }
}