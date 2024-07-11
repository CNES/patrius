/**
 * HISTORY
 * VERSION:4.9:FA:FA-3156:10/05/2022:[Patrius] Erreur dans le calcul de l'attitude par QuaternionPolynomialProfile ...
 * END-HISTORY
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.IOException;

import org.junit.Test;

import junit.framework.Assert;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLawLeg;
import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.attitudes.profiles.QuaternionPolynomialProfile;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
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
        final NadirPointing idealPointing = new NadirPointing(new ExtendedOneAxisEllipsoid(
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

}
