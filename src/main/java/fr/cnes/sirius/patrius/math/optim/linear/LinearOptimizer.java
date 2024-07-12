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
package fr.cnes.sirius.patrius.math.optim.linear;

import java.util.Collection;
import java.util.Collections;

import fr.cnes.sirius.patrius.math.exception.TooManyIterationsException;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateOptimizer;

/**
 * Base class for implementing linear optimizers.
 * 
 * @version $Id: LinearOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class LinearOptimizer
    extends MultivariateOptimizer {
    // CHECKSTYLE: resume AbstractClassName check

    /**
     * Linear objective function.
     */
    private LinearObjectiveFunction function;
    /**
     * Linear constraints.
     */
    private Collection<LinearConstraint> linearConstraints;
    /**
     * Whether to restrict the variables to non-negative values.
     */
    private boolean nonNegative;

    /**
     * Simple constructor with default settings.
     * 
     */
    protected LinearOptimizer() {
        // No convergence checker.
        super(null);
    }

    /**
     * @return {@code true} if the variables are restricted to non-negative values.
     */
    protected boolean isRestrictedToNonNegative() {
        return this.nonNegative;
    }

    /**
     * @return the optimization type.
     */
    protected LinearObjectiveFunction getFunction() {
        return this.function;
    }

    /**
     * @return the optimization type.
     */
    protected Collection<LinearConstraint> getConstraints() {
        return Collections.unmodifiableCollection(this.linearConstraints);
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.MaxIter}</li>
     *        <li>{@link LinearObjectiveFunction}</li>
     *        <li>{@link LinearConstraintSet}</li>
     *        <li>{@link NonNegativeConstraint}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyIterationsException
     *         if the maximal number of
     *         iterations is exceeded.
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
     *        <li>{@link LinearObjectiveFunction}</li>
     *        <li>{@link LinearConstraintSet}</li>
     *        <li>{@link NonNegativeConstraint}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof LinearObjectiveFunction) {
                this.function = (LinearObjectiveFunction) data;
                continue;
            }
            if (data instanceof LinearConstraintSet) {
                this.linearConstraints = ((LinearConstraintSet) data).getConstraints();
                continue;
            }
            if (data instanceof NonNegativeConstraint) {
                this.nonNegative = ((NonNegativeConstraint) data).isRestrictedToNonNegative();
                continue;
            }
        }
    }
}
