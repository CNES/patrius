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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Interface summing up the contribution of several forces into orbit and mass derivatives.
 * 
 * <p>
 * The aim of this interface is to gather the contributions of various perturbing forces expressed as accelerations into
 * one set of time-derivatives of {@link fr.cnes.sirius.patrius.orbits.Orbit}. It implements Gauss equations for
 * different kind of parameters.
 * </p>
 * <p>
 * An implementation of this interface is automatically provided by
 * {@link fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator numerical propagators} to the various
 * {@link fr.cnes.sirius.patrius.forces.ForceModel force models}.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.forces.ForceModel
 * @see fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public interface TimeDerivativesEquations extends Serializable {

    /**
     * Initialize all derivatives to zero.
     * 
     * @param yDot
     *        reference to the array where to put the derivatives.
     * @param currentOrbit
     *        current orbit parameters
     * @exception PropagationException
     *            if the orbit evolve out of supported range
     */
    void initDerivatives(double[] yDot, Orbit currentOrbit) throws PropagationException;

    /**
     * Add the contribution of an acceleration expressed in the inertial frame
     * (it is important to make sure this acceleration is defined in the
     * same frame as the orbit) .
     * 
     * @param x
     *        acceleration along the X axis (m/s<sup>2</sup>)
     * @param y
     *        acceleration along the Y axis (m/s<sup>2</sup>)
     * @param z
     *        acceleration along the Z axis (m/s<sup>2</sup>)
     */
    void addXYZAcceleration(final double x, final double y, final double z);

    /**
     * Add the contribution of an acceleration expressed in some inertial frame.
     * 
     * @param gamma
     *        acceleration vector (m/s<sup>2</sup>)
     * @param frame
     *        frame in which acceleration is defined (must be an inertial frame)
     * @exception PatriusException
     *            if frame transforms cannot be computed
     */
    void addAcceleration(final Vector3D gamma, final Frame frame) throws PatriusException;

    /**
     * Add the contribution of the change rate (dX/dt) of the additional state.
     * 
     * @param name
     *        the additional state name
     * @param pDot
     *        the change rate (dX/dt)
     * @exception IllegalArgumentException
     *            if the mass flow-rate is positive
     */
    void addAdditionalStateDerivative(final String name, final double[] pDot);

}
