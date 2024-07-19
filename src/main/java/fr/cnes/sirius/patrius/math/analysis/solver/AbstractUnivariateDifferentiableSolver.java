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
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;

/**
 * Provide a default implementation for several functions useful to generic
 * solvers.
 * 
 * @since 3.1
 * @version $Id: AbstractUnivariateDifferentiableSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class AbstractUnivariateDifferentiableSolver
    extends BaseAbstractUnivariateSolver<UnivariateDifferentiableFunction>
    implements UnivariateDifferentiableSolver {

    /** Serializable UID. */
    private static final long serialVersionUID = 6261906775912038820L;

    /** Function to solve. */
    private UnivariateDifferentiableFunction function;

    /**
     * Construct a solver with given absolute accuracy.
     * 
     * @param absoluteAccuracy
     *        Maximum absolute error.
     */
    protected AbstractUnivariateDifferentiableSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }

    /**
     * Construct a solver with given accuracies.
     * 
     * @param relativeAccuracy
     *        Maximum relative error.
     * @param absoluteAccuracy
     *        Maximum absolute error.
     * @param functionValueAccuracy
     *        Maximum function value error.
     */
    protected AbstractUnivariateDifferentiableSolver(final double relativeAccuracy,
        final double absoluteAccuracy,
        final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
    }

    /**
     * Compute the objective function value.
     * 
     * @param point
     *        Point at which the objective function must be evaluated.
     * @return the objective function value and derivative at specified point.
     * @throws fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException
     *         if the maximal number of evaluations is exceeded.
     */
    protected DerivativeStructure computeObjectiveValueAndDerivative(final double point) {
        this.incrementEvaluationCount();
        return this.function.value(new DerivativeStructure(1, 1, 0, point));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setup(final int maxEval, final UnivariateDifferentiableFunction f,
                         final double min, final double max, final double startValue) {
        super.setup(maxEval, f, min, max, startValue);
        this.function = f;
    }
}
