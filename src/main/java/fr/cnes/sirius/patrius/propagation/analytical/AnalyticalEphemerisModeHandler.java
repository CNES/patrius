/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:468:22/10/2015:Proper handling of ephemeris mode for analytical propagators
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.propagation.AnalyticalIntegratedEphemeris;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
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
 * ephemeris generation mode}. Once propagation is over, a {@link BoundedPropagator} can be built from the stored steps.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator
 * @author Luc Maisonobe
 */
public class AnalyticalEphemerisModeHandler implements PatriusStepHandler {

    /** UID. */
    private static final long serialVersionUID = -2554972295829753354L;

    /** List of initial dates. */
    private List<AbsoluteDate> initialDates;

    /** List of final dates. */
    private List<AbsoluteDate> finalDates;

    /** List of initial state at initial date. */
    private List<SpacecraftState> initialStates;

    /** Propagator. */
    private final Propagator propagator;

    /** True if propagation is forward, false if propagation is backward. */
    private boolean isForward = true;

    /** Attitude provider for forces computation. */
    private AttitudeProvider attitudeForcesProvider;

    /** Attitude provider for events computation. */
    private AttitudeProvider attitudeEventsProvider;

    /**
     * Constructor.
     * 
     * @param propagatorIn
     *        propagator
     * @param attForcesProvider
     *        attitude provider for force computation
     * @param attEventsProvider
     *        attitude provider for events computation
     */
    public AnalyticalEphemerisModeHandler(final Propagator propagatorIn,
        final AttitudeProvider attForcesProvider, final AttitudeProvider attEventsProvider) {
        this.propagator = propagatorIn;
        this.attitudeForcesProvider = attForcesProvider;
        this.attitudeEventsProvider = attEventsProvider;
    }

    /**
     * Get the generated ephemeris.
     * 
     * @return a new instance of the generated ephemeris
     */
    public BoundedPropagator getEphemeris() {
        return new AnalyticalIntegratedEphemeris(this.initialDates, this.finalDates,
            this.initialStates, this.propagator, this.attitudeForcesProvider, this.attitudeEventsProvider,
            this.isForward);
    }

    /**
     * Set attitude provider for forces computation.
     * 
     * @param attProvForces
     *        the attitude provider
     */
    public void setAttitudeProviderForces(final AttitudeProvider attProvForces) {
        this.attitudeForcesProvider = attProvForces;
    }

    /**
     * Set attitude provider for events computation.
     * 
     * @param attProvEvents
     *        the attitude provider
     */
    public void setAttitudeProviderEvents(final AttitudeProvider attProvEvents) {
        this.attitudeEventsProvider = attProvEvents;
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final PatriusStepInterpolator interpolator,
                           final boolean isLast) throws PropagationException {
        try {
            if (!isLast) {
                this.initialDates.add(interpolator.getCurrentDate());
            }
            this.initialStates.add(interpolator.getInterpolatedState());
            this.finalDates.add(interpolator.getCurrentDate());

        } catch (final PatriusException e) {
            throw new PropagationException(e, PatriusMessages.INTERNAL_ERROR);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Initialization
        this.initialDates = new ArrayList<AbsoluteDate>();
        this.finalDates = new ArrayList<AbsoluteDate>();
        this.initialStates = new ArrayList<SpacecraftState>();

        // Add initial propagation date and state
        this.initialDates.add(s0.getDate());

        this.isForward = t.compareTo(s0.getDate()) >= 0;
    }
}
