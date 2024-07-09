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
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.numerical;

/**
 * Event at t = t1.
 * 
 * @version $Id: Detector1.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class Detector1 extends Detector {

    /**
     * E2 dependance with respect to E1.
     */
    public enum Dependance {

        /** No dependance. */
        NONE,

        /** E1 cancels E2. */
        CANCEL,

        /** E1 creates E2. */
        CREATE,

        /** E1 delays E2. */
        DELAY;
    }

    /** Dependance. */
    private final Dependance dependance;

    /**
     * Constructor.
     * 
     * @param eventTime
     *        event time
     * @param action
     *        action
     * @param dependance2
     *        dependance
     * @param convergence
     *        convergence parameter
     */
    public Detector1(final double eventTime, final Action action, final Dependance dependance2,
        final double convergence) {
        super(eventTime, action, convergence);
        this.dependance = dependance2;
    }

    @Override
    public double g(final double t, final double[] y) {
        return t - this.getEventTime();
    }

    @Override
    public void resetState(final double t, final double[] y) {

        switch (this.dependance) {
            case NONE:
                // Nothing is done
                break;
            case CANCEL:
                y[0] = y[0] - 10;
                break;
            case CREATE:
                y[0] = y[0] + 10.1;
                break;
            case DELAY:
                // Delays but stays in the step
                y[0] = y[0] - 0.2;
                break;
            default:
                break;
        }
    }

    /**
     * Get dependance E2/E1.
     * 
     * @return dependance E2/E1
     */
    public Dependance getDependance() {
        return this.dependance;
    }

    /**
     * TODO describe the changes with regard to the overriden method
     * 
     * @see fr.cnes.sirius.patrius.propagation.events.numerical.Detector#shouldBeRemoved()
     */
    @Override
    public boolean shouldBeRemoved() {
        // TODO Auto-generated method stub
        return false;
    }
}
