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
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.sampling;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class wraps an object implementing {@link FixedStepHandler} into a {@link StepHandler}.
 * 
 * <p>
 * This wrapper allows to use fixed step handlers with general integrators which cannot guaranty their integration steps
 * will remain constant and therefore only accept general step handlers.
 * </p>
 * 
 * <p>
 * The stepsize used is selected at construction time. The {@link FixedStepHandler#handleStep handleStep} method of the
 * underlying {@link FixedStepHandler} object is called at normalized times. The normalized times can be influenced by
 * the {@link StepNormalizerMode} and {@link StepNormalizerBounds}.
 * </p>
 * 
 * <p>
 * There is no constraint on the integrator, it can use any time step it needs (time steps longer or shorter than the
 * fixed time step and non-integer ratios are all allowed).
 * </p>
 * 
 * <table border="1" align="center">
 * <tr BGCOLOR="#CCCCFF">
 * <td colspan=6><font size="+2">Examples (step size = 0.5)</font></td>
 * </tr>
 * <tr BGCOLOR="#EEEEFF">
 * <font size="+1">
 * <td>Start time</td>
 * <td>End time</td>
 * <td>Direction</td>
 * <td>{@link StepNormalizerMode Mode}</td>
 * <td>{@link StepNormalizerBounds Bounds}</td>
 * <td>Output</td></font>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>0.8, 1.3, 1.8, 2.3, 2.8</td>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>0.3, 0.8, 1.3, 1.8, 2.3, 2.8</td>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>0.8, 1.3, 1.8, 2.3, 2.8, 3.1</td>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>0.3, 0.8, 1.3, 1.8, 2.3, 2.8, 3.1</td>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>0.3, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.1</td>
 * </tr>
 * <tr>
 * <td>0.3</td>
 * <td>3.1</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>0.3, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.1</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>0.0</td>
 * <td>3.0</td>
 * <td>forward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>2.6, 2.1, 1.6, 1.1, 0.6</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>3.1, 2.6, 2.1, 1.6, 1.1, 0.6</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>2.6, 2.1, 1.6, 1.1, 0.6, 0.3</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>3.1, 2.6, 2.1, 1.6, 1.1, 0.6, 0.3</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>3.0, 2.5, 2.0, 1.5, 1.0, 0.5</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>3.1, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.3</td>
 * </tr>
 * <tr>
 * <td>3.1</td>
 * <td>0.3</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>3.1, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.3</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#INCREMENT INCREMENT}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#NEITHER NEITHER}</td>
 * <td>2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#FIRST FIRST}</td>
 * <td>3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#LAST LAST}</td>
 * <td>2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * <tr>
 * <td>3.0</td>
 * <td>0.0</td>
 * <td>backward</td>
 * <td>{@link StepNormalizerMode#MULTIPLES MULTIPLES}</td>
 * <td>{@link StepNormalizerBounds#BOTH BOTH}</td>
 * <td>3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0</td>
 * </tr>
 * </table>
 * 
 * @see StepHandler
 * @see FixedStepHandler
 * @see StepNormalizerMode
 * @see StepNormalizerBounds
 * @version $Id: StepNormalizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
@SuppressWarnings("PMD.NullAssignment")
public class StepNormalizer implements StepHandler {
    /** Fixed time step. */
    private double h;

    /** Underlying step handler. */
    private final FixedStepHandler handler;

    /** First step time. */
    private double firstTime;

    /** Last step time. */
    private double lastTime;

    /** Last state vector. */
    private double[] lastState;

    /** Last derivatives vector. */
    private double[] lastDerivatives;

    /** Integration direction indicator. */
    private boolean forward;

    /** The step normalizer bounds settings to use. */
    private final StepNormalizerBounds bounds;

    /** The step normalizer mode to use. */
    private final StepNormalizerMode mode;

    /**
     * Simple constructor. Uses {@link StepNormalizerMode#INCREMENT INCREMENT} mode, and
     * {@link StepNormalizerBounds#FIRST FIRST} bounds setting, for
     * backwards compatibility.
     * 
     * @param hIn
     *        fixed time step (sign is not used)
     * @param handlerIn
     *        fixed time step handler to wrap
     */
    public StepNormalizer(final double hIn, final FixedStepHandler handlerIn) {
        this(hIn, handlerIn, StepNormalizerMode.INCREMENT,
            StepNormalizerBounds.FIRST);
    }

    /**
     * Simple constructor. Uses {@link StepNormalizerBounds#FIRST FIRST} bounds setting.
     * 
     * @param hIn
     *        fixed time step (sign is not used)
     * @param handlerIn
     *        fixed time step handler to wrap
     * @param modeIn
     *        step normalizer mode to use
     * @since 3.0
     */
    public StepNormalizer(final double hIn, final FixedStepHandler handlerIn,
        final StepNormalizerMode modeIn) {
        this(hIn, handlerIn, modeIn, StepNormalizerBounds.FIRST);
    }

    /**
     * Simple constructor. Uses {@link StepNormalizerMode#INCREMENT INCREMENT} mode.
     * 
     * @param hIn
     *        fixed time step (sign is not used)
     * @param handlerIn
     *        fixed time step handler to wrap
     * @param boundsIn
     *        step normalizer bounds setting to use
     * @since 3.0
     */
    public StepNormalizer(final double hIn, final FixedStepHandler handlerIn,
        final StepNormalizerBounds boundsIn) {
        this(hIn, handlerIn, StepNormalizerMode.INCREMENT, boundsIn);
    }

    /**
     * Simple constructor.
     * 
     * @param hIn
     *        fixed time step (sign is not used)
     * @param handlerIn
     *        fixed time step handler to wrap
     * @param modeIn
     *        step normalizer mode to use
     * @param boundsIn
     *        step normalizer bounds setting to use
     * @since 3.0
     */
    public StepNormalizer(final double hIn, final FixedStepHandler handlerIn,
        final StepNormalizerMode modeIn,
        final StepNormalizerBounds boundsIn) {
        this.h = MathLib.abs(hIn);
        this.handler = handlerIn;
        this.mode = modeIn;
        this.bounds = boundsIn;
        this.firstTime = Double.NaN;
        this.lastTime = Double.NaN;
        this.lastState = null;
        this.lastDerivatives = null;
        this.forward = true;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {

        this.firstTime = Double.NaN;
        this.lastTime = Double.NaN;
        this.lastState = null;
        this.lastDerivatives = null;
        this.forward = true;

        // initialize the underlying handler
        this.handler.init(t0, y0, t);

    }

    /**
     * Handle the last accepted step
     * 
     * @param interpolator
     *        interpolator for the last accepted step. For
     *        efficiency purposes, the various integrators reuse the same
     *        object on each call, so if the instance wants to keep it across
     *        all calls (for example to provide at the end of the integration a
     *        continuous model valid throughout the integration range), it
     *        should build a local copy using the clone method and store this
     *        copy.
     * @param isLast
     *        true if the step is the last one
     * @exception MaxCountExceededException
     *            if the interpolator throws one because
     *            the number of functions evaluations is exceeded
     */
    @Override
    public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
        // The first time, update the last state with the start information.
        if (this.lastState == null) {
            this.firstTime = interpolator.getPreviousTime();
            this.lastTime = interpolator.getPreviousTime();
            interpolator.setInterpolatedTime(this.lastTime);
            this.lastState = interpolator.getInterpolatedState().clone();
            this.lastDerivatives = interpolator.getInterpolatedDerivatives().clone();

            // Take the integration direction into account.
            this.forward = interpolator.getCurrentTime() >= this.lastTime;
            if (!this.forward) {
                this.h = -this.h;
            }
        }

        // Calculate next normalized step time.
        double nextTime = (this.mode == StepNormalizerMode.INCREMENT) ?
            this.lastTime + this.h :
            (MathLib.floor(this.lastTime / this.h) + 1) * this.h;
        if (this.mode == StepNormalizerMode.MULTIPLES &&
            (nextTime == this.lastTime)) {
            nextTime += this.h;
        }

        // Process normalized steps as long as they are in the current step.
        boolean nextInStep = this.isNextInStep(nextTime, interpolator);
        while (nextInStep) {
            // Output the stored previous step.
            this.doNormalizedStep(false);

            // Store the next step as last step.
            this.storeStep(interpolator, nextTime);

            // Move on to the next step.
            nextTime += this.h;
            nextInStep = this.isNextInStep(nextTime, interpolator);
        }

        if (isLast) {
            // There will be no more steps. The stored one should be given to
            // the handler. We may have to output one more step. Only the last
            // one of those should be flagged as being the last.
            final boolean addLast = this.bounds.lastIncluded() &&
                this.lastTime != interpolator.getCurrentTime();
            this.doNormalizedStep(!addLast);
            if (addLast) {
                this.storeStep(interpolator, interpolator.getCurrentTime());
                this.doNormalizedStep(true);
            }
        }
    }

    /**
     * Returns a value indicating whether the next normalized time is in the
     * current step.
     * 
     * @param nextTime
     *        the next normalized time
     * @param interpolator
     *        interpolator for the last accepted step, to use to
     *        get the end time of the current step
     * @return value indicating whether the next normalized time is in the
     *         current step
     */
    private boolean isNextInStep(final double nextTime,
                                 final StepInterpolator interpolator) {
        return this.forward ?
            nextTime <= interpolator.getCurrentTime() :
            nextTime >= interpolator.getCurrentTime();
    }

    /**
     * Invokes the underlying step handler for the current normalized step.
     * 
     * @param isLast
     *        true if the step is the last one
     */
    private void doNormalizedStep(final boolean isLast) {
        if (!this.bounds.firstIncluded() && this.firstTime == this.lastTime) {
            return;
        }
        this.handler.handleStep(this.lastTime, this.lastState, this.lastDerivatives, isLast);
    }

    /**
     * Stores the interpolated information for the given time in the current
     * state.
     * 
     * @param interpolator
     *        interpolator for the last accepted step, to use to
     *        get the interpolated information
     * @param t
     *        the time for which to store the interpolated information
     * @exception MaxCountExceededException
     *            if the interpolator throws one because
     *            the number of functions evaluations is exceeded
     */
    private void storeStep(final StepInterpolator interpolator, final double t) {
        this.lastTime = t;
        interpolator.setInterpolatedTime(this.lastTime);
        System.arraycopy(interpolator.getInterpolatedState(), 0,
            this.lastState, 0, this.lastState.length);
        System.arraycopy(interpolator.getInterpolatedDerivatives(), 0,
            this.lastDerivatives, 0, this.lastDerivatives.length);
    }
}
