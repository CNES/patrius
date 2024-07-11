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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2563:27/01/2021:[PATRIUS] Ajout de la matrice de transition J2Secular 
 * VERSION:4.6:DM:DM-2624:27/01/2021:[PATRIUS] Correction tropospherique avec le modele d’Azoulay
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
import fr.cnes.sirius.patrius.forces.gravity.BalminoAttractionModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test class for {@link J2SecularPropagator}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: J2SecularPropagatorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 */
public class J2SecularPropagatorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the J2 secular propagator
         * 
         * @featureDescription Validate the J2 secular propagator
         * 
         * @coveredRequirements
         */
        J2_SECULAR_PROPAGATOR
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(J2SecularPropagatorTest.class.getSimpleName(), "J2 secular propagator");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation (nominal case)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, tolerance: 1E-16)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagation() throws PatriusException {

        final double eps = 1E-16;

        Report.printMethodHeader("testPropagation", "Propagation", "Celestlab 3.1.0", eps, ComparisonType.ABSOLUTE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());

        // Propagation
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actual = (KeplerianOrbit) finalState.getOrbit();

        // Check results (Celestlab reference)
        Assert.assertEquals(7000000, actual.getA(), eps);
        Assert.assertEquals(0.01, actual.getE(), eps);
        Assert.assertEquals(0.001, actual.getI(), eps);
        Assert.assertEquals(1.82198195888265158, actual.getPerigeeArgument(), eps);
        Assert.assertEquals(-0.02559291023856020, actual.getRightAscensionOfAscendingNode(), eps);
        Assert.assertEquals(93.4654442804006464, actual.getMeanAnomaly(), eps);
        Assert.assertEquals(86400, actual.getDate().durationFrom(date), 0);

        Report.printToReport("a", 7000000, actual.getA());
        Report.printToReport("e", 0.01, actual.getE());
        Report.printToReport("i", 0.001, actual.getI());
        Report.printToReport("Pa", 1.82198195888265158, actual.getPerigeeArgument());
        Report.printToReport("RAAN", -0.02559291023856020, actual.getRightAscensionOfAscendingNode());
        Report.printToReport("M", 93.4654442804006464, actual.getMeanAnomaly());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation with different input parameters (cartesian)
     * 
     * @input Cartesian orbit
     * 
     * @output Cartesian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, relative tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationCart() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final Orbit cart = new CartesianOrbit(orbit);
        final J2SecularPropagator propagator = new J2SecularPropagator(cart, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());

        final double eps = 3E-15;

        // Propagation
        final CartesianOrbit actual = (CartesianOrbit) propagator.propagate(date.shiftedBy(86400.)).getOrbit();

        // Expected orbit
        final Orbit expKep = new KeplerianOrbit(7000000, 0.01, 0.001, 1.82198195888265158, -0.02559291023856020,
            93.4654442804006464, PositionAngle.MEAN, FramesFactory.getCIRF(), date, 3.98600442E14);
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
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation with orbit frame (GCRF) different from body frame (CIRF)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is close to orbit obtained with numerical integration including J2 only
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationBodyFrame() throws PatriusException, IOException, ParseException {

        // Gravity potential
        final double ae = 6378136.46;
        final double[][] c = { { 1.0 }, { 0.0 }, { -0.0010826264572317671 } };
        final double[][] cnorm = { { 1.0 }, { 0.0 }, { -4.84165270522E-4 } };
        final double[][] snorm = { { 0.0 }, { 0.0 }, { 0.0 } };
        final double mu = 3.986004415E14;
        final Frame bodyFrame = FramesFactory.getCIRF();

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, ae, mu, c[2][0], bodyFrame);

        // Numerical propagator
        final NumericalPropagator numerical = new NumericalPropagator(new DormandPrince853Integrator(0.01, 100, 1E-7,
            1E-12));
        numerical.addForceModel(new BalminoAttractionModel(bodyFrame, ae, mu, cnorm, snorm));
        numerical.setInitialState(new SpacecraftState(orbit));
        numerical.setOrbitType(OrbitType.CARTESIAN);

        final double eps = 2E-3;

        // Propagation (actual and expected)
        final KeplerianOrbit actual = (KeplerianOrbit) propagator.propagate(date.shiftedBy(orbit.getKeplerianPeriod()))
            .getOrbit();
        final KeplerianOrbit expected = new KeplerianOrbit(numerical.propagate(
            date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit());

        // Check results
        Assert.assertEquals(
            0.,
            this.relDiff(expected.getPVCoordinates().getPosition().getX(), actual.getPVCoordinates().getPosition()
                .getX()), eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expected.getPVCoordinates().getPosition().getY(), actual.getPVCoordinates().getPosition()
                .getY()), eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expected.getPVCoordinates().getPosition().getZ(), actual.getPVCoordinates().getPosition()
                .getZ()), eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expected.getPVCoordinates().getVelocity().getX(), actual.getPVCoordinates().getVelocity()
                .getX()), eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expected.getPVCoordinates().getVelocity().getY(), actual.getPVCoordinates().getVelocity()
                .getY()), eps);
        Assert.assertEquals(
            0.,
            this.relDiff(expected.getPVCoordinates().getVelocity().getZ(), actual.getPVCoordinates().getVelocity()
                .getZ()), eps);
        Assert.assertEquals(actual.getFrame(), FramesFactory.getGCRF());
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation with orbit frame (GCRF) different from body frame (ITRF)
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
            final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2,
                PositionAngle.MEAN, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 3.986004415E14);
            new J2SecularPropagator(orbit, 6378136.46, 3.986004415E14, -0.0010826264572317671, FramesFactory.getITRF());
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
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation with orbit frame (MOD) different from propagation frame (GCRF)
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
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378136.3, 3.98600442e+14,
            -0.001082626613, FramesFactory.getCIRF());

        // Propagation in GCRF
        final Orbit actual = propagator.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();

        // Propagation in GCRF frame with initial orbit in MOD
        final Orbit orbit2 = orbit.getType().convertOrbit(orbit, FramesFactory.getMOD(false));
        final J2SecularPropagator propagator2 = new J2SecularPropagator(orbit2, 6378136.3, 3.98600442e+14,
            -0.001082626613, FramesFactory.getCIRF());
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
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation until initial date (no propagation)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 0s propagation
     * 
     * @testPassCriteria final orbit is exactly identical to initial orbit
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationInitialDate() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2,
            PositionAngle.MEAN, FramesFactory.getCIRF(), date, 3.98600442E14);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());

        // Propagation
        final KeplerianOrbit actual = (KeplerianOrbit) propagator.propagate(date).getOrbit();

        // Check results (Celestlab reference)
        Assert.assertEquals(orbit.getA(), actual.getA(), 0);
        Assert.assertEquals(orbit.getE(), actual.getE(), 0);
        Assert.assertEquals(orbit.getI(), actual.getI(), 0);
        Assert.assertEquals(orbit.getPerigeeArgument(), actual.getPerigeeArgument(), 0);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNode(), actual.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(orbit.getMeanAnomaly(), actual.getMeanAnomaly(), 0);
        Assert.assertEquals(0., actual.getDate().durationFrom(date), 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation (nominal case)
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
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());

        final double eps = 1E-15;

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
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation with mass provider (using all constructors)
     * 
     * @input Keplerian orbit, mass provider
     * 
     * @output Keplerian orbit and mass after 1 day propagation
     * 
     * @testPassCriteria mass is as expected, orbit is also as expected (reference : Celestlab 3.1.0, tolerance: 1E-16)
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
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final MassProvider mass = new SimpleMassModel(1000., "mass");
        final J2SecularPropagator propagator1 = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF(), mass);
        final J2SecularPropagator propagator2 = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF(), null, mass);
        final J2SecularPropagator propagator3 = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF(), null, null, mass);

        final double eps = 1E-16;

        // Propagation
        final SpacecraftState finalState1 = propagator1.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit1 = (KeplerianOrbit) finalState1.getOrbit();

        final SpacecraftState finalState2 = propagator2.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit2 = (KeplerianOrbit) finalState2.getOrbit();

        final SpacecraftState finalState3 = propagator3.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actualOrbit3 = (KeplerianOrbit) finalState3.getOrbit();

        // Check: final state vector (Celestlab reference) and mass
        final double[] expected = { 7000000, 0.01, 0.001, 1.82198195888265158, -0.02559291023856020,
            93.4654442804006464 };
        final double[] actual1 = { actualOrbit1.getA(), actualOrbit1.getE(), actualOrbit1.getI(),
            actualOrbit1.getPerigeeArgument(), actualOrbit1.getRightAscensionOfAscendingNode(),
            actualOrbit1.getMeanAnomaly() };
        final double[] actual2 = { actualOrbit2.getA(), actualOrbit2.getE(), actualOrbit2.getI(),
            actualOrbit2.getPerigeeArgument(), actualOrbit2.getRightAscensionOfAscendingNode(),
            actualOrbit2.getMeanAnomaly() };
        final double[] actual3 = { actualOrbit3.getA(), actualOrbit3.getE(), actualOrbit3.getI(),
            actualOrbit3.getPerigeeArgument(), actualOrbit3.getRightAscensionOfAscendingNode(),
            actualOrbit3.getMeanAnomaly() };
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], actual1[i], eps);
            Assert.assertEquals(expected[i], actual2[i], eps);
            Assert.assertEquals(expected[i], actual3[i], eps);
        }

        Assert.assertEquals(1000., finalState1.getMass("mass"), 0.);
        Assert.assertEquals(1000., finalState2.getMass("mass"), 0.);
        Assert.assertEquals(1000., finalState3.getMass("mass"), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation with attitude providers (using all constructors)
     * 
     * @input Keplerian orbit, attitude providers
     * 
     * @output Keplerian orbit and attitude after 1 day propagation
     * 
     * @testPassCriteria attitude is as expected, orbit is also as expected (reference : Celestlab 3.1.0, tolerance:
     *                   1E-16) although already checked in testPropagation(),
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
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final MassProvider mass = new SimpleMassModel(1000., "mass");
        final Rotation rot1 = Rotation.IDENTITY;
        final Rotation rot2 = new Rotation(Vector3D.PLUS_K, 3.2);
        final AttitudeProvider attitudeProvider = new ConstantAttitudeLaw(orbit.getFrame(), rot1);
        final AttitudeProvider attitudeProviderForces = new ConstantAttitudeLaw(orbit.getFrame(), rot1);
        final AttitudeProvider attitudeProviderEvents = new ConstantAttitudeLaw(orbit.getFrame(), rot2);
        final J2SecularPropagator propagator1 = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF(), attitudeProvider);
        final J2SecularPropagator propagator2 = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF(), attitudeProviderForces, attitudeProviderEvents);
        final J2SecularPropagator propagator3 = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF(), attitudeProvider, mass);
        final J2SecularPropagator propagator4 = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF(), attitudeProviderForces, attitudeProviderEvents, mass);

        final double eps = 1E-16;

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
        final double[] expected = { 7000000, 0.01, 0.001, 1.82198195888265158, -0.02559291023856020,
            93.4654442804006464 };
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
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], actual1[i], eps);
            Assert.assertEquals(expected[i], actual2[i], eps);
            Assert.assertEquals(expected[i], actual3[i], eps);
            Assert.assertEquals(expected[i], actual4[i], eps);
        }

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
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the J2 secular propagation with J2 = 0 => Keplerian propagation
     * 
     * @input Keplerian orbit, J2 = 0
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is initial orbit except for mean anomaly computed thanks to orbit mean motion
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
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, 0,
            FramesFactory.getCIRF());

        // Propagation
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(86400.));
        final KeplerianOrbit actual = (KeplerianOrbit) finalState.getOrbit();

        // Check results (Celestlab reference)
        Assert.assertEquals(7000000, actual.getA(), 0);
        Assert.assertEquals(0.01, actual.getE(), 0);
        Assert.assertEquals(0.001, actual.getI(), 0);
        Assert.assertEquals(FastMath.PI / 2., actual.getPerigeeArgument(), 0);
        Assert.assertEquals(0.1, actual.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(0.2 + orbit.getKeplerianMeanMotion() * 86400., actual.getMeanAnomaly(), 0);
        Assert.assertEquals(86400, actual.getDate().durationFrom(date), 0);
    }

    /**
     * @throws PatriusException
     * @throws IOException
     * @throws URISyntaxException
     * @testType VT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description validation of the J2 secular propagation model
     * 
     * @input Keplerian orbit
     * 
     * @output Ephemeris of Keplerian orbits, sampled every 15min
     * 
     * @testPassCriteria orbits are as expected (reference : Celestlab 3.1.0, tolerance: 1E-14)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationValidation() throws PatriusException, IOException, URISyntaxException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());

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

        final double eps = 2E-14;

        // Read reference data
        final List<double[]> reference = this.readData("analytical" + File.separator + "J2SecularCelestlab");

        // Propagation
        propagator.propagate(date.shiftedBy(86400.));

        // Check results (Celestlab reference)
        for (int i = 0; i < reference.size(); i++) {
            for (int j = 0; j < reference.get(i).length; j++) {
                Assert.assertEquals(reference.get(i)[j], actual.get(i)[j], eps);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#getTransitionMatrix(AbsoluteDate)}
     * 
     * @description test the J2 secular transition matrix computation
     * 
     * @input Keplerian orbit
     * 
     * @output Transtion matrix after 1 day propagation
     * 
     * @testPassCriteria transition matrix is as expected (reference : Celestlab, relative tolerance: 1E-13)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testTransitionMatrixCelestlab() throws PatriusException {

        final double eps = 1E-13;

        Report.printMethodHeader("testTransitionMatrix", "J2 transition matrix (keplerian)", "Celestlab 3.3.0", eps, ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final double a = 7000000;
        final double e = 0.1;
        final double i = FastMath.PI / 4.;
        final double pa = FastMath.PI / 2.;
        final double raan = 0.1;
        final double m = 0.2;
        final Frame frame = FramesFactory.getCIRF();
        final double mu = 3.98600442E14;
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());
        final AbsoluteDate finalDate = date.shiftedBy(86400.);

        // Get reference (Celestlab)
        final RealMatrix expected = new Array2DRowRealMatrix(new double[][] {
                { 1., 0., 0., 0., 0., 0. },
                { 0., 1., 0., 0., 0., 0. }, { 0., 0., 1., 0., 0., 0. },
                { -0.0000000480440219223922, 0.038823452058498742000, -0.320293479482614610000, 1., 0., 0. },
                { 0.0000000452963382623982, -0.0366031016261803770000, 0.0905926765247964390000, 0., 1., 0. },
                { -0.0000199744753513174570, 0.0096572117658293554000, -0.1912127929634210900000, 0., 0., 1. } });

        // Actual result
        final RealMatrix actual = propagator.getTransitionMatrix(finalDate);

        // Check results
        Report.printToReport("Transition matrix", expected.getData(false), actual.getData(false));
        for (int j = 0; j < 6; j++) {
            for (int k = 0; k < 6; k++) {
                Assert.assertEquals(0., relDiff(expected.getEntry(j, k), actual.getEntry(j, k)), eps);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#getTransitionMatrix(AbsoluteDate)}
     * 
     * @description test the J2 secular transition matrix computation
     * 
     * @input Keplerian orbit
     * 
     * @output Transtion matrix after 1 day propagation
     * 
     * @testPassCriteria transition matrix is as expected (reference : centered finite differences, relative tolerance: 1E-5, limited due to finite differences reference)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testTransitionMatrix() throws PatriusException {

        final double eps = 1E-5;

        Report.printMethodHeader("testTransitionMatrix", "J2 transition matrix (keplerian)", "Finites differences", eps, ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final double a = 7000000;
        final double e = 0.1;
        final double i = FastMath.PI / 4.;
        final double pa = FastMath.PI / 2.;
        final double raan = 0.1;
        final double m = 0.2;
        final Frame frame = FramesFactory.getCIRF();
        final double mu = 3.98600442E14;
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());
        final AbsoluteDate finalDate = date.shiftedBy(86400.);

        // Compute reference (centered finite differences)
        final double delta = 1E-6;
        final double da = a * delta;
        final double de = e * delta;
        final double di = i * delta;
        final double dpa = pa * delta;
        final double draan = raan * delta;
        final double dm = m * delta;
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a + da / 2., e, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit pdaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a - da / 2., e, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit mdaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e + de / 2., i, pa, raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit pdeOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e - de / 2., i, pa, raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit mdeOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i + di / 2., pa, raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit pdiOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i - di / 2., pa, raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit mdiOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa + dpa / 2., raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit pdpaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa - dpa / 2., raan, m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit mdpaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan + draan / 2., m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit pdraanOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan - draan / 2., m, PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit mdraanOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan, m + dm / 2., PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit pdmOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan, m - dm / 2., PositionAngle.MEAN, frame, date, mu)));
        final KeplerianOrbit mdmOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        final RealMatrix expected = new Array2DRowRealMatrix(new double[][] {
                { (pdaOrbit.getA() - mdaOrbit.getA()) / da, (pdaOrbit.getE() - mdaOrbit.getE()) / da, (pdaOrbit.getI() - mdaOrbit.getI()) / da, (pdaOrbit.getPerigeeArgument() - mdaOrbit.getPerigeeArgument()) / da, (pdaOrbit.getRightAscensionOfAscendingNode() - mdaOrbit.getRightAscensionOfAscendingNode()) / da, (pdaOrbit.getMeanAnomaly() - mdaOrbit.getMeanAnomaly()) / da },
                { (pdeOrbit.getA() - mdeOrbit.getA()) / de, (pdeOrbit.getE() - mdeOrbit.getE()) / de, (pdeOrbit.getI() - mdeOrbit.getI()) / de, (pdeOrbit.getPerigeeArgument() - mdeOrbit.getPerigeeArgument()) / de, (pdeOrbit.getRightAscensionOfAscendingNode() - mdeOrbit.getRightAscensionOfAscendingNode()) / de, (pdeOrbit.getMeanAnomaly() - mdeOrbit.getMeanAnomaly()) / de },
                { (pdiOrbit.getA() - mdiOrbit.getA()) / di, (pdiOrbit.getE() - mdiOrbit.getE()) / di, (pdiOrbit.getI() - mdiOrbit.getI()) / di, (pdiOrbit.getPerigeeArgument() - mdiOrbit.getPerigeeArgument()) / di, (pdiOrbit.getRightAscensionOfAscendingNode() - mdiOrbit.getRightAscensionOfAscendingNode()) / di, (pdiOrbit.getMeanAnomaly() - mdiOrbit.getMeanAnomaly()) / di },
                { (pdpaOrbit.getA() - mdpaOrbit.getA()) / dpa, (pdpaOrbit.getE() - mdpaOrbit.getE()) / dpa, (pdpaOrbit.getI() - mdpaOrbit.getI()) / dpa, (pdpaOrbit.getPerigeeArgument() - mdpaOrbit.getPerigeeArgument()) / dpa, (pdpaOrbit.getRightAscensionOfAscendingNode() - mdpaOrbit.getRightAscensionOfAscendingNode()) / dpa, (pdpaOrbit.getMeanAnomaly() - mdpaOrbit.getMeanAnomaly()) / dpa },
                { (pdraanOrbit.getA() - mdraanOrbit.getA()) / draan, (pdraanOrbit.getE() - mdraanOrbit.getE()) / draan, (pdraanOrbit.getI() - mdpaOrbit.getI()) / raan, (pdraanOrbit.getPerigeeArgument() - mdraanOrbit.getPerigeeArgument()) / draan, (pdraanOrbit.getRightAscensionOfAscendingNode() - mdraanOrbit.getRightAscensionOfAscendingNode()) / draan, (pdraanOrbit.getMeanAnomaly() - mdraanOrbit.getMeanAnomaly()) / draan },
                { (pdmOrbit.getA() - mdmOrbit.getA()) / dm, (pdmOrbit.getE() - mdmOrbit.getE()) / dm, (pdmOrbit.getI() - mdmOrbit.getI()) / dm, (pdmOrbit.getPerigeeArgument() - mdmOrbit.getPerigeeArgument()) / dm, (pdmOrbit.getRightAscensionOfAscendingNode() - mdmOrbit.getRightAscensionOfAscendingNode()) / dm, (pdmOrbit.getMeanAnomaly() - mdmOrbit.getMeanAnomaly()) / dm },
                }).transpose();

        // Actual result
        final RealMatrix actual = propagator.getTransitionMatrix(finalDate);

        // Check results
        Report.printToReport("Transition matrix", expected.getData(false), actual.getData(false));
        for (int j = 0; j < 6; j++) {
            for (int k = 0; k < 6; k++) {
                Assert.assertEquals(0., relDiff(expected.getEntry(j, k), actual.getEntry(j, k)), eps);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#getTransitionMatrix(AbsoluteDate)}
     * 
     * @description test the J2 secular transition matrix computation in different orbit frame
     * 
     * @input Keplerian orbit
     * 
     * @output Transtion matrix after 1 day propagation
     * 
     * @testPassCriteria transition matrix is as expected (reference : centered finite differences, relative tolerance: 1E-5, limited due to finite differences reference)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testTransitionMatrixFrame() throws PatriusException {

        final double eps = 1E-5;

        Report.printMethodHeader("testTransitionMatrixFrame", "J2 transition matrix (GCRF <=> CIRF)", "Finites differences", eps, ComparisonType.RELATIVE);

        // Define new frame (simple extension of CIRF frame)
        final Frame frame = new Frame(FramesFactory.getCIRF(), Transform.IDENTITY, "Frame");

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final double a = 7000000;
        final double e = 0.1;
        final double i = FastMath.PI / 4.;
        final double pa = FastMath.PI / 2.;
        final double raan = 0.1;
        final double m = 0.2;
        final double mu = 3.98600442E14;
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, m, PositionAngle.MEAN, frame, date, mu);
        final Frame cirf = FramesFactory.getCIRF();
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());
        final AbsoluteDate finalDate = date.shiftedBy(86400.);

        // Compute reference (centered finite differences)
        final double delta = 1E-6;
        final double da = a * delta;
        final double de = e * delta;
        final double di = i * delta;
        final double dpa = pa * delta;
        final double draan = raan * delta;
        final double dm = m * delta;
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a + da / 2., e, i, pa, raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit pdaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a - da / 2., e, i, pa, raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit mdaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e + de / 2., i, pa, raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit pdeOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e - de / 2., i, pa, raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit mdeOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i + di / 2., pa, raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit pdiOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i - di / 2., pa, raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit mdiOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa + dpa / 2., raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit pdpaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa - dpa / 2., raan, m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit mdpaOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan + draan / 2., m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit pdraanOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan - draan / 2., m, PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit mdraanOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan, m + dm / 2., PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit pdmOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new KeplerianOrbit(a, e, i, pa, raan, m - dm / 2., PositionAngle.MEAN, cirf, date, mu)));
        final KeplerianOrbit mdmOrbit = (KeplerianOrbit) propagator.propagateOrbit(finalDate);

        final RealMatrix expected = new Array2DRowRealMatrix(new double[][] {
                { (pdaOrbit.getA() - mdaOrbit.getA()) / da, (pdaOrbit.getE() - mdaOrbit.getE()) / da, (pdaOrbit.getI() - mdaOrbit.getI()) / da, (pdaOrbit.getPerigeeArgument() - mdaOrbit.getPerigeeArgument()) / da, (pdaOrbit.getRightAscensionOfAscendingNode() - mdaOrbit.getRightAscensionOfAscendingNode()) / da, (pdaOrbit.getMeanAnomaly() - mdaOrbit.getMeanAnomaly()) / da },
                { (pdeOrbit.getA() - mdeOrbit.getA()) / de, (pdeOrbit.getE() - mdeOrbit.getE()) / de, (pdeOrbit.getI() - mdeOrbit.getI()) / de, (pdeOrbit.getPerigeeArgument() - mdeOrbit.getPerigeeArgument()) / de, (pdeOrbit.getRightAscensionOfAscendingNode() - mdeOrbit.getRightAscensionOfAscendingNode()) / de, (pdeOrbit.getMeanAnomaly() - mdeOrbit.getMeanAnomaly()) / de },
                { (pdiOrbit.getA() - mdiOrbit.getA()) / di, (pdiOrbit.getE() - mdiOrbit.getE()) / di, (pdiOrbit.getI() - mdiOrbit.getI()) / di, (pdiOrbit.getPerigeeArgument() - mdiOrbit.getPerigeeArgument()) / di, (pdiOrbit.getRightAscensionOfAscendingNode() - mdiOrbit.getRightAscensionOfAscendingNode()) / di, (pdiOrbit.getMeanAnomaly() - mdiOrbit.getMeanAnomaly()) / di },
                { (pdpaOrbit.getA() - mdpaOrbit.getA()) / dpa, (pdpaOrbit.getE() - mdpaOrbit.getE()) / dpa, (pdpaOrbit.getI() - mdpaOrbit.getI()) / dpa, (pdpaOrbit.getPerigeeArgument() - mdpaOrbit.getPerigeeArgument()) / dpa, (pdpaOrbit.getRightAscensionOfAscendingNode() - mdpaOrbit.getRightAscensionOfAscendingNode()) / dpa, (pdpaOrbit.getMeanAnomaly() - mdpaOrbit.getMeanAnomaly()) / dpa },
                { (pdraanOrbit.getA() - mdraanOrbit.getA()) / draan, (pdraanOrbit.getE() - mdraanOrbit.getE()) / draan, (pdraanOrbit.getI() - mdpaOrbit.getI()) / raan, (pdraanOrbit.getPerigeeArgument() - mdraanOrbit.getPerigeeArgument()) / draan, (pdraanOrbit.getRightAscensionOfAscendingNode() - mdraanOrbit.getRightAscensionOfAscendingNode()) / draan, (pdraanOrbit.getMeanAnomaly() - mdraanOrbit.getMeanAnomaly()) / draan },
                { (pdmOrbit.getA() - mdmOrbit.getA()) / dm, (pdmOrbit.getE() - mdmOrbit.getE()) / dm, (pdmOrbit.getI() - mdmOrbit.getI()) / dm, (pdmOrbit.getPerigeeArgument() - mdmOrbit.getPerigeeArgument()) / dm, (pdmOrbit.getRightAscensionOfAscendingNode() - mdmOrbit.getRightAscensionOfAscendingNode()) / dm, (pdmOrbit.getMeanAnomaly() - mdmOrbit.getMeanAnomaly()) / dm },
                }).transpose();

        // Actual result
        propagator.resetInitialState(new SpacecraftState(orbit));
        final RealMatrix actual = propagator.getTransitionMatrix(finalDate);

        // Check results
        Report.printToReport("Transition matrix", expected.getData(false), actual.getData(false));
        for (int j = 0; j < 6; j++) {
            for (int k = 0; k < 6; k++) {
                Assert.assertEquals(0., relDiff(expected.getEntry(j, k), actual.getEntry(j, k)), eps);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#J2_SECULAR_PROPAGATOR}
     * 
     * @testedMethod {@link J2SecularPropagator#getTransitionMatrix(AbsoluteDate)}
     * 
     * @description test the J2 secular transition matrix computation in different orbit type
     * 
     * @input Keplerian orbit
     * 
     * @output Transtion matrix after 1 day propagation
     * 
     * @testPassCriteria transition matrix is as expected (reference : centered finite differences, relative tolerance: 1.5E-5, limited due to finite differences reference)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testTransitionMatrixType() throws PatriusException {

        final double eps = 1.5E-5;

        Report.printMethodHeader("testTransitionMatrixType", "J2 transition matrix (equinoctial)", "Finites differences", eps, ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final double a = 7000000;
        final double ex = -0.009983341664682819;
        final double ey = 0.09950041652780257;
        final double ix = 0.1121442198758789;
        final double iy = 0.04135235515316014;
        final double m = 1.8707963267948966;
        final Frame frame = FramesFactory.getCIRF();
        final double mu = 3.98600442E14;
        final Orbit orbit = new EquinoctialOrbit(a, ex, ey, ix, iy, m, PositionAngle.MEAN, frame, date, mu);
        final J2SecularPropagator propagator = new J2SecularPropagator(orbit, 6378000, 3.98600442E14, -0.001082626613,
            FramesFactory.getCIRF());
        final AbsoluteDate finalDate = date.shiftedBy(86400.);

        // Compute reference (centered finite differences)
        final double delta = 1E-6;
        final double da = a * delta;
        final double dex = ex * delta;
        final double dey = ey * delta;
        final double dix = ix * delta;
        final double diy = iy * delta;
        final double dm = m * delta;
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a + da / 2., ex, ey, ix, iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit pdaOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a - da / 2., ex, ey, ix, iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit mdaOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex + dex / 2., ey, ix, iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit pdexOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex - dex / 2., ey, ix, iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit mdexOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey + dey / 2., ix, iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit pdeyOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey - dey / 2., ix, iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit mdeyOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey, ix + dix / 2., iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit pdixOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey, ix - dix / 2., iy, m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit mdixOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey, ix, iy + diy / 2., m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit pdiyOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey, ix, iy - diy / 2., m, PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit mdiyOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);

        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey, ix, iy, m + dm / 2., PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit pdmOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(a, ex, ey, ix, iy, m - dm / 2., PositionAngle.MEAN, frame, date, mu)));
        final EquinoctialOrbit mdmOrbit = (EquinoctialOrbit) propagator.propagateOrbit(finalDate);

        final RealMatrix expected = new Array2DRowRealMatrix(new double[][] {
                { (pdaOrbit.getA() - mdaOrbit.getA()) / da, (pdaOrbit.getEquinoctialEx() - mdaOrbit.getEquinoctialEx()) / da, (pdaOrbit.getEquinoctialEy() - mdaOrbit.getEquinoctialEy()) / da, (pdaOrbit.getHx() - mdaOrbit.getHx()) / da, (pdaOrbit.getHy() - mdaOrbit.getHy()) / da, (pdaOrbit.getLM() - mdaOrbit.getLM()) / da },
                { (pdexOrbit.getA() - mdexOrbit.getA()) / dex, (pdexOrbit.getEquinoctialEx() - mdexOrbit.getEquinoctialEx()) / dex, (pdexOrbit.getEquinoctialEy() - mdexOrbit.getEquinoctialEy()) / dex, (pdexOrbit.getHx() - mdexOrbit.getHx()) / dex, (pdexOrbit.getHy() - mdexOrbit.getHy()) / dex, (pdexOrbit.getLM() - mdexOrbit.getLM()) / dex },
                { (pdeyOrbit.getA() - mdeyOrbit.getA()) / dey, (pdeyOrbit.getEquinoctialEx() - mdeyOrbit.getEquinoctialEx()) / dey, (pdeyOrbit.getEquinoctialEy() - mdeyOrbit.getEquinoctialEy()) / dey, (pdeyOrbit.getHx() - mdeyOrbit.getHx()) / dey, (pdeyOrbit.getHy() - mdeyOrbit.getHy()) / dey, (pdeyOrbit.getLM() - mdeyOrbit.getLM()) / dey },
                { (pdixOrbit.getA() - mdixOrbit.getA()) / dix, (pdixOrbit.getEquinoctialEx() - mdixOrbit.getEquinoctialEx()) / dix, (pdixOrbit.getEquinoctialEy() - mdixOrbit.getEquinoctialEy()) / dix, (pdixOrbit.getHx() - mdixOrbit.getHx()) / dix, (pdixOrbit.getHy() - mdixOrbit.getHy()) / dix, (pdixOrbit.getLM() - mdixOrbit.getLM()) / dix },
                { (pdiyOrbit.getA() - mdiyOrbit.getA()) / diy, (pdiyOrbit.getEquinoctialEx() - mdiyOrbit.getEquinoctialEx()) / diy, (pdiyOrbit.getEquinoctialEy() - mdixOrbit.getEquinoctialEy()) / iy, (pdiyOrbit.getHx() - mdiyOrbit.getHx()) / diy, (pdiyOrbit.getHy() - mdiyOrbit.getHy()) / diy, (pdiyOrbit.getLM() - mdiyOrbit.getLM()) / diy },
                { (pdmOrbit.getA() - mdmOrbit.getA()) / dm, (pdmOrbit.getEquinoctialEx() - mdmOrbit.getEquinoctialEx()) / dm, (pdmOrbit.getEquinoctialEy() - mdmOrbit.getEquinoctialEy()) / dm, (pdmOrbit.getHx() - mdmOrbit.getHx()) / dm, (pdmOrbit.getHy() - mdmOrbit.getHy()) / dm, (pdmOrbit.getLM() - mdmOrbit.getLM()) / dm },
                }).transpose();

        // Actual result
        final RealMatrix actual = propagator.getTransitionMatrix(finalDate);

        // Check results
        Report.printToReport("Transition matrix", expected.getData(false), actual.getData(false));
        for (int j = 0; j < 6; j++) {
            for (int k = 0; k < 6; k++) {
                if (j != 2 && k != 4) {
                    // j == 2 and k == 4: initial value is very small (1E-9) and probably leads to high round-off errors
                    Assert.assertEquals(0., relDiff(expected.getEntry(j, k), actual.getEntry(j, k)), eps);
                }
            }
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
