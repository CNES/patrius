/**
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
 * 
 * @history creation 05/08/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * This is a test class for the validation of Matrix3D functions.
 * </p>
 * 
 * @see Matrix3D
 * 
 * @author Thomas Trapier, Julie Anton
 * 
 * @version $Id: Matrix3DTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class Matrix3DTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Matrix3D constructors
         * 
         * @featureDescription Construction of Matrix3D objects with several constructors.
         * 
         * @coveredRequirements DV-MATHS_220, DV-MATHS_250
         */
        MATRIX3D_CONSTRUCTOR,

        /**
         * @featureTitle operations on Matrix3D
         * 
         * @featureDescription All operations available in the Matrix3D class.
         * 
         * @coveredRequirements DV-MATHS_220
         */
        MATRIX3D_OPERATIONS

    }

    // /** 3 x 3 identity matrix */
    // protected double[][] id = { {1d,0d,0d}, {0d,1d,0d}, {0d,0d,1d} };

    /** Test data for group operations */
    protected double[][] testData = { { 1d, 2d, 3d }, { 2d, 5d, 3d }, { 1d, 0d, 8d } };
    /** Test data with NaN */
    protected double[][] testNaN = { { 1d, 2d, 3d }, { 2d, Double.NaN, 3d }, { 1d, 0d, 8d } };
    /** Bad test data for constructors */
    protected double[][] badRowDimData = { { 1d, 2d, 3d }, { 1d, 2d, 3d }, { 2d, 5d, 3d }, { 1d, 0d, 8d } };
    /** Bad test data for constructors */
    protected double[][] badColDimData = { { 1d, 2d, 3d, 6d }, { 2d, 5d, 3d, 9d }, { 1d, 0d, 8d, 1d } };
    /** Bad test data for constructors */
    protected double[][] badColAndRowDimData = { { 1d, 2d, 3d, 6d }, { 2d, 5d, 3d, 9d }, { 1d, 0d, 8d, 1d },
        { 1d, 0d, 8d, 1d } };
    /** Test data for group operations */
    protected double[][] testData2 = { { 5d, 2d, 9d }, { 2d, 4d, 3d }, { 0d, 4d, 6d } };
    /** Res data for multiplication */
    protected double[][] multData = { { 9d, 22d, 33d }, { 20d, 36d, 51d }, { 5d, 34d, 57d } };
    /** Res data for addition */
    protected double[][] addData = { { 6d, 4d, 12d }, { 4d, 9d, 6d }, { 1d, 4d, 14d } };
    /** Res data for subtraction */
    protected double[][] subData = { { -4d, 0d, -6d }, { 0d, 1d, 0d }, { 1d, -4d, 2d } };
    /** Res data for transposition */
    protected double[][] transData = { { 1d, 2d, 1d }, { 2d, 5d, 0d }, { 3d, 3d, 8d } };
    /** Res data for scalar multiplication */
    protected double[][] scalarMultData = { { 4d, 8d, 12d }, { 8d, 20d, 12d }, { 4d, 0d, 32d } };
    /** Test data for orthogonal matrix */
    protected double[][] orthogonalData = { { 8.0 / 9.0, 1.0 / 9.0, -4.0 / 9.0 },
        { -4.0 / 9.0, 4.0 / 9.0, -7.0 / 9.0 }, { 1.0 / 9.0, 8.0 / 9.0, 4.0 / 9.0 } };
    /** Test data for non orthogonal matrix */
    protected double[][] nonOrthogonalData = { { 8.0, 1.0, -4.0 }, { -4.0, 4.0, -7.0 }, { 1.0, 8.0, 4.0 } };
    /** Test data for almost orthogonal matrix */
    protected double[][] almostOrthogonalData = { { 8.0 / 9.0, 1.0 / 9.0, -4.0 / 9.0 },
        { -4.0000001 / 9.0, 4.0 / 9.0, -7.0 / 9.0 }, { 1.0 / 9.0, 8.0 / 9.0, 4.0 / 9.0 } };

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_CONSTRUCTOR}
     * 
     * @testedMethod {@link Matrix3D#Matrix3D(double[][])}
     * 
     * @description Creation of a Matrix3D with the different constructors (from an array, from a real matrix
     *              {@link RealMatrix} and from a vector {@link Vector3D } (cross product matrix).
     * 
     * @input Data to fill the matrix to be created
     * 
     * @output Matrix3D
     * 
     * @testPassCriteria The returned Matrix3D contains the expected values, the right exception is thrown or the cross
     *                   product matrix of the vector (1,-4,9.2) multiplied by the vector (-2,0.5,1.5) gives the same
     *                   result as the cross product function of Vector3D {@link Vector3D} with an epsilon of 1e-14 due
     *                   to potential computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void constructorTest() {
        // equality test between constructing a Matrix3D directly with its data
        // and with a RealMatrix
        final Array2DRowRealMatrix matCM = new Array2DRowRealMatrix(this.testData);
        final Matrix3D ref = new Matrix3D(this.testData);
        final Matrix3D res = new Matrix3D(matCM);
        // equality check
        Assert.assertTrue(res.equals(ref));

        // exceptions test
        try {
            final Array2DRowRealMatrix badMatCM = new Array2DRowRealMatrix(this.badRowDimData);
            new Matrix3D(badMatCM);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected !
        }
        try {
            final Array2DRowRealMatrix badMatCM = new Array2DRowRealMatrix(this.badColDimData);
            new Matrix3D(badMatCM);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected !
        }
        try {
            final Array2DRowRealMatrix badMatCM = new Array2DRowRealMatrix(this.badColAndRowDimData);
            new Matrix3D(badMatCM);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected !
        }
        try {
            new Matrix3D(this.badRowDimData);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected !
        }
        try {
            new Matrix3D(this.badColDimData);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected !
        }
        try {
            new Matrix3D(this.badColAndRowDimData);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected !
        }

        // creation of a cross product matrix from a vector 3D
        final Vector3D vector1 = new Vector3D(1, -4, 9.2);
        final Vector3D vector2 = new Vector3D(-2, 0.5, 1.5);
        final Matrix3D matrix = new Matrix3D(vector1);

        final Vector3D result = matrix.multiply(vector2);
        final Vector3D reference = Vector3D.crossProduct(vector1, vector2);

        Assert.assertEquals(result.getX(), reference.getX(), this.comparisonEpsilon);
        Assert.assertEquals(result.getY(), reference.getY(), this.comparisonEpsilon);
        Assert.assertEquals(result.getZ(), reference.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#equals}
     * 
     * @description Test of equality : are all entries equal ?
     * 
     * @input 2 Matrix3D to be compared
     * 
     * @output boolean
     * 
     * @testPassCriteria Return "true" if and only if the matrices are equal (with an implicit epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void equalsTest() {
        final Matrix3D matrix1 = new Matrix3D(this.testData);
        final Matrix3D matrix2 = new Matrix3D(this.testData);
        final Matrix3D matrix3 = new Matrix3D(this.testData2);
        final double d = 52.0;

        Assert.assertTrue(matrix1.equals(matrix1));
        Assert.assertTrue(matrix1.equals(matrix2));
        Assert.assertTrue(!matrix1.equals(matrix3));
        Assert.assertTrue(!matrix1.equals(d));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#multiply}
     * 
     * @description Multiplication of two Matrix3D containing doubles.
     * 
     * @input 2 Matrix3D to be multiplied
     * 
     * @output Matrix3D
     * 
     * @testPassCriteria The returned Matrix3D contains the expected values (with an implicit epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void matrix3DMultiplicationTest() {
        // Initialisations
        final Matrix3D matData = new Matrix3D(this.testData);
        final Matrix3D matData2 = new Matrix3D(this.testData2);
        final Matrix3D ref = new Matrix3D(this.multData);
        // test
        final Matrix3D res = matData.multiply(matData2);
        // check
        Assert.assertTrue(res.equals(ref));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#multiply}
     * 
     * @description Multiplication of a Matrix3D with a Vector3D.
     * 
     * @input A Matrix3D and a Vector3D to be multiplied
     * 
     * @output Vector3D
     * 
     * @testPassCriteria The returned Vector3D contains the expected values (with an epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void vector3DMultiplicationTest() {
        // Initialisations
        final Matrix3D matData = new Matrix3D(this.testData);
        final Vector3D vect = new Vector3D(1.0, 2.0, 3.0);
        final Vector3D ref = new Vector3D(14.0, 21.0, 25.0);
        // test
        final Vector3D res = matData.multiply(vect);
        // check
        Assert.assertEquals(res.getX(), ref.getX(), this.comparisonEpsilon);
        Assert.assertEquals(res.getY(), ref.getY(), this.comparisonEpsilon);
        Assert.assertEquals(res.getZ(), ref.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#add}
     * 
     * @description Addition of two Matrix3D containing doubles.
     * 
     * @input 2 Matrix3D to be added
     * 
     * @output Matrix3D
     * 
     * @testPassCriteria The returned Matrix3D contains the expected values (with an implicit epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void addTest() {
        // Initialisations
        final Matrix3D matData = new Matrix3D(this.testData);
        final Matrix3D matData2 = new Matrix3D(this.testData2);
        final Matrix3D ref = new Matrix3D(this.addData);
        // test
        final Matrix3D res = matData.add(matData2);
        // check
        Assert.assertTrue(res.equals(ref));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#subtract}
     * 
     * @description Subtraction of a Matrix3D containing doubles to another one.
     * 
     * @input 2 Matrix3D to be subtracted
     * 
     * @output Matrix3D
     * 
     * @testPassCriteria The returned Matrix3D contains the expected values (with an implicit epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void subtractTest() {
        // Initialisations
        final Matrix3D matData = new Matrix3D(this.testData);
        final Matrix3D matData2 = new Matrix3D(this.testData2);
        final Matrix3D ref = new Matrix3D(this.subData);
        // test
        final Matrix3D res = matData.subtract(matData2);
        // check
        Assert.assertTrue(res.equals(ref));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#transpose}
     * 
     * @description Transposition of a Matrix3D containing doubles.
     * 
     * @input 2 Matrix3D to be transposed
     * 
     * @output Matrix3D
     * 
     * @testPassCriteria The returned Matrix3D contains the expected values (with an implicit epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void transposeTest() {
        // Initialisations
        final Matrix3D matData = new Matrix3D(this.testData);
        final Matrix3D ref = new Matrix3D(this.transData);
        // test
        final Matrix3D res = matData.transpose();
        // check
        Assert.assertTrue(res.equals(ref));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#multiply}
     * 
     * @description Transposition of a Matrix3D containing doubles.
     * 
     * @input 2 Matrix3D to be transposed
     * 
     * @output Matrix3D
     * 
     * @testPassCriteria The returned Matrix3D contains the expected values (with an implicit epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void scalarMultiplyTest() {
        // Initialisations
        final Matrix3D matData = new Matrix3D(this.testData);
        final Matrix3D ref = new Matrix3D(this.scalarMultData);
        // test
        final Matrix3D res = matData.multiply(4.0);
        // check
        Assert.assertTrue(res.equals(ref));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#transposeAndMultiply}
     * 
     * @description Transposition of a Matrix3D containing doubles and then multiplication with a Vector3D.
     * 
     * @input a Matrix3D and a Vector3D
     * 
     * @output Vector3D
     * 
     * @testPassCriteria The returned Vector3D contains the expected values (with an epsilon of 1e-14 due to
     *                   potential computation errors).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void transposeAndMultiplyTest() {
        // Initialisation
        final Matrix3D transMatrix = new Matrix3D(this.transData);
        final Vector3D inVector = new Vector3D(1.0, 2.0, 3.0);
        final Vector3D ref = new Vector3D(14.0, 21.0, 25.0);
        // operation
        final Vector3D res = transMatrix.transposeAndMultiply(inVector);
        // check
        Assert.assertEquals(res.getX(), ref.getX(), this.comparisonEpsilon);
        Assert.assertEquals(res.getY(), ref.getY(), this.comparisonEpsilon);
        Assert.assertEquals(res.getZ(), ref.getZ(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#isOrthogonal}
     * 
     * @description The method isOrthogonal is tested with orthogonal, non orthogonal and almost orthogonal Matrix3D
     *              objects. The test is done with several thresholds.
     * 
     * @input some Matrix3D with different sizes and data
     * 
     * @output boolean
     * 
     * @testPassCriteria The boolean is true if the matrix is orthogonal.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIsOrthogonal() {

        // case : orthogonal matrix
        final Matrix3D matrix1 = new Matrix3D(this.orthogonalData);
        Assert.assertTrue(matrix1.isOrthogonal(Precision.EPSILON, Precision.EPSILON));

        // case : non orthogonal matrix (the column vectors are thus orthogonal with
        // each other but non normalized)
        final RealMatrix matrix2 = new Array2DRowRealMatrix(this.nonOrthogonalData);
        Assert.assertFalse(matrix2.isOrthogonal(Precision.EPSILON, Precision.EPSILON));

        // case : almost orthogonal matrix
        final RealMatrix matrix4 = new Array2DRowRealMatrix(this.almostOrthogonalData);
        // with a threshold of 1E-6
        Assert.assertTrue(matrix4.isOrthogonal(1E-6, 1E-6));
        // with an implicit threshold of 1E-16
        Assert.assertFalse(matrix4.isOrthogonal(Precision.EPSILON, Precision.EPSILON));
        // with a threshold of 1E-16 (for the normality) and 1E-6
        // (for the orthogonality)
        Assert.assertFalse(matrix4.isOrthogonal(1E-16, 1E-6));
        // with a threshold of 1E-16
        Assert.assertFalse(matrix4.isOrthogonal(1E-6, 1E-16));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#toString}
     * 
     * @description The method toString is tested on a Matrix3D.
     * 
     * @input Matrix3D object
     * 
     * @output String
     * 
     * @testPassCriteria The string is the expected one, describing the matrix the same way as in the AbstractRealMatrix
     *                   class.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testToString() {
        final Matrix3D matrix = new Matrix3D(this.testData);
        final String expected = "Matrix3D{{1.0,2.0,3.0},{2.0,5.0,3.0},{1.0,0.0,8.0}}";

        // test
        final String result = matrix.toString();
        Assert.assertEquals(result, expected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#isNaN}
     * 
     * @description The method isNaN is tested on 2 matrices.
     * 
     * @input Matrix3D object without NaN values, and one with one.
     * 
     * @output boolean
     * 
     * @testPassCriteria The result must be false for the Matrix3D without NaN, and true for the one with NaN.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testIsNaN() {
        final Matrix3D matrixOk = new Matrix3D(this.testData);
        final Matrix3D matrixNok = new Matrix3D(this.testNaN);
        Assert.assertTrue(!matrixOk.isNaN());
        Assert.assertTrue(matrixNok.isNaN());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MATRIX3D_OPERATIONS}
     * 
     * @testedMethod {@link Matrix3D#hashCode}
     * 
     * @description The method hashCode is tested on 2 Matrix3D objects.
     * 
     * @input Matrix3D object without NaN values, and one with one.
     * 
     * @output int
     * 
     * @testPassCriteria The returned value must be the one expected in both cases : the one computed for the non NaN
     *                   matrix, 642 for the matrix with NaN.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testHashCode() {
        final Matrix3D matrixOk = new Matrix3D(this.testData);
        final Matrix3D matrixNok = new Matrix3D(this.testNaN);
        Assert.assertEquals(matrixOk.hashCode(), 1093926912);
        Assert.assertEquals(matrixNok.hashCode(), 642);
    }

}
