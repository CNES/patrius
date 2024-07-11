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
 * @history created 21/01/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes and added sub-step drag computation mechanism
 * VERSION::DM:289:30/11/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * VERSION::DM:523:08/02/2016: add solid tides effects in STELA PATRIUS
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.StelaLagrangeEquations;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;
import fr.cnes.sirius.patrius.stela.forces.gravity.SolidTidesAcc;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaTesseralAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.forces.noninertial.NonInertialContribution;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.stela.propagation.data.TimeDerivativeData;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Class representing the differential system of a STELA GTO propagation. It implements a Commons-Math first order
 * differential equations system.
 * <p>
 * Forces contributions to dE'/dt are computed and summed in this class (E' being the state vector of the mean orbital
 * parameters).
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment not thread-safe due to use of mutable attributes
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaDifferentialEquations implements FirstOrderDifferentialEquations {

    /** Size of the state vector. */
    public static final int STATE_SIZE = 6;

    /** Required for step computation of the tesseral quads */
    protected boolean tesseralComputed = false;

    /** Gauss force models list. */
    private final List<AbstractStelaGaussContribution> gaussForces;

    /** Lagrange force models list. */
    private final List<AbstractStelaLagrangeContribution> lagrangeForces;

    /** Extrapolation initial date. */
    private final AbsoluteDate initialDate;

    /** Extrapolation frame. */
    private final Frame frame;

    /** Orbit central attraction coefficient. */
    private final double mu;

    /** List of Stela additional equations. */
    private final List<StelaAdditionalEquations> addEquationsList;

    /** The mean / osculating orbit converter. */
    private final OrbitNatureConverter converter;

    /** Flag indicating if drag has already been computed. */
    private boolean dragComputed = false;

    /** The computed drag derivatives. */
    private double[] daidtDrag = new double[6];

    /** Stela propagator. */
    private final StelaGTOPropagator stelaPropagator;

    /** Derivatives map <Force model, dE'/dt (E' = mean orbital parameters)>. */
    private Map<StelaForceModel, double[]> derivatives;

    /**
     * Build a new instance of the Stela differential equations.
     * 
     * @param inStelaPropagator
     *        the Stela GTO propagator.
     * @throws PatriusException
     *         exception in propagator
     */
    public StelaDifferentialEquations(final StelaGTOPropagator inStelaPropagator) throws PatriusException {
        this.initialDate = inStelaPropagator.getReferenceDate();
        this.frame = inStelaPropagator.getFrame();
        this.mu = inStelaPropagator.getInitialState().getMu();
        this.gaussForces = inStelaPropagator.getGaussForceModels();
        this.lagrangeForces = inStelaPropagator.getLagrangeForceModels();
        this.addEquationsList = inStelaPropagator.getAddEquations();
        this.converter = inStelaPropagator.getOrbitNatureConverter();
        this.stelaPropagator = inStelaPropagator;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        int dim = STATE_SIZE;
        for (final StelaAdditionalEquations eq : this.addEquationsList) {
            dim += eq.getEquationsDimension();
        }
        return dim;
    }

    /** {@inheritDoc} */
    @Override
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        try {

            // Update space dynamics parameters:
            final AbsoluteDate date = this.initialDate.shiftedBy(t);
            final StelaEquinoctialOrbit orbit =
                new StelaEquinoctialOrbit(y[0], y[2], y[3], y[4], y[5], y[1], this.frame,
                    date, this.mu);

            // Initialize derivatives:
            Arrays.fill(yDot, 0.0);

            // Initialization for time derivatives storage
            if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                this.derivatives = new HashMap<StelaForceModel, double[]>();
            }
            Map<StelaForceModel, double[][]> dstmdt = null;
            double[][] meanMotionSTM = null;

            // First column of y is the real state of the system
            // Next columns represent the partial derivatives dy/dp

            // ********** Usual derivatives computation : dE/dt **********

            // Compute the contributions of all perturbing forces:

            // Add the Kepler contribution
            orbit.addKeplerContribution(PositionAngle.MEAN, this.mu, yDot);
            final double[] meanMotion = new double[6];
            System.arraycopy(yDot, 0, meanMotion, 0, 6);

            // Compute the contributions of all Gauss forces
            this.gaussForcesYDot(yDot, orbit);
            // Compute the contributions of all Lagrange forces
            this.lagrangeForcesYDot(yDot, orbit);

            // Add contribution for additional state
            int index = STATE_SIZE;
            for (final StelaAdditionalEquations eu : this.addEquationsList) {
                final int size = eu.getEquationsDimension();
                final double[] p = new double[size];
                final double[] pDot = new double[size];

                // update current additional state
                System.arraycopy(y, index, p, 0, p.length);

                // compute additional derivatives
                eu.computeDerivatives(orbit, p, pDot);

                // update each additional state contribution in global array
                System.arraycopy(pDot, 0, yDot, index, p.length);

                // incrementing index
                index += p.length;

                if (this.stelaPropagator.isRegisterTimeDerivatives() &&
                    eu.getClass().equals(StelaPartialDerivativesEquations.class)) {
                    dstmdt = ((StelaPartialDerivativesEquations) eu).getDerivatives();
                    meanMotionSTM = ((StelaPartialDerivativesEquations) eu).getMeanMotion();
                }
            }

            // Store time derivatives if required
            if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                this.stelaPropagator.addTimeDerivativeData(new TimeDerivativeData(orbit, meanMotion, meanMotionSTM,
                    this.derivatives, dstmdt));
            }

        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * Compute the contribution of all Lagrange forces.
     * 
     * @param yDot
     *        the time derivative of the state vector
     * @param orbit
     *        the current orbit
     * 
     * @throws PatriusException
     *         error when computing perturbations.
     */
    private void lagrangeForcesYDot(final double[] yDot, final StelaEquinoctialOrbit orbit) throws PatriusException {

        // Initialization
        final double[][] lagrangeEq = new StelaLagrangeEquations().computeLagrangeEquations(orbit);

        // Loop on all forces
        for (final StelaForceModel lagrange : this.lagrangeForces) {
            // Tesseral case
            this.updateQuads(orbit, lagrange);

            // Generic case
            final double[] daidt = ((AbstractStelaLagrangeContribution) lagrange).computePerturbation(orbit);

            final double[] yLagrange;
            if (lagrange.getClass().equals(SolidTidesAcc.class)) {
                // Solid tides acceleration not to be multiplied by Lagrange equations (performed in solid tides class)
                yLagrange = daidt;
            } else {
                yLagrange = JavaMathAdapter.negate(JavaMathAdapter.matrixVectorMultiply(lagrangeEq, daidt));
            }

            // J2² part of zonal perturbation is not to be multiplied by Lagrange equations
            if (lagrange.getClass().equals(StelaZonalAttraction.class)
                && ((StelaZonalAttraction) lagrange).isJ2SquareComputed()) {
                final double[] j2Square = ((StelaZonalAttraction) lagrange).computeJ2Square(orbit);
                for (int i = 0; i < j2Square.length; i++) {
                    yLagrange[i] += j2Square[i];
                }
            }

            // Update yDot
            for (int i = 0; i < yLagrange.length; i++) {
                yDot[i] += yLagrange[i];
            }

            // Store derivative if required
            if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                this.derivatives.put(lagrange, yLagrange);
            }
        }
    }

    /**
     * Update quads for {@link StelaTesseralAttraction tesseral pertubations}
     * 
     * @param orbit
     *        orbit
     * @param lagrange
     *        Lagrange force model
     */
    protected void updateQuads(final StelaEquinoctialOrbit orbit, final StelaForceModel lagrange) {
        if (!this.tesseralComputed && lagrange.getClass().equals(StelaTesseralAttraction.class)) {
            ((StelaTesseralAttraction) lagrange).updateQuads(orbit);
            this.tesseralComputed = true;
        }
    }

    /**
     * Compute the contribution of all Gauss forces.
     * 
     * @param yDot
     *        the time derivative of the state vector
     * @param orbit
     *        the current orbit
     * 
     * @throws PatriusException
     *         error when computing perturbations
     */
    protected void gaussForcesYDot(final double[] yDot, final StelaEquinoctialOrbit orbit) throws PatriusException {

        // Loop on all forces
        for (final StelaForceModel gauss : this.gaussForces) {

            if (gauss.getClass().equals(StelaAtmosphericDrag.class)
                && ((StelaAtmosphericDrag) gauss).getDragRecomputeStep() != 0) {

                this.dragForceYDot(yDot, orbit, gauss);

            } else if (gauss.getClass().equals(NonInertialContribution.class)) {

                // Non-inertial contribution case: computed once every step by step handler
                final double[] dNonInertial = this.stelaPropagator.getForcesStepHandler().getDnonInertial();
                for (int i = 0; i < dNonInertial.length; i++) {
                    // add the drag contribution:
                    yDot[i] += dNonInertial[i];
                }

                if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                    this.derivatives.put(gauss, dNonInertial);
                }

            } else {
                // all the other Gauss forces are computed every sub-step:
                final double[] daidt =
                    ((AbstractStelaGaussContribution) gauss).computePerturbation(orbit, this.converter);
                for (int i = 0; i < daidt.length; i++) {
                    yDot[i] += daidt[i];
                }

                // Store derivative if required
                if (this.stelaPropagator.isRegisterTimeDerivatives()) {
                    this.derivatives.put(gauss, daidt);
                }
            }
        }
    }

    /**
     * Compute the contribution of drag.
     * 
     * @param yDot
     *        the time derivative of the state vector
     * @param orbit
     *        an orbit
     * @param gauss
     *        force
     * @throws PatriusException
     *         error when computing perturbations
     */
    private void dragForceYDot(final double[] yDot, final StelaEquinoctialOrbit orbit,
                               final StelaForceModel gauss) throws PatriusException {

        if (!this.dragComputed) {
            // the drag is computed EVERY STEP (and not every sub-step):
            if (this.stelaPropagator.isRecomputeDrag()) {
                this.daidtDrag = ((AbstractStelaGaussContribution) gauss).computePerturbation(orbit, this.converter);
                this.stelaPropagator.setdDragdt(this.daidtDrag);

            } else {
                this.daidtDrag = this.stelaPropagator.getdDragdt();
            }
            this.dragComputed = true;
        }
        for (int i = 0; i < this.daidtDrag.length; i++) {
            // add the drag contribution:
            yDot[i] += this.daidtDrag[i];
        }

        // Store derivative if required
        if (this.stelaPropagator.isRegisterTimeDerivatives()) {
            this.derivatives.put(gauss, this.daidtDrag);
        }
    }
}
