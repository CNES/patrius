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
 * @history creation 13/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * RadiativeModelTest class test.
 * 
 */
final class RadiativeTestModel extends Parameterizable implements RadiationSensitive {

    /** serialVersionUID */
    private static final long serialVersionUID = -7084381325470816250L;

    /** Parameter for absorption coefficient. */
    public static final Parameter ABSORPTION_COEFFICIENT = new Parameter("absorption coefficient", 0.);

    /** Parameter for reflection coefficient. */
    public static final Parameter SPECULAR_COEFFICIENT = new Parameter("specular reflection coefficient", 0.);

    /** Parameter for diffusion coefficient. */
    public static final Parameter DIFFUSION_COEFFICIENT = new Parameter("diffusion reflection coefficient", 0.);

    /** List of the parameters names. */
    private static final ArrayList<Parameter> parameters;
    static {
        parameters = new ArrayList<Parameter>();
        parameters.add(ABSORPTION_COEFFICIENT);
        parameters.add(SPECULAR_COEFFICIENT);
        parameters.add(DIFFUSION_COEFFICIENT);
    }

    @Override
    public Vector3D radiationPressureAcceleration(final SpacecraftState state,
                                                  final Vector3D flux) throws PatriusException {
        return new Vector3D(0., 0., 0.23561944901923448);
    }

    @Override
    public void addDSRPAccDParam(final SpacecraftState s, final Parameter param,
                                 final double[] dAccdParam, final Vector3D flux) throws PatriusException {
        if (ABSORPTION_COEFFICIENT.equals(param)) {
            dAccdParam[0] = 1.;
            dAccdParam[1] = 2.;
            dAccdParam[2] = 3.;
        } else {
            dAccdParam[0] = 0.;
            dAccdParam[1] = 0.;
            dAccdParam[2] = 0.;
        }
    }

    @Override
    public void addDSRPAccDState(final SpacecraftState s, final double[][] dAccdPos,
                                 final double[][] dAccdVel, final Vector3D flux) {
        dAccdPos[0][0] = 1.;
        dAccdPos[0][1] = 2.;
        dAccdPos[0][2] = 3.;
        dAccdPos[1][0] = 4.;
        dAccdPos[1][1] = 5.;
        dAccdPos[1][2] = 6.;
        dAccdPos[2][0] = 7.;
        dAccdPos[2][1] = 8.;
        dAccdPos[2][2] = 9.;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // return all parameters
        return this.getParameters();
    }

}
