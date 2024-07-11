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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::FA:665:28/07/2016:forbid non-inertial frames
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.forces.gravity.CunninghamAttractionModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test class for {@link LyddaneSecularPropagator}.
 * 
 * @author Galpin Thomas
 * 
 * @version $Id: LyddaneSecularPropagatorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 */
public class LyddaneSecularPropagatorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Lyddane secular propagator
         * 
         * @featureDescription Validate the Lyddane secular propagator
         * 
         * @coveredRequirements
         */
        LYDDANE_SECULAR_PROPAGATOR
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(LyddaneSecularPropagatorTest.class.getSimpleName(), "Lyddane secular propagator");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the LYDDANE secular propagation (nominal case)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagation() throws PatriusException {

        final double eps = 5E-15;

        Report.printMethodHeader("testPropagation", "Propagation", "Celestlab 3.1.0", eps, ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actual = (KeplerianOrbit) finalState.getOrbit();

        // Check results (Celestlab reference)
        Assert.assertEquals(0., this.relDiff(7001324.04933762271, actual.getA()), eps);
        Assert.assertEquals(0., this.relDiff(0.10094383427718791, actual.getE()), eps);
        Assert.assertEquals(0., this.relDiff(0.00091590296269827, actual.getI()), eps);
        Assert.assertEquals(0., this.relDiff(1.84013623456242748, actual.getPerigeeArgument()), eps);
        Assert.assertEquals(0., this.relDiff(-0.05210035006367786, actual.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0.,
            this.relDiff(93.4791323397896861 % (2. * FastMath.PI) - 2. * FastMath.PI, actual.getMeanAnomaly()), 2E-13);
        Assert.assertEquals(86400, actual.getDate().durationFrom(date), 0);

        Report.printToReport("a", 7001324.04933762271, actual.getA());
        Report.printToReport("e", 0.10094383427718791, actual.getE());
        Report.printToReport("i", 0.00091590296269827, actual.getI());
        Report.printToReport("Pa", 1.84013623456242748, actual.getPerigeeArgument());
        Report.printToReport("RAAN", -0.05210035006367786, actual.getRightAscensionOfAscendingNode());
        Report.printToReport("M", 93.4791323397896861 % (2. * FastMath.PI) - 2. * FastMath.PI, actual.getMeanAnomaly());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation with different input parameters (cartesian)
     * 
     * @input Cartesian orbit
     * 
     * @output Cartesian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, relative tolerance: 1E-13)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationCart() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final Orbit cart = new CartesianOrbit(orbit);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(cart, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);

        final double eps = 3E-13;

        // Propagation
        final CartesianOrbit actual = (CartesianOrbit) propagator.propagate(date.shiftedBy(86400.)).getOrbit();

        // Expected orbit
        final Orbit expKep = new KeplerianOrbit(7001324.04933762271, 0.10094383427718791, 0.00091590296269827,
            1.84013623456242748,
            -0.05210035006367786, 93.4791323397896861, PositionAngle.MEAN, FramesFactory.getCIRF(), date,
            3.98600442E14);
        final Orbit expCart = new CartesianOrbit(expKep);

        // Check results (Celestlab reference)
        Assert.assertEquals(
            0.,
            this.relDiff(expCart.getPVCoordinates().getPosition().getX(), actual.getPVCoordinates().getPosition()
                .getX()),
            eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expCart.getPVCoordinates().getPosition().getY(), actual.getPVCoordinates().getPosition()
                .getY()),
            eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expCart.getPVCoordinates().getPosition().getZ(), actual.getPVCoordinates().getPosition()
                .getZ()),
            eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expCart.getPVCoordinates().getVelocity().getX(), actual.getPVCoordinates().getVelocity()
                .getX()),
            eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expCart.getPVCoordinates().getVelocity().getY(), actual.getPVCoordinates().getVelocity()
                .getY()),
            eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expCart.getPVCoordinates().getVelocity().getZ(), actual.getPVCoordinates().getVelocity()
                .getZ()),
            eps);
        Assert.assertEquals(86400, actual.getDate().durationFrom(date), 0);
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation with orbit frame (GCRF) different from body frame (CIRF)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 orbital period
     * 
     * @testPassCriteria orbit is close (< 130m) to orbit obtained with numerical integration including J2 to J5 only
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationBodyFrame() throws PatriusException, IOException, ParseException {

        // Gravity potential
        final double ae = 6378136.46;
        final double[][] c = { { 1.0 }, { 0.0 }, { -0.0010826264572317671 }, { 2.532547231862799E-6 },
            { 1.6199644341360001E-6 }, { 2.2779284870054367E-7 } };
        final double[][] s = { { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 } };
        final double mu = 3.986004415E14;
        final Frame bodyFrame = FramesFactory.getCIRF();

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, ae, mu, c[2][0], c[3][0],
            c[4][0], c[5][0], bodyFrame, ParametersType.OSCULATING);

        // Numerical propagator
        final NumericalPropagator numerical = new NumericalPropagator(new DormandPrince853Integrator(0.01, 100, 1E-7,
            1E-12));
        numerical.addForceModel(new CunninghamAttractionModel(bodyFrame, ae, mu, c, s));
        numerical.setInitialState(new SpacecraftState(orbit));

        final double eps = 130;

        // Propagation (actual and expected)
        final Orbit actual = propagator.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();
        final Orbit expected = numerical.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();

        // Check results
        Assert.assertEquals(expected.getPVCoordinates().getPosition().getX(), actual.getPVCoordinates().getPosition()
            .getX(), eps);
        Assert.assertEquals(expected.getPVCoordinates().getPosition().getY(), actual.getPVCoordinates().getPosition()
            .getY(), eps);
        Assert.assertEquals(expected.getPVCoordinates().getPosition().getZ(), actual.getPVCoordinates().getPosition()
            .getZ(), eps);
        Assert.assertEquals(actual.getFrame(), FramesFactory.getGCRF());
        Assert.assertEquals(propagator.propagateMeanOrbit(date.shiftedBy(orbit.getKeplerianPeriod())).getFrame(),
            FramesFactory.getGCRF());
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation with orbit frame (GCRF) different from body frame (ITRF)
     * 
     * @input Keplerian orbit
     * 
     * @output exception
     * 
     * @testPassCriteria an exception is thrown
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testPropagationBodyFrameITRF() throws PatriusException, IOException, ParseException {
        try {
            final double[][] c = { { 1.0 }, { 0.0 }, { -0.0010826264572317671 }, { 2.532547231862799E-6 },
                { 1.6199644341360001E-6 }, { 2.2779284870054367E-7 } };

            final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2,
                PositionAngle.MEAN, FramesFactory.getGCRF(), AbsoluteDate.FIFTIES_EPOCH_TAI, 3.986004415E14);
            new LyddaneSecularPropagator(orbit, 6378136.46, 3.986004415E14, c[2][0], c[3][0], c[4][0], c[5][0],
                FramesFactory.getITRF(), ParametersType.OSCULATING);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation with orbit frame (MOD) different from propagation frame (GCRF)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 orbital period
     * 
     * @testPassCriteria orbit obtained with propagation of initial state in MOD is identical to orbit obtained with
     *                   propagation of initial state in GCRF
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationPropagationFrame() throws PatriusException, IOException, ParseException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, 3.98600442e+14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);

        // Propagation in GCRF
        final Orbit actual = propagator.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();

        // Propagation in GCRF frame with initial orbit in MOD
        final Orbit orbit2 = orbit.getType().convertOrbit(orbit, FramesFactory.getMOD(false));
        final LyddaneSecularPropagator propagator2 = new LyddaneSecularPropagator(orbit2, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);
        propagator2.setOrbitFrame(FramesFactory.getGCRF());
        final Orbit actual2 = propagator2.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();

        final double eps2 = 2E-13;

        // Check results
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getPosition().getX(), actual.getPVCoordinates().getPosition()
                .getX()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getPosition().getY(), actual.getPVCoordinates().getPosition()
                .getY()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getPosition().getZ(), actual.getPVCoordinates().getPosition()
                .getZ()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getVelocity().getX(), actual.getPVCoordinates().getVelocity()
                .getX()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getVelocity().getY(), actual.getPVCoordinates().getVelocity()
                .getY()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getVelocity().getZ(), actual.getPVCoordinates().getVelocity()
                .getZ()),
            eps2);
        Assert.assertEquals(actual2.getFrame(), FramesFactory.getGCRF());

        // Complementary test: check reset state
        final Orbit orbit_rs1 = propagator.propagateOrbit(orbit.getDate());
        propagator.resetInitialState(new SpacecraftState(orbit_rs1));
        final Orbit orbit_rs2 = propagator.propagateOrbit(orbit.getDate());
        final double eps3 = 2E-15;

        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getPosition().getX(), orbit_rs2.getPVCoordinates().getPosition()
                .getX()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getPosition().getY(), orbit_rs2.getPVCoordinates().getPosition()
                .getY()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getPosition().getZ(), orbit_rs2.getPVCoordinates().getPosition()
                .getZ()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getVelocity().getX(), orbit_rs2.getPVCoordinates().getVelocity()
                .getX()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getVelocity().getY(), orbit_rs2.getPVCoordinates().getVelocity()
                .getY()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getVelocity().getZ(), orbit_rs2.getPVCoordinates().getVelocity()
                .getZ()), eps3);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation until initial date (no propagation)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 0s propagation
     * 
     * @testPassCriteria final orbit is exactly identical to initial orbit (1E-15 relative tolerance due to mean <=> osc
     *                   conversion)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationInitialDate() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2,
            PositionAngle.MEAN, FramesFactory.getCIRF(), date, 3.98600442e+14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.OSCULATING);

        final double eps = 1E-15;

        // Propagation
        final KeplerianOrbit actual = (KeplerianOrbit) propagator.propagate(date).getOrbit();

        // Check results (Celestlab reference)
        Assert.assertEquals(orbit.getA(), actual.getA(), eps);
        Assert.assertEquals(orbit.getE(), actual.getE(), eps);
        Assert.assertEquals(orbit.getI(), actual.getI(), eps);
        Assert.assertEquals(orbit.getPerigeeArgument(), actual.getPerigeeArgument(), eps);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNode(), actual.getRightAscensionOfAscendingNode(), eps);
        Assert.assertEquals(orbit.getMeanAnomaly(), actual.getMeanAnomaly(), eps);
        Assert.assertEquals(0., actual.getDate().durationFrom(date), 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation (nominal case)
     * 
     * @input Keplerian orbit, Perigee detector
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria perigee has been detected (propagation stopped at perigee, i.e. final anomaly is 0)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationEvents() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2,
            PositionAngle.MEAN, FramesFactory.getCIRF(), date, 3.98600442e+14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);

        final double eps = 5E-13;

        propagator.addEventDetector(new ApsideDetector(orbit, ApsideDetector.PERIGEE));

        // Propagation
        final KeplerianOrbit actual = (KeplerianOrbit) propagator.propagate(date.shiftedBy(86400.)).getOrbit();

        // Check results (propagation stopped exactly at node)
        Assert.assertEquals(0., actual.getMeanAnomaly() % (2. * FastMath.PI), eps);
        Assert.assertFalse(actual.getDate().durationFrom(date) == 86400);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateMeanOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular mean orbit computation
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian mean elements after 1 day propagation
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationMean() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);

        final double eps = 1E-15;

        // Propagation
        final KeplerianOrbit actual = (KeplerianOrbit) propagator.propagateMeanOrbit(date.shiftedBy(86400.));

        // Check results (Celestlab reference)
        Assert.assertEquals(7000000, actual.getA(), eps);
        Assert.assertEquals(0.1, actual.getE(), eps);
        Assert.assertEquals(0.001, actual.getI(), eps);
        Assert.assertEquals(1.82891916356237072, actual.getPerigeeArgument(), eps);
        Assert.assertEquals(-0.02897469428766244, actual.getRightAscensionOfAscendingNode(), eps);
        Assert.assertEquals(93.46760444302015 % (2. * FastMath.PI), actual.getMeanAnomaly(), eps);
        Assert.assertEquals(86400, actual.getDate().durationFrom(date), 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation with mass provider (using all constructors)
     * 
     * @input Keplerian orbit, mass provider
     * 
     * @output Keplerian orbit and mass after 1 day propagation
     * 
     * @testPassCriteria mass is as expected, orbit is also as expected (reference : Celestlab 3.1.0, tolerance: 1E-15)
     *                   although already checked in testPropagation(),
     *                   This test ensure using different constructors will still lead to the same result.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationMass() throws PatriusException {

        // Initialization (with the 3 constructors)
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final MassProvider mass = new SimpleMassModel(1000., "mass");
        final LyddaneSecularPropagator propagator1 = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442E14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN, mass);
        final LyddaneSecularPropagator propagator2 = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442E14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN, null, mass);
        final LyddaneSecularPropagator propagator3 = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442E14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN, null, null, mass);

        final double eps = 5E-15;

        // Propagation
        final SpacecraftState finalState1 = propagator1.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit1 = (KeplerianOrbit) finalState1.getOrbit();

        final SpacecraftState finalState2 = propagator2.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit2 = (KeplerianOrbit) finalState2.getOrbit();

        final SpacecraftState finalState3 = propagator3.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit3 = (KeplerianOrbit) finalState3.getOrbit();

        // Check: final state vector (Celestlab reference) and mass
        final double[] expected = { 7001324.04933762271, 0.10094383427718791, 0.00091590296269827, 1.84013623456242748,
            -0.05210035006367786, 93.4791323397896861 % (2. * FastMath.PI) - 2. * FastMath.PI };
        final double[] actual1 = { actualOrbit1.getA(), actualOrbit1.getE(), actualOrbit1.getI(),
            actualOrbit1.getPerigeeArgument(), actualOrbit1.getRightAscensionOfAscendingNode(),
            actualOrbit1.getMeanAnomaly() };
        final double[] actual2 = { actualOrbit2.getA(), actualOrbit2.getE(), actualOrbit2.getI(),
            actualOrbit2.getPerigeeArgument(), actualOrbit2.getRightAscensionOfAscendingNode(),
            actualOrbit2.getMeanAnomaly() };
        final double[] actual3 = { actualOrbit3.getA(), actualOrbit3.getE(), actualOrbit3.getI(),
            actualOrbit3.getPerigeeArgument(), actualOrbit3.getRightAscensionOfAscendingNode(),
            actualOrbit3.getMeanAnomaly() };
        for (int i = 0; i < expected.length - 1; i++) {
            Assert.assertEquals(0., this.relDiff(expected[i], actual1[i]), eps);
            Assert.assertEquals(0., this.relDiff(expected[i], actual2[i]), eps);
            Assert.assertEquals(0., this.relDiff(expected[i], actual3[i]), eps);
        }
        Assert.assertEquals(0., this.relDiff(expected[5], actual1[5]), 5E-13);
        Assert.assertEquals(0., this.relDiff(expected[5], actual2[5]), 5E-13);
        Assert.assertEquals(0., this.relDiff(expected[5], actual3[5]), 5E-13);

        Assert.assertEquals(1000., finalState1.getMass("mass"), 0.);
        Assert.assertEquals(1000., finalState2.getMass("mass"), 0.);
        Assert.assertEquals(1000., finalState3.getMass("mass"), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation with attitude providers (using all constructors)
     * 
     * @input Keplerian orbit, attitude providers
     * 
     * @output Keplerian orbit and attitude after 1 day propagation
     * 
     * @testPassCriteria attitude is as expected, orbit is also as expected (reference : Celestlab 3.1.0, tolerance:
     *                   1E-15) although already checked in testPropagation(),
     *                   This test ensure using different constructors will still lead to the same result.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationAttitudeProvider() throws PatriusException {

        // Initialization (with the 3 constructors)
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final MassProvider mass = new SimpleMassModel(1000., "mass");
        final Rotation rot1 = Rotation.IDENTITY;
        final Rotation rot2 = new Rotation(Vector3D.PLUS_K, 3.2);
        final AttitudeProvider attitudeProvider = new ConstantAttitudeLaw(orbit.getFrame(), rot1);
        final AttitudeProvider attitudeProviderForces = new ConstantAttitudeLaw(orbit.getFrame(), rot1);
        final AttitudeProvider attitudeProviderEvents = new ConstantAttitudeLaw(orbit.getFrame(), rot2);
        final LyddaneSecularPropagator propagator1 = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442E14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN, attitudeProvider);
        final LyddaneSecularPropagator propagator2 = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442E14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN, attitudeProviderForces, attitudeProviderEvents);
        final LyddaneSecularPropagator propagator3 = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442E14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN, attitudeProvider, mass);
        final LyddaneSecularPropagator propagator4 = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442E14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN, attitudeProviderForces, attitudeProviderEvents, mass);

        final double eps = 5E-15;

        // Propagation
        final SpacecraftState finalState1 = propagator1.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit1 = (KeplerianOrbit) finalState1.getOrbit();

        final SpacecraftState finalState2 = propagator2.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit2 = (KeplerianOrbit) finalState2.getOrbit();

        final SpacecraftState finalState3 = propagator3.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit3 = (KeplerianOrbit) finalState3.getOrbit();

        final SpacecraftState finalState4 = propagator4.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit4 = (KeplerianOrbit) finalState4.getOrbit();

        // Check: final state vector (Celestlab reference) and attitude
        final double[] expected = { 7001324.04933762271, 0.10094383427718791, 0.00091590296269827, 1.84013623456242748,
            -0.05210035006367786, 93.4791323397896861 % (2. * FastMath.PI) - 2. * FastMath.PI };
        final double[] actual1 = { actualOrbit1.getA(), actualOrbit1.getE(), actualOrbit1.getI(),
            actualOrbit1.getPerigeeArgument(), actualOrbit1.getRightAscensionOfAscendingNode(),
            actualOrbit1.getMeanAnomaly() };
        final double[] actual2 = { actualOrbit2.getA(), actualOrbit2.getE(), actualOrbit2.getI(),
            actualOrbit2.getPerigeeArgument(), actualOrbit2.getRightAscensionOfAscendingNode(),
            actualOrbit2.getMeanAnomaly() };
        final double[] actual3 = { actualOrbit3.getA(), actualOrbit3.getE(), actualOrbit3.getI(),
            actualOrbit3.getPerigeeArgument(), actualOrbit3.getRightAscensionOfAscendingNode(),
            actualOrbit3.getMeanAnomaly() };
        final double[] actual4 = { actualOrbit4.getA(), actualOrbit4.getE(), actualOrbit4.getI(),
            actualOrbit4.getPerigeeArgument(), actualOrbit4.getRightAscensionOfAscendingNode(),
            actualOrbit4.getMeanAnomaly() };
        for (int i = 0; i < expected.length - 1; i++) {
            Assert.assertEquals(0., this.relDiff(expected[i], actual1[i]), eps);
            Assert.assertEquals(0., this.relDiff(expected[i], actual2[i]), eps);
            Assert.assertEquals(0., this.relDiff(expected[i], actual3[i]), eps);
            Assert.assertEquals(0., this.relDiff(expected[i], actual4[i]), eps);
        }
        Assert.assertEquals(0., this.relDiff(expected[5], actual1[5]), 5E-13);
        Assert.assertEquals(0., this.relDiff(expected[5], actual2[5]), 5E-13);
        Assert.assertEquals(0., this.relDiff(expected[5], actual3[5]), 5E-13);
        Assert.assertEquals(0., this.relDiff(expected[5], actual4[5]), 5E-13);

        Assert.assertEquals(0., Rotation.distance(finalState1.getAttitude().getRotation(), rot1));
        Assert.assertEquals(0., Rotation.distance(finalState2.getAttitudeForces().getRotation(), rot1));
        Assert.assertEquals(0., Rotation.distance(finalState2.getAttitudeEvents().getRotation(), rot2));
        Assert.assertEquals(0., Rotation.distance(finalState3.getAttitude().getRotation(), rot1));
        Assert.assertEquals(0., Rotation.distance(finalState4.getAttitudeForces().getRotation(), rot1));
        Assert.assertEquals(0., Rotation.distance(finalState4.getAttitudeEvents().getRotation(), rot2));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#mean2osc(Orbit)}
     * @testedMethod {@link LyddaneSecularPropagator#osc2mean(Orbit)}
     * 
     * @description test the Lyddane secular mean <=> osculating conversion
     * 
     * @input Keplerian orbit in mean/osculating parameters
     * 
     * @output Keplerian orbit in osculating/mean parameters
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testMeanOscConversion() throws PatriusException {

        final double eps = 5E-15;

        Report.printMethodHeader("testMeanOscConversion", "Mean <=> Osculating conversion", "Celestlab 3.1.0", eps,
            ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);

        // Mean to osc conversion
        final KeplerianOrbit osc = (KeplerianOrbit) propagator.mean2osc(orbit);

        // Check results (Celestlab reference)
        Assert.assertEquals(0., this.relDiff(7002173.07684397511, osc.getA()), eps);
        Assert.assertEquals(0., this.relDiff(0.10153819792786475, osc.getE()), eps);
        Assert.assertEquals(0., this.relDiff(0.00091231729772511, osc.getI()), eps);
        Assert.assertEquals(0., this.relDiff(1.5749947889448208, osc.getPerigeeArgument()), eps);
        Assert.assertEquals(0., this.relDiff(0.09950491676271800, osc.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(0.19641059549957640, osc.getMeanAnomaly()), eps);

        Report.printToReport("a", 7002173.07684397511, osc.getA());
        Report.printToReport("e", 0.10153819792786475, osc.getE());
        Report.printToReport("i", 0.00091231729772511, osc.getI());
        Report.printToReport("Pa", 1.5749947889448208, osc.getPerigeeArgument());
        Report.printToReport("RAAN", 0.09950491676271800, osc.getRightAscensionOfAscendingNode());
        Report.printToReport("M", 0.19641059549957640, osc.getMeanAnomaly());

        // Osc to mean conversion
        final KeplerianOrbit mean = (KeplerianOrbit) propagator.osc2mean(osc);

        // Check results (Celestlab reference)
        Assert.assertEquals(0., this.relDiff(7000000, mean.getA()), eps);
        Assert.assertEquals(0., this.relDiff(0.1, mean.getE()), eps);
        Assert.assertEquals(0., this.relDiff(0.001, mean.getI()), eps);
        Assert.assertEquals(0., this.relDiff(FastMath.PI / 2., mean.getPerigeeArgument()), eps);
        Assert.assertEquals(0., this.relDiff(0.1, mean.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(0.2, mean.getMeanAnomaly()), eps);

        Report.printToReport("a", 7000000, mean.getA());
        Report.printToReport("e", 0.1, mean.getE());
        Report.printToReport("i", 0.001, mean.getI());
        Report.printToReport("Pa", FastMath.PI / 2., mean.getPerigeeArgument());
        Report.printToReport("RAAN", 0.1, mean.getRightAscensionOfAscendingNode());
        Report.printToReport("M", 0.2, mean.getMeanAnomaly());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Lyddane secular propagation with {J2, J3, J4, J5} ~ {0, 0, 0, 0} => close to Keplerian
     *              propagation
     * 
     * @input Keplerian orbit, {J2, J3, J4, J5} ~ {0, 0, 0, 0}
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is close to expected Keplerian orbit (relative tolerance of {1E-13, 1E-4, 1E-2, 1E-10,
     *                   1E-8, 1E-9})
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationKeplerian() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378000, 3.98600442E14,
            -0.001082626613 / 1E9, 0.000002532393 / 1E9, 0.000001619137 / 1E9, 0.000000227742 / 1E9,
            FramesFactory.getCIRF(), ParametersType.MEAN);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actual = (KeplerianOrbit) finalState.getOrbit();

        // Check results (Keplerian reference)
        Assert.assertEquals(0., this.relDiff(7000000, actual.getA()), 1E-13);
        Assert.assertEquals(0., this.relDiff(0.01, actual.getE()), 1E-4);
        Assert.assertEquals(0., this.relDiff(0.001, actual.getI()), 1E-2);
        Assert.assertEquals(0., this.relDiff(FastMath.PI / 2., actual.getPerigeeArgument()), 1E-10);
        Assert.assertEquals(0., this.relDiff(0.1, actual.getRightAscensionOfAscendingNode()), 1E-8);
        Assert.assertEquals(
            0.,
            this.relDiff((0.2 + orbit.getKeplerianMeanMotion() * 86400.) % (2. * FastMath.PI) - 2. * FastMath.PI,
                actual.getMeanAnomaly()), 1E-9);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#LYDDANE_LONG_PERIOD_PROPAGATOR}
     * 
     * @description test the LYDDANE long period propagation exceptions (eccentricity or inclination out of boundaries)
     * 
     * @input invalid Keplerian orbit
     * 
     * @output Exception
     * 
     * @testPassCriteria exception is thrown as expected when eccentricity > 0.9 and inclination close to critical
     *                   inclination
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testExceptions() {

        try {
            // Eccentricity out of bounds
            final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
            final Orbit orbit = new KeplerianOrbit(7000000, 0.91, 0.001, FastMath.PI / 2., 0.1, 0.2,
                PositionAngle.MEAN, FramesFactory.getCIRF(), date, 3.98600442e+14);
            new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14, -0.001082626613, 0.000002532393,
                0.000001619137, 0.000000227742, FramesFactory.getCIRF(), ParametersType.OSCULATING);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            // Inclination close to 1st critical inclination
            final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
            final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 1.10714, FastMath.PI / 2., 0.1, 0.2,
                PositionAngle.MEAN, FramesFactory.getCIRF(), date, 3.98600442e+14);
            new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14, -0.001082626613, 0.000002532393,
                0.000001619137, 0.000000227742, FramesFactory.getCIRF(), ParametersType.OSCULATING);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            // Inclination close to 2nd critical inclination
            final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
            final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 2.03444, FastMath.PI / 2., 0.1, 0.2,
                PositionAngle.MEAN, FramesFactory.getCIRF(), date, 3.98600442e+14);
            new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14, -0.001082626613, 0.000002532393,
                0.000001619137, 0.000000227742, FramesFactory.getCIRF(), ParametersType.OSCULATING);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @throws IOException
     * @throws URISyntaxException
     * @testType VT
     * 
     * @testedFeature {@link features#LYDDANE_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link LyddaneSecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description validation of the Lyddane secular propagation model
     * 
     * @input Keplerian orbit
     * 
     * @output Ephemeris of Keplerian orbits, sampled every 15min
     * 
     * @testPassCriteria orbits are as expected (reference : Celestlab 3.1.0, tolerance: 1E-13)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationValidation() throws PatriusException, IOException, URISyntaxException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final LyddaneSecularPropagator propagator = new LyddaneSecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, FramesFactory.getCIRF(),
            ParametersType.MEAN);

        // Set propagator in master mode to retrieve ephemeris sampled every 900s
        final List<double[]> actual = new ArrayList<double[]>();
        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                final double[] stateVector = new double[6];
                currentState.mapStateToArray(OrbitType.KEPLERIAN, PositionAngle.MEAN, stateVector);
                actual.add(stateVector);
            }
        };
        propagator.setMasterMode(900., handler);

        final double eps = 6E-13;

        // Read reference data
        final List<double[]> reference = this.readData("analytical" + File.separator + "LyddaneSecularCelestlab");

        // Propagation
        propagator.propagate(date.shiftedBy(86400.));

        // Check results (Celestlab reference)
        for (int i = 0; i < reference.size(); i++) {
            for (int j = 0; j < reference.get(i).length - 1; j++) {
                Assert.assertEquals(0., this.relDiff(reference.get(i)[j], actual.get(i)[j]), eps);
            }
            Assert.assertEquals(MathUtils.normalizeAngle(reference.get(i)[5], FastMath.PI),
                MathUtils.normalizeAngle(actual.get(i)[5], FastMath.PI), eps);
        }
    }

    /**
     * Set up.
     * 
     * @throws IOException
     * @throws PatriusException
     */
    @BeforeClass
    public static void setup() throws IOException, PatriusException {
        Utils.setDataRoot("regular-dataCNES-2010");
    }

    /**
     * Read reference data.
     * 
     * @param path
     *        file path
     * @return reference data
     * @throws IOException
     * @throws URISyntaxException
     */
    private List<double[]> readData(final String filename) throws IOException, URISyntaxException {

        // Initialization
        final String path = this.getClass().getClassLoader().getResource(filename).toURI().getPath();
        final BufferedReader reader = new BufferedReader(new FileReader(path));
        final List<double[]> list = new ArrayList<double[]>();

        // Read data
        while (reader.ready()) {
            final String line = reader.readLine();
            final String[] array = line.split("[ ]+");
            final double[] result = { Double.parseDouble(array[1]), Double.parseDouble(array[2]),
                Double.parseDouble(array[3]),
                Double.parseDouble(array[4]), Double.parseDouble(array[5]), Double.parseDouble(array[6]) };
            list.add(result);
        }
        reader.close();

        // Return result
        return list;
    }

    /**
     * Compute relative difference.
     * 
     * @param expected
     *        expected
     * @param actual
     *        actual
     * @return relative difference
     */
    private double relDiff(final double expected, final double actual) {
        if (expected == 0) {
            return MathLib.abs(expected - actual);
        } else {
            return MathLib.abs((expected - actual) / expected);
        }
    }
}
