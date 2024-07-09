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
 * @history created 12/09/2014
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 *  VERSION::DM:226:12/09/2014: problem with event detections.
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.analytical;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Event at t = t1 or t = t2. Passive detector.
 * 
 * @version $Id: Detector3.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */

public class Detector3 extends Detector {

    /** Default serial version ID */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param eventTime
     *        event time
     * @param convergence
     *        convergence parameter
     */
    public Detector3(final AbsoluteDate eventTime, final double convergence) {
        super(eventTime, Action.CONTINUE, convergence);
    }

    @Override
    public double g(final SpacecraftState s) {
        return s.getDate().durationFrom(this.getEventTime());
    }
}
