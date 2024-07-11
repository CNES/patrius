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
 * @history creation 16/06/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2447:27/05/2020:Mathlib.divide() incomplète 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:01/08/2016:add wall property
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.cook;

import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Alpha (energy accomodation coefficient) following Cook model.
 * 
 * @concurrency thread-safe
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id$
 */
public class AlphaCookModel implements AlphaProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -898725635710341361L;

    /** Atmosphere. */
    private final ExtendedAtmosphere atmosphere;

    /** k. */
    private final double k;

    /** Wall gas molar mass. */
    private final double wallGasMolarMass;

    /**
     * Constructor.
     * 
     * @param atmos
     *        atmosphere
     * @param kValue
     *        k
     * @param wallGasMolarMassValue
     *        wallGasMolarMass
     */
    public AlphaCookModel(final ExtendedAtmosphere atmos, final double kValue, final double wallGasMolarMassValue) {
        this.atmosphere = atmos;
        this.k = kValue;
        this.wallGasMolarMass = wallGasMolarMassValue;
    }

    /** {@inheritDoc} */
    @Override
    public double getAlpha(final SpacecraftState state) {
        try {
            final AtmosphereData data =
                this.atmosphere.getData(state.getDate(), state.getPVCoordinates().getPosition(),
                    state.getFrame());
            final double molarMass = data.getMeanAtomicMass() * Constants.AVOGADRO_CONSTANT
                * AtmosphereData.HYDROGEN_MASS;
            final double mu = wallGasMolarMass == 0 ? 1. : MathLib.min(
                    MathLib.divide(molarMass, this.wallGasMolarMass), 1.);
            final double mup1 = mu + 1.;
            return this.k * mu / (mup1 * mup1);

        } catch (final PatriusException e) {
            // Computation failed: catch and wrap exception as only Runtime can be sent
            throw new PatriusExceptionWrapper(e);
        }
    }
}
