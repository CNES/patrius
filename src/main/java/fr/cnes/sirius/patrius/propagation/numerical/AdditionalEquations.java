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
 * HISTORY
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Externalizable;
import java.io.Serializable;

import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.CowellIntegrator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface allows users to add their own differential equations to a numerical propagator.
 * 
 * <p>
 * In some cases users may need to integrate some problem-specific equations along with classical spacecraft equations
 * of motions. One example is optimal control in low thrust where adjoint parameters linked to the minimized hamiltonian
 * must be integrated. Another example is formation flying or rendez-vous which use the Clohessy-Whiltshire equations
 * for the relative motion.
 * </p>
 * <p>
 * This interface allows users to add such equations to a {@link NumericalPropagator numerical
 * propagator}. Users provide the equations as an implementation of this interface and register it to the propagator
 * thanks to its {@link NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. Several such objects
 * can be registered with each numerical propagator, but it is recommended to gather in the same object the sets of
 * parameters which equations can interact on each others states.
 * </p>
 * <p>
 * The additional parameters are gathered in a simple p array. The additional equations compute the pDot array, which is
 * the time-derivative of the p array. Since the additional parameters p may also have an influence on the equations of
 * motion themselves (for example an equation linked to a complex thrust model may induce an acceleration and a mass
 * change), the same {@link TimeDerivativesEquations time derivatives equations adder} already shared by all force
 * models to add their contributions is also provided to the additional equations implementation object. This means
 * these equations can be used as an additional force model if needed. If the additional parameters have no influence at
 * all on the spacecraft state, this adder can simply be ignored.
 * </p>
 * <p>
 * This interface is the numerical (read not already integrated) counterpart of the
 * {@link fr.cnes.sirius.patrius.propagation.AdditionalStateProvider} interface. It allows to append various additional
 * state parameters to any {@link NumericalPropagator numerical propagator}.
 * </p>
 * 
 * @see NumericalPropagator
 * @see fr.cnes.sirius.patrius.propagation.AdditionalStateProvider
 * @author Luc Maisonobe
 */
public interface AdditionalEquations extends Serializable, Externalizable {

    /**
     * Get the name of the additional state.
     * 
     * @return name of the additional state
     */
    String getName();

    /**
     * Compute the derivatives related to the additional state parameters.
     * 
     * @param s
     *        current state information: date, kinematics, attitude, additional states
     * @param adder
     *        object where the contribution of the additional parameters
     *        to the orbit evolution (accelerations) should be added
     * @exception PatriusException
     *            if some specific error occurs
     */
    void computeDerivatives(SpacecraftState s, TimeDerivativesEquations adder) throws PatriusException;

    /**
     * Compute the second derivatives related to the additional state parameters.
     * This method is only used by second order integrator such as {@link CowellIntegrator}.
     * 
     * @param s
     *        current state information: date, kinematics, attitude, additional states
     * @return second derivative
     * @exception PatriusException
     *            if some specific error occurs
     */
    double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException;

    /**
     * Returns the number of first order additional states.
     * This method is only used by second order integrator such as {@link CowellIntegrator}.
     * @return the number of first order additional states
     */
    int getFirstOrderDimension();

    /**
     * Returns the number of second order additional states.
     * This method is only used by second order integrator such as {@link CowellIntegrator}.
     * @return the number of second order additional states
     */
    int getSecondOrderDimension();

    /**
     * Build full first order additional state from second order y and yDot.
     * This method is only used by second order integrator such as {@link CowellIntegrator}.
     * @param y second order additional state y
     * @param yDot second order additional state derivative yDot
     * @return full first order additional state
     */
    double[] buildAdditionalState(final double[] y,
            final double[] yDot);

    /**
     * Retrieve second order additional state y from full first order additional state.
     * This method is only used by second order integrator such as {@link CowellIntegrator}.
     * @param additionalState full first order additional state
     * @return second order additional state y
     */
    double[] extractY(final double[] additionalState);

    /**
     * Retrieve second order additional state derivative yDot from full first order additional state.
     * This method is only used by second order integrator such as {@link CowellIntegrator}.
     * @param additionalState full first order additional state
     * @return second order additional state derivative yDot
     */
    double[] extractYDot(final double[] additionalState);
}
