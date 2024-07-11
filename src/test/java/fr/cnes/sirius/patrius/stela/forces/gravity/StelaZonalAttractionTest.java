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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:315:26/02/2015:add zonal terms J8 to J15
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.PotentialCoefficientsProviderTest;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test Class computing Zonal Perturbation
 * </p>
 * 
 * @see STELA
 * 
 * @author Cedric Dental
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaZonalAttractionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Zonal Perturbation
         * 
         * @featureDescription Computation of Zonal Perturbation, its short periods and partial derivatives.
         * 
         * @coveredRequirements
         */
        ZONAL
    }

    /** The potential coefficients provider used for test purposes. */
    PotentialCoefficientsProviderTest provider = new PotentialCoefficientsProviderTest();

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(StelaZonalAttractionTest.class.getSimpleName(), "STELA zonal attraction force");
    }

    /**
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ZONAL}
     * 
     * @testedMethod {@link StelaZonalAttraction#computePerturbation(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of J2 to J15 perturbations
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output J2 to J15 zonal perturbations
     * 
     * @testPassCriteria references from Stela (epsilon value has been increased because of the different conception
     *                   (Patrius sums the zonals contributions))
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testComputeZonal() throws PatriusException {

        /** Epsilon */
        final double EPS = 2e-12;

        Report
            .printMethodHeader("testComputeZonal", "Perturbation computation", "STELA", 1E-8, ComparisonType.RELATIVE);

        // Context
        /** Semi-major axis (Type 8 param). */
        final double aIn = 2.43505E+7;
        /** Eccentricity (Type 8 param). */
        final double exIn = 0.3644381018870251;
        /** Eccentricity (Type 8 param). */
        final double eyIn = 0.6312253086822904;
        /** Inclination (Type 8 param). */
        final double ixIn = 0.09052430460833645;
        /** Inclination (Type 8 param). */
        final double iyIn = 0.05226423163382672;

        // Context 2
        /** Semi-major axis (Type 8 param). */
        final double aIn2 = 7628000;
        /** Excentricity (Type 8 param). */
        final double exIn2 = 0.10785581192748654;
        /** Excentricity (Type 8 param). */
        final double eyIn2 = 0.062270582049999995;
        /** Inclination (Type 8 param). */
        final double ixIn2 = 0.49240387650610395;
        /** Inclination (Type 8 param). */
        final double iyIn2 = 0.08682408883346517;

        // Expected results
        /** Expected 2nd order zonal Earth potential derivatives. */
        final double[] expDeg2 = { -0.00021824877417559615, 0, 4131.920328103231, 7156.695941101461,
            -2012.8146178633624,
            -1162.099061452226 };
        /** Expected 3rd order zonal Earth potential derivatives. */
        final double[] expDeg3 = { 8.75901861188511E-8, 0, -1.341279722969231, -4.85736057657651, -8.28755123643917,
            5.417496065517777 };
        /** Expected 4th order zonal Earth potential derivatives. */
        final double[] expDeg4 = { -1.9948956038362632E-7, 0, 6.010992171013283, 10.153445545381068,
            -3.932290684303598,
            -1.23204685411046 };
        /** Expected 5th order zonal Earth potential derivatives. */
        final double[] expDeg5 = { -1.1496242795101205E-8, 0, 0.28219745057182233, 0.7081806485322722,
            0.647027885131129,
            -0.5097130923597532 };
        /** Expected 6th order zonal Earth potential derivatives. */
        final double[] expDeg6 = { -4.634728317790751E-8, 0, 1.6629842936396668, 2.6970786987960103,
            -1.5082180619435812,
            -0.13285189804461703 };
        /** Expected 7th order zonal Earth potential derivatives. */
        final double[] expDeg7 = { 2.158618476521534E-8, 0, -0.6430172586678384, -1.4111279306701079,
            -0.7546721351766295,
            0.7615375561329488 };
        /** Expected 8th order zonal Earth potential derivatives. */
        final double[] expDeg8 =
        { 6.70441905783119E-8, 0., 0.011468971422513425, -0.16916272977218602, -4.566131917033571,
            -0.7666285707881193 };
        /** Expected 9th order zonal Earth potential derivatives. */
        final double[] expDeg9 =
        { -1.1309994589916172E-8, 0., -0.00970830483440178, 0.2141587569790011, -0.7064446670259164,
            -0.17270218004232285 };
        /** Expected 10th order zonal Earth potential derivatives. */
        final double[] expDeg10 = { -1.7102988286733263E-7, 0., 0.23399200754736107, 0.47370957735566666,
            -2.7489402684986706, -0.5588821692493408 };
        /** Expected 11th order zonal Earth potential derivatives. */
        final double[] expDeg11 =
        { 8.177903400579172E-8, 0., 0.09690916123081751, -1.3227674119054036, 0.34016469574322267,
            0.3619736589078293 };
        /** Expected 12th order zonal Earth potential derivatives. */
        final double[] expDeg12 =
        { -1.5063200190714992E-7, 0., 0.13361910023638365, 0.512750512115426, 0.7328579663023097,
            0.03380790100782666 };
        /** Expected 13th order zonal Earth potential derivatives. */
        final double[] expDeg13 =
        { -4.663715585227342E-8, 0., -0.05639834859367776, 0.6656232539227503, 0.8155874945591904,
            -0.009119857942702059 };
        /** Expected 14th order zonal Earth potential derivatives. */
        final double[] expDeg14 = { 2.0569576678705126E-8, 0., 0.015503775176038168, -0.08729298760972776,
            -1.5457295570896064, -0.25147258404504486 };
        /** Expected 15th order zonal Earth potential derivatives. */
        final double[] expDeg15 = { -1.1635475885560343E-11, 0., 0.000005846730309587421, 0.00015054064509793764,
            -0.0006649209168193153,
            -0.00014947842709333854 };
        /** Expected squared 2nd order zonal Earth potential derivatives. */
        final double[] expDeg22 = { 0.0, 1.6935037200511283E-10, -8.185254571767508E-11, 4.725758930395587E-11,
            4.721014486484003E-12, -8.177036953858986E-12 };

        final double mu = 398600441449820.000;
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getMOD(false);

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, 0, frame, date, mu);
        final StelaEquinoctialOrbit orbit2 = new StelaEquinoctialOrbit(aIn2, exIn2, eyIn2, ixIn2, iyIn2, 0, frame,
            date, mu);

        final StelaZonalAttraction zonalJ2 = new StelaZonalAttraction(this.provider, 2, false, 0, 2, false);
        final double[] pertJ2 = zonalJ2.computePerturbation(orbit);
        final double[] pertDJ2 = zonalJ2.getdPot();
        final StelaZonalAttraction zonalJ3 = new StelaZonalAttraction(this.provider, 3, false, 0, 3, false);
        final double[] pertJ3 = zonalJ3.computePerturbation(orbit);
        final double[] pertDJ3 = zonalJ3.getdPot();
        final StelaZonalAttraction zonalJ4 = new StelaZonalAttraction(this.provider, 4, false, 0, 4, false);
        final double[] pertJ4 = zonalJ4.computePerturbation(orbit);
        final double[] pertDJ4 = zonalJ4.getdPot();
        final StelaZonalAttraction zonalJ5 = new StelaZonalAttraction(this.provider, 5, false, 0, 5, false);
        final double[] pertJ5 = zonalJ5.computePerturbation(orbit);
        final double[] pertDJ5 = zonalJ5.getdPot();
        final StelaZonalAttraction zonalJ6 = new StelaZonalAttraction(this.provider, 6, false, 0, 6, false);
        final double[] pertJ6 = zonalJ6.computePerturbation(orbit);
        final double[] pertDJ6 = zonalJ6.getdPot();
        final StelaZonalAttraction zonalJ7 = new StelaZonalAttraction(this.provider, 7, false, 0, 7, false);
        final double[] pertJ7 = zonalJ7.computePerturbation(orbit);
        final double[] pertDJ7 = zonalJ7.getdPot();
        final double[] pertJ7_2 = zonalJ7.computePerturbation(orbit2);
        final StelaZonalAttraction zonalJ8 = new StelaZonalAttraction(this.provider, 8, false, 0, 7, false);
        final double[] pertJ8 = zonalJ8.computePerturbation(orbit2);
        final double[] pertDJ8 = zonalJ8.getdPot();
        final StelaZonalAttraction zonalJ9 = new StelaZonalAttraction(this.provider, 9, false, 0, 7, false);
        final double[] pertJ9 = zonalJ9.computePerturbation(orbit2);
        final double[] pertDJ9 = zonalJ9.getdPot();
        final StelaZonalAttraction zonalJ10 = new StelaZonalAttraction(this.provider, 10, false, 0, 7, false);
        final double[] pertJ10 = zonalJ10.computePerturbation(orbit2);
        final double[] pertDJ10 = zonalJ10.getdPot();
        final StelaZonalAttraction zonalJ11 = new StelaZonalAttraction(this.provider, 11, false, 0, 7, false);
        final double[] pertJ11 = zonalJ11.computePerturbation(orbit2);
        final double[] pertDJ11 = zonalJ11.getdPot();
        final StelaZonalAttraction zonalJ12 = new StelaZonalAttraction(this.provider, 12, false, 0, 7, false);
        final double[] pertJ12 = zonalJ12.computePerturbation(orbit2);
        final double[] pertDJ12 = zonalJ12.getdPot();
        final StelaZonalAttraction zonalJ13 = new StelaZonalAttraction(this.provider, 13, false, 0, 7, false);
        final double[] pertJ13 = zonalJ13.computePerturbation(orbit2);
        final double[] pertDJ13 = zonalJ13.getdPot();
        final StelaZonalAttraction zonalJ14 = new StelaZonalAttraction(this.provider, 14, false, 0, 7, false);
        final double[] pertJ14 = zonalJ14.computePerturbation(orbit2);
        final double[] pertDJ14 = zonalJ14.getdPot();
        final StelaZonalAttraction zonalJ15 = new StelaZonalAttraction(this.provider, 15, false, 0, 7, false);
        final double[] pertJ15 = zonalJ15.computePerturbation(orbit2);
        final double[] pertDJ15 = zonalJ15.getdPot();
        final StelaZonalAttraction zonalJ22 = new StelaZonalAttraction(this.provider, 2, true, 0, 0, true);
        final double[] pertJ22 = zonalJ22.computeJ2Square(orbit);
        zonalJ22.getdPot();

        // Terms are checked one by one by subtracting previous terms
        // The higher the degree, the higher the tolerance because of numerical quality issues
        for (int j = 0; j < expDeg2.length; j++) {
            this.check(pertJ2[j], expDeg2[j], pertDJ2[j], pertJ2[j], EPS);
            this.check(pertJ3[j] - pertJ2[j], expDeg3[j], pertDJ3[j], pertJ3[j], EPS);
            this.check(pertJ4[j] - pertJ3[j], expDeg4[j], pertDJ4[j], pertJ4[j], EPS);
            this.check(pertJ5[j] - pertJ4[j], expDeg5[j], pertDJ5[j], pertJ5[j], EPS);
            this.check(pertJ6[j] - pertJ5[j], expDeg6[j], pertDJ6[j], pertJ6[j], EPS);
            this.check(pertJ7[j] - pertJ6[j], expDeg7[j], pertDJ7[j], pertJ7[j], EPS);
            this.check(pertJ8[j] - pertJ7_2[j], expDeg8[j], pertDJ8[j], pertJ7_2[j], 1E-11);
            this.check(pertJ9[j] - pertJ8[j], expDeg9[j], pertDJ9[j], pertJ7_2[j], 1E-11);
            this.check(pertJ10[j] - pertJ9[j], expDeg10[j], pertDJ10[j], pertJ7_2[j], 1E-11);
            this.check(pertJ11[j] - pertJ10[j], expDeg11[j], pertDJ11[j], pertJ7_2[j], 1E-11);
            this.check(pertJ12[j] - pertJ11[j], expDeg12[j], pertDJ12[j], pertJ7_2[j], 1E-10);
            this.check(pertJ13[j] - pertJ12[j], expDeg13[j], pertDJ13[j], pertJ7_2[j], 1E-10);
            this.check(pertJ14[j] - pertJ13[j], expDeg14[j], pertDJ14[j], pertJ7_2[j], 1E-9);
            this.check(pertJ15[j] - pertJ14[j], expDeg15[j], pertDJ15[j], pertJ7_2[j], 1E-8);
            this.check(pertJ22[j], expDeg22[j], 0, 0, 1E-10);
        }

        Report.printToReport("Perturbation (degree 2)", expDeg2, pertJ2);
        Report.printToReport("Perturbation (degree 3)", expDeg3, this.subtract(pertJ3, pertJ2));
        Report.printToReport("Perturbation (degree 4)", expDeg4, this.subtract(pertJ4, pertJ3));
        Report.printToReport("Perturbation (degree 5)", expDeg5, this.subtract(pertJ5, pertJ4));
        Report.printToReport("Perturbation (degree 6)", expDeg6, this.subtract(pertJ6, pertJ5));
        Report.printToReport("Perturbation (degree 7)", expDeg7, this.subtract(pertJ7, pertJ6));
        Report.printToReport("Perturbation (degree 8)", expDeg8, this.subtract(pertJ8, pertJ7));
        Report.printToReport("Perturbation (degree 9)", expDeg9, this.subtract(pertJ9, pertJ8));
        Report.printToReport("Perturbation (degree 10)", expDeg10, this.subtract(pertJ10, pertJ9));
        Report.printToReport("Perturbation (degree 11)", expDeg11, this.subtract(pertJ11, pertJ10));
        Report.printToReport("Perturbation (degree 12)", expDeg12, this.subtract(pertJ12, pertJ11));
        Report.printToReport("Perturbation (degree 13)", expDeg13, this.subtract(pertJ13, pertJ12));
        Report.printToReport("Perturbation (degree 14)", expDeg14, this.subtract(pertJ14, pertJ13));
        Report.printToReport("Perturbation (degree 15)", expDeg15, this.subtract(pertJ15, pertJ14));
    }

    /**
     * Check method (automatic switch between absolute and relative tolerance).
     * 
     * @param actual1
     *        actual 1
     * @param expected1
     *        expected 1
     * @param actual2
     *        actual 2
     * @param expected2
     *        expected 2
     * @param eps
     *        epsilon
     */
    private void check(final double actual1, final double expected1, final double actual2, final double expected2,
                       final double eps) {
        if (MathLib.abs(expected1) < eps) {
            Assert.assertEquals(expected1, actual1, eps);
        } else {
            Assert.assertEquals(0, MathLib.abs((expected1 - actual1) / expected1), eps);
        }
        Assert.assertEquals(expected2, actual2, 0);
    }

    /**
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#ZONAL}
     * 
     * @testedMethod {@link StelaZonalAttraction#computeShortPeriods(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of J2 short Periods
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output J2 short periods
     * 
     * @testPassCriteria references from Stela (same epsiolon value)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testComputeZonalShortPeriods() throws PatriusException, IOException, ParseException {

        /** Epsilon */
        final double EPS = 1e-13;

        Report.printMethodHeader("testComputeZonalShortPeriods", "Short periods computation", "STELA", EPS,
            ComparisonType.RELATIVE);

        // Context
        /** Semi-major axis (Type 8 param). */
        final double aIn = 2.43505E+7;
        /** Eccentricity (Type 8 param). */
        final double exIn = 0.3644381018870251;
        /** Eccentricity (Type 8 param). */
        final double eyIn = 0.6312253086822904;
        /** Inclination (Type 8 param). */
        final double ixIn = 0.09052430460833645;
        /** Inclination (Type 8 param). */
        final double iyIn = 0.05226423163382672;
        /** ksi */
        final double ksi = 0.8922879297441284;

        // Expected results
        /** Expected 2nd order zonal Earth potential derivatives. */
        final double[] expDeg2 = { -2.26641766967085267, -61299898.2128525302, 7093202.63737592846,
            178839621.315058649, -15992636.9859726317, -2590550.48214230267 };

        final double mu = 398600441449820.000;
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getMOD(false);

        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, ksi, frame, date, mu);

        final StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 7, true, 2, 0, true);
        final double[] pertJ2 = zonal.computeShortPeriods(orbit);

        for (int j = 0; j < expDeg2.length; j++) {
            if (MathLib.abs(expDeg2[j]) < EPS) {
                Assert.assertEquals(expDeg2[j], pertJ2[j], EPS);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg2[j] - pertJ2[j])) / expDeg2[j], EPS);
            }
        }

        Report.printToReport("Short periods", expDeg2, pertJ2);

        // Order 8 and higher
        final double[] sp8 = new StelaZonalAttractionJ8(1., 1.).computeShortPeriods(orbit);
        final double[] sp9 = new StelaZonalAttractionJ9(1., 1.).computeShortPeriods(orbit);
        final double[] sp10 = new StelaZonalAttractionJ10(1., 1.).computeShortPeriods(orbit);
        final double[] sp11 = new StelaZonalAttractionJ11(1., 1.).computeShortPeriods(orbit);
        final double[] sp12 = new StelaZonalAttractionJ12(1., 1.).computeShortPeriods(orbit);
        final double[] sp13 = new StelaZonalAttractionJ13(1., 1.).computeShortPeriods(orbit);
        final double[] sp14 = new StelaZonalAttractionJ14(1., 1.).computeShortPeriods(orbit);
        final double[] sp15 = new StelaZonalAttractionJ15(1., 1.).computeShortPeriods(orbit);
        for (int i = 0; i < sp15.length; i++) {
            Assert.assertEquals(0, sp8[i], 0);
            Assert.assertEquals(0, sp9[i], 0);
            Assert.assertEquals(0, sp10[i], 0);
            Assert.assertEquals(0, sp11[i], 0);
            Assert.assertEquals(0, sp12[i], 0);
            Assert.assertEquals(0, sp13[i], 0);
            Assert.assertEquals(0, sp14[i], 0);
            Assert.assertEquals(0, sp15[i], 0);
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#ZONAL}
     * 
     * @testedMethod {@link StelaZonalAttraction#computePartialDerivatives(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of J2 to J7 partial derivatives
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output J2 to J7 zonal partial derivatives
     * 
     * @testPassCriteria references from Stela (epsilon value has been increased because of the different conception
     *                   (Patrius sums the zonals contributions))
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testComputeZonalPartialDerivatives() throws PatriusException, IOException, ParseException {

        /** Epsilon */
        final double EPS = 4e-12;
        // compare to zero
        final double EPS2 = 1e-17;

        Report.printMethodHeader("testComputeZonalPartialDerivatives", "Partial derivatives computation", "STELA", EPS,
            ComparisonType.RELATIVE);

        // Context
        /** Semi-major axis (Type 8 param). */
        final double aIn = 2.4228E+7;
        /** Eccentricity (Type 8 param). */
        final double exIn = 0.4592777886100000151;
        /** Eccentricity (Type 8 param). */
        final double eyIn = -0.566545395240000027;
        /** Inclination (Type 8 param). */
        final double ixIn = 0.05490687833999999962;
        /** Inclination (Type 8 param). */
        final double iyIn = -0.06770144794999999327;

        // J22 Context
        /** Semi-major axis (Type 8 param). */
        final double a22 = 2.500000000000000000E+07;
        /** Eccentricity (Type 8 param). */
        final double ex22 = 6.321985447626403687E-01;
        /** Eccentricity (Type 8 param). */
        final double ey22 = -3.649999999999997691E-01;
        /** Inclination (Type 8 param). */
        final double ix22 = 1.511422733185859013E-02;
        /** Inclination (Type 8 param). */
        final double iy22 = 8.726203218641754092E-03;

        // Expected results
        /** Expected 2nd order zonal Earth potential second derivatives. */
        final double[][] expDeg2 = {
            { 3.761540580886990856E-11, 0.000000000000000000E+00, -6.706415360790285310E-04,
                8.272746550016401874E-04, 1.548417618813642484E-04, -1.909234653185616919E-04 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -6.706415360790285310E-04, 0.000000000000000000E+00, 3.836332509548678354E+04,
                -3.277644910873915069E+04, -3.680878966274130107E+03, 4.538608700395074266E+03 },
            { 8.272746550016401874E-04, 0.000000000000000000E+00, -3.277644910873915069E+04,
                5.222427026754405233E+04, 4.540574529174987219E+03, -5.598633166622601493E+03 },
            { 1.548417618813642484E-04, 0.000000000000000000E+00, -3.680878966274130107E+03,
                4.540574529174987219E+03, -2.249607932067191723E+04, -3.438688061675471772E+02 },
            { -1.909234653185616919E-04, 0.000000000000000000E+00, 4.538608700395074266E+03,
                -5.598633166622601493E+03, -3.438688061675471772E+02, -2.235096384959620264E+04 } };
        /** Expected 3rd order zonal Earth potential second derivatives. */
        final double[][] expDeg3 = {
            { 6.701401282158124528E-18, 0.000000000000000000E+00, 1.630762631223134089E-07,
                1.325829781853021054E-07, -1.365982637561438150E-06, -1.107393506496280976E-06 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { 1.630762631223134089E-07, 0.000000000000000000E+00, -9.692310549075374482E+00,
                2.041374604559432093E+00, 4.120023400317151641E+01, 4.675762992361912751E+01 },
            { 1.325829781853021054E-07, 0.000000000000000000E+00, 2.041374604559432093E+00,
                9.717425845689099617E+00, -6.417918104572028426E+01, -4.120173545918191849E+01 },
            { -1.365982637561438150E-06, 0.000000000000000000E+00, 4.120023400317151641E+01,
                -6.417918104572028426E+01, -1.021607228275123980E+01, 2.156922935861375734E+00 },
            { -1.107393506496280976E-06, 0.000000000000000000E+00, 4.675762992361912751E+01,
                -4.120173545918191849E+01, 2.156922935861375734E+00, 1.020731955274541924E+01 } };
        /** Expected 4th order zonal Earth potential second derivatives. */
        final double[][] expDeg4 = {
            { 5.563618494540348169E-14, 0.000000000000000000E+00, -1.723053880210264554E-06,
                2.125470302070334940E-06, 4.066702628318112081E-07, -5.013073948409233020E-07 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -1.723053880210264554E-06, 0.000000000000000000E+00, 9.600737809004138512E+01,
                -9.637344524512501209E+01, -1.301053287473206765E+01, 1.911701572910384073E+01 },
            { 2.125470302070334940E-06, 0.000000000000000000E+00, -9.637344524512501209E+01,
                1.367619179423644198E+02, 1.913051721495218516E+01, -2.108503488325199982E+01 },
            { 4.066702628318112081E-07, 0.000000000000000000E+00, -1.301053287473206765E+01,
                1.913051721495218516E+01, -4.663596406002582739E+01, -1.251167415546704476E+01 },
            { -5.013073948409233020E-07, 0.000000000000000000E+00, 1.911701572910384073E+01,
                -2.108503488325199982E+01, -1.251167415546704476E+01, -4.134767948426507900E+01 } };
        /** Expected 5th order zonal Earth potential second derivatives. */
        final double[][] expDeg5 = {
            { -1.288842043737356872E-18, 0.000000000000000000E+00, -2.238275222054782128E-08,
                -1.823789560357438712E-08, 1.876471961957695724E-07, 1.521324637529486442E-07 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -2.238275222054782128E-08, 0.000000000000000000E+00, 1.688542785843794469E+00,
                -3.548964763818724943E-01, -7.210255704324085357E+00, -6.905144742601688534E+00 },
            { -1.823789560357438712E-08, 0.000000000000000000E+00, -3.548964763818724943E-01,
                -1.694739331273151794E+00, 9.953655480979668013E+00, 7.210859072622519683E+00 },
            { 1.876471961957695724E-07, 0.000000000000000000E+00, -7.210255704324085357E+00,
                9.953655480979668013E+00, 2.276773420059845066E+00, -4.805532468395692924E-01 },
            { 1.521324637529486442E-07, 0.000000000000000000E+00, -6.905144742601688534E+00,
                7.210859072622519683E+00, -4.805532468395692924E-01, -2.274663627802030241E+00 } };
        /** Expected 6th order zonal Earth potential second derivatives. */
        final double[][] expDeg6 = {
            { 1.972440989463499937E-14, 0.000000000000000000E+00, -7.410412155726217849E-07,
                9.141010266289884313E-07, 1.772388720456362049E-07, -2.184086647000182884E-07 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -7.410412155726217849E-07, 0.000000000000000000E+00, 4.128143617561523371E+01,
                -4.430603326595345237E+01, -6.233518241950852357E+00, 9.959077489709626363E+00 },
            { 9.141010266289884313E-07, 0.000000000000000000E+00, -4.430603326595345237E+01,
                6.001683469605217880E+01, 9.969160018564847903E+00, -1.043761848605265996E+01 },
            { 1.772388720456362049E-07, 0.000000000000000000E+00, -6.233518241950852357E+00,
                9.969160018564847903E+00, -1.936111619036313414E+01, -8.964463399371975783E+00 },
            { -2.184086647000182884E-07, 0.000000000000000000E+00, 9.959077489709626363E+00,
                -1.043761848605265996E+01, -8.964463399371975783E+00, -1.557264017936071276E+01 } };
        /** Expected 7th order zonal Earth potential second derivatives. */
        final double[][] expDeg7 = {
            { 3.313406220141398319E-18, 0.000000000000000000E+00, 4.471405229642231914E-08,
                3.651825892130114615E-08, -3.751993977611181913E-07, -3.042063879591798104E-07 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { 4.471405229642231914E-08, 0.000000000000000000E+00, -3.783318021018346577E+00,
                7.934258110975503930E-01, 1.619881508166221096E+01, 1.444137577299040309E+01 },
            { 3.651825892130114615E-08, 0.000000000000000000E+00, 7.934258110975503930E-01,
                3.801485383739516610E+00, -2.128934562031006195E+01, -1.620100340958000018E+01 },
            { -3.751993977611181913E-07, 0.000000000000000000E+00, 1.619881508166221096E+01,
                -2.128934562031006195E+01, -5.718853686417691762E+00, 1.206499364798915064E+00 },
            { -3.042063879591798104E-07, 0.000000000000000000E+00, 1.444137577299040309E+01,
                -1.620100340958000018E+01, 1.206499364798915064E+00, 5.712867053309679477E+00 } };
        /** Expected 2nd order zonal Earth potential (J22) contribution. */
        final double[][] expDeg22 = {
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00 },
            { -3.648798626973877402E-17, 0.000000000000000000E+00, 1.726503652611051758E-09,
                -9.967973485918617959E-10,
                -5.126522453016687679E-11, -2.959799118255844792E-11 },
            { -1.021873804662106717E-17, 0.000000000000000000E+00, 4.975259589912214295E-10,
                -4.145037581409644248E-10,
                -1.456826465472083550E-11, -8.410991520028783016E-12 },
            { -1.769937348598484737E-17, 0.000000000000000000E+00, 9.889972508457824044E-10,
                -4.975259589912213261E-10,
                -2.523297456008637490E-11, -1.456826465472084519E-11 },
            { -1.631980709567056957E-19, 0.000000000000000000E+00, 7.799189639645061288E-12,
                -4.502864237910013480E-12,
                -1.468285953855423991E-13, 8.492464148286190012E-11 },
            { 2.826673505942451338E-19, 0.000000000000000000E+00, -1.350859271373005336E-11,
                7.799189639645059672E-12,
                -8.475509842472074587E-11, 1.468285953855424243E-13 } };

        final double mu = 398600441449820.000;
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        final AbsoluteDate date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getMOD(false);

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, 0, frame, date, mu);

        StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 7, false, 2, 2, false);
        final double[][] pertJ2 = zonal.computePartialDerivatives(orbit);
        zonal = new StelaZonalAttraction(this.provider, 7, false, 2, 3, false);
        final double[][] pertJ3 = zonal.computePartialDerivatives(orbit);
        zonal = new StelaZonalAttraction(this.provider, 7, false, 2, 4, false);
        final double[][] pertJ4 = zonal.computePartialDerivatives(orbit);
        zonal = new StelaZonalAttraction(this.provider, 7, false, 2, 5, false);
        final double[][] pertJ5 = zonal.computePartialDerivatives(orbit);
        zonal = new StelaZonalAttraction(this.provider, 7, false, 2, 6, false);
        final double[][] pertJ6 = zonal.computePartialDerivatives(orbit);
        zonal = new StelaZonalAttraction(this.provider, 7, false, 2, 7, false);
        final double[][] pertJ7 = zonal.computePartialDerivatives(orbit);
        final StelaEquinoctialOrbit orbit22 =
            new StelaEquinoctialOrbit(a22, ex22, ey22, ix22, iy22, 0, frame, date, mu);
        zonal = new StelaZonalAttraction(this.provider, 7, true, 2, 0, true);
        final double[][] pertJ22 = zonal.computeJ2SquarePartialDerivatives(orbit22);

        for (int i = 0; i < pertJ2.length; i++) {
            for (int j = 0; j < pertJ2[0].length; j++) {

                if (MathLib.abs(expDeg2[i][j]) < EPS2) {

                    Assert.assertEquals(expDeg2[i][j], pertJ2[i][j], EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expDeg2[i][j] - pertJ2[i][j])) / expDeg2[i][j], 0, EPS);
                }
                if (MathLib.abs(expDeg3[i][j]) < EPS2) {

                    Assert.assertEquals(expDeg3[i][j], pertJ3[i][j] - pertJ2[i][j], EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expDeg3[i][j] - (pertJ3[i][j] - pertJ2[i][j]))) / expDeg3[i][j],
                        0, EPS);
                }
                if (MathLib.abs(expDeg4[i][j]) < EPS2) {

                    Assert.assertEquals(expDeg4[i][j], pertJ4[i][j] - pertJ3[i][j], EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expDeg4[i][j] - (pertJ4[i][j] - pertJ3[i][j]))) / expDeg4[i][j],
                        0, EPS);
                }
                if (MathLib.abs(expDeg5[i][j]) < EPS2) {

                    Assert.assertEquals(expDeg5[i][j], pertJ5[i][j] - pertJ4[i][j], EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expDeg5[i][j] - (pertJ5[i][j] - pertJ4[i][j]))) / expDeg5[i][j],
                        0, EPS);
                }
                if (MathLib.abs(expDeg6[i][j]) < EPS2) {

                    Assert.assertEquals(expDeg6[i][j], pertJ6[i][j] - pertJ5[i][j], EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expDeg6[i][j] - (pertJ6[i][j] - pertJ5[i][j]))) / expDeg6[i][j],
                        0, EPS);
                }
                if (MathLib.abs(expDeg7[i][j]) < EPS2) {

                    Assert.assertEquals(expDeg7[i][j], pertJ7[i][j] - pertJ6[i][j], EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expDeg7[i][j] - (pertJ7[i][j] - pertJ6[i][j]))) / expDeg7[i][j],
                        0, EPS);
                }
                if (MathLib.abs(expDeg22[i][j]) < EPS2) {

                    Assert.assertEquals(expDeg22[i][j], pertJ22[i][j]
                        , EPS);

                } else {

                    Assert.assertEquals(MathLib.abs((expDeg22[i][j] - (pertJ22[i][j]
                        )))
                        / expDeg22[i][j], 0, EPS);
                }
            }
        }

        Report.printToReport("Partial derivatives (degree 2)", expDeg2, pertJ2);
        Report.printToReport("Partial derivatives (degree 3)", expDeg3, this.subtract(pertJ3, pertJ2));
        Report.printToReport("Partial derivatives (degree 4)", expDeg4, this.subtract(pertJ4, pertJ3));
        Report.printToReport("Partial derivatives (degree 5)", expDeg5, this.subtract(pertJ5, pertJ4));
        Report.printToReport("Partial derivatives (degree 6)", expDeg6, this.subtract(pertJ6, pertJ5));
        Report.printToReport("Partial derivatives (degree 7)", expDeg7, this.subtract(pertJ7, pertJ6));

        // Order 8 and higher
        final double[][] sp8 = new StelaZonalAttractionJ8(1., 1.).computePartialDerivatives(orbit);
        final double[][] sp9 = new StelaZonalAttractionJ9(1., 1.).computePartialDerivatives(orbit);
        final double[][] sp10 = new StelaZonalAttractionJ10(1., 1.).computePartialDerivatives(orbit);
        final double[][] sp11 = new StelaZonalAttractionJ11(1., 1.).computePartialDerivatives(orbit);
        final double[][] sp12 = new StelaZonalAttractionJ12(1., 1.).computePartialDerivatives(orbit);
        final double[][] sp13 = new StelaZonalAttractionJ13(1., 1.).computePartialDerivatives(orbit);
        final double[][] sp14 = new StelaZonalAttractionJ14(1., 1.).computePartialDerivatives(orbit);
        final double[][] sp15 = new StelaZonalAttractionJ15(1., 1.).computePartialDerivatives(orbit);
        for (int i = 0; i < sp15.length; i++) {
            for (int j = 0; j < sp15.length; j++) {
                Assert.assertEquals(0, sp8[i][j], 0);
                Assert.assertEquals(0, sp9[i][j], 0);
                Assert.assertEquals(0, sp10[i][j], 0);
                Assert.assertEquals(0, sp11[i][j], 0);
                Assert.assertEquals(0, sp12[i][j], 0);
                Assert.assertEquals(0, sp13[i][j], 0);
                Assert.assertEquals(0, sp14[i][j], 0);
                Assert.assertEquals(0, sp15[i][j], 0);
            }
        }
    }

    /**
     * Subtract two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @return subtracted vectors
     */
    private double[] subtract(final double[] v1, final double[] v2) {
        final double[] res = new double[v1.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = v1[i] - v2[i];
        }
        return res;
    }

    /**
     * Subtract two matrices.
     * 
     * @param v1
     *        first matrix
     * @param v2
     *        second matrix
     * @return subtracted matrices
     */
    private double[][] subtract(final double[][] v1, final double[][] v2) {
        final double[][] res = new double[v1.length][v1[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                res[i][j] = v1[i][j] - v2[i][j];
            }
        }
        return res;
    }
}
