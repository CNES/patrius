/**
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
 * 
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.13.3:FA:FA-283:27/03/2024:[PATRIUS] Methode filterEvent() non-wrappe dans OneSatEventDetectorWrapper
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Overload method shouldBeRemoved()
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.utils;

import java.util.Map;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.MultiAbstractDetector;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class allows to convert an {@link EventDetector} into a {@link MultiEventDetector}. The {@link EventDetector} is
 * associated with a single spacecraft identified by its ID.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @see fr.cnes.sirius.patrius.propagation.MultiPropagator#addEventDetector(EventDetector, String)
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class OneSatEventDetectorWrapper extends MultiAbstractDetector {

    /**
     * An event detector associated with a single spacecraft.
     */
    private final EventDetector monoSatDetector;

    /**
     * The ID of the spacecraft associated with the detector.
     */
    private final String identifier;

    /**
     * Simple constructor.
     * 
     * @param detector
     *        the event detector
     * @param satId
     *        the ID of the spacecraft associated with the detector
     */
    public OneSatEventDetectorWrapper(final EventDetector detector, final String satId) {
        super(detector.getSlopeSelection(), detector.getMaxCheckInterval(), detector.getThreshold());
        this.monoSatDetector = detector;
        this.identifier = satId;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final Map<String, SpacecraftState> statesMap, final AbsoluteDate date) throws PatriusException {
        this.monoSatDetector.init(statesMap.get(this.identifier), date);
    }

    /**
     * Compute the value of the switching function. This function is not meant to be used by external user.
     * Hence its visibility has been limited to package.
     * 
     * @param state
     *        the current states information: date, kinematics, attitudes for forces
     *        and events computation, and additional states for each states
     * @return value of the switching function
     * @exception PatriusException
     *            if some specific error occurs
     */
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        return this.monoSatDetector.g(state);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final Map<String, SpacecraftState> state) throws PatriusException {
        return this.monoSatDetector.g(state.get(this.identifier));
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final Map<String, SpacecraftState> state, final boolean increasing,
                                final boolean forward)
        throws PatriusException {
        return this.monoSatDetector.eventOccurred(state.get(this.identifier), increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.monoSatDetector.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState> resetStates(final Map<String, SpacecraftState> oldStates)
        throws PatriusException {
        final SpacecraftState newState = this.monoSatDetector.resetState(oldStates.get(this.identifier));
        // Reset the specific state
        oldStates.put(this.identifier, newState);
        return oldStates;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean filterEvent(final Map<String, SpacecraftState> states, final boolean increasing,
                               final boolean forward)
        throws PatriusException {
        return this.monoSatDetector.filterEvent(states.get(this.identifier), increasing, forward);
    }

    /**
     * Returns the ID of the spacecraft associated with the detector.
     * 
     * @return the ID of the spacecraft associated with the detector
     */
    public String getID() {
        return this.identifier;
    }
}
