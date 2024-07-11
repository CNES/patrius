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
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.numerical;

import java.util.Locale;

import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

/**
 * Ephemeris step handler.
 * 
 * @version $Id: EphemerisStepHandler.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class EphemerisStepHandler implements StepHandler {

    /** Display ephemeris flag. */
    private final boolean displayEphemeris = false;

    /**
     * Constructor.
     */
    public EphemerisStepHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final StepInterpolator interpolator, final boolean isLast) {

        if (this.displayEphemeris) {
            final double time = interpolator.getCurrentTime();
            interpolator.setInterpolatedTime(time);
            final double[] state = interpolator.getInterpolatedState();
            System.out.println(String.format(Locale.US, "Ephemeris: t = %.1f, y = %.1f", time, state[0]));
        }
    }
}
