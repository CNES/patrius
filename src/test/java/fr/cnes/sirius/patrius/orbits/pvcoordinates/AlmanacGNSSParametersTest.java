/**
 * HISTORY
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
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

/**
 * <p>
 * This class validates AlmanacGNSSParameters and GNSSPVCoordinates
 * </p>
 * @author fteilhard
 *
 */
public class AlmanacGNSSParametersTest {

    /** Double used for comparing positions in the tests */
    private static final double POSITION_COMPARISON_EPSILON = 1e-7;
    /** Double used for comparing velocities in the tests */
    private static final double VELOCITY_COMPARISON_EPSILON = 1e-10;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AlmanacGNSSParametersTest.class.getSimpleName(), "Almanac GNSS Parameters");
    }

    /**
     * GPS Almanac test
     *
     * @testedMethod {@link GNSSPVCoordinates#GNSSPVCoordinates(GNSSParameters, AbsoluteDate)}
     * @testedMethod {@link AlmanacGNSSParameters#AlmanacGNSSParameters(GNSSType,double,double,double,double,double,double,double,double,double,double)}
     * @testedMethod {@link GNSSPVCoordinates#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link GNSSPVCoordinates#getClockCorrection(double)}
     * @testedMethod {@link GNSSPVCoordinates#getRelativisticCorrection(double)}
     *
     * @description Computes a pv coordinates ephemeris from a reference almanac and compares
     *              to the reference ephemeris provided at three instants.
     *              The clock correction is also computed at the three instants and compared to its
     *              reference value
     * @testPassCriteria Relative difference on position and velocity lower than
     *                   PV_VALUES_COMPARISON_EPSILON
     *                   Relative difference on clock correction lower than
     *                   Precision.DOUBLE_COMPARISON_EPSILON
     * @throws PatriusException PatriusException
     */
    @Test
    public void gpsG10AlmanacTest() throws PatriusException {

        // Values for the GNSSParameters constructor
        final double ecc = 7.609693799168E-03;
        final double incl = 9.736377165133E-01;
        final double omegaRate = -7.551385974108E-09;
        final double sqrtA = 5.153682521820E+03;
        final double omega0 = -2.506928304038E+00;
        final double w0 = -2.494141557384E+00;
        final double m0 = -1.993894650150E+00;
        final double af0 = -4.174583591521E-04;
        final double af1 = -1.455191522837E-11;
        final double tref = 1.872000000000E+05;

        final TAIScale tai = TimeScalesFactory.getTAI();
        final Frame frame = FramesFactory.getITRF();

        final AbsoluteDate weekDate = new AbsoluteDate("2022-05-15T00:00:19", tai);

        final AlmanacGNSSParameters g10Parameters = new AlmanacGNSSParameters(GNSSType.GPS, tref, m0, ecc, sqrtA,
                omega0, incl, w0, omegaRate, af0, af1);

        final GNSSPVCoordinates g10Coordinates = new GNSSPVCoordinates(g10Parameters, weekDate);

        // First check
        AbsoluteDate date = new AbsoluteDate("2022-05-17T00:00:00.000", tai);
        PVCoordinates pvCoordActual = g10Coordinates.getPVCoordinates(date, frame);
        PVCoordinates pvCoordExp = new PVCoordinates(-2.351184078800576E7, -1.0845642810380615E7, -6425645.812269772,
                -563.5922234622003, -641.0917306605972, 3045.238936931335);

        // Difference in norm between expected and actual position lower than EPSILON_POS
        double deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        double deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);

        // Clock test
        Assert.assertEquals(-4.1726269947155035E-4, g10Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);
        System.out.println("deltaPos= " + deltaPos);
        System.out.println("deltaVel= " + deltaVel);
        // Second check
        date = new AbsoluteDate("2022-05-17T10:45:00.000", tai);
        pvCoordActual = g10Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(1.912908434208154E7, 5368532.274475862, -1.7622328321805593E7,
                1258.5834793879562, 1872.7140452985661, 1892.280331034862);

        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(-4.17829115858952E-4, g10Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);
        // Third check
        date = new AbsoluteDate("2022-05-17T15:00:00.000", tai);
        pvCoordActual = g10Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(1.0400995119933104E7, 1.260319283762102E7, 2.1145185132874243E7,
                -2580.252861980988, 563.5732311867024, 911.3739585279621);

        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(-4.1802403082435915E-4, g10Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Galileo Almanac test
     *
     * @description Computes a pv coordinates ephemeris from a reference almanac and compares
     *              to the reference ephemeris provided at three instants.
     *              The clock correction is also computed at the three instants and compared to its
     *              reference value
     * @testPassCriteria Relative difference on position and velocity lower than
     *                   PV_VALUES_COMPARISON_EPSILON
     *                   Relative difference on clock correction lower than
     *                   Precision.DOUBLE_COMPARISON_EPSILON
     * @throws PatriusException
     */
    @Test
    public void galileoE10AlmanacTest() throws PatriusException {
        // Values for the GNSSParameters constructor
        final double ecc = 3.514460986480E-04;
        final double incl = 9.977804949569E-01;
        final double omegaRate = -5.433440610325E-09;
        final double sqrtA = 5.440618213654E+03;
        final double omega0 = 2.606139718694E+00;
        final double w0 = 1.473835374611E+00;
        final double m0 = -1.234212597017E+00;
        final double af0 = -5.134998355061E-04;
        final double af1 = -2.515321284591E-12;
        final double tref = 1.854000000000E+05;

        final TAIScale tai = TimeScalesFactory.getTAI();
        final Frame frame = FramesFactory.getITRF();

        final AbsoluteDate weekDate = new AbsoluteDate("2022-05-15T00:00:19", tai);

        final AlmanacGNSSParameters e10Parameters = new AlmanacGNSSParameters(GNSSType.Galileo, tref, m0, ecc, sqrtA,
                omega0, incl, w0, omegaRate, af0, af1);

        final GNSSPVCoordinates e10Coordinates = new GNSSPVCoordinates(e10Parameters, weekDate);

        // First check
        AbsoluteDate date = new AbsoluteDate("2022-05-17T00:00:00.000", tai);
        PVCoordinates pvCoordActual = e10Coordinates.getPVCoordinates(date, frame);
        PVCoordinates pvCoordExp = new PVCoordinates(2310139.918235926, 1.70009184450089E7, -2.4132788246710293E7,
                -2019.3830874592404, 1338.7627503244819, 750.3472963093453);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        double deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        double deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);

        // Clock test
        Assert.assertEquals(-5.134678090816205E-4, e10Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Second check
        date = new AbsoluteDate("2022-05-17T07:45:00.000", tai);
        pvCoordActual = e10Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(-2.0528716153211504E7, 3533159.41676086, 2.1018602038800325E7,
                -1861.1450925407078, -1006.9771698931717, -1647.3831189552368);

        // / Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(-5.135387932574758E-4, e10Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Third check
        date = new AbsoluteDate("2022-05-17T15:00:00.000", tai);
        pvCoordActual = e10Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(-8746533.339017257, -2.0296775676799204E7, -1.9701963263313662E7,
                303.09708679129983, -1957.0799613469997, 1882.934093791405);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(-5.136033404526708E-4, e10Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * BeiDou Almanac test
     *
     * @description Computes a pv coordinates ephemeris from a reference almanac and compares
     *              to the reference ephemeris provided at three instants.
     *              The clock correction is also computed at the three instants and compared to its
     *              reference value
     * @testPassCriteria Relative difference on position and velocity lower than
     *                   PV_VALUES_COMPARISON_EPSILON
     *                   Relative difference on clock correction lower than
     *                   Precision.DOUBLE_COMPARISON_EPSILON
     * @throws PatriusException
     */
    @Test
    public void beidouC11AlmanacTest() throws PatriusException {

        // Values for the GNSSParameters constructor
        final double ecc = 1.790514914319E-03;
        final double incl = 9.894237524422E-01;
        final double omegaRate = -6.984933807673E-09;
        final double sqrtA = 5.282605478287E+03;
        final double omega0 = 2.127955159474E+00;
        final double w0 = -1.876329996310E+00;
        final double m0 = 2.406065455290E+00;
        final double af0 = 3.567560343072E-04;
        final double af1 = 2.323119474568E-11;
        final double tref = 1.764000000000E+05;

        final TAIScale tai = TimeScalesFactory.getTAI();
        final Frame frame = FramesFactory.getITRF();

        final AbsoluteDate weekDate = new AbsoluteDate("2022-05-15T00:00:33", tai);

        final AlmanacGNSSParameters c11Parameters = new AlmanacGNSSParameters(GNSSType.BeiDou, tref, m0, ecc, sqrtA,
                omega0, incl, w0, omegaRate, af0, af1);

        final GNSSPVCoordinates c11Coordinates = new GNSSPVCoordinates(c11Parameters, weekDate);

        // First check
        AbsoluteDate date = new AbsoluteDate("2022-05-17T00:00:00.000", tai);
        PVCoordinates pvCoordActual = c11Coordinates.getPVCoordinates(date, frame);
        PVCoordinates pvCoordExp = new PVCoordinates(-1.4532824869559357E7, 2.3823566593199845E7, 958280.3840251229,
                18.655898739179513, -108.03068872223255, 3154.1050824027047);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        double deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        double deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(3.566676806219842E-4, c11Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Second check
        date = new AbsoluteDate("2022-05-17T08:45:00.000", tai);
        pvCoordActual = c11Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(-1.3658290685523989E7, -1.1539233270600095E7, -2.1360388819973294E7,
                2397.398643252647, -508.1142320383723, -1257.5457533618608);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(3.574038508911605E-4, c11Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Third check
        date = new AbsoluteDate("2022-05-17T15:00:00.000", tai);
        pvCoordActual = c11Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(9357630.185358055, -1.6570419035416324E7, 2.0477634788512517E7,
                639.1374210136728, 2235.4142761353546, 1518.6276783851977);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(3.579252973479795E-4, c11Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);
    }
}
