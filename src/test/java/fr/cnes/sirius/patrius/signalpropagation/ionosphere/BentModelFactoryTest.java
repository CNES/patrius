/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link BentModelFactory} class.
 *
 * @author bonitt
 */
public class BentModelFactoryTest {

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Builds a new instance of the factory and use it.
     *
     * @testedMethod {@link AbstractIonosphericCorrectionFactory#AbstractIonosphericCorrectionFactory()}
     * @testedMethod {@link AbstractIonosphericCorrectionFactory#getIonoCorrection(TopocentricFrame)}
     * @testedMethod {@link BentModelFactory#BentModelFactory(R12Provider, SolarActivityDataProvider, USKProvider)}
     * @testedMethod {@link BentModelFactory#buildIonoCorrection(TopocentricFrame)}
     *
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testBentModelFactory() throws PatriusException {

        // Environment initialization
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(84., 15.);
        final R12Loader r12Prov = new R12Loader("CCIR12");
        final USKLoader uskProv = new USKLoader("NEWUSK");

        final double frequency = 2.4e9;

        final Frame itrf = FramesFactory.getITRF();
        final BodyShape earth = new OneAxisEllipsoid(Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
            Constants.GRS80_EARTH_FLATTENING, itrf, "Earth");
        final GeodeticPoint geodeticPoint = new GeodeticPoint(MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 521.18);
        final TopocentricFrame topoFrame = new TopocentricFrame(earth, geodeticPoint, "topo");

        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = new AbsoluteDate(2011, 01, 01, TimeScalesFactory.getUTC());
        final PVCoordinatesProvider orbit =
            new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, frame, date, Constants.GRS80_EARTH_MU);
        final Vector3D spaceObjectPos = orbit.getPVCoordinates(date, frame).getPosition();

        // Ionospheric factory and model initialization
        final AbstractIonosphericCorrectionFactory ionoFactory = new BentModelFactory(r12Prov, solarActivity, uskProv);

        // Initialize reference ionospheric correction model and compute the reference signal delay
        final IonosphericCorrection bentRefCorr = new BentModel(r12Prov, solarActivity, uskProv, topoFrame);
        final double expectedIonoDelay = bentRefCorr.computeSignalDelay(frequency, date, spaceObjectPos, frame);

        // Call the method AbstractIonosphericCorrectionFactory#getModel(TopocentricFrame) to initialize the ionospheric correction
        final IonosphericCorrection ionoCorrection1 = ionoFactory.getIonoCorrection(topoFrame);
        Assert.assertNotNull(ionoCorrection1);

        // Call the method BentModelFactory#buildIonoCorrection(TopocentricFrame) to initialize the ionospheric correction
        final IonosphericCorrection ionoCorrection2 = ionoFactory.buildIonoCorrection(topoFrame);
        Assert.assertNotNull(ionoCorrection2);

        // Compute and evaluate the signal delays
        Assert.assertEquals(expectedIonoDelay, ionoCorrection1.computeSignalDelay(frequency, date, spaceObjectPos, frame), 0.);
        Assert.assertEquals(expectedIonoDelay, ionoCorrection2.computeSignalDelay(frequency, date, spaceObjectPos, frame), 0.);
    }

    /** Setup. */
    @BeforeClass
    public static void setUp() {
        Utils.setDataRoot("bent");
    }
}
