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

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the representation of a linear function in dimension 2.
 * If double[] are given to the constructor, they should be sorted by increasing order (duplicates are allowed).
 * No test to ensure that will be made, therefore, if these tab are not correctly sorted,
 * results can be totally wrong. No exception will be thrown.
 * 
 * @author Sophie LAURENS
 * @concurrency not thread-safe
 * @concurrency.comment input ISearchIndex not thread-safe
 * @version $Id: BiLinearIntervalsFunction.java 17603 2017-05-18 08:28:32Z bignon $
 * @since 2.3
 */
public class BiLinearIntervalsFunction extends AbstractLinearIntervalsFunction implements BivariateFunction {

    /** Serial UID. */
    private static final long serialVersionUID = -6763122651717619414L;

    /** Values array. */
    private final double[][] ftab;

    /**
     * Constructor, in two dimensions. The search index algorithm is set by default to an instance of
     * class BinarySearchIndexClosedOpen, see its javadoc for convention details.
     * 
     * Checks the lengths of xval and fval.length and yval and fval[i].length :
     * they should be equal, and at least 2.
     * 
     * @precondition : xval, yval, and fval are increasingly sorted. No test to ensure that will be made,
     *               therefore, if these tab are not correctly sorted, results can be totally wrong. No exception will
     *               be thrown.
     * 
     * @param xval
     *        : abscissas array increasingly sorted of length >= 2 (duplicates are allowed).
     * @param yval
     *        : ordinates array increasingly sorted of length >= 2 (duplicates are allowed).
     * @param fval
     *        : function values array increasingly sorted of length >= 2.
     * */
    public BiLinearIntervalsFunction(final double[] xval, final double[] yval, final double[][] fval) {
        super();

        // initialization
        this.xtab = xval;
        this.ytab = yval;
        this.ftab = fval;

        // verifies the compatibilities of lengths in the two dimensions. Updates maximal indices
        this.checkLength2D();

        // creating the sorting algorithm
        this.searchXIndex = new BinarySearchIndexClosedOpen(this.xtab);
        this.searchYIndex = new BinarySearchIndexClosedOpen(this.ytab);
    }

    /**
     * Constructor, in two dimensions with a search index algorithm as a parameter.
     * The attributes xtab and ytab are retrieved from the search index algorithm.
     * 
     * Checks the lengths of xval and fval.length and yval and fval[i].length :
     * they should be equal, and at least 2.
     * 
     * @precondition : fval is increasingly sorted, and the search index algorithm had to be
     *               created with a sorted tab also. No test to ensure that will be made, therefore, if these
     *               tab are not correctly sorted, results can be totally wrong. No exception will be thrown.
     * 
     * @param algoX
     *        : an instance of a class implemented ISearchIndex, containing xtab.
     *        When it has been created, it should have been with a sorted tab.
     * @param algoY
     *        : an instance of a class implemented ISearchIndex, containing ytab.
     *        When it has been created, it should have been with a sorted tab.
     * @param fval
     *        : function values array increasingly sorted of length >= 2.
     */
    public BiLinearIntervalsFunction(final ISearchIndex algoX, final ISearchIndex algoY, final double[][] fval) {
        super();

        // initialization
        this.searchXIndex = algoX;
        this.searchYIndex = algoY;
        this.xtab = this.searchXIndex.getTab();
        this.ytab = this.searchYIndex.getTab();
        this.ftab = fval;

        // verifies the compatibilities of lengths in the two dimensions. Updates maximal indices
        this.checkLength2D();
    }

    /**
     * Computation of the interpolated/extrapolated value f(x,y).
     * 
     * @param x
     *        : abscissa where to interpolate.
     * @param y
     *        : ordinate where to interpolate.
     * @return fxy : the value of the function at (x,y).
     */
    @Override
    public double value(final double x, final double y) {

        // gets the index of the (lower) closest abscissa (resp. ordinate) from x (resp. y)
        int xindex = this.searchXIndex.getIndex(x);
        int yindex = this.searchYIndex.getIndex(y);

        // if x (resp. y) does not belong to xtab (resp. ytab), or with convention OPEN_CLOSED, if x = tab[0][:]
        // or y = tab[:][0]
        if (xindex == -1) {
            xindex = 0;
        }
        if (yindex == -1) {
            yindex = 0;
        }

        // Temporary variable
        final double[] valXY = { Double.NaN, Double.NaN };

        // Are x or y at xindex, yindex ?
        valXY[0] = (x == this.xtab[xindex]) ? xindex : valXY[0];
        valXY[1] = (y == this.ytab[yindex]) ? yindex : valXY[1];

        // if x (resp. y) does not belong to xtab (resp. ytab), or with convention CLOSED_OPEN, if x = tab[nxmax-1][:]
        // or y = tab[:][nymax-1]
        if (xindex == this.nxmax) {
            valXY[0] = (x == this.xtab[xindex]) ? xindex : valXY[0];
            xindex = this.nxmax - 1;
        }

        if (yindex == this.nymax) {
            valXY[1] = (y == this.ytab[yindex]) ? yindex : valXY[1];
            yindex = this.nymax - 1;
        }

        // Make a quick return if x and y are at exactly index xindex, yindex in ftab
        if (!this.isNan(valXY)) {
            return this.ftab[(int) valXY[0]][(int) valXY[1]];
        }

        // initialization of temporary variables
        final double x0 = this.xtab[xindex];
        final double x1 = this.xtab[xindex + 1];

        final double y0 = this.ytab[yindex];
        final double y1 = this.ytab[yindex + 1];

        final double f00 = this.ftab[xindex][yindex];
        final double f01 = this.ftab[xindex][yindex + 1];
        final double f10 = this.ftab[xindex + 1][yindex];
        final double f11 = this.ftab[xindex + 1][yindex + 1];

        // Avoid interpolation if x0 = x1 or y0 = y1 leading to a division by zero
        return this.controlOnInterpolation(x, y, x0, x1, y0, y1, f00, f01, f10, f11);
    }

    /**
     * Tests if the interpolation on x and y is possible, and makes a quick return to avoid
     * it in particular cases leading to a NaN value (division by zero if interpolation occurs
     * in a zero amplitude interval).
     * 
     * @param x
     *        abscissa where to interpolate
     * @param y
     *        ordinate where to interpolate
     * @param x0
     *        abscissa returned by the search index algorithm
     * @param x1
     *        abscissa following x0 in xtab table
     * @param y0
     *        ordinate returned by the search index algorithm
     * @param y1
     *        ordinate following y0 in ytab table
     * @param f00
     *        the value of the function at (x0, y0)
     * @param f01
     *        the value of the function at (x0, y1)
     * @param f10
     *        the value of the function at (x1, y0)
     * @param f11
     *        the value of the function at (x1, y1)
     * @return fxy : the value of the function at (x, y)
     */
    private double controlOnInterpolation(final double x, final double y, final double x0, final double x1,
                                          final double y0, final double y1, final double f00, final double f01,
                                          final double f10, final double f11) {

        double fxy = Double.NaN;
        fxy = (x0 == x1) ? this.interp1D(f00, f01, y0, y1, y) : fxy;

        if (!Double.isNaN(fxy)) {
            return fxy;
        }

        fxy = (y0 == y1) ? this.interp1D(f00, f10, x0, x1, x) : fxy;

        // Return fvalue if defined, interpolate otherwise
        if (Double.isNaN(fxy)) {
            // interpolation 1d in the x direction
            final double fxyip1 = this.interp1D(f01, f11, x0, x1, x);
            final double fxyi = this.interp1D(f00, f10, x0, x1, x);

            // interpolation 1d in the y direction, with the already interpolated values in x
            fxy = this.interp1D(fxyi, fxyip1, y0, y1, y);
        }
        return fxy;
    }

    // Getters

    /**
     * Gets ytab
     * 
     * @return the y values
     */
    public double[] getytab() {
        return this.ytab;
    }

    /**
     * Gets ftab in dimension 2
     * 
     * @return the function values
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getValues() {
        return this.ftab;
    }

    // Utility functions for quality reasons
    /**
     * Checks the lengths of xtab and ftab.length and ytab and ftab[i].length.
     * Update attribute nxmax, nymax, nzmax.
     * 
     * Used for quality reasons (cyclomatic complexity , code duplication and code lisibility).
     */
    private void checkLength2D() {

        // abscissas vector and function values vectors for the first component should have the same length
        final int xLength = this.xtab.length;
        if (xLength != this.ftab.length) {
            throw new DimensionMismatchException(xLength, this.ftab.length);
        }
        // this length should be at least 2
        if (xLength < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 2, xLength, true);
        }

        // ordinates vector and function values vectors for all the lines should have the same length
        final int yLength = this.ytab.length;
        for (int i = 0; i < xLength; i++) {
            if (yLength != this.ftab[i].length) {
                throw new DimensionMismatchException(yLength, this.ftab[i].length);
            }
        }
        if (yLength < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 2, yLength, true);
        }
        // maximal indices
        this.nxmax = this.xtab.length - 1;
        this.nymax = this.ytab.length - 1;
    }
}
