/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:468:22/10/2015:Proper handling of ephemeris mode for analytical propagators
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.util.List;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class stores sequentially generated orbital parameters for
 * later retrieval.
 * 
 * <p>
 * Instances of this class are built and then must be fed with the results provided by
 * {@link fr.cnes.sirius.patrius.propagation.Propagator Propagator} objects configured in
 * {@link fr.cnes.sirius.patrius.propagation.Propagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is o, random access to any intermediate state of the orbit throughout
 * the propagation range is possible.
 * </p>
 */
public class AnalyticalIntegratedEphemeris extends AbstractPropagator implements BoundedPropagator {

    /** UID. */
    private static final long serialVersionUID = -2679532330842851649L;

    /** List of initial dates. */
    private final List<AbsoluteDate> initialDates;

    /** List of final dates. */
    private final List<AbsoluteDate> finalDates;

    /** List of initial state at initial date. */
    private final List<SpacecraftState> initialStates;

    /** Propagator. */
    private final Propagator propagator;

    /** True if propagation is forward, false if propagation is backward. */
    private final boolean isForward;

    /**
     * Constructor.
     * 
     * @param initialDatesIn
     *        list of initial dates
     * @param finalDatesIn
     *        list of final dates
     * @param initialStatesIn
     *        list of initial states at initial dates
     * @param propagatorIn
     *        propagator
     * @param attForcesProvider
     *        attitude provider for force computation
     * @param attEventsProvider
     *        attitude provider for events computation
     * @param isForwardIn
     *        true if propagation is forward, false if propagation is backward
     */
    public AnalyticalIntegratedEphemeris(final List<AbsoluteDate> initialDatesIn,
        final List<AbsoluteDate> finalDatesIn,
        final List<SpacecraftState> initialStatesIn, final Propagator propagatorIn,
        final AttitudeProvider attForcesProvider, final AttitudeProvider attEventsProvider,
        final boolean isForwardIn) {
        super(attForcesProvider, attEventsProvider);
        this.initialDates = initialDatesIn;
        this.finalDates = finalDatesIn;
        this.initialStates = initialStatesIn;
        this.propagator = propagatorIn;
        this.isForward = isForwardIn;
    }

    /**
     * Find ephemeris leg valid for provided date.
     * 
     * @param date
     *        a date
     * @return ephemeris leg valid for provided date
     * @throws PropagationException
     *         thrown if required date is outside of ephemeris range
     */
    private int getLeg(final AbsoluteDate date) throws PropagationException {

        // Get min and max dates
        AbsoluteDate minDate;
        AbsoluteDate maxDate;
        if (this.isForward) {
            minDate = this.initialDates.get(0);
            maxDate = this.finalDates.get(this.finalDates.size() - 1);
        } else {
            minDate = this.finalDates.get(this.initialDates.size() - 1);
            maxDate = this.initialDates.get(0);
        }
        if ((date.compareTo(minDate) < 0) || (date.compareTo(maxDate) > 0)) {
            // Date is outside of supported range
            throw new PropagationException(PatriusMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date, minDate, maxDate);
        }

        // Find propagator valid for provided date
        boolean found = false;
        int i = 0;
        while (!found) {
            // Loop until segment has been found
            if (this.isForward) {
                minDate = this.initialDates.get(i);
                maxDate = this.finalDates.get(i);
            } else {
                minDate = this.finalDates.get(i);
                maxDate = this.initialDates.get(i);
            }
            if (date.compareTo(minDate) >= 0 && date.compareTo(maxDate) <= 0) {
                found = true;
            }
            i++;
        }

        // Return result
        return i - 1;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {

        // Set proper initial state corresponding to ephemeris leg
        this.resetInitialState(this.initialStates.get(this.getLeg(target)));
        this.setStartDate(null);

        // Propagate
        return super.propagate(target);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState propagate(final AbsoluteDate start, final AbsoluteDate target) throws PropagationException {

        // Set proper initial state corresponding to initial ephemeris leg
        this.resetInitialState(this.initialStates.get(this.getLeg(start)));
        this.setStartDate(null);

        // Propagate
        return super.propagate(start, target);
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        this.propagator.resetInitialState(this.initialStates.get(this.getLeg(date)));
        return ((AbstractPropagator) this.propagator).propagateOrbit(date);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return this.initialDates.get(0);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return this.finalDates.get(this.finalDates.size() - 1);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return this.propagator.getFrame();
    }
}
