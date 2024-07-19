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
 * This class validates LNAVGNSSParameters and GNSSPVCoordinates
 * </p>
 * @author fteilhard
 *
 */
public class LNAVGNSSParametersTest {


    /** Double used for comparing positions in the tests */
    private static final double POSITION_COMPARISON_EPSILON = 1e-7;
    /** Double used for comparing velocities in the tests */
    private static final double VELOCITY_COMPARISON_EPSILON = 1e-10;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(LNAVGNSSParametersTest.class.getSimpleName(), "LNAV GNSS Parameters");
    }

    /**
     * GPS LNAV Broadcast model test
     *
     * @testedMethod {@link GNSSPVCoordinates#GNSSPVCoordinates(GNSSParameters, AbsoluteDate)}
     * @testedMethod {@link LNAVGNSSParameters#LNAVGNSSParameters}
     * @testedMethod {@link GNSSPVCoordinates#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link GNSSPVCoordinates#getClockCorrection(double)}
     * @testedMethod {@link GNSSPVCoordinates#getRelativisticCorrection(double)}
     *
     * @description Computes a pv coordinates ephemeris from a reference LNAV broadcast model and
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
    public void gpsG01LNAVTest() throws PatriusException {
        // Values for the GNSSParameters constructor
        final double ecc = 1.191046519671E-02;
        final double incl = 9.879538357628E-01;
        final double omegaRate = -7.934973380634E-09;
        final double sqrtA = 5.153670530319E+03;
        final double omega0 = 2.745506195319E+00;
        final double w0 = 9.059767020461E-01;
        final double m0 = 3.607435439023E-02;
        final double af0 = 3.613173030317E-04;
        final double af1 = -8.299139153678E-12;
        final double af2 = 0.000000000000E+00;
        final double iRate = -1.314340461842E-10;
        final double deltaN = 3.747656104920E-09;
        final double cuc = -7.184222340584E-06;
        final double cus = 7.338821887970E-06;
        final double crc = 2.568437500000E+02;
        final double crs = -1.361562500000E+02;
        final double cic = -6.146728992462E-08;
        final double cis = 1.769512891769E-07;

        final double tref = 1.872000000000E+05;

        final TAIScale tai = TimeScalesFactory.getTAI();
        final Frame frame = FramesFactory.getITRF();

        final AbsoluteDate weekDate = new AbsoluteDate("2022-05-15T00:00:19", tai);

        final LNAVGNSSParameters g01Parameters = new LNAVGNSSParameters(GNSSType.GPS, tref, m0, ecc, sqrtA, omega0,
                incl, w0, omegaRate, af0, af1, af2, deltaN, iRate, cuc, cus, crc, crs, cic, cis);

        final GNSSPVCoordinates g01Coordinates = new GNSSPVCoordinates(g01Parameters, weekDate);

        // First check
        AbsoluteDate date = new AbsoluteDate("2022-05-17T00:00:00.000", tai);
        PVCoordinates pvCoordActual = g01Coordinates.getPVCoordinates(date, frame);
        PVCoordinates pvCoordExp = new PVCoordinates(-3552655.6751776617, 1.6585766344561148E7,
                -2.063771203227869E7, -2360.286430217653, 985.6278836993575, 1250.2405633033993);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        double deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        double deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(3.6146081324443986E-4, g01Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Second check
        date = new AbsoluteDate("2022-05-17T09:45:00.000", tai);
        pvCoordActual = g01Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(-1.649532120400512E7, -1.3473075708314518E7, -1.6390904963547643E7,
                1998.0290539248672, 204.399489845492, -2185.2250239906957);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(3.611432722259815E-4, g01Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Third check
        date = new AbsoluteDate("2022-05-17T15:00:00.000", tai);
        pvCoordActual = g01Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(1.3679705065369409E7, -2.0568626408802148E7, 8967728.95103841,
                -202.13199232979616, 1190.6406001504283, 2975.9442150825876);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(3.610013935420867E-4, g01Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * BeiDou LNAV Broadcast model test
     *
     * @testedMethod {@link GNSSPVCoordinates#GNSSPVCoordinates(GNSSParameters, AbsoluteDate)}
     * @testedMethod {@link LNAVGNSSParameters#LNAVGNSSParameters}
     * @testedMethod {@link GNSSPVCoordinates#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link GNSSPVCoordinates#getClockCorrection(double)}
     * @testedMethod {@link GNSSPVCoordinates#getRelativisticCorrection(double)}
     *
     * @description Computes a pv coordinates ephemeris from a reference LNAV broadcast model and
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
    public void beidouC02LNAVTest() throws PatriusException {
        // Values for the GNSSParameters constructor
        final double ecc = 1.273794681765E-03;
        final double incl = 1.127453862179E-01;
        final double omegaRate = -8.079265105250E-09;
        final double sqrtA = 6.493443450928E+03;
        final double omega0 = -2.006906744704E+00;
        final double w0 = -1.596288214143E+00;
        final double m0 = 1.335016263415E-01;
        final double af0 = 1.834856811911E-05;
        final double af1 = -3.939248927054E-11;
        final double af2 = 0.000000000000E+00;
        final double iRate = 5.382367054335E-10;
        final double deltaN = 9.017875630717E-09;
        final double cuc = 2.520158886909E-05;
        final double cus = -6.588175892830E-06;
        final double crc = 2.026875000000E+02;
        final double crs = 7.619375000000E+02;
        final double cic = 2.468004822731E-08;
        final double cis = 2.607703208923E-07;

        final double tref = 1.908000000000E+05;

        final TAIScale tai = TimeScalesFactory.getTAI();
        final Frame frame = FramesFactory.getITRF();

        final AbsoluteDate weekDate = new AbsoluteDate("2022-05-15T00:00:33", tai);

        final LNAVGNSSParameters c02Parameters = new LNAVGNSSParameters(GNSSType.BeiDou, tref, m0, ecc, sqrtA, omega0,
                incl, w0, omegaRate, af0, af1, af2, deltaN, iRate, cuc, cus, crc, crs, cic, cis);

        final GNSSPVCoordinates c02Coordinates = new GNSSPVCoordinates(c02Parameters, weekDate);

        // First check
        final AbsoluteDate date = new AbsoluteDate("2022-05-17T00:00:00.000", tai);
        final PVCoordinates pvCoordActual = c02Coordinates.getPVCoordinates(date, frame);
        final PVCoordinates pvCoordExp = new PVCoordinates(4450822.43276818, 4.189417829225343E7,
                -1131871.9015830054, -3.869575813517713, -4.764065283110995, -58.85651180918069);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        final double deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        final double deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(1.906233450940687E-5, c02Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Galileo LNAV Broadcast model test
     *
     * @testedMethod {@link GNSSPVCoordinates#GNSSPVCoordinates(GNSSParameters, AbsoluteDate)}
     * @testedMethod {@link LNAVGNSSParameters#LNAVGNSSParameters}
     * @testedMethod {@link GNSSPVCoordinates#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link GNSSPVCoordinates#getClockCorrection(double)}
     * @testedMethod {@link GNSSPVCoordinates#getRelativisticCorrection(double)}
     *
     * @description Computes a pv coordinates ephemeris from a reference LNAV broadcast model and
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
    public void galileoE02LNAVTest() throws PatriusException {
        // Values for the GNSSParameters constructor
        final double ecc = 2.914146753028E-04;
        final double incl = 9.741382729520E-01;
        final double omegaRate = -5.264505002050E-09;
        final double sqrtA = 5.440616081238E+03;
        final double omega0 = 5.252879612663E-01;
        final double w0 = 3.081352147714E-01;
        final double m0 = 1.387443531564E-01;
        final double af0 = 2.996585681103E-04;
        final double af1 = 2.458477865730E-12;
        final double af2 = 0.000000000000E+00;
        final double iRate = -5.035924052165E-11;
        final double deltaN = 2.520819287956E-09;
        final double cuc = 1.087971031666E-05;
        final double cus = 1.056492328644E-05;
        final double crc = 1.187500000000E+02;
        final double crs = 2.337187500000E+02;
        final double cic = 1.303851604462E-08;
        final double cis = 7.264316082001E-08;

        final double tref = 1.860000000000E+05;

        final TAIScale tai = TimeScalesFactory.getTAI();
        final Frame frame = FramesFactory.getITRF();

        final AbsoluteDate weekDate = new AbsoluteDate("2022-05-15T00:00:19", tai);

        final LNAVGNSSParameters e02Parameters = new LNAVGNSSParameters(GNSSType.Galileo, tref, m0, ecc, sqrtA, omega0,
                incl, w0, omegaRate, af0, af1, af2, deltaN, iRate, cuc, cus, crc, crs, cic, cis);

        final GNSSPVCoordinates e02Coordinates = new GNSSPVCoordinates(e02Parameters, weekDate);

        // First check
        AbsoluteDate date = new AbsoluteDate("2022-05-17T00:00:00.000", tai);
        PVCoordinates pvCoordActual = e02Coordinates.getPVCoordinates(date, frame);
        PVCoordinates pvCoordExp = new PVCoordinates(1.6938523501591254E7, -8453013.69042204, -2.275434893619985E7,
                2028.2639712968785, 1048.101499426999, 1121.913256210571);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        double deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        double deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(2.996267721413872E-4, e02Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

        // Second check
        date = new AbsoluteDate("2022-05-17T11:15:00.000", tai);
        pvCoordActual = e02Coordinates.getPVCoordinates(date, frame);
        pvCoordExp = new PVCoordinates(1.1150840225595564E7, 2.2602787321391426E7, -1.5538343572648276E7,
                -1163.4038596400005, -1039.1253045901067, -2345.6696433516645);
        // Difference in norm between expected and actual position lower than EPSILON_POS
        deltaPos = pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm();
        Assert.assertEquals(0, deltaPos, POSITION_COMPARISON_EPSILON);
        // Difference in norm between expected and actual position lower than EPSILON_VEL
        deltaVel = pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm();
        Assert.assertEquals(0, deltaVel, VELOCITY_COMPARISON_EPSILON);
        // Clock test
        Assert.assertEquals(2.9972589859703444E-4, e02Coordinates.getClockCorrection(date),
                Precision.DOUBLE_COMPARISON_EPSILON);

    }

}
