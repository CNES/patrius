/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.fraction.FractionField;

public class FieldLUDecompositionTest {
    private final Fraction[][] testData = {
        { new Fraction(1), new Fraction(2), new Fraction(3) },
        { new Fraction(2), new Fraction(5), new Fraction(3) },
        { new Fraction(1), new Fraction(0), new Fraction(8) }
    };
    private final Fraction[][] testDataMinus = {
        { new Fraction(-1), new Fraction(-2), new Fraction(-3) },
        { new Fraction(-2), new Fraction(-5), new Fraction(-3) },
        { new Fraction(-1), new Fraction(0), new Fraction(-8) }
    };
    private final Fraction[][] luData = {
        { new Fraction(2), new Fraction(3), new Fraction(3) },
        { new Fraction(2), new Fraction(3), new Fraction(7) },
        { new Fraction(6), new Fraction(6), new Fraction(8) }
    };

    // singular matrices
    private final Fraction[][] singular = {
        { new Fraction(2), new Fraction(3) },
        { new Fraction(2), new Fraction(3) }
    };
    private final Fraction[][] bigSingular = {
        { new Fraction(1), new Fraction(2), new Fraction(3), new Fraction(4) },
        { new Fraction(2), new Fraction(5), new Fraction(3), new Fraction(4) },
        { new Fraction(7), new Fraction(3), new Fraction(256), new Fraction(1930) },
        { new Fraction(3), new Fraction(7), new Fraction(6), new Fraction(8) }
    }; // 4th row = 1st + 2nd

    /** test dimensions */
    @Test
    public void testDimensions() {
        final FieldMatrix<Fraction> matrix =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.testData);
        final FieldLUDecomposition<Fraction> LU = new FieldLUDecomposition<Fraction>(matrix);
        Assert.assertEquals(this.testData.length, LU.getL().getRowDimension());
        Assert.assertEquals(this.testData.length, LU.getL().getColumnDimension());
        Assert.assertEquals(this.testData.length, LU.getU().getRowDimension());
        Assert.assertEquals(this.testData.length, LU.getU().getColumnDimension());
        Assert.assertEquals(this.testData.length, LU.getP().getRowDimension());
        Assert.assertEquals(this.testData.length, LU.getP().getColumnDimension());

    }

    /** test non-square matrix */
    @Test
    public void testNonSquare() {
        try {
            // we don't use FractionField.getInstance() for testing purposes
            new FieldLUDecomposition<Fraction>(new Array2DRowFieldMatrix<Fraction>(new Fraction[][] {
                { Fraction.ZERO, Fraction.ZERO },
                { Fraction.ZERO, Fraction.ZERO },
                { Fraction.ZERO, Fraction.ZERO }
            }));
            Assert.fail("Expected NonSquareMatrixException");
        } catch (final NonSquareMatrixException ime) {
            // expected behavior
        }
    }

    /** test PA = LU */
    @Test
    public void testPAEqualLU() {
        FieldMatrix<Fraction> matrix = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.testData);
        FieldLUDecomposition<Fraction> lu = new FieldLUDecomposition<Fraction>(matrix);
        FieldMatrix<Fraction> l = lu.getL();
        FieldMatrix<Fraction> u = lu.getU();
        FieldMatrix<Fraction> p = lu.getP();
        TestUtils.assertEquals(p.multiply(matrix), l.multiply(u));

        matrix = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.testDataMinus);
        lu = new FieldLUDecomposition<Fraction>(matrix);
        l = lu.getL();
        u = lu.getU();
        p = lu.getP();
        TestUtils.assertEquals(p.multiply(matrix), l.multiply(u));

        matrix = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), 17, 17);
        for (int i = 0; i < matrix.getRowDimension(); ++i) {
            matrix.setEntry(i, i, Fraction.ONE);
        }
        lu = new FieldLUDecomposition<Fraction>(matrix);
        l = lu.getL();
        u = lu.getU();
        p = lu.getP();
        TestUtils.assertEquals(p.multiply(matrix), l.multiply(u));

        matrix = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.singular);
        lu = new FieldLUDecomposition<Fraction>(matrix);
        Assert.assertFalse(lu.getSolver().isNonSingular());
        Assert.assertNull(lu.getL());
        Assert.assertNull(lu.getU());
        Assert.assertNull(lu.getP());

        matrix = new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.bigSingular);
        lu = new FieldLUDecomposition<Fraction>(matrix);
        Assert.assertFalse(lu.getSolver().isNonSingular());
        Assert.assertNull(lu.getL());
        Assert.assertNull(lu.getU());
        Assert.assertNull(lu.getP());

    }

    /** test that L is lower triangular with unit diagonal */
    @Test
    public void testLLowerTriangular() {
        final FieldMatrix<Fraction> matrix =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.testData);
        final FieldMatrix<Fraction> l = new FieldLUDecomposition<Fraction>(matrix).getL();
        for (int i = 0; i < l.getRowDimension(); i++) {
            Assert.assertEquals(Fraction.ONE, l.getEntry(i, i));
            for (int j = i + 1; j < l.getColumnDimension(); j++) {
                Assert.assertEquals(Fraction.ZERO, l.getEntry(i, j));
            }
        }
    }

    /** test that U is upper triangular */
    @Test
    public void testUUpperTriangular() {
        final FieldMatrix<Fraction> matrix =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.testData);
        final FieldMatrix<Fraction> u = new FieldLUDecomposition<Fraction>(matrix).getU();
        for (int i = 0; i < u.getRowDimension(); i++) {
            for (int j = 0; j < i; j++) {
                Assert.assertEquals(Fraction.ZERO, u.getEntry(i, j));
            }
        }
    }

    /** test that P is a permutation matrix */
    @Test
    public void testPPermutation() {
        final FieldMatrix<Fraction> matrix =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), this.testData);
        final FieldMatrix<Fraction> p = new FieldLUDecomposition<Fraction>(matrix).getP();

        final FieldMatrix<Fraction> ppT = p.multiply(p.transpose());
        final FieldMatrix<Fraction> id =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(),
                p.getRowDimension(), p.getRowDimension());
        for (int i = 0; i < id.getRowDimension(); ++i) {
            id.setEntry(i, i, Fraction.ONE);
        }
        TestUtils.assertEquals(id, ppT);

        for (int i = 0; i < p.getRowDimension(); i++) {
            int zeroCount = 0;
            int oneCount = 0;
            int otherCount = 0;
            for (int j = 0; j < p.getColumnDimension(); j++) {
                final Fraction e = p.getEntry(i, j);
                if (e.equals(Fraction.ZERO)) {
                    ++zeroCount;
                } else if (e.equals(Fraction.ONE)) {
                    ++oneCount;
                } else {
                    ++otherCount;
                }
            }
            Assert.assertEquals(p.getColumnDimension() - 1, zeroCount);
            Assert.assertEquals(1, oneCount);
            Assert.assertEquals(0, otherCount);
        }

        for (int j = 0; j < p.getColumnDimension(); j++) {
            int zeroCount = 0;
            int oneCount = 0;
            int otherCount = 0;
            for (int i = 0; i < p.getRowDimension(); i++) {
                final Fraction e = p.getEntry(i, j);
                if (e.equals(Fraction.ZERO)) {
                    ++zeroCount;
                } else if (e.equals(Fraction.ONE)) {
                    ++oneCount;
                } else {
                    ++otherCount;
                }
            }
            Assert.assertEquals(p.getRowDimension() - 1, zeroCount);
            Assert.assertEquals(1, oneCount);
            Assert.assertEquals(0, otherCount);
        }

    }

    /** test singular */
    @Test
    public void testSingular() {
        FieldLUDecomposition<Fraction> lu =
            new FieldLUDecomposition<Fraction>(new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(),
                this.testData));
        Assert.assertTrue(lu.getSolver().isNonSingular());
        lu = new FieldLUDecomposition<Fraction>(new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(),
            this.singular));
        Assert.assertFalse(lu.getSolver().isNonSingular());
        lu = new FieldLUDecomposition<Fraction>(new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(),
            this.bigSingular));
        Assert.assertFalse(lu.getSolver().isNonSingular());
    }

    /** test matrices values */
    @Test
    public void testMatricesValues1() {
        final FieldLUDecomposition<Fraction> lu =
            new FieldLUDecomposition<Fraction>(new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(),
                this.testData));
        final FieldMatrix<Fraction> lRef =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), new Fraction[][] {
                { new Fraction(1), new Fraction(0), new Fraction(0) },
                { new Fraction(2), new Fraction(1), new Fraction(0) },
                { new Fraction(1), new Fraction(-2), new Fraction(1) }
            });
        final FieldMatrix<Fraction> uRef =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), new Fraction[][] {
                { new Fraction(1), new Fraction(2), new Fraction(3) },
                { new Fraction(0), new Fraction(1), new Fraction(-3) },
                { new Fraction(0), new Fraction(0), new Fraction(-1) }
            });
        final FieldMatrix<Fraction> pRef =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), new Fraction[][] {
                { new Fraction(1), new Fraction(0), new Fraction(0) },
                { new Fraction(0), new Fraction(1), new Fraction(0) },
                { new Fraction(0), new Fraction(0), new Fraction(1) }
            });
        final int[] pivotRef = { 0, 1, 2 };

        // check values against known references
        final FieldMatrix<Fraction> l = lu.getL();
        TestUtils.assertEquals(lRef, l);
        final FieldMatrix<Fraction> u = lu.getU();
        TestUtils.assertEquals(uRef, u);
        final FieldMatrix<Fraction> p = lu.getP();
        TestUtils.assertEquals(pRef, p);
        final int[] pivot = lu.getPivot();
        for (int i = 0; i < pivotRef.length; ++i) {
            Assert.assertEquals(pivotRef[i], pivot[i]);
        }

        // check the same cached instance is returned the second time
        Assert.assertTrue(l == lu.getL());
        Assert.assertTrue(u == lu.getU());
        Assert.assertTrue(p == lu.getP());

    }

    /** test matrices values */
    @Test
    public void testMatricesValues2() {
        final FieldLUDecomposition<Fraction> lu =
            new FieldLUDecomposition<Fraction>(new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(),
                this.luData));
        final FieldMatrix<Fraction> lRef =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), new Fraction[][] {
                { new Fraction(1), new Fraction(0), new Fraction(0) },
                { new Fraction(3), new Fraction(1), new Fraction(0) },
                { new Fraction(1), new Fraction(0), new Fraction(1) }
            });
        final FieldMatrix<Fraction> uRef =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), new Fraction[][] {
                { new Fraction(2), new Fraction(3), new Fraction(3) },
                { new Fraction(0), new Fraction(-3), new Fraction(-1) },
                { new Fraction(0), new Fraction(0), new Fraction(4) }
            });
        final FieldMatrix<Fraction> pRef =
            new Array2DRowFieldMatrix<Fraction>(FractionField.getInstance(), new Fraction[][] {
                { new Fraction(1), new Fraction(0), new Fraction(0) },
                { new Fraction(0), new Fraction(0), new Fraction(1) },
                { new Fraction(0), new Fraction(1), new Fraction(0) }
            });
        final int[] pivotRef = { 0, 2, 1 };

        // check values against known references
        final FieldMatrix<Fraction> l = lu.getL();
        TestUtils.assertEquals(lRef, l);
        final FieldMatrix<Fraction> u = lu.getU();
        TestUtils.assertEquals(uRef, u);
        final FieldMatrix<Fraction> p = lu.getP();
        TestUtils.assertEquals(pRef, p);
        final int[] pivot = lu.getPivot();
        for (int i = 0; i < pivotRef.length; ++i) {
            Assert.assertEquals(pivotRef[i], pivot[i]);
        }

        // check the same cached instance is returned the second time
        Assert.assertTrue(l == lu.getL());
        Assert.assertTrue(u == lu.getU());
        Assert.assertTrue(p == lu.getP());
    }
}
