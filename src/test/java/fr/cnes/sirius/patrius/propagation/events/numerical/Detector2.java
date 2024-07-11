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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.numerical;

/**
 * Event at t = t1 or t2. g function depends on the state so that E1 may create or invalidate E2.
 * 
 * @version $Id: Detector2.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class Detector2 extends Detector {

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
    public Detector2(final double eventTime, final Action action, final double convergence) {
        super(eventTime, action, convergence);
    }

    @Override
    public double g(final double t, final double[] y) {
        return y[0] - (this.getEventTime());
    }

    @Override
    public void resetState(final double t, final double[] y) {
        // Nothing is done
        // To make sure reset state has been called, we just add it to checked events list (although it's not an event)
        this.eventList.add(new ResetState(this.getClass().getSimpleName(), t, y[0], this.convergence));
    }

    /**
     * 
     * @see fr.cnes.sirius.patrius.propagation.events.numerical.Detector#shouldBeRemoved()
     */
    @Override
    public boolean shouldBeRemoved() {
        return false;
    }
}
