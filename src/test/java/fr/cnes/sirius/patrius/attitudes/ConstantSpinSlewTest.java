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
 * @history created 29/02/12
 * 
 * HISTORY
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:456:03/11/2015:Rajout check sur la valeur du spin
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1870:05/10/2018:remove slew recomputation in getAttitude() method
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.slew.ConstantSpinSlewComputer;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
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
 * <p>
 * This class tests the constant spin slew.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew
 * 
 * @author Julie Anton
 * 
 * @version $Id $
 * 
 * @since 1.1
 * 
 */
public class ConstantSpinSlewTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle constant spin slew tests
         * 
         * @featureDescription tests on the limit cases
         * 
         * @coveredRequirements DV-ATT_290, DV-ATT_291
         */
        LIMIT_CASES,
        /**
         * @featureTitle constant spin slew tests
         * 
         * @featureDescription tests with the duration constraint
         * 
         * @coveredRequirements DV-ATT_290, DV-ATT_291
         */
        DURATION_CONSTRAINT,
        /**
         * @featureTitle constant spin slew tests
         * 
         * @featureDescription tests with the angular velocity constraint
         * 
         * @coveredRequirements DV-ATT_290, DV-ATT_291
         */
        ANUGULAR_VELOCITY_CONSTRAINT
    }

    /** First attitude law. */
    private AttitudeLeg previousLaw;
    /** Second attitude law. */
    private AttitudeLeg nextLaw;
    /** PV coordinates provider. */
    CircularOrbit circOrbit;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ConstantSpinSlewTest.class.getSimpleName(), "Constant spin slew attitude provider");
    }

    /**
     * @throws PatriusException
     *         orekit exception
     * @testType UT
     * 
     * @testedFeature {@link features#LIMIT_CASES}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * 
     * @description test the constant spin slew on the limit cases.
     * 
     * @input two attitude laws which have to be joined by the constant spin slew.
     * 
     * @output exceptions.
     * 
     * @testPassCriteria when the user asks for an attitude outside the interval of validity, an exception is thrown ;
     *                   when the user asks for an attitude before the slew computation, an exception is
     *                   thrown.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testLimitCases() throws PatriusException {
        // start date
        final AbsoluteDate refdate = this.previousLaw.getTimeInterval().getUpperData();
        // constant spin slew
        final Slew slerp = new ConstantSpinSlew(
                this.previousLaw.getAttitude(circOrbit, refdate, FramesFactory.getEME2000()), this.nextLaw.getAttitude(circOrbit,
                        refdate.shiftedBy(300), FramesFactory.getEME2000()));

        final AbsoluteDateInterval intervalOfValidity = slerp.getTimeInterval();

        // when the user asks for an attitude outside the interval of validity, an exception is thrown
        try {
            slerp.getAttitude(intervalOfValidity.getLowerData().shiftedBy(-1), FramesFactory.getITRF());
            Assert.fail();
        } catch (final PatriusException e) {
        }

        try {
            slerp.getAttitude(intervalOfValidity.getLowerData().shiftedBy(301), FramesFactory.getITRF());
            Assert.fail();
        } catch (final PatriusException e) {
        }
    }

    /**
     * @throws PatriusException
     *         orekit exception
     * @testType UT
     * 
     * @testedFeature {@link features#DURATION_CONSTRAINT}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * 
     * @description test the constant spin slew constrained in duration.
     * 
     * @input two attitude laws which have to be joined by the constant spin slew.
     * 
     * @output duration of the slew, initial and final attitudes.
     * 
     * @testPassCriteria the duration is the expected one, the intial and the final attitudes are the expected ones.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testDurationConstraint() throws PatriusException {
        // Case : the given date is the start date
        ConstantSpinSlew slerp =
            new ConstantSpinSlew(this.previousLaw.getAttitude(circOrbit, this.previousLaw.getTimeInterval().getUpperData(), FramesFactory.getITRF()), 
                    this.nextLaw.getAttitude(circOrbit, this.previousLaw.getTimeInterval().getUpperData().shiftedBy(300), FramesFactory.getITRF()));

        // time interval of the slew
        AbsoluteDateInterval interval = slerp.getTimeInterval();

        Assert.assertEquals(300., interval.getDuration(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(300., slerp.getDuration(), Precision.DOUBLE_COMPARISON_EPSILON);

        Assert.assertTrue(this.previousLaw.getTimeInterval().getUpperData().equals(interval.getLowerData()));

        Attitude att;

        att = slerp.getAttitude(null, interval.getLowerData(), FramesFactory.getITRF());
        Attitude initialAttitude =
            this.previousLaw.getAttitude(this.circOrbit, this.previousLaw.getTimeInterval().getUpperData(),
                FramesFactory.getITRF());

        Assert.assertTrue(this.compareAttitude(att, initialAttitude));

        att = slerp.getAttitude(interval.getUpperData(), FramesFactory.getITRF());
        Attitude finalAttitude = this.nextLaw.getAttitude(this.circOrbit,
            this.nextLaw.getTimeInterval().getLowerData().shiftedBy(interval.getDuration()),
            FramesFactory.getITRF());

        Assert.assertTrue(this.compareAttitude(att, finalAttitude));

        // Check spin value
        Rotation initialRot = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getGCRF())
            .getRotation();
        Rotation finalRot = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getGCRF())
            .getRotation();
        Rotation rot = finalRot.applyInverseTo(initialRot);
        Vector3D axis = initialRot.applyInverseTo(rot.getAxis());
        Vector3D spinReference = axis.scalarMultiply(rot.getAngle() / 300.);
        Vector3D actualSpin = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getGCRF())
            .getSpin();
        Assert.assertEquals(spinReference.distance(actualSpin), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);

        // Case : the given date is not the start date

        slerp =
            new ConstantSpinSlew(this.previousLaw.getAttitude(circOrbit, this.nextLaw.getTimeInterval().getLowerData().shiftedBy(-300), FramesFactory.getEME2000()),
                    this.nextLaw.getAttitude(circOrbit, this.nextLaw.getTimeInterval().getLowerData(), FramesFactory.getEME2000()));
        slerp.getAttitude(this.circOrbit, this.nextLaw.getTimeInterval().getLowerData(), FramesFactory.getEME2000());

        // time interval of the slew
        interval = slerp.getTimeInterval();

        Assert.assertEquals(300., interval.getDuration(), Precision.DOUBLE_COMPARISON_EPSILON);

        Assert.assertTrue(this.nextLaw.getTimeInterval().getLowerData().equals(interval.getUpperData()));

        att = slerp.getAttitude(this.circOrbit, interval.getLowerData(), FramesFactory.getITRF());
        initialAttitude = this.previousLaw.getAttitude(this.circOrbit,
            this.previousLaw.getTimeInterval().getUpperData().shiftedBy(-interval.getDuration()),
            FramesFactory.getITRF());

        Assert.assertTrue(this.compareAttitude(att, initialAttitude));

        att = slerp.getAttitude(interval.getUpperData(), FramesFactory.getITRF());
        finalAttitude = this.nextLaw.getAttitude(this.circOrbit, this.nextLaw.getTimeInterval().getLowerData(),
            FramesFactory.getITRF());

        Assert.assertTrue(this.compareAttitude(att, finalAttitude));

        // Check spin value
        initialRot = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getGCRF()).getRotation();
        finalRot = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getGCRF()).getRotation();
        rot = finalRot.applyInverseTo(initialRot);
        axis = initialRot.applyInverseTo(rot.getAxis());
        spinReference = axis.scalarMultiply(rot.getAngle() / 300.);
        actualSpin = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getGCRF()).getSpin();
        Assert.assertEquals(spinReference.distance(actualSpin), 0.0, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException
     *         orekit exception
     * @testType UT
     * 
     * @testedFeature {@link features#ANUGULAR_VELOCITY_CONSTRAINT}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     * 
     * @description test the constant spin slew constrained in angular velocity.
     * 
     * @input two attitude laws which have to be joined by the constant spin slew.
     * 
     * @output duration of the slew, initial and final attitudes.
     * 
     * @testPassCriteria the duration is the expected one, the intial and the final attitudes are the expected ones.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testAngularVelocityConstraint() throws PatriusException {
        // Case : the given date is the start date
        Slew slerp =
            new ConstantSpinSlewComputer(0.2).compute(circOrbit, this.previousLaw, this.previousLaw.getTimeInterval().getUpperData(), this.nextLaw, null);

        double duration = slerp.getTimeInterval().getDuration();

        Rotation initialRot = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getEME2000())
            .getRotation();
        Rotation finalRot = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getEME2000())
            .getRotation();

        double angle = Rotation.distance(initialRot, finalRot);

        Assert.assertEquals(angle / duration, 0.2, 1e-8);

        // Check spin value
        Rotation initialRotGcrf = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getGCRF())
            .getRotation();
        Rotation finalRotGcrf = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getGCRF())
            .getRotation();
        Rotation rot = finalRotGcrf.applyInverseTo(initialRotGcrf);
        Vector3D axis = initialRotGcrf.applyInverseTo(rot.getAxis());
        Vector3D spinReference = axis.scalarMultiply(0.2);
        Vector3D actualSpin = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getGCRF())
            .getSpin();
        Assert.assertEquals(spinReference.distance(actualSpin), 0.0, 0);

        // Case : the given date is not the start date
        slerp = new ConstantSpinSlewComputer(0.2).compute(circOrbit, this.previousLaw, null, this.nextLaw, this.nextLaw
                .getTimeInterval().getLowerData());

        duration = slerp.getTimeInterval().getDuration();
        Assert.assertTrue(slerp.getTimeInterval().getLowerEndpoint() == IntervalEndpointType.CLOSED);
        Assert.assertTrue(slerp.getTimeInterval().getUpperEndpoint() == IntervalEndpointType.CLOSED);

        initialRot = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getEME2000())
            .getRotation();
        finalRot = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getEME2000()).getRotation();

        angle = Rotation.distance(initialRot, finalRot);

        Assert.assertEquals(angle / duration, 0.2, 1e-10);

        // Check spin value
        initialRotGcrf = slerp.getAttitude(slerp.getTimeInterval().getLowerData(), FramesFactory.getGCRF())
            .getRotation();
        finalRotGcrf = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getGCRF()).getRotation();
        rot = finalRotGcrf.applyInverseTo(initialRotGcrf);
        axis = initialRotGcrf.applyInverseTo(rot.getAxis());
        spinReference = axis.scalarMultiply(0.2);
        actualSpin = slerp.getAttitude(slerp.getTimeInterval().getUpperData(), FramesFactory.getGCRF()).getSpin();
        Assert.assertEquals(spinReference.distance(actualSpin), 0.0, 0);
    }

    @Test
    // test for rotation acceleration : compare actual acceleration and acceleration obtained with finite differences.
            public
            void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            1E-12, ComparisonType.ABSOLUTE);

        // Case : the given date is the start date
        final Slew slerp =
            new ConstantSpinSlew(this.previousLaw.getAttitude(circOrbit, this.previousLaw.getTimeInterval().getUpperData(), FramesFactory.getEME2000()),
                    this.nextLaw.getAttitude(circOrbit, this.previousLaw.getTimeInterval().getUpperData().shiftedBy(300), FramesFactory.getEME2000()));
        slerp.setSpinDerivativesComputation(true);

        final double duration = slerp.getTimeInterval().getDuration();

        // frame
        final Frame frameToCompute = FramesFactory.getTEME();
        for (int i = 1; i < duration; i += 1) {
            final Vector3D acc = slerp.getAttitude(slerp.getTimeInterval().getLowerData().shiftedBy(i), frameToCompute)
                .getRotationAcceleration();
            final Vector3D accDerivateSpin = this.getSpinFunction(slerp, null, frameToCompute,
                slerp.getTimeInterval().getLowerData().shiftedBy(i))
                .nthDerivative(1).getVector3D(slerp.getTimeInterval().getLowerData().shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 1e-12);
            if (i == 1) {
                Report.printToReport("Rotation acceleration", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is 0 when spin derivative is deactivated
        slerp.setSpinDerivativesComputation(false);
        Assert.assertNull(slerp.getAttitude(this.circOrbit, slerp.getTimeInterval().getLowerData(), frameToCompute)
            .getRotationAcceleration());
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link ConstantSpinSlew#copy(AbsoluteDateInterval)}
     * 
     * @description Test the new method
     * 
     * @input parameters
     * 
     * @output AbsoluteDateInterval
     * 
     * @testPassCriteria The method behavior is correct
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testCopyMethod() throws PatriusException {

        // Constructor dates
        final AbsoluteDate startDate = this.previousLaw.getTimeInterval().getUpperData();
        final AbsoluteDate endDate = startDate.shiftedBy(300);

        // Intervals creation
        final double offset = 5;
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityOpen = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.OPEN);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // Slew creation
        final Attitude att1 = this.previousLaw.getAttitude(circOrbit, startDate, FramesFactory.getEME2000());
        final Attitude att2 = this.nextLaw.getAttitude(circOrbit, endDate, FramesFactory.getEME2000());
        ConstantSpinSlew slerp1 = new ConstantSpinSlew(att1, att2);
        slerp1.setSpinDerivativesComputation(true);
        ConstantSpinSlew slerp2 = new ConstantSpinSlew(att1, att2);
        final Slew slerp3 = new ConstantSpinSlew(att1, att2);
        final Slew slerp4 = new ConstantSpinSlewComputer(0.2).compute(circOrbit, this.previousLaw, this.previousLaw
                .getTimeInterval().getUpperData(), this.nextLaw, null);

        final Attitude attitudeRef = slerp1.getAttitude(null, startDate.shiftedBy(5), FramesFactory.getEME2000());

        // Test case n°1 : in a standard usage, the interval stored should be updated
        slerp1 = slerp1.copy(newIntervalOfValidity);
        Assert.assertTrue(slerp1.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = slerp1.getAttitude(null, startDate.shiftedBy(5), FramesFactory.getEME2000());
        Assert.assertEquals(0, Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(0, attitudeActual.getRotationAcceleration().distance(attitudeRef.getRotationAcceleration()), 1E-15);

        // Test case n°2 : if we send an opened interval, it is closed before to process the truncation
        slerp2 = slerp2.copy(newIntervalOfValidityOpen);
        Assert.assertFalse(slerp2.getTimeInterval().equals(newIntervalOfValidityOpen));
        Assert.assertTrue(slerp2.getTimeInterval().equals(newIntervalOfValidity));

        // Test case n°3 : when the new interval isn't included, the method copy should throw an exception
        try {
            slerp3.copy(newIntervalOfValidityNotIncluded);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Build the two attitude laws that have to be joined by a slew.
     * 
     * @throws PatriusException
     *         orekit exception
     */
    @Before
    public final void setUp() throws PatriusException {
        fr.cnes.sirius.patrius.Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true)); /*
                                                                                    * YAW STEERING WITH OREKIT
                                                                                    */
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07), TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final double mu = Constants.EGM96_EARTH_MU;

        // Satellite position
        this.circOrbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(270.),
            MathLib.toRadians(5.300), PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
            FramesFactory.getITRF());

        // Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        final AttitudeLaw nadirLaw = new NadirPointing(earthShape);

        final GeodeticPoint point = new GeodeticPoint(60, 120, 0);

        final AttitudeLaw fixedRateLaw = new TargetPointing(earthShape.getBodyFrame(), earthShape.transform(point));

        this.previousLaw = new AttitudeLawLeg(nadirLaw, date, date.shiftedBy(3600));
        this.nextLaw = new AttitudeLawLeg(fixedRateLaw, date.shiftedBy(3600), date.shiftedBy(2 * 3600));
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
    public Vector3DFunction getSpinFunction(final Slew slew, final PVCoordinatesProvider pvProv, final Frame frame,
                                            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return slew.getAttitude(date, frame).getSpin();
            }
        };
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * Compare two attitudes.
     * 
     * @param att1
     *        : first attitude
     * @param att2
     *        : second attitude
     * @return true if the attitudes are the same, false otherwise
     * @throws PatriusException
     *         orekit exception
     */
    private boolean compareAttitude(final Attitude att1, final Attitude att2) throws PatriusException {
        if (MathLib.abs(att1.getDate().durationFrom(att2.getDate())) > Precision.DOUBLE_COMPARISON_EPSILON) {
            return false;
        }
        final Rotation rot1 = att1.getRotation();
        final Rotation rot2 = att2.getRotation();
        if (att1.getReferenceFrame() != att2.getReferenceFrame()) {
            final Transform t = att1.getReferenceFrame().getTransformTo(att2.getReferenceFrame(), att1.getDate());
            if (Rotation.distance(rot1, t.getRotation().applyTo(rot2)) > Precision.DOUBLE_COMPARISON_EPSILON) {
                return false;
            }
        } else {
            if (Rotation.distance(rot1, rot2) > Precision.DOUBLE_COMPARISON_EPSILON) {
                return false;
            }
        }
        return true;
    }
}
