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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
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

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * A detector to test additional state event detection.
 * This detector is based on the temperature and mass additional states equation.
 * It detects the Fusion date.
 * 
 * @author chabaudp
 * 
 * @version $Id: FusionStateDetector.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class FusionStateDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 4132490490096460865L;

    /** to know which added state to use */
    private final String addStateDataName;

    /** the reference which should be fusion temperature minus fusion mass */
    private final double reference;

    /**
     * constructor.
     * 
     * @param adsDataName
     *        the key to get additional state in the HashMap.
     * @param ref
     *        the reference for the switching function
     * 
     */
    public FusionStateDetector(final String adsDataName, final double ref) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.addStateDataName = adsDataName;
        this.reference = ref;
    }

    /**
     * The switching function is temperature minus mass plus reference.
     * Where the reference is fusion temperature minus fusion mass. {@inheritDoc}
     */
    @Override
    public double g(final SpacecraftState s) throws PatriusException {

        final double[] massAndTemp = s.getAdditionalStates().get(this.addStateDataName);
        if (massAndTemp != null) {
            final double g = massAndTemp[0] - massAndTemp[1] + this.reference;
            return g;
        }
        throw new PatriusRuntimeException("error", new Throwable());
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return Action.STOP;
    }

    /**
     * 
     * @see fr.cnes.sirius.patrius.propagation.events.AbstractDetector#shouldBeRemoved()
     */
    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    @Override
    public EventDetector copy() {
        return null;
    }
}
