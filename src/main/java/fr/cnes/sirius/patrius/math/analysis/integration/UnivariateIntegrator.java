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
package fr.cnes.sirius.patrius.math.analysis.integration;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;

/**
 * Interface for univariate real integration algorithms.
 * 
 * @version $Id: UnivariateIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public interface UnivariateIntegrator extends Serializable {

    /**
     * Get the actual relative accuracy.
     * 
     * @return the accuracy
     */
    double getRelativeAccuracy();

    /**
     * Get the actual absolute accuracy.
     * 
     * @return the accuracy
     */
    double getAbsoluteAccuracy();

    /**
     * Get the min limit for the number of iterations.
     * 
     * @return the actual min limit
     */
    int getMinimalIterationCount();

    /**
     * Get the upper limit for the number of iterations.
     * 
     * @return the actual upper limit
     */
    int getMaximalIterationCount();

    /**
     * Integrate the function in the given interval.
     * 
     * @param maxEval
     *        Maximum number of evaluations.
     * @param f
     *        the integrand function
     * @param min
     *        the min bound for the interval
     * @param max
     *        the upper bound for the interval
     * @return the value of integral
     * @throws TooManyEvaluationsException
     *         if the maximum number of function
     *         evaluations is exceeded.
     * @throws MaxCountExceededException
     *         if the maximum iteration count is exceeded
     *         or the integrator detects convergence problems otherwise
     * @throws MathIllegalArgumentException
     *         if min > max or the endpoints do not
     *         satisfy the requirements specified by the integrator
     * @throws NullArgumentException
     *         if {@code f} is {@code null}.
     */
    double integrate(int maxEval, UnivariateFunction f, double min,
                     double max);

    /**
     * Get the number of function evaluations of the last run of the integrator.
     * 
     * @return number of function evaluations
     */
    int getEvaluations();

    /**
     * Get the number of iterations of the last run of the integrator.
     * 
     * @return number of iterations
     */
    int getIterations();

}
