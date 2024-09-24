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
 * @history created 28/03/13
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Additional fake equation for test.
 * Making a constant variation of the electrical power function of time.
 * The constant is positive when PSO is between [0,180] deg and negative when PSO is between [180, 360] deg.
 * 
 * @author chabaudp
 * 
 * @version $Id: ElectricalPowerEquation.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class ElectricalPowerEquation implements AdditionalEquations {

     /** Serializable UID. */
    private static final long serialVersionUID = -9022422372213869630L;
    /** constant deviation. */
    private static final double CONSTANT_DEVIATION = 0.1;

    @Override
    public String getName() {
        return "electrical power";
    }

    @Override
    public void
            computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                             throws PatriusException {

        final CircularOrbit orbit = new CircularOrbit(s.getOrbit());
        final double pso = MathUtils.normalizeAngle(orbit.getAlphaM(), FastMath.PI);

        final double[] pDot = new double[1];
        pDot[0] = MathLib.signum(MathLib.sin(pso)) * CONSTANT_DEVIATION;
        adder.addAdditionalStateDerivative(this.getName(), pDot);

    }

    /**
     * Method to compute the min and max electrical power value reachable
     * from an initial PSO and Power state for the period and the mean motion of the orbit used
     * 
     * @param initPSO
     *        initial PSO
     * @param meanMotion
     *        delta PSO / delta t in rad/s
     * @param period
     *        orbital period in s
     * @param initialPower
     *        initial power given at the initial PSO
     * @return the min and the max power value between the power will evolve
     */
    public static double[] minMaxexpected(final double initPSO, final double meanMotion,
                                          final double period, final double initialPower) {

        // compute the duration from the initial PSO to PSO = 180 or 360
        final double deltaT = (((initPSO < FastMath.PI) ? FastMath.PI : MathUtils.TWO_PI) - initPSO) / meanMotion;

        // compute the first born which will be max if going to 180 or min if going to 360
        final double born1 = (initPSO < FastMath.PI) ? initialPower + CONSTANT_DEVIATION * deltaT : initialPower
            - CONSTANT_DEVIATION * deltaT;
        final double born2 = (initPSO < FastMath.PI) ? born1 - CONSTANT_DEVIATION * period / 2 : born1
            + CONSTANT_DEVIATION * period / 2;

        // return min and max
        final double min = (born1 < born2) ? born1 : born2;
        final double max = (born1 > born2) ? born1 : born2;
        final double[] minMax = { min, max };
        return minMax;
    }

    /**
     * 
     * Return the Pso corresponding to the Power.
     * 
     * @param power
     *        to look for the matching PSO
     * @param min
     *        the power for PSO = 0°
     * @param max
     *        the power for PSO = 180°
     * @param meanMotion
     *        delta PSO / delta t in rad/s
     * @param slope
     *        to specify if return the Pso when power is increasing or decreasing
     * @return at which Pso will be detected this Power
     * 
     */
    public static double getPsoReference(final double power, final double min, final double max,
                                         final double meanMotion, final int slope) {
        final double deltaPower = (slope == EventDetector.INCREASING) ? power - min : max - power;
        final double deltaT = deltaPower / CONSTANT_DEVIATION;
        return (slope == EventDetector.INCREASING) ? deltaT * meanMotion : FastMath.PI + deltaT * meanMotion;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
        return new double[] { 0. };
    }

    /** {@inheritDoc} */
    @Override
    public int getFirstOrderDimension() {
        // Unused
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getSecondOrderDimension() {
        // Unused
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public double[] buildAdditionalState(final double[] y,
            final double[] yDot) {
        // Unused
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractY(final double[] additionalState) {
        // Unused
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractYDot(final double[] additionalState) {
        // Unused
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        // Unused
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        // Unused
    }
}
