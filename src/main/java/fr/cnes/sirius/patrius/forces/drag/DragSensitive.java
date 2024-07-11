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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:284:01/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:268:30/04/2015:Aero drag and lift model
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:705:07/12/2016: corrected anomaly in dragAcceleration()
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.drag;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.JacobianParametersProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for spacecraft that are sensitive to atmospheric drag and lift forces.
 * 
 * @see DragForce
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public interface DragSensitive extends Serializable, JacobianParametersProvider {

    /**
     * Compute the acceleration due to drag and the lift.
     * <p>
     * The computation includes all spacecraft specific characteristics like shape, area and
     * coefficients.
     * </p>
     * 
     * @param state
     *        current state information: date, kinematics, attitude
     * @param density
     *        atmospheric density at spacecraft position
     * @param relativeVelocity
     *        relative velocity of atmosphere with respect to spacecraft,
     *        in the same inertial frame as spacecraft orbit (m/s)
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit
     *         (m/s<sup>2</sup>)
     * @throws PatriusException
     *         if acceleration cannot be computed
     */
    Vector3D dragAcceleration(SpacecraftState state, double density, Vector3D relativeVelocity)
            throws PatriusException;

    /**
     * Compute acceleration derivatives with respect to additional parameters (the ballistic
     * coefficient).
     * 
     * @param s
     *        spacecraft state
     * @param param
     *        parameter
     * @param density
     *        the atmospheric density value
     * @param relativeVelocity
     *        relative velocity of atmosphere with respect to spacecraft,
     *        in the same inertial frame as spacecraft orbit (m/s)
     * @param dAccdParam
     *        acceleration derivatives with respect to ballistic coefficient
     * @exception PatriusException
     *            if derivatives cannot be computed
     */
    void addDDragAccDParam(SpacecraftState s, final Parameter param, double density,
            Vector3D relativeVelocity, double[] dAccdParam) throws PatriusException;

    /**
     * Compute acceleration derivatives with respect to state parameters (position and velocity).
     * 
     * @param s
     *        spacecraft state
     * @param dAccdPos
     *        acceleration derivatives with respect to position parameters
     * @param dAccdVel
     *        acceleration derivatives with respect to velocity parameters
     * @param density
     *        the atmospheric density value
     * @param acceleration
     *        the spacecraft acceleration in the inertial frame
     * @param relativeVelocity
     *        relative velocity of atmosphere with respect to spacecraft,
     *        in the same inertial frame as spacecraft orbit (m/s)
     * @param computeGradientPosition
     *        true if partial derivatives with respect to position should be computed
     * @param computeGradientVelocity
     *        true if partial derivatives with respect to position should be computed
     * @throws PatriusException
     *         if derivatives cannot be computed
     */
    void addDDragAccDState(SpacecraftState s, double[][] dAccdPos, double[][] dAccdVel,
            double density, Vector3D acceleration, Vector3D relativeVelocity,
            final boolean computeGradientPosition, final boolean computeGradientVelocity)
            throws PatriusException;
    
    /**
     * Copy drag sensitive object using new assembly.
     * @param assembly new assembly
     * @return drag sensitive object with new assembly
     */
    DragSensitive copy(final Assembly assembly);
}
