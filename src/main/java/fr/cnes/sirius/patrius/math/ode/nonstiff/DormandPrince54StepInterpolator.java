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
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.EquationsMapper;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such

/**
 * This class represents an interpolator over the last step during an
 * ODE integration for the 5(4) Dormand-Prince integrator.
 * 
 * @see DormandPrince54Integrator
 * 
 * @version $Id: DormandPrince54StepInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

// CHECKSTYLE: stop MagicNumber check
@SuppressWarnings("PMD.NullAssignment")
class DormandPrince54StepInterpolator
    extends RungeKuttaStepInterpolator {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Last row of the Butcher-array internal weights, element 0. */
    private static final double A70 = 35.0 / 384.0;

    // element 1 is zero, so it is neither stored nor used

    /** Last row of the Butcher-array internal weights, element 2. */
    private static final double A72 = 500.0 / 1113.0;

    /** Last row of the Butcher-array internal weights, element 3. */
    private static final double A73 = 125.0 / 192.0;

    /** Last row of the Butcher-array internal weights, element 4. */
    private static final double A74 = -2187.0 / 6784.0;

    /** Last row of the Butcher-array internal weights, element 5. */
    private static final double A75 = 11.0 / 84.0;

    /** Shampine (1986) Dense output, element 0. */
    private static final double D0 = -12715105075.0 / 11282082432.0;

    // element 1 is zero, so it is neither stored nor used

    /** Shampine (1986) Dense output, element 2. */
    private static final double D2 = 87487479700.0 / 32700410799.0;

    /** Shampine (1986) Dense output, element 3. */
    private static final double D3 = -10690763975.0 / 1880347072.0;

    /** Shampine (1986) Dense output, element 4. */
    private static final double D4 = 701980252875.0 / 199316789632.0;

    /** Shampine (1986) Dense output, element 5. */
    private static final double D5 = -1453857185.0 / 822651844.0;

    /** Shampine (1986) Dense output, element 6. */
    private static final double D6 = 69997945.0 / 29380423.0;

     /** Serializable UID. */
    private static final long serialVersionUID = 20111120L;

    /** First vector for interpolation. */
    private double[] v1;

    /** Second vector for interpolation. */
    private double[] v2;

    /** Third vector for interpolation. */
    private double[] v3;

    /** Fourth vector for interpolation. */
    private double[] v4;

    /** Initialization indicator for the interpolation vectors. */
    private boolean vectorsInitialized;

    /**
     * Simple constructor.
     * This constructor builds an instance that is not usable yet, the {@link #reinitialize} method should be called
     * before using the
     * instance in order to initialize the internal arrays. This
     * constructor is used only in order to delay the initialization in
     * some cases. The {@link EmbeddedRungeKuttaIntegrator} uses the
     * prototyping design pattern to create the step interpolators by
     * cloning an uninitialized model and latter initializing the copy.
     */
    public DormandPrince54StepInterpolator() {
        super();
        this.v1 = null;
        this.v2 = null;
        this.v3 = null;
        this.v4 = null;
        this.vectorsInitialized = false;
    }

    /**
     * Copy constructor.
     * 
     * @param interpolator
     *        interpolator to copy from. The copy is a deep
     *        copy: its arrays are separated from the original arrays of the
     *        instance
     */
    public DormandPrince54StepInterpolator(final DormandPrince54StepInterpolator interpolator) {

        super(interpolator);

        if (interpolator.v1 == null) {

            this.v1 = null;
            this.v2 = null;
            this.v3 = null;
            this.v4 = null;
            this.vectorsInitialized = false;

        } else {

            this.v1 = interpolator.v1.clone();
            this.v2 = interpolator.v2.clone();
            this.v3 = interpolator.v3.clone();
            this.v4 = interpolator.v4.clone();
            this.vectorsInitialized = interpolator.vectorsInitialized;

        }

    }

    /** {@inheritDoc} */
    @Override
    protected StepInterpolator doCopy() {
        return new DormandPrince54StepInterpolator(this);
    }

    /** {@inheritDoc} */
    @Override
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
            public
            void reinitialize(final AbstractIntegrator integrator,
                              final double[] y, final double[][] yDotK, final boolean forward,
                              final EquationsMapper primaryMapper,
                              final EquationsMapper[] secondaryMappers) {
        // CHECKSTYLE: resume IllegalType check
        super.reinitialize(integrator, y, yDotK, forward, primaryMapper, secondaryMappers);
        this.v1 = null;
        this.v2 = null;
        this.v3 = null;
        this.v4 = null;
        this.vectorsInitialized = false;
    }

    /** {@inheritDoc} */
    @Override
    public void storeTime(final double t) {
        super.storeTime(t);
        this.vectorsInitialized = false;
    }

    /** {@inheritDoc} */
    @Override
    protected void computeInterpolatedStateAndDerivatives(final double theta,
                                                          final double oneMinusThetaH) {

        if (!this.vectorsInitialized) {

            if (this.v1 == null) {
                this.v1 = new double[this.interpolatedState.length];
                this.v2 = new double[this.interpolatedState.length];
                this.v3 = new double[this.interpolatedState.length];
                this.v4 = new double[this.interpolatedState.length];
            }

            // no step finalization is needed for this interpolator

            // we need to compute the interpolation vectors for this time step
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                final double yDot0 = this.yDotK[0][i];
                final double yDot2 = this.yDotK[2][i];
                final double yDot3 = this.yDotK[3][i];
                final double yDot4 = this.yDotK[4][i];
                final double yDot5 = this.yDotK[5][i];
                final double yDot6 = this.yDotK[6][i];
                this.v1[i] = A70 * yDot0 + A72 * yDot2 + A73 * yDot3 + A74 * yDot4 + A75 * yDot5;
                this.v2[i] = yDot0 - this.v1[i];
                this.v3[i] = this.v1[i] - this.v2[i] - yDot6;
                this.v4[i] = D0 * yDot0 + D2 * yDot2 + D3 * yDot3 + D4 * yDot4 + D5 * yDot5 + D6 * yDot6;
            }

            this.vectorsInitialized = true;

        }

        // interpolate
        final double eta = 1 - theta;
        final double twoTheta = 2 * theta;
        final double dot2 = 1 - twoTheta;
        final double dot3 = theta * (2 - 3 * theta);
        final double dot4 = twoTheta * (1 + theta * (twoTheta - 3));
        if ((this.previousState != null) && (theta <= HALF)) {
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                this.interpolatedState[i] =
                    this.previousState[i] + theta * this.h
                        * (this.v1[i] + eta * (this.v2[i] + theta * (this.v3[i] + eta * this.v4[i])));
                this.interpolatedDerivatives[i] =
                    this.v1[i] + dot2 * this.v2[i] + dot3 * this.v3[i] + dot4 * this.v4[i];
            }
        } else {
            for (int i = 0; i < this.interpolatedState.length; ++i) {
                this.interpolatedState[i] =
                    this.currentState[i] - oneMinusThetaH
                        * (this.v1[i] - theta * (this.v2[i] + theta * (this.v3[i] + eta * this.v4[i])));
                this.interpolatedDerivatives[i] =
                    this.v1[i] + dot2 * this.v2[i] + dot3 * this.v3[i] + dot4 * this.v4[i];
            }
        }

    }

    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume CommentRatio check
}
