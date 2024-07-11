/**
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.sphere.lebedev;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Incrementor;

/**
 * Lebedev integrator.
 *
 * @author GMV
 * 
 * @since 4.0
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class LebedevIntegrator {

    /** Functions evaluation count. */
    private final Incrementor evaluations;

    /** Function to integrate. */
    private LebedevFunction function;

    /** Lebedev grid used for the integration. */
    private LebedevGrid grid;

    /**
     * Constructor.
     */
    public LebedevIntegrator() {
        // Evaluations counter
        this.evaluations = new Incrementor();
    }

    /**
     * Setup.
     * 
     * @param maxEval maximum number of evaluations
     * @param f function to integrate
     * @param gridIn grid
     */
    protected void setup(final int maxEval,
                         final LebedevFunction f,
                         final LebedevGrid gridIn) {
        // Reset
        this.function = f;
        this.grid = gridIn;

        this.evaluations.setMaximalCount(maxEval);
        this.evaluations.resetCount();
    }

    /**
     * Integration.
     * 
     * @param maxEval maximum number of evaluations
     * @param f function to integrate
     * @param gridIn grid
     * @return integrated function
     */
    public double integrate(final int maxEval,
                            final LebedevFunction f,
                            final LebedevGrid gridIn) {
        // Initialization.
        this.setup(maxEval, f, gridIn);

        // Perform computation.
        return this.doIntegrate();
    }

    /**
     * Integration.
     * 
     * @return integration result
     */
    protected double doIntegrate() {
        double integral = 0.;

        for (final LebedevGridPoint p : this.grid.getPoints()) {
            integral += this.function.value(p) * p.getWeight();
            this.evaluations.incrementCount();
        }

        return 4. * FastMath.PI * integral;
    }

    /**
     * Return the current number of function evaluations.
     * 
     * @return the current number of function evaluations
     */
    public int getEvaluations() {
        return this.evaluations.getCount();
    }
}
