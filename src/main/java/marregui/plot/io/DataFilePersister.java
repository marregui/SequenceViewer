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


import java.io.File;
import java.io.PrintWriter;

import marregui.plot.AxisLabels;

public class DataFilePersister {


    public static void persist(Data data, File file) throws Exception {
        new DataFilePersister(data, file).persist();
    }

    private static final String SEPARATOR = ", ";

    private final Data data;
    private final File file;

    private DataFilePersister(Data data, File file) {
        this.data = data;
        this.file = file;
    }

    private void writeHeader(PrintWriter pw) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(this.data.sourceName).append(SEPARATOR);
        for (String bandName : this.data.pointsPerBand.keySet()) {
            sb.append(bandName).append(SEPARATOR);
        }
        sb.setLength(sb.length() - SEPARATOR.length());
        pw.println(sb.toString());
    }

    private void writeBands(PrintWriter pw) throws Exception {
        StringBuilder sb = new StringBuilder();

        // First band
        DataSet firstDataSet = this.data.pointsPerBand.values().iterator().next();
        for (int i = 0; i < firstDataSet.getSize(); i++) {
            sb.setLength(0);
            sb.append(AxisLabels.formatForXAxis(firstDataSet.xValues.get(i))).append(SEPARATOR);
            int numberOfValidBands = 0;
            for (String bandName : this.data.pointsPerBand.keySet()) {
                DataSet dataSet = this.data.pointsPerBand.get(bandName);
                String yval = DataFileParser.NO_VALUE;
                String yerr = DataFileParser.NO_VALUE;
                if (dataSet.isValid(i)) {
                    yval = AxisLabels.formatForYAxis(dataSet.yValues.get(i));
                    yerr = AxisLabels.formatForYAxis(dataSet.yError.get(i));
                    numberOfValidBands++;
                }
                sb.append(yval).append(SEPARATOR);
                sb.append(yerr).append(SEPARATOR);
            }
            if (numberOfValidBands > 0) {
                sb.setLength(sb.length() - SEPARATOR.length());
                pw.println(sb.toString());
            }
        }
        pw.println("# Eof");
    }

    private void persist() throws Exception {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(this.file, "UTF-8");
            writeHeader(pw);
            writeBands(pw);
        } finally {
            if (null != pw) {
                try {
                    pw.close();
                } catch (Throwable t) {
                    /* no-op */
                }
            }
        }
    }
}