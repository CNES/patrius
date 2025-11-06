package fr.cnes.sirius.patrius.stela.forces.gravity.recurrence;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.linear.CheckUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.PotentialCoefficientsProviderTest;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test cases for the zonal Earth potential perturbations computed using recurrence methods
 * ({@link StelaRecurrenceZonalAttraction} & {@link StelaStelaRecurrenceZonalEquationuation} class).
 * <p>
 * This test is based on STELA's test "RecurrenceZonalTest" (reference version : 3.7-SNAPSHOT).
 * </p>
 * 
 * @author Maxime Ecochard, Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-391:25/04/2025:[STELA-PATRIUS] Implementation zonaux par recurrence
 * END-HISTORY
 * @since 4.16
 */
public class StelaRecurrenceZonalTest {

    // TU Satlight 28/11/2011 (J2, J3, J4, J5)
    // TU Satlight 02/03/2012 (J2, squared J2, J6, J7)
    // TU Satlight 04/09/2013 (J8 to J15)

    // Context 1
    /** Semi-major axis. */
    private static final double a = 2.43505E+7;
    /** Excentricity. */
    private static final double ex = 0.3644381018870251;
    /** Excentricity. */
    private static final double ey = 0.6312253086822904;
    /** Inclination. */
    private static final double ix = 0.09052430460833645;
    /** Inclination. */
    private static final double iy = 0.05226423163382672;

    // Context 2
    /** Semi-major axis. */
    private static final double a2 = 7628000;
    /** Excentricity. */
    private static final double ex2 = 0.10785581192748654;
    /** Excentricity. */
    private static final double ey2 = 0.062270582049999995;
    /** Inclination. */
    private static final double ix2 = 0.49240387650610395;
    /** Inclination. */
    private static final double iy2 = 0.08682408883346517;

    // Expected results
    /** Expected 2nd order zonal Earth potential derivatives. */
    private static final double[] expDeg2 =
        { -2.1824877417559615E-4, 0.0, 4131.920328103233, 7156.695941101464, -2012.8146178633629, -1162.0990614522264 };
    /** Expected 3rd order zonal Earth potential derivatives. */
    private static final double[] expDeg3 =
        { 8.75901861188511E-8, 0.0, -1.3412797229692321, -4.857360576576513, -8.287551236439171, 5.417496065517779 };
    /** Expected 4th order zonal Earth potential derivatives. */
    private static final double[] expDeg4 =
        { -1.9948956038362642E-7, 0.0, 6.010992171013287, 10.153445545381075, -3.9322906843036, -1.2320468541104614 };
    /** Expected 5th order zonal Earth potential derivatives. */
    private static final double[] expDeg5 = { -1.1496242795101212E-8, 0.0, 0.2821974505718225, 0.7081806485322726,
        0.6470278851311291, -0.5097130923597533 };
    /** Expected 6th order zonal Earth potential derivatives. */
    private static final double[] expDeg6 = { -4.634728317790753E-8, 0.0, 1.662984293639668, 2.6970786987960125,
        -1.5082180619435825, -0.13285189804461686 };
    /** Expected 7th order zonal Earth potential derivatives. */
    private static final double[] expDeg7 = { 2.158618476521536E-8, 0.0, -0.6430172586678393, -1.4111279306701094,
        -0.7546721351766301, 0.7615375561329496 };
    /** Expected 8th order zonal Earth potential derivatives. */
    private static final double[] expDeg8 = { 6.704419057831528E-8, 0.0, 0.011468971422448557, -0.169162729772198,
        -4.566131917033672, -0.7666285707880918 };
    /** Expected 9th order zonal Earth potential derivatives. */
    private static final double[] expDeg9 = { -1.1309994589913947E-8, 0.0, -0.009708304834395061, 0.21415875697895767,
        -0.7064446670259699, -0.17270218004231633 };
    /** Expected 10th order zonal Earth potential derivatives. */
    private static final double[] expDeg10 = { -1.7102988286730945E-7, 0.0, 0.2339920075475658, 0.47370957735563163,
        -2.7489402684985365, -0.5588821692493131 };
    /** Expected 11th order zonal Earth potential derivatives. */
    private static final double[] expDeg11 = { 8.177903400582699E-8, 0.0, 0.09690916123089034, -1.3227674119059731,
        0.34016469574261754, 0.3619736589079088 };
    /** Expected 12th order zonal Earth potential derivatives. */
    private static final double[] expDeg12 = { -1.5063200190699166E-7, 0.0, 0.13361910023648968, 0.5127505121160094,
        0.7328579662991762, 0.03380790100746257 };
    /** Expected 13th order zonal Earth potential derivatives. */
    private static final double[] expDeg13 = { -4.663715585223029E-8, 0.0, -0.056398348593570916, 0.6656232539222693,
        0.8155874945591963, -0.009119857942611345 };
    /** Expected 14th order zonal Earth potential derivatives. */
    private static final double[] expDeg14 = { 2.05695766788318E-8, 0.0, 0.015503775185402836, -0.08729298760891965,
        -1.5457295570824068, -0.2514725840459936 };
    /** Expected 15th order zonal Earth potential derivatives. */
    private static final double[] expDeg15 = { -1.1635475886262802E-11, 0.0, 5.8467303090629574E-6,
        1.5054064510679823E-4, -6.649209168166169E-4, -1.4947842709496287E-4 };
    /** Expected squared 2nd order zonal Earth potential derivatives. */
    private static final double[] expDeg22 = { 0.0, 1.6935037200511254E-10, -8.185254571767494E-11,
        4.725758930395578E-11, 4.721014486483997E-12, -8.177036953858978E-12 };

    /** The potential coefficients provider used for test purposes. */
    private static PotentialCoefficientsProviderTest provider;

    /** Tolerance. */
    private static final double tol14 = 1.E-14;
    private static final double tol13 = 1.E-13;
    private static final double tol11 = 1.E-11;

    // ========== Test methods ==========

    /**
     * setUp.
     */
    @BeforeClass
    public static void setUp() {

        // Next line clears data set by other tests
        Utils.clear();
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        provider = new PotentialCoefficientsProviderTest();
    }

    /**
     * Basic test to evaluate the constructors behavior and basic getters.
     * 
     * @throws PatriusException
     *         if perturbation computation fails
     */
    @Test
    public void testConstructor() throws PatriusException {

        // Simple constructor
        final StelaRecurrenceZonalAttraction zonalHarmonics0 = new StelaRecurrenceZonalAttraction(provider, 15);
        Assert.assertEquals(15, zonalHarmonics0.getZonalDegreeMaxPerturbation());
        Assert.assertTrue(zonalHarmonics0.isJ2SquareComputed());
        Assert.assertTrue(zonalHarmonics0.isNormalizedLegendrePolynomials());

        final StelaRecurrenceZonalAttraction zonalHarmonics1 =
            new StelaRecurrenceZonalAttraction(provider, 0, true, true);
        Assert.assertEquals(0, zonalHarmonics1.getZonalDegreeMaxPerturbation());
        Assert.assertTrue(zonalHarmonics1.isJ2SquareComputed());
        Assert.assertTrue(zonalHarmonics1.isNormalizedLegendrePolynomials());

        final StelaRecurrenceZonalAttraction zonalHarmonics2 =
            new StelaRecurrenceZonalAttraction(provider, 1, false, true);
        Assert.assertEquals(1, zonalHarmonics2.getZonalDegreeMaxPerturbation());
        Assert.assertFalse(zonalHarmonics2.isJ2SquareComputed());
        Assert.assertTrue(zonalHarmonics2.isNormalizedLegendrePolynomials());

        final StelaRecurrenceZonalAttraction zonalHarmonics3 =
            new StelaRecurrenceZonalAttraction(provider, 2, true, false);
        Assert.assertEquals(2, zonalHarmonics3.getZonalDegreeMaxPerturbation());
        Assert.assertTrue(zonalHarmonics3.isJ2SquareComputed());
        Assert.assertFalse(zonalHarmonics3.isNormalizedLegendrePolynomials());

        final StelaRecurrenceZonalAttraction zonalHarmonics4 =
            new StelaRecurrenceZonalAttraction(provider, 5, false, false);
        Assert.assertEquals(5, zonalHarmonics4.getZonalDegreeMaxPerturbation());
        Assert.assertFalse(zonalHarmonics4.isJ2SquareComputed());
        Assert.assertFalse(zonalHarmonics4.isNormalizedLegendrePolynomials());

        // Evaluate short periods computation (not used for this force model), should be a 0 array
        final double[] shortPeriods = zonalHarmonics4.computeShortPeriods(null, null);
        CheckUtils.checkEquality(new double[6], shortPeriods, tol14, tol14);

        // Try to build the force model with a negative degree (should fail)
        try {
            new StelaRecurrenceZonalAttraction(provider, -1, false, false);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Test zonal Earth potential when order 0 is needed. Attention is paid if J22 is computed or not.<br>
     * Test method for {@link StelaRecurrenceZonalAttraction#computePerturbation(StelaEquinoctialOrbit)}.
     * 
     * @throws PatriusException
     *         if perturbation computation fails
     */
    @Test
    public void testEarthPotentialDeg0() throws PatriusException {

        // Initialization
        final int order = 0;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);

        final StelaRecurrenceZonalAttraction zonalHarmonicsTrue =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);
        final double[] resTrue = zonalHarmonicsTrue.computePerturbation(orbit);

        final StelaRecurrenceZonalAttraction zonalHarmonicsFalse =
            new StelaRecurrenceZonalAttraction(provider, order, false, true);
        final double[] resFalse = zonalHarmonicsFalse.computePerturbation(orbit);

        // For degree < 2, the J22 term is not used ; so we should observe no impact if it's enabled or not
        CheckUtils.checkEquality(resFalse, resTrue, tol14, tol14);
    }

    /**
     * Test zonal Earth potential when order 2 is needed. Attention is paid if J22 is computed or not.<br>
     * Test method for {@link StelaRecurrenceZonalAttraction#computePerturbation(StelaEquinoctialOrbit)}.
     * 
     * @throws PatriusException
     *         if perturbation computation fails
     */
    @Test
    public void testEarthPotentialDeg2WithJ22() throws PatriusException {

        // Initialization
        final int order = 2;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);

        final StelaRecurrenceZonalAttraction zonalHarmonicsTrue =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);
        final double[] resTrue = zonalHarmonicsTrue.computePerturbation(orbit);

        final StelaRecurrenceZonalAttraction zonalHarmonicsFalse =
            new StelaRecurrenceZonalAttraction(provider, order, false, true);
        final double[] resFalse = zonalHarmonicsFalse.computePerturbation(orbit);

        // For degree >= 2, the J22 term is used ; so we should observe impact if it's enabled or not (expect different
        // results)
        boolean isSame = true;
        for (int i = 0; i < resFalse.length; i++) {
            if (MathLib.abs(resTrue[i] - resFalse[i]) > tol14) {
                isSame = false;
                break;
            }
        }
        Assert.assertFalse(isSame);
    }

    /**
     * Test 2nd order zonal Earth potential and compare the normalized / unnormalized configuration (which should
     * produce the same J2 zonal terms).<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg2UnnormalizedLegendrePolynomials() throws PatriusException {

        // Initialization
        final int order = 2;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);

        // Compute J2 zonal term using normalized Legendre Polynomials for the zonal perturbation
        final StelaRecurrenceZonalAttraction zonalHarmonicsNorm =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);
        final StelaRecurrenceZonalEquation zonalEqNorm = zonalHarmonicsNorm.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg2Norm = zonalHarmonicsNorm.nDegZonalPartialDerivatives(zonalEqNorm, order);

        // Compute J2 zonal term using unnormalized Legendre Polynomials for the zonal perturbation
        final StelaRecurrenceZonalAttraction zonalHarmonicsUnnorm =
            new StelaRecurrenceZonalAttraction(provider, order, true, false);
        final StelaRecurrenceZonalEquation zonalEqUnnorm =
            zonalHarmonicsUnnorm.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg2Unnorm = zonalHarmonicsUnnorm.nDegZonalPartialDerivatives(zonalEqUnnorm, order);

        // The resulting J2 zonal terms should be the same as the betaN factor is used to denormalized the normalized
        // terms
        CheckUtils.checkEquality(resDeg2Norm, resDeg2Unnorm, tol14, tol14);
    }

    /**
     * Test 2nd order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg2() throws PatriusException {

        // Initialization
        final int order = 2;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg2 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg2, resDeg2, tol14, tol14);
    }

    /**
     * Test 3rd order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg3() throws PatriusException {

        // Initialization
        final int order = 3;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg3 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg3, resDeg3, tol14, tol14);
    }

    /**
     * Test 4th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg4() throws PatriusException {

        // Initialization
        final int order = 4;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg4 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg4, resDeg4, tol14, tol14);
    }

    /**
     * Test 5th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg5() throws PatriusException {

        // Initialization
        final int order = 5;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg5 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg5, resDeg5, tol14, tol14);
    }

    /**
     * Test 6th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg6() throws PatriusException {

        // Initialization
        final int order = 6;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg6 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg6, resDeg6, tol14, tol14);
    }

    /**
     * Test 7th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg7() throws PatriusException {

        // Initialization
        final int order = 7;
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg7 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg7, resDeg7, tol14, tol14);
    }

    /**
     * Test 8th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg8() throws PatriusException {

        // Initialization
        final int order = 8;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg8 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg8, resDeg8, tol13, tol14);
    }

    /**
     * Test 9th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg9() throws PatriusException {

        // Initialization
        final int order = 9;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg9 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg9, resDeg9, tol13, tol14);
    }

    /**
     * Test 10th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg10() throws PatriusException {

        // Initialization
        final int order = 10;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg10 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg10, resDeg10, tol11, tol13);
    }

    /**
     * Test 11th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg11() throws PatriusException {

        // Initialization
        final int order = 11;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg11 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg11, resDeg11, tol11, tol13);
    }

    /**
     * Test 12th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg12() throws PatriusException {

        // Initialization
        final int order = 12;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg12 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg12, resDeg12, tol11, tol13);
    }

    /**
     * Test 13th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg13() throws PatriusException {

        // Initialization
        final int order = 13;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg13 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg13, resDeg13, tol11, tol13);
    }

    /**
     * Test 14th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg14() throws PatriusException {

        // Initialization
        final int order = 14;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg14 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg14, resDeg14, tol11, tol11);
    }

    /**
     * Test 15th order zonal Earth potential.<br>
     * Test method for
     * {@link StelaRecurrenceZonalAttraction#nDegZonalPartialDerivatives(StelaRecurrenceZonalEquation, int)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg15() throws PatriusException {

        // Initialization
        final int order = 15;
        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(a2, ex2, ey2, ix2, iy2, 0, FramesFactory.getCIRF(),
                AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, order, true, true);

        final StelaRecurrenceZonalEquation zonalEq = zonalHarmonics.buildStelaRecurrenceZonalEquation(orbit);
        final double[] resDeg15 = zonalHarmonics.nDegZonalPartialDerivatives(zonalEq, order);

        CheckUtils.checkEquality(expDeg15, resDeg15, tol11, tol11);
    }

    /**
     * Test squared 2nd order zonal Earth potential.<br>
     * Test method for {@link StelaRecurrenceZonalAttraction#derParUdeg22(StelaEquinoctialOrbit)}
     * 
     * @throws PatriusException
     *         if the requested maximal degree exceeds the available degree
     */
    @Test
    public void testEarthPotentialDeg22() throws PatriusException {
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, 0, FramesFactory.getCIRF(),
            AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        final StelaRecurrenceZonalAttraction zonalHarmonics =
            new StelaRecurrenceZonalAttraction(provider, 5, true, true);

        final double[] resDeg22 = zonalHarmonics.derParUdeg22(orbit);
        CheckUtils.checkEquality(expDeg22, resDeg22, tol11, tol11);
    }
}
