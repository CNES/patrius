/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.JacobianParametersProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for spacecraft that are sensitive to radiation pressure forces.
 * 
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public interface RadiationSensitive extends Serializable, JacobianParametersProvider {

    /**
     * Compute the acceleration due to radiation pressure.
     * <p>
     * The computation includes all spacecraft specific characteristics like shape, area and coefficients.
     * </p>
     * 
     * @param state
     *        current state information: date, kinematics, attitude
     * @param flux
     *        radiation flux in the same inertial frame as spacecraft orbit
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s<sup>2</sup>)
     * @throws PatriusException
     *         if acceleration cannot be computed
     */
    Vector3D radiationPressureAcceleration(SpacecraftState state, Vector3D flux) throws PatriusException;

    /**
     * Compute acceleration derivatives with respect to additional parameters.
     * 
     * @param s
     *        spacecraft state
     * @param param
     *        the parameter with respect to which derivatives are required
     * @param dAccdParam
     *        acceleration derivatives with respect to additional parameters
     * @param satSunVector
     *        satellite to sun vector, expressed in the spacecraft frame
     * @exception PatriusException
     *            if derivatives cannot be computed
     */
    void addDSRPAccDParam(SpacecraftState s, Parameter param, double[] dAccdParam,
                          Vector3D satSunVector) throws PatriusException;

    /**
     * Compute acceleration derivatives with respect to state parameters.
     * 
     * @param s
     *        spacecraft state
     * @param dAccdPos
     *        acceleration derivatives with respect to position parameters
     * @param dAccdVel
     *        acceleration derivatives with respect to velocity parameters
     * @param satSunVector
     *        satellite to sun vector, expressed in the spacecraft frame
     * @exception PatriusException
     *            if derivatives cannot be computed
     */
    void addDSRPAccDState(SpacecraftState s, double[][] dAccdPos, double[][] dAccdVel,
                          Vector3D satSunVector) throws PatriusException;
}
