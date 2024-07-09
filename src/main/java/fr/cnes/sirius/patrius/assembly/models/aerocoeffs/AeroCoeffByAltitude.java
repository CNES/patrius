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
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.aerocoeffs;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * Aerodynamic coefficient function of the spacecraft altitude. Aerodynamic coefficient is then
 * retrieved by linear interpolation of an <altitude, aerodynamic coefficient> array.
 * 
 * @author Marie Capot
 * 
 * @version $Id$
 * 
 * @since 4.1
 * 
 */
public class AeroCoeffByAltitude extends AbstractAeroCoeff1D {

    /** Serial UID. */
    private static final long serialVersionUID = -8711405951225638618L;

    /** Earth shape. */
    private final EllipsoidBodyShape earthShape;

    /**
     * Constructor.
     * 
     * @param xVariables array of x variables (altitude (m))
     * @param yVariables array of y variables (aerodynamic coefficient)
     * @param earthShapeIn Earth shape
     */
    public AeroCoeffByAltitude(final double[] xVariables, final double[] yVariables,
        final EllipsoidBodyShape earthShapeIn) {
        super(xVariables, yVariables);
        this.earthShape = earthShapeIn;
    }

    /** {@inheritDoc} */
    @Override
    protected double computeXVariable(final SpacecraftState state) {
        // Get altitude
        return state.getOrbit().getParameters()
            .getReentryParameters(this.earthShape.getEquatorialRadius(), this.earthShape.getFlattening())
            .getAltitude();
    }

    /** {@inheritDoc} */
    @Override
    public AerodynamicCoefficientType getType() {
        return AerodynamicCoefficientType.ALTITUDE;
    }
}
