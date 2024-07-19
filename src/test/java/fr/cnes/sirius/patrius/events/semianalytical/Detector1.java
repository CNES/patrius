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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * (remove exception throw by SpacecraftState constructor)
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events.semianalytical;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Event at t = t1.
 * 
 * @version $Id$
 * 
 * @since 2.3
 */
public class Detector1 extends Detector {

    /** Serializable UID. */
    private static final long serialVersionUID = -2149551980233523596L;

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
    public Detector1(final AbsoluteDate eventTime, final Action action, final Dependance dependance2,
        final double convergence) {
        super(eventTime, action, convergence);
        this.dependance = dependance2;
    }

    @Override
    public double g(final SpacecraftState s) {
        return s.getDate().durationFrom(this.getEventTime());
    }

    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) {

        double newAnomaly = oldState.getLM();
        final double cst = 2. * FastMath.PI / oldState.getKeplerianPeriod();

        switch (this.dependance) {
            case NONE:
                // Nothing is done
                break;
            case CANCEL:
                newAnomaly -= cst * 10;
                break;
            case CREATE:
                newAnomaly += cst * 10.1;
                break;
            case DELAY:
                // Delays but stays in the step
                newAnomaly -= cst * 0.2;
                break;
            default:
                break;
        }

        final Orbit newOrbit = new KeplerianOrbit(oldState.getA(), oldState.getE(), oldState.getI(), 0, 0, newAnomaly,
            PositionAngle.MEAN, oldState.getFrame(), oldState.getDate(), oldState.getMu());
        return new SpacecraftState(newOrbit, null, null, oldState.getAdditionalStates());
    }

    /**
     * Get dependance E2/E1.
     * 
     * @return dependance E2/E1
     */
    public Dependance getDependance() {
        return this.dependance;
    }
}
