/**
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
 * @history 01/10/2014:creation
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:269:01/10/2014:piecewise linear interpolations
 * VERSION::FA:386:19/12/2014:index mutualisation for ephemeris interpolation
 * VERSION::FA:417:12/02/2015:suppressed clone1DArray method
 * VERSION::FA:664:29/07/2016:corrected method value() leading to NaN value in some cases
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;

/**
 * Abstract class for linear interpolation. Allows 1, 2 or 3 dimensions.
 * Owns an optimised indices search.
 * 
 * @author Sophie LAURENS
 * @version $Id: AbstractLinearIntervalsFunction.java 17603 2017-05-18 08:28:32Z bignon $
 * @since 2.3
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class AbstractLinearIntervalsFunction {

    /** Numerical precision. */
    protected static final double EPSILON = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Maximal indices in the first dimension of fthe tab in dimension 1, 2 and 3. */
    protected int nxmax;

    /** Maximal indices in the second dimension of ftab in dimension 2 and 3. */
    protected int nymax;

    /** Maximal indices in the third dimension of ftab in dimension 3. */
    protected int nzmax;

    /** Abscissas array. */
    protected double[] xtab;

    /** Ordinates array. */
    protected double[] ytab;

    /** Heights array. */
    protected double[] ztab;

    /** xtab used for search and satisfies condition from SearchIndex. */
    protected ISearchIndex searchXIndex;

    /** ytab used for search and satisfies condition from SearchIndex. */
    protected ISearchIndex searchYIndex;

    /** ztab used for search and satisfies condition from SearchIndex. */
    protected ISearchIndex searchZIndex;

    // interpolation function
    /**
     * Interpolates in 1D.
     * 
     * @precondition x0 different from x1, and x belongs to [x0, x1[
     * 
     * @param f0
     *        : f(x0)
     * @param f1
     *        : f(x1)
     * @param x0
     *        : lower bound
     * @param x1
     *        : upper bound
     * @param x
     *        : the point where to do the interpolation
     * @return finterp : f(x) = a x + b.
     */
    protected double interp1D(final double f0, final double f1, final double x0, final double x1, final double x) {
        final double a = (f1 - f0) / (x1 - x0);
        final double b = (f0 * x1 - f1 * x0) / (x1 - x0);
        return a * x + b;
    }

    // Utility functions for 2D array
    /**
     * Interpolates in 2D by 2-successives 1D interpolation.
     * In this case, the first interpolation is in the x direction, and the second in y.
     * 
     * @precondition x0 (resp. y0) different from x1 (resp. y1), and x (resp. y) belongs to [x0, x1[ (resp. [y0, y1[).
     * 
     * @param fx0y0
     *        : f(x0,y0): lower point for the 1st interpolation (the lower bound for the 2nd interpolation).
     * @param fx1y0
     *        : f(x1,y0): upper point for the 1st interpolation (the lower bound for the 2nd interpolation).
     * @param fx0y1
     *        : f(x0,y1): lower point for the 2nd interpolation (the upper bound for the 2nd interpolation).
     * @param fx1y1
     *        : f(x1,y1): upper point for the 2nd interpolation (the upper bound for the 2nd interpolation).
     * @param x0
     *        : lower bound for the first direction
     * @param x1
     *        : upper bound for the first direction
     * @param y0
     *        : lower bound for the second direction
     * @param y1
     *        : upper bound for the second direction
     * @param x
     *        : the point where to do the interpolation in the 1st direction
     * @param y
     *        : the point where to do the interpolation in the 2nd direction
     * @return finterp : f(x,y) = b f(x,y0) + (1-b) f(x,y1), and
     *         f(x,y0) = a0 f(x0,y0) + (1-a0) f(x1,y0)
     *         f(x,y1) = a1 f(x0,y1) + (1-a1) f(x1,y1).
     */
    protected double interp2D(final double fx0y0, final double fx1y0, final double fx0y1, final double fx1y1,
                              final double x0, final double x1, final double y0, final double y1, final double x,
                              final double y) {

        // interpolation 1d in the x direction
        final double fxy0 = this.interp1D(fx0y0, fx1y0, x0, x1, x);
        final double fxy1 = this.interp1D(fx0y1, fx1y1, x0, x1, x);

        // interpolation 1d in the y direction, with the already interpolated values in x
        return this.interp1D(fxy0, fxy1, y0, y1, y);
    }

    /**
     * Tests if an array of double contains NaN values.
     * 
     * @param val
     *        the array
     * @return true if at least one value is NaN
     */
    protected boolean isNan(final double[] val) {
        for (final double element : val) {
            if (Double.isNaN(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets xtab
     * 
     * @return the x values
     */
    public double[] getxtab() {
        return this.xtab.clone();
    }
}
