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
* VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParameterizable;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents a force modifying spacecraft motion.
 * 
 * <p>
 * Objects implementing this interface are intended to be added to a
 * {@link fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator numerical propagator} before the propagation
 * is started.
 * </p>
 * <p>
 * The propagator will call at each step the {@link #addContribution(SpacecraftState, TimeDerivativesEquations)} method.
 * The force model instance will extract all the state data it needs (date,position, velocity, frame, attitude, mass)
 * from the first parameter. From these state data, it will compute the perturbing acceleration. It will then add this
 * acceleration to the second parameter which will take thins contribution into account and will use the Gauss equations
 * to evaluate its impact on the global state derivative.
 * </p>
 * <p>
 * Force models which create discontinuous acceleration patterns (typically for maneuvers start/stop or solar eclipses
 * entry/exit) must provide one or more {@link fr.cnes.sirius.patrius.events.EventDetector events detectors}
 * to the propagator thanks to their {@link #getEventsDetectors()} method. This method is called once just before
 * propagation starts. The events states will be checked by the propagator to ensure accurate propagation and proper
 * events handling.
 * </p>
 * 
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
public interface ForceModel extends IParameterizable {

    /**
     * Compute the contribution of the force model to the perturbing
     * acceleration.
     * 
     * @param s
     *        current state information: date, kinematics, attitude
     * @param adder
     *        object where the contribution should be added
     * @exception PatriusException
     *            if some specific error occurs
     */
    void addContribution(SpacecraftState s, TimeDerivativesEquations adder) throws PatriusException;

    /**
     * Compute the acceleration due to the force.
     * 
     * @param s
     *        current state information: date, kinematics, attitude
     * @return acceleration in the {@link SpacecraftState#getFrame() SpacecraftState frame}
     * @exception PatriusException
     *            if some specific error occurs
     */
    Vector3D computeAcceleration(SpacecraftState s) throws PatriusException;

    /**
     * Get the discrete events related to the model.
     * 
     * @return array of events detectors or null if the model is not
     *         related to any discrete events
     */
    EventDetector[] getEventsDetectors();
    
    /**
     * This methods throws an exception if the user did not provide all the required data to perform model call on
     * provided range [start; end]. It is the responsibility of the model implementation to properly throw exceptions
     * (for example DragForce will throw an exception if solar activity data is missing in the range [start, end]).
     * @param start range start date
     * @param end range end date
     * @throws PatriusException thrown if some data is missing
     */
    void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException;
}
