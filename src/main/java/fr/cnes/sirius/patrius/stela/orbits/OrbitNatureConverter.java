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
 * @history created 22/01/13
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2501:27/01/2021:[PATRIUS] Nouveau seuil dans OrbitNatureConverter 
 * VERSION:4.5:DM:DM-2416:27/05/2020:Nouveau seuil dans OrbitNatureConverter 
 * VERSION:4.4:FA:FA-2297:04/10/2019:[PATRIUS] probleme de convergence osculateur -> moyen de Stela
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.orbits;

import java.io.Serializable;
import java.util.List;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.StelaLagrangeEquations;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Converts a {@link StelaEquinoctialOrbit} from mean to osculating parameters, and reverse.
 * Since the {@link StelaEquinoctialOrbit} does not contain a "mean" or "osculating" information flag,
 * it is the user's responsibility to ensure a coherent use of this converter.
 * 
 * @useSample final OrbitNatureConverter obc = new OrbitNatureConverter();<br>
 *            final StelaEquinoctialOrbit meanOrbit = obc.toMean(osculatingOrbit,...);
 * 
 * @concurrency not thread-safe
 * @concurrency.comment instances are mutable
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class OrbitNatureConverter implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -85396581495142806L;

    /** Max. number of loops to converge on a proper conversion. */
    private static final int MAX_FOR_CONVERGENCE = 20;

    /** Epsilon for conversion (absolute error). */
    private static final double EPS_CVG_BULL_NATURE = 1E-99;

    /** Epsilon for conversion (relative error). */
    private static final double DEFAULT_THRESHOLD = 1E-14;

    /** Relative convergence threshold for osculating to mean conversion. */
    private static double threshold1 = DEFAULT_THRESHOLD;

    /**
     * Second relative convergence threshold for osculating to mean conversion used only in maximum number of iterations
     * has been reached.
     */
    private static double threshold2 = DEFAULT_THRESHOLD;
    
    /** List of force models used for conversion. */
    private final List<StelaForceModel> forceModels;
    /** Lagrange equations */
    private final StelaLagrangeEquations lag;

    /**
     * Default constructor.
     * 
     * @param inForceModels
     *        list of force models used for conversion
     */
    public OrbitNatureConverter(final List<StelaForceModel> inForceModels) {
        this.forceModels = inForceModels;
        this.lag = new StelaLagrangeEquations();
    }

    /**
     * Converts an osculating {@link StelaEquinoctialOrbit} to a mean one.
     * 
     * @param oscOrbit
     *        input osculating orbit
     * @return a mean orbit equivalent to the osculating input orbit
     * @throws PatriusException
     *         if a position computation fails
     */
    public StelaEquinoctialOrbit toMean(final StelaEquinoctialOrbit oscOrbit) throws PatriusException {

        // Conversion into CIRF frame
        final StelaEquinoctialOrbit cirfOscOrbit;
        if (FramesFactory.getCIRF() == oscOrbit.getFrame()) {
            cirfOscOrbit = oscOrbit;
        } else {
            // Convert to the right frame
            final PVCoordinates modPvc = oscOrbit.getPVCoordinates(FramesFactory.getCIRF());
            cirfOscOrbit = new StelaEquinoctialOrbit(modPvc, FramesFactory.getCIRF(), oscOrbit.getDate(),
                oscOrbit.getMu());
        }

        // Mean parameters computation
        // Get T8 params in an array
        final double[] ppT8 = cirfOscOrbit.mapOrbitToArray();
        final double[] paramsT8 = { ppT8[0], ppT8[1], ppT8[2], ppT8[3], ppT8[4], ppT8[5] };
        // A copy of the input state initializes the algorithm => it is set to "Mean"
        StelaEquinoctialOrbit orbTmp = cirfOscOrbit;
        boolean stopIter = false;
        final double[] bullNew = { 0., 0., 0., 0., 0., 0. };
        final double[] bullOld = { 0., 0., 0., 0., 0., 0. };
        final double[] bullOldOld = { 0., 0., 0., 0., 0., 0. };
        final int bullLength = bullOld.length;
        int icount = 0;
        double[] shortPeriods;
        // Iteration loop
        while (!stopIter) {
            if (icount < MAX_FOR_CONVERGENCE) {
                shortPeriods = this.shortPeriodsGTO(orbTmp);
                // Update states for steps n-1, n, and n+1
                for (int i = 0; i < bullLength; i++) {
                    bullOldOld[i] = bullOld[i];
                    bullOld[i] = bullNew[i];
                    bullNew[i] = paramsT8[i] - shortPeriods[i];
                }
                bullNew[1] = JavaMathAdapter.mod(bullNew[1], 2 * FastMath.PI);
                // Convergence test
                stopIter = checkConvergence(bullNew, bullOld, bullOldOld, threshold1);
                icount++;
                // orbTmp update
                orbTmp = new StelaEquinoctialOrbit(bullNew[0], bullNew[2], bullNew[3],
                    bullNew[4],
                    bullNew[5], bullNew[1],
                    FramesFactory.getCIRF(), oscOrbit.getDate(), oscOrbit.getMu());
            } else {
                // Check with relative threshold for degraded case
                stopIter = checkConvergence(bullNew, bullOld, bullOldOld, threshold2);
                if (stopIter) {
                    // Exit while loop keeping last bulletin
                    break;
                }
                // Throw exception
                throw new PatriusException(PatriusMessages.PDB_OSC_MEAN_CVG_ERROR);
            }
        }

        // Create the mean orbit
        return new StelaEquinoctialOrbit(bullNew[0], bullNew[2], bullNew[3],
            bullNew[4], bullNew[5], bullNew[1], FramesFactory.getCIRF(), oscOrbit.getDate(), oscOrbit.getMu());
    }

    /**
     * Check convergence.
     * @param bullNew new bulletin
     * @param bullOld previous bulletin
     * @param bullOldOld bulletin before previous bulletin
     * @param threshold relative threshold
     * @return true if bulletin has converged with provided threshold
     */
    private static boolean checkConvergence(final double[] bullNew, final double[] bullOld, final double[] bullOldOld,
                                            final double threshold) {
        boolean stopIter = true;
        for (int i = 0; i < 6; i++) {
            if (bullOld[i] == 0.) {
                stopIter &= (MathLib.abs(bullOld[i] - bullNew[i]) < EPS_CVG_BULL_NATURE);
            } else {
                /*
                 * Since the problem is not continuous, with the previous test the algorithm
                 * could become non-convergent
                 */
                if ((MathLib.abs(MathLib.divide((bullOld[i] - bullNew[i]),
                    bullOld[i])) < threshold)
                    || (Double.compare(bullNew[i], bullOldOld[i]) == 0)) {
                    stopIter &= true;
                } else {
                    stopIter &= false;
                }
            }
        }
        return stopIter;
    }
    
    /**
     * Converts a mean {@link StelaEquinoctialOrbit} to an osculating one.
     * 
     * @param meanOrbit
     *        input mean orbit
     * @return an osculating orbit equivalent to the mean input orbit
     * @throws PatriusException
     *         if a position computation fails
     */
    public StelaEquinoctialOrbit toOsculating(final StelaEquinoctialOrbit meanOrbit) throws PatriusException {

        // Conversion into CIRF frame
        final StelaEquinoctialOrbit cirfOrbit;
        if (FramesFactory.getCIRF().equals(meanOrbit.getFrame())) {
            cirfOrbit = meanOrbit;
        } else {
            // Convert to the right frame
            final PVCoordinates modPvc = meanOrbit.getPVCoordinates(FramesFactory.getCIRF());
            cirfOrbit = new StelaEquinoctialOrbit(modPvc, FramesFactory.getCIRF(), meanOrbit.getDate(),
                meanOrbit.getMu());
        }

        // Osculating parameters computation
        final double[] shortPeriods = this.shortPeriodsGTO(cirfOrbit);

        // Set converted values
        final double a = cirfOrbit.getA() + shortPeriods[0];
        final double lambdaEq = JavaMathAdapter.mod(cirfOrbit.getLM() + shortPeriods[1], 2 * FastMath.PI);
        final double eX = cirfOrbit.getEquinoctialEx() + shortPeriods[2];
        final double eY = cirfOrbit.getEquinoctialEy() + shortPeriods[3];
        final double iX = cirfOrbit.getIx() + shortPeriods[4];
        final double iY = cirfOrbit.getIy() + shortPeriods[5];

        // Create the osculating orbit
        return new StelaEquinoctialOrbit(a, eX, eY, iX, iY, lambdaEq,
            FramesFactory.getCIRF(), meanOrbit.getDate(), meanOrbit.getMu());
    }

    /**
     * Compute short periods terms for GTO orbits.
     * 
     * @param orbit
     *        the orbit
     * @return the GTO short period terms
     * @throws PatriusException
     *         thrown if input orbital parameters are incoherent
     */
    private double[] shortPeriodsGTO(final StelaEquinoctialOrbit orbit) throws PatriusException {

        final double[] dW = new double[6];

        for (final StelaForceModel model : this.forceModels) {
            final double[] dWtmp = model.computeShortPeriods(orbit);
            for (int j = 0; j < dW.length; j++) {
                dW[j] += dWtmp[j];
            }
        }

        // Division by 2 of the sma derivative (mean motion, impacting only lambda)
        dW[0] = dW[0] / 2;

        return JavaMathAdapter.matrixVectorMultiply(this.lag.computeLagrangeEquations(orbit), dW);
    }

    /**
     * Setter for osculating to mean conversion relative convergence threshold. Default value for
     * this threshold is {@link #DEFAULT_THRESHOLD}.
     * 
     * @param newThreshold new threshold to set
     */
    public static void setThreshold(final double newThreshold) {
        threshold1 = newThreshold;
    }

    /**
     * Setter for osculating to mean conversion second relative convergence threshold.
     * This threshold is used only if convergence has not been reached within maximum number of iterations.
     * If convergence is reached with this threshold, then last bulletin is returned, otherwise an exception is thrown.
     * Default value for this threshold is {@link #DEFAULT_THRESHOLD}.
     * 
     * @param newThreshold new threshold to set
     */
    public static void setThresholdDegraded(final double newThreshold) {
        threshold2 = newThreshold;
    }
}
