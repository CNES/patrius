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
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events.numerical;

import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;

/**
 * 
 * @version $Id: ODE.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */

public class ODE implements FirstOrderDifferentialEquations {

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        yDot[0] = 1.;
    }
}
