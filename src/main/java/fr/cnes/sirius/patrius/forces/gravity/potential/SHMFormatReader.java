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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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

/**
 * Reader for the SHM gravity field format.
 * 
 * <p>
 * This format was used to describe the gravity field of EIGEN models published by the GFZ Potsdam up to 2003. It was
 * then replaced by {@link ICGEMFormatReader ICGEM format}. The SHM format is described in <a
 * href="http://www.gfz-potsdam.de/grace/results/"> Potsdam university website</a>.
 * 
 * <p>
 * The proper way to use this class is to call the {@link GravityFieldFactory} which will determine which reader to use
 * with the selected potential coefficients file
 * <p>
 * 
 * @see GravityFieldFactory
 * @author Fabien Maussion
 */
public class SHMFormatReader extends PotentialCoefficientsReader {

     /** Serializable UID. */
    private static final long serialVersionUID = -2626720440169940271L;

    /** First field labels. */
    private static final String GRCOEF = "GRCOEF";

    /** Second field labels. */
    private static final String GRCOF2 = "GRCOF2";

    /** Start SHM index. */
    private static final int START = 49;

    /** End SHM index. */
    private static final int END = 56;

    /**
     * Simple constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     */
    public SHMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        boolean okEarth = false;
        boolean okSHM = false;
        boolean okCoeffs = false;
        // Read first line
        String line = r.readLine();
        if ((line != null) &&
            "FIRST ".equals(line.substring(0, 6)) &&
            "SHM    ".equals(line.substring(START, END))) {
            for (line = r.readLine(); line != null; line = r.readLine()) {
                // Loop on lines
                if (line.length() >= 6) {
                    final String[] tab = line.split("\\s+");

                    // read the earth values
                    if ("EARTH".equals(tab[0])) {
                        this.mu = Double.parseDouble(tab[1].replace('D', 'E'));
                        this.ae = Double.parseDouble(tab[2].replace('D', 'E'));
                        okEarth = true;
                    }

                    // initialize the arrays
                    if ("SHM".equals(tab[0])) {
                        final int i = Integer.parseInt(tab[1]);
                        this.normalizedC = new double[i + 1][];
                        this.normalizedS = new double[i + 1][];
                        for (int k = 0; k < this.normalizedC.length; k++) {
                            this.normalizedC[k] = new double[k + 1];
                            this.normalizedS[k] = new double[k + 1];
                            if (!this.missingCoefficientsAllowed()) {
                                // If missing coeffs are not allowed, fill arrays with Nan
                                // to if the coeffs have not been correctly set
                                Arrays.fill(this.normalizedC[k], Double.NaN);
                                Arrays.fill(this.normalizedS[k], Double.NaN);
                            }
                        }
                        if (this.missingCoefficientsAllowed()) {
                            // set the default value for the only expected non-zero coefficient
                            this.normalizedC[0][0] = 1.0;
                        }
                        okSHM = true;
                    }

                    // fill the arrays
                    // (only if they were initialized first!)
                    if (okSHM) {
                        try {
                            if (GRCOEF.equals(line.substring(0, 6))) {
                                final int i = Integer.parseInt(tab[1]);
                                final int j = Integer.parseInt(tab[2]);
                                this.normalizedC[i][j] = Double.parseDouble(tab[3].replace('D', 'E'));
                                this.normalizedS[i][j] = Double.parseDouble(tab[4].replace('D', 'E'));
                                okCoeffs = true;
                            }
                            if (GRCOF2.equals(tab[0])) {
                                final int i = Integer.parseInt(tab[1]);
                                final int j = Integer.parseInt(tab[2]);
                                this.normalizedC[i][j] = Double.parseDouble(tab[3].replace('D', 'E'));
                                this.normalizedS[i][j] = Double.parseDouble(tab[4].replace('D', 'E'));
                                okCoeffs = true;
                            }
                        } catch (final NumberFormatException e) {
                            // At least one of the number fields could not be parsed
                            okCoeffs = false;
                        }
                    }

                }
            }
        }

        // Check coefficients have been filed
        // c and s
        for (int k = 0; okCoeffs && k < this.normalizedC.length; k++) {
            final double[] cK = this.normalizedC[k];
            for (int i = 0; okCoeffs && i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    // Coefficient exception
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "C", i, k);
                }
            }
            final double[] sK = this.normalizedS[k];
            for (int i = 0; okCoeffs && i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    // Coefficient exception
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "S", i, k);
                }
            }
        }

        if (!(okEarth && okSHM && okCoeffs)) {
            // Format exception
            String loaderName = this.getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                name, loaderName);
        }

        // Done
        this.readCompleted = true;
    }

}
