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
* VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
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
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This reader is adapted to the EGM Format.
 * 
 * <p>
 * The proper way to use this class is to call the {@link GravityFieldFactory} which will determine which reader to use
 * with the selected potential coefficients file
 * <p>
 * 
 * @see GravityFieldFactory
 * @author Fabien Maussion
 */
public class EGMFormatReader extends PotentialCoefficientsReader {

     /** Serializable UID. */
    private static final long serialVersionUID = -9032200314611552426L;

    /** Header length. */
    private static final int HEADER_LENGTH = 15;

    /**
     * Simple constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     */
    public EGMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);

        this.ae = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        this.mu = Constants.EGM96_EARTH_MU;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        final BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        final List<double[]> cl = new ArrayList<>();
        final List<double[]> sl = new ArrayList<>();
        boolean okFields = true;
        // Loop on lines until the end or one is incorrect
        for (String line = r.readLine(); okFields && line != null; line = r.readLine()) {
            // For the header do nothing
            if (line.length() >= HEADER_LENGTH) {

                // get the fields defining the current the potential terms
                final String[] tab = line.trim().split("\\s+");
                if (tab.length != 6) {
                    // End of loop if not the right number of fields
                    okFields = false;
                }

                final int i = Integer.parseInt(tab[0]);
                final int j = Integer.parseInt(tab[1]);
                final double c = Double.parseDouble(tab[2]);
                final double s = Double.parseDouble(tab[3]);

                // extend the cl array if needed
                final int ck = cl.size();
                // Initialize extended array
                for (int k = ck; k <= i; ++k) {
                    final double[] d = new double[k + 1];
                    if (this.missingCoefficientsAllowed()) {
                        if (k == 0) {
                            d[0] = 1.0;
                        }
                    } else {
                        Arrays.fill(d, Double.NaN);
                    }
                    cl.add(new double[k + 1]);
                }
                final double[] cli = cl.get(i);

                // extend the sl array if needed
                final int sk = sl.size();
                // Initialize extended array
                for (int k = sk; k <= i; ++k) {
                    final double[] d = new double[k + 1];
                    if (!this.missingCoefficientsAllowed()) {
                        Arrays.fill(d, Double.NaN);
                    }
                    sl.add(new double[k + 1]);
                }
                final double[] sli = sl.get(i);

                // store the terms
                cli[j] = c;
                sli[j] = s;

            }
        }

        // Check coefficients have been filed
        // c and s
        for (int k = 0; k < cl.size(); k++) {
            final double[] cK = cl.get(k);
            for (int i = 0; i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    // Missing coefficient exception
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "C", i, k);
                }
            }
            final double[] sK = sl.get(k);
            for (int i = 0; i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    // Missing coefficient exception
                    throw new PatriusException(PatriusMessages.MISSING_GRAVITY_COEFFICIENT, "S", i, k);
                }
            }
        }

        if ((!okFields) || (cl.isEmpty())) {
            // Format error
            String loaderName = this.getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                name, loaderName);
        }

        // convert to simple triangular arrays
        this.normalizedC = cl.toArray(new double[cl.size()][]);
        this.normalizedS = sl.toArray(new double[sl.size()][]);
        this.readCompleted = true;
    }

}
