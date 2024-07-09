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
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:94:09/10/2013:Updated 2D propagator
 * VERSION::DM:211:08/04/2014:Modified analytical 2D propagator
 * VERSION::DM:266:29/04/2015:add various centered analytical models
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
import fr.cnes.sirius.patrius.propagation.analytical.twod.DatePolynomialFunction;
import fr.cnes.sirius.patrius.propagation.analytical.twod.UnivariateDateFunction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;

class ParameterModelReader {

    /** regERxp for integer */
    private static final String INT = "[-]?\\d+";

    /** regERxp for real number */
    private static final String REAL = "[-]?\\d+\\.\\d+[eE]?-?\\d*+";

    /** regExp for # 1.28497956820648e-06 0.0 */
    private static final String POLYPART = "\\#\\s+(" + REAL + ")+\\s+(" + REAL + ")\\s*+";

    /** # 0.00228727089711216 0.00103696259570654 2.62468386607797 -7.27220521663104e-05 */
    private static final String HARMPART = "\\#\\s+(" + REAL + ")\\s+(" + REAL + ")\\s+(" + REAL + ")\\s+(" + REAL
        + ")\\s*+";

    /** 2 0 8978.69225155589 -0.000110494660997245 */
    private static final String HARMCOEFS = "\\s*(" + INT + ")\\s+(" + INT + ")\\s(" + REAL + ")\\s(" + REAL + ")\\s*+";

    /** pso lna */
    private double[][] commonPolynomials;

    /** polynomial part */
    private double[] polynomialPart;

    /** trigonometric part */
    private double[][] harmonic;

    private String date;

    /**
     * read data
     * 
     * @param filename
     *        read
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     */
    public void readData(final String filename) throws URISyntaxException, IOException {

        final String path = ParameterModelReader.class.getClassLoader().getResource(filename).toURI().getPath();
        final File file = new File(path);

        final FileInputStream fis = new FileInputStream(file);
        final BufferedReader bf = new BufferedReader(new InputStreamReader(fis));

        bf.readLine();
        bf.readLine();
        String line = bf.readLine();

        // # t0 (TAI) = 2020-01-01T11:50:35.000
        // System.out.println(line);
        // System.out.println(line.substring(13, 36));
        this.date = line.substring(15, 38);

        // # Partie polynomiale: A + B*t (m)
        // A + B * t
        Pattern pattern = Pattern.compile(POLYPART);

        bf.readLine();
        line = bf.readLine();
        Matcher matcher = pattern.matcher(line);
        if (!matcher.matches()) {
            Assert.fail();
        }

        final double A = Double.parseDouble(matcher.group(1));
        final double B = Double.parseDouble(matcher.group(2));

        this.polynomialPart = new double[] { A, B };

        // # psoM0, psoMp, lna0, lnap:
        pattern = Pattern.compile(HARMPART);

        bf.readLine();
        bf.readLine();
        line = bf.readLine();

        matcher = pattern.matcher(line);
        if (!matcher.matches()) {
            Assert.fail();
        }
        final double psoM0 = Double.parseDouble(matcher.group(1));
        final double psoMp = Double.parseDouble(matcher.group(2));
        final double lna0 = Double.parseDouble(matcher.group(3));
        final double lnap = Double.parseDouble(matcher.group(4));

        this.commonPolynomials = new double[][] { { psoM0, psoMp }, { lna0, lnap } };

        // # Col 1: n
        // # Col 2: k
        // # Col 3: amp (m)
        // # Col 4: phi (rad)
        // 3 0 0.000730117483300990 -0.000165710970947441
        pattern = Pattern.compile(HARMCOEFS);

        bf.readLine();
        bf.readLine();
        bf.readLine();
        bf.readLine();

        final ArrayList<double[]> temp = new ArrayList<double[]>();

        for (line = bf.readLine(); line != null; line = bf.readLine()) {

            matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                Assert.fail();
            }

            final int n = Integer.parseInt(matcher.group(1));
            final int k = Integer.parseInt(matcher.group(2));
            final double amp = Double.parseDouble(matcher.group(3));
            final double phi = Double.parseDouble(matcher.group(4));
            temp.add(new double[] { n, k, amp, phi });
        }

        this.harmonic = new double[temp.size()][4];
        for (int i = 0; i < this.harmonic.length; i++) {
            this.harmonic[i] = temp.get(i);
        }
    }

    /**
     * @return the commonPolynomials
     */
    public double[][] getCommonPolynomials() {
        return this.commonPolynomials;
    }

    /**
     * @return the polynomialPart
     */
    public double[] getPolynomialPart() {
        return this.polynomialPart;
    }

    /**
     * @return the harmonic
     */
    public double[][] getHarmonic() {
        return this.harmonic;
    }

    /**
     * Return the parameter model
     * 
     * @return the model
     */
    public Analytical2DParameterModel getModel() {
        final UnivariateDateFunction centeredPart = new DatePolynomialFunction(new AbsoluteDate(this.getDate(),
            TimeScalesFactory.getTAI()), this.polynomialPart);
        return new Analytical2DParameterModel(centeredPart, this.harmonic);
    }

    /**
     * @return the date
     */
    public String getDate() {
        return this.date;
    }

}
