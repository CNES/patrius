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
 *
 * @history 21/08/2014 (creation)
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:277:21/08/2014:Covariance Matrix Interpolation
 * VERSION::FA:387:05/12/2014:Problem in Covariance Matrix Interpolation
 * VERSION::FA:437:06/05/2015:Problem in Covariance Matrix Interpolation Algorithm
 * VERSION::DM:482:02/11/2015:New implementation of the class without CovarianceMatrix but using RealMatrix
 * VERSION::FA:579:17/03/2016:Bug in method createApproximatedTransitionMatrix() of CovarianceInterpolation class
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.covariance;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class allows the interpolation of a covariance matrix at a date t in [t<sub>1</sub>, t<sub>2</sub>]
 * using the surrounding covariances matrices Cov<sub>t1</sub> Cov<sub>t2</sub>.
 * The interpolated covariance matrix is computed using a polynomial approximation of the transition matrix.
 **
 * @author Sophie LAURENS
 * 
 * @concurrency not thread-safe
 * @concurrency.comment internal mutable attributes
 * 
 * @version $Id: CovarianceInterpolation.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 2.3
 * 
 */
public class CovarianceInterpolation {

    // constants for the class CovarianceInterpolation
    /** Dimension for a 3x3 matrix */
    private static final int DIM_THREE = 3;
    /** Dimension for a 6x6 matrix */
    private static final int DIM_SIX = 6;

    /** Null matrix of dimension 3 */
    private static final RealMatrix ZERO_MAT3 = createDiagonalMatrix(DIM_THREE, 0.);
    /** Null matrix of dimension 6 */
    private static final RealMatrix ZERO_MAT6 = createDiagonalMatrix(DIM_SIX, 0.);
    /** Identity matrix of dimension 3 */
    private static final RealMatrix ID_MAT3 = createDiagonalMatrix(DIM_THREE, 1.);
    /** Identity matrix of dimension 6 */
    private static final RealMatrix ID_MAT6 = createDiagonalMatrix(DIM_SIX, 1.);
    
    /** -3 */
    private static final double MINUS_THREE = -3.;

    // private attributs for the class CovarianceInterpolation
    /** First covariance matrix */
    private RealMatrix covarianceMatrix1;
    /** Second covariance matrix */
    private RealMatrix covarianceMatrix2;

    /** Date for first covariance matrix */
    private AbsoluteDate t1;
    /** Date for second covariance matrix */
    private AbsoluteDate t2;

    /**
     * Approximated transition matrix A, constant on [t1, t2]. If the interpolation interval
     * or the orbital parameters changes, it has to be computed again
     */
    private RealMatrix matrixA;
    /**
     * If covarianceMatrix1 or 2 is modified, meaning we interpolate on a different interval, the computation
     * of A has to be done again. If the orbit or the gravitation constant is modified, since A depends on them,
     * its computation has also to be done again.
     */
    private boolean changeOfA;

    /** Polynomial order of the interpolation, equals 0,1 or 2 */
    private int polynomialOrder;
    /** Orbit required to retrieve the PV coordinates of the satellite */
    private Orbit orbit;

    /**
     * Standard gravitational parameter, can be different from the one
     * contained in orbitSatellite
     */
    private double mu;

    /**
     * Constructor of the class CovarianceInterpolation
     * 
     * @param t1In
     *        : begining date of performed interpolation
     * @param matrix1
     *        : first covariance matrix
     * @param t2In
     *        : ending date of performed interpolation
     * @param matrix2
     *        : second covariance matrix
     * @param order
     *        : Polynomial order of the interpolation, equals 0,1 or 2
     * @param orbitSatellite
     *        : Orbit required to retrieve the PV coordinates of the satellite
     * @param muValue
     *        : Standard gravitational parameter, can be different from the one
     *        contained in orbitSatellite, because the mu value for conversion can be different from
     *        the mu value used for propagation.
     * 
     * @throws PatriusException
     *         DATE_OUTSIDE_INTERVAL or OUT_OF_RANGE_POLYNOMIAL_ORDER
     **/
    public CovarianceInterpolation(final AbsoluteDate t1In,
        final RealMatrix matrix1,
        final AbsoluteDate t2In,
        final RealMatrix matrix2,
        final int order,
        final Orbit orbitSatellite,
        final double muValue) throws PatriusException {

        this.covarianceMatrix1 = matrix1;
        this.covarianceMatrix2 = matrix2;
        this.t1 = t1In;
        this.t2 = t2In;

        // if t1 >= t2, no interpolation can be done
        if (this.t2.durationFrom(this.t1) <= 0) {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_INTERVAL);
        }

        // if the polynomial order of interpolation is not 0, 1 or 2, the interpolation cannot be done
        if (order > 2 || order < 0) {
            throw new PatriusException(PatriusMessages.OUT_OF_RANGE_POLYNOMIAL_ORDER);
        } else {
            this.polynomialOrder = order;
        }

        this.orbit = orbitSatellite;
        this.mu = muValue;

        // the approximated transition matrix is computed and valid for interval [t1,t2]
        this.matrixA = this.createApproximatedTransitionMatrix();
        this.changeOfA = false;
    }

    /**
     * Constructor of the class CovarianceInterpolation
     * 
     * @param t1In
     *        : begining date of performed interpolation
     * @param matrix1
     *        : first covariance matrix
     * @param t2In
     *        : ending date of performed interpolation
     * @param matrix2
     *        : second covariance matrix
     * @param order
     *        : Polynomial order of the interpolation, equals 0,1 or 2
     * @param orbitSatellite
     *        : Orbit required to retrieve the PV coordinates of the satellite
     * @param muValue
     *        : Standard gravitational parameter, can be different from the one
     *        contained in orbitSatellite, because the mu value for conversion can be different from
     *        the mu value used for propagation.
     * 
     * @throws PatriusException
     *         DATE_OUTSIDE_INTERVAL or OUT_OF_RANGE_POLYNOMIAL_ORDER
     **/
    public CovarianceInterpolation(final AbsoluteDate t1In,
        final double[][] matrix1,
        final AbsoluteDate t2In,
        final double[][] matrix2,
        final int order,
        final Orbit orbitSatellite,
        final double muValue) throws PatriusException {

        this(t1In, new Array2DRowRealMatrix(matrix1), t2In, new Array2DRowRealMatrix(matrix2), order,
            orbitSatellite, muValue);
    }

    /**
     * Computes the interpolation of a covariance matrix based on its two surrounding covariance matrices which define
     * the interpolation interval allowed. Since the transition matrix is constant on [t1, t], its computation
     * has to be done only once
     * 
     * @param t
     *        : Interpolation date
     * @return interpolatedCovarianceMatrix under the form of a RealMatrix
     * @throws PatriusException
     *         if the date does not belong to the interpolation interval defined at the
     *         construction of the instance through the two CovarianceMatrix entered as parameters.
     */
    public RealMatrix interpolate(final AbsoluteDate t) throws PatriusException {

        // absolute dates and covariance matrices required for the interpolation
        // if the date t is not between t1 and t2, the interpolation cannot be done
        if (t.durationFrom(this.t1) < 0 || t.durationFrom(this.t2) > 0) {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_INTERVAL);
        }

        // tests if date equals t1 or t2 : in that case, no interpolation has to be done
        final double delta = t.durationFrom(this.t1);
        final double deltaPlus = this.t2.durationFrom(t);
        boolean isBoundary = false;
        RealMatrix noInterpolationMatrix = this.covarianceMatrix1;
        if (delta == 0) {
            isBoundary = true;
        }
        if (deltaPlus == 0) {
            isBoundary = true;
            noInterpolationMatrix = this.covarianceMatrix2;
        }
        if (isBoundary) {
            return noInterpolationMatrix;
        }

        // otherwise, do interpolation
        // t2 != t1
        final double alpha = delta / this.t2.durationFrom(this.t1);

        // if it is not the first call, checks if the interpolation interval or orbital parameters has been modified
        if (this.changeOfA) {
            // we recompute the approximated transition matrix, and set it for next computations ...
            this.matrixA = this.createApproximatedTransitionMatrix();
            // ... until t1 or t2 is modified again.
            this.changeOfA = false;
        }

        // Transition matrices
        final RealMatrix phi1 = this.createTransitionMatrix(this.t1, t);
        final RealMatrix phi2 = this.createTransitionMatrix(this.t2, t);

        // matrix products (1 - alpha) * phi1 * P(t1) * phi^T
        final RealMatrix firstMember = phi1.multiply(this.covarianceMatrix1).multiply(phi1.transpose())
            .scalarMultiply(1. - alpha);
        // idem with the second interpolation date
        final RealMatrix secondMember = phi2.multiply(this.covarianceMatrix2).multiply(phi2.transpose())
            .scalarMultiply(alpha);
        // covMatrix2 = phi2.multiply(covMatrix2);

        // barycentric approximation
        return firstMember.add(secondMember);
    }

    /**
     * Computes the interpolation of a covariance matrix based on its two surrounding covariance matrices which define
     * the interpolation interval allowed. Since the transition matrix is constant on [t1, t], its computation
     * has to be done only once
     * 
     * @param t
     *        : Interpolation date
     * @return interpolatedCovarianceMatrix under the form double[][]
     * @throws PatriusException
     *         if the date does not belong to the interpolation interval defined at the
     *         construction of the instance through the two CovarianceMatrix entered as parameters.
     */
    public double[][] interpolateArray(final AbsoluteDate t) throws PatriusException {
        return this.interpolate(t).getData();
    }

    /**
     * Computes a 3x3 matrix, considered as a constant on [t1,t],
     * where t1 is the first interpolation date and t the date where the interpolation is searched.
     * It is computed in the constructor, since it is valid for every interpolation in the interval [t1,t2].
     * If the first or second CovarianceMatrix is modified, meaning the interpolation interval [t1, t2] changes,
     * the matrix has to be re computed.
     * 
     * @return approximatedTransitionMatrix : a 3x3 matrix that occurs in the 6x6 transition matrix expression.
     */
    private RealMatrix createApproximatedTransitionMatrix() {

        // retrieving the position vector
        final PVCoordinates pv = this.orbit.getPVCoordinates();
        final Vector3D position = pv.getPosition();

        // Norm
        final double r = position.getNorm();

        // the position vector must not be null
        if (r < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_VECTOR);
        }

        // position coordinates
        final double x = position.getX();
        final double y = position.getY();
        final double z = position.getZ();

        // matrix filling: - mu / r^3 (I3 - 3/r^2 PP^T)

        // initialization
        final RealMatrix temporaryMatrix = ZERO_MAT3.copy();

        // matrix PP^T
        temporaryMatrix.addToEntry(0, 0, x * x);
        temporaryMatrix.addToEntry(0, 1, x * y);
        temporaryMatrix.addToEntry(0, 2, x * z);

        temporaryMatrix.addToEntry(1, 0, x * y);
        temporaryMatrix.addToEntry(1, 1, y * y);
        temporaryMatrix.addToEntry(1, 2, y * z);

        temporaryMatrix.addToEntry(2, 0, x * z);
        temporaryMatrix.addToEntry(2, 1, y * z);
        temporaryMatrix.addToEntry(2, 2, z * z);

        // matrix scaling by -3/r^2
        RealMatrix tmpMatrix1 =
            temporaryMatrix.scalarMultiply(MathLib.divide(MINUS_THREE, r * r));

        // adding the identity matrix
        tmpMatrix1 = tmpMatrix1.add(ID_MAT3);

        // matrix scaling
        return tmpMatrix1.scalarMultiply(MathLib.divide(-this.mu, MathLib.pow(r, 3)));
    }

    /**
     * Creates the transition matrix between t1 and t. It can be approximated by
     * at order 0 : identity matrix 6x6
     * at order 1 : I + JPV (t-t1)
     * at order 2 : I + JPV (t-t1) + 0.5*JPV^2 (t-t1)^2
     * where JPV = [ 0_3 I_3
     * A 0_3]
     * 
     * JPV^2 = [ A 0_3
     * 0_3 A]
     * and A is the approximated transition matrix, constant on [t1, t] and
     * computed by createApproximatedTransitionMatrix .
     * 
     * @param tLow
     *        : the lower bound of the interpolation interval
     * @param t
     *        : date where the interpolation is searched
     * @return phi : the transition matrix
     */
    private RealMatrix createTransitionMatrix(final AbsoluteDate tLow, final AbsoluteDate t) {

        // Nota bene: This method should be further optimized
        // the computation of JPV and JPV2 should not be done here
        // since they depend on position vector which is (considered as) constant
        // on the interpolation interval

        // deltaDate is the offset in seconds between the two dates and positive if t is posterior to t1
        final double deltaDate = t.durationFrom(tLow);

        // Initialization
        RealMatrix phi = ID_MAT6.copy();

        // if polynomial order = 1 or 2, we add JPV (t-t1)

        if (this.polynomialOrder > 0) {

            RealMatrix jpv = ZERO_MAT6.copy();
            jpv.setSubMatrix(ID_MAT3.getData(), 0, 3);
            final double[][] transitionMatrix = this.matrixA.getData();
            jpv.setSubMatrix(transitionMatrix, 3, 0);
            jpv = jpv.scalarMultiply(deltaDate);

            phi = phi.add(jpv);

            // if polynomial order = 2, we add 0.5 * JPV^2 (t-t1)^2
            if (this.polynomialOrder == 2) {

                RealMatrix jpv2 = ZERO_MAT6.copy();
                jpv2.setSubMatrix(transitionMatrix, 0, 0);
                jpv2.setSubMatrix(transitionMatrix, 3, 3);
                jpv2 = jpv2.scalarMultiply(deltaDate * deltaDate / 2.0);

                phi = phi.add(jpv2);
            }
        }
        return phi;
    }

    // Utility functions
    /**
     * Creates a diagonal square matrix of dimension dim equals to coef * identity (dim)
     * 
     * @param dim
     *        : dimension of the square matrix
     * @param coef
     *        : value of all the diagonal coefficients of the matrix
     * @return matrix : a Array2DRowRealMatrix square diagonal matrix proportional to identity
     */
    public static double[][] createDiagonalArray(final int dim, final double coef) {

        // initialization of the square matrix
        final double[][] array = new double[dim][dim];

        for (int i = 0; i < dim; i++) {
            array[i][i] = coef;
        }

        return array;
    }

    /**
     * Creates a diagonal square matrix of dimension dim equals to coef * identity (dim)
     * 
     * @param dim
     *        : dimension of the square matrix
     * @param coef
     *        : value of all the diagonal coefficients of the matrix
     * @return matrix : a Array2DRowRealMatrix square diagonal matrix proportional to identity
     */
    public static RealMatrix createDiagonalMatrix(final int dim, final double coef) {
        return new Array2DRowRealMatrix(createDiagonalArray(dim, coef));
    }

    // Getters and setters

    /**
     * Allows to change the CovarianceMatrix standing for the lower bound of the
     * interpolation interval, associated with t1. If do so, the computation of the
     * approximated transition matrix A has to be done again, since A is considered
     * constant on [t1,t2] if multiple calls to method interpolate(AbsoluteDate) are made.
     * 
     * @param covMatrix
     *        the new covariance matrix covarianceMatrix1
     * @param t
     *        : setting date for the new covariance matrix
     */
    public void setFirstCovarianceMatrix(final RealMatrix covMatrix, final AbsoluteDate t) {
        this.covarianceMatrix1 = covMatrix;
        this.t1 = t;
        this.changeOfA = true;
    }

    /**
     * Allows to change the CovarianceMatrix standing for the upper bound of the
     * interpolation interval, associated with t2. If do so, the computation of the
     * approximated transition matrix A has to be done again, since A is considered
     * constant on [t1,t2] if multiple calls to method interpolate(AbsoluteDate) are made.
     * 
     * @param covMatrix
     *        the new covariance matrix covarianceMatrix2
     * @param t
     *        : setting date for the new covariance matrix
     */
    public void setSecondCovarianceMatrix(final RealMatrix covMatrix, final AbsoluteDate t) {
        this.covarianceMatrix2 = covMatrix;
        this.t2 = t;
        this.changeOfA = true;
    }

    /**
     * @param order
     *        the polynomial order to set
     */
    public void setPolynomialOrder(final int order) {
        this.polynomialOrder = order;
    }

    /**
     * Allows to change the orbit. If do so, the computation of the
     * approximated transition matrix A has to be done again, since A depends on the PV coordinates
     * extracted from orbit.
     * 
     * @param newOrbit
     *        the orbit to set
     */
    public void setOrbit(final Orbit newOrbit) {
        this.orbit = newOrbit;
        this.changeOfA = true;
    }

    /**
     * Allows to change the value of the gravitational parameter. If do so, the computation of the
     * approximated transition matrix A has to be done again, since A depends on mu.
     * 
     * @param newMu
     *        the mu value to set
     */
    public void setMu(final double newMu) {
        this.mu = newMu;
        this.changeOfA = true;
    }

    /**
     * @return the first covariance matrix covarianceMatrix1
     */
    public RealMatrix getFirstCovarianceMatrix() {
        return this.covarianceMatrix1;
    }

    /**
     * @return the second covariance matrix covarianceMatrix1
     */
    public RealMatrix getSecondCovarianceMatrix() {
        return this.covarianceMatrix2;
    }

    /**
     * @return the polynomial order
     */
    public int getPolynomialOrder() {
        return this.polynomialOrder;
    }

    /**
     * @return the orbit
     */
    public Orbit getOrbit() {
        return this.orbit;
    }

    /**
     * @return the standard gravitational parameter
     */
    public double getMu() {
        return this.mu;
    }

    /**
     * @return beginning interpolation date t1
     */
    public AbsoluteDate getT1() {
        return this.t1;
    }

    /**
     * @return ending interpolation date t2
     */
    public AbsoluteDate getT2() {
        return this.t2;
    }
}
