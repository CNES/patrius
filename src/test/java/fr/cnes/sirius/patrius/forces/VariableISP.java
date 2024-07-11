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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class is an implementation of the {@link IDependentVariable} interface and
 * it has been created for testing purposes.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class VariableISP implements IDependentVariable<SpacecraftState> {

    /** The initial date. */
    private final AbsoluteDate date0;

    /** The initial ISP. */
    private final double isp0;

    /** The ISP rate. */
    private final double rate;

    /**
     * Constructor.
     * 
     * @param initialDate
     *        the initial date
     * @param initialIsp
     *        the ISP at the initial date
     * @param ispRate
     *        the ISP rate
     */
    public VariableISP(final AbsoluteDate initialDate, final double initialIsp,
        final double ispRate) {
        this.date0 = initialDate;
        this.isp0 = initialIsp;
        this.rate = ispRate;
    }

    @Override
    public double value(final SpacecraftState s) {
        final double t = s.getDate().durationFrom(this.date0);
        return this.isp0 + this.rate * t;
    }
}
