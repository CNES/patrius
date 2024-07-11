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
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EventDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1422:27/11/2017:correct PatriusStepNormalizer
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class wraps an object implementing {@link PatriusFixedStepHandler} into a {@link PatriusStepHandler}.
 * 
 * <p>
 * It mirrors the <code>StepNormalizer</code> interface from <a href="http://commons.apache.org/math/">commons-math</a>
 * but provides a space-dynamics interface to the methods.
 * </p>
 * <p>
 * Modified to take into account propagation direction (in time). Lines 111 to 115 Cf A-1031.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class PatriusStepNormalizer implements PatriusStepHandler {

    /** Serializable UID. */
    private static final long serialVersionUID = 6335110162884693078L;

    /** Fixed time step. */
    private double h;

    /** Underlying step handler. */
    private final PatriusFixedStepHandler handler;

    /** Last step date. */
    private AbsoluteDate lastDate;

    /** Last State vector. */
    private SpacecraftState lastState;

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
    public PatriusStepNormalizer(final double hIn, final PatriusFixedStepHandler handlerIn) {
        this.h = MathLib.abs(hIn);
        this.handler = handlerIn;
        this.lastDate = null;
        this.lastState = null;
        this.forward = true;
    }

    /**
     * Determines whether this handler needs dense output.
     * This handler needs dense output in order to provide data at
     * regularly spaced steps regardless of the steps the propagator
     * uses, so this method always returns true.
     * 
     * @return always true
     */
    public boolean requiresDenseOutput() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
        this.lastDate = null;
        this.lastState = null;
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
    public void handleStep(final PatriusStepInterpolator interpolator,
                           final boolean isLast) throws PropagationException {
        try {
            if (this.lastState == null) {
                // initialize lastState in the first step case

                this.lastDate = interpolator.getPreviousDate();
                interpolator.setInterpolatedDate(this.lastDate);
                this.lastState = interpolator.getInterpolatedState();
            }

            // take the propagation direction into account
            this.forward = interpolator.getCurrentDate().compareTo(this.lastDate) >= 0;
            if (!this.forward) {
                this.h = -MathLib.abs(this.h);
            } else {
                this.h = MathLib.abs(this.h);
            }

            // use the interpolator to push fixed steps events to the underlying handler
            AbsoluteDate nextTime = this.lastDate.shiftedBy(this.h);
            boolean nextInStep = this.forward ^ (nextTime.compareTo(interpolator.getCurrentDate()) > 0);
            while (nextInStep) {
                interpolator.setInterpolatedDate(this.lastDate);
                // output the stored previous step
                this.handler.handleStep(this.lastState, false);

                // store the next step
                this.lastDate = nextTime;
                interpolator.setInterpolatedDate(this.lastDate);
                this.lastState = interpolator.getInterpolatedState();

                // prepare next iteration
                nextTime = nextTime.shiftedBy(this.h);
                nextInStep = this.forward ^ (nextTime.compareTo(interpolator.getCurrentDate()) > 0);

            }

            if (isLast) {
                // there will be no more steps,
                // the stored one should be flagged as being the last
                this.handler.handleStep(this.lastState, true);
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
