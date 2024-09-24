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
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] L'attitude des spacecraft state devrait etre initialisee de maniere lazy
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-38:22/05/2023:[PATRIUS] Suppression de setters pour le MultiNumericalPropagator
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::FA:492:06/10/2015:Propagation until final date in master mode
 * VERSION::DM:426:30/10/2015: Tests the new functionalities on orbit definition and orbit propagation
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::FA:1871:09/10/2018: MassModel update fix validation test implementation (testMassModel)
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProviderWrapper;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.sensor.SatToSatMutualVisibilityTest;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.AbstractHarmonicGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ConstantThrustManeuverTest;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.MultiPropagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiAdaptedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepNormalizer;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * Validation class for {@link MultiNumericalPropagatorTest}
 * </p>
 *
 * @see SatToSatMutualVisibilityTest
 * @see MultiEventDetectorTest
 *
 * @author maggioranic
 *
 * @version $Id$
 *
 * @since 3.0
 *
 */
public class MultiNumericalPropagatorTest {

    /**
     * Tolerance 1e-14
     */
    private static final double E_14 = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * Tolerance 1e-13
     */
    private static final double E_13 = 1.0e-13;

    /**
     * First state name
     */
    private static final String STATE1 = "state1";

    /**
     * Second state name
     */
    private static final String STATE2 = "state2";

    /**
     * Third state name
     */
    private static final String STATE3 = "state3";

    /**
     * Main part name (MassProvider)
     */
    private static final String DEFAULT = "default";

    /**
     * First numerical propagator
     */
    private NumericalPropagator firstPropagator;

    /**
     * Second numerical propagator
     */
    private NumericalPropagator secondPropagator;

    /**
     * Integrator defined for two states.
     */
    private AdaptiveStepsizeIntegrator integratorMultiSat;

    /**
     * Multi-sat numerical propagator
     */
    private MultiNumericalPropagator multiNumericalPropagator;

    /**
     * Initial date
     */
    private AbsoluteDate initialDate;

    /**
     * Final date
     */
    private AbsoluteDate finalDate;

    /**
     * MU
     */
    private double mu;

    /**
     * First orbit
     */
    private Orbit orbit1;

    /**
     * First state
     */
    private SpacecraftState state1;

    /**
     * Second orbit
     */
    private Orbit orbit2;

    /**
     * Second state
     */
    private SpacecraftState state2;

    /**
     * Attitude Provider
     */
    private AttitudeProvider attProv;

    /**
     * Mass Provider
     */
    private MassProvider defaultMassModel;

    /**
     * Complex mass Provider
     */
    private MassProvider complexMassModel;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test methods
         *
         * @featureDescription Test methods behavior (no propagation)
         */
        METHODS,

        /**
         * @featureTitle Multi-sat numerical propagation mode
         *
         * @featureDescription test multi-sat numerical propagation modes (slave, ephemeris, master)
         *
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION_MODE,
        /**
         * @featureTitle Events detection in multi-sat numerical propagation
         *
         * @featureDescription test events detection during multi-sat numerical propagation (events
         *                     added to the propagator and events associated with forces model)
         *
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION_EVENTS,

        /**
         * @featureTitle Test different integrator
         *
         * @featureDescription test propagation with a scalar or a vectorial integrator
         *
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION_INTEGRATOR,

        /**
         * @featureTitle Multi-sat numerical propagation : general features
         *
         * @featureDescription Test general features of multi-sat numerical propagation
         *
         * @coveredRequirements
         */
        MULTI_SAT_PROPAGATION

    }

    /**
     * @throws PatriusException exception raised by addInitialState
     * @testType UT
     *
     * @testedFeature {@link features#METHODS}
     *
     * @testedMethod {@link MultiNumericalPropagator#addInitialState(SpacecraftState, String)}
     * @testedMethod {@link MultiNumericalPropagator#getFrame(String)}
     *
     * @description Test the exceptions raised by addInitialState method
     *
     * @input a propagator and an initial state with a null or empty satID
     *
     * @output an exception
     *
     * @testPassCriteria an exception should be raised
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testAddInitialStateExceptions() throws PatriusException {
        Assert.assertNull(this.multiNumericalPropagator.getFrame(STATE1));
        Assert.assertNull(this.multiNumericalPropagator.getFrame(STATE2));
        /*
         * Input ID is null
         */
        boolean testOk = false;
        try {
            this.multiNumericalPropagator.addInitialState(this.state1, null);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.PDB_NULL_STATE_ID.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * Input ID is empty
         */
        testOk = false;
        try {
            this.multiNumericalPropagator.addInitialState(this.state1, "");
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.PDB_NULL_STATE_ID.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * Input initial state date does not correspond with the ones already added
         */
        // second state with different date
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final Orbit orbitWithDiffDate = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getGCRF(), this.initialDate.shiftedBy(100), this.mu);
        final SpacecraftState stateWithFiffDate = new SpacecraftState(orbitWithDiffDate);
        testOk = false;
        try {
            this.multiNumericalPropagator.addInitialState(stateWithFiffDate, STATE3);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
        }
        Assert.assertTrue(testOk);

        /*
         * Input initial state already added to the propagator
         */
        testOk = false;
        try {
            this.multiNumericalPropagator.addInitialState(this.state1, STATE1);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
        }
        Assert.assertTrue(testOk);

    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#METHODS}
     *
     * @testedMethod {@link MultiNumericalPropagator#setAttitudeProvider(AttitudeProvider, String)}
     * @testedMethod {@link MultiNumericalPropagator#setAttitudeProviderForces(AttitudeProvider, String)}
     * @testedMethod {@link MultiNumericalPropagator#setAttitudeProviderEvents(AttitudeProvider, String)}
     * @testedMethod {@link MultiNumericalPropagator#getAttitudeProvider(String)}
     * @testedMethod {@link MultiNumericalPropagator#getAttitudeProviderForces(String)}
     * @testedMethod {@link MultiNumericalPropagator#getAttitudeProviderEvents(String)}
     *
     * @description Test the exceptions raised by setAttitudeProvider* methods Test
     *              getAttitudeProviders
     *
     * @input a propagator with AttitudeProvider defined in a wrong way
     *
     * @output an exception
     *
     * @testPassCriteria an exception should be raised
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSetAttitudeProviderExceptions() throws PatriusException {
        final AttitudeProvider provider1 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(true, 0.1,
            0.1, 0.5, 0.3));
        final AttitudeEquation eqsProviderForces = new AttitudeEquation(AttitudeType.ATTITUDE_FORCES){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) {
                // nothing to do
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
        };

        final AttitudeEquation eqsProviderDefault = new AttitudeEquation(AttitudeType.ATTITUDE){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) {
                // nothing to do
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
        };

        final AttitudeEquation eqsProviderEvents = new AttitudeEquation(AttitudeType.ATTITUDE_EVENTS){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) {
                // nothing to do
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
        };

        /*
         * TEST 1 : add an attitude provider for forces computation -> A - Try to add an additional
         * equation representing the attitude for forces computation -> B - Try to add an additional
         * equation representing the attitude by default -> C - Try to add an attitude provider by
         * default
         */
        MultiAttitudeProvider forcesProvider = this.multiNumericalPropagator.getAttitudeProviderForces(STATE1);
        Assert.assertNull(forcesProvider);
        forcesProvider = this.multiNumericalPropagator.getAttitudeProvider(STATE1);
        Assert.assertNull(forcesProvider);
        MultiAttitudeProvider eventsProvider = this.multiNumericalPropagator.getAttitudeProvider(STATE2);
        Assert.assertNotNull(eventsProvider);

        this.multiNumericalPropagator.setAttitudeProviderForces(provider1, STATE1);
        forcesProvider = this.multiNumericalPropagator.getAttitudeProvider(STATE1);
        Assert.assertNotNull(forcesProvider);

        boolean testOk = false;
        // 1-A the test should fail because a force attitude provider is already defined in the
        // propagator:
        try {
            this.multiNumericalPropagator.setAttitudeProvider(new BodyCenterPointing(), STATE1);
            this.multiNumericalPropagator.addAttitudeEquation(eqsProviderForces, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertTrue(true);
        }

        forcesProvider = this.multiNumericalPropagator.getAttitudeProviderForces(STATE1);
        Assert.assertNotNull(forcesProvider);
        eventsProvider = this.multiNumericalPropagator.getAttitudeProviderEvents(STATE1);
        Assert.assertNull(eventsProvider);

        testOk = false;
        // 1-B the test should fail because a two attitudes treatment is expected
        try {
            this.multiNumericalPropagator.setAttitudeProvider(new BodyCenterPointing(), STATE1);
            this.multiNumericalPropagator.addAttitudeEquation(eqsProviderDefault, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertTrue(true);
        }

        testOk = false;
        // 1-C the test should fail because a two attitudes treatment is expected
        try {
            this.multiNumericalPropagator.setAttitudeProvider(new BodyCenterPointing(), STATE1);
            this.multiNumericalPropagator.setAttitudeProvider(provider1, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertTrue(true);
        }

        /*
         * TEST 2 : add an attitude provider for events computation -> A - Try to add an additional
         * equation representing the attitude for events computation
         */

        this.multiNumericalPropagator.setAttitudeProviderEvents(provider1, STATE1);

        testOk = false;
        // the test should fail because an events attitude provider is already defined in the
        // propagator:
        try {
            this.multiNumericalPropagator.setAttitudeProvider(new BodyCenterPointing(), STATE1);
            this.multiNumericalPropagator.addAttitudeEquation(eqsProviderEvents, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertTrue(true);
        }
        // eventsProvider = multiNumericalPropagator.getAttitudeProviderEvents(STATE1);
        // Assert.assertNotNull(eventsProvider);

        /*
         * TEST 3 : an attitude provider was added by default in setUp method (STATE2) -> A - Try to
         * add an additional equation representing the attitude by default -> B - Try to add an
         * attitude provider for forces computation
         */

        testOk = false;
        // the test should fail because an attitude provider by default is already defined in
        // the propagator:
        try {
            this.multiNumericalPropagator.addAttitudeEquation(eqsProviderDefault, STATE2);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // the test should fail because a single attitude treatment is expected

        // Create a clean propagator for another series of exception tests:
        this.setUp();

        /*
         * TEST 4 : add additional equation representing the attitude for forces computation -> A -
         * Try to add an additional equation representing the attitude for forces computation -> B -
         * Try to add a default attitude provider
         */
        this.multiNumericalPropagator.addAttitudeEquation(eqsProviderForces, STATE1);

        testOk = false;
        // the test should fail because the force attitude equation is already defined in
        // the propagator:

        testOk = false;
        // the test should fail because the force attitude equation is already defined in
        // the propagator:

        // Assert.assertTrue(testOk);

        /*
         * TEST 5 : add additional equation representing the attitude for events computation -> A -
         * Try to add an additional equation representing the attitude for events computation
         */
        this.multiNumericalPropagator.addAttitudeEquation(eqsProviderEvents, STATE1);
        testOk = false;
        // the test should fail because the events attitude equation is already defined in
        // the propagator:

        // Assert.assertTrue(testOk);
        // forcesProvider = multiNumericalPropagator.getAttitudeProviderForces(STATE1);
        // Assert.assertNull(forcesProvider);
        // eventsProvider = multiNumericalPropagator.getAttitudeProviderEvents(STATE1);
        // Assert.assertNull(eventsProvider);

        // Create a clean propagator for another series of exception tests:
        this.setUp();

        /*
         * TEST 6 : add additional equation representing the attitude by default -> A - Try to add
         * an attitude provider representing the attitude for forces computation -> B - Try to add
         * an attitude provider representing the attitude for events computation -> C - Try to add
         * an attitude provider representing the attitude by default -> D - Try to add an additional
         * equation representing the attitude for forces computation -> E - Try to add an additional
         * equation representing the attitude for events computation
         */
        this.multiNumericalPropagator.addAttitudeEquation(eqsProviderDefault, STATE1);
        testOk = false;
        // A - the test should fail because a single attitude treatment is expected:
        try {
            this.multiNumericalPropagator.setAttitudeProviderForces(provider1, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // B - the test should fail because a single attitude treatment is expected:
        try {
            this.multiNumericalPropagator.setAttitudeProviderEvents(provider1, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // C - the test should fail because an additional equation representing the attitude
        // by default is already defined
        testOk = false;
        try {
            this.multiNumericalPropagator.setAttitudeProvider(provider1, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // D - the test should fail because a single attitude treatment is expected
        testOk = false;
        try {
            this.multiNumericalPropagator.addAttitudeEquation(eqsProviderForces, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // E - the test should fail because a single attitude treatment is expected
        testOk = false;
        try {
            this.multiNumericalPropagator.addAttitudeEquation(eqsProviderEvents, STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * TEST 6 : Try to add an attitude provider associated with a non state non defined
         */
        testOk = false;
        try {
            this.multiNumericalPropagator.setAttitudeProvider(provider1, DEFAULT);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
        }
        Assert.assertTrue(testOk);

    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#METHODS}
     *
     * @testedMethod {@link MultiNumericalPropagator#setAdditionalStateTolerance(String, double[], double[], String)}
     *
     * @description Test selectEquationsAndTolerances exception
     *
     * @input a propagator with a wrong additional state name
     *
     * @output an exception
     *
     * @testPassCriteria an exception should be raised
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testAddStatesExceptions() throws PatriusException {
        final String aeqName = "bogus";
        final double[] aeqState = { 0., 0. };
        final AdditionalEquations aeq = new AdditionalEquations(){
            /** Serializable UID. */
            private static final long serialVersionUID = 5323096745041968839L;

            @Override
            public String getName() {
                return aeqName;
            }

            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                // does nothing.
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

            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };

        // add additional state
        this.multiNumericalPropagator.addInitialState(this.state2.addAdditionalState(aeqName, aeqState), STATE3);

        // Add the equation
        this.multiNumericalPropagator.addAdditionalEquations(aeq, STATE3);

        // After this, tolerances may be set

        // Exception : tolerance arrays too big
        final double[] okay = { 1., 1. };
        final double[] tooBig = { 1., 1., 1. };
        try {
            this.multiNumericalPropagator.setAdditionalStateTolerance(aeqName, okay, tooBig, STATE3);
            this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(1000.));
            Assert.fail();
        } catch (final PatriusException e) {
            final String expectedMessage = new PatriusException(PatriusMessages.ADDITIONAL_STATE_WRONG_TOLERANCES_SIZE)
                .getLocalizedMessage();
            Assert.assertEquals(expectedMessage, e.getLocalizedMessage());
        }
        // Exception : tolerance arrays too small
        final double[] tooSmall = { 1. };
        try {
            // add additional state
            this.multiNumericalPropagator.addInitialState(this.state2.addAdditionalState(aeqName, aeqState), "state4");
            this.multiNumericalPropagator.addAdditionalEquations(aeq, "state4");
            this.multiNumericalPropagator.setAdditionalStateTolerance(aeqName, tooSmall, okay, "state4");
            this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(1000.));
            Assert.fail();
        } catch (final PatriusException e) {
            final String expectedMessage = new PatriusException(PatriusMessages.ADDITIONAL_STATE_WRONG_TOLERANCES_SIZE)
                .getLocalizedMessage();
            Assert.assertEquals(expectedMessage, e.getLocalizedMessage());
        }

        // Exception : wrong name for additional state
        try {
            this.multiNumericalPropagator.setAdditionalStateTolerance("ploc", new double[] { 0.1, 0., }, okay, STATE1);
            Assert.fail();
        } catch (final PatriusException e) {
            final String expectedMessage = new PatriusException(PatriusMessages.UNKNOWN_ADDITIONAL_EQUATION, "ploc")
                .getLocalizedMessage();
            Assert.assertEquals(expectedMessage, e.getLocalizedMessage());
        }

        // Exceptions raised by checkStatesEquations()
        this.setUp();
        boolean testOk = false;
        // the test should fail because the additional states or the additional equations are
        // empty:
        try {
            this.multiNumericalPropagator.addInitialState(
                this.state2.addAdditionalState("New State", new double[] { 0.0, 0.0 }), STATE3);
            this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(1000.));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert
                .assertEquals(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // the test should fail because the additional states number does not correspond to
        // the additional equations number:
        try {
            this.multiNumericalPropagator.addAdditionalEquations(aeq, STATE3);
            this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(1000.));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert
                .assertEquals(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        this.multiNumericalPropagator.addAdditionalEquations(new AdditionalEquations(){
            @Override
            public String getName() {
                return "New Equation";
            }

            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                // nothing to do
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

            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        }, STATE3);

        this.multiNumericalPropagator.setMassProviderEquation(this.defaultMassModel, STATE3);
        testOk = false;
        // the test should fail because the additional states names do not correspond to
        // the additional equations names:
        try {
            this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(1000.));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert
                .assertEquals(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link MultiNumericalPropagator#getInitialStates()}
     *
     * @description Test no extrapolation
     *
     * @input A multi numerical propagator with initial states
     *
     * @output the initial states
     *
     * @testPassCriteria The final states should be equal to the initial states
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testNoExtrapolation() throws PatriusException {
        // Propagate of the initial states at the initial date
        final Map<String, SpacecraftState> finalStates = this.multiNumericalPropagator.propagate(this.initialDate);

        // Final orbit definition
        final Orbit finalOrbit1 = finalStates.get(STATE1).getOrbit();
        final Orbit finalOrbit2 = finalStates.get(STATE2).getOrbit();

        // Test getInitialStates
        Assert.assertEquals(2, this.multiNumericalPropagator.getInitialStates().size());

        // Check results
        Assert.assertEquals(this.orbit1.getA(), finalOrbit1.getA(), E_14);
        Assert.assertEquals(this.orbit1.getEquinoctialEx(), finalOrbit1.getEquinoctialEx(), E_14);
        Assert.assertEquals(this.orbit1.getEquinoctialEy(), finalOrbit1.getEquinoctialEy(), E_14);
        Assert.assertEquals(this.orbit1.getHx(), finalOrbit1.getHx(), E_14);
        Assert.assertEquals(this.orbit1.getHy(), finalOrbit1.getHy(), E_14);
        Assert.assertEquals(this.orbit1.getLM(), finalOrbit1.getLM(), E_14);
        Assert.assertEquals(this.initialDate, finalOrbit1.getDate());

        Assert.assertEquals(this.orbit2.getA(), finalOrbit2.getA(), E_14);
        Assert.assertEquals(this.orbit2.getEquinoctialEx(), finalOrbit2.getEquinoctialEx(), E_14);
        Assert.assertEquals(this.orbit2.getEquinoctialEy(), finalOrbit2.getEquinoctialEy(), E_14);
        Assert.assertEquals(this.orbit2.getHx(), finalOrbit2.getHx(), E_14);
        Assert.assertEquals(this.orbit2.getHy(), finalOrbit2.getHy(), E_14);
        Assert.assertEquals(this.orbit2.getLM(), finalOrbit2.getLM(), E_14);
        Assert.assertEquals(this.initialDate, finalOrbit2.getDate());

        // Test initial state not specified
        this.multiNumericalPropagator = new MultiNumericalPropagator(this.integratorMultiSat);
        boolean testOk = false;
        try {
            this.multiNumericalPropagator.propagate(this.finalDate);
            Assert.fail();
        } catch (final PropagationException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        try {
            this.multiNumericalPropagator.propagate(this.initialDate, this.finalDate);
            Assert.fail();
        } catch (final PropagationException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Test integrator is null
        testOk = false;
        try {
            this.multiNumericalPropagator = new MultiNumericalPropagator(null);
            Assert.fail();
        } catch (final PropagationException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION}
     *
     * @testedMethod {@link MultiNumericalPropagator#setAttitudeProvider}
     *
     * @description FA418 It should be possible to define a new AttitudeProvider to the multi
     *              propagator after a first propagation
     *
     * @input an initial multi propagator defined with - two initial states - a single attitude
     *        provider defined for the first state - two attitude providers defined for the second
     *        state These attitude providers are updated after a first propagation.
     *
     * @output no exceptions raised
     *
     * @testPassCriteria everything goes well
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testAttitudeProviderTreatment() throws PatriusException {
        // A single attitude provider was already given to the multi propagator for the second state
        // Give two attitude providers to the multi propagator for the first state
        final AttitudeProvider provider1 = new LofOffset(FramesFactory.getEME2000(), LOFType.TNW);
        final AttitudeProvider provider2 = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);
        final MultiAttitudeProviderWrapper wrapperEv = new MultiAttitudeProviderWrapper(provider2, STATE1);
        this.multiNumericalPropagator.setAttitudeProviderEvents(wrapperEv, STATE1);
        Assert.assertNotNull(this.multiNumericalPropagator.getAttitudeProvider(STATE1));
        final MultiAttitudeProviderWrapper wrapperFor = new MultiAttitudeProviderWrapper(provider1, STATE1);
        this.multiNumericalPropagator.setAttitudeProviderForces(wrapperFor, STATE1);
        this.multiNumericalPropagator.setAttitudeProvider(this.attProv, STATE2);
        // First propagation
        this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(10.));
        Assert.assertEquals(wrapperFor.hashCode(), this.multiNumericalPropagator.getAttitudeProviderForces(STATE1)
            .hashCode());
        Assert.assertEquals(wrapperEv.hashCode(), this.multiNumericalPropagator.getAttitudeProviderEvents(STATE1)
            .hashCode());
        Assert.assertEquals(this.attProv.hashCode(), ((MultiAttitudeProviderWrapper) this.multiNumericalPropagator
            .getAttitudeProvider(STATE2)).getAttitudeProvider().hashCode());

        // Change the single attitude provider defined
        final AttitudeProvider provider3 = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        final AttitudeProvider provider4 = new ConstantAttitudeLaw(FramesFactory.getICRF(), Rotation.IDENTITY);
        final AttitudeProvider provider5 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        final MultiAttitudeProviderWrapper wrapperFor2 = new MultiAttitudeProviderWrapper(provider3, STATE1);
        this.multiNumericalPropagator.setAttitudeProviderForces(wrapperFor2, STATE1);
        final MultiAttitudeProviderWrapper wrapperEv2 = new MultiAttitudeProviderWrapper(provider4, STATE1);
        this.multiNumericalPropagator.setAttitudeProviderEvents(wrapperEv2, STATE1);
        this.multiNumericalPropagator.setAttitudeProvider(provider5, STATE2);

        // Second propagation
        this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(20.));
        Assert.assertEquals(wrapperFor2.hashCode(), this.multiNumericalPropagator.getAttitudeProviderForces(STATE1)
            .hashCode());
        Assert.assertEquals(wrapperEv2.hashCode(), this.multiNumericalPropagator.getAttitudeProviderEvents(STATE1)
            .hashCode());
        Assert.assertEquals(provider5.hashCode(), ((MultiAttitudeProviderWrapper) this.multiNumericalPropagator
            .getAttitudeProvider(STATE2)).getAttitudeProvider().hashCode());

        // Other checks
        Assert.assertEquals(STATE1, wrapperEv2.getID());
    }

    /**
     *
     * Propagation of two simple SpacecraftState in slave mode: Comparison between two numerical
     * propagation and one multi-sat numerical propagation
     *
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_MODE}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     *
     * @description Propagation in slave mode of a simple SpacecraftState composed of an orbit and a
     *              simple mass provider.
     *
     * @input two Numerical propagator and one multi-sat numerical propagator
     *
     * @output final equinoctial parameters from multi-sat propagation
     *
     * @testPassCriteria final equinoctial parameters from numerical propagation and from multi-sat
     *                   propagation should be equal.
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSlaveMode() throws PatriusException {

        /*
         * TWO PROPAGATIONS WITH TWO NUMERICAL PROPAGATOR
         */
        final SpacecraftState finalState1 = this.firstPropagator.propagate(this.finalDate);
        final SpacecraftState finalState2 = this.secondPropagator.propagate(this.finalDate);

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT PROPAGATOR
         */
        final Map<String, SpacecraftState> finalMultiSatState = this.multiNumericalPropagator.propagate(this.finalDate);

        Assert.assertEquals(finalMultiSatState.get(STATE1).getA(), finalState1.getA(), 2E-8);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getEquinoctialEx(), finalState1.getEquinoctialEx(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getEquinoctialEy(), finalState1.getEquinoctialEy(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getHx(), finalState1.getHx(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getHy(), finalState1.getHy(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getLM(), finalState1.getLM(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getMass(DEFAULT), finalState1.getMass(DEFAULT), E_14);

        Assert.assertEquals(finalMultiSatState.get(STATE2).getA(), finalState2.getA(), 3E-8);
        Assert.assertEquals(finalMultiSatState.get(STATE2).getEquinoctialEx(), finalState2.getEquinoctialEx(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE2).getEquinoctialEy(), finalState2.getEquinoctialEy(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE2).getHx(), finalState2.getHx(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE2).getHy(), finalState2.getHy(), E_14);
        // Error due to the AdaptativeStepSizeIntegrator
        // No forces models added => low error
        Assert.assertEquals(finalMultiSatState.get(STATE2).getLM(), finalState2.getLM(), 2E-13);

        Assert.assertEquals(this.multiNumericalPropagator.getMode(), MultiPropagator.SLAVE_MODE);

        // Test exception raised by getGeneratedEphemeriq
        boolean testOk = false;
        // 1-A the test should fail because a force attitude provider is already defined in
        // the propagator:
        try {
            this.multiNumericalPropagator.getGeneratedEphemeris(STATE1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

    }

    /**
     *
     * Propagation of two simple SpacecraftState in ephemeris mode: Comparison between ephemeris
     * generated during simple numerical propagation and during multi-sat numerical propagation
     *
     * @throws PatriusException if position cannot be computed in given frame
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_MODE}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link MultiNumericalPropagator#setEphemerisMode()}
     * @testedMethod {@link MultiNumericalPropagator#getGeneratedEphemeris(String)}
     *
     * @description Propagation in ephemeris mode of a simple SpacecraftState composed of an orbit
     *              and a simple mass provider.
     *
     * @input a numerical propagator and one multi-sat numerical propagator
     *
     * @output ephemeris generated during simple numerical propagation and during multi-sat
     *         numerical propagation
     *
     * @testPassCriteria position/velocity from ephemeris generated during simple numerical
     *                   propagation and during multi-sat numerical propagation should be equal.
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testEphemerisMode() throws PatriusException {
        final double dt = 3200.;

        // Set ephemeris mode
        this.multiNumericalPropagator.setEphemerisMode();
        this.firstPropagator.setEphemerisMode();

        this.firstPropagator.propagate(this.initialDate.shiftedBy(dt));

        final BoundedPropagator ephemeris = this.firstPropagator.getGeneratedEphemeris();

        final PVCoordinates pv = ephemeris.getPVCoordinates(this.initialDate.shiftedBy(0.5 * dt),
            FramesFactory.getEME2000());
        final Vector3D pos = pv.getPosition();
        final Vector3D vel = pv.getVelocity();

        this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(dt));

        final BoundedPropagator ephemerisMultiSat = this.multiNumericalPropagator.getGeneratedEphemeris(STATE1);
        Assert.assertEquals(this.initialDate, ephemerisMultiSat.getMinDate());
        Assert.assertEquals(this.initialDate.shiftedBy(dt), ephemerisMultiSat.getMaxDate());

        final PVCoordinates pvMultiSat = ephemerisMultiSat.getPVCoordinates(this.initialDate.shiftedBy(0.5 * dt),
            FramesFactory.getEME2000());
        final Vector3D posMultiSat = pvMultiSat.getPosition();
        final Vector3D velMultiSat = pvMultiSat.getVelocity();

        checkVectors(pos, posMultiSat, new Vector3D(3E-14, E_14, E_14));
        checkVectors(vel, velMultiSat, new Vector3D(E_14, 2E-14, E_14));

        Assert.assertEquals(this.multiNumericalPropagator.getMode(), MultiPropagator.EPHEMERIS_GENERATION_MODE);
    }

    /**
     * Propagation of two simple SpacecraftState in master mode: Propagate two circular orbit with
     * multi-sat numerial propagator. Check that the mean latitude argument delta between both orbit
     * remain equal during propagation using a step handler.
     *
     * @throws PropagationException thrown during propagation
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_MODE}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link MultiNumericalPropagator#setMasterMode(double, MultiSatOrekitFixedStepHandler)}
     *
     * @input two SpacecraftState defined using circular orbits, a MultiSatOrekitFixedStepHandler
     *
     * @output mean latitude argument delta
     *
     * @testPassCriteria the mean latitude argument delta between both orbit remain equal during
     *                   propagation using a step handler
     *
     * @referenceVersion 2.4
     *
     * @nonRegressionVersion 2.4
     */
    @Test
    public void testMasterMode() throws PropagationException {
        // Set master mode
        class MyMultiOrekitFixedStepHandler implements MultiPatriusFixedStepHandler {
            /** Serializable UID. */
            private static final long serialVersionUID = 2151689080824095205L;
            int count;

            @Override
            public void init(final Map<String, SpacecraftState> s0,
                             final AbsoluteDate t) {
                this.count = 0;
            }

            @Override
            public void handleStep(final Map<String, SpacecraftState> currentState,
                                   final boolean isLast) throws PropagationException {
                this.count++;
            }

            public int getCount() {
                return this.count;
            }
        }

        class MyOrekitFixedStepHandler implements PatriusFixedStepHandler {
            /** Serializable UID. */
            private static final long serialVersionUID = 5003349266622633767L;
            public int count;

            @Override
            public void init(final SpacecraftState s0,
                             final AbsoluteDate t) {
                this.count = 0;
            }

            @Override
            public void handleStep(final SpacecraftState currentState,
                                   final boolean isLast) throws PropagationException {
                this.count++;
            }

            public int getCount() {
                return this.count;
            }
        }

        final double dt = 300.;
        final MyMultiOrekitFixedStepHandler multiSatStepHandler = new MyMultiOrekitFixedStepHandler();
        final MyOrekitFixedStepHandler stepHandler = new MyOrekitFixedStepHandler();
        this.multiNumericalPropagator.setMasterMode(dt, multiSatStepHandler);
        this.firstPropagator.setMasterMode(dt, stepHandler);

        /*
         * NUMERICAL PROPAGATOR
         */
        final double a = 15;
        this.firstPropagator.propagate(this.initialDate.shiftedBy(dt * a));
        this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(dt * a));

        Assert.assertEquals(multiSatStepHandler.getCount(), stepHandler.getCount());
        Assert.assertEquals(this.multiNumericalPropagator.getMode(), MultiPropagator.MASTER_MODE);
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @throws PatriusException
     *
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link MultiNumericalPropagator#getPVCoordinates(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame, String)}
     * @testedMethod {@link MultiNumericalPropagator#setOrbitType(OrbitType)}
     * @testedMethod {@link MultiNumericalPropagator#setPositionAngleType(PositionAngle)}
     * @testedMethod {@link MultiNumericalPropagator#getOrbitType()}
     * @testedMethod {@link MultiNumericalPropagator#getPositionAngleType()}
     *
     * @description Test propagations in different orbit types
     *
     * @input propagator with defined orbit integration types
     *
     * @output cartesian parameters
     *
     * @testPassCriteria output orbital parameters from NumericalPropagation et
     *                   MultiNumericalPropagation should be equal.
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testPropInOrbitType() throws PatriusException, IOException, ParseException {
        this.propagateInType(OrbitType.CARTESIAN, PositionAngle.MEAN);
        this.propagateInType(OrbitType.CIRCULAR, PositionAngle.MEAN);
        this.propagateInType(OrbitType.EQUINOCTIAL, PositionAngle.MEAN);
        this.propagateInType(OrbitType.KEPLERIAN, PositionAngle.MEAN);

        this.propagateInType(OrbitType.CARTESIAN, PositionAngle.ECCENTRIC);
        this.propagateInType(OrbitType.CIRCULAR, PositionAngle.ECCENTRIC);
        this.propagateInType(OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC);
        this.propagateInType(OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC);

        this.propagateInType(OrbitType.CARTESIAN, PositionAngle.TRUE);
        this.propagateInType(OrbitType.CIRCULAR, PositionAngle.TRUE);
        this.propagateInType(OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        this.propagateInType(OrbitType.KEPLERIAN, PositionAngle.TRUE);
    }

    private void propagateInType(final OrbitType type,
                                 final PositionAngle angle) throws PatriusException {
        this.setUp();
        final Vector3D vectorE_14 = new Vector3D(E_14, E_14, E_14);

        final int step = 60;
        final RungeKuttaIntegrator rki1 = new ClassicalRungeKuttaIntegrator(step);
        final RungeKuttaIntegrator rki = new ClassicalRungeKuttaIntegrator(step);

        this.firstPropagator = new NumericalPropagator(rki1, this.state1.getFrame(), type, angle);
        this.multiNumericalPropagator = new MultiNumericalPropagator(rki, new HashMap<String, Frame>(), type, angle);

        // Add initial state
        this.firstPropagator.setInitialState(this.state1);
        this.multiNumericalPropagator.addInitialState(this.state1, STATE1);

        // Add Newtonian attraction
        this.firstPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state1.getMu())));
        this.multiNumericalPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state1
            .getMu())), STATE1);

        // Add additional equation associated with the mass provider of the first state
        this.firstPropagator.setMassProviderEquation(this.defaultMassModel);
        final double[] absT = { 0.01 };
        final double[] relT = { 1.0e-7 };
        final String eqName = this.defaultMassModel.getAdditionalEquation(DEFAULT).getName();
        this.firstPropagator.setAdditionalStateTolerance(eqName, absT, relT);
        this.multiNumericalPropagator.setMassProviderEquation(this.defaultMassModel, STATE1);
        this.multiNumericalPropagator.setAdditionalStateTolerance(eqName, absT, relT, STATE1);

        final PVCoordinates finalPV = this.firstPropagator.getPVCoordinates(this.finalDate, FramesFactory.getGCRF());
        final Vector3D finalPos = finalPV.getPosition();
        final Vector3D finalVel = finalPV.getVelocity();

        final PVCoordinates finalPVMultiSat = this.multiNumericalPropagator.getPVCoordinates(this.finalDate,
            FramesFactory.getGCRF(), STATE1);
        final Vector3D finalPosMultiSat = finalPVMultiSat.getPosition();
        final Vector3D finalVelMultiSat = finalPVMultiSat.getVelocity();

        checkVectors(finalPos, finalPosMultiSat, vectorE_14);
        checkVectors(finalVel, finalVelMultiSat, vectorE_14);

        Assert.assertEquals(this.multiNumericalPropagator.getOrbitType(), type);
        Assert.assertEquals(this.multiNumericalPropagator.getPositionAngleType(), angle);
    }

    /**
     * Tests multi-sat numerical propagation with several forces model. Tests that MU defined for
     * Droziner attraction model is taken into account for propagation.
     *
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link MultiNumericalPropagator#addForceModel(ForceModel, String)}
     * @testedMethod {@link MultiNumericalPropagator#getMu(String)}
     * @testedMethod {@link MultiNumericalPropagator#removeForceModels()}
     * @testedMethod {@link MultiNumericalPropagator#getForceModels(String)}
     *
     * @input a numerical propagator and one multi-sat numerical propagator with the same forces
     *        added RK integrator Complete force model without srp
     *
     * @output final equinoctial parameters from multi-sat propagation
     *
     * @testPassCriteria Final equinoctial parameters from numerical propagation and from multi-sat
     *                   propagation should be equal.
     *
     * @comments MU from the initial orbit and from gravity model should particulary be checked.
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testForcesModels() throws IOException, ParseException, PatriusException {
        /*
         * Forces model initializations
         */

        // a) gravitational force

        // add a reader for gravity fields
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr'
        // file
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // we get the data as extracted from the file
        final int n = 60;
        final int m = 60;
        final double[][] C = provider.getC(n, m, false);
        final double[][] S = provider.getS(n, m, false);

        // return perturbing force (ITRF central body frame)
        final DrozinerGravityModel earthGravityModel = new DrozinerGravityModel(FramesFactory.getITRF(), provider.getAe(),
            provider.getMu(), C, S);
        earthGravityModel.setCentralTermContribution(false);
        final ForceModel potential = new DirectBodyAttraction(earthGravityModel);

        // b) Third Body attraction
        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SUN);

        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);
        final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);

        final GravityModel sunGravityModel = sun.getGravityModel();
        ((AbstractHarmonicGravityModel) sunGravityModel).setCentralTermContribution(false);
        final ForceModel sunAttraction = new ThirdBodyAttraction(sunGravityModel);
        final GravityModel moonGravityModel = moon.getGravityModel();
        ((AbstractHarmonicGravityModel) moonGravityModel).setCentralTermContribution(false);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moonGravityModel);

        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(provider.getMu()));

        // // c) Solar radiation pressure
        // final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46,
        // 1.0 / 298.25765, FramesFactory.getITRF());
        // final RadiationSensitive vehicle = new OrekitSphericalSpacecraft(10., 2.2, 1., 0., 0.,
        // DEFAULT);
        // final ForceModel srp = new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
        // vehicle);

        // d) drag force
        final Frame itrf = FramesFactory.getITRF();
        final SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Utils.ae,
            1.0 / 298.257222101, itrf), 0.0004, 42000.0, 7500.0);
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 1., 0., 0., DEFAULT);
        final DragForce drag = new DragForce(atm, spacecraft);

        /*
         * ADD FORCES MODELS
         */
        final int step = 60;
        final RungeKuttaIntegrator rki1 = new ClassicalRungeKuttaIntegrator(step);
        final RungeKuttaIntegrator rki = new ClassicalRungeKuttaIntegrator(step);
        this.firstPropagator = new NumericalPropagator(rki1);
        this.multiNumericalPropagator = new MultiNumericalPropagator(rki);
        // Add initial state
        this.firstPropagator.setInitialState(this.state1);
        this.multiNumericalPropagator.addInitialState(this.state1, STATE1);

        // Add additional equation associated with the mass provider of the first state
        this.firstPropagator.setMassProviderEquation(this.defaultMassModel);
        final double[] absT = { 0.01 };
        final double[] relT = { 1.0e-7 };
        final String eqName = this.defaultMassModel.getAdditionalEquation(DEFAULT).getName();
        this.firstPropagator.setAdditionalStateTolerance(eqName, absT, relT);
        this.multiNumericalPropagator.setMassProviderEquation(this.defaultMassModel, STATE1);
        this.multiNumericalPropagator.setAdditionalStateTolerance(eqName, absT, relT, STATE1);

        this.firstPropagator.addForceModel(potential);
        this.multiNumericalPropagator.addForceModel(potential, STATE1);
        this.firstPropagator.addForceModel(sunAttraction);
        this.multiNumericalPropagator.addForceModel(sunAttraction, STATE1);
        this.firstPropagator.addForceModel(moonAttraction);
        this.multiNumericalPropagator.addForceModel(moonAttraction, STATE1);
        // firstPropagator.addForceModel(srp);
        // multiNumericalPropagator.addForceModel(srp, STATE1);
        this.firstPropagator.addForceModel(drag);
        this.multiNumericalPropagator.addForceModel(drag, STATE1);
        this.firstPropagator.addForceModel(newtonianAttraction);
        this.multiNumericalPropagator.addForceModel(newtonianAttraction, STATE1);

        /*
         * PROPAGATION WITH SIMPLE NUMERICAL PROPAGATOR
         */
        final AbsoluteDate end = this.initialDate.shiftedBy(86400);
        final SpacecraftState finalState1 = this.firstPropagator.propagate(end);

        /*
         * PROPAGATION WITH A MULTI-SAT PROPAGATOR
         */
        final Map<String, SpacecraftState> finalMultiSatState = this.multiNumericalPropagator.propagate(end);

        Assert.assertEquals(finalMultiSatState.get(STATE1).getA(), finalState1.getA(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getEquinoctialEx(), finalState1.getEquinoctialEx(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getEquinoctialEy(), finalState1.getEquinoctialEy(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getHx(), finalState1.getHx(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getHy(), finalState1.getHy(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getLM(), finalState1.getLM(), E_14);
        Assert.assertEquals(finalMultiSatState.get(STATE1).getMass(DEFAULT), finalState1.getMass(DEFAULT), E_14);

        // Test MU for propagation is different from MU defined with the initial orbit
        Assert.assertNotSame(this.state1.getMu(), this.multiNumericalPropagator.getMu(STATE1));
        // Test MU from droziner model is equal to the propagation MU.
        Assert.assertEquals(this.multiNumericalPropagator.getMu(STATE1), provider.getMu(), 3.8e6);

        /*
         * Test getForcesModels et removeForcesModels
         */
        Assert.assertEquals(5, this.multiNumericalPropagator.getForceModels(STATE1).size());
        this.multiNumericalPropagator.removeForceModels();
        Assert.assertNull(this.multiNumericalPropagator.getForceModels(STATE1));
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION}
     *
     * @testedMethod {@link MultiNumericalPropagator#setMu(double, String)}
     * @testedMethod {@link MultiNumericalPropagator#getNewtonianAttractionForceModel(String)}
     *
     * @description Test propagation with specific newtonian attraction model
     *
     * @input a MultiNumericalPropagation with an attraction model
     *
     * @output MU
     *
     * @testPassCriteria the MU is the expected one.
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testAttractionModel() throws IOException, ParseException, PatriusException {
        // First Propagation
        this.multiNumericalPropagator.propagate(this.finalDate);
        // Get mu
        Assert.assertEquals(Utils.mu, this.multiNumericalPropagator.getMu(STATE1), E_14);
    }

    /**
     *
     * Test detection of an event from force model.
     *
     * @throws PatriusException
     * @throws IllegalArgumentException
     *
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_EVENTS}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link MultiNumericalPropagator#addForceModel(ForceModel, String)}
     *
     * @description This test is copied from {@link ConstantThrustManeuverTest#testSameBeforeFiring()} and adapted to
     *              multi-sat propagation.
     *
     * @see ConstantThrustManeuverTest#testSameBeforeFiring()
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testEventsFromForceModel() throws IllegalArgumentException, PatriusException {

        final double isp = 318;
        final double mass = 2500;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = MathLib.toRadians(-7.4978);
        final double alpha = MathLib.toRadians(351);
        final AttitudeProvider law = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(Vector3D.PLUS_I,
            new Vector3D(alpha, delta))));

        this.initialDate = new AbsoluteDate(new DateComponents(2004, 01, 01), new TimeComponents(23, 30, 00.000),
            TimeScalesFactory.getUTC());

        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getGCRF(),
            this.initialDate, this.mu);

        // Define states
        final MassProvider massModel = new SimpleMassModel(mass, DEFAULT);
        final MassProvider massModel1 = new SimpleMassModel(mass, DEFAULT);

        final SpacecraftState initialState = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
            orbit.getFrame()), massModel);
        final SpacecraftState initialState1 = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
            orbit.getFrame()), massModel1);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(04, 15,
            34.080), TimeScalesFactory.getUTC());

        final TankProperty tank = new TankProperty(mass);
        tank.setPartName(DEFAULT);
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(fireDate, duration,
            new PropulsiveProperty(f, isp), Vector3D.PLUS_I, massModel, tank);

        // Multi-sat numerical propagator definition
        this.multiNumericalPropagator = new MultiNumericalPropagator(this.integratorMultiSat);

        // Add initial state
        this.multiNumericalPropagator.addInitialState(initialState, STATE1);
        this.multiNumericalPropagator.addInitialState(initialState1, STATE2);

        // Add additional equation associated with the mass provider
        this.multiNumericalPropagator.setMassProviderEquation(massModel, STATE1);
        this.multiNumericalPropagator.setMassProviderEquation(massModel1, STATE2);

        // Add Newtonian attraction
        this.multiNumericalPropagator.addForceModel(
            new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())), STATE1);
        this.multiNumericalPropagator.addForceModel(
            new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())), STATE2);

        // Add force model to the second state
        this.multiNumericalPropagator.addForceModel(maneuver, STATE2);

        // Add attitude law
        this.multiNumericalPropagator.setAttitudeProvider(law, STATE1);
        this.multiNumericalPropagator.setAttitudeProvider(law, STATE2);

        for (AbsoluteDate t = this.initialDate; t.durationFrom(fireDate) < 8000; t = t.shiftedBy(600)) {
            final Map<String, SpacecraftState> finalStates = this.multiNumericalPropagator.propagate(t);
            final PVCoordinates with = finalStates.get(STATE2).getPVCoordinates();
            final PVCoordinates without = finalStates.get(STATE1).getPVCoordinates();

            if (t.compareTo(fireDate) < 0) {
                Assert.assertEquals(0, new PVCoordinates(with, without).getPosition().getNorm(), E_14);
            } else {
                Assert.assertTrue(new PVCoordinates(with, without).getPosition().getNorm() > E_14);
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_EVENTS}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link MultiNumericalPropagator#addEventDetector(fr.cnes.sirius.patrius.propagation.events.EventDetector, String)}
     * @testedMethod {@link MultiNumericalPropagator#clearEventsDetectors()}
     * @testedMethod {@link MultiNumericalPropagator#getEventsDetectors()}
     * @description Test a single sat detector
     *
     * @input a Node detector + a multi propagator
     *
     * @output final states
     *
     * @testPassCriteria The ascending node is detected at the same time with the numerical
     *                   propagator and the multi numerical propagator
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSingleSatDetectors() throws PatriusException {
        // Check initial detectors number
        Assert.assertEquals(0, this.multiNumericalPropagator.getEventsDetectors().size());
        this.finalDate = this.initialDate.shiftedBy(10 * 3600);
        // add node detector
        final EventDetector firstNodeDetectorContinue = myAscNodeDetector(Action.CONTINUE, STATE1);
        EventDetector secondNodeDetectorContinue = myAscNodeDetector(Action.CONTINUE, STATE2);

        System.out.println("    =========    NUMERICAL PROPAGATION : FIRST STATE => CONTINUE     =========");
        this.firstPropagator.addEventDetector(firstNodeDetectorContinue);
        this.firstPropagator.propagate(this.finalDate);
        System.out.println("    =========    NUMERICAL PROPAGATION : SECOND STATE => CONTINUE   =========");
        this.secondPropagator.addEventDetector(secondNodeDetectorContinue);
        this.secondPropagator.propagate(this.finalDate);
        System.out.println("    =========    MULTI-SAT NUMERICAL PROPAGATION => CONTINUE   =========");
        this.multiNumericalPropagator.addEventDetector(firstNodeDetectorContinue, STATE1);
        this.multiNumericalPropagator.addEventDetector(secondNodeDetectorContinue, STATE2);
        Assert.assertEquals(2, this.multiNumericalPropagator.getEventsDetectors().size());
        this.multiNumericalPropagator.propagate(this.finalDate);

        // Test clear events detectors
        this.multiNumericalPropagator.clearEventsDetectors();
        Assert.assertEquals(0, this.multiNumericalPropagator.getEventsDetectors().size());

        this.setUp();
        this.finalDate = this.initialDate.shiftedBy(10 * 3600);

        // First detection date of the first state
        secondNodeDetectorContinue = myAscNodeDetector(Action.CONTINUE, STATE2);

        final EventDetector nodeDetectorStop = myAscNodeDetector(Action.STOP, STATE1);
        System.out.println("    =========    NUMERICAL PROPAGATION : FIRST STATE => STOP     =========");
        this.firstPropagator.addEventDetector(nodeDetectorStop);
        final SpacecraftState finalState = this.firstPropagator.propagate(this.finalDate);
        System.out.println("    =========    MULTI-SAT NUMERICAL PROPAGATION => STOP   =========");
        secondNodeDetectorContinue = myAscNodeDetector(Action.CONTINUE, STATE2);
        this.multiNumericalPropagator.addEventDetector(nodeDetectorStop, STATE1);
        this.multiNumericalPropagator.addEventDetector(secondNodeDetectorContinue, STATE2);
        final Map<String, SpacecraftState> finalStates = this.multiNumericalPropagator.propagate(this.finalDate);
        Assert.assertEquals(0., finalState.getDate().durationFrom(finalStates.get(STATE1).getDate()), 1.e-3);
    }

    /**
     * This class handles satellite equator crossing events (ascending nodes).
     *
     * @return NodeDetector
     */
    public static EventDetector myAscNodeDetector(final Action action,
                                                  final String satId) {
        final double maxcheck = 1000.0;
        final double threshold = 1.0e-3;
        final NodeDetector detector = new NodeDetector(FramesFactory.getEME2000(), NodeDetector.ASCENDING, maxcheck,
            threshold, action){
            /** Serializable UID. */
            private static final long serialVersionUID = 1189809280852001194L;

            @Override
            public Action eventOccurred(final SpacecraftState s,
                                        final boolean increasing,
                                        final boolean forward) throws PatriusException {
                eventMessage(s, true, "Ascending Node", satId);
                return super.eventOccurred(s, increasing, forward);
            }
        };
        return detector;
    }

    /**
     * Event message.
     *
     * @param s
     * @param increasing
     * @param evtName
     * @throws PatriusException
     */
    private static void eventMessage(final SpacecraftState s,
                                     final boolean increasing,
                                     final String evtName,
                                     final String satId) throws PatriusException {
        final KeplerianOrbit kep = new KeplerianOrbit(s.getOrbit());
        double aol = MathLib.toDegrees(kep.getTrueAnomaly() + kep.getPerigeeArgument());
        if (aol > 360.) {
            aol -= 360.;
        }
        System.out.format(satId + " " + s.getDate().toString(TimeScalesFactory.getTAI()) + " (AoL = %8.3f deg) "
                + (increasing ? " ORBITO -> ENTER " + evtName : " ORBITO <- EXIT " + evtName) + "%n", aol);
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_INTEGRATOR}
     *
     * @testedMethod {@link MultiNumericalPropagator#MultiNumericalPropagator(FirstOrderIntegrator)}
     *
     * @description Test integrator with the state variables tolerances represented by a scalar
     *              value Test storeDefaultTolerance private method This test is copied from a test
     *              of NumericalPropagatorTest Test exception raised is input tolerance size is
     *              different from 6.
     *
     * @input an integrator with scalar state variables tolerances
     *
     * @output a final state
     *
     * @testPassCriteria everything goes well
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testTolerances() throws PatriusException {

        final double abstolScal = 1.0e-1;
        final double reltolScal = 1.0e-2;
        final FirstOrderIntegrator integratorScal = new DormandPrince853Integrator(0.001, 200, abstolScal, reltolScal);

        final double[] abstolVec = { 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1, 1.0e-1 };
        final double[] reltolVec = { 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2, 1.0e-2 };
        final FirstOrderIntegrator integratorVec = new DormandPrince853Integrator(0.001, 200, abstolVec, reltolVec);

        final SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Utils.ae,
            1.0 / 298.257222101, FramesFactory.getITRF()), 0.0004, 42000.0, 7500.0);
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 1., 0., 0., DEFAULT);
        final DragForce drag = new DragForce(atm, spacecraft);

        // Attitude equations (for coverage purposes)
        // Add a new state with attitude equation
        final AttitudeEquation eqsProviderForces = new AttitudeEquation(AttitudeType.ATTITUDE_FORCES){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                // nothing to do
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
        };

        final AttitudeEquation eqsProviderEvents = new AttitudeEquation(AttitudeType.ATTITUDE_EVENTS){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                // nothing to do
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
        };

        // First propagation: scalar tolerance:
        MultiNumericalPropagator propagator = new MultiNumericalPropagator(integratorScal);
        final SpacecraftState s1 = new SpacecraftState(this.attProv, this.attProv, this.orbit1, this.defaultMassModel);
        Assert.assertEquals(1, s1.getAdditionalStates().size());
        propagator.addInitialState(s1, STATE1);
        propagator.setMassProviderEquation(this.defaultMassModel, STATE1);
        propagator.addAttitudeEquation(eqsProviderForces, STATE1);
        propagator.addAttitudeEquation(eqsProviderEvents, STATE1);
        propagator.addForceModel(drag, STATE1);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(s1.getMu())), STATE1);
        final double[] absT = { 5.0e-6 };
        final double[] relT = { 5.0e-7 };
        propagator.setAdditionalStateTolerance("MASS_" + DEFAULT, absT, relT, STATE1);
        // add second state with complew mass model
        propagator.addInitialState(new SpacecraftState(this.orbit2, this.complexMassModel), STATE2);
        propagator.setMassProviderEquation(this.complexMassModel, STATE2);
        propagator.setAdditionalStateTolerance("MASS_main", absT, relT, STATE2);
        propagator.setAdditionalStateTolerance("MASS_part5", absT, relT, STATE2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.orbit2.getMu())), STATE2);
        final SpacecraftState res1 = propagator.propagate(this.initialDate.shiftedBy(1E5)).get(STATE1);
        Assert.assertEquals(3, propagator.getInitialStates().get(STATE1).getAdditionalStates().size());

        // Second propagation: vector tolerance (scalar tolerance x 6):
        propagator = new MultiNumericalPropagator(integratorVec);
        propagator.addInitialState(this.state1, STATE1);
        propagator.setMassProviderEquation(this.defaultMassModel, STATE1);
        propagator.addForceModel(drag, STATE1);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state1.getMu())), STATE1);
        propagator.setAdditionalStateTolerance("MASS_" + DEFAULT, absT, relT, STATE1);
        // add second state with complew mass model
        propagator.addInitialState(new SpacecraftState(this.orbit2, this.complexMassModel), STATE2);
        propagator.setMassProviderEquation(this.complexMassModel, STATE2);
        propagator.setAdditionalStateTolerance("MASS_main", absT, relT, STATE2);
        propagator.setAdditionalStateTolerance("MASS_part5", absT, relT, STATE2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.orbit2.getMu())), STATE2);
        final SpacecraftState res2 = propagator.propagate(this.initialDate.shiftedBy(1E5)).get(STATE1);

        // Check the results of the two propagations are the same:
        Assert.assertEquals(res1.getA(), res2.getA(), 0.0);
        Assert.assertEquals(res1.getE(), res2.getE(), 0.0);
        Assert.assertEquals(res1.getLv(), res2.getLv(), 0.0);

        // Test exception
        boolean testOk = false;
        final double[] absoluteTolerance = new double[] { 0. };
        final double[] relativeTolerance = new double[] { 0. };
        // 1-A the test should fail because a force attitude provider is already defined in the
        // propagator:
        try {
            propagator.setOrbitTolerance(absoluteTolerance, relativeTolerance, STATE1);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
        }
        Assert.assertTrue(testOk);

    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_INTEGRATOR}
     *
     * @testedMethod {@link MultiNumericalPropagator #MultiNumericalPropagator(FirstOrderIntegrator)}
     * @testedMethod {@link MultiNumericalPropagator#setOrbitTolerance(double[], double[], String)}
     *
     * @description Test propagation with the state variables tolerances defined using
     *              setOrbitTolerance
     *
     * @input an integrator with scalar state variables tolerances
     *
     * @output a final state
     *
     * @testPassCriteria everything goes well
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testTolerances2() throws PatriusException {

        final double[] abstolVecA = { 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7 };
        final double[] reltolVecA = { 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7 };
        final double[] abstolVecB = { 10000000, 10000000, 10000000, 10000000, 10000000, 10000000 };
        final double[] reltolVecB = { 10000000, 10000000, 10000000, 10000000, 10000000, 10000000 };

        final FirstOrderIntegrator integratorVecA = new DormandPrince853Integrator(10, 20000, abstolVecA, reltolVecA);
        final FirstOrderIntegrator integratorVecB = new DormandPrince853Integrator(10, 20000, abstolVecB, reltolVecB);

        /*
         * Test 1 : Check that final state orbit depend on the integrator values
         */
        MultiNumericalPropagator propagatorA = new MultiNumericalPropagator(integratorVecA);
        MultiNumericalPropagator propagatorB = new MultiNumericalPropagator(integratorVecB);
        propagatorA.addInitialState(this.state2, STATE2);
        propagatorB.addInitialState(this.state2, STATE2);
        propagatorA.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state2.getMu())), STATE2);
        propagatorB.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state2.getMu())), STATE2);
        PVCoordinates resA = propagatorA.propagate(this.initialDate.shiftedBy(1E5)).get(STATE2).getPVCoordinates();
        PVCoordinates resB = propagatorB.propagate(this.initialDate.shiftedBy(1E5)).get(STATE2).getPVCoordinates();
        // Results should be different
        checkVectors(resA.getPosition(), resB.getPosition(), new Vector3D(1, 1, 1));
        checkVectors(resA.getVelocity(), resB.getVelocity(), new Vector3D(1, 1, 1));

        /*
         * Test 2 : Check that orbit tolerances given by setOrbitTolerance are taken into account
         */
        propagatorA = new MultiNumericalPropagator(integratorVecA);
        propagatorB = new MultiNumericalPropagator(integratorVecB);
        propagatorA.addInitialState(this.state2, STATE2);
        propagatorB.addInitialState(this.state2, STATE2);
        propagatorA.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state2.getMu())), STATE2);
        propagatorB.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state2.getMu())), STATE2);
        propagatorB.setOrbitTolerance(abstolVecA, reltolVecA, STATE2);
        resA = propagatorA.propagate(this.initialDate.shiftedBy(1E5)).get(STATE2).getPVCoordinates();
        resB = propagatorB.propagate(this.initialDate.shiftedBy(1E5)).get(STATE2).getPVCoordinates();
        // Results should be different
        checkVectors(resA.getPosition(), resB.getPosition(), new Vector3D(E_14, E_14, E_14));
        checkVectors(resA.getVelocity(), resB.getVelocity(), new Vector3D(E_14, E_14, E_14));
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_MODE}
     *
     * @testedMethod {@link MultiAdaptedStepHandler#init(double, double[], double)}
     *
     * @description TU for MultiAdaptedStepHandler class; check the initialization of the
     *              OrekitStepHandler (Master mode propagation) is automatically executed by the
     *              AdaptedStepHandler. These test is copied from test of NumericalPropagatorTest
     *              added for FT377.
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testMultiAdaptedStepHandler() throws PatriusException {
        // Add a new state with attitude equation
        final AttitudeEquation eqsProviderForces = new AttitudeEquation(AttitudeType.ATTITUDE){
            @Override
            public void computeDerivatives(final SpacecraftState s,
                                           final TimeDerivativesEquations adder) throws PatriusException {
                // nothing to do
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
        };
        this.multiNumericalPropagator.addInitialState(
            new SpacecraftState(this.orbit1, this.attProv.getAttitude(this.orbit1, this.initialDate,
                this.orbit1.getFrame())), STATE3);
        this.multiNumericalPropagator.addAdditionalEquations(eqsProviderForces, STATE3);

        final double duration = 100.;
        final MultiPatriusStepHandler handler = new MultiPatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6224970648766677791L;
            AbsoluteDate date;

            @Override
            public void init(final Map<String, SpacecraftState> s0,
                             final AbsoluteDate t) {
                final SpacecraftState s01 = s0.get(STATE1);
                this.date = s01.getDate();
                // Check the s0 parameters correspond to the propagation initial state:
                // Compare the date:
                Assert.assertEquals(duration, t.durationFrom(this.date), 0.0);
                // Compare the orbital parameters:
                Assert.assertEquals(MultiNumericalPropagatorTest.this.state1.getA(), s01.getA(), 0.0);
                Assert.assertEquals(MultiNumericalPropagatorTest.this.state1.getE(), s01.getE(), 0.0);
                Assert.assertEquals(MultiNumericalPropagatorTest.this.state1.getI(), s01.getI(), 0.0);
                Assert.assertEquals(MultiNumericalPropagatorTest.this.state1.getLv(), s01.getLv(), 0.0);
            }

            @Override
            public void handleStep(final MultiPatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                // Check the date during propagation:
                Assert.assertTrue(interpolator.getInterpolatedDate().durationFrom(this.date) <= duration);
            }
        };
        this.multiNumericalPropagator.setMasterMode(handler);

        final AbsoluteDate finalDate = this.initialDate.shiftedBy(duration);
        this.multiNumericalPropagator.propagate(finalDate);

        final MultiAdaptedStepHandler shandler = (MultiAdaptedStepHandler) this.integratorMultiSat.getStepHandlers()
            .toArray()[0];
        Assert.assertEquals(finalDate, shandler.getInterpolatedDate());
        Assert.assertTrue(shandler.isForward());
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#MULTI_SAT_PROPAGATION_MODEe}
     *
     * @testedMethod {@link MultiPatriusStepNormalizer #handleStep(MultiPatriusStepInterpolator, boolean)}
     *
     * @description Test retropolation in master mode.
     *
     * @input a MultiNumericalPropagator in master mode
     *
     * @output same final state as input state.
     *
     * @testPassCriteria The final state after propagation and retropropagation should be equal to
     *                   the initial state
     *
     * @comments This test is copied from testRetropolation() of NumericalPropagatorTest
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testRetropolation() throws PropagationException {

        this.multiNumericalPropagator.setMasterMode(60., new MultiPatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 352933544377706521L;

            @Override
            public void init(final Map<String, SpacecraftState> s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final Map<String, SpacecraftState> currentState,
                                   final boolean isLast) throws PropagationException {
                // System.out.println(currentState.getDate().offsetFrom(initDate,
                // TimeScalesFactory.getTAI()));
            }
        });

        final myDetector dateDet = new myDetector(this.initialDate.shiftedBy(200));
        this.multiNumericalPropagator.addEventDetector(dateDet, STATE1);

        final SpacecraftState state0 = this.multiNumericalPropagator.getInitialStates().get(STATE1);
        // extrapolation => t0 + 400
        final SpacecraftState state1 = this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(400.)).get(
            STATE1);

        // retropolation => t0
        // we should have the same position/velocity as in state0
        final SpacecraftState state2 = this.multiNumericalPropagator.propagate(this.initialDate).get(STATE1);
        Vector3D res = state2.getPVCoordinates().getPosition().subtract(state0.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        // we propagate again until t0+400
        // we should have the same position/velocity as in state1
        final SpacecraftState state3 = this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(400.)).get(
            STATE1);
        res = state3.getPVCoordinates().getPosition().subtract(state1.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        // we propagate again until t0
        // we should have the same position/velocity as in state1
        final SpacecraftState state4 = this.multiNumericalPropagator.propagate(this.initialDate).get(STATE1);
        res = state4.getPVCoordinates().getPosition().subtract(state2.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        // we propagate again until t0+400
        // we should have the same position/velocity as in state1
        final SpacecraftState state5 = this.multiNumericalPropagator.propagate(this.initialDate.shiftedBy(400.)).get(
            STATE1);
        res = state5.getPVCoordinates().getPosition().subtract(state3.getPVCoordinates().getPosition());
        Assert.assertEquals(0., res.getNorm(), 1e-6);

        Assert.assertEquals(5, dateDet.getCount());
    }

    class myDetector extends DateDetector {
        /** Serializable UID. */
        private static final long serialVersionUID = 5664878744270357057L;
        private int count = 0;

        public myDetector(final AbsoluteDate target) {
            super(target);
        }

        @Override
        public Action eventOccurred(final SpacecraftState s,
                                    final boolean increasing,
                                    final boolean forward) throws PatriusException {
            this.count++;
            return Action.CONTINUE;
        }

        private int getCount() {
            return this.count;
        }
    }

    /**
     * FA325: propagation must stop exactly at required date in slave mode.
     *
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPropagationDurationSlaveMode() throws PatriusException, IOException, ParseException {
        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 10, 10, 10, 0, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.1, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(1, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN, FramesFactory.getGCRF(),
            initialDate, 1.0);
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        final MultiNumericalPropagator propagator = new MultiNumericalPropagator(ode);
        propagator.addInitialState(new SpacecraftState(orbit), "1");
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())), "1");

        // Propagation
        final SpacecraftState finalState = propagator.propagate(finalDate).get("1");

        // Check
        Assert.assertEquals(0, finalState.getDate().durationFrom(finalDate), 0);
    }

    /**
     * FA492: propagation must stop exactly at required date in master mode.
     *
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPropagationDurationMasterMode() throws PatriusException, IOException, ParseException {
        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 10, 10, 10, 0, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.1, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(1, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN, FramesFactory.getGCRF(),
            initialDate, 1.0);
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        final MultiNumericalPropagator propagator = new MultiNumericalPropagator(ode);
        propagator.setMasterMode(0.1, new MultiPatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -8163985058108788278L;

            @Override
            public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final Map<String, SpacecraftState> currentStates,
                                   final boolean isLast) throws PropagationException {
                // Check state is correct (only anomaly is modified during this simple Keplerian
                // propagation)
                final double actual = currentStates.get("1").getLv();
                final double expected = currentStates.get("1").getDate().durationFrom(initialDate);
                Assert.assertEquals(expected, actual, 4E-12);
            }
        });
        propagator.addInitialState(new SpacecraftState(orbit), "1");

        // =============================================== FORWARD ============================= //

        // Propagation
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())), "1");
        final SpacecraftState finalState = propagator.propagate(finalDate).get("1");

        // Check final date
        Assert.assertEquals(0, finalState.getDate().durationFrom(finalDate), 0);

        // =============================================== BACKWARD ============================ //

        // Propagation
        final SpacecraftState finalStateb = propagator.propagate(initialDate).get("1");

        // Check final date
        Assert.assertEquals(0, finalStateb.getDate().durationFrom(initialDate), 0);
    }

    /**
     * FA476: propagation must stop exactly at required date in ephemeris mode.
     *
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPropagationDurationEphemerisMode() throws PatriusException, IOException, ParseException {
        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 10, 10, 10, 0, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.1, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(1, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN, FramesFactory.getGCRF(),
            initialDate, 1.0);
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        final MultiNumericalPropagator propagator = new MultiNumericalPropagator(ode, new HashMap<String, Frame>(),
            OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), "1");
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())), "1");

        // =============================================== FORWARD ============================= //

        // Propagation
        final SpacecraftState finalState = propagator.propagate(finalDate).get("1");

        // Propagation with generated ephemeris
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris("1");
        final SpacecraftState finalState2 = ephemeris.propagate(finalDate);

        // Check (final date and bulletin)
        Assert.assertEquals(0, finalState.getDate().durationFrom(finalDate), 0);
        Assert.assertEquals(0, finalState2.getDate().durationFrom(finalDate), 0);

        final PVCoordinates coord1 = finalState.getPVCoordinates();
        final PVCoordinates coord2 = finalState2.getPVCoordinates();
        final PVCoordinates coord3 = ephemeris.propagate(finalDate).getPVCoordinates();
        Assert.assertEquals(coord1.getPosition().getX(), coord2.getPosition().getX(), 0);
        Assert.assertEquals(coord1.getPosition().getY(), coord2.getPosition().getY(), 0);
        Assert.assertEquals(coord1.getPosition().getZ(), coord2.getPosition().getZ(), 0);
        Assert.assertEquals(coord1.getVelocity().getX(), coord2.getVelocity().getX(), 0);
        Assert.assertEquals(coord1.getVelocity().getY(), coord2.getVelocity().getY(), 0);
        Assert.assertEquals(coord1.getVelocity().getZ(), coord2.getVelocity().getZ(), 0);
        Assert.assertEquals(coord1.getPosition().getX(), coord3.getPosition().getX(), 0);
        Assert.assertEquals(coord1.getPosition().getY(), coord3.getPosition().getY(), 0);
        Assert.assertEquals(coord1.getPosition().getZ(), coord3.getPosition().getZ(), 0);
        Assert.assertEquals(coord1.getVelocity().getX(), coord3.getVelocity().getX(), 0);
        Assert.assertEquals(coord1.getVelocity().getY(), coord3.getVelocity().getY(), 0);
        Assert.assertEquals(coord1.getVelocity().getZ(), coord3.getVelocity().getZ(), 0);

        // =============================================== BACKWARD ============================ //

        // Propagation
        final SpacecraftState finalStateb = propagator.propagate(initialDate).get("1");

        // Propagation with generated ephemeris
        final BoundedPropagator ephemerisb = propagator.getGeneratedEphemeris("1");
        final SpacecraftState finalStateb2 = ephemerisb.propagate(initialDate);

        // Check (final date and bulletin)
        Assert.assertEquals(0, finalStateb.getDate().durationFrom(initialDate), 0);
        Assert.assertEquals(0, finalStateb2.getDate().durationFrom(initialDate), 0);

        final PVCoordinates coordb1 = finalStateb.getPVCoordinates();
        final PVCoordinates coordb2 = finalStateb2.getPVCoordinates();
        final PVCoordinates coordb3 = ephemerisb.propagate(initialDate).getPVCoordinates();
        Assert.assertEquals(coordb1.getPosition().getX(), coordb2.getPosition().getX(), 0);
        Assert.assertEquals(coordb1.getPosition().getY(), coordb2.getPosition().getY(), 0);
        Assert.assertEquals(coordb1.getPosition().getZ(), coordb2.getPosition().getZ(), 0);
        Assert.assertEquals(coordb1.getVelocity().getX(), coordb2.getVelocity().getX(), 0);
        Assert.assertEquals(coordb1.getVelocity().getY(), coordb2.getVelocity().getY(), 0);
        Assert.assertEquals(coordb1.getVelocity().getZ(), coordb2.getVelocity().getZ(), 0);
        Assert.assertEquals(coordb1.getPosition().getX(), coordb3.getPosition().getX(), 0);
        Assert.assertEquals(coordb1.getPosition().getY(), coordb3.getPosition().getY(), 0);
        Assert.assertEquals(coordb1.getPosition().getZ(), coordb3.getPosition().getZ(), 0);
        Assert.assertEquals(coordb1.getVelocity().getX(), coordb3.getVelocity().getX(), 0);
        Assert.assertEquals(coordb1.getVelocity().getY(), coordb3.getVelocity().getY(), 0);
        Assert.assertEquals(coordb1.getVelocity().getZ(), coordb3.getVelocity().getZ(), 0);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link MultiNumericalPropagator#setOrbitFrame(String, Frame)}
     *
     * @description This test aims at verifying that an exception is risen if a non pseudo-inertial
     *              or non inertial frame is provided for propagation
     *
     * @input KeplerianOrbit(1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
     * @input MultiNumericalPropagator
     * @input Frame provided for propagation : TIRF
     *
     * @output Expected an OrekitException
     * @throws PatriusException
     * @testPassCriteria An OrekitException must be caught
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testSetNonInertialFrame() throws PatriusException {
        // Propagator
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        Map<String, Frame> propFrameMap = new HashMap<String, Frame>();
        propFrameMap.put("1", FramesFactory.getTIRF());

        // An exception should occur here !
        new MultiNumericalPropagator(ode, propFrameMap);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     *
     * @description This test aims at verifying that an exception is risen if the non
     *              pseudo-inertial or non inertial orbit's frame is used for propagation
     *
     * @input KeplerianOrbit(1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
     * @input MultiNumericalPropagator
     * @input Frame ITRF is used to define the orbit and then for propagation
     *
     * @output Expected an OrekitException
     * @throws PatriusException
     * @testPassCriteria An OrekitException must be caught
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testPropagateWithNonInertialFrame() throws PatriusException {

        // Initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2010, 10, 10, 10, 0, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.1, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(1, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN, FramesFactory.getITRF(),
            initialDate, 1.0);

        // Propagator
        final FirstOrderIntegrator ode = new DormandPrince853Integrator(0.1, 10.0, 1.0e-3, 1e-6);
        final MultiNumericalPropagator propagator = new MultiNumericalPropagator(ode);
        propagator.addInitialState(new SpacecraftState(orbit), "1");

        propagator.propagate(finalDate).get("1");
    }

    /**
     * @throws PatriusException
     * @testType VT
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     *
     * @description This test is up to ensure that the propagation of an orbit in a different frame
     *              from the one in which the orbit is defined (not necessary inertial or
     *              pseudo-inertial) provide the same final state. It is an extension of VT written
     *              in the class NumericalPropagator since two orbits are now propagated.
     *
     * @input EquinoctialOrbit(9756941.799209522, 0.16219353575954956, 0.1424059584243473,
     *        0.2640974135590645, 0.030288658204842982, 1.554396409424242) in ITRF
     *
     * @input EquinoctialOrbit(9756940.912430497, 0.162193534007533, 0.1424058727481423,
     *        0.26409746136329015, 0.030288649473182046, 1.554396336772627) in ITRF
     * @input MultiNumericalPropagator
     * @input Frame ITRF is used to define each orbit propagation is done in each case in frame
     *        EME2000
     *
     * @output The SpacecraftState final states of propagation
     * @testPassCriteria The orbital elements of the output orbits must be the same as the one
     *                   expected
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testKeplerMultiSatInITRF() throws PatriusException {

        final AttitudeProvider attitudeProvider = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        // ================================ First state =========================================//

        // Frames used to define the orbit and for propagation for each state
        final Frame orbitFrame = FramesFactory.getTIRF();
        final Frame propFrame = FramesFactory.getEME2000();

        this.initialDate.shiftedBy(584.);

        // The first orbit is orbit1 defined in SetUp() and expressed in TIRF
        final Orbit initialOrbit1 = new EquinoctialOrbit(9756941.799209522, 0.16219353575954956, 0.1424059584243473,
            0.2640974135590645, 0.030288658204842982, 1.5543964094242422, PositionAngle.TRUE, orbitFrame,
            this.initialDate, this.mu);
        final Attitude attitude1 = attitudeProvider.getAttitude(initialOrbit1);

        final OrbitType type1 = initialOrbit1.getType();

        // ================================ Second state ========================================//

        // The second orbit is taken from analog VT in the class NumericalPropagator
        final Orbit initialOrbit2 = new EquinoctialOrbit(9756940.912430497, 0.162193534007533, 0.1424058727481423,
            0.26409746136329015, 0.030288649473182046, 1.554396336772627, PositionAngle.TRUE, orbitFrame,
            this.initialDate, this.mu);
        final Attitude attitude2 = attitudeProvider.getAttitude(initialOrbit2);

        final OrbitType type2 = initialOrbit2.getType();

        // ============================== MultiNumericalPropagator ==============================//

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator dop = new DormandPrince853Integrator(0.001, 200, absTolerance, relTolerance);
        dop.setInitialStepSize(60);
        Map<String, Frame> propFrameMap = new HashMap<String, Frame>();
        propFrameMap.put("key1", propFrame);
        propFrameMap.put("key2", propFrame);
        final MultiNumericalPropagator propagator = new MultiNumericalPropagator(dop, propFrameMap);
        propagator.addInitialState(new SpacecraftState(initialOrbit1, attitude1), "key1");
        propagator.addInitialState(new SpacecraftState(initialOrbit2, attitude2), "key2");
        Assert.assertTrue(propagator.getFrame("key1") == propFrame);
        Assert.assertTrue(propagator.getFrame("key2") == propFrame);

        propagator.setAttitudeProvider(attitudeProvider, "key1");
        propagator.setAttitudeProvider(attitudeProvider, "key2");

        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get("key1").getMu())), "key1");
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get("key2").getMu())), "key2");

        // ================================== Propagation =======================================//

        final double delta_t = 100000.0;
        final AbsoluteDate finalDate1 = this.initialDate.shiftedBy(delta_t);
        final double dt = 3200;
        final AbsoluteDate finalDate2 = this.initialDate.shiftedBy(dt);
        final SpacecraftState finalOrbit1 = propagator.propagate(finalDate1).get("key1");
        final SpacecraftState finalOrbit2 = propagator.propagate(finalDate2).get("key2");

        // ============================== Check on first state ==================================//

        // Compute the converted initial orbit in Frame EME2000 in order to allow comparisons
        final Orbit convInitialOrbit1 = type1.convertOrbit(initialOrbit1, propFrame);

        final double n = MathLib.sqrt(convInitialOrbit1.getMu() / convInitialOrbit1.getA()) / convInitialOrbit1.getA();
        Assert.assertEquals(convInitialOrbit1.getA(), finalOrbit1.getA(), 6.0e-8);
        Assert.assertEquals(convInitialOrbit1.getEquinoctialEx(), finalOrbit1.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit1.getEquinoctialEy(), finalOrbit1.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit1.getHx(), finalOrbit1.getHx(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit1.getHy(), finalOrbit1.getHy(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit1.getLM() + n * delta_t, finalOrbit1.getLM(), 2.0e-9);

        // ==================================== Check on second state ===========================//

        // Compute the converted initial orbit in Frame EME2000 in order to allow comparisons
        final Orbit convInitialOrbit2 = type2.convertOrbit(initialOrbit2, propFrame);

        final double m = MathLib.sqrt(convInitialOrbit2.getMu() / convInitialOrbit2.getA()) / convInitialOrbit2.getA();
        Assert.assertEquals(convInitialOrbit2.getA(), finalOrbit2.getA(), 3.0e-8);
        Assert.assertEquals(convInitialOrbit2.getEquinoctialEx(), finalOrbit2.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit2.getEquinoctialEy(), finalOrbit2.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit2.getHx(), finalOrbit2.getHx(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit2.getHy(), finalOrbit2.getHy(), 1.0e-10);
        Assert.assertEquals(convInitialOrbit2.getLM() + m * dt, finalOrbit2.getLM(), 2.0e-9);

        // Check attitudes
        final Attitude finalAttitude = propagator.propagate(this.initialDate).get("key1").getAttitude()
            .withReferenceFrame(orbitFrame);
        Assert.assertEquals(attitude1.getRotation().getAngle(), finalAttitude.getRotation().getAngle(), 0.);
        Assert.assertEquals(attitude1.getRotation().getAxis().getX(), finalAttitude.getRotation().getAxis().getX(), 0.);
        Assert.assertEquals(attitude1.getRotation().getAxis().getY(), finalAttitude.getRotation().getAxis().getY(), 0.);
        Assert.assertEquals(attitude1.getRotation().getAxis().getZ(), finalAttitude.getRotation().getAxis().getZ(), 0.);
    }

    /**
     * FA1871: This test verify these following features : - The massModel is now updated at every
     * step - The massModel is well updated when the propagation is done - A "null" element in a
     * list isn't considered anymore - The tank mass doesn't leak anymore (MultiNumericalPropagator
     * adjusted with NumericalPropagator's code)
     *
     * @throws PatriusException
     *
     * @description Test multi-sat numerical propagation, included features are:
     *              <ul>
     *              <li>Propagation in master mode</li>
     *              <li>Maneuvers (constant thrust)</li>
     *              <li>Mass provider</li>
     *              <li>Assembly</li>
     *              </ul>
     *
     * @testPassCriteria MassModel from the integrator is well updated over the propagation steps
     *                   (compared with the current SpaceCraftState)
     *
     * @throws ParseException
     */
    @Test
    public void testMassModel() throws PatriusException {

        // ====================== Initialization ======================
        final int nPropagators = 2;

        final MassProvider[] massProviders = new MassProvider[nPropagators];
        for (int i = 0; i < nPropagators; i++) {
            final AssemblyBuilder builder = new AssemblyBuilder();
            builder.addMainPart("Main" + i);
            builder.addProperty(new MassProperty(1000.), "Main" + i);

            final TankProperty tank = new TankProperty(1000.);
            builder.addPart("Tank" + i, "Main" + i, Transform.IDENTITY);
            builder.addProperty(tank, "Tank" + i);
            final Assembly assembly = builder.returnAssembly();
            massProviders[i] = new MassModel(assembly);
        }

        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(30.);
        final MultiNumericalPropagator multiNumericalPropagator = new MultiNumericalPropagator(integrator);

        // Set master mode
        class MyMultiPatriusStepHandler implements MultiPatriusStepHandler {
            /** Serializable UID. */
            private static final long serialVersionUID = -6571738955389545708L;

            @Override
            public void init(final Map<String, SpacecraftState> s0,
                             final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final MultiPatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                try {
                    for (int i = 0; i < nPropagators; i++) {
                        final SpacecraftState state = interpolator.getInterpolatedStates().get("state" + i);
                        Assert.assertEquals(massProviders[i].getMass("Tank" + i), state.getMass("Tank" + i), E_14);
                    }

                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        }

        final MultiPatriusStepHandler multiSatStepHandler = new MyMultiPatriusStepHandler();
        multiNumericalPropagator.setMasterMode(multiSatStepHandler);

        // Initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2002, 01, 02, TimeScalesFactory.getTAI());
        final AttitudeProvider attitudeProvider = new ConstantAttitudeLaw(FramesFactory.getCIRF(), Rotation.IDENTITY);

        for (int i = 0; i < nPropagators; i++) {
            final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0.001, 1.5, 0, 0, i * 10, PositionAngle.MEAN,
                FramesFactory.getGCRF(), initialDate, Constants.EGM96_EARTH_MU);
            final SpacecraftState initialState = new SpacecraftState(initialOrbit, massProviders[i]);

            multiNumericalPropagator.addInitialState(initialState, "state" + i);
            multiNumericalPropagator.addForceModel(
                new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())), "state" + i);
            multiNumericalPropagator.setAttitudeProvider(attitudeProvider, "state" + i);
            multiNumericalPropagator.setMassProviderEquation(massProviders[i], "state" + i);
        }

        // Add maneuver
        for (int i = 0; i < nPropagators; i++) {
            final TankProperty tank = new TankProperty(massProviders[i].getMass("Tank" + i));
            tank.setPartName("Tank" + i);
            final ContinuousThrustManeuver thrust = new ContinuousThrustManeuver(initialDate.shiftedBy(100),
                100 + 100 * i, new PropulsiveProperty(100, 100), Vector3D.PLUS_I, massProviders[i], tank);
            multiNumericalPropagator.addForceModel(thrust, "state" + i);
        }

        // ====================== Propagation ======================
        final AbsoluteDate finalDate = initialDate.shiftedBy(1000);

        multiNumericalPropagator.propagate(finalDate);
    }

    /**
     * @testType UT
     *
     * @description check that after multiple propagations with same propagator,
     *              once detector is removed, it is not detected on future propagations.
     *
     * @testPassCriteria only one event is detected
     *
     * @referenceVersion 4.2
     *
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testRemoveDetectorMultiplePropagation() throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(5), MathLib.toRadians(3),
            MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE, FramesFactory.getGCRF(), initDate,
            Constants.WGS84_EARTH_MU);
        final MultiNumericalPropagator propagator = new MultiNumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator.addInitialState(new SpacecraftState(initialOrbit), "Sat1");
        propagator.addInitialState(new SpacecraftState(initialOrbit), "Sat2");
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu())), "Sat1");
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu())), "Sat2");

        // Add node detector
        final NodeDetector detector1 = new NodeDetector(FramesFactory.getGCRF(), NodeDetector.ASCENDING, 1.e2, 1.e-4,
            Action.CONTINUE, true){
            /** Serializable UID. */
            private static final long serialVersionUID = -4087881340627575587L;

            private int count = 0;

            @Override
            public Action eventOccurred(final SpacecraftState s,
                                        final boolean increasing,
                                        final boolean forward) throws PatriusException {
                this.count++;
                // Check that only one occurrence is detected
                Assert.assertTrue(this.count == 1);
                return super.eventOccurred(s, increasing, forward);
            }
        };

        final NodeDetector detector2 = new NodeDetector(FramesFactory.getGCRF(), NodeDetector.ASCENDING, 1.e2, 1.e-4,
            Action.CONTINUE, true){
            /** Serializable UID. */
            private static final long serialVersionUID = -4087881340627575587L;

            private int count = 0;

            @Override
            public Action eventOccurred(final SpacecraftState s,
                                        final boolean increasing,
                                        final boolean forward) throws PatriusException {
                this.count++;
                // Check that only one occurrence is detected
                Assert.assertTrue(this.count == 1);
                return super.eventOccurred(s, increasing, forward);
            }
        };

        propagator.addEventDetector(detector1, "Sat1");
        propagator.addEventDetector(detector2, "Sat2");

        // First propagation: one detection expected
        propagator.propagate(initDate.shiftedBy(3600 * 12));

        propagator.addEventDetector(new DateDetector(AbsoluteDate.J2000_EPOCH), "Sat1");
        propagator.addEventDetector(new DateDetector(AbsoluteDate.J2000_EPOCH), "Sat2");

        // Second propagation: no detection expected
        propagator.propagate(initDate.shiftedBy(3600 * 24));
    }

    /**
     * Initializations
     *
     * @throws PatriusException
     *
     * @since 3.0
     */
    @Before
    public void setUp() throws PatriusException {
        // Initializations
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Integrator
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final double minstep = 0.001;
        final double maxstep = 200.;
        final AdaptiveStepsizeIntegrator integrator1 = new DormandPrince853Integrator(minstep, maxstep, absTolerance,
            relTolerance);
        final AdaptiveStepsizeIntegrator integrator2 = new DormandPrince853Integrator(minstep, maxstep, absTolerance,
            relTolerance);
        this.integratorMultiSat = new DormandPrince853Integrator(minstep, maxstep, absTolerance, relTolerance);

        // Start date
        this.initialDate = AbsoluteDate.J2000_EPOCH;

        // Final date
        final double dt = 30000.;
        this.finalDate = this.initialDate.shiftedBy(dt);

        // Define MU
        this.mu = Utils.mu;

        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        this.orbit1 = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getGCRF(),
            this.initialDate, this.mu);

        this.defaultMassModel = new SimpleMassModel(1000., DEFAULT);
        this.state1 = new SpacecraftState(this.orbit1, this.defaultMassModel);

        // Second Spacecraft definition
        // orbit
        final double a = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS + 400e3;
        final double e = .0001;
        final double i = 60 * 3.14 / 180;
        final double raan = 0;
        final double pa = 270 * 3.14 / 180;
        final double w = 0;

        // state
        this.orbit2 = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, FramesFactory.getGCRF(),
            this.initialDate, this.mu);
        this.attProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        this.state2 = new SpacecraftState(this.orbit2, this.attProv.getAttitude(this.orbit2, this.initialDate,
            this.orbit2.getFrame()));

        // Numerical propagator definition
        this.firstPropagator = new NumericalPropagator(integrator1, this.state1.getFrame());
        this.secondPropagator = new NumericalPropagator(integrator2, this.state2.getFrame());
        this.multiNumericalPropagator = new MultiNumericalPropagator(this.integratorMultiSat);

        // Add initial state
        this.firstPropagator.setInitialState(this.state1);
        this.secondPropagator.setInitialState(this.state2);
        this.multiNumericalPropagator.addInitialState(this.state1, STATE1);
        this.multiNumericalPropagator.addInitialState(this.state2, STATE2);

        // Add Newtonian attraction
        this.firstPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state1.getMu())));
        this.secondPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state2.getMu())));
        this.multiNumericalPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state1
            .getMu())), STATE1);
        this.multiNumericalPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.state2
            .getMu())), STATE2);

        // Add additional equation associated with the mass provider of the first state
        this.firstPropagator.setMassProviderEquation(this.defaultMassModel);
        final double[] absT = { 0.01 };
        final double[] relT = { 1.0e-7 };
        final String eqName = this.defaultMassModel.getAdditionalEquation(DEFAULT).getName();
        this.firstPropagator.setAdditionalStateTolerance(eqName, absT, relT);
        this.multiNumericalPropagator.setMassProviderEquation(this.defaultMassModel, STATE1);
        this.multiNumericalPropagator.setAdditionalStateTolerance(eqName, absT, relT, STATE1);

        // Add attitude provider associated with the second propagator
        this.secondPropagator.setAttitudeProvider(this.attProv);
        this.multiNumericalPropagator.setAttitudeProvider(this.attProv, STATE2);

        // Define complex mass provider
        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main part test
        try {
            // add main part
            builder.addMainPart("main");

            // add other parts
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);

            builder.addPart("part2", "main", transform1);
            builder.addPart("part3", "main", transform1);
            builder.addPart("part4", "part3", transform1);
            builder.addPart("part5", "part4", transform1);

            // mass properties
            final MassProperty mass1 = new MassProperty(10.0);
            final MassProperty mass2 = new MassProperty(5.0);
            final MassProperty mass4 = new MassProperty(20.0);
            final MassProperty mass5 = new MassProperty(2.0);
            builder.addProperty(mass1, "main");
            builder.addProperty(mass2, "part2");
            builder.addProperty(mass4, "part4");
            builder.addProperty(mass5, "part5");

        } catch (final IllegalArgumentException ex) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // model creation
        this.complexMassModel = new MassModel(assembly);
    }

    /**
     * Compare each terms of two vectors by relative differences
     */
    private static void checkVectors(final Vector3D expected,
                                     final Vector3D actual,
                                     final Vector3D tol) {
        Assert.assertEquals(0., MathLib.abs((expected.getX() - actual.getX()) / expected.getX()), tol.getX());
        Assert.assertEquals(0., MathLib.abs((expected.getY() - actual.getY()) / expected.getY()), tol.getY());
        Assert.assertEquals(0., MathLib.abs((expected.getZ() - actual.getZ()) / expected.getZ()), tol.getZ());
    }
}
