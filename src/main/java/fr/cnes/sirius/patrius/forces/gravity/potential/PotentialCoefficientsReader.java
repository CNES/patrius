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
 *
 * @history created 15/11/2017
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1303:15/11/2017: Problem for high order/degree for denormalization
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.potential;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This abstract class represents a Gravitational Potential Coefficients file reader.
 * 
 * <p>
 * As it exits many different coefficients models and containers this interface represents all the methods that should
 * be implemented by a reader. The proper way to use this interface is to call the {@link GravityFieldFactory} which
 * will determine which reader to use with the selected potential coefficients file.
 * <p>
 * 
 * @see GravityFieldFactory
 * @author Fabien Maussion
 */
// CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.NullAssignment"})
public abstract class PotentialCoefficientsReader
    implements DataLoader, PotentialCoefficientsProvider {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = -4966936658950728369L;

    /** Indicator for completed read. */
    protected boolean readCompleted;

    /** Sigmas reader indicator */
    protected boolean readSigmas;

    /** Central body reference radius. */
    protected double ae;

    /** Central body attraction coefficient. */
    protected double mu;

    /** fully normalized zonal coefficients array. */
    protected double[] normalizedJ;

    /** fully normalized tesseral-sectorial coefficients matrix. */
    protected double[][] normalizedC;

    /** fully normalized tesseral-sectorial coefficients matrix. */
    protected double[][] normalizedS;

    /** fully normalized tesseral-sectorial sigma coefficients matrix. */
    protected double[][] normalizedSigmasC;

    /** fully normalized tesseral-sectorial sigma coefficients matrix. */
    protected double[][] normalizedSigmasS;

    /** un-normalized zonal coefficients array. */
    private double[] unNormalizedJ;

    /** un-normalized tesseral-sectorial coefficients matrix. */
    private double[][] unNormalizedC;

    /** un-normalized tesseral-sectorial coefficients matrix. */
    private double[][] unNormalizedS;

    /** un-normalized tesseral-sectorial sigma coefficients matrix. */
    private double[][] unNormalizedSigmasC;

    /** un-normalized tesseral-sectorial sigma coefficients matrix. */
    private double[][] unNormalizedSigmasS;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Allow missing coefficients in the input data. */
    private final boolean missingCoefficientsAllowedFlag;

    /**
     * Simple constructor.
     * <p>
     * Build an uninitialized reader with a default "false" value for reading sigmas coefficient.
     * </p>
     * 
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param missingCoefficientsAllowedIn
     *        allow missing coefficients in the input data
     */
    protected PotentialCoefficientsReader(final String supportedNamesIn,
                                          final boolean missingCoefficientsAllowedIn) {
        this(supportedNamesIn, missingCoefficientsAllowedIn, false);
    }

    /**
     * Simple constructor.
     * <p>
     * Build an uninitialized reader.
     * </p>
     * 
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param missingCoefficientsAllowedIn
     *        allow missing coefficients in the input data
     * @param readSigmasIn
     *        if true, will read sigmas coefficient (sigma C & sigma S)
     */
    public PotentialCoefficientsReader(final String supportedNamesIn, final boolean missingCoefficientsAllowedIn,
                                       final boolean readSigmasIn) {
        this.supportedNames = supportedNamesIn;
        this.missingCoefficientsAllowedFlag = missingCoefficientsAllowedIn;
        this.readCompleted = false;
        this.ae = Double.NaN;
        this.mu = Double.NaN;
        this.normalizedJ = null;
        this.normalizedC = null;
        this.normalizedS = null;
        this.unNormalizedJ = null;
        this.unNormalizedC = null;
        this.unNormalizedS = null;
        this.readSigmas = readSigmasIn;
    }

    /**
     * Get the regular expression for supported files names.
     * 
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return this.supportedNames;
    }

    /**
     * Check if missing coefficients are allowed in the input data.
     * 
     * @return true if missing coefficients are allowed in the input data
     */
    public boolean missingCoefficientsAllowed() {
        return this.missingCoefficientsAllowedFlag;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return !this.readCompleted;
    }

    /** {@inheritDoc} */
    @Override
    public abstract void loadData(InputStream input, String name) throws IOException, ParseException, PatriusException;

    /** {@inheritDoc} */
    @Override
    public double[] getJ(final boolean normalized, final int n) throws PatriusException {
        if (n >= this.normalizedC.length) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, n,
                this.normalizedC.length - 1);
        }

        final double[] completeJ = normalized ? this.getNormalizedJ() : this.getUnNormalizedJ();

        // truncate the array as per caller request
        final double[] result = new double[n + 1];
        System.arraycopy(completeJ, 0, result, 0, n + 1);

        return result;

    }

    /** {@inheritDoc} */
    @Override
    public double[][] getC(final int n, final int m, final boolean normalized) throws PatriusException {
        return truncateArray(n, m, normalized ? this.getNormalizedC() : this.getUnNormalizedC());
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getS(final int n, final int m, final boolean normalized) throws PatriusException {
        return truncateArray(n, m, normalized ? this.getNormalizedS() : this.getUnNormalizedS());
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getSigmaC(final int n, final int m, final boolean normalized) throws PatriusException {
        return truncateArray(n, m, normalized ? this.getNormalizedSigmasC() : this.getUnNormalizedSigmasC());
    }

    /** {@inheritDoc} */
    @Override
    public double[][] getSigmaS(final int n, final int m, final boolean normalized) throws PatriusException {
        return truncateArray(n, m, normalized ? this.getNormalizedSigmasS() : this.getUnNormalizedSigmasS());
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return this.mu;
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return this.ae;
    }

    /**
     * Get the tesseral-sectorial and zonal coefficients.
     * 
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param complete
     *        the complete array
     * @return the trunctated coefficients array
     * @exception PatriusException
     *            if the requested maximal degree or order exceeds the
     *            available degree or order
     */
    private static double[][] truncateArray(final int n, final int m, final double[][] complete)
        throws PatriusException {

        // safety checks
        if (n >= complete.length) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, n, complete.length - 1);
        }
        if (m >= complete[complete.length - 1].length) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, m,
                complete[complete.length - 1].length - 1);
        }

        // truncate each array row in turn
        final double[][] result = new double[n + 1][];
        // Loop on the first n rows of the complete array
        for (int i = 0; i <= n; i++) {
            final double[] ri = new double[MathLib.min(i, m) + 1];
            // Copy truncated row in ri
            System.arraycopy(complete[i], 0, ri, 0, ri.length);
            result[i] = ri;
        }

        return result;

    }

    /**
     * Get the fully normalized zonal coefficients.
     * 
     * @return J the coefficients matrix
     */
    private double[] getNormalizedJ() {
        if (this.normalizedJ == null) {
            this.normalizedJ = new double[this.normalizedC.length];
            for (int i = 0; i < this.normalizedC.length; i++) {
                this.normalizedJ[i] = -this.normalizedC[i][0];
            }
        }
        return this.normalizedJ;
    }

    /**
     * Get the fully normalized tesseral-sectorial and zonal coefficients.
     * 
     * @return C the coefficients matrix
     */
    private double[][] getNormalizedC() {
        return this.normalizedC;
    }

    /**
     * Get the fully normalized tesseral-sectorial coefficients.
     * 
     * @return S the coefficients matrix
     */
    private double[][] getNormalizedS() {
        return this.normalizedS;
    }

    /**
     * Get the un-normalized zonal coefficients.
     * 
     * @return J the zonal coefficients array.
     */
    private double[] getUnNormalizedJ() {
        if (this.unNormalizedJ == null) {
            final double[][] uC = this.getUnNormalizedC();
            this.unNormalizedJ = new double[uC.length];
            for (int i = 0; i < uC.length; i++) {
                this.unNormalizedJ[i] = -uC[i][0];
            }
        }
        return this.unNormalizedJ;
    }

    /**
     * Get the un-normalized tesseral-sectorial and zonal coefficients.
     * 
     * @return C the coefficients matrix
     */
    private double[][] getUnNormalizedC() {
        // calculate only if asked
        if (this.unNormalizedC == null) {
            this.unNormalizedC = unNormalize(this.normalizedC);
        }
        return this.unNormalizedC;
    }

    /**
     * Get the un-normalized tesseral-sectorial coefficients.
     * 
     * @return S the coefficients matrix
     */
    private double[][] getUnNormalizedS() {
        // calculate only if asked
        if (this.unNormalizedS == null) {
            this.unNormalizedS = unNormalize(this.normalizedS);
        }
        return this.unNormalizedS;
    }

    /**
     * Get the fully normalized sigmas tesseral-sectorial and zonal coefficients.
     * 
     * @return S the coefficients matrix
     */
    private double[][] getNormalizedSigmasC() {
        return this.normalizedSigmasC;
    }

    /**
     * Get the fully normalized sigmas tesseral-sectorial coefficients.
     * 
     * @return S the coefficients matrix
     */
    private double[][] getNormalizedSigmasS() {
        return this.normalizedSigmasS;
    }

    /**
     * Get the un-normalized sigmas tesseral-sectorial and zonal coefficients.
     * 
     * @return S the coefficients matrix
     */
    private double[][] getUnNormalizedSigmasC() {
        if (this.unNormalizedSigmasC == null) {
            this.unNormalizedSigmasC = unNormalize(this.normalizedSigmasC);
        }
        return this.unNormalizedSigmasC;
    }

    /**
     * Get the un-normalized sigmas tesseral-sectorial coefficients.
     * 
     * @return S the coefficients matrix
     */
    private double[][] getUnNormalizedSigmasS() {
        if (this.unNormalizedSigmasS == null) {
            this.unNormalizedSigmasS = unNormalize(this.normalizedSigmasS);
        }
        return this.unNormalizedSigmasS;
    }

    /**
     * Unnormalize a coefficients array.
     * 
     * @param normalized
     *        normalized coefficients array
     * @return unnormalized array
     */
    private static double[][] unNormalize(final double[][] normalized) {

        // allocate a triangular array
        final double[][] unNormalized = new double[normalized.length][];
        // Initialize first value
        unNormalized[0] = new double[] {
            normalized[0][0]
        };

        // unnormalize the coefficients
        for (int n = 1; n < normalized.length; n++) {
            final double[] tab = new double[n + 1];
            final double[] uRow = new double[n + 1];
            final double[] nRow = normalized[n];
            final double coeffN = 2.0 * (2 * n + 1);
            uRow[0] = MathLib.sqrt(2 * n + 1) * normalized[n][0];
            for (int m = 1; m < uRow.length; m++) {
                // Decomposition of the previous calculation
                if (m == 1) {
                    tab[m] = MathLib.sqrt(coeffN / ((n + m) * (n - m + 1)));
                } else {
                    tab[m] = tab[m - 1] * MathLib.sqrt(1.0 / ((n + m) * (n - m + 1)));
                }
                uRow[m] = tab[m] * nRow[m];
            }
            // Set unnormalized row
            unNormalized[n] = uRow;
        }

        // Return result
        return unNormalized;
    }
}
