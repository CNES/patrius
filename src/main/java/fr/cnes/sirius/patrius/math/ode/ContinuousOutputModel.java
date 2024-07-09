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
package fr.cnes.sirius.patrius.math.ode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class stores all information provided by an ODE integrator
 * during the integration process and build a continuous model of the
 * solution from this.
 * 
 * <p>
 * This class act as a step handler from the integrator point of view. It is called iteratively during the integration
 * process and stores a copy of all steps information in a sorted collection for later use. Once the integration process
 * is over, the user can use the {@link #setInterpolatedTime setInterpolatedTime} and {@link #getInterpolatedState
 * getInterpolatedState} to retrieve this information at any time. It is important to wait for the integration to be
 * over before attempting to call {@link #setInterpolatedTime setInterpolatedTime} because some internal variables are
 * set only once the last step has been handled.
 * </p>
 * 
 * <p>
 * This is useful for example if the main loop of the user application should remain independent from the integration
 * process or if one needs to mimic the behaviour of an analytical model despite a numerical model is used (i.e. one
 * needs the ability to get the model value at any time or to navigate through the data).
 * </p>
 * 
 * <p>
 * If problem modeling is done with several separate integration phases for contiguous intervals, the same
 * ContinuousOutputModel can be used as step handler for all integration phases as long as they are performed in order
 * and in the same direction. As an example, one can extrapolate the trajectory of a satellite with one model (i.e. one
 * set of differential equations) up to the beginning of a maneuver, use another more complex model including thrusters
 * modeling and accurate attitude control during the maneuver, and revert to the first model after the end of the
 * maneuver. If the same continuous output model handles the steps of all integration phases, the user do not need to
 * bother when the maneuver begins or ends, he has all the data available in a transparent manner.
 * </p>
 * 
 * <p>
 * An important feature of this class is that it implements the <code>Serializable</code> interface. This means that the
 * result of an integration can be serialized and reused later (if stored into a persistent medium like a filesystem or
 * a database) or elsewhere (if sent to another application). Only the result of the integration is stored, there is no
 * reference to the integrated problem by itself.
 * </p>
 * 
 * <p>
 * One should be aware that the amount of data stored in a ContinuousOutputModel instance can be important if the state
 * vector is large, if the integration interval is long or if the steps are small (which can result from small tolerance
 * settings in {@link fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator adaptive
 * step size integrators}).
 * </p>
 * 
 * @see StepHandler
 * @see StepInterpolator
 * @version $Id: ContinuousOutputModel.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public class ContinuousOutputModel
    implements StepHandler, Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = -1417964919405031606L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Convergence threshold. */
    private static final double THRESHOLD = 1e-6;

    /** Convergence threshold 2. */
    private static final double THRESHOLD2 = 1e-3;

    /** Initial integration time. */
    private double initialTime;

    /** Final integration time. */
    private double finalTime;

    /** Integration direction indicator. */
    private boolean forward;

    /** Current interpolator index. */
    private int index;

    /** Steps table. */
    private final List<StepInterpolator> steps;

    /**
     * Simple constructor.
     * Build an empty continuous output model.
     */
    public ContinuousOutputModel() {
        this.steps = new ArrayList<StepInterpolator>();
        this.initialTime = Double.NaN;
        this.finalTime = Double.NaN;
        this.forward = true;
        this.index = 0;
    }

    /**
     * Append another model at the end of the instance.
     * 
     * @param model
     *        model to add at the end of the instance
     * @exception MathIllegalArgumentException
     *            if the model to append is not
     *            compatible with the instance (dimension of the state vector,
     *            propagation direction, hole between the dates)
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     *            during step finalization
     */
    public void append(final ContinuousOutputModel model) {

        if (model.steps.isEmpty()) {
            // Nothing to append
            return;
        }

        if (this.steps.isEmpty()) {
            // Nothing to do
            this.initialTime = model.initialTime;
            this.forward = model.forward;
        } else {

            if (this.getInterpolatedState().length != model.getInterpolatedState().length) {
                // Exception
                throw new DimensionMismatchException(model.getInterpolatedState().length,
                    this.getInterpolatedState().length);
            }

            if (this.forward ^ model.forward) {
                // Exception
                throw new MathIllegalArgumentException(PatriusMessages.PROPAGATION_DIRECTION_MISMATCH);
            }

            final StepInterpolator lastInterpolator = this.steps.get(this.index);
            final double current = lastInterpolator.getCurrentTime();
            final double previous = lastInterpolator.getPreviousTime();
            final double step = current - previous;
            final double gap = model.getInitialTime() - current;
            if (MathLib.abs(gap) > THRESHOLD2 * MathLib.abs(step)) {
                // Exception
                throw new MathIllegalArgumentException(PatriusMessages.HOLE_BETWEEN_MODELS_TIME_RANGES,
                    MathLib.abs(gap));
            }

        }

        for (final StepInterpolator interpolator : model.steps) {
            this.steps.add(interpolator.copy());
        }

        this.index = this.steps.size() - 1;
        this.finalTime = (this.steps.get(this.index)).getCurrentTime();

    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        this.initialTime = Double.NaN;
        this.finalTime = Double.NaN;
        this.forward = true;
        this.index = 0;
        this.steps.clear();
    }

    /**
     * Handle the last accepted step.
     * A copy of the information provided by the last step is stored in
     * the instance for later use.
     * 
     * @param interpolator
     *        interpolator for the last accepted step.
     * @param isLast
     *        true if the step is the last one
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     *            during step finalization
     */
    @Override
    public void handleStep(final StepInterpolator interpolator, final boolean isLast) {

        if (this.steps.isEmpty()) {
            this.initialTime = interpolator.getPreviousTime();
            this.forward = interpolator.isForward();
        }

        this.steps.add(interpolator.copy());

        if (isLast) {
            this.finalTime = interpolator.getCurrentTime();
            this.index = this.steps.size() - 1;
        }

    }

    /**
     * Get the initial integration time.
     * 
     * @return initial integration time
     */
    public double getInitialTime() {
        return this.initialTime;
    }

    /**
     * Get the final integration time.
     * 
     * @return final integration time
     */
    public double getFinalTime() {
        return this.finalTime;
    }

    /**
     * Get the time of the interpolated point.
     * If {@link #setInterpolatedTime} has not been called, it returns
     * the final integration time.
     * 
     * @return interpolation point time
     */
    public double getInterpolatedTime() {
        return this.steps.get(this.index).getInterpolatedTime();
    }

    /**
     * Set the time of the interpolated point.
     * <p>
     * This method should <strong>not</strong> be called before the integration is over because some internal variables
     * are set only once the last step has been handled.
     * </p>
     * <p>
     * Setting the time outside of the integration interval is now allowed (it was not allowed up to version 5.9 of
     * Mantissa), but should be used with care since the accuracy of the interpolator will probably be very poor far
     * from this interval. This allowance has been added to simplify implementation of search algorithms near the
     * interval endpoints.
     * </p>
     * 
     * @param time
     *        time of the interpolated point
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public void setInterpolatedTime(final double time) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check

        // initialize the search with the complete steps table
        int iMin = 0;
        final StepInterpolator sMin = this.steps.get(iMin);

        // handle points outside of the integration interval
        // or in the first and last step
        if (this.locatePoint(time, sMin) <= 0) {
            this.index = iMin;
            sMin.setInterpolatedTime(time);
            return;
        }
        int iMax = this.steps.size() - 1;
        final StepInterpolator sMax = this.steps.get(iMax);
        if (this.locatePoint(time, sMax) >= 0) {
            this.index = iMax;
            sMax.setInterpolatedTime(time);
            return;
        }

        // Min and max
        double tMin = HALF * (sMin.getPreviousTime() + sMin.getCurrentTime());
        double tMax = HALF * (sMax.getPreviousTime() + sMax.getCurrentTime());

        // reduction of the table slice size
        while (iMax - iMin > 5) {

            // use the last estimated index as the splitting index
            final StepInterpolator si = this.steps.get(this.index);
            final int location = this.locatePoint(time, si);
            if (location < 0) {
                iMax = this.index;
                tMax = HALF * (si.getPreviousTime() + si.getCurrentTime());
            } else if (location > 0) {
                iMin = this.index;
                tMin = HALF * (si.getPreviousTime() + si.getCurrentTime());
            } else {
                // we have found the target step, no need to continue searching
                si.setInterpolatedTime(time);
                return;
            }

            // compute a new estimate of the index in the reduced table slice
            final int iMed = (iMin + iMax) / 2;
            final StepInterpolator sMed = this.steps.get(iMed);
            final double tMed = 0.5 * (sMed.getPreviousTime() + sMed.getCurrentTime());

            if ((MathLib.abs(tMed - tMin) < THRESHOLD) || (MathLib.abs(tMax - tMed) < THRESHOLD)) {
                // too close to the bounds, we estimate using a simple dichotomy
                this.index = iMed;
            } else {
                // estimate the index using a reverse quadratic polynom
                // (reverse means we have i = P(t), thus allowing to simply
                // compute index = P(time) rather than solving a quadratic equation)
                final double d12 = tMax - tMed;
                final double d23 = tMed - tMin;
                final double d13 = tMax - tMin;
                final double dt1 = time - tMax;
                final double dt2 = time - tMed;
                final double dt3 = time - tMin;
                final double iLagrange = ((dt2 * dt3 * d23) * iMax -
                    (dt1 * dt3 * d13) * iMed +
                    (dt1 * dt2 * d12) * iMin) /
                    (d12 * d23 * d13);
                this.index = (int) MathLib.rint(iLagrange);
            }

            // force the next size reduction to be at least one tenth
            final int low = MathLib.max(iMin + 1, (9 * iMin + iMax) / 10);
            final int high = MathLib.min(iMax - 1, (iMin + 9 * iMax) / 10);
            if (this.index < low) {
                this.index = low;
            } else if (this.index > high) {
                this.index = high;
            }

        }

        // now the table slice is very small, we perform an iterative search
        this.index = iMin;
        while ((this.index <= iMax) && (this.locatePoint(time, this.steps.get(this.index)) > 0)) {
            ++this.index;
        }

        this.steps.get(this.index).setInterpolatedTime(time);

    }

    /**
     * Get the state vector of the interpolated point.
     * 
     * @return state vector at time {@link #getInterpolatedTime}
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     */
    public double[] getInterpolatedState() {
        return this.steps.get(this.index).getInterpolatedState();
    }

    /**
     * Compare a step interval and a double.
     * 
     * @param time
     *        point to locate
     * @param interval
     *        step interval
     * @return -1 if the double is before the interval, 0 if it is in
     *         the interval, and +1 if it is after the interval, according to
     *         the interval direction
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private int locatePoint(final double time, final StepInterpolator interval) {
        // CHECKSTYLE: resume ReturnCount check
        if (this.forward) {
            // forward integration
            if (time < interval.getPreviousTime()) {
                // before interval
                return -1;
            } else if (time > interval.getCurrentTime()) {
                // after interval
                return +1;
            } else {
                // in interval
                return 0;
            }
        }
        // backward integration
        if (time > interval.getPreviousTime()) {
            // before interval
            return -1;
        } else if (time < interval.getCurrentTime()) {
            // after interval
            return +1;
        } else {
            // in interval
            return 0;
        }
    }

}
