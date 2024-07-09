/**
 * 
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
 * 
 * @history created 13/04/2015
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation.data;

import java.util.Map;
import java.util.Set;

import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class for data resulting from STELA differential equation system.
 * 
 * @author Emmanuel Bignon
 * @concurrency not thread-safe
 * @version $Id$
 * @since 3.0
 */
public class TimeDerivativeData {

    /** State. */
    private final Orbit orbit;

    /** Mean motion. */
    private final double[] meanMotion;

    /** Mean motion from STM (state transition matrix). */
    private final double[][] meanMotionSTM;

    /** Derivatives map <Force model, dE'/dt (E' = mean orbital parameters)>. */
    private final Map<StelaForceModel, double[]> derivatives;

    /** Derivatives map <Force model, dSTM/dt (STM = state transition matrix)>. */
    private final Map<StelaForceModel, double[][]> derivativesSTM;

    /**
     * Constructor.
     * 
     * @param aOrbit
     *        state
     * @param aMeanMotion
     *        mean motion
     * @param aMeanMotionSTM
     *        mean motion from state transition matrix
     * @param aDerivatives
     *        derivatives map <Force model, dE'/dt (E' = mean orbital parameters)>
     * @param aDerivativesSTM
     *        derivatives map <Force model, dSTM/dt (STM = state transition matrix)>
     */
    public TimeDerivativeData(final Orbit aOrbit, final double[] aMeanMotion, final double[][] aMeanMotionSTM,
        final Map<StelaForceModel, double[]> aDerivatives,
        final Map<StelaForceModel, double[][]> aDerivativesSTM) {
        this.orbit = aOrbit;
        this.meanMotion = aMeanMotion;
        this.meanMotionSTM = aMeanMotionSTM;
        this.derivatives = aDerivatives;
        this.derivativesSTM = aDerivativesSTM;
    }

    /**
     * Returns the orbit.
     * 
     * @return the orbit
     */
    public Orbit getOrbit() {
        return this.orbit;
    }

    /**
     * Returns mean motion.
     * 
     * @return mean motion
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getMeanMotion() {
        return this.meanMotion;
    }

    /**
     * Returns mean motion from STM (STM = state transition matrix).
     * 
     * @return mean motion from STM (STM = state transition matrix)
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getMeanMotionSTM() {
        return this.meanMotionSTM;
    }

    /**
     * Returns derivative dE'/dt (E' = mean orbital parameters) for provided force model.
     * 
     * @param force
     *        a force model
     * @return time derivative for provided force model
     */
    public double[] getDerivatives(final StelaForceModel force) {
        return this.derivatives.get(force);
    }

    /**
     * Returns derivative dSTM/dt (STM = state transition matrix) for provided force model.
     * 
     * @param force
     *        a force model
     * @return transition matrix time derivative for provided force model
     */
    public double[][] getDerivativesSTM(final StelaForceModel force) {
        return this.derivativesSTM.get(force);
    }

    /**
     * Getter for the sum of all contributions to dE'/dt (E' = mean orbital parameters).
     * 
     * @return the sum of all contribution to dE'/dt (E' = mean orbital parameters)
     * @throws PatriusException
     *         thrown if derivatives dimension mismatch
     */
    public double[] getTotalContribution() throws PatriusException {
        // Add all derivatives
        double[] res = new double[6];
        for (final double[] d : this.derivatives.values()) {
            res = this.vectorAdd(res, d);
        }

        // Add mean motion
        res = this.vectorAdd(res, this.meanMotion);

        return res;
    }

    /**
     * Add 2 vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @return sum of the two vectors
     */
    private double[] vectorAdd(final double[] v1, final double[] v2) {
        final double[] res = new double[v1.length];
        for (int i = 0; i < v1.length; i++) {
            res[i] = v1[i] + v2[i];
        }
        return res;
    }

    /**
     * Getter for the sum of all contributions to dSTM/dt (STM = state transition matrix).
     * 
     * @return the sum of all contribution to dSTM/dt (STM = state transition matrix)
     * @throws PatriusException
     *         thrown if derivatives dimension mismatch
     */
    public double[][] getTotalContributionSTM() throws PatriusException {
        // Add all derivatives
        double[][] res = new double[6][6 + 2];
        for (final double[][] d : this.derivativesSTM.values()) {
            res = JavaMathAdapter.matrixAdd(res, d);
        }

        // Add mean motion
        res = JavaMathAdapter.matrixAdd(res, this.meanMotionSTM);

        return res;
    }

    /**
     * Returns available force models.
     * 
     * @return available force models
     */
    public Set<StelaForceModel> getAvailableForceModels() {
        return this.derivatives.keySet();
    }

    /**
     * Returns available force models for state transition matrix.
     * 
     * @return available force models for state transition matrix
     */
    public Set<StelaForceModel> getAvailableForceModelsSTM() {
        return this.derivativesSTM.keySet();
    }
}
