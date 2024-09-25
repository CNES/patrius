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
 * @history created 28/03/13
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
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

    /** Serializable UID. */
    private static final long serialVersionUID = 7564579718692110528L;

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
