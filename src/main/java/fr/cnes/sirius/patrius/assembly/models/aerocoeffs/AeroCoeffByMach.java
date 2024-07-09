/**
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
 * @history creation 23/05/2018
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.aerocoeffs;

import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Aerodynamic coefficient function of the spacecraft Mach number. Aerodynamic coefficient is then
 * retrieved by linear interpolation of an <Mach number, aerodynamic coefficient> array.
 * 
 * @author Marie Capot
 * 
 * @version $Id$
 * 
 * @since 4.1
 * 
 */
public class AeroCoeffByMach extends AbstractAeroCoeff1D {

    /** Serial UID. */
    private static final long serialVersionUID = -6254942178964046393L;

    /** Atmosphere model. */
    private final Atmosphere atmosphere;

    /**
     * Constructor.
     * 
     * @param xVariables array of x variables (Mach number)
     * @param yVariables array of y variables (aerodynamic coefficient)
     * @param atmosphereIn atmosphere model
     */
    public AeroCoeffByMach(final double[] xVariables, final double[] yVariables,
        final Atmosphere atmosphereIn) {
        super(xVariables, yVariables);
        this.atmosphere = atmosphereIn;
    }

    /** {@inheritDoc} */
    @Override
    protected double computeXVariable(final SpacecraftState state) throws PatriusException {
        return machFromSpacecraftState(state, this.atmosphere);
    }

    /**
     * Computes the Mach number from the spacecraft state and an atmosphere model.
     * 
     * @param state the spacecraft state
     * @param atmosphere the atmosphere model
     * @return the mach number
     * @throws PatriusException thrown if conversion to ITRF failed
     */
    public static double
            machFromSpacecraftState(final SpacecraftState state, final Atmosphere atmosphere)
                                                                                             throws PatriusException {

        // Relative velocity with respect the atmosphere
        final Vector3D atmosphereVel = atmosphere.getVelocity(state.getDate(), state
            .getPVCoordinates().getPosition(), state.getFrame());
        final Vector3D relativeVel = atmosphereVel.subtract(state.getPVCoordinates().getVelocity());

        // Speed of sound
        final double vSound = atmosphere.getSpeedOfSound(state.getDate(), state.getPVCoordinates()
            .getPosition(), state.getFrame());

        // Mach number
        return relativeVel.getNorm() / vSound;
    }

    /** {@inheritDoc} */
    @Override
    public AerodynamicCoefficientType getType() {
        return AerodynamicCoefficientType.MACH;
    }
}
