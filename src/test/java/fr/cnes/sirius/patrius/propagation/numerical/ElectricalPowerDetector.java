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
 * @history created 28/03/13
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * A detector to test additional state event detection.
 * This detector is based on the electrical power additional states equation.
 * It detects the dates when power reaches a value.
 * 
 * @author chabaudp
 * 
 * @version $Id: ElectricalPowerDetector.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class ElectricalPowerDetector extends AbstractDetector {

    /** serial uid. */
    private static final long serialVersionUID = 1L;

    /** to know which added state to use */
    private final String addStateDataName;

    /** the value to detect. */
    private final double reference;

    /**
     * 
     * Constructor.
     * 
     * @param adsDataName
     *        the key to get additional state in the HashMap.
     * @param ref
     *        the reference for the switching function
     * @param slopeSelection
     *        INCREASE to detect when power is increasing or DECREASE to detect when power is decreasing
     */
    public ElectricalPowerDetector(final String adsDataName, final double ref, final int slopeSelection) {
        super(slopeSelection, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.addStateDataName = adsDataName;
        this.reference = ref;
    }

    /**
     * the switching function is power minus reference {@inheritDoc}
     */
    @Override
    public double g(final SpacecraftState s) throws PatriusException {
        final double[] electricalPower = s.getAdditionalStates().get(this.addStateDataName);
        if (electricalPower != null) {
            return s.getAdditionalStates().get(this.addStateDataName)[0] - this.reference;
        }
        throw new PatriusRuntimeException("error", new Throwable());
    }

    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return Action.STOP;
    }

    @Override
    public EventDetector copy() {
        return null;
    }
}