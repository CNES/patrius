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
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Custom additional state : mass and temperature, the latter influencing the former after a "fusion" temperature.
 * HISTORY
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
class TemperatureAndMassEquationsLinTri implements AdditionalEquations {

    /** Default fusion temperature. */
    public static final double TFUSION = 300;

    /** Equation name. */
    public static final String KEY = "TEMP_AND_MASS";

    /** SerialUID. */
    private static final long serialVersionUID = 1L;

    private final double startTemp;

    private final AbsoluteDate startDate;

    public TemperatureAndMassEquationsLinTri(final double startingTemp, final double startingMass,
        final AbsoluteDate startingDate) {
        this.startTemp = startingTemp;
        this.startDate = startingDate;
    }

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void
            computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                             throws PatriusException {

        // current value of the additional parameters
        final double[] p = s.getAdditionalState(this.getName());
        // derivatives of the additional parameters
        final double[] pDot = new double[p.length];

        final double temperature = p[0];
        if (temperature < TFUSION) {
            // temperature's rising
            pDot[0] = 0.9;
            pDot[1] = 0;
        } else {
            final AbsoluteDate fusDate = this.startDate.shiftedBy(TemperatureAndMassEquationsLinTri
                .referenceFusionStart(this.startTemp));
            final double delta = s.getDate().durationFrom(fusDate);
            // temperature is constant, but mass decreases (it's "melting")
            pDot[0] = 0;
            pDot[1] = -0.8 + 0.4 * MathLib.sin(delta);
            // adder.addMassDerivative(pDot[1]);
        }
        adder.addAdditionalStateDerivative(this.getName(), pDot);

        // System.out.printf("%s - %f (%f) %f (%f)\n", s.getDate().toString(), p[0], pDot[0], p[1], pDot[1]);
    }

    public static double referenceTemp(final double startTemp, final double duration) {
        double rez = 0.;
        final double riseTemp = startTemp + 0.9 * duration;
        if (riseTemp > TFUSION) {
            rez = TFUSION;
        } else {
            rez = riseTemp;
        }
        return rez;
    }

    public static double referenceFusionStart(final double startTemp) {
        return (TFUSION - startTemp) / 0.9;
    }

    public static double referenceMass(final double startTemp, final double startMass, final double duration) {
        double rez = 0.;

        final double fusionDuration = referenceFusionStart(startTemp);

        if (duration < fusionDuration) {
            rez = startMass;
        } else {
            final double delta = (duration - fusionDuration);
            rez = startMass - 0.8 * delta - 0.4 * (MathLib.cos(delta) - 1);
        }

        return rez;
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