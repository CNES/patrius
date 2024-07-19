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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.filter;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.CholeskyDecomposition;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.MatrixDimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.NonSquareMatrixException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SingularMatrixException;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Implementation of a Kalman filter to estimate the state <i>x<sub>k</sub></i>
 * of a discrete-time controlled process that is governed by the linear
 * stochastic difference equation:
 * 
 * <pre>
 * <i>x<sub>k</sub></i> = <b>A</b><i>x<sub>k-1</sub></i> + <b>B</b><i>u<sub>k-1</sub></i> + <i>w<sub>k-1</sub></i>
 * </pre>
 * 
 * with a measurement <i>x<sub>k</sub></i> that is
 * 
 * <pre>
 * <i>z<sub>k</sub></i> = <b>H</b><i>x<sub>k</sub></i> + <i>v<sub>k</sub></i>.
 * </pre>
 * 
 * <p>
 * The random variables <i>w<sub>k</sub></i> and <i>v<sub>k</sub></i> represent the process and measurement noise and
 * are assumed to be independent of each other and distributed with normal probability (white noise).
 * <p>
 * The Kalman filter cycle involves the following steps:
 * <ol>
 * <li>predict: project the current state estimate ahead in time</li>
 * <li>correct: adjust the projected estimate by an actual measurement</li>
 * </ol>
 * <p>
 * The Kalman filter is initialized with a {@link ProcessModel} and a {@link MeasurementModel}, which contain the
 * corresponding transformation and noise covariance matrices. The parameter names used in the respective models
 * correspond to the following names commonly used in the mathematical literature:
 * <ul>
 * <li>A - state transition matrix</li>
 * <li>B - control input matrix</li>
 * <li>H - measurement matrix</li>
 * <li>Q - process noise covariance matrix</li>
 * <li>R - measurement noise covariance matrix</li>
 * <li>P - error covariance matrix</li>
 * </ul>
 * 
 * @see <a href="http://www.cs.unc.edu/~welch/kalman/">Kalman filter resources</a>
 * @see <a href="http://www.cs.unc.edu/~welch/media/pdf/kalman_intro.pdf">An introduction to the Kalman filter by Greg
 *      Welch and Gary Bishop</a>
 * @see <a href="http://academic.csuohio.edu/simond/courses/eec644/kalman.pdf"> Kalman filter example by Dan Simon</a>
 * @see ProcessModel
 * @see MeasurementModel
 * @since 3.0
 * @version $Id: KalmanFilter.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class KalmanFilter {
    /** The process model used by this filter instance. */
    private final ProcessModel processModel;
    /** The measurement model used by this filter instance. */
    private final MeasurementModel measurementModel;
    /** The transition matrix, equivalent to A. */
    private final RealMatrix transitionMatrix;
    /** The transposed transition matrix. */
    private final RealMatrix transitionMatrixT;
    /** The control matrix, equivalent to B. */
    private RealMatrix controlMatrix;
    /** The measurement matrix, equivalent to H. */
    private final RealMatrix measurementMatrix;
    /** The transposed measurement matrix. */
    private final RealMatrix measurementMatrixT;
    /** The internal state estimation vector, equivalent to x hat. */
    private RealVector stateEstimation;
    /** The error covariance matrix, equivalent to P. */
    private RealMatrix errorCovariance;

    /**
     * Creates a new Kalman filter with the given process and measurement models.
     * 
     * @param process
     *        the model defining the underlying process dynamics
     * @param measurement
     *        the model defining the given measurement characteristics
     * @throws NullArgumentException
     *         if any of the given inputs is null (except for the control matrix)
     * @throws NonSquareMatrixException
     *         if the transition matrix is non square
     * @throws DimensionMismatchException
     *         if the column dimension of the transition matrix does not match the dimension of the
     *         initial state estimation vector
     * @throws MatrixDimensionMismatchException
     *         if the matrix dimensions do not fit together
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public KalmanFilter(final ProcessModel process, final MeasurementModel measurement) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        MathUtils.checkNotNull(process);
        MathUtils.checkNotNull(measurement);

        this.processModel = process;
        this.measurementModel = measurement;

        this.transitionMatrix = this.processModel.getStateTransitionMatrix();
        MathUtils.checkNotNull(this.transitionMatrix);
        this.transitionMatrixT = this.transitionMatrix.transpose();

        // create an empty matrix if no control matrix was given
        if (this.processModel.getControlMatrix() == null) {
            this.controlMatrix = new Array2DRowRealMatrix();
        } else {
            this.controlMatrix = this.processModel.getControlMatrix();
        }

        this.measurementMatrix = this.measurementModel.getMeasurementMatrix();
        MathUtils.checkNotNull(this.measurementMatrix);
        this.measurementMatrixT = this.measurementMatrix.transpose();

        // check that the process and measurement noise matrices are not null
        // they will be directly accessed from the model as they may change
        // over time
        final RealMatrix processNoise = this.processModel.getProcessNoise();
        MathUtils.checkNotNull(processNoise);
        final RealMatrix measNoise = this.measurementModel.getMeasurementNoise();
        MathUtils.checkNotNull(measNoise);

        // set the initial state estimate to a zero vector if it is not
        // available from the process model
        if (this.processModel.getInitialStateEstimate() == null) {
            this.stateEstimation = new ArrayRealVector(this.transitionMatrix.getColumnDimension());
        } else {
            this.stateEstimation = this.processModel.getInitialStateEstimate();
        }

        if (this.transitionMatrix.getColumnDimension() != this.stateEstimation.getDimension()) {
            throw new DimensionMismatchException(this.transitionMatrix.getColumnDimension(),
                this.stateEstimation.getDimension());
        }

        // initialize the error covariance to the process noise if it is not
        // available from the process model
        if (this.processModel.getInitialErrorCovariance() == null) {
            this.errorCovariance = processNoise.copy();
        } else {
            this.errorCovariance = this.processModel.getInitialErrorCovariance();
        }

        // sanity checks, the control matrix B may be null

        // A must be a square matrix
        if (!this.transitionMatrix.isSquare()) {
            throw new NonSquareMatrixException(
                this.transitionMatrix.getRowDimension(),
                this.transitionMatrix.getColumnDimension());
        }

        // row dimension of B must be equal to A
        if (this.controlMatrix != null) {
            final boolean cond = controlMatrix.getRowDimension() > 0 && controlMatrix.getColumnDimension() > 0;
            if (cond && (this.controlMatrix.getRowDimension() != this.transitionMatrix.getRowDimension() ||
                    this.controlMatrix.getColumnDimension() != 1)) {
                throw new MatrixDimensionMismatchException(this.controlMatrix.getRowDimension(),
                        this.controlMatrix.getColumnDimension(),
                        this.transitionMatrix.getRowDimension(), 1);
            }
        }

        // Q must be equal to A
        MatrixUtils.checkAdditionCompatible(this.transitionMatrix, processNoise);

        // column dimension of H must be equal to row dimension of A
        if (this.measurementMatrix.getColumnDimension() != this.transitionMatrix.getRowDimension()) {
            throw new MatrixDimensionMismatchException(this.measurementMatrix.getRowDimension(),
                this.measurementMatrix.getColumnDimension(),
                this.measurementMatrix.getRowDimension(),
                this.transitionMatrix.getRowDimension());
        }

        // row dimension of R must be equal to row dimension of H
        if (measNoise.getRowDimension() != this.measurementMatrix.getRowDimension() ||
            measNoise.getColumnDimension() != 1) {
            throw new MatrixDimensionMismatchException(measNoise.getRowDimension(),
                measNoise.getColumnDimension(),
                this.measurementMatrix.getRowDimension(), 1);
        }
    }

    /**
     * Returns the dimension of the state estimation vector.
     * 
     * @return the state dimension
     */
    public int getStateDimension() {
        return this.stateEstimation.getDimension();
    }

    /**
     * Returns the dimension of the measurement vector.
     * 
     * @return the measurement vector dimension
     */
    public int getMeasurementDimension() {
        return this.measurementMatrix.getRowDimension();
    }

    /**
     * Returns the current state estimation vector.
     * 
     * @return the state estimation vector
     */
    public double[] getStateEstimation() {
        return this.stateEstimation.toArray();
    }

    /**
     * Returns a copy of the current state estimation vector.
     * 
     * @return the state estimation vector
     */
    public RealVector getStateEstimationVector() {
        return this.stateEstimation.copy();
    }

    /**
     * Returns the current error covariance matrix.
     * 
     * @return the error covariance matrix
     */
    public double[][] getErrorCovariance() {
        return this.errorCovariance.getData();
    }

    /**
     * Returns a copy of the current error covariance matrix.
     * 
     * @return the error covariance matrix
     */
    public RealMatrix getErrorCovarianceMatrix() {
        return this.errorCovariance.copy();
    }

    /**
     * Predict the internal state estimation one time step ahead.
     */
    public void predict() {
        this.predict((RealVector) null);
    }

    /**
     * Predict the internal state estimation one time step ahead.
     * 
     * @param u
     *        the control vector
     * @throws DimensionMismatchException
     *         if the dimension of the control vector does not fit
     */
    public void predict(final double[] u) {
        this.predict(new ArrayRealVector(u));
    }

    /**
     * Predict the internal state estimation one time step ahead.
     * 
     * @param u
     *        the control vector
     * @throws DimensionMismatchException
     *         if the dimension of the control vector does not match
     */
    public void predict(final RealVector u) {
        // sanity checks
        if (u != null &&
            u.getDimension() != this.controlMatrix.getColumnDimension()) {
            throw new DimensionMismatchException(u.getDimension(),
                this.controlMatrix.getColumnDimension());
        }

        // project the state estimation ahead (a priori state)
        // xHat(k)- = A * xHat(k-1) + B * u(k-1)
        this.stateEstimation = this.transitionMatrix.operate(this.stateEstimation);

        // add control input if it is available
        if (u != null) {
            this.stateEstimation = this.stateEstimation.add(this.controlMatrix.operate(u));
        }

        // project the error covariance ahead
        // P(k)- = A * P(k-1) * A' + Q
        this.errorCovariance = this.transitionMatrix.multiply(this.errorCovariance)
            .multiply(this.transitionMatrixT)
            .add(this.processModel.getProcessNoise());
    }

    /**
     * Correct the current state estimate with an actual measurement.
     * 
     * @param z
     *        the measurement vector
     * @throws NullArgumentException
     *         if the measurement vector is {@code null}
     * @throws DimensionMismatchException
     *         if the dimension of the measurement vector does not fit
     * @throws SingularMatrixException
     *         if the covariance matrix could not be inverted
     */
    public void correct(final double[] z) {
        this.correct(new ArrayRealVector(z));
    }

    /**
     * Correct the current state estimate with an actual measurement.
     * 
     * @param z
     *        the measurement vector
     * @throws NullArgumentException
     *         if the measurement vector is {@code null}
     * @throws DimensionMismatchException
     *         if the dimension of the measurement vector does not fit
     * @throws SingularMatrixException
     *         if the covariance matrix could not be inverted
     */
    public void correct(final RealVector z) {

        // sanity checks
        MathUtils.checkNotNull(z);
        if (z.getDimension() != this.measurementMatrix.getRowDimension()) {
            throw new DimensionMismatchException(z.getDimension(),
                this.measurementMatrix.getRowDimension());
        }

        // S = H * P(k) - * H' + R
        final RealMatrix s = this.measurementMatrix.multiply(this.errorCovariance)
            .multiply(this.measurementMatrixT)
            .add(this.measurementModel.getMeasurementNoise());

        // invert S
        // as the error covariance matrix is a symmetric positive
        // semi-definite matrix, we can use the cholesky decomposition
        final DecompositionSolver solver = new CholeskyDecomposition(s).getSolver();
        final RealMatrix invertedS = solver.getInverse();

        // Inn = z(k) - H * xHat(k)-
        final RealVector innovation = z.subtract(this.measurementMatrix.operate(this.stateEstimation));

        // calculate gain matrix
        // K(k) = P(k)- * H' * (H * P(k)- * H' + R)^-1
        // K(k) = P(k)- * H' * S^-1
        final RealMatrix kalmanGain = this.errorCovariance.multiply(this.measurementMatrixT).multiply(invertedS);

        // update estimate with measurement z(k)
        // xHat(k) = xHat(k)- + K * Inn
        this.stateEstimation = this.stateEstimation.add(kalmanGain.operate(innovation));

        // update covariance of prediction error
        // P(k) = (I - K * H) * P(k)-
        final RealMatrix identity = MatrixUtils.createRealIdentityMatrix(kalmanGain.getRowDimension(), true);
        this.errorCovariance =
            identity.subtract(kalmanGain.multiply(this.measurementMatrix)).multiply(this.errorCovariance);
    }
}
