/**
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
 * 
 * 
 * @history created 18/03/2015
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling.multi;

import java.util.Map;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This class is copied from {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusStepNormalizer} and adapted to
 * multi propagation.
 * </p>
 * <p>
 * This class wraps an object implementing {@link MultiPatriusFixedStepHandler} into a {@link MultiPatriusStepHandler}.
 * 
 * <p>
 * It mirrors the <code>StepNormalizer</code> interface from <a href="http://commons.apache.org/math/">commons-math</a>
 * but provides a space-dynamics interface to the methods.
 * </p>
 * <p>
 * Modified to take into account propagation direction (in time). Lines 111 to 115 Cf A-1031.
 * </p>
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author maggioranic
 * 
 * @version $Id: MultiOrekitStepNormalizer.java 18109 2017-10-04 06:48:22Z bignon $
 * 
 * @since 3.0
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class MultiPatriusStepNormalizer implements MultiPatriusStepHandler {

    /** Serial UID. */
    private static final long serialVersionUID = 5083521519814863721L;

    /** Fixed time step. */
    private double h;

    /** Underlying step handler. */
    private final MultiPatriusFixedStepHandler handler;

    /** Last step date. */
    private AbsoluteDate lastDate;

    /** Last State vector. */
    private Map<String, SpacecraftState> lastStates;

    /** Integration direction indicator. */
    private boolean forward;

    /**
     * Simple constructor.
     * 
     * @param hIn
     *        fixed time step (sign is not used)
     * @param handlerIn
     *        fixed time step handler to wrap
     */
    public MultiPatriusStepNormalizer(final double hIn, final MultiPatriusFixedStepHandler handlerIn) {
        this.h = MathLib.abs(hIn);
        this.handler = handlerIn;
        this.lastDate = null;
        this.lastStates = null;
        this.forward = true;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
        this.lastDate = null;
        this.lastStates = null;
        this.forward = true;
        this.handler.init(s0, t);
    }

    /**
     * Handle the last accepted step.
     * 
     * @param interpolator
     *        interpolator for the last accepted step. For
     *        efficiency purposes, the various propagators reuse the same
     *        object on each call, so if the instance wants to keep it across
     *        all calls (for example to provide at the end of the propagation a
     *        continuous model valid throughout the propagation range), it
     *        should build a local copy using the clone method and store this
     *        copy.
     * @param isLast
     *        true if the step is the last one
     * @throws PropagationException
     *         this exception is propagated to the
     *         caller if the underlying user function triggers one
     */
    @Override
    public void handleStep(final MultiPatriusStepInterpolator interpolator,
                           final boolean isLast) throws PropagationException {
        try {
            if (this.lastStates == null) {
                // initialize lastState in the first step case

                this.lastDate = interpolator.getPreviousDate();
                interpolator.setInterpolatedDate(this.lastDate);
                this.lastStates = interpolator.getInterpolatedStates();

                // take the propagation direction into account
                this.forward = interpolator.getCurrentDate().compareTo(this.lastDate) >= 0;
                if (this.forward) {
                    this.h = MathLib.abs(this.h);
                } else {
                    this.h = -MathLib.abs(this.h);
                }

            }

            // use the interpolator to push fixed steps events to the underlying handler
            AbsoluteDate nextTime = this.lastDate.shiftedBy(this.h);
            boolean nextInStep = this.forward ^ (nextTime.compareTo(interpolator.getCurrentDate()) > 0);
            while (nextInStep) {
                interpolator.setInterpolatedDate(this.lastDate);
                // output the stored previous step
                this.handler.handleStep(this.lastStates, false);

                // store the next step
                this.lastDate = nextTime;
                interpolator.setInterpolatedDate(this.lastDate);
                this.lastStates = interpolator.getInterpolatedStates();

                // prepare next iteration
                nextTime = nextTime.shiftedBy(this.h);
                nextInStep = this.forward ^ (nextTime.compareTo(interpolator.getCurrentDate()) > 0);

            }

            if (isLast) {
                // there will be no more steps,
                // the stored one should be flagged as being the last
                this.handler.handleStep(this.lastStates, true);
            }

        } catch (final PatriusException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(oe);

        }
    }
}
