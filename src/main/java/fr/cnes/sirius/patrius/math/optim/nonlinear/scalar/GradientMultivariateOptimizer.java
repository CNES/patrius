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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar;

import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;

/**
 * Base class for implementing optimizers for multivariate scalar
 * differentiable functions.
 * It contains boiler-plate code for dealing with gradient evaluation.
 * 
 * @version $Id: GradientMultivariateOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class GradientMultivariateOptimizer
    extends MultivariateOptimizer {
    // CHECKSTYLE: resume AbstractClassName check

    /**
     * Gradient of the objective function.
     */
    private MultivariateVectorFunction gradient;

    /**
     * @param checker
     *        Convergence checker.
     */
    protected GradientMultivariateOptimizer(final ConvergenceChecker<PointValuePair> checker) {
        super(checker);
    }

    /**
     * Compute the gradient vector.
     * 
     * @param params
     *        Point at which the gradient must be evaluated.
     * @return the gradient at the specified point.
     */
    protected double[] computeObjectiveGradient(final double[] params) {
        return this.gradient.value(params);
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.MaxEval}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.InitialGuess}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.SimpleBounds}</li>
     *        <li>{@link ObjectiveFunction}</li>
     *        <li>{@link GoalType}</li>
     *        <li>{@link ObjectiveFunctionGradient}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations (of the objective function) is exceeded.
     */
    @Override
    public PointValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Set up base class and perform computation.
        return super.optimize(optData);
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link ObjectiveFunctionGradient}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof ObjectiveFunctionGradient) {
                this.gradient = ((ObjectiveFunctionGradient) data).getObjectiveFunctionGradient();
                // If more data must be parsed, this statement _must_ be
                // changed to "continue".
                break;
            }
        }
    }
}
