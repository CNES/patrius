/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff.cowell;

import java.io.Externalizable;

import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;

/**
 * Mapper for second order integrator state vector. This mapper maps a full first order state vs a second order state
 * (y, yDot). First order state is used for {@link FirstOrderIntegrator}, second order state is used for second order
 * integrator such as {@link CowellIntegrator}.
 * <p>
 * For example for PV coordinates integration:
 * </p>
 * <ul>
 * <li>Full first order state is (x, y, z, vx, vy, vz). This state is built from second order state and state
 * derivative using method {@link SecondOrderStateMapper#buildFullState(double[], double[])}</li>
 * <li>Second order state y is (x, y, z). This state is retrieved using method
 * {@link SecondOrderStateMapper#extractY(double[])}</li>
 * <li>Second order state derivative yDot is (vx, vy, vz). This state derivative is retrieved from first order state
 * vector using methode {@link SecondOrderStateMapper#extractYDot(double[])}</li>
 * </ul>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public interface SecondOrderStateMapper extends Externalizable {

    /**
     * Build full first order state from second order y and yDot.
     * @param y second order state y
     * @param yDot second order state derivative yDot
     * @return full first order state
     */
    double[] buildFullState(final double[] y,
            final double[] yDot);

    /**
     * Retrieve second order state y from full first order state.
     * @param fullState full first order state
     * @return second order state y
     */
    double[] extractY(final double[] fullState);

    /**
     * Retrieve second order state derivative yDot from full first order state.
     * @param fullState full first order state
     * @return second order state derivative yDot
     */
    double[] extractYDot(final double[] fullState);
}
