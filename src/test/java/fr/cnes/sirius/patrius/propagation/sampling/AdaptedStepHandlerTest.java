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
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:389:10/03/2015:add interpolator copy method
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * 
 * @version $Id$
 * 
 */
public class AdaptedStepHandlerTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Validation of copy method
         * 
         * @featureDescription test the copy method
         * 
         * @coveredRequirements
         */
        COPY,

        /**
         * @featureTitle Validation of propagation using copy method
         * 
         * @featureDescription test the propagation using copy method
         * 
         * @coveredRequirements
         */
        PROPAGATION_COPY;
    }

    /**
     * @throws PatriusException
     *         thrown if propagation failed
     * @testType UT
     * 
     * @testedFeature {@link features#PROPAGATION_COPY}
     * 
     * @description tests propagation using copied step handlers
     * 
     * @testPassCriteria propagation in ephemeris mode is the same as propagation using copied interpolators
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testPropagationCopy() throws PatriusException {
        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        // Initial state
        final double mu = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(),
            initDate, mu);
        final MassProvider massModel = new SimpleMassModel(1000, "Satellite");
        final SpacecraftState initialState = new SpacecraftState(new CartesianOrbit(orbit), massModel);

        // Propagator
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 200, 1E-7, 1E-12);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setAttitudeProvider(new BodyCenterPointing(FramesFactory.getGCRF()));
        propagator.setInitialState(initialState);
        propagator.addAdditionalEquations(massModel.getAdditionalEquation("Satellite"));
        propagator.setAdditionalStateTolerance("MASS_Satellite", new double[] { 5.0e-6 }, new double[] { 5.0e-7 });

        // Add forces
        final RadiationSensitive vehicule = new SphericalSpacecraft(5., 2.1, 0.4, 0.4, 0.2, "Satellite");
        final ForceModel srp = new SolarRadiationPressure(CelestialBodyFactory.getSun(), 6378000, vehicule);
        propagator.addForceModel(srp);

        // Add maneuvers
        propagator.addEventDetector(new ImpulseManeuver(new DateDetector(initDate.shiftedBy(500)), new Vector3D(100, 0,
            0), FramesFactory.getGCRF(), 100, massModel, "Satellite"));

        // Interpolators
        final List<PatriusStepInterpolator> interpolators = new ArrayList<>();
        final List<AbsoluteDate> start = new ArrayList<>();
        final List<AbsoluteDate> end = new ArrayList<>();

        final PatriusStepHandler handler = new PatriusStepHandler(){

            /** Serializable UID. */
            private static final long serialVersionUID = 6999923618159383923L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                start.add(s0.getDate());
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                throws PropagationException {
                // Store current interpolator
                interpolators.add(((AdaptedStepHandler) interpolator).copy());
                end.add(interpolator.getCurrentDate());

                if (!isLast) {
                    start.add(interpolator.getCurrentDate());
                }
            }
        };

        final SpacecraftState[] ephemerisStates = new SpacecraftState[1000];
        final SpacecraftState[] interpolatorStates = new SpacecraftState[1000];

        // Propagation (ephemeris mode)
        propagator.setEphemerisMode();
        propagator.propagate(initDate.shiftedBy(1000));
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        // Propagation (master mode)
        propagator.setMasterMode(handler);
        propagator.setInitialState(initialState);
        propagator.propagate(initDate.shiftedBy(1000));

        // Ephemeris case
        for (int i = 0; i < ephemerisStates.length; i++) {
            ephemerisStates[i] = ephemeris.propagate(initDate.shiftedBy(i));
        }

        // Interpolator case
        for (int i = 0; i < interpolatorStates.length; i++) {
            final AbsoluteDate date = initDate.shiftedBy(i);

            // Find relevant interpolator in list
            int count = 0;
            while (date.durationFrom(end.get(count)) > 0) {
                count++;
            }

            final PatriusStepInterpolator interpolator = interpolators.get(count);
            interpolator.setInterpolatedDate(date);
            interpolator.getInterpolatedState();
            interpolatorStates[i] = interpolator.getInterpolatedState();
        }

        // Check ephemeris are equals
        for (int i = 0; i < ephemerisStates.length; i++) {
            Assert.assertEquals(ephemerisStates[i].getDate().durationFrom(interpolatorStates[i].getDate()), 0.);
            Assert.assertEquals(ephemerisStates[i].getA(), interpolatorStates[i].getA());
            Assert.assertEquals(ephemerisStates[i].getE(), interpolatorStates[i].getE());
            Assert.assertEquals(ephemerisStates[i].getI(), interpolatorStates[i].getI());
            Assert.assertEquals(ephemerisStates[i].getLM(), interpolatorStates[i].getLM());
            Assert.assertEquals(ephemerisStates[i].getEquinoctialEx(), interpolatorStates[i].getEquinoctialEx());
            Assert.assertEquals(ephemerisStates[i].getEquinoctialEy(), interpolatorStates[i].getEquinoctialEy());
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COPY}
     * 
     * @testedMethod {@link AdaptedStepHandler#copy()}
     * 
     * @description copy step handler
     * 
     * @input a step handler
     * 
     * @output copied step handler
     * 
     * @testPassCriteria hashcodes are different
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testCopy() {
        Utils.setDataRoot("regular-data");

        // Build step handler
        final PatriusStepHandler orekitStepHandler = new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -2196495623998480632L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                throws PropagationException {
                // nothing to do
            }
        };
        final AdaptedStepHandler stepHandler = new AdaptedStepHandler(orekitStepHandler);
        final StepInterpolator stepInterpolator = new MyStepInterpolator();
        stepHandler.handleStep(stepInterpolator, false);

        // Get copy
        final AdaptedStepHandler copy = stepHandler.copy();

        // Check
        Assert.assertFalse(stepHandler.hashCode() == copy.hashCode());
    }

    /**
     * Dummy step interpolator.
     */
    private class MyStepInterpolator implements StepInterpolator {
        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            // nothing to do
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            // nothing to do
        }

        @Override
        public void setInterpolatedTime(final double time) {
            // nothing to do
        }

        @Override
        public boolean isForward() {
            return true;
        }

        @Override
        public double getPreviousTime() {
            return 0.;
        }

        @Override
        public double getInterpolatedTime() {
            return 0.;
        }

        @Override
        public double[] getInterpolatedState() throws MaxCountExceededException {
            return null;
        }

        @Override
        public double[] getInterpolatedSecondaryState(final int index) throws MaxCountExceededException {
            return null;
        }

        @Override
        public double[] getInterpolatedSecondaryDerivatives(final int index) throws MaxCountExceededException {
            return null;
        }

        @Override
        public double[] getInterpolatedDerivatives() throws MaxCountExceededException {
            return null;
        }

        @Override
        public double getCurrentTime() {
            return 0;
        }

        @Override
        public StepInterpolator copy() throws MaxCountExceededException {
            return new MyStepInterpolator();
        }
    }
}
