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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela;

import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class is a provider of potential coefficients for test purposes; the values of J, equatorial radius and mu are
 * taken from Stela.
 * </p>
 * 
 * 
 * @author SabatiniT
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class PotentialCoefficientsProviderTest implements PotentialCoefficientsProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 3486959496127653854L;

    @Override
    public double[] getJ(final boolean normalized, final int n) throws PatriusException {
        final double[] J = new double[16];
        J[2] = 0.10826261107488972E-02;
        J[3] = -0.25361508420093188E-05;
        J[4] = -0.16193635251753600E-05;
        J[5] = -0.22289177538077055E-06;
        J[6] = 0.54030052565874444E-06;
        J[7] = -0.36062769005142777E-06;
        J[8] = -0.20807048979064489E-06;
        J[9] = -0.11423641514139811E-06;
        J[10] = -0.23262031558643691E-06;
        J[11] = 0.24092047459748041E-06;
        J[12] = -0.21091044168889998E-06;
        J[13] = -0.21967959574907912E-06;
        J[14] = 0.16882822277732491E-06;
        J[15] = 0.15917867232150533E-09;

        final double[] K = new double[n + 1];
        for (int i = 0; i < n + 1; i++) {
            K[i] = J[i];
        }
        return K;
    }

    @Override
    public double[][] getC(final int n, final int m, final boolean normalized) throws PatriusException {
        // This method is not called: return null
        return null;
    }

    @Override
    public double[][] getS(final int n, final int m, final boolean normalized) throws PatriusException {
        // This method is not called: return null
        return null;
    }

    @Override
    public double getMu() {
        return 398600441449820.000;
    }

    @Override
    public double getAe() {
        return 6378136.46;
    }
}
