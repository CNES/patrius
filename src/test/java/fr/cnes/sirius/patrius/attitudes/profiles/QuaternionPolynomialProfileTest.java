/**
 * HISTORY
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre la classe QuaternionPolynomialSegment plus generique et ajouter de la coherence dans le package polynomials
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3156:10/05/2022:[Patrius] Erreur dans le calcul de l'attitude par QuaternionPolynomialProfile ...
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLawLeg;
import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.math.analysis.polynomials.DatePolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialsUtils;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/*
 *
 * Test class useful for the validation of the {@link #QuaternionPolynomialProfile} class and of the FT-3156.
 */
public class QuaternionPolynomialProfileTest {
    
    /** Tolerance value to be used for the validation of the tests of this class. */
    static final double TOLERANCE = 1.0E-15;

    /**
     * Test useful for the validation of the {@link #QuaternionPolynomialProfile} class and of the FT-3156.
     * 
     * @throws PatriusException
     *         PatriusException
     * @throws IOException
     *         IOException
     */
    @Test
    public void testQuaternionPolynomialProfile() throws PatriusException, IOException {

        // Set data root
        Utils.setDataRoot("regular-dataPBASE");

        // Orbit parameters and frames
        final AbsoluteDate start = new AbsoluteDate("2002-02-02T00:00:00.000");
        final double a = 7027053.935062064;
        final double e = 0.00113059932064981;
        final double i = 1.7104060815420514;
        final double pa = 1.5707963267948966;
        final double raan = 5.290087275658208;
        final double anomaly = 4.71238898038469;

        final KeplerianOrbit orbit =
            new KeplerianOrbit(a, e, i, pa, raan, anomaly, PositionAngle.MEAN, FramesFactory.getCIRF(), start,
                Constants.GRIM5C1_EARTH_MU);
        final KeplerianPropagator pvProv = new KeplerianPropagator(orbit);
        final LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(), LOFType.LVLH, pvProv, "LOF");
        final Frame gcrf = FramesFactory.getGCRF();

        // Time interval
        final double duration = 120.;
        final AbsoluteDateInterval timeInterval = new AbsoluteDateInterval(start, duration);
        final double deltaTime = 1.0;

        // Ideal pointing
        final NadirPointing idealPointing = new NadirPointing(new OneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS, Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getITRF(),
            "Terre"));
        final StrictAttitudeLegsSequence<AttitudeLeg> idealSequence = new StrictAttitudeLegsSequence<>();
        idealSequence.add(new AttitudeLawLeg(idealPointing, timeInterval));

        // Patrius polynomial profile
        final int order = 4;
        final QuaternionLagrangePolynomialProfileComputer polynomialComputer = new QuaternionLagrangePolynomialProfileComputer(
            lof, order);
        final QuaternionPolynomialProfile polynomials = polynomialComputer.compute(idealSequence, pvProv);

        // Polynomials

        AbsoluteDate current = timeInterval.getLowerData();
        while (current.durationFrom(timeInterval.getUpperData()) < 0) {

            // Comparison in the computation frame (LOF)
            final Attitude polyAttLof = polynomials.getAttitude(pvProv, current, lof);
            final Attitude idealAttLof = idealSequence.getAttitude(pvProv, current, lof);
            final double angleLof = polyAttLof.getRotation().applyInverseTo(idealAttLof.getRotation()).getAngle();

            // Comparison in another frame (GCRF)
            final Attitude polyAttGcrf = polynomials.getAttitude(pvProv, current, gcrf);
            final Attitude idealAttGcrf = idealSequence.getAttitude(pvProv, current, gcrf);
            final double angleGcrf = polyAttGcrf.getRotation().applyInverseTo(idealAttGcrf.getRotation()).getAngle();

            final Rotation lofToGcrf = lof.getTransformTo(gcrf, current).getRotation();

            final Rotation computedIdealRotGcrf = lofToGcrf.applyInverseTo(idealAttLof.getRotation());
            final double angleIdeal = computedIdealRotGcrf.applyInverseTo(idealAttGcrf.getRotation()).getAngle();

            final Rotation computedPolyRotGcrf = lofToGcrf.applyInverseTo(polyAttLof.getRotation());
            final double anglePoly = computedPolyRotGcrf.applyInverseTo(polyAttGcrf.getRotation()).getAngle();

            Assert.assertEquals(angleLof, angleGcrf, TOLERANCE);
            Assert.assertEquals(angleIdeal, anglePoly, TOLERANCE);

            current = current.shiftedBy(deltaTime);
        }
    }

    /**
     * Test used for the validation of the {@link #QuaternionDatePolynomialProfile} constructors.
     */
    @Test
    public void testConstructors() {
        // Set data root
        Utils.setDataRoot("regular-dataPBASE");
        // Initialization
        final double dt = 50;
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
        final double[] y0 = { 1, 2, 3 };
        final DatePolynomialFunction q0pf = new DatePolynomialFunction(date0, new PolynomialFunction(y0));
        final double[] y1 = { 2, 3, 4 };
        final DatePolynomialFunction q1pf = new DatePolynomialFunction(date0, new PolynomialFunction(y1));
        final double[] y2 = { 3, 4, 5 };
        final DatePolynomialFunction q2pf = new DatePolynomialFunction(date0, new PolynomialFunction(y2));
        final double[] y3 = { 4, 5, 6 };
        final DatePolynomialFunction q3pf = new DatePolynomialFunction(date0, new PolynomialFunction(y3));
        // Create the segment
        final AbsoluteDateInterval expectedInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0,
            date1, IntervalEndpointType.CLOSED);
        final QuaternionDatePolynomialSegment segment = new QuaternionDatePolynomialSegment(q0pf, q1pf, q2pf, q3pf,
            expectedInterval, false);
        final List<QuaternionDatePolynomialSegment> segments = new ArrayList<>();
        segments.add(segment);
        // Create the time interval
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(date0, date1);
        // Define the reference frame
        final Frame frame = FramesFactory.getGCRF();
        // Define the custom nature
        final String customNature = "custom nature";
        // Define the default nature
        final String defaultNature = "QUATERNION_POLYNOMIAL_PROFILE";
        // Define the custom spin delta-t
        final double customSpinDeltaT = 0.1;
        // Define the default spin delta-t
        final double defaultSpinDeltaT = 0.2;

        // Create the profile with the 1st constructor
        final QuaternionDatePolynomialProfile profile1 = new QuaternionDatePolynomialProfile(frame, interval,
            segments, customNature);
        // Validate the profile built with the 1st constructor
        Assert.assertEquals(date0, profile1.getDate());
        Assert.assertEquals(date1, profile1.getEnd());
        Assert.assertEquals(customNature, profile1.getNature());
        Assert.assertEquals(interval, profile1.getTimeInterval());
        Assert.assertEquals(defaultSpinDeltaT, profile1.getSpinDeltaT());
        Assert.assertEquals(segments.size(), profile1.size());
        Assert.assertEquals(segment, profile1.getSegment(0));
        Assert.assertEquals(frame, profile1.getReferenceFrame());

        // Test the getSegment(int) method exception cases
        try {
            profile1.getSegment(-1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected
            Assert.assertTrue(true);
        }
        try {
            profile1.getSegment(1); // Only one segment
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Cover the segment's getters
        Assert.assertEquals(q0pf, segment.getQ0Polynomial());
        Assert.assertEquals(q1pf, segment.getQ1Polynomial());
        Assert.assertEquals(q2pf, segment.getQ2Polynomial());
        Assert.assertEquals(q3pf, segment.getQ3Polynomial());

        // Create the profile with the 2nd constructor
        final QuaternionDatePolynomialProfile profile2 = new QuaternionDatePolynomialProfile(frame, interval,
            segments);
        // Validate the profile built with the 2nd constructor
        Assert.assertEquals(date0, profile2.getDate());
        Assert.assertEquals(date1, profile2.getEnd());
        Assert.assertEquals(defaultNature, profile2.getNature());
        Assert.assertEquals(interval, profile2.getTimeInterval());
        Assert.assertEquals(defaultSpinDeltaT, profile2.getSpinDeltaT());

        // Create the profile with the 3rd constructor
        final QuaternionDatePolynomialProfile profile3 = new QuaternionDatePolynomialProfile(frame, interval,
            segments, customNature, customSpinDeltaT);
        // Validate the profile built with the 3rd constructor
        Assert.assertEquals(date0, profile3.getDate());
        Assert.assertEquals(date1, profile3.getEnd());
        Assert.assertEquals(customNature, profile3.getNature());
        Assert.assertEquals(interval, profile3.getTimeInterval());
        Assert.assertEquals(customSpinDeltaT, profile3.getSpinDeltaT());

        // Create the profile with the 4th constructor
        final QuaternionDatePolynomialProfile profile4 = new QuaternionDatePolynomialProfile(frame, interval,
            segments, customSpinDeltaT);
        // Validate the profile built with the 4th constructor
        Assert.assertEquals(date0, profile4.getDate());
        Assert.assertEquals(date1, profile4.getEnd());
        Assert.assertEquals(defaultNature, profile4.getNature());
        Assert.assertEquals(interval, profile4.getTimeInterval());
        Assert.assertEquals(customSpinDeltaT, profile4.getSpinDeltaT());
    }

    /**
     * Test used for the validation of the copy() method of the {@link #QuaternionDatePolynomialProfile} class.
     */
    @Test
    public void testCopy() throws PatriusException {
        // Set data root
        Utils.setDataRoot("regular-dataPBASE");
        // Initialization
        final double dt = 50;
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
        final double[] y0 = { 1, 2, 3 };
        final DatePolynomialFunction q0pf = new DatePolynomialFunction(date0, new PolynomialFunction(y0));
        final double[] y1 = { 2, 3, 4 };
        final DatePolynomialFunction q1pf = new DatePolynomialFunction(date0, new PolynomialFunction(y1));
        final double[] y2 = { 3, 4, 5 };
        final DatePolynomialFunction q2pf = new DatePolynomialFunction(date0, new PolynomialFunction(y2));
        final double[] y3 = { 4, 5, 6 };
        final DatePolynomialFunction q3pf = new DatePolynomialFunction(date0, new PolynomialFunction(y3));
        // Create the segment
        final AbsoluteDateInterval expectedInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0,
            date1, IntervalEndpointType.CLOSED);
        final QuaternionDatePolynomialSegment segment = new QuaternionDatePolynomialSegment(q0pf, q1pf, q2pf, q3pf,
            expectedInterval, false);
        final List<QuaternionDatePolynomialSegment> segments = new ArrayList<>();
        segments.add(segment);
        // Create the time interval
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(date0, date1);
        // Define the reference frame
        final Frame frame = FramesFactory.getGCRF();
        // Define the custom nature
        final String customNature = "custom nature";
        // Define the custom spin delta-t
        final double customSpinDeltaT = 0.1;

        // Create the profile with the 1st constructor
        final QuaternionDatePolynomialProfile profile1 = new QuaternionDatePolynomialProfile(frame, interval,
            segments, customNature, customSpinDeltaT);
        // Create a new time interval
        final AbsoluteDate date2 = date1.shiftedBy(-10);
        final AbsoluteDateInterval newInterval = new AbsoluteDateInterval(date0, date2);
        // Copy the profile
        final QuaternionDatePolynomialProfile profile2 = profile1.copy(newInterval);
        // Validate the profile built with the 1st constructor
        Assert.assertEquals(date0, profile1.getDate());
        Assert.assertEquals(date1, profile1.getEnd());
        Assert.assertEquals(customNature, profile1.getNature());
        Assert.assertEquals(customSpinDeltaT, profile1.getSpinDeltaT());
        Assert.assertEquals(interval, profile1.getTimeInterval());
        // Validate the profile built with the 1st constructor
        Assert.assertEquals(date0, profile2.getDate());
        Assert.assertEquals(date2, profile2.getEnd());
        Assert.assertEquals(customNature, profile2.getNature());
        Assert.assertEquals(customSpinDeltaT, profile2.getSpinDeltaT());
        Assert.assertEquals(newInterval, profile2.getTimeInterval());

        // FA-116: test in case of date out of bounds of new interval
        final AbsoluteDateInterval interval116 = interval;
        final QuaternionDatePolynomialProfile profile116 = new QuaternionDatePolynomialProfile(FramesFactory.getGCRF(),
                interval116, segments, "My nature");
        final AbsoluteDateInterval interval112Reduced = new AbsoluteDateInterval(date0.shiftedBy(-10), date2.shiftedBy(-10));
        try {
            // New interval not included in former interval
            final QuaternionDatePolynomialProfile profile116Copy = profile116.copy(interval112Reduced);
            profile116Copy.getAttitude(null, date0.shiftedBy(-5.), FramesFactory.getGCRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            // Date included in segments but not in new interval
            final AbsoluteDateInterval interval116Reduced2 = new AbsoluteDateInterval(date0.shiftedBy(10),
                    date2.shiftedBy(-10));
            final QuaternionDatePolynomialProfile profile116Copy2 = profile116.copy(interval116Reduced2);
            profile116Copy2.getAttitude(null, date0.shiftedBy(-5.), FramesFactory.getGCRF());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Test used for the validation of the getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame) and the
     * setSpinDerivativesComputation() methods of the {@link #QuaternionDatePolynomialProfile} class.
     * 
     * @throws PatriusException if the attitude cannot be retrieved
     */
    @Test
    public void testGetAttitude() throws PatriusException {
        // Set data root
        Utils.setDataRoot("regular-dataPBASE");
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        // Define the time interval
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date0, date1,
            IntervalEndpointType.CLOSED);
        // Create the first segment
        final DatePolynomialFunction q0pf = new DatePolynomialFunction(date0,
            PolynomialsUtils.createLegendrePolynomial(4));
        final DatePolynomialFunction q1pf = new DatePolynomialFunction(date0,
            PolynomialsUtils.createLegendrePolynomial(4));
        final DatePolynomialFunction q2pf = new DatePolynomialFunction(date0,
            PolynomialsUtils.createLegendrePolynomial(4));
        final DatePolynomialFunction q3pf = new DatePolynomialFunction(date0,
            PolynomialsUtils.createLegendrePolynomial(4));
        final QuaternionDatePolynomialSegment segment = new QuaternionDatePolynomialSegment(q0pf, q1pf, q2pf,
            q3pf, interval, false);
        // Create the segment
        final List<QuaternionDatePolynomialSegment> segments = new ArrayList<>();
        segments.add(segment);
        // Define the reference frame
        final Frame frame = FramesFactory.getGCRF();
        // Create the profile with the 1st constructor
        final QuaternionDatePolynomialProfile profile = new QuaternionDatePolynomialProfile(frame, interval,
            segments);

        // Retrieve the attitude without the computation of the spin derivatives
        final AbsoluteDate date2 = date0.shiftedBy(0.1);
        Attitude actualAttitude = profile.getAttitude(null, date2, frame);
        // Check the attitude without the computation of the spin derivatives
        Rotation actualRotation = actualAttitude.getRotation();
        final Rotation expectedRotation = segment.getOrientation(date2);
        Assert.assertEquals(expectedRotation.getAngle(), actualRotation.getAngle(), 0.0);
        // Check that the rotation acceleration is null in case of no computation of the spin derivatives
        Vector3D actualRotationAcceleration = actualAttitude.getRotationAcceleration();
        Assert.assertNull(actualRotationAcceleration);

        // Retrieve the attitude with the computation of the spin derivatives
        profile.setSpinDerivativesComputation(true);
        actualAttitude = profile.getAttitude(null, date2, frame);
        // Check the attitude with the computation of the spin derivatives
        actualRotation = actualAttitude.getRotation();
        Assert.assertEquals(expectedRotation.getAngle(), actualRotation.getAngle(), 0.0);
        // Check that the rotation acceleration is not null in case of computation of the spin derivatives
        actualRotationAcceleration = actualAttitude.getRotationAcceleration();
        Assert.assertNotNull(actualRotationAcceleration);

        // Cover an exception cases in the private method getSegment(AbsoluteDate)
        final AbsoluteDateInterval intervalBis = new AbsoluteDateInterval(date0, date1.shiftedBy(60.));
        final QuaternionDatePolynomialProfile profileBis = new QuaternionDatePolynomialProfile(frame, intervalBis,
            segments);
        try {
            profileBis.getAttitude(null, date1.shiftedBy(40.), frame);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }
}
