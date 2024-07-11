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
 * @history created 31/01/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:91:26/07/2013:use of sigma matrix improved
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.StelaLagrangeEquations;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.forces.radiation.StelaSRPSquaring;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a set of {@link StelaAdditionalEquations additional equations} computing the partial
 * derivatives of the state (orbit) with respect to initial state.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment use of mutable attributes
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaPartialDerivativesEquations implements StelaAdditionalEquations {

    /** Serial UID. */
    private static final long serialVersionUID = 2145056564146909194L;

    /** Size of the state vector. */
    private static final int STATE_SIZE = 6;

    /** Name. */
    private static final String NAME = "PARTIAL_DERIVATIVES";
    
    /** -3.0 */
    private static final double MINUS_THREE = -3.0;

    /** The Stela Gauss force models */
    private final List<AbstractStelaGaussContribution> gaussForces;

    /** The Stela Lagrange force models */
    private final List<AbstractStelaLagrangeContribution> lagrangeForces;

    /** Old state vector */
    private double[] pDotOld;

    /** Step counter */
    private int stepCounter;

    /** Re-computation step */
    private final int recomputeStep;

    /** SRP. */
    private StelaSRPSquaring srp;
    /** SRP potential value. */
    private double[] potSRP;
    /** SRP derivatives value. */
    private double[][] derSRP;

    /** Stela propagator. */
    private final StelaGTOPropagator stelaPropagator;

    /** Derivatives map <Force model, dSTM/dt (STM = state transition matrix)>. */
    private Map<StelaForceModel, double[][]> derivatives;

    /** Mean motion from state transition matrix. */
    private double[][] meanMotion;

    /**
     * Simple constructor.
     * 
     * @param forcesG
     *        the list of Gauss forces associated to the propagator
     * @param forcesL
     *        the list of Lagrange forces associated to the propagator
     * @param recomputeStepIn
     *        recompute every recomputeStepIn steps
     * @param inStelaPropagator
     *        Stela propagator
     */
    public StelaPartialDerivativesEquations(final List<AbstractStelaGaussContribution> forcesG,
        final List<AbstractStelaLagrangeContribution> forcesL, final int recomputeStepIn,
        final StelaGTOPropagator inStelaPropagator) {
        this.gaussForces = forcesG;
        this.lagrangeForces = forcesL;
        this.stepCounter = 0;
        this.recomputeStep = recomputeStepIn;
        this.stelaPropagator = inStelaPropagator;

        for (int i = 0; i < this.gaussForces.size(); i++) {
            // Look for the atmospheric drag force in the list:
            if (this.gaussForces.get(i).getClass().equals(StelaAtmosphericDrag.class)) {
                // We set to true the transition matrix computation flag for the atmospheric drag:
                ((StelaAtmosphericDrag) this.gaussForces.get(i)).setTransMatComputationFlag(true);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getEquationsDimension() {
        return STATE_SIZE * (STATE_SIZE + 2);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return NAME;
    }

    /** {@inheritDoc} */
    @Override
    public void computeDerivatives(final StelaEquinoctialOrbit orbit, final double[] p,
                                   final double[] pDot) throws PatriusException {

        // Initialize partial derivatives storage

        final int mod = (int) JavaMathAdapter.mod(this.stepCounter, this.recomputeStep);

        if (mod == 0) {

            // ********** Transition matrix : d(dE/dp)/dt with p = ( E0 K10 K20 ... ) **********

            // Convert transition matrix from vector representation to matrix
            final double[][] pMat = new double[STATE_SIZE][p.length / STATE_SIZE];
            JavaMathAdapter.vectorToMatrix(p, pMat);

            // Add mean motion
            final double a = orbit.getA();
            final double[][] dEDotDEMeanMotion = new double[STATE_SIZE][STATE_SIZE];
            dEDotDEMeanMotion[1][0] = MINUS_THREE * MathLib.sqrt(orbit.getMu() / (a * a * a)) / (2.0 * a);
            double[][] transitionMatrix = JavaMathAdapter.matrixMultiply(dEDotDEMeanMotion, pMat);

            // Initialize time derivatives and store mean motion
            if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                this.derivatives = new HashMap<StelaForceModel, double[][]>();
                this.meanMotion = this.matrixCopy(transitionMatrix);
            }

            // Gauss contribution
            final double[][] transitionMatrix1 = this.gaussForcesYDot(orbit, pMat);

            // Lagrange contribution
            final double[][] transitionMatrix2 = this.lagrangeForcesYDot(orbit, pMat);

            transitionMatrix = JavaMathAdapter.matrixAdd(transitionMatrix, transitionMatrix1);
            transitionMatrix = JavaMathAdapter.matrixAdd(transitionMatrix, transitionMatrix2);

            // Add transition matrix derivatives to yDot
            JavaMathAdapter.matrixToVector(transitionMatrix, pDot, 0);

            // Update pDotOld state
            this.pDotOld = pDot.clone();
        } else {
            System.arraycopy(this.pDotOld.clone(), 0, pDot, 0, this.pDotOld.clone().length);
        }
    }

    /**
     * Returns derivatives map <Force model, dSTM/dt (STM = state transition matrix)>.
     * 
     * @return derivatives map <Force model, dSTM/dt (STM = state transition matrix)>
     */
    public Map<StelaForceModel, double[][]> getDerivatives() {
        return this.derivatives;
    }

    /**
     * Returns mean motion from state transition matrix.
     * 
     * @return mean motion from state transition matrix
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getMeanMotion() {
        return this.meanMotion;
    }

    /** update step counter */
    public void updateStepCounter() {
        this.stepCounter++;

    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
        final double[] initialState = new double[STATE_SIZE * (STATE_SIZE + 2)];
        // Initial derivatives are not zero : da/da0 = 1, etc.
        for (int i = 0; i < STATE_SIZE; i++) {
            initialState[i * (1 + STATE_SIZE)] = 1.0;
        }
        return state.addAdditionalState(this.getName(), initialState);
    }

    /**
     * Compute the contribution of all Gauss forces.
     * 
     * @param orbit
     *        current orbit
     * @param pMat
     *        current transition matrix as a matrix
     * @return transitionMatrix
     *         transition matrix derivative
     * @throws PatriusException
     *         error when computing perturbations
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private double[][] gaussForcesYDot(final StelaEquinoctialOrbit orbit,
                                       final double[][] pMat) throws PatriusException {

        // Initialization
        double[][] res = new double[STATE_SIZE][STATE_SIZE + 2];

        for (final StelaForceModel gauss : this.gaussForces) {
            // Compute partial derivatives
            final double[][] daidt = ((AbstractStelaGaussContribution) gauss).computePartialDerivatives(orbit);

            if (gauss.getClass().equals(StelaSRPSquaring.class)) {
                // Dealing with particular case of Stela SRP: SRP is a Gauss force
                // but considered as a Lagrange force for transition matrix computation
                this.srp = (StelaSRPSquaring) gauss;
                this.potSRP = this.srp.computePotentialPerturbation(orbit);
                this.derSRP = daidt;
            } else {
                // Multiply by previous state to get transition matrix derivative (yDot = TM*y + sigma)
                final double[][] contribution = JavaMathAdapter.matrixMultiply(daidt, pMat);

                // Add drag part if required (sigma 7th column)
                if (gauss.getClass().equals(StelaAtmosphericDrag.class)) {
                    final double[] dragPert = ((AbstractStelaGaussContribution) gauss).getdPert();
                    for (int i = 0; i < res.length; i++) {
                        contribution[i][STATE_SIZE] = dragPert[i];
                    }
                }

                // Add contribution
                res = JavaMathAdapter.matrixAdd(res, contribution);

                // Store derivative if required
                if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                    this.derivatives.put(gauss, contribution);
                }
            }
        }

        return res;
    }

    /**
     * Compute the contribution of all Lagrange forces.
     * 
     * @param orbit
     *        current orbit
     * @param pMat
     *        current transition matrix as a matrix
     * @return transitionMatrix
     *         updated transition matrix derivative
     * @throws PatriusException
     *         error when computing perturbations
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private double[][] lagrangeForcesYDot(final StelaEquinoctialOrbit orbit,
                                          final double[][] pMat) throws PatriusException {

        // Initialization
        double[][] res = new double[STATE_SIZE][STATE_SIZE + 2];
        final StelaLagrangeEquations lag = new StelaLagrangeEquations();
        final double[][][] dPoisson = lag.computeLagrangeDerivativeEquations(orbit);
        final double[][] poisson = lag.computeLagrangeEquations(orbit);

        // Loop on all Lagrange forces
        for (final StelaForceModel lagrange : this.lagrangeForces) {
            // Compute partial derivatives
            final double[][] daidt = ((AbstractStelaLagrangeContribution) lagrange).computePartialDerivatives(orbit);

            // Get potential
            final double[] pot = ((AbstractStelaLagrangeContribution) lagrange).getdPot();

            // Check if partial derivatives are computed (if not potential is not taken into account)
            double[][] contribution = new double[STATE_SIZE][STATE_SIZE + 2];
            if (!this.isEmpty(daidt)) {
                final double[][] l1 = JavaMathAdapter.threeDMatrixVectorMultiply(dPoisson, pot);
                final double[][] l2 = JavaMathAdapter.scalarMultiply(-1, JavaMathAdapter.matrixMultiply(poisson,
                    daidt));
                contribution = JavaMathAdapter.matrixMultiply(JavaMathAdapter.matrixAdd(l1, l2), pMat);
            }

            // J2² part of zonal perturbation is not to be multiplied by Lagrange equations
            if (lagrange.getClass().equals(StelaZonalAttraction.class)
                && ((StelaZonalAttraction) lagrange).isJ2SquareParDerComputed()) {
                final double[][] j2Square = ((StelaZonalAttraction) lagrange).computeJ2SquarePartialDerivatives(orbit);
                contribution = JavaMathAdapter.matrixAdd(contribution, JavaMathAdapter.matrixMultiply(j2Square, pMat));
            }

            // Add contribution
            res = JavaMathAdapter.matrixAdd(res, contribution);

            // Store derivative if required
            if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                this.derivatives.put(lagrange, contribution);
            }
        }

        // SRP case (Gauss force but partial derivatives are a Lagrange force)
        if (this.srp != null) {
            // Lagrange part
            final double[][] l1 = JavaMathAdapter.threeDMatrixVectorMultiply(dPoisson, this.potSRP);
            final double[][] l2 =
                JavaMathAdapter.scalarMultiply(-1, JavaMathAdapter.matrixMultiply(poisson, this.derSRP));
            final double[][] contribution = JavaMathAdapter.matrixMultiply(JavaMathAdapter.matrixAdd(l1, l2), pMat);

            // Special part (derivative with respect to Cr)
            final double[] sRPPert = JavaMathAdapter.matrixVectorMultiply(JavaMathAdapter.scalarMultiply(-1, poisson),
                this.potSRP);
            for (int i = 0; i < res.length; i++) {
                contribution[i][STATE_SIZE + 1] = sRPPert[i];
            }

            // Add contribution
            res = JavaMathAdapter.matrixAdd(res, contribution);

            // Store derivative if required
            if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                this.derivatives.put(this.srp, contribution);
            }
        }

        return res;
    }

    /**
     * Check that provided matrix is empty.
     * 
     * @param m
     *        a matrix
     * @return true if provided matrix is empty
     */
    private boolean isEmpty(final double[][] m) {
        for (final double[] element : m) {
            for (int j = 0; j < element.length; j++) {
                if (element[j] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Copy matrix.
     * 
     * @param m
     *        a matrix
     * @return copy of provided matrix
     */
    private double[][] matrixCopy(final double[][] m) {
        final double[][] res = new double[m.length][m[0].length];
        for (int i = 0; i < res.length; i++) {
            res[i] = m[i].clone();
        }
        return res;
    }
}
