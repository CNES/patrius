/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history created 07/11/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Reader for the GRGS RL02 gravity field format.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment because of static fields
 * 
 * @author Pierre Cardoso, Rami Houdroge
 * 
 * @version $Id: GRGSRL02FormatReader.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
public class GRGSRL02FormatReader extends VariablePotentialCoefficientsReader {

    /** Patterns for lines (the last pattern is repeated for all data lines). */
    private static final Pattern[] LINES;
    /** Static coef buffer */
    private final Map<DegOrdKey, double[]> staticBuf = new ConcurrentHashMap<DegOrdKey, double[]>();
    /** DOT coef buffer */
    private final Map<DegOrdKey, double[]> dotBuf = new ConcurrentHashMap<DegOrdKey, double[]>();
    /** C1A coef buffer */
    private final Map<DegOrdKey, double[]> c1aBuf = new ConcurrentHashMap<DegOrdKey, double[]>();
    /** S1A coef buffer */
    private final Map<DegOrdKey, double[]> s1aBuf = new ConcurrentHashMap<DegOrdKey, double[]>();
    /** C2A coef buffer */
    private final Map<DegOrdKey, double[]> c2aBuf = new ConcurrentHashMap<DegOrdKey, double[]>();
    /** S2A coef buffer */
    private final Map<DegOrdKey, double[]> s2aBuf = new ConcurrentHashMap<DegOrdKey, double[]>();
    /** DegOrdKey list */
    private final Set<DegOrdKey> entries = new HashSet<DegOrdKey>();

    static {

        // sub-patterns
        final String real = "[-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d";
        final String sep = ")\\s*(";

        // regular expression for header lines
        final String[] header = { "^\\s*FIELD - .*$", "^\\s+AE\\s+1/F\\s+GM\\s+OMEGA\\s*$",
            "^\\s*(" + real + sep + real + sep + real + sep + real + ")\\s*$",
            "^\\s*REFERENCE\\s+DATE\\s+:\\s+([0-9]{4}).*$", "^\\s*MAXIMAL\\s+DEGREE\\s+:\\s+(\\d+)\\s.*$",
            // case insensitive for the next line
            "(?i)^\\s*L\\s+M\\s+DOT\\s+CBAR\\s+SBAR\\s+SIGMA C\\s+SIGMA S(\\s+LIB)?\\s*$" };

        // regular expression for data lines
        final String data = "^([ 0-9]{3})([ 0-9]{3})(   |DOT|C1A|S1A|C2A|S2A|SUM)\\s*(" + real + sep + real + sep
                + real + sep + real + ")(\\s+[0-9]+)?\\s*$";

        // compile the regular expressions
        LINES = new Pattern[header.length + 1];
        for (int i = 0; i < header.length; ++i) {
            LINES[i] = Pattern.compile(header[i]);
        }
        LINES[LINES.length - 1] = Pattern.compile(data);

    }

    /**
     * Simple constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    public GRGSRL02FormatReader(final String supportedNames) {
        super(supportedNames);
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, PatriusException {

        // Below : file sample

        // FIELD - EIGEN, VERSION : dyd_out.cumul_GRACE_LAGEOS.2003_2010.dg_150.G_O.test_14.CORRSUM
        // AE 1/F GM OMEGA
        // 0.63781364600000E+070.29825765000000E+030.39860044150000E+150.72921150000000E-04
        // REFERENCE DATE : 2005.00 Tide convention: "TIDE FREE"
        // MAXIMAL DEGREE : 160 Sigmas calibration factor : .2000E+01 (already applied)
        // l m dot cbar sbar sigma c sigma s
        // 0 0 1.0000000000000E+00 0.0000000000000E+00 0.000000E+00 0.000000E+00
        // 1 0 -2.8365362896910E-10 0.0000000000000E+00 6.939550E-12 0.000000E+00
        // 1 0DOT -8.4383910578618E-11 0.0000000000000E+00 6.865640E-13 0.000000E+00
        // 1 0C1A 5.3410240653938E-11 0.0000000000000E+00 4.210980E-12 0.000000E+00
        // 1 0S1A -2.1911961446260E-11 0.0000000000000E+00 4.387360E-12 0.000000E+00
        // 1 0C2A -5.4167490912730E-11 0.0000000000000E+00 4.183900E-12 0.000000E+00
        // 1 0S2A -5.2184699439340E-11 0.0000000000000E+00 4.274160E-12 0.000000E+00
        // 1 0SUM 9.7720535456835E-13 0.0000000000000E+00 0.000000E+00 0.000000E+00
        // 2 0 -4.8416529907354E-04 0.0000000000000E+00 3.670500E-13 0.000000E+00

        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        boolean okConstants = false;
        boolean okMaxDegree = false;
        DegOrdKey key;
        int lineNumber = 0;
        int fileYear = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {

            ++lineNumber;

            // match current header or data line
            final Matcher matcher = LINES[MathLib.min(LINES.length, lineNumber) - 1].matcher(line);
            if (!matcher.matches()) {
                throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber,
                    name, line);
            }

            if (lineNumber == 3) {
                // header line defining ae, 1/f, GM and Omega
                this.setAe(Double.parseDouble(matcher.group(1).trim()));
                this.setMu(Double.parseDouble(matcher.group(3).trim()));
                okConstants = true;
            } else if (lineNumber == 4) {
                fileYear = Integer.parseInt(matcher.group(1));
            } else if (lineNumber == 5) {
                // header line defining max degree
                final int maxDegree = Integer.parseInt(matcher.group(1).trim());
                this.setMaxDegree(maxDegree);
                // fill the map up to maxDegree + 1 with lists
                // (+1 since there is a degree zero)
                for (int i = 0; i < maxDegree + 1; i++) {
                    this.put(i);
                }

                // normalizedC = new double[maxDegree + 1][];
                // normalizedS = new double[maxDegree + 1][];
                // for (int k = 0; k < normalizedC.length; k++) {
                // normalizedC[k] = new double[k + 1];
                // normalizedS[k] = new double[k + 1];
                // if (!missingCoefficientsAllowed) {
                // Arrays.fill(normalizedC[k], Double.NaN);
                // Arrays.fill(normalizedS[k], Double.NaN);
                // }
                // }
                // if (missingCoefficientsAllowed) {
                // // set the default value for the only expected non-zero coefficient
                // normalizedC[0][0] = 1.0;
                // }

                okMaxDegree = true;
            } else if (lineNumber > 6) {
                // data line

                // common data
                final int degree = Integer.parseInt(matcher.group(1).trim());
                final int order = Integer.parseInt(matcher.group(2).trim());
                final String type = matcher.group(3).trim();
                final double cbar = Double.parseDouble(matcher.group(4).trim());
                final double sbar = Double.parseDouble(matcher.group(5).trim());

                // The two following lines are commented since sigc and sigs are unused
                // final double sigc = Double.parseDouble(matcher.group(6));
                // final double sigs = Double.parseDouble(matcher.group(7));

                // buffer array
                final double[] smolBuf = new double[2];
                smolBuf[0] = cbar;
                smolBuf[1] = sbar;

                // store all keys (a one key per degree and order, overwritten if exists already)
                key = new DegOrdKey(degree, order);
                this.entries.add(key);

                this.bufferData(key, type, smolBuf);
            }

        }

        this.checkValidity(name, okConstants, okMaxDegree);

        this.storeData(fileYear);

    }

    /**
     * Checks that the file is read correctly.
     * 
     * @param name
     *        name of file
     * @param okConstants
     *        constants ok ?
     * @param okMaxDegree
     *        max degree ok ?
     * @throws PatriusException
     *         if something went wrong (any of the booleans false)
     * 
     */
    private void checkValidity(final String name, final boolean okConstants,
                               final boolean okMaxDegree) throws PatriusException {
        // check if data was correctly read and stored
        if (!(okConstants && okMaxDegree && !this.staticBuf.isEmpty())) {
            String loaderName = this.getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
        }
    }

    /**
     * Stores buffered data.
     * 
     * @param key
     *        DegOrdKey
     * @param type
     *        type of data
     * @param smolBuf
     *        data to store
     */
    private void bufferData(final DegOrdKey key, final String type, final double[] smolBuf) {
        // store in relevant buffer
        if ("".equals(type)) {
            this.staticBuf.put(key, smolBuf);
        } else if ("DOT".equals(type)) {
            this.dotBuf.put(key, smolBuf);
        } else if ("C1A".equals(type)) {
            this.c1aBuf.put(key, smolBuf);
        }

        this.bufferDataContinued(key, type, smolBuf);
    }

    /**
     * Stores buffered data. Separated for cyclomatic complexity.
     * 
     * @param key
     *        DegOrdKey
     * @param type
     *        type of data
     * @param smolBuf
     *        data to store
     */
    private void bufferDataContinued(final DegOrdKey key, final String type,
                                     final double[] smolBuf) {
        if ("S1A".equals(type)) {
            this.s1aBuf.put(key, smolBuf);
        } else if ("C2A".equals(type)) {
            this.c2aBuf.put(key, smolBuf);
        } else if ("S2A".equals(type)) {
            this.s2aBuf.put(key, smolBuf);
        }
    }

    /**
     * Stores data.
     * 
     * @param fileYear
     *        is file year read correctly
     * @throws PatriusException
     *         ff something went wrong when reading the file
     */
    private void storeData(final int fileYear) throws PatriusException {
        // store data in dedicated map
        double[] sBuff;
        double[] dot;
        double[] c1a;
        double[] s1a;
        double[] c2a;
        double[] s2a;
        double[] cc;
        double[] sc;

        for (final DegOrdKey current : this.entries) {
            // get data for the current degree and order:
            sBuff = this.staticBuf.get(current);
            dot = this.dotBuf.get(current);
            c1a = this.c1aBuf.get(current);
            c2a = this.c2aBuf.get(current);
            s1a = this.s1aBuf.get(current);
            s2a = this.s2aBuf.get(current);

            // these arrays contain the corrections in the following order
            // see VariablePotentialCoefficientsSet
            // {DOT, S1A, C1A, S2A, C2A}

            cc = new double[5];
            sc = new double[5];

            if (dot != null) {
                cc[0] = dot[0];
                sc[0] = dot[1];
            }
            if (s1a != null) {
                cc[1] = s1a[0];
                sc[1] = s1a[1];
            }
            if (c1a != null) {
                cc[2] = c1a[0];
                sc[2] = c1a[1];
            }
            if (s2a != null) {
                cc[3] = s2a[0];
                sc[3] = s2a[1];
            }
            if (c2a != null) {
                cc[4] = c2a[0];
                sc[4] = c2a[1];
            }
            // new set of coefficients:
            final VariablePotentialCoefficientsSet newSet = new VariablePotentialCoefficientsSet(current.deg,
                current.ord, sBuff[0], sBuff[1], cc, sc);
            // add the new set to the reader:
            this.add(newSet);
        }

        // put indicator for completed read at true
        this.setReadCompleted(true);
        // store the new file year
        this.setYear(fileYear);
    }

    /** Private class, represents a Degree and Order key for the {@link VariablePotentialCoefficientsSet} */
    private static final class DegOrdKey implements Comparable<DegOrdKey> {

        /**
         * Offset for hashCode computation
         */
        private static final int OFFSET = 10000;

        /** degree */
        private final int deg;

        /** order */
        private final int ord;

        /**
         * private constructor
         * 
         * @param degree
         *        degree of set
         * @param order
         *        order of set
         */
        public DegOrdKey(final int degree, final int order) {
            this.deg = degree;
            this.ord = order;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final DegOrdKey o) {
            final int result;
            final int diff = this.hashCode() - o.hashCode();
            if (diff == 0) {
                result = 0;
            } else if (diff > 0) {
                result = 1;
            } else {
                result = -1;
            }
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.deg * OFFSET + this.ord;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object o) {
            boolean rez = false;
            if (o instanceof DegOrdKey) {
                rez = this.compareTo((DegOrdKey) o) == 0;
            }
            return rez;
        }

    }

}
