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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:84:20/11/2013:added test for coverage purpose
 * VERSION::FA:178:06/01/2014:Corrected log id format
 * VERSION::DM:84:07/04/2014:test updated to match the new TwoSpinBiasSlew signature
 * VERSION::DM:278:21/07/2014:added test for coverage purpose
 * VERSION::DM:282:22/07/2014:added getEphemeris method test
 * VERSION::FA:279:11/09/2014:slew final date anomaly
 * VERSION::FA:366:21/11/2014:added test for computeMaxDuration method
 * VERSION::FA:367:04/12/2014:Recette V2.3 corrections (changed getAttitudes return type)
 * VERSION::DM:362:20/02/2015:Corrected interval of validity
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::DM:489:17/11/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:681:19/10/2016:Correction last target law point
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.slew.TwoSpinBiasSlewComputer;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the two spin bias slew.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: TwoSpinBiasSlewTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 2.1
 * 
 */
public class TwoSpinBiasSlewTest {

    double PRECISIONDATE = 1e-10;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Spin bias slew tests
         * 
         * @featureDescription Tests with the spin bias slew computation
         * 
         * @coveredRequirements DV-ATT_290
         */
        SPIN_BIAS_SLEW
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TwoSpinBiasSlewTest.class.getSimpleName(), "Two spin bias slew attitude provider");
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link TabulatedSlew#copy(AbsoluteDateInterval)}
     * 
     * @description Test the new method
     * 
     * @input parameters
     * 
     * @output AbsoluteDateInterval
     * 
     * @testPassCriteria The method behavior is correct
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testCopyMethod() throws PatriusException {

        // Compute slew
        final TabulatedSlew slew1 = new TwoSpinBiasSlewComputer(this.step,
                this.theta_max, this.tau, this.epsInRall, this.omega2, this.theta, this.epsOutRall, this.omega1,
                this.dtStab).compute(orbit, startLaw, initialDate, finalLaw, null);

        // Copy and truncate slew

        // Test case n°1 : in a standard usage, the interval stored should be updated
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(this.initialDate,
                this.initialDate.shiftedBy(10.));
        Assert.assertTrue(slew1.copy(newIntervalOfValidity).getTimeInterval().equals(newIntervalOfValidity));

        // Test case n°2 : if we send an opened interval, it is closed before to process the truncation
        final AbsoluteDateInterval newIntervalOfValidityOpen = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
                this.initialDate, this.initialDate.shiftedBy(10.), IntervalEndpointType.OPEN);
        Assert.assertFalse(slew1.copy(newIntervalOfValidityOpen).getTimeInterval().equals(newIntervalOfValidityOpen));
        Assert.assertTrue(slew1.copy(newIntervalOfValidityOpen).getTimeInterval().equals(newIntervalOfValidity));

        // Test case n°3 : when the new interval isn't included, the method copy should throw an exception
        try {
            final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
                    IntervalEndpointType.OPEN, this.initialDate.shiftedBy(-10), this.initialDate.shiftedBy(10.),
                    IntervalEndpointType.OPEN);
            slew1.copy(newIntervalOfValidityNotIncluded);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     *         when orientation cannot be computed
     * @testType UT
     * 
     * @testedFeature {@link features#SPIN_BIAS_SLEW}
     * 
     * @testedMethod {@link TwoSpinBiasSlewComputer#computeMaxDuration()}
     * @testedMethod {@link TwoSpinBiasSlewComputer#getAttitude(AbsoluteDate, Frame)}
     * 
     * @description coverage test
     * 
     * @input a TwoSpinBiasSlew profile
     * 
     * @output the computed slews
     * 
     * @testPassCriteria coverage test
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void test() throws PatriusException {

        boolean rez = false;
        final double mu = Constants.EGM96_EARTH_MU;
        final Orbit orbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.),
                MathLib.toRadians(270.), MathLib.toRadians(5.300), PositionAngle.MEAN, FramesFactory.getEME2000(),
                this.initialDate, mu);

        // TwoSpinBiasSlew profile with an theta max too small:
        rez = false;
        try {
            final TabulatedSlew slew1 = new TwoSpinBiasSlewComputer(
                    this.step, 0.002, this.tau, this.epsInRall, this.omega2, this.theta, this.epsOutRall, this.omega1,
                    this.dtStab).compute(orbit, startLaw, initialDate, finalLaw, null);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        final TwoSpinBiasSlewComputer slew2 = new TwoSpinBiasSlewComputer(this.step,
                this.theta_max, this.tau, this.epsInRall, this.omega2, this.theta, this.epsOutRall, this.omega1,
                this.dtStab);
        final TabulatedSlew slew3 = slew2.compute(orbit, startLaw, initialDate, finalLaw, null);

        /*
         * FT 278 : Test for computeMaxDuration method
         */
        // expected slew computed with matlab
        final double exectedSlew = 280;
        Assert.assertEquals(exectedSlew, slew2.computeMaxDuration(), Precision.DOUBLE_COMPARISON_EPSILON);

        /*
         * FT 279 : Tests added for testing that the slew duration is multiple of AOCS step
         */
        Assert.assertEquals(MathLib.round(slew3.getTimeInterval().getDuration() / this.step) * this.step, slew3
                .getTimeInterval().getDuration(), this.PRECISIONDATE);

        // TwoSpinBiasSlew profile with an epsInRall too high
        rez = false;
        try {
            final TabulatedSlew slew4 = new TwoSpinBiasSlewComputer(
                    this.step, this.theta_max, this.tau, 10, this.omega2, this.theta, this.epsOutRall, this.omega1,
                    this.dtStab).compute(orbit, startLaw, initialDate, finalLaw, null);
            rez = true;
        } catch (final PatriusException e) {
            rez = false;
        }
        Assert.assertTrue(rez);

        // TwoSpinBiasSlew profile with a omegaHigh and thetaSwitch too high
        rez = false;
        try {
            final TwoSpinBiasSlewComputer slew5 = new TwoSpinBiasSlewComputer(
                    this.step, this.theta_max, this.tau, this.epsInRall, 1, 1, this.epsOutRall, this.omega1,
                    this.dtStab);
            slew5.computeMaxDuration();
            rez = true;
        } catch (final PatriusException e) {
            rez = false;
        }
        Assert.assertTrue(rez);

        // TwoSpinBiasSlew profile with input date not included in the slew time interval
        rez = false;
        try {
            final TabulatedSlew slew7 = new TwoSpinBiasSlewComputer(
                    this.step, this.theta_max, this.tau, this.epsInRall, this.omega2, this.theta, this.epsOutRall,
                    this.omega1, this.dtStab).compute(orbit, startLaw, initialDate, startLaw, null);
            slew7.getAttitude(this.initialDate.shiftedBy(-100.0), this.itrf);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.attitudes.TabulatedSlew#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider)}
     * .
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testGetAttitude() throws PatriusException {

        final TabulatedSlew slew2 = new TwoSpinBiasSlewComputer(this.step,
                this.theta_max, this.tau, this.epsInRall, this.omega2, this.theta, this.epsOutRall, this.omega1,
                this.dtStab).compute(orbit, startLaw, initialDate, finalLaw, null);
        final Attitude att = slew2.getAttitude(this.orbit);
        final Attitude att2 = slew2.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame());
        Assert.assertEquals(att.getDate(), att2.getDate());
        Assert.assertEquals(att.getReferenceFrame(), att2.getReferenceFrame());
        Assert.assertTrue(att.getRotation().isEqualTo(att2.getRotation()));
        Assert.assertEquals(0, att.getSpin().getNorm(), att2.getSpin().getNorm());

        // test acceleration before and after activation
        slew2.setSpinDerivativesComputation(true);
        final Vector3D acc = slew2.getAttitude(this.orbit, this.orbit.getDate().shiftedBy(1), this.orbit.getFrame())
                .getRotationAcceleration();
        Assert.assertNotSame(acc, Vector3D.ZERO);
    }

    @Test
    // test for rotation acceleration : compare actual acceleration and acceleration obtained with finite differences.
            public
            void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
                0, ComparisonType.ABSOLUTE);

        final TabulatedSlew slew = new TwoSpinBiasSlewComputer(this.step,
                this.theta_max, this.tau, this.epsInRall, this.omega2, this.theta, this.epsOutRall, this.omega1,
                this.dtStab).compute(orbit, startLaw, initialDate, finalLaw, null);

        final double duration = slew.getTimeInterval().getDuration();
        slew.setSpinDerivativesComputation(true);

        // frame
        final Frame frameToCompute = this.itrf;
        for (int i = 1; i < duration; i += 1) {
            final Vector3D acc = slew.getAttitude(slew.getTimeInterval().getLowerData().shiftedBy(i), frameToCompute)
                    .getRotationAcceleration();
            final Vector3D accDerivateSpin = this
                    .getSpinFunction(slew, null, frameToCompute, slew.getTimeInterval().getLowerData().shiftedBy(i))
                    .nthDerivative(1).getVector3D(slew.getTimeInterval().getLowerData().shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 0);
            if (i == 1) {
                Report.printToReport("Rotation acceleration", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        slew.setSpinDerivativesComputation(false);
        Assert.assertNull(slew.getAttitude(this.orbit).getRotationAcceleration());
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link TabulatedSlew#getNature()}
     * 
     * @description Test the new constructor which adds the "nature" attribute
     * 
     * @input parameters
     * 
     * @output slew
     * 
     * @testPassCriteria The nature attribute is well managed
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void FT2105() throws PatriusException {

        final String DEFAULT_NATURE = "TWO_SPIN_BIAS_SLEW";
        final String nature = "testNature";

        // Test all the 2 constructors of TwoSpinBiasSlew class
        final Slew slew1 = new TwoSpinBiasSlewComputer(this.step, this.theta_max, this.tau, this.epsInRall,
                this.omega2, this.theta, this.epsOutRall, this.omega1, this.dtStab).compute(this.orbit, startLaw,
                initialDate, finalLaw, null);
        final Slew slew2 = new TwoSpinBiasSlewComputer(this.step, this.theta_max, this.tau, this.epsInRall,
                this.omega2, this.theta, this.epsOutRall, this.omega1, this.dtStab, nature).compute(this.orbit, startLaw,
                initialDate, finalLaw, null);

        Assert.assertEquals(slew1.getNature(), DEFAULT_NATURE);
        Assert.assertEquals(slew2.getNature(), nature);
    }

    /**
     * Local function to provide spin function.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @param slew
     *        slew
     * @return spin function of date relative
     */
    public Vector3DFunction getSpinFunction(final Slew slew,
            final PVCoordinatesProvider pvProv,
            final Frame frame,
            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa) {
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return slew.getAttitude(date, frame).getSpin();
            }
        };
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        // Initial date:
        this.initialDate = new AbsoluteDate(new DateComponents(2008, 04, 07), TimeComponents.H00,
                TimeScalesFactory.getTAI());
        // Initial attitude law:
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final Frame frame = FramesFactory.getEME2000();
        this.startLaw = new CelestialBodyPointed(frame, sun, Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);
        // Final attitude law:
        this.itrf = FramesFactory.getITRF();
        this.finalLaw = new BodyCenterPointing(this.itrf);
        // Slew parameters:
        this.omega1 = 0.003;
        this.omega2 = 0.012;
        this.theta = 0.1;
        this.theta_max = 3.106686;
        this.step = 0.2;
        this.tau = 3.0;
        this.dtStab = 0.2;
        this.epsInRall = 7.0E-4;
        this.epsOutRall = 0.001;
        // The orbit
        final double mu = Constants.EGM96_EARTH_MU;
        this.orbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN, FramesFactory.getEME2000(), this.initialDate, mu);
    }

    /*
     * Initial Date
     */
    AbsoluteDate initialDate;

    /*
     * Initial attitude law
     */
    AttitudeLaw startLaw;

    /*
     * Final Attitude Law
     */
    AttitudeLaw finalLaw;

    /*
     * Slew Parameters
     */
    double omega1;
    double omega2;
    double theta;
    double theta_max;
    double step;
    double tau;
    double dtStab;
    double epsInRall;
    double epsOutRall;

    /*
     * Frames
     */
    Frame itrf;

    /*
     * The orbit
     */
    Orbit orbit;
}
