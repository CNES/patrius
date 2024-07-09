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
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:269:01/10/2014:piecewise linear interpolations
 * VERSION::FA:386:19/12/2014:index mutualisation for ephemeris interpolation
 * VERSION::FA:417:12/02/2015:suppressed input tab clone
 * VERSION::FA:664:29/07/2016:corrected method value() leading to NaN value in some cases
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the representation of a linear function in dimension 2.
 * If double[] are given to the constructor, they should be sorted by increasing order.
 * No test to ensure that will be made, therefore, if these tab are not correctly sorted,
 * results can be totally wrong. No exception will be thrown.
 * 
 * @author Sophie LAURENS
 * @concurrency not thread-safe
 * @concurrency.comment input ISearchIndex not thread-safe
 * @version $Id: TriLinearIntervalsFunction.java 17603 2017-05-18 08:28:32Z bignon $
 * @since 2.3
 */
public class TriLinearIntervalsFunction extends AbstractLinearIntervalsFunction implements TrivariateFunction {

    /** Serial UID. */
    private static final long serialVersionUID = -3130378074950577275L;

    /** Values array. */
    private final double[][][] ftab;

    /**
     * Constructor, in three dimensions. The search index algorithm is set by default to an instance of
     * class BinarySearchIndexClosedOpen, see its javadoc for convention details.
     * 
     * Checks the lengths of xval and fval.length and yval and fval[i].length
     * and zval and fval[i][j].length : they should be equal, and at least 2.
     * 
     * @precondition : xval, yval, zval and fval are increasingly sorted. No test to ensure that will be made,
     *               therefore, if these tab are not correctly sorted, results can be totally wrong. No exception will
     *               be thrown.
     * 
     * @param xval
     *        : abscissas array increasingly sorted of length >= 2 (duplicates are allowed).
     * @param yval
     *        : ordinates array increasingly sorted of length >= 2 (duplicates are allowed).
     * @param zval
     *        : heights array increasingly sorted of length >= 2 (duplicates are allowed).
     * @param fval
     *        : function values array increasingly sorted of length >= 2.
     * */
    public TriLinearIntervalsFunction(final double[] xval, final double[] yval, final double[] zval,
        final double[][][] fval) {
        super();

        // initialization
        this.xtab = xval;
        this.ytab = yval;
        this.ztab = zval;
        this.ftab = fval;

        // verifies the compatibilities of lengths in the three dimensions. Updates maximal indices
        this.checkLength3D();

        // creating the sorting algorithm
        this.searchXIndex = new BinarySearchIndexClosedOpen(this.xtab);
        this.searchYIndex = new BinarySearchIndexClosedOpen(this.ytab);
        this.searchZIndex = new BinarySearchIndexClosedOpen(this.ztab);
    }

    /**
     * Constructor, in three dimensions with a search index algorithm as a parameter.
     * The attributes xtab, ytab and ztab are retrieved from the search index algorithm.
     * 
     * Checks the lengths of xval and fval.length and yval and fval[i].length
     * and zval and fval[i][j].length : they should be equal, and at least 2.
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
     * @param algoZ
     *        : an instance of a class implemented ISearchIndex, containing ztab.
     *        When it has been created, it should have been with a sorted tab.
     * @param fval
     *        : function values array increasingly sorted of length >= 2.
     */
    public TriLinearIntervalsFunction(final ISearchIndex algoX, final ISearchIndex algoY,
        final ISearchIndex algoZ, final double[][][] fval) {
        super();

        // initialization
        this.searchXIndex = algoX;
        this.searchYIndex = algoY;
        this.searchZIndex = algoZ;
        this.xtab = this.searchXIndex.getTab();
        this.ytab = this.searchYIndex.getTab();
        this.ztab = this.searchZIndex.getTab();
        this.ftab = fval;

        // verifies the compatibilities of lengths in the two dimensions. Updates maximal indices
        this.checkLength3D();
    }

    /**
     * Computation of the interpolated/extrapolated value f(x,y,z).
     * 
     * @param x
     *        : abscissa where to interpolate.
     * @param y
     *        : ordinate where to interpolate.
     * @param z
     *        : height where to interpolate.
     * @return fxyz : the value of the function at (x,y,z).
     */
    @Override
    public double value(final double x, final double y, final double z) {

        // gets the index of the (lower) closest values from x, y and z
        int xindex = this.searchXIndex.getIndex(x);
        int yindex = this.searchYIndex.getIndex(y);
        int zindex = this.searchZIndex.getIndex(z);

        // boundary conditions : 2^3 cases

        // if x (resp. y, z) does not belong to xtab (resp. ytab, ztab), or with convention OPEN_CLOSED,
        // if x = tab[0][:][:] or y = tab[:][0][:] or z = tab[:][:][0]
        if (xindex == -1) {
            xindex = 0;
        }
        if (yindex == -1) {
            yindex = 0;
        }

        if (zindex == -1) {
            zindex = 0;
        }

        // Temporary variable
        final double[] valXYZ = new double[3];

        valXYZ[0] = this.checkInterpValues(this.xtab, x, xindex);
        valXYZ[1] = this.checkInterpValues(this.ytab, y, yindex);
        valXYZ[2] = this.checkInterpValues(this.ztab, z, zindex);

        // if x (resp. y,z) does not belong to xtab (resp. ytab, ztab), or with convention CLOSED_OPEN,
        // if x = tab[nxmax-1][:][:] or y = tab[:][nymax-1][:] or z = tab[:][:][nzmax-1]
        if (xindex == this.nxmax) {
            valXYZ[0] = this.checkInterpValues(this.xtab, x, xindex);
            xindex = this.nxmax - 1;
        }
        if (yindex == this.nymax) {
            valXYZ[1] = this.checkInterpValues(this.ytab, y, yindex);
            yindex = this.nymax - 1;
        }
        if (zindex == this.nzmax) {
            valXYZ[2] = this.checkInterpValues(this.ztab, z, zindex);
            zindex = this.nzmax - 1;
        }

        // Make a quick return if x, y and z are at exactly index xindex, yindex, zindex in ftab
        if (!this.isNan(valXYZ)) {
            return this.ftab[(int) valXYZ[0]][(int) valXYZ[1]][(int) valXYZ[2]];
        }

        // interpolation 3D
        final double x0 = this.xtab[xindex];
        final double x1 = this.xtab[xindex + 1];
        final double y0 = this.ytab[yindex];
        final double y1 = this.ytab[yindex + 1];
        final double z0 = this.ztab[zindex];
        final double z1 = this.ztab[zindex + 1];

        final double f000 = this.ftab[xindex][yindex][zindex];
        final double f001 = this.ftab[xindex][yindex][zindex + 1];
        final double f010 = this.ftab[xindex][yindex + 1][zindex];
        final double f011 = this.ftab[xindex][yindex + 1][zindex + 1];
        final double f100 = this.ftab[xindex + 1][yindex][zindex];
        final double f101 = this.ftab[xindex + 1][yindex][zindex + 1];
        final double f110 = this.ftab[xindex + 1][yindex + 1][zindex];
        final double f111 = this.ftab[xindex + 1][yindex + 1][zindex + 1];

        // Avoid interpolation if x0 = x1, y0 = y1 or z0 = z1 leading to a division by zero
        return this.controlOnInterpolation(x, y, z, x0, x1, y0, y1, z0, z1, f000, f001, f010, f011, f100, f101, f110,
            f111);
    }

    /**
     * Check if the value at returned index (by the search algorithm) in the table corresponds
     * to the point where interpolation is wanted.
     * 
     * @param tab
     *        the table on which interpolation is performed
     * @param x
     *        the point where to interpolate
     * @param xindex
     *        the index found by the search algorithm
     * @return xindex if values are equals, NaN otherwise
     */
    private double checkInterpValues(final double[] tab, final double x, final int xindex) {
        return (x == tab[xindex]) ? xindex : Double.NaN;
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
     * @param z
     *        height where to interpolate
     * @param x0
     *        abscissa returned by the search index algorithm
     * @param x1
     *        abscissa following x0 in xtab table
     * @param y0
     *        ordinate returned by the search index algorithm
     * @param y1
     *        ordinate following y0 in ytab table
     * @param z0
     *        height returned by the search index algorithm
     * @param z1
     *        height following z0 in ztab table
     * @param f000
     *        the value of the function at (x0, y0, z0)
     * @param f001
     *        the value of the function at (x0, y0, z1)
     * @param f010
     *        the value of the function at (x0, y1, z0)
     * @param f011
     *        the value of the function at (x0, y1, z1)
     * @param f100
     *        the value of the function at (x1, y0, z0)
     * @param f101
     *        the value of the function at (x1, y0, z1)
     * @param f110
     *        the value of the function at (x1, y1, z0)
     * @param f111
     *        the value of the function at (x1, y1, z1)
     * @return fxyz : the value of the function at (x, y, z)
     */
    private double controlOnInterpolation(final double x, final double y, final double z, final double x0,
                                          final double x1, final double y0, final double y1, final double z0,
                                          final double z1, final double f000,
                                          final double f001, final double f010, final double f011, final double f100,
                                          final double f101,
                                          final double f110, final double f111) {

        // Temporary variable
        double fxyz = Double.NaN;

        if (x0 == x1) {
            if (y0 == y1) {
                // z0 != z1
                fxyz = this.interp1D(f000, f001, z0, z1, z);
            } else {
                // y0 != y1
                // Split into 2 cases : z0 = z1 or not
                if (z0 == z1) {
                    fxyz = this.interp1D(f000, f010, y0, y1, y);
                } else {
                    fxyz = this.interp2D(f000, f010, f001, f011, y0, y1, z0, z1, y, z);
                }
            }
        }

        if (!Double.isNaN(fxyz)) {
            return fxyz;
        }

        // Check the same conditions on y
        // x0 != x1 obviously since the last condition on x is passed above
        if (y0 == y1) {
            if (z0 == z1) {
                fxyz = this.interp1D(f000, f100, x0, x1, x);
            } else {
                final double fval1 = this.interp1D(f000, f100, x0, x1, x);
                final double fval2 = this.interp1D(f001, f101, x0, x1, x);
                fxyz = this.interp1D(fval1, fval2, z0, z1, z);
            }
        }

        // Check the same conditions on z
        // x0 != x1 and y0 =! y1 obviously since the last conditions on x, y are passed above
        if (z0 == z1 && Double.isNaN(fxyz)) {
            final double fval1 = this.interp1D(f000, f100, x0, x1, x);
            final double fval2 = this.interp1D(f010, f110, x0, x1, x);
            fxyz = this.interp1D(fval1, fval2, y0, y1, y);
        }

        // Return fvalue if defined, interpolate otherwise
        if (Double.isNaN(fxyz)) {
            // interpolation 1D in the x direction
            final double fxy0z0 = this.interp1D(f000, f100, x0, x1, x);
            final double fxy1z0 = this.interp1D(f010, f110, x0, x1, x);
            final double fxy0z1 = this.interp1D(f001, f101, x0, x1, x);
            final double fxy1z1 = this.interp1D(f011, f111, x0, x1, x);

            // interpolation 2D in the y,z directions, with the already interpolated values in x
            fxyz = this.interp2D(fxy0z0, fxy1z0, fxy0z1, fxy1z1, y0, y1, z0, z1, y, z);
        }

        return fxyz;
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
     * Gets ztab
     * 
     * @return the z values
     */
    public double[] getztab() {
        return this.ztab;
    }

    /**
     * Gets ftab in dimension 3
     * 
     * @return the function values
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][][] getValues() {
        return this.ftab;
    }

    // Utility functions for quality reasons
    /**
     * Checks the lengths of xtab and ftab.length and ytab and ftab[i].length
     * and ztab and ftab[i][j].length.
     * Update attribute nxmax, nymax, nzmax.
     * 
     * Used for quality reasons (cyclomatic complexity , code duplication and code lisibility).
     */
    private void checkLength3D() {

        // gets the dimension of three components vectors
        final int xLength = this.xtab.length;

        // first component vector and function values should have the same length
        if (xLength != this.ftab.length) {
            throw new DimensionMismatchException(xLength, this.ftab.length);
        }
        // second component vector and function values should have the same length
        final int yLength = this.ytab.length;
        for (int i = 0; i < xLength; i++) {
            if (yLength != this.ftab[i].length) {
                throw new DimensionMismatchException(yLength, this.ftab[i].length);
            }
        }
        // third component vector and function values should have the same length
        final int zLength = this.ztab.length;
        for (int i = 0; i < xLength; i++) {
            for (int j = 0; j < yLength; j++) {
                if (zLength != this.ftab[i][j].length) {
                    throw new DimensionMismatchException(zLength, this.ftab[i][j].length);
                }
            }
        }
        // each length should be at least 2
        if (xLength < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 2, xLength, true);
        }
        if (yLength < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 2, yLength, true);
        }
        if (zLength < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.WRONG_NUMBER_OF_POINTS, 2, zLength, true);
        }

        // maximal indices
        this.nxmax = this.xtab.length - 1;
        this.nymax = this.ytab.length - 1;
        this.nzmax = this.ztab.length - 1;
    }
}
