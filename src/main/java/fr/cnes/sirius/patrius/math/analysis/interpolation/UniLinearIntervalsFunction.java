/**
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
 * @history 01/10/2014:creation
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:269:01/10/2014:piecewise linear interpolations
 * VERSION::FA:386:19/12/2014:index mutualisation for ephemeris interpolation
 * VERSION::FA:417:12/02/2015:suppressed input tab clone
 * VERSION::FA:664:29/07/2016:corrected method value() leading to NaN value in some cases
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the representation of a linear function in dimension 1.
 * If double[] are given to the constructor, they should be sorted by increasing order.
 * No test to ensure that will be made, therefore, if these tab are not correctly sorted,
 * results can be totally wrong. No exception will be thrown.
 * 
 * @author Sophie LAURENS
 * @concurrency not thread-safe
 * @concurrency.comment input ISearchIndex not thread-safe
 * @version $Id: UniLinearIntervalsFunction.java 17603 2017-05-18 08:28:32Z bignon $
 * @since 2.3
 */
public class UniLinearIntervalsFunction extends AbstractLinearIntervalsFunction implements UnivariateFunction {

    /** Values array. */
    private final double[] ftab;

    /**
     * Constructor, in one dimension. The search index algorithm is set by default to an instance of
     * class BinarySearchIndexClosedOpen, see its javadoc for convention details.
     * 
     * Checks the lengths of xval and fval : they should be equal, and at least 2.
     * 
     * @precondition : xval and fval are increasingly sorted. No test to ensure that will
     *               be made, therefore, if these tab are not correctly sorted, results can be totally wrong. No
     *               exception will
     *               be thrown.
     * 
     * @param xval
     *        : abscissas array increasingly sorted of length >= 2 (duplicates are allowed).
     * @param fval
     *        : function values array increasingly sorted of length >= 2.
     * */
    public UniLinearIntervalsFunction(final double[] xval, final double[] fval) {
        super();

        // initialization
        this.xtab = xval;
        this.ftab = fval;

        // checks the lengths of xtab and ftab.length. Update attribute nxmax.
        this.checkLength1D();

        // creating the sorting algorithm
        this.searchXIndex = new BinarySearchIndexClosedOpen(this.xtab);
    }

    /**
     * Constructor, in one dimension with a search index algorithm as a parameter.
     * The attribute xtab is retrieved from the search index algorithm.
     * 
     * Checks the lengths of xval and fval : they should be equal, and at least 2.
     * 
     * @precondition : fval is increasingly sorted, and the search index algorithm had to be
     *               created with a sorted tab also. No test to ensure that will be made, therefore, if these
     *               tab are not correctly sorted, results can be totally wrong. No exception will be thrown.
     * 
     * @param algoX
     *        : an instance of a class implemented ISearchIndex, containing xtab.
     *        When it has been created, it should have been with a sorted tab.
     * @param fval
     *        : function values array increasingly sorted of length >= 2.
     * */
    public UniLinearIntervalsFunction(final ISearchIndex algoX, final double[] fval) {
        super();

        // initialization
        this.searchXIndex = algoX;
        this.xtab = this.searchXIndex.getTab();
        this.ftab = fval;

        // checks the lengths of xtab and ftab.length. Update attribute nxmax.
        this.checkLength1D();
    }

    /**
     * Computation of the interpolated/extrapolated value f(x).
     * 
     * If the index returned by method getIndex(key) is -1, with the convention OPEN_CLOSED, it can be either
     * tab[0] = key, and in that case it's an interpolation that has to be done, or key < tab[0], and in that case,
     * it is an extrapolation, where the slope used is the one computed in the first interval, and extended to key.
     * 
     * Important remark : in the last case, the result obtained is highly optimistic, because it can work even if key is
     * far away from tab[0], and therefore, the information the extrapolation is made upon is highly approximated.
     * 
     * If the index return is numberOfPoints, with the convention CLOSED_OPEN,
     * it can be either tab[numberOfPoints-1] = key or key > tab[numberOfPoints-1].
     * 
     * @param x
     *        : abscissa where to interpolate.
     * @return fx : the value of the function at x.
     */
    @Override
    public double value(final double x) {

        // gets the index of the (lower) closest abscissa from x
        int xindex = this.searchXIndex.getIndex(x);

        // Temporary variable
        double fx = Double.NaN;

        // if x does not belong to xtab, or with convention OPEN_CLOSED, if x = tab[0]
        if (xindex == -1) {
            xindex = 0;
            // Direct return if x is an element of xtab
            fx = (x == this.xtab[xindex]) ? this.ftab[xindex] : fx;
        }

        // if x does not belong to xtab, or with convention CLOSED_OPEN, if x = tab[nxmax-1]
        if (xindex == this.nxmax) {
            // Direct return if x is an element of xtab
            fx = (x == this.xtab[xindex]) ? this.ftab[xindex] : fx;
            xindex = this.nxmax - 1;
        }

        // Quick return if fvalue is not NaN
        if (!Double.isNaN(fx)) {
            return fx;
        }

        // initializes temporary variables
        final double f0 = this.ftab[xindex];
        final double f1 = this.ftab[xindex + 1];
        final double x0 = this.xtab[xindex];
        final double x1 = this.xtab[xindex + 1];

        // Avoid interpolation conducting to a division by zero
        // if x0 = x1 : direct return of f(x0)
        if (x0 == x1) {
            fx = this.ftab[xindex];
        }

        if (Double.isNaN(fx)) {
            fx = this.interp1D(f0, f1, x0, x1, x);
        }

        return fx;
    }

    // Getters

    /**
     * Getter for ftab in dimension 1
     * 
     * @return the function values
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getValues() {
        return this.ftab;
    }

    // Utility function for quality reasons
    /**
     * Checks the lengths of xtab and ftab.length. Update attribute nxmax.
     * 
     * Used for quality reasons (cyclomatic complexity , code duplication and code lisibility).
     */
    private void checkLength1D() {

        final int numberOfAbscissas = this.xtab.length;
        // abscissa vector and function values vector should have the same length
        if (numberOfAbscissas != this.ftab.length) {
            throw new DimensionMismatchException(numberOfAbscissas, this.ftab.length);
        }
        // this length should be at least 2
        if (numberOfAbscissas < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 2, numberOfAbscissas, true);
        }
        // maximal index for abscissas
        this.nxmax = numberOfAbscissas - 1;
    }
}
