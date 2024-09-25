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
 * @history creation 12/06/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.JacobianParametersProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * rediffused radiative pressure interface
 * 
 * @author ClaudeD
 * 
 * @version $Id: RediffusedRadiationSensitive.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public interface RediffusedRadiationSensitive extends JacobianParametersProvider {

    /**
     * rediffused radiative pressure acceleration
     * 
     * @param state
     *        Spacecraft state
     * @param flux
     *        elemantary fluxes
     * @return acceleration
     * @throws PatriusException
     *         OREKIT exception
     * 
     * @since 1.2
     */
    Vector3D rediffusedRadiationPressureAcceleration(final SpacecraftState state,
                                                     final ElementaryFlux[] flux)
        throws PatriusException;

    /**
     * Compute acceleration derivatives.
     * 
     * @param s
     *        Spacecraft state.
     * @param dAccdPos
     *        acceleration derivatives with respect to position
     * @param dAccdVel
     *        acceleration derivatives with respect to velocity
     * @throws PatriusException
     *         OREKIT exception
     * 
     * @since 1.2
     */
    void addDAccDStateRediffusedRadiativePressure(final SpacecraftState s, final double[][] dAccdPos,
                                                  final double[][] dAccdVel)
        throws PatriusException;

    /**
     * Compute acceleration derivatives.
     * 
     * @param s
     *        Spacecraft state.
     * @param param
     *        name of the parameter with respect to which derivatives are required
     * @param dAccdParam
     *        acceleration derivatives with respect to specified parameters
     * @throws PatriusException
     *         OREKIT exception
     * 
     * @since 1.2
     */
    void addDAccDParamRediffusedRadiativePressure(final SpacecraftState s, final Parameter param,
                                                  final double[] dAccdParam)
        throws PatriusException;

    /**
     * albedo getter
     * 
     * @return calculation indicator of the albedo force
     */
    @SuppressWarnings("PMD.BooleanGetMethodName")
    boolean getFlagAlbedo();

    /**
     * infrared getter
     * 
     * @return calculation indicator of the infrared force
     */
    @SuppressWarnings("PMD.BooleanGetMethodName")
    boolean getFlagIr();

    /** derivatives initialisation */
    void initDerivatives();

    /**
     * K0 albedo getter
     * 
     * @return albedo global multiplicative factor
     */
    Parameter getK0Albedo();

    /**
     * K0 infrared getter
     * 
     * @return the infrared global multiplicative factor
     */
    Parameter getK0Ir();

    /**
     * assembly getter
     * 
     * @return assembly
     */
    Assembly getAssembly();

}
