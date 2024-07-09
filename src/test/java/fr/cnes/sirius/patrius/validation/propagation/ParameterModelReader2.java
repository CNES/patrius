/**
 * 
 * Copyright 2011-2017 CNES
 *
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
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:07/05/2015:add various centered analytical models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.propagation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

import fr.cnes.sirius.patrius.propagation.analytical.twod.Analytical2DParameterModel;
import fr.cnes.sirius.patrius.propagation.analytical.twod.DateIntervalLinearFunction;
import fr.cnes.sirius.patrius.propagation.analytical.twod.DateIntervalParabolicFunction;
import fr.cnes.sirius.patrius.propagation.analytical.twod.UnivariateDateFunction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

class ParameterModelReader2 {

    /** regERxp for integer. */
    private static final String INT = "[-]?\\d+";

    /** regERxp for real number. */
    private static final String REAL = "[-]?\\d+\\.\\d+[eE]?-?\\d*+";

    /** 2 0 8978.69225155589 -0.000110494660997245 */
    private static final String HARMCOEFS = "\\s*(" + INT + ")\\s+(" + INT + ")\\s(" + REAL + ")\\s(" + REAL + ")\\s*+";

    /**
     * read data
     * 
     * @param filename
     *        read
     * @param isParabolic
     *        true if function is a piecewise parabolic function
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     */
    public Analytical2DParameterModel
            readData(final String filename, final boolean isParabolic)
                                                                      throws URISyntaxException, IOException {

        // Get file
        final String path = ParameterModelReader2.class.getClassLoader().getResource(filename).toURI().getPath();
        final File file = new File(path);

        final FileInputStream fis = new FileInputStream(file);
        final BufferedReader bf = new BufferedReader(new InputStreamReader(fis));

        // Header
        bf.readLine();
        bf.readLine();
        bf.readLine();
        bf.readLine();
        bf.readLine();

        // Date (TAI)
        String line = bf.readLine();
        final String[] dateString = line.split(" = ")[1].split(" ");
        final double mjd = Double.parseDouble(dateString[0]);
        final double sec = Double.parseDouble(dateString[1]);
        final AbsoluteDate date = AbsoluteDate.MODIFIED_JULIAN_EPOCH.shiftedBy(mjd * Constants.JULIAN_DAY).shiftedBy(
            sec + 32.184);

        // x0
        line = bf.readLine();
        final String x0String = line.split(" = ")[1];
        final double x0 = Double.parseDouble(x0String);

        int nIntervals = 4;
        double x0Dot = 0;
        if (isParabolic) {
            line = bf.readLine();
            final String x0DotString = line.split(" = ")[1];
            x0Dot = Double.parseDouble(x0DotString);
            nIntervals = 6;
        }

        // Date intervals
        bf.readLine();
        final AbsoluteDate[] dateIntervals = new AbsoluteDate[nIntervals];
        for (int i = 0; i < dateIntervals.length; i++) {
            line = bf.readLine();
            final String dtString = line.split(" = ")[1];
            dateIntervals[i] = date.shiftedBy(Double.parseDouble(dtString));
        }

        // x0Dot
        bf.readLine();
        final double[] xDotIntervals = new double[nIntervals - 1];
        for (int i = 0; i < xDotIntervals.length; i++) {
            line = bf.readLine();
            final String xDotString = line.split(" = ")[1];
            xDotIntervals[i] = Double.parseDouble(xDotString);
        }

        // Trigonometric part
        // # Col 1: n
        // # Col 2: k
        // # Col 3: amp (m)
        // # Col 4: phi (rad)
        // 3 0 0.000730117483300990 -0.000165710970947441
        final Pattern pattern = Pattern.compile(HARMCOEFS);

        bf.readLine();
        bf.readLine();
        bf.readLine();
        bf.readLine();
        bf.readLine();
        bf.readLine();

        final ArrayList<double[]> temp = new ArrayList<double[]>();

        for (line = bf.readLine(); line != null; line = bf.readLine()) {

            final Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                Assert.fail();
            }

            final int n = Integer.parseInt(matcher.group(1));
            final int k = Integer.parseInt(matcher.group(2));
            final double amp = Double.parseDouble(matcher.group(3));
            final double phi = Double.parseDouble(matcher.group(4));
            temp.add(new double[] { n, k, amp, phi });
        }

        final double[][] harmonic = new double[temp.size()][4];
        for (int i = 0; i < harmonic.length; i++) {
            harmonic[i] = temp.get(i);
        }

        // Build model
        final UnivariateDateFunction centeredFunction;
        if (isParabolic) {
            centeredFunction = new DateIntervalParabolicFunction(x0, x0Dot, dateIntervals, xDotIntervals);
        } else {
            centeredFunction = new DateIntervalLinearFunction(x0, dateIntervals, xDotIntervals);
        }

        return new Analytical2DParameterModel(centeredFunction, harmonic);
    }
}
