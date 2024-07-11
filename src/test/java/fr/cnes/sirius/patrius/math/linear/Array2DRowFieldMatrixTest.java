/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:13/11/2014: (creation) coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.fraction.FractionField;

/**
 * Test class for abstract class AbstractFieldMatrix and its daughter Array2DRowFieldMatrix
 * 
 * @version $Id: Array2DRowFieldMatrixTest.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.4
 * 
 */
public class Array2DRowFieldMatrixTest {

    /**
     * For coverage purpose.
     * Checks exception on strictly negative number of columns in protected constructor
     * of AbstractFieldMatrix(Field<T>, double, double)
     */
    @Test(expected = NotStrictlyPositiveException.class)
    public void testNotStrictlyPositiveExceptionConstructor() {
        new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), 3, -1);
    }

    /**
     * For coverage purpose.
     * Checks exception of protected static method extractField (T[][])
     */
    @Test(expected = NullArgumentException.class)
    public void testNullArgumentExceptionExtractField() {
        final Fraction[][] d = null;
        AbstractFieldMatrix.extractField(d);
    }

    /**
     * For coverage purpose.
     * Checks exception of protected static method extractField (T[][])
     */
    @Test(expected = NoDataException.class)
    public void testNoDataExceptionExtractField() {
        final Fraction[][] d = new Fraction[][] {};
        AbstractFieldMatrix.extractField(d);
    }

    // /**
    // * For coverage purpose.
    // * Checks exception of protected static method extractField (T[])
    // * even if this method is only used in one constructor
    // * of Array2DRowFieldMatrix that is not used in PATRIUS
    // */
    // @Test (expected = NoDataException.class)
    // public void testNoDataExceptionExtractField2() {
    // Fraction[] d = new Fraction[]{};
    // Array2DRowFieldMatrix.extractField(d);
    // }

    /**
     * For coverage purpose.
     * Checks exception on zero rows for submatrix in method setSubMatrix
     */
    @Test(expected = NoDataException.class)
    public void testNoDataExceptionSetSubMatrix() {
        final Array2DRowFieldMatrix<Fraction> m =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), 3, 1);
        final Fraction[][] subMatrix = new Fraction[][] {};
        m.setSubMatrix(subMatrix, 1, 1);
    }

    /**
     * For coverage purpose.
     * Checks exception if (endColumn < startColumn) in 1st method checkSubMatrixIndex
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void testNumberIsTooSmallExceptionCheckSubMatrixIndex() {
        final Array2DRowFieldMatrix<Fraction> m =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), 3, 3);
        m.checkSubMatrixIndex(1, 2, 2, 1);
    }

    /**
     * For coverage purpose.
     * Checks exception if (selectedRows == null || selectedColumns == null) in
     * 2nd method checkSubMatrixIndex
     */
    @Test(expected = NullArgumentException.class)
    public void testNullArgumentExceptionCheckSubMatrixIndex() {
        final int[] selectedRow = { 1 };
        final int[] selectedColumns = null;
        final Array2DRowFieldMatrix<Fraction> m =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), 3, 3);
        m.checkSubMatrixIndex(selectedRow, selectedColumns);
    }

    /**
     * For coverage purpose for the 6th constructor
     * public Array2DRowFieldMatrix(final Field<T> field, final T[][] d, final boolean copyArray)
     */
    @Test
    public void testConstructor6() {
        // tests if (copyArray)
        final Fraction[][] d = new Fraction[1][1];
        d[0][0] = new Fraction(14);
        final Array2DRowFieldMatrix<Fraction> m =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), d, true);
        final Fraction[][] result = m.getDataRef();
        Assert.assertArrayEquals(d, result);
    }

    /**
     * For coverage purpose for the 6th constructor
     * public Array2DRowFieldMatrix(final Field<T> field, final T[][] d, final boolean copyArray)
     */
    @Test(expected = NoDataException.class)
    public void testConstructor6Rows() {

        // tests if (nRows == 0)
        final Fraction[][] d = new Fraction[][] {};
        new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), d, false);
    }

    /**
     * For coverage purpose for the 6th constructor
     * public Array2DRowFieldMatrix(final Field<T> field, final T[][] d, final boolean copyArray)
     */
    @Test(expected = NoDataException.class)
    public void testConstructor6Cols() {

        // tests if (nCols == 0)
        final Fraction[][] d = new Fraction[1][1];
        d[0] = new Fraction[] {};
        new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), d, false);
    }

    /**
     * For coverage purpose for the 6th constructor
     * public Array2DRowFieldMatrix(final Field<T> field, final T[][] d, final boolean copyArray)
     */
    @Test(expected = DimensionMismatchException.class)
    public void testConstructor6drCols() {

        // tests if (d[r].length != nCols) {
        final Fraction[][] d = new Fraction[2][1];
        d[0] = new Fraction[14];
        d[1] = new Fraction[7];
        new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), d, false);
    }

    /**
     * For coverage purpose for method setSubMatrix
     */
    @Test(expected = NoDataException.class)
    public void testSetSubMatrixRows() {

        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance());
        // tests if (nRows == 0) {
        final Fraction[][] d = new Fraction[][] {};
        m2.setSubMatrix(d, 0, 0);

    }

    /**
     * For coverage purpose for method setSubMatrix
     */
    @Test(expected = NoDataException.class)
    public void testSetSubMatrixCols() {

        final Array2DRowFieldMatrix<Fraction> m2 = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance());
        // tests if (nCols == 0) {
        final Fraction[][] d = new Fraction[1][1];
        d[0] = new Fraction[] {};
        m2.setSubMatrix(d, 0, 0);
    }

}
