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
 * @history created 12/09/2014
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.analytical;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Event at t = t1 or t2. g function depends on the state so that E1 may create or invalidate E2.
 * 
 * @version $Id: Detector2.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class Detector2 extends Detector {

    /** Default serial version ID */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param eventTime
     *        event time
     * @param action
     *        action
     * @param convergence
     *        convergence parameter
     */
    public Detector2(final AbsoluteDate eventTime, final Action action, final double convergence) {
        super(eventTime, action, convergence);
    }

    @Override
    public double g(final SpacecraftState s) {
        double dt = this.getEventTime().durationFrom(this.getT0());

        // Retropolation case
        dt = dt < 0 ? dt + 10 : dt;

        return s.getLM() * s.getKeplerianPeriod() / (2. * FastMath.PI) - dt;
    }

    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) {
        // Nothing is done
        // To make sure reset state has been called, we just add it to checked events list (although it's not an event)
        this.eventList.add(new ResetState(this.getClass().getSimpleName(), oldState.getDate(), this.convergence));
        return oldState;
    }
}
