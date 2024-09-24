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
 * VERSION:4.3:DM:DM-2099:15/05/2019: Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.ode.MultistepIntegrator;

/**
 * Base class for {@link AdamsBashforthIntegrator Adams-Bashforth} and {@link AdamsMoultonIntegrator Adams-Moulton}
 * integrators.
 * 
 * @version $Id: AdamsIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class AdamsIntegrator extends MultistepIntegrator {
    // CHECKSTYLE: resume AbstractClassName check

    /** Transformer. */
    private final AdamsNordsieckTransformer transformer;

    /**
     * Build an Adams integrator with the given order and step control parameters.
     * 
     * @param name name of the method
     * @param nSteps number of steps of the method excluding the one being computed
     * @param order order of the method
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param scalAbsoluteTolerance allowed absolute error
     * @param scalRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     * @exception NumberIsTooSmallException if order is 1 or less
     */
    public AdamsIntegrator(final String name, final int nSteps, final int order,
        final double minStep, final double maxStep, final double scalAbsoluteTolerance,
        final double scalRelativeTolerance, final boolean acceptSmall) {
        super(name, nSteps, order, minStep, maxStep, scalAbsoluteTolerance, scalRelativeTolerance,
            acceptSmall);
        this.transformer = AdamsNordsieckTransformer.getInstance(nSteps);
    }

    /**
     * Build an Adams integrator with the given order and step control parameters.
     * 
     * @param name name of the method
     * @param nSteps number of steps of the method excluding the one being computed
     * @param order order of the method
     * @param minStep minimal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param maxStep maximal step (sign is irrelevant, regardless of integration direction, forward
     *        or backward), the last step can be smaller than this
     * @param vecAbsoluteTolerance allowed absolute error
     * @param vecRelativeTolerance allowed relative error
     * @param acceptSmall if true, steps smaller than the minimal value are silently increased up to
     *        this value, if false such small steps generate an exception
     * @exception IllegalArgumentException if order is 1 or less
     */
    public AdamsIntegrator(final String name, final int nSteps, final int order,
        final double minStep, final double maxStep, final double[] vecAbsoluteTolerance,
        final double[] vecRelativeTolerance, final boolean acceptSmall) {
        super(name, nSteps, order, minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance,
            acceptSmall);
        this.transformer = AdamsNordsieckTransformer.getInstance(nSteps);
    }

    /** {@inheritDoc} */
    @Override
    public abstract void integrate(final ExpandableStatefulODE equations, final double t);

    /** {@inheritDoc} */
    @Override
    protected Array2DRowRealMatrix initializeHighOrderDerivatives(final double h, final double[] t,
                                                                  final double[][] y,
                                                                  final double[][] yDot) {
        return this.transformer.initializeHighOrderDerivatives(h, t, y, yDot);
    }

    /**
     * Update the high order scaled derivatives for Adams integrators (phase 1).
     * <p>
     * The complete update of high order derivatives has a form similar to:
     * 
     * <pre>
     * r<sub>n+1</sub> = (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u + P<sup>-1</sup> A P r<sub>n</sub>
     * </pre>
     * 
     * this method computes the P<sup>-1</sup> A P r<sub>n</sub> part.
     * </p>
     * 
     * @param highOrder
     *        high order scaled derivatives
     *        (h<sup>2</sup>/2 y'', ... h<sup>k</sup>/k! y(k))
     * @return updated high order derivatives
     * @see #updateHighOrderDerivativesPhase2(double[], double[], Array2DRowRealMatrix)
     */
    public Array2DRowRealMatrix updateHighOrderDerivativesPhase1(final Array2DRowRealMatrix highOrder) {
        return this.transformer.updateHighOrderDerivativesPhase1(highOrder);
    }

    /**
     * Update the high order scaled derivatives Adams integrators (phase 2).
     * <p>
     * The complete update of high order derivatives has a form similar to:
     * 
     * <pre>
     * r<sub>n+1</sub> = (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u + P<sup>-1</sup> A P r<sub>n</sub>
     * </pre>
     * 
     * this method computes the (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u part.
     * </p>
     * <p>
     * Phase 1 of the update must already have been performed.
     * </p>
     * 
     * @param start
     *        first order scaled derivatives at step start
     * @param end
     *        first order scaled derivatives at step end
     * @param highOrder
     *        high order scaled derivatives, will be modified
     *        (h<sup>2</sup>/2 y'', ... h<sup>k</sup>/k! y(k))
     * @see #updateHighOrderDerivativesPhase1(Array2DRowRealMatrix)
     */
    public void updateHighOrderDerivativesPhase2(final double[] start,
                                                 final double[] end,
                                                 final Array2DRowRealMatrix highOrder) {
        this.transformer.updateHighOrderDerivativesPhase2(start, end, highOrder);
    }

}
