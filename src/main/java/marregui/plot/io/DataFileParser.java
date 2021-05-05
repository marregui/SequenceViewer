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

package marregui.plot.io;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataFileParser {
    public static Data parse(File file) throws Exception {
        return new DataFileParser(file).parse();
    }

    public static final String NO_VALUE = "n";
    public static final int Y_VALUE_IDX = 0;
    public static final int Y_ERROR_IDX = 1;
    private static final String EMPTY_LINE = "";

    private final File file;

    private DataFileParser(File file) {
        this.file = file;
    }

    private static String getNextLine(BufferedReader br, boolean isHeader) throws Exception {
        String line = br.readLine();
        if (null != line) {
            line = line.trim();
            if (line.startsWith("#")) {
                line = isHeader ? line = line.substring(1) : EMPTY_LINE;
            } else {
                if (line.isEmpty()) {
                    line = EMPTY_LINE;
                }
            }
        }
        return line;
    }

    private static float parseFloat(String candidate, long lineNumber, int fieldNumber) throws Exception {
        try {
            return Float.valueOf(candidate).floatValue();
        } catch (Exception e) {
            throw new Exception(String.format(
                    "Data Line %s, column %d is not a valid value: %s",
                    String.valueOf(lineNumber), String.valueOf(fieldNumber), candidate));
        }
    }

    private static class Header {
        final String sourceName;
        final int numberOfBands;
        final String[] bandNames;

        private Header(String[] parts) throws Exception {
            if (parts.length <= 1) {
                throw new Exception("Header format should be: src name, flux name 1, ..., flux name n");
            }
            this.numberOfBands = parts.length - 1;
            this.sourceName = parts[0];
            this.bandNames = new String[this.numberOfBands];
            for (int i = 0; i < this.numberOfBands; i++) {
                this.bandNames[i] = parts[i + 1].trim();
            }
        }

        static Header parseHeader(BufferedReader br) throws Exception {
            for (String line = null; null != (line = getNextLine(br, true)); ) {
                if (EMPTY_LINE != line) {
                    return new Header(line.split("[,]"));
                }
            }
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Source Name: ").append(this.sourceName).append("\r\n");
            sb.append("Number of Bands: ").append(String.valueOf(this.numberOfBands)).append("\r\n");
            sb.append("Band Names:\r\n");
            for (String fn : this.bandNames) {
                sb.append(" - '").append(fn).append("'\r\n");
            }
            return sb.toString();
        }
    }

    private static class Band {
        float flux, error;
        boolean isValid;

        private void parseBand(String[] parts, long lineNumber, int bandStartOffset) throws Exception {
            this.flux = 0.0F;
            this.error = 0.0F;
            this.isValid = false;
            String yval = parts[bandStartOffset];
            String yerr = parts[bandStartOffset + 1];
            if (null == yval || yval.trim().toLowerCase().equals(NO_VALUE) ||
                    null == yerr || yerr.trim().toLowerCase().equals(NO_VALUE)) {
                System.out.printf(
                        "Data Line %s, columns %s, %s have no value\n",
                        String.valueOf(lineNumber), String.valueOf(1 + bandStartOffset), String.valueOf(1 + bandStartOffset + 1));
                return;
            }
            this.flux = parseFloat(yval, lineNumber, 1 + bandStartOffset);
            this.error = parseFloat(yerr, lineNumber, 1 + bandStartOffset + 1);
            this.isValid = true;
        }

        static Data parseData(BufferedReader br) throws Exception {
            Header header = Header.parseHeader(br);
            if (null == header) {
                throw new Exception("No header found");
            }
            Points xPoints = new Points(true);
            Map<String, Points[]> pointsInBand = new LinkedHashMap<String, Points[]>();
            for (int i = 0; i < header.numberOfBands; i++) {
                // Flux, negative error, positive error
                pointsInBand.put(header.bandNames[i], new Points[]{new Points(false), new Points(true)});
            }

            // The parsing
            long lineNumber = 1;
            final int expectedNumberOfFields = 1 + (header.numberOfBands * 2);
            for (String line = null; null != (line = getNextLine(br, false)); ) {
                if (EMPTY_LINE != line) {
                    String[] parts = line.split("[,]");
                    if (parts.length != expectedNumberOfFields) {
                        System.err.printf(
                                "Data Line %s does not contain the correct number of fields: %s\r\n",
                                String.valueOf(lineNumber), line);
                        continue;
                    }
                    Band band = new Band();
                    xPoints.addValidPoint(parseFloat(parts[0], lineNumber, 0));
                    for (int i = 0; i < header.numberOfBands; i++) {
                        band.parseBand(parts, lineNumber, 1 + (i * 2));
                        Points[] values = pointsInBand.get(header.bandNames[i]);
                        if (band.isValid) {
                            values[Y_VALUE_IDX].addValidPoint(band.flux);
                            values[Y_ERROR_IDX].addValidPoint(band.error);
                        } else {
                            values[Y_VALUE_IDX].addNonValidPoint();
                            values[Y_ERROR_IDX].addNonValidPoint();
                        }
                    }
                }
                lineNumber++;
            }
            if (0 == xPoints.getSize()) {
                throw new Exception("No data available");
            }
            for (int i = 0; i < header.numberOfBands; i++) {
                pointsInBand.get(header.bandNames[i])[0].done();
            }

            Map<String, DataSet> data = new LinkedHashMap<String, DataSet>();
            xPoints.done();
            for (String bandName : pointsInBand.keySet()) {
                data.put(bandName, new DataSet(bandName, xPoints, pointsInBand.get(bandName)));
            }
            return new Data(header.sourceName, data);
        }
    }

    private Data parse() throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(this.file));
            Data data = Band.parseData(br);
            data.setFilePath(this.file.getAbsolutePath());
            return data;
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (Throwable t) {
                    /* no-op */
                }
            }
        }
    }
}