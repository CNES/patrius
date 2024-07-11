/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1174:26/06/2017:allow incomplete coefficients files
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

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

// CHECKSTYLE: stop NestedBlockDepth check
// Reason: model - Orekit code kept as such

/**
 * Reader for the ICGEM gravity field format.
 * 
 * <p>
 * This format is used to describe the gravity field of EIGEN models published by the GFZ Potsdam since 2004. It is
 * described in Franz Barthelmes and Christoph F&ouml;rste paper: <a
 * href="http://op.gfz-potsdam.de/grace/results/grav/g005_ICGEM-Format.pdf">the ICGEM-format</a>.
 * 
 * <p>
 * The proper way to use this class is to call the {@link GravityFieldFactory} which will determine which reader to use
 * with the selected potential coefficients file
 * <p>
 * 
 * @see GravityFieldFactory
 * @author Luc Maisonobe
 */
public class ICGEMFormatReader extends PotentialCoefficientsReader {

    /** Serial UID. */
    private static final long serialVersionUID = -3600898497359026652L;

    /** Product type. */
    private static final String PRODUCT_TYPE = "product_type";

    /** Gravity field product type. */
    private static final String GRAVITY_FIELD = "gravity_field";

    /** Gravity constant marker. */
    private static final String GRAVITY_CONSTANT = "earth_gravity_constant";

    /** Reference radius. */
    private static final String REFERENCE_RADIUS = "radius";

    /** Max degree. */
    private static final String MAX_DEGREE = "max_degree";

    /** Normalization indicator. */
    private static final String NORMALIZATION_INDICATOR = "norm";

    /** Indicator value for normalized coefficients. */
    private static final String NORMALIZED = "fully_normalized";

    /** End of header marker. */
    private static final String END_OF_HEADER = "end_of_head";

    /** Gravity field coefficient. */
    private static final String GFC = "gfc";

    /** Time stamped gravity field coefficient. */
    private static final String GFCT = "gfct";

    /** Gravity field coefficient first time derivative. */
    private static final String DOT = "dot";

    /** Tab length for gravity field coefficient or gravity field coefficient first time derivative */
    private static final int TAB_LENGTH_GFC_DOT = 7;

    /** Tab length for time stamped gravity field coefficient */
    private static final int TAB_LENGTH_GFCT = 8;

    /**
     * Simple constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     */
    public ICGEMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    @SuppressWarnings({"PMD.PreserveStackTrace", "PMD.InefficientEmptyStringCheck"})
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        boolean inHeader = true;
        boolean okMu = false;
        boolean okAe = false;
        int lineNumber = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            // Loop on all lines
            try {
                ++lineNumber;
                if (line.trim().isEmpty()) {
                    continue;
                }
                // Split line in tab
                final String[] tab = line.split("\\s+");
                if (inHeader) {
                    if ((tab.length == 2) && PRODUCT_TYPE.equals(tab[0])) {
                        if (!GRAVITY_FIELD.equals(tab[1])) {
                            // Parse exception
                            throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && GRAVITY_CONSTANT.equals(tab[0])) {
                        this.mu = Double.parseDouble(tab[1].replace('D', 'E'));
                        okMu = true;
                    } else if ((tab.length == 2) && REFERENCE_RADIUS.equals(tab[0])) {
                        this.ae = Double.parseDouble(tab[1].replace('D', 'E'));
                        okAe = true;
                    } else if ((tab.length == 2) && MAX_DEGREE.equals(tab[0])) {

                        final int maxDegree = Integer.parseInt(tab[1]);

                        // allocate arrays
                        this.normalizedC = new double[maxDegree + 1][];
                        this.normalizedS = new double[maxDegree + 1][];
                        for (int k = 0; k < this.normalizedC.length; k++) {
                            this.normalizedC[k] = new double[k + 1];
                            this.normalizedS[k] = new double[k + 1];
                            if (!this.missingCoefficientsAllowed()) {
                                // Fill array with NaN to recognize if a coefficient is missing
                                Arrays.fill(this.normalizedC[k], Double.NaN);
                                Arrays.fill(this.normalizedS[k], Double.NaN);
                            }
                        }
                        if (this.missingCoefficientsAllowed()) {
                            // set the default value for the only expected non-zero coefficient
                            this.normalizedC[0][0] = 1.0;
                        }

                    } else if ((tab.length == 2) && NORMALIZATION_INDICATOR.equals(tab[0])) {
                        if (!NORMALIZED.equals(tab[1])) {
                            // Parse exception
                            throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && END_OF_HEADER.equals(tab[0])) {
                        // The end of the header has been reached
                        inHeader = false;
                    }
                } else {
                    if (((tab.length == TAB_LENGTH_GFC_DOT) && GFC.equals(tab[0])) ||
                            ((tab.length == TAB_LENGTH_GFCT) && GFCT.equals(tab[0]))) {
                        final int degree = Integer.parseInt(tab[1]);
                        final int order = Integer.parseInt(tab[2]);
                        this.normalizedC[degree][order] = Double.parseDouble(tab[3].replace('D', 'E'));
                        this.normalizedS[degree][order] = Double.parseDouble(tab[4].replace('D', 'E'));
                        // we ignore the time derivative records
                    } else if ((tab.length != TAB_LENGTH_GFC_DOT) || !DOT.equals(tab[0])) {
                        // Parse exception
                        throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                            lineNumber, name, line);
                    }
                }
            } catch (final NumberFormatException nfe) {
                // Exception
                throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                    lineNumber, name, line);
            }
        }
        
        // raise exceptions
        raisePatriusExceptions(okMu, okAe, name);

        // Done
        this.readCompleted = true;
    }

    /**
     * raise patrius exceptions
     * 
     * @param okMu boolean
     * @param okAe boolean
     * @param name name of the file (or zip entry)
     * @throws PatriusException if some data is missing or if some loader specific error occurs
     */
    private void raisePatriusExceptions(final boolean okMu, final boolean okAe, final Object name)
        throws PatriusException {

        // Check coefficients have been filed
        // c and s
        for (int k = 0; k < this.normalizedC.length; k++) {
            final double[] cK = this.normalizedC[k];
            for (int i = 0; i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    // Missing coefficient exception
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "C", i, k);
                }
            }
            final double[] sK = this.normalizedS[k];
            for (int i = 0; i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    // Missing coefficient exception
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "S", i, k);
                }
            }
        }

        if (!(okMu && okAe)) {
            // Format error
            String loaderName = this.getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                name, loaderName);
        }
    }

    // CHECKSTYLE: resume NestedBlockDepth check
}
