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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a real 3x3 matrix designed to be used in geometric calculations. It is compatible with the Vector3D type.
 * </p>
 * 
 * @useSample <p>
 *            Creation with a double[][] data : Matrix3D matrix = new Matrix3D(data); Multiplication with a Vector3D :
 *            Vector3D result = matrix.multiply(vector3D);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Vector3D
 * 
 * @author Thomas Trapier, Julie Anton
 * 
 * @version $Id: Matrix3D.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class Matrix3D implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -451912259186366524L;

    /** Hash code ni the Nan case */
    private static final int NAN_HASH_CODE = 642;

    /** Entries of the matrix. */
    private final double[][] data;

    /**
     * Constructor<br>
     * Needs the data to fill the matrix.
     * 
     * @param dataIn
     *        data to fill the matrix
     * @since 1.0
     */
    public Matrix3D(final double[][] dataIn) {
        if (dataIn.length == 3 && dataIn[0].length == 3) {
            this.data = new double[3][3];
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    this.data[i][j] = dataIn[i][j];
                }
            }
        } else {
            // If the input data is of bad dimensions, an exception is thrown
            throw new MathIllegalArgumentException(PatriusMessages.BAD_SIZE_MATRIX_CREATION);
        }
    }

    /**
     * Constructor<br>
     * Creates a Matrix3D with a RealMatrix.
     * 
     * @param matrix
     *        RealMatrix to transform
     * @since 1.0
     */
    public Matrix3D(final RealMatrix matrix) {
        if (matrix.getColumnDimension() == 3 && matrix.getRowDimension() == 3) {
            this.data = matrix.getData();
        } else {
            // If the input RealMatrix is of bad dimensions, an exception is thrown
            throw new MathIllegalArgumentException(PatriusMessages.BAD_SIZE_MATRIX_CREATION);
        }
    }

    /**
     * Constructor<br>
     * Builds a cross product matrix <b>M</b> from a {@link Vector3D vector} <b>u</b> such as : M(u) * v = u^v
     * 
     * @param vector
     *        the vector u such as M(u) * v = u^v
     * 
     * @since 1.0
     */
    public Matrix3D(final Vector3D vector) {
        this.data = new double[3][3];

        this.data[0][0] = 0;
        this.data[1][1] = 0;
        this.data[2][2] = 0;

        this.data[0][1] = -vector.getZ();
        this.data[0][2] = vector.getY();

        this.data[1][0] = vector.getZ();
        this.data[1][2] = -vector.getX();

        this.data[2][0] = -vector.getY();
        this.data[2][1] = vector.getX();
    }

    /**
     * Computes a matrix multiplication between two Matrix3D objects
     * 
     * @param mult
     *        the Matrix3D right term of the multiplication
     * @return the resulting Matrix3D
     */
    public Matrix3D multiply(final Matrix3D mult) {
        // double[][] initialization:
        final double[][] returnedData = new double[3][3];
        final double[][] multData = mult.getData();

        // computation of each value of the returned matrix
        returnedData[0][0] =
            multData[0][0] * this.data[0][0] + multData[1][0] * this.data[0][1] + multData[2][0] * this.data[0][2];
        returnedData[1][0] =
            multData[0][0] * this.data[1][0] + multData[1][0] * this.data[1][1] + multData[2][0] * this.data[1][2];
        returnedData[2][0] =
            multData[0][0] * this.data[2][0] + multData[1][0] * this.data[2][1] + multData[2][0] * this.data[2][2];
        returnedData[0][1] =
            multData[0][1] * this.data[0][0] + multData[1][1] * this.data[0][1] + multData[2][1] * this.data[0][2];
        returnedData[1][1] =
            multData[0][1] * this.data[1][0] + multData[1][1] * this.data[1][1] + multData[2][1] * this.data[1][2];
        returnedData[2][1] =
            multData[0][1] * this.data[2][0] + multData[1][1] * this.data[2][1] + multData[2][1] * this.data[2][2];
        returnedData[0][2] =
            multData[0][2] * this.data[0][0] + multData[1][2] * this.data[0][1] + multData[2][2] * this.data[0][2];
        returnedData[1][2] =
            multData[0][2] * this.data[1][0] + multData[1][2] * this.data[1][1] + multData[2][2] * this.data[1][2];
        returnedData[2][2] =
            multData[0][2] * this.data[2][0] + multData[1][2] * this.data[2][1] + multData[2][2] * this.data[2][2];
        // get a matrix from the double[][]:
        return new Matrix3D(returnedData);
    }

    /**
     * Computes the addition of two Matrix3D
     * 
     * @param added
     *        the Matrix3D to be added
     * @return the resulting Matrix3D
     */
    public Matrix3D add(final Matrix3D added) {
        // double[][] initialization:
        final double[][] returnedData = new double[3][3];
        final double[][] addedData = added.getData();

        // computation of each value of the returned matrix
        returnedData[0][0] = addedData[0][0] + this.data[0][0];
        returnedData[1][0] = addedData[1][0] + this.data[1][0];
        returnedData[2][0] = addedData[2][0] + this.data[2][0];
        returnedData[0][1] = addedData[0][1] + this.data[0][1];
        returnedData[1][1] = addedData[1][1] + this.data[1][1];
        returnedData[2][1] = addedData[2][1] + this.data[2][1];
        returnedData[0][2] = addedData[0][2] + this.data[0][2];
        returnedData[1][2] = addedData[1][2] + this.data[1][2];
        returnedData[2][2] = addedData[2][2] + this.data[2][2];
        // get a matrix from the double[][]:
        return new Matrix3D(returnedData);
    }

    /**
     * Computes the transposition of this Matrix3D
     * 
     * @return the resulting Matrix3D
     */
    public Matrix3D transpose() {
        // double[][] initialization:
        final double[][] returnedData = new double[3][3];

        // attribution of each value of the returned matrix
        returnedData[0][0] = this.data[0][0];
        returnedData[1][0] = this.data[0][1];
        returnedData[2][0] = this.data[0][2];
        returnedData[0][1] = this.data[1][0];
        returnedData[1][1] = this.data[1][1];
        returnedData[2][1] = this.data[1][2];
        returnedData[0][2] = this.data[2][0];
        returnedData[1][2] = this.data[2][1];
        returnedData[2][2] = this.data[2][2];
        // get a matrix from the double[][]:
        return new Matrix3D(returnedData);
    }

    /**
     * Computes the subtraction of a Matrix3D to this one
     * 
     * @param sub
     *        the Matrix3D to be subtracted
     * @return the resulting Matrix3D
     */
    public Matrix3D subtract(final Matrix3D sub) {
        // double[][] initialization:
        final double[][] returnedData = new double[3][3];
        final double[][] subData = sub.getData();

        // computation of each value of the returned matrix
        returnedData[0][0] = this.data[0][0] - subData[0][0];
        returnedData[1][0] = this.data[1][0] - subData[1][0];
        returnedData[2][0] = this.data[2][0] - subData[2][0];
        returnedData[0][1] = this.data[0][1] - subData[0][1];
        returnedData[1][1] = this.data[1][1] - subData[1][1];
        returnedData[2][1] = this.data[2][1] - subData[2][1];
        returnedData[0][2] = this.data[0][2] - subData[0][2];
        returnedData[1][2] = this.data[1][2] - subData[1][2];
        returnedData[2][2] = this.data[2][2] - subData[2][2];
        // get a matrix from the double[][]:
        return new Matrix3D(returnedData);
    }

    /**
     * Computes the multiplication between a Matrix3D and a Vector3D
     * 
     * @param mult
     *        the Vector3D right term of the multiplication
     * @return the resulting Vector3D
     */
    public Vector3D multiply(final Vector3D mult) {
        final double[] returnedData = new double[3];

        // computation of each value of the returned vector
        returnedData[0] = mult.getX() * this.data[0][0] + mult.getY() * this.data[0][1] + mult.getZ() * this.data[0][2];
        returnedData[1] = mult.getX() * this.data[1][0] + mult.getY() * this.data[1][1] + mult.getZ() * this.data[1][2];
        returnedData[2] = mult.getX() * this.data[2][0] + mult.getY() * this.data[2][1] + mult.getZ() * this.data[2][2];

        return new Vector3D(returnedData[0], returnedData[1], returnedData[2]);
    }

    /**
     * Computes a multiplication of this Matrix3D with a scalar
     * 
     * @param x
     *        the Matrix3D right term of the multiplication
     * @return the resulting Matrix3D
     */
    public Matrix3D multiply(final double x) {
        // double[][] initialization:
        final double[][] returnedData = new double[3][3];

        // computation of each value of the returned matrix
        returnedData[0][0] = this.data[0][0] * x;
        returnedData[1][0] = this.data[1][0] * x;
        returnedData[2][0] = this.data[2][0] * x;
        returnedData[0][1] = this.data[0][1] * x;
        returnedData[1][1] = this.data[1][1] * x;
        returnedData[2][1] = this.data[2][1] * x;
        returnedData[0][2] = this.data[0][2] * x;
        returnedData[1][2] = this.data[1][2] * x;
        returnedData[2][2] = this.data[2][2] * x;
        // get a matrix from the double[][]:
        return new Matrix3D(returnedData);
    }

    /**
     * Computes the multiplication of the transposed matrix of this Matrix3D with a Vector3D
     * 
     * @param vector
     *        the Vector3D right term of the multiplication
     * @return the resulting Vector3D
     */
    public Vector3D transposeAndMultiply(final Vector3D vector) {
        return this.transpose().multiply(vector);
    }

    /**
     * Given a threshold, is this an orthogonal matrix?
     * 
     * The method indicates if the matrix is orthogonal. To do so the method checks if the column vectors of the matrix
     * form an orthonomal set.
     * 
     * @param thresholdNorm
     *        : allowed error with respect to the normality of the vectors
     * @param thresholdOrthogonality
     *        : allowed error with respect to the mutual orthogonality of the vectors
     * @return true if the vectors form an orthonormal set taking into account an allowed error, false otherwise
     */
    public boolean isOrthogonal(final double thresholdNorm, final double thresholdOrthogonality) {
        // identical RealMatrix creation
        final RealMatrix testedMat = this.getRealMatrix();

        return testedMat.isOrthogonal(thresholdNorm, thresholdOrthogonality);
    }

    /**
     * @return the data
     */
    public double[][] getData() {
        final double[][] returnedData = new double[3][3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                returnedData[i][j] = this.data[i][j];
            }
        }
        return returnedData;
    }

    /**
     * @return the Array2DRowRealMatrix with identical data
     */
    public RealMatrix getRealMatrix() {
        return MatrixUtils.createRealMatrix(this.data);
    }

    /**
     * Asserts two Matrix3D to be equal.
     * 
     * @param other
     *        the Matrix3D to be compared to this
     * @return true if the entries of both Matrix3D are equal
     */
    @Override
    public boolean equals(final Object other) {
        boolean isEqual = true;
        // smple test
        if (this == other) {
            isEqual = true;
        }
        // test of the type
        if (other instanceof Matrix3D) {
            final Matrix3D mat = (Matrix3D) other;
            // test of each value
            for (int i = 0; i < 3 && isEqual; ++i) {
                for (int j = 0; j < 3 && isEqual; ++j) {
                    isEqual = Precision.equalsWithRelativeTolerance(mat.getEntry(i, j), this.data[i][j]);
                }
            }
        } else {
            isEqual = false;
        }

        return isEqual;
    }

    /**
     * Get a hashCode for the 3D matrix.
     * <p>
     * All NaN values have the same hash code.
     * </p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        // Check if any of the entry of the instance is Nan
        if (this.isNaN()) {
            return NAN_HASH_CODE;
        }
        // Initialize hashCode
        int hashValue = 0;
        // Compute hashCode from matrix entries
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                hashValue += i * j * j * MathUtils.hash(this.data[i][j]);
            }
        }
        return hashValue;
    }

    /**
     * Returns true if any entry of this matrix is NaN; false otherwise
     * 
     * @return true if any entry of this matrix is NaN; false otherwise
     */
    public boolean isNaN() {
        boolean isNaN = false;
        // test of each value
        for (int i = 0; i < 3 && !isNaN; ++i) {
            for (int j = 0; j < 3 && !isNaN; ++j) {
                isNaN = Double.isNaN(this.data[i][j]);
            }
        }
        return isNaN;
    }

    /**
     * Get a string representation for this matrix.
     * 
     * @return a string representation for this matrix
     */
    @Override
    public String toString() {
        final StringBuffer res = new StringBuffer();
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        res.append(shortClassName).append(open);

        for (int i = 0; i < 3; ++i) {
            if (i > 0) {
                // add a comma:
                res.append(comma);
            }
            // open {
            res.append(open);
            for (int j = 0; j < 3; ++j) {
                if (j > 0) {
                    // add a comma:
                    res.append(comma);
                }
                // add one entry value:
                res.append(this.getEntry(i, j));
            }
            // close }
            res.append(close);
        }

        res.append(close);
        return res.toString();
    }

    /**
     * Returns the value of one entry of the matrix
     * 
     * @param row
     *        the row of the wanted data
     * @param column
     *        the column of the wanted data
     * @return the data value
     */
    public double getEntry(final int row, final int column) {
        return this.data[row][column];
    }

}
