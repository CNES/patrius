/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Update for robustness
 * VERSION::DM:1174:26/06/2017:allow incomplete coefficients files
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Reader for the GRGS gravity field format.
 * 
 * <p>
 * This format was used to describe various gravity fields at GRGS (Toulouse).
 * 
 * <p>
 * The proper way to use this class is to call the {@link GravityFieldFactory} which will determine which reader to use
 * with the selected potential coefficients file
 * <p>
 * 
 * @see GravityFieldFactory
 * @author Luc Maisonobe
 */
public class GRGSFormatReader extends PotentialCoefficientsReader {

    /** Serial UID. */
    private static final long serialVersionUID = 4256198721877642903L;

    /** Patterns for lines (the last pattern is repeated for all data lines). */
    private static final Pattern[] LINES;

    static {

        // sub-patterns
        final String real = "[-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d";
        final String sep = ")\\s*(";
        final String ishere = ")?";

        // regular expression for header lines
        final String[] header = {
            "^\\s*FIELD - .*$",
            "^\\s+AE\\s+1/F\\s+GM\\s+OMEGA\\s*$",
            "^\\s*(" + real + sep + real + sep + real + sep + real + ")\\s*$",
            "^\\s*REFERENCE\\s+DATE\\s+:\\s+\\d.*$",
            "^\\s*MAXIMAL\\s+DEGREE\\s+:\\s+(\\d+)\\s.*$",
            // case insensitive for the next line
            "(?i)^\\s*(l|L)\\s+M\\s+DOT\\s+CBAR\\s+SBAR\\s+(SIGMA C)?(\\s+SIGMA S)?(\\s+LIB)?\\s*$"
        };

        // regular expression for data lines
        final String bracket = "(";
        final String data = "^([ 0-9]{3})([ 0-9]{3})(   |DOT)\\s*(" +
            real + sep + real + sep + bracket + real + ishere + sep + bracket + real + ishere +
            ")(\\s+[0-9]+)?\\s*$";

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
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     */
    public GRGSFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // FIELD - GRIM5, VERSION : C1, november 1999
        // AE 1/F GM OMEGA
        // 0.63781364600000E+070.29825765000000E+030.39860044150000E+150.72921150000000E-04
        // REFERENCE DATE : 1997.00
        // MAXIMAL DEGREE : 120 Sigmas calibration factor : .5000E+01 (applied)
        // L M DOT CBAR SBAR SIGMA C SIGMA S
        // 2 0DOT 0.13637590952454E-10 0.00000000000000E+00 .143968E-11 .000000E+00
        // 3 0DOT 0.28175700027753E-11 0.00000000000000E+00 .496704E-12 .000000E+00
        // 4 0DOT 0.12249148508277E-10 0.00000000000000E+00 .129977E-11 .000000E+00
        // 0 0 .99999999988600E+00 .00000000000000E+00 .153900E-09 .000000E+00
        // 2 0 -0.48416511550920E-03 0.00000000000000E+00 .204904E-10 .000000E+00

        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        boolean okConstants = false;
        boolean okMaxDegree = false;
        boolean okCoeffs = false;
        int lineNumber = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {

            ++lineNumber;

            // match current header or data line
            final Matcher matcher = LINES[MathLib.min(LINES.length, lineNumber) - 1].matcher(line);
            if (!matcher.matches()) {
                throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                    lineNumber, name, line);
            }

            if (lineNumber == 3) {
                // header line defining ae, 1/f, GM and Omega
                this.ae = Double.parseDouble(matcher.group(1).replace('D', 'E'));
                this.mu = Double.parseDouble(matcher.group(3).replace('D', 'E'));
                okConstants = true;
            } else if (lineNumber == 5) {
                // header line defining max degree
                final int maxDegree = Integer.parseInt(matcher.group(1));
                this.normalizedC = new double[maxDegree + 1][];
                this.normalizedS = new double[maxDegree + 1][];
                for (int k = 0; k < this.normalizedC.length; k++) {
                    this.normalizedC[k] = new double[k + 1];
                    this.normalizedS[k] = new double[k + 1];
                    if (!this.missingCoefficientsAllowed()) {
                        Arrays.fill(this.normalizedC[k], Double.NaN);
                        Arrays.fill(this.normalizedS[k], Double.NaN);
                    }
                }
                if (this.missingCoefficientsAllowed()) {
                    // set the default value for the only expected non-zero coefficient
                    this.normalizedC[0][0] = 1.0;
                }
                okMaxDegree = true;
            } else if (lineNumber > 6) {
                // data line
                if ("".equals(matcher.group(3).trim())) {
                    // non-dot data line
                    final int i = Integer.parseInt(matcher.group(1).trim());
                    final int j = Integer.parseInt(matcher.group(2).trim());
                    this.normalizedC[i][j] = Double.parseDouble(matcher.group(4).replace('D', 'E'));
                    this.normalizedS[i][j] = Double.parseDouble(matcher.group(5).replace('D', 'E'));
                    okCoeffs = true;
                }
            }

        }

        for (int k = 0; okCoeffs && k < this.normalizedC.length; k++) {
            final double[] cK = this.normalizedC[k];
            for (int i = 0; okCoeffs && i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "C", i, k);
                }
            }
            final double[] sK = this.normalizedS[k];
            for (int i = 0; okCoeffs && i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "S", i, k);
                }
            }
        }

        if (!(okConstants && okMaxDegree && okCoeffs)) {
            String loaderName = this.getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                name, loaderName);
        }

        this.readCompleted = true;

    }

}
