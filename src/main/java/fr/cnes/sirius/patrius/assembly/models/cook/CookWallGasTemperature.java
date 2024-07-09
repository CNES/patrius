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
 * @history creation 16/06/2016
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:600:16/06/2016:add Cook (Cn, Ct) models
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:705:07/12/2016: code factorisation in getWallGasTemperature()
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.cook;

import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Wall gas temperature following Cook model.
 * 
 * @concurrency thread-safe
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id$
 */
public class CookWallGasTemperature implements WallGasTemperatureProvider {

    /** Serial UID. */
    private static final long serialVersionUID = 8528862676079931911L;

    /** Atmosphere. */
    private final ExtendedAtmosphere atmosphere;

    /** Alpha coefficient. */
    private final AlphaProvider alphaProvider;

    /** Surface temperature square root (square root stored for GinsWallGasTemperature). */
    private final double sqrtSurfaceTemperature;

    /**
     * Constructor.
     * 
     * @param atmos
     *        atmosphere
     * @param alpha
     *        alpha coefficient
     * @param surfaceTemp
     *        surface temperature
     */
    public CookWallGasTemperature(final ExtendedAtmosphere atmos, final AlphaProvider alpha, final double surfaceTemp) {
        this.atmosphere = atmos;
        this.alphaProvider = alpha;
        this.sqrtSurfaceTemperature = MathLib.sqrt(surfaceTemp);
    }

    /** {@inheritDoc} */
    @Override
    public double getWallGasTemperature(final SpacecraftState state, final Vector3D relativeVelocity,
                                        final double theta) {

        try {
            // Atmospheric data
            final Vector3D pos = state.getPVCoordinates().getPosition();
            final AtmosphereData data = this.atmosphere.getData(state.getDate(), pos, state.getFrame());
            final double molarMass = data.getMeanAtomicMass() * Constants.AVOGADRO_CONSTANT
                * AtmosphereData.HYDROGEN_MASS;
            final double tAtmo = data.getLocalTemperature();

            // Temporary variables
            final double vrelnorm2 = relativeVelocity.getNormSq();
            final double r = MathLib.divide(Constants.PERFECT_GAS_CONSTANT, molarMass);
            final double s2Tatmo = theta > 0 ? vrelnorm2 / (2. * r) : tAtmo;
            final double alpha = this.alphaProvider.getAlpha(state);

            // Compute temperature
            final double sqrtRes = this.sqrtSurfaceTemperature + MathLib.sqrt(1. - alpha)
                * (MathLib.sqrt(s2Tatmo) - this.sqrtSurfaceTemperature);
            return sqrtRes * sqrtRes;

        } catch (final PatriusException e) {
            // Computation failed: catch and wrap exception as only Runtime can be sent
            throw new PatriusExceptionWrapper(e);
        }
    }
}
