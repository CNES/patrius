/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @description test class for UD decomposition
 * 
 * @author Denis Claude, Julie Anton
 * 
 * @version $Id: UDDecompositionImplTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class UDDecompositionImplTest {

    /** Data test. */
    final double[][] testData = new double[][] {
        { 1, 2, 4, 7, 11 },
        { 2, 13, 23, 38, 58 },
        { 4, 23, 77, 122, 182 },
        { 7, 38, 122, 294, 430 },
        { 11, 58, 182, 430, 855 }
    };

    /** Features description. */
    public enum features {
        /**
         * @featureTitle decomposition
         * 
         * @featureDescription UD decomposition
         * 
         * @coveredRequirements DV-MATHS_270
         */
        DECOMPOSITION

    }

    /** Test the constructors */
    @Test
    public void testConstructors() {

        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);

        final UDDecompositionImpl ud1 = new UDDecompositionImpl(matrix);
        final UDDecompositionImpl ud2 = new UDDecompositionImpl(matrix, 1.0e-15, 0.);
        
        Assert.assertTrue(ud1.getU().equals(ud2.getU()));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecomposition#getU()}
     * 
     * @description Test decomposition with a non-square matrix.
     * 
     * @input Non-square matrix
     * 
     * @output Exception
     * 
     * @testPassCriteria A NonSquareMatrixException exception should be raised.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = NonSquareMatrixException.class)
    public void testNonSquare() {
        new UDDecompositionImpl(MatrixUtils.createRealMatrix(new double[3][2]));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecompositionImpl#UDDecompositionImpl(RealMatrix)}
     * 
     * @description Test decomposition with a non-positive matrix.
     * 
     * @input Non positive matrix
     * 
     * @output Exception
     * 
     * @testPassCriteria A NonPositiveDefiniteMatrixException exception should be raised.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = NonPositiveDefiniteMatrixException.class)
    public void testNonPositiveDefiniteMatrix() {
        new UDDecompositionImpl(MatrixUtils.createRealMatrix(new double[][] {
            { 0.40434286, -0.09376327, 0.30328980, 0.04909388 },
            { -0.09376327, 0.10400408, 0.07137959, 0.04762857 },
            { 0.30328980, 0.07137959, 0.30458776, 0.04882449 },
            { 0.04909388, 0.04762857, 0.04882449, 0.07543265 }

        }));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecompositionImpl#UDDecompositionImpl(RealMatrix)}
     * 
     * @description Test decomposition with a non-positive matrix.
     * 
     * @input Non positive matrix
     * 
     * @output Exception
     * 
     * @testPassCriteria A NonPositiveDefiniteMatrixException exception should be raised.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = NonPositiveDefiniteMatrixException.class)
    public void testNotPositiveDefinite() {
        new UDDecompositionImpl(MatrixUtils.createRealMatrix(new double[][] {
            { 14, 11, 13, 15, 24 },
            { 11, 34, 13, 8, 25 },
            { 13, 13, 14, 15, 21 },
            { 15, 8, 15, 18, 23 },
            { 24, 25, 21, 23, 45 }
        }));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecompositionImpl#UDDecompositionImpl(RealMatrix)}
     * 
     * @description Test decomposition with a non-symmetric matrix.
     * 
     * @input Non symmetric matrix
     * 
     * @output Exception
     * 
     * @testPassCriteria A NonSymmetricMatrixException exception should be raised.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = NonSymmetricMatrixException.class)
    public void testNotSymmetricMatrixException() {
        final double[][] changed = this.testData.clone();
        changed[0][changed[0].length - 1] += 1.0e-5;
        new UDDecompositionImpl(MatrixUtils.createRealMatrix(changed));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecompositionImpl#UDDecompositionImpl(RealMatrix)}
     * 
     * @description Test decomposition with a non-symmetric matrix with threshold.
     * 
     * @input Non symmetric matrix
     * 
     * @output None
     * 
     * @testPassCriteria No exception is raised.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testNotSymmetricMatrixWithThresholdException() {
        final double[][] changed = this.testData.clone();
        changed[0][changed[0].length - 1] += 1.0e-5;
        new UDDecompositionImpl(MatrixUtils.createRealMatrix(changed), 1.0e-6, 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecompositionImpl#UDDecompositionImpl(RealMatrix)}
     * 
     * @description Test A = UDUT.
     * 
     * @input A matrix
     * 
     * @output UDUT matrix
     * 
     * @testPassCriteria The norm of A-UDUT equals zero.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testUDDecomposition() {

        final RealMatrix m = MatrixUtils.createRealMatrix(this.testData);
        final UDDecomposition ud = new UDDecompositionImpl(m);
        final RealMatrix u = ud.getU();
        final RealMatrix d = ud.getD();
        final RealMatrix ut = ud.getUT();
        final double norm = u.multiply(d.multiply(ut)).subtract(m).getNorm();
        Assert.assertEquals(0, norm, 1.0e-14);

        // exception dimension mismatch

        try {
            ((UDDecompositionImpl) ud).getSolver().solve(new ArrayRealVector(new double[] { 1, -2, 1 }, false));
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        final RealMatrix X = ((UDDecompositionImpl) ud).getSolver().getInverse();
        for (int i = 0; i < m.getColumnDimension(); i++) {
            for (int j = 0; j < m.getColumnDimension(); j++) {
                if (i == j) {
                    Assert.assertEquals(1, m.multiply(X).getEntry(i, j), 1e-14);
                } else {
                    Assert.assertEquals(0, m.multiply(X).getEntry(i, j), 1e-14);
                }

            }
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecomposition#getUT()}
     * 
     * @description Test that UT is transpose of U.
     * 
     * @input Matrix
     * 
     * @output norm
     * 
     * @testPassCriteria the norm of UT - transpose(U) equals zero
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testUTTransposed() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);
        final UDDecomposition udut = new UDDecompositionImpl(matrix);
        final RealMatrix u = udut.getU();
        final RealMatrix ut = udut.getUT();
        final double norm = u.subtract(ut.transpose()).getNorm();
        Assert.assertEquals(0, norm, 1.0e-15);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecompositionImpl#UDDecompositionImpl(RealMatrix)}
     * 
     * @description Test matrices values.
     * 
     * @input Matrix uRef
     * 
     * @output norm
     * 
     * @testPassCriteria The norms of u-uRef and ut-utRef are zero.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    // @Test
    public void testMatricesValues() {
        final RealMatrix uRef = MatrixUtils.createRealMatrix(new double[][] {
            { 1, 2, 3, 4, 5 },
            { 0, 6, 7, 8, 9 },
            { 0, 0, 10, 11, 12 },
            { 0, 0, 0, 13, 14 },
            { 0, 0, 0, 0, 15 }
        });

        final UDDecomposition udut =
            new UDDecompositionImpl(MatrixUtils.createRealMatrix(this.testData));

        // check values against known references
        final RealMatrix u = udut.getU();
        Assert.assertEquals(0, u.subtract(uRef).getNorm(), 1.0e-13);
        final RealMatrix ut = udut.getUT();
        Assert.assertEquals(0, ut.subtract(uRef.transpose()).getNorm(), 1.0e-13);

        // check the same cached instance is returned the second time
        Assert.assertTrue(u == udut.getU());
        Assert.assertTrue(ut == udut.getUT());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DECOMPOSITION}
     * 
     * @testedMethod {@link UDDecompositionImpl#UDDecompositionImpl(RealMatrix)}
     * 
     * @description Test matrices values.
     * 
     * @input Matrix uRef
     * 
     * @output norm
     * 
     * @testPassCriteria The norms of u-uRef and ut-utRef are zero.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void test() {
        MatrixUtils.createRealMatrix(new double[][] {
            { 1, 2, 3, 4, 5 },
            { 0, 6, 7, 8, 9 },
            { 0, 0, 10, 11, 12 },
            { 0, 0, 0, 13, 14 },
            { 0, 0, 0, 0, 15 }
        });

        final UDDecomposition udut =
            new UDDecompositionImpl(MatrixUtils.createRealMatrix(this.testData));
        final RealMatrix u = udut.getU();
        // System.out.println(u);
        u.setEntry(0, 0, 10000);
        final RealMatrix ub = udut.getU();
        // System.out.println(ub);

    }
    
    /** test getInvert and decompositionBuilder methods */
    @Test
    public void testInverse() {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(this.testData);
        RealMatrix inv = new UDDecompositionImpl(matrix).getSolver().getInverse();
        Assert.assertTrue(inv.equals(matrix.getInverse(UDDecompositionImpl.decompositionBuilder(1.0e-15, 0.0))));
    }
}
