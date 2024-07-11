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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;

/**
 * Base class for solvers.
 * 
 * @since 3.0
 * @version $Id: AbstractPolynomialSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class AbstractPolynomialSolver
    extends BaseAbstractUnivariateSolver<PolynomialFunction>
    implements PolynomialSolver {
    /** Function. */
    private PolynomialFunction polynomialFunction;

    /**
     * Construct a solver with given absolute accuracy.
     * 
     * @param absoluteAccuracy
     *        Maximum absolute error.
     */
    protected AbstractPolynomialSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }

    /**
     * Construct a solver with given accuracies.
     * 
     * @param relativeAccuracy
     *        Maximum relative error.
     * @param absoluteAccuracy
     *        Maximum absolute error.
     */
    protected AbstractPolynomialSolver(final double relativeAccuracy,
        final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
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
    protected AbstractPolynomialSolver(final double relativeAccuracy,
        final double absoluteAccuracy,
        final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setup(final int maxEval, final PolynomialFunction f,
                         final double min, final double max, final double startValue) {
        super.setup(maxEval, f, min, max, startValue);
        this.polynomialFunction = f;
    }

    /**
     * @return the coefficients of the polynomial function.
     */
    protected double[] getCoefficients() {
        return this.polynomialFunction.getCoefficients();
    }
}
