/**
 * HISTORY
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.11:DM:DM-3217:22/05/2023:[PATRIUS] Modeles broadcast et almanach GNSS
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.6:FA:FA-2608:27/01/2021:Mauvaise date de reference pour le Galileo System Time
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
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
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TAIScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class validates CNAVGNSSParameters and GNSSPVCoordinates
 * </p>
 * 
 * @author fteilhard
 *
 */
public class CNAVGNSSParametersTest {

    /** Double used for comparing positions in the tests */
    private static final double EPSILON_POS = 1e-7;
    /** Double used for comparing velocities in the tests */
    private static final double EPSILON_VEL = 1e-10;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(CNAVGNSSParametersTest.class.getSimpleName(), "CNAV GNSS Parameters");
    }

    /**
     * GPS CNAV Broadcast model test
     *
     * @testedMethod {@link GNSSPVCoordinates#GNSSPVCoordinates(GNSSParameters, AbsoluteDate)}
     * @testedMethod {@link CNAVGNSSParameters#CNAVGNSSParameters}
     * @testedMethod {@link GNSSPVCoordinates#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link GNSSPVCoordinates#getClockCorrection(double)}
     * @testedMethod {@link GNSSPVCoordinates#getRelativisticCorrection(double)}
     *
     * @description Computes a pv coordinates ephemeris from a reference CNAV broadcast model and
     *              compares to the reference ephemeris provided at three instants.
     *              The clock correction is also computed at the three instants and compared to its
     *              reference value
     * @testPassCriteria Relative difference on position and velocity lower than
     *                   PV_VALUES_COMPARISON_EPSILON
     *                   Relative difference on clock correction lower than
     *                   Precision.DOUBLE_COMPARISON_EPSILON
     * @throws PatriusException PatriusException
     */
    @Test
    public void gpsG03CNAVTest() throws PatriusException {
        // Values for the GNSSParameters constructor
        final double ecc = 4.146514693275e-03;
        final double incl = 9.738272039033e-01;
        final double omegaRate = -7.650976500218e-09;
        final double sqrtA = 5.153626670168e+03;
        final double omega0 = -2.504157239492e+00;
        final double w0 = 9.531470911108e-01;
        final double m0 = -1.334489692635e+00;
        final double af0 = -2.469198661856e-04;
        final double af1 = -1.348254841105e-11;
        final double af2 = 1e-18;
        final double iRate = 6.934217409417e-10;
        final double deltaN = 4.219818629527e-09;
        final double cuc = -7.776543498039e-07;
        final double cus = 1.083035022020e-05;
        final double crc = 1.813984375000e+02;
        final double crs = -1.405468750000e+01;
        final double cic = 6.984919309616e-08;
        final double cis = 6.519258022308e-09;
        final double aRate = 1.026010513306e-02;
        final double deltaNRate = -1.112848042646e-13;

        final double tref = 1.854000000000E+05;

        final TAIScale tai = TimeScalesFactory.getTAI();
        final Frame frame = FramesFactory.getITRF();

        final AbsoluteDate weekDate = new AbsoluteDate("2022-05-15T00:00:19", tai);

        final CNAVGNSSParameters g03Parameters = new CNAVGNSSParameters(GNSSType.GPS, tref, m0, ecc, sqrtA, omega0,
            incl, w0, omegaRate, af0, af1, af2, deltaN, iRate, cuc, cus, crc, crs, cic, cis, aRate, deltaNRate);

        final GNSSPVCoordinates g03Coordinates = new GNSSPVCoordinates(g03Parameters, weekDate);

        // First check
        AbsoluteDate date = new AbsoluteDate("2022-05-17T00:00:00.000", tai);
        PVCoordinates pvCoordActual = g03Coordinates.getPVCoordinates(date, frame);
        PVCoordinates pvCoordExp = new PVCoordinates(6516743.9825727055, 1.899755383985609E7, -1.7547088744022924E7,
            -1886.5512076461605, -1138.14567362453, -1933.617481286891);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        double deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, EPSILON_POS);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        double deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, EPSILON_VEL);
        // Clock test
        Assert.assertEquals(-2.4674988772946957E-4, g03Coordinates.getClockCorrection(date),
            Precision.DOUBLE_COMPARISON_EPSILON);

        // Second check
        date = new AbsoluteDate("2022-05-17T04:15:00.000", tai);
        pvCoordActual = g03Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(-2.312880267177838E7, 1.2925860772136832E7, 65266.500709010936,
            -104.04618990454141, -229.16773670989124, 3211.834265280305);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, EPSILON_POS);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, EPSILON_VEL);
        // Clock test
        Assert.assertEquals(-2.4694830077884186E-4, g03Coordinates.getClockCorrection(date),
            Precision.DOUBLE_COMPARISON_EPSILON);

        // Third check
        date = new AbsoluteDate("2022-05-17T15:00:00.000", tai);
        pvCoordActual = g03Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(1.926507597544558E7, -1.2793097516681658E7, -1.3066563578416979E7,
            1651.2813448781426, -118.75289370761395, 2583.411281043441);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, EPSILON_POS);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, EPSILON_VEL);
        // Clock test
        Assert.assertEquals(-2.474665820576605E-4, g03Coordinates.getClockCorrection(date),
            Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Covers expcetion branch in constructor.
     */
    @Test
    public void testConstructorWithGalileo() {

        // Values for the GNSSParameters constructor
        final double ecc = 4.146514693275e-03;
        final double incl = 9.738272039033e-01;
        final double omegaRate = -7.650976500218e-09;
        final double sqrtA = 5.153626670168e+03;
        final double omega0 = -2.504157239492e+00;
        final double w0 = 9.531470911108e-01;
        final double m0 = -1.334489692635e+00;
        final double af0 = -2.469198661856e-04;
        final double af1 = -1.348254841105e-11;
        final double af2 = 1e-18;
        final double iRate = 6.934217409417e-10;
        final double deltaN = 4.219818629527e-09;
        final double cuc = -7.776543498039e-07;
        final double cus = 1.083035022020e-05;
        final double crc = 1.813984375000e+02;
        final double crs = -1.405468750000e+01;
        final double cic = 6.984919309616e-08;
        final double cis = 6.519258022308e-09;
        final double aRate = 1.026010513306e-02;
        final double deltaNRate = -1.112848042646e-13;

        final double tref = 1.854000000000E+05;

        final GNSSType gnssTypeGalileo = GNSSType.Galileo;

        try {
            new CNAVGNSSParameters(gnssTypeGalileo, tref, m0, ecc, sqrtA, omega0,
                incl, w0, omegaRate, af0, af1, af2, deltaN, iRate, cuc, cus, crc, crs, cic, cis, aRate, deltaNRate);
            Assert.fail();
        } catch (final IllegalArgumentException iae) {
            Assert.assertEquals(iae.getMessage(), PatriusMessages.CNAV_FOR_GALILEO_ERROR.getSourceString());
        }
    }
}
