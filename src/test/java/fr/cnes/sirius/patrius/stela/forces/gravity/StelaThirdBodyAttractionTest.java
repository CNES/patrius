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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2556:27/01/2021:PATRIUS] Extension modele lunisolaire dans STELA 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.6:FA:FA-2692:27/01/2021:[PATRIUS] Robustification de AbstractGroundPointing dans le cas de vitesses non significatives 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.IAUPole;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.ephemeris.IEphemerisBody;
import fr.cnes.sirius.patrius.tools.ephemeris.IUserEphemeris;
import fr.cnes.sirius.patrius.tools.ephemeris.UserCelestialBody;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class computing third body attraction.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaThirdBodyAttractionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Third body attraction
         * 
         * @featureDescription Computation of Third body attraction perturbations, its short periods and partial
         *                     derivatives.
         * 
         * @coveredRequirements
         */
        STELA_THIRD_BODY_ATTRACTION
    }

    /** The date. */
    private static AbsoluteDate date;

    /** Third body coordinates output frame. */
    private static Frame frame;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report
            .printClassHeader(StelaThirdBodyAttractionTest.class.getSimpleName(), "STELA third body attraction force");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_THIRD_BODY_ATTRACTION}
     * 
     * @testedMethod {@link StelaThirdBodyAttraction#computePerturbation(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of third body perturbations
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output third body perturbations degree 2 to 8
     * 
     * @testPassCriteria references from Stela 3.3 (degree <= 5: eps = 1E-8, degree >= 6: eps = 1E-14)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testComputePerturbations() throws PatriusException {

        /** Epsilon */
        final double EPS = 1e-8;

        Report.printMethodHeader("testComputePerturbations", "Perturbation computation (degree 0 to 5)", "STELA 3.3", EPS,
            ComparisonType.RELATIVE);

        // Context:
        /** Equinoctial parameters: */
        final double a = 2.43505E+7;
        final double ksi = 1.919862177193762;
        final double ex = 0.3644381018870251;
        final double ey = 0.6312253086822904;
        final double ix = 0.09052430460833645;
        final double iy = 0.05226423163382672;

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, ksi, FramesFactory.getCIRF(),
            date,
            Constants.CNES_STELA_MU);

        // The user celestial body:
        final UserEphemerisForTest1 ephemeris = new UserEphemerisForTest1();
        final EphemerisBodyForTest body = new EphemerisBodyForTest();
        final UserCelestialBody sun = new UserCelestialBody(body, ephemeris,
            FramesConfigurationFactory.getStelaConfiguration());

        // Compute the perturbation for degree 0:
        final StelaThirdBodyAttraction thirdBody0 = new StelaThirdBodyAttraction(sun, 0, 2, 2);
        final double[] pert0 = thirdBody0.computePerturbation(orbit);

        // Compute the perturbation for degree 1 (throws an exception):
        boolean rez = false;
        try {
            new StelaThirdBodyAttraction(sun, 1, 2, 2);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // Compute the perturbation for degree 2:
        final StelaThirdBodyAttraction thirdBody2 = new StelaThirdBodyAttraction(sun, 2, 2, 2);
        final double[] pert2 = thirdBody2.computePerturbation(orbit);

        // Compute the perturbation for degree 3:
        final StelaThirdBodyAttraction thirdBody3 = new StelaThirdBodyAttraction(sun, 3, 2, 2);
        final double[] pert3 = thirdBody3.computePerturbation(orbit);

        // Compute the perturbation for degree 4:
        final StelaThirdBodyAttraction thirdBody4 = new StelaThirdBodyAttraction(sun, 4, 2, 2);
        final double[] pert4 = thirdBody4.computePerturbation(orbit);

        // Compute the perturbation for degree 5:
        final StelaThirdBodyAttraction thirdBody5 = new StelaThirdBodyAttraction(sun, 5, 2, 2);
        final double[] pert5 = thirdBody5.computePerturbation(orbit);

        // Expected results
        /** Expected degree 0 third body potential derivatives. */
        final double[] expDeg0 = { 0., 0., 0., 0., 0., 0. };
        /** Expected degree 2 third body potential derivatives. */
        final double[] expDeg2 = { 5.720536384736034E-7, 0, 56.14950298066464, -27.850234370487286, 11.494109005997633,
            -5.140911691277889 };
        /** Expected degree 3 third body potential derivatives. */
        final double[] expDeg3 = { 1.383724191750181E-10, 0, -0.006317691867123573, 0.01384828528241165,
            -0.0025382144796967532, 0.0023454965489764085 };
        /** Expected degree 4 third body potential derivatives. */
        final double[] expDeg4 = { -7.982076086717027E-14, 0, -0.0000012255043034701827, -0.000003219239081079885,
            2.2433508321431105E-7, -6.744402956829258E-7 };
        /** Expected degree 5 third body potential derivatives. */
        final double[] expDeg5 = { 1.650131546497912E-17, 0, 7.466118927903608E-10, 2.7190950374235063E-10,
            6.104653494289863E-11, 1.2498974868964605E-10 };
        /** Expected third body derivatives. */
        final double[] expDeriv = { 5.721919310885188E-7, 0, 56.14318406403983, -27.836389304172044,
            11.491571015914065, -5.138566869044219 };

        for (int j = 0; j < expDeg0.length; j++) {
            // degree 0
            Assert.assertEquals(expDeg0[j], pert0[j], EPS);
        }

        for (int j = 0; j < expDeg2.length; j++) {
            // degree 2
            if (MathLib.abs(expDeg2[j]) < EPS) {
                Assert.assertEquals(expDeg2[j], pert2[j], EPS);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg2[j] - pert2[j]) / expDeg2[j]), EPS);
            }
        }
        for (int j = 0; j < expDeg3.length; j++) {
            // degree 3
            if (MathLib.abs(expDeg3[j]) < EPS) {
                Assert.assertEquals(expDeg3[j], pert3[j] - pert2[j], EPS);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg3[j] - pert3[j] + pert2[j]) / expDeg3[j]), EPS);
            }
        }
        for (int j = 0; j < expDeg4.length; j++) {
            // degree 4
            if (MathLib.abs(expDeg4[j]) < EPS) {
                Assert.assertEquals(expDeg4[j], pert4[j] - pert3[j], EPS);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg4[j] - pert4[j] + pert3[j]) / expDeg4[j]), EPS);
            }
        }
        for (int j = 0; j < expDeg5.length; j++) {
            // degree 5
            if (MathLib.abs(expDeg5[j]) < EPS) {
                Assert.assertEquals(expDeg5[j], pert5[j] - pert4[j], EPS);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg5[j] - pert5[j] + pert4[j]) / expDeg5[j]), EPS);
            }
        }
        for (int j = 0; j < expDeriv.length; j++) {
            // degree 5 - Total derivatives
            if (MathLib.abs(expDeriv[j]) < EPS) {
                Assert.assertEquals(expDeriv[j], pert5[j], EPS);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeriv[j] - pert5[j]) / expDeriv[j]), EPS);
            }
        }
        Report.printToReport("Perturbation (degree 2)", expDeg2, pert2);
        Report.printToReport("Perturbation (degree 3)", expDeg3, subtract(pert3, pert2));
        Report.printToReport("Perturbation (degree 4)", expDeg4, subtract(pert4, pert3));
        Report.printToReport("Perturbation (degree 5)", expDeg5, subtract(pert5, pert4));
        
        // Degrees 6 to 8

        /** Epsilon */
        final double EPS2 = 1e-15;

        Report.printMethodHeader("testComputePerturbations", "Perturbation computation (degree 6 to 8)", "STELA 3.3", EPS2,
                ComparisonType.RELATIVE);

        // Compute the perturbation for degree 6 to 8
        final StelaThirdBodyAttraction thirdBody6 = new StelaThirdBodyAttraction(sun, 6, 2, 2);
        final double[] pert6 = thirdBody6.computePerturbation(orbit);
        final StelaThirdBodyAttraction thirdBody7 = new StelaThirdBodyAttraction(sun, 7, 2, 2);
        final double[] pert7 = thirdBody7.computePerturbation(orbit);
        final StelaThirdBodyAttraction thirdBody8 = new StelaThirdBodyAttraction(sun, 8, 2, 2);
        final double[] pert8 = thirdBody8.computePerturbation(orbit);

        // Results which are for a specific order only not taking into account lower orders (reference STELA 3.4)
        // They must then be added to take into account lower orders
        final double[] expDeg6 = { -3.62781477800391E-22, 0, -1.649599591318378E-13, 9.303190768031335E-14, -2.9812759417202503E-14,
                -9.825931521113246E-15 };
        final double[] expDeg7 = { -8.711421799479971E-25, 0, 9.20092452998783E-18, -4.431127762220445E-17, 5.5446390240033795E-18,
                -2.5230896469095608E-18 };
        final double[] expDeg8 = { 2.9635669449811877E-28, 0, 6.52067246230141E-21, 8.575299382734076E-21, 7.064481911467409E-24,
                1.124449647572516E-21 };
        final double[] exp2 = expDeg2;
        final double[] exp3 = add(expDeg3, exp2);
        final double[] exp4 = add(expDeg4, exp3);
        final double[] exp5 = add(expDeg5, exp4);
        final double[] exp6 = add(expDeg6, exp5);
        final double[] exp7 = add(expDeg7, exp6);
        final double[] exp8 = add(expDeg8, exp7);
        
        // Print to report
        Report.printToReport("Perturbation (degree 6)", exp6, pert6);
        Report.printToReport("Perturbation (degree 7)", exp7, pert7);
        Report.printToReport("Perturbation (degree 8)", exp8, pert8);
        
        // Check
        for (int j = 0; j < expDeg6.length; j++) {
            // degree 6
            if (MathLib.abs(expDeg6[j]) < EPS2) {
                Assert.assertEquals(expDeg6[j], pert6[j] - pert5[j], EPS2);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg6[j] - pert6[j] + pert5[j]) / expDeg6[j]), EPS2);
            }
        }
        for (int j = 0; j < expDeg7.length; j++) {
            // degree 7
            if (MathLib.abs(expDeg7[j]) < EPS2) {
                Assert.assertEquals(expDeg7[j], pert7[j] - pert6[j], EPS2);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg7[j] - pert7[j] + pert6[j]) / expDeg7[j]), EPS2);
            }
        }
        for (int j = 0; j < expDeg8.length; j++) {
            // degree 8
            if (MathLib.abs(expDeg8[j]) < EPS2) {
                Assert.assertEquals(expDeg8[j], pert8[j] - pert7[j], EPS2);
            } else {
                Assert.assertEquals(0, MathLib.abs((expDeg8[j] - pert8[j] + pert7[j]) / expDeg8[j]), EPS2);
            }
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_THIRD_BODY_ATTRACTION}
     * 
     * @testedMethod {@link StelaThirdBodyAttraction#computePartialDerivatives(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of 2nd to 4th degree partial derivatives
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output 2nd to 4th degree third body partial derivatives
     * 
     * @testPassCriteria references from Stela (same epsilon value)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputePartialDerivatives() throws PatriusException, IOException, ParseException {

        /** Epsilon */
        final double EPS = 1e-14;

        Report.printMethodHeader("testComputePartialDerivatives", "Partial derivatives computation", "STELA", EPS,
            ComparisonType.RELATIVE);

        // Context:
        /** Equinoctial parameters: */
        final double a = 2.422800000000000000E+07;
        final double ex = 4.592777886100000151E-01;
        final double ey = -5.665453952400000270E-01;
        final double ix = 5.490687833999999962E-02;
        final double iy = -6.770144794999999327E-02;
        final double ksi = 5.393598442489803801E+00;

        final double mu = 398600441449820.000;

        final Frame frame = FramesFactory.getGCRF();

        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(a, ex, ey, ix, iy, ksi, frame, date, mu);

        // The user celestial body:
        final UserEphemerisForTest2 ephemeris = new UserEphemerisForTest2();
        final EphemerisBodyForTest body = new EphemerisBodyForTest();
        final UserCelestialBody sun = new UserCelestialBody(body, ephemeris);

        // Compute the partial derivatives for degree 0:
        final StelaThirdBodyAttraction thirdBody0 = new StelaThirdBodyAttraction(sun, 2, 2, 0);
        final double[][] der0 = thirdBody0.computePartialDerivatives(orbit);

        // Compute the partial derivatives for degree 1 (throws an exception):
        boolean rez = false;
        try {
            new StelaThirdBodyAttraction(sun, 2, 1, 2);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // Compute the partial derivatives for degree 2:
        final StelaThirdBodyAttraction thirdBody2 = new StelaThirdBodyAttraction(sun, 2, 2, 2);
        final double[][] der2 = thirdBody2.computePartialDerivatives(orbit);

        // Compute the partial derivatives for degree 3:
        final StelaThirdBodyAttraction thirdBody3 = new StelaThirdBodyAttraction(sun, 2, 2, 3);
        final double[][] der3 = thirdBody3.computePartialDerivatives(orbit);

        // Compute the partial derivatives for degree 3:
        final StelaThirdBodyAttraction thirdBody4 = new StelaThirdBodyAttraction(sun, 2, 2, 4);
        final double[][] der4 = thirdBody4.computePartialDerivatives(orbit);

        // Expected results
        /** Expected degree 0 third body potential second derivatives. */
        final double[][] expDeg0SecDeriv = {
            { 0., 0., 0., 0., 0., 0. },
            { 0., 0., 0., 0., 0., 0. },
            { 0., 0., 0., 0., 0., 0. },
            { 0., 0., 0., 0., 0., 0. },
            { 0., 0., 0., 0., 0., 0. },
            { 0., 0., 0., 0., 0., 0. } };
        /** Expected degree 2 third body potential second derivatives. */
        final double[][] expDeg2SecDeriv = {
            { 3.321843893634189544E-15, 0.000000000000000000E+00, 3.020951171463672157E-06,
                3.797398464804238080E-06, 1.550015457545388199E-07, 4.172892357226398367E-07 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { 3.020951171463672157E-06, 0.000000000000000000E+00, 1.008790463720509507E+02,
                1.718432966806842188E+01, 5.201685032577778145E+00, 1.130232348457633051E+01 },
            { 3.797398464804238080E-06, 0.000000000000000000E+00, 1.718432966806842188E+01,
                -6.726610858393613057E+01, -4.984168803684077886E+00, 1.589064424858226587E+01 },
            { 1.550015457545388199E-07, 0.000000000000000000E+00, 5.201685032577778145E+00,
                -4.984168803684077886E+00, 1.599193526448335945E+01, -6.459936886116486221E+01 },
            { 4.172892357226398367E-07, 0.000000000000000000E+00, 1.130232348457633051E+01,
                1.589064424858226587E+01, -6.459936886116486221E+01, -1.895812961868269610E+02 } };
        /** Expected degree 3 third body potential second derivatives. */
        final double[][] expDeg3SecDeriv = {
            { -2.040584786820250422E-17, 0.000000000000000000E+00, -2.891922883504572335E-10,
                1.652497693101231099E-09, 1.457000100120809899E-11, 1.876832433287666402E-10 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -2.891922883504572335E-10, 0.000000000000000000E+00, 2.279401550728978390E-02,
                3.833504580141275375E-02, 2.097635664002659751E-03, 4.055278688273902116E-03 },
            { 1.652497693101231099E-09, 0.000000000000000000E+00, 3.833504580141275375E-02,
                -1.486067247190187707E-02, 4.209012798964533461E-04, 5.442198053171100856E-03 },
            { 1.457000100120809899E-11, 0.000000000000000000E+00, 2.097635664002659751E-03,
                4.209012798964533461E-04, 1.689259881936658715E-03, -5.397309326773623225E-03 },
            { 1.876832433287666402E-10, 0.000000000000000000E+00, 4.055278688273902116E-03,
                5.442198053171100856E-03, -5.397309326773623225E-03, -3.973444090996888778E-02 } };
        /** Expected degree 4 third body potential second derivatives. */
        final double[][] expDeg4SecDeriv = {
            { -8.509431668907569174E-21, 0.000000000000000000E+00, -4.567033038660687592E-13,
                2.097368528184632419E-13, -1.357637405982728859E-14, 4.048347647137248102E-14 },
            { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
                0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00 },
            { -4.567033038660687592E-13, 0.000000000000000000E+00, -5.400137464024824556E-06,
                1.366988176603739260E-05, 1.646930331094485070E-07, 8.924438929268040743E-07 },
            { 2.097368528184632419E-13, 0.000000000000000000E+00, 1.366988176603739260E-05,
                6.986219543197026192E-06, 5.957654164462807902E-07, 1.844261709573003155E-06 },
            { -1.357637405982728859E-14, 0.000000000000000000E+00, 1.646930331094485070E-07,
                5.957654164462807902E-07, -4.415350503523941996E-07, 2.316829423343469424E-06 },
            { 4.048347647137248102E-14, 0.000000000000000000E+00, 8.924438929268040743E-07,
                1.844261709573003155E-06, 2.316829423343469424E-06, -2.428651000086135475E-06 } };

        for (int i = 0; i < der0.length; i++) {
            // Degree 0:
            for (int j = 0; j < der0[0].length; j++) {
                Assert.assertEquals(expDeg0SecDeriv[i][j], der0[i][j], EPS);
            }
        }

        for (int i = 0; i < der2.length; i++) {
            // Degree 2:
            for (int j = 0; j < der2[0].length; j++) {
                if (MathLib.abs(expDeg2SecDeriv[i][j]) < EPS) {
                    Assert.assertEquals(expDeg2SecDeriv[i][j], der2[i][j], EPS);
                } else {
                    Assert.assertEquals(0., MathLib.abs((expDeg2SecDeriv[i][j] - der2[i][j])) / expDeg2SecDeriv[i][j],
                        EPS);
                }
            }
        }
        for (int i = 0; i < der3.length; i++) {
            // Degree 3:
            for (int j = 0; j < der3[0].length; j++) {
                if (MathLib.abs(expDeg3SecDeriv[i][j]) < EPS) {
                    Assert.assertEquals(expDeg3SecDeriv[i][j], der3[i][j] - der2[i][j], EPS);
                } else {
                    Assert.assertEquals(0., MathLib.abs((expDeg3SecDeriv[i][j] - der3[i][j] + der2[i][j]))
                        / expDeg3SecDeriv[i][j], EPS);
                }
            }
        }
        for (int i = 0; i < der4.length; i++) {
            // Degree 4:
            for (int j = 0; j < der4[0].length; j++) {
                if (MathLib.abs(expDeg4SecDeriv[i][j]) < EPS) {
                    Assert.assertEquals(expDeg4SecDeriv[i][j], der4[i][j] - der3[i][j], EPS);
                } else {
                    Assert.assertEquals(0., MathLib.abs((expDeg4SecDeriv[i][j] - der4[i][j] + der3[i][j]))
                        / expDeg4SecDeriv[i][j], EPS);
                }
            }
        }

        Report.printToReport("Partial derivatives (degree 2)", expDeg2SecDeriv, der2);
        Report.printToReport("Partial derivatives (degree 3)", expDeg3SecDeriv, subtract(der3, der2));
        Report.printToReport("Partial derivatives (degree 4)", expDeg4SecDeriv, subtract(der4, der3));
    }

    /**
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_THIRD_BODY_ATTRACTION}
     * 
     * @testedMethod {@link StelaThirdBodyAttraction#computeShortPeriods(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of third body short periods
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output third body short periods
     * 
     * @testPassCriteria references from Stela (same epsilon value)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeShortPeriods() throws PatriusException {

        final double EPS = 1.E-14;

        Report.printMethodHeader("testComputeShortPeriods", "Short periods computation", "STELA", EPS,
            ComparisonType.RELATIVE);

        // Context
        /** Equinoctial parameters: */
        final double aIn = 2.444776345040522E+7;
        final double ksi = 0.23756137270231648;
        final double exIn = 0.6732807217051231;
        final double eyIn = 0.27729700918224887;
        final double ixIn = 0.00074176167540498;
        final double iyIn = 0.00033579986381688;

        final double mu = 398600441449820.000;

        final StelaEquinoctialOrbit orbit =
            new StelaEquinoctialOrbit(aIn, exIn, eyIn, ixIn, iyIn, ksi, frame, date, mu);

        // The user celestial body:
        final UserEphemerisForTest3 ephemeris = new UserEphemerisForTest3();
        final EphemerisBodyForTest body = new EphemerisBodyForTest();
        final UserCelestialBody sun = new UserCelestialBody(body, ephemeris);

        final double[] expectedDP0 = { 0., 0., 0., 0., 0., 0. };

        final double[] expectedDP = { -0.007046168608287823, 128857.52321269333, -6682.904199073357,
            -110660.13004211312, -133074.9200725073, 60159.211909443016 };

        // Compute the partial derivatives for degree 0:
        final StelaThirdBodyAttraction thirdBody0 = new StelaThirdBodyAttraction(sun, 2, 0, 2);
        final double[] actualDP0 = thirdBody0.computeShortPeriods(orbit);
        for (int i = 0; i < expectedDP0.length; i++) {
            Assert.assertEquals(expectedDP0[i], actualDP0[i], EPS);
        }

        // Compute the partial derivatives for degree 2:
        final StelaThirdBodyAttraction thirdBody = new StelaThirdBodyAttraction(sun, 2, 2, 2);
        final double[] actualDP = thirdBody.computeShortPeriods(orbit);
        for (int i = 0; i < expectedDP.length; i++) {
            if (MathLib.abs(expectedDP[i]) < EPS) {
                Assert.assertEquals(expectedDP[i], actualDP[i], EPS);
            } else {
                Assert.assertEquals(0., MathLib.abs((expectedDP[i] - actualDP[i]) / expectedDP[i]), EPS);
            }
        }

        Report.printToReport("Short periods", expectedDP, actualDP);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_THIRD_BODY_ATTRACTION}
     * 
     * @testedMethod {@link StelaThirdBodyAttraction#StelaThirdBodyAttraction(CelestialBody, int, int, int)}
     * 
     * @description tests the exceptions at the creation of the third body force model
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output exceptions
     * 
     * @testPassCriteria the exceptions are thrown when expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testException() throws PatriusException {

        // The user celestial body:
        final CelestialBody sun = new MeeusSun(MODEL.STELA);

        boolean rez = false;
        // short periods, degree 8:
        try {
            new StelaThirdBodyAttraction(sun, 3, 2, 8);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        // partial derivatives, degree 3:
        try {
            new StelaThirdBodyAttraction(sun, 2, 3, 2);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    /**
     * setUp.
     * 
     * @throws PatriusException
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        final DateComponents datec = new DateComponents(2011, 04, 01);
        final TimeComponents timec = new TimeComponents(0, 0, 0.0);
        date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());
        frame = FramesFactory.getCIRF();
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
    private static double[] subtract(final double[] v1, final double[] v2) {
        final double[] res = new double[v1.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = v1[i] - v2[i];
        }
        return res;
    }

    /**
     * Add two vectors.
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @return added vectors
     */
    private static double[] add(final double[] v1, final double[] v2) {
        final double[] res = new double[v1.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = v1[i] + v2[i];
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
    private static double[][] subtract(final double[][] v1, final double[][] v2) {
        final double[][] res = new double[v1.length][v1[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                res[i][j] = v1[i][j] - v2[i][j];
            }
        }
        return res;
    }

    /**
     * Class containing an ephemeris for the third body, at the selected date, for test purposes
     */
    private class UserEphemerisForTest1 implements IUserEphemeris {

        @Override
        public Map<AbsoluteDate, PVCoordinates> getEphemeris() {
            final Map<AbsoluteDate, PVCoordinates> map = new TreeMap<>();
            /** Third Body direction cosine. */
            final double xp = 0.981874618634436;
            /** Third Body direction cosine. */
            final double yp = 0.1738937828349084;
            /** Third Body direction cosine. */
            final double zp = 0.07538690584475846;
            /** Third Body distance. */
            final double rp = 1.4946021369005167E+11;
            final PVCoordinates pvs = new PVCoordinates(new Vector3D(xp * rp, yp * rp, zp * rp), new Vector3D(0., 0.,
                0.));
            // One date is enough:
            map.put(date, pvs);
            return map;
        }

        @Override
        public Frame getReferenceFrame() {
            return frame;
        }
    }

    /**
     * Class containing an ephemeris for the third body, at the selected date, for test purposes
     */
    private class UserEphemerisForTest2 implements IUserEphemeris {

        @Override
        public Map<AbsoluteDate, PVCoordinates> getEphemeris() {
            final Map<AbsoluteDate, PVCoordinates> map = new TreeMap<>();
            /** Third Body direction cosine. */
            final double xp = -9.936502885512082939E-01;
            /** Third Body direction cosine. */
            final double yp = -1.032392149731359426E-01;
            /** Third Body direction cosine. */
            final double zp = -4.472995141771566457E-02;
            /** Third Body distance. */
            final double rp = 1.499176766493516541E+11;
            final PVCoordinates pvs = new PVCoordinates(new Vector3D(xp * rp, yp * rp, zp * rp), new Vector3D(0., 0.,
                0.));
            // One date is enough:
            map.put(date, pvs);
            return map;
        }

        @Override
        public Frame getReferenceFrame() {
            return frame;
        }
    }

    /**
     * Class containing an ephemeris for the third body, at the selected date, for test purposes
     */
    private class UserEphemerisForTest3 implements IUserEphemeris {

        @Override
        public Map<AbsoluteDate, PVCoordinates> getEphemeris() {
            final Map<AbsoluteDate, PVCoordinates> map = new TreeMap<>();
            /** Third Body direction cosine. */
            final double xp = -0.6004931649183609;
            /** Third Body direction cosine. */
            final double yp = -0.7336373199120156;
            /** Third Body direction cosine. */
            final double zp = -0.31809470558097175;
            /** Third Body distance. */
            final double rp = 1.4795463294220422E+11;

            final PVCoordinates pvs = new PVCoordinates(new Vector3D(xp * rp, yp * rp, zp * rp), new Vector3D(0., 0.,
                0.));
            // One date is enough:
            map.put(date, pvs);
            return map;
        }

        @Override
        public Frame getReferenceFrame() {
            return frame;
        }
    }

    /**
     * Class containing the third body information, for test purposes
     */
    private class EphemerisBodyForTest implements IEphemerisBody {

        @Override
        public IAUPole getIAUPole() {
            return null;
        }

        @Override
        public double getGM() {
            return 1.32712440018E20;
        }

        @Override
        public String name() {
            return null;
        }
    }
}
