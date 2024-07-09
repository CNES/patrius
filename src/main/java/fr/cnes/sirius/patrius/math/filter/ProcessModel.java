/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.filter;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;

/**
 * Defines the process dynamics model for the use with a {@link KalmanFilter}.
 * 
 * @since 3.0
 * @version $Id: ProcessModel.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface ProcessModel {
    /**
     * Returns the state transition matrix.
     * 
     * @return the state transition matrix
     */
    RealMatrix getStateTransitionMatrix();

    /**
     * Returns the control matrix.
     * 
     * @return the control matrix
     */
    RealMatrix getControlMatrix();

    /**
     * Returns the process noise matrix. This method is called by the {@link KalmanFilter} every
     * prediction step, so implementations of this interface may return a modified process noise
     * depending on the current iteration step.
     * 
     * @return the process noise matrix
     * @see KalmanFilter#predict()
     * @see KalmanFilter#predict(double[])
     * @see KalmanFilter#predict(RealVector)
     */
    RealMatrix getProcessNoise();

    /**
     * Returns the initial state estimation vector.
     * <p>
     * <b>Note:</b> if the return value is zero, the Kalman filter will initialize the state estimation with a zero
     * vector.
     * 
     * @return the initial state estimation vector
     */
    RealVector getInitialStateEstimate();

    /**
     * Returns the initial error covariance matrix.
     * <p>
     * <b>Note:</b> if the return value is zero, the Kalman filter will initialize the error covariance with the process
     * noise matrix.
     * 
     * @return the initial error covariance matrix
     */
    RealMatrix getInitialErrorCovariance();
}
