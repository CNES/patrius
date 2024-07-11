/* $Id: NRLMSISE00.java 17582 2017-05-10 12:58:16Z bignon $
 * =============================================================
 * Copyright (c) CNES 2010
 * This software is part of STELA, a CNES tool for long term
 * orbit propagation. This source file is licensed as described
 * in the file LICENCE which is part of this distribution
 * =============================================================
 */
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
 * @history Created 25/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.5:FA:FA-2364:27/05/2020:Problèmes rencontres dans le modèle MSIS00
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:130:08/10/2013:MSIS2000 model update
 * VERSION::FA:345:03/11/2014:add setAp coverage test
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author Vincent Ruch, Rami Houdroge
 * @since 1.2
 */
public class NRLMSIS00Test {

    /** atmospherice model. */
    NRLMSISE00 atmosModel = new NRLMSISE00();

    /** NRLMSISE00 Outputs. */
    Output[] output = new Output[19];

    /** NRLMSISE00 Inputs. */
    Input[] input = new Input[19];

    /** Doubles comparison epsilon */
    public static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle NRLMSIS2000 unit test
         * 
         * @featureDescription validate the underlying low-level getDensity method
         * 
         * @coveredRequirements DV-MOD_260
         */
        MSIS2000_MODEL
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(NRLMSIS00Test.class.getSimpleName(), "NRLMSIS-00 atmosphere");
    }

    /**
     * @throws PatriusException
     *         couldnt load UTC TAI history
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_MODEL}
     * 
     * @testedMethod {@link NRLMSISE00#gtd7d(Input, Flags, Output)}
     * 
     * @description the test computes the density for a number of predifined locations
     * 
     * @input Input data
     * 
     * @output density
     * 
     * @testPassCriteria if the relative difference between the expected and computed density is under the threshold,
     *                   the test passes
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @comments the threshold is set to 10<sup>-14</sup> for relative comparisons
     */
    @Test
    public void testGtd7() throws PatriusException {

        Report.printMethodHeader("testGtd7", "Density computation", "Unknown", EPS, ComparisonType.RELATIVE);

        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, "src//test//resources//regular-dataCNES");

        // *** DECLARATIONS *** //
        for (int k = 0; k < 19; k++) {
            this.output[k] = new Output();
            this.input[k] = new Input();
        }
        final Flags flags = new Flags();

        // *** INITIALIZATIONS *** //
        // input values
        final ApCoef aph = new ApCoef(new double[] { 100, 100, 10, 10, 10, 10, 10 });
        // setters of Ap coefs test
        aph.setAp(new double[] { 50, 100, 100, 100, 100, 100, 100 });
        aph.setAp(1, 100);

        flags.setSwitches(0, 0);
        for (int i = 1; i < 24; i++) {
            flags.setSwitches(i, 1);
        }
        for (int i = 0; i < 18; i++) {
            this.input[i].setDoy(172);
            this.input[i].setSec(29000);
            this.input[i].setAlt(400);
            this.input[i].setgLat(60);
            this.input[i].setgLong(-70);
            this.input[i].setLst(16);
            this.input[i].setF107A(150);
            this.input[i].setF107(150);
            this.input[i].setAp(4);
        }
        this.input[1].setDoy(81);
        this.input[2].setSec(75000);
        this.input[2].setAlt(Double.valueOf(1000));
        this.input[3].setAlt(Double.valueOf(100));
        this.input[10].setAlt(Double.valueOf(0));
        this.input[11].setAlt(10);
        this.input[12].setAlt(30);
        this.input[13].setAlt(50);
        this.input[14].setAlt(70);
        this.input[16].setAlt(100);
        this.input[4].setgLat(0);
        this.input[5].setgLong(0);
        this.input[6].setLst(4);
        this.input[7].setF107A(70);
        this.input[8].setF107(180);
        this.input[9].setAp(40);
        this.input[15].setApA(aph);
        this.input[16].setApA(aph);
        // CNES DATA
        this.input[17].setDoy(210);
        this.input[17].setSec(0);
        this.input[17].setAlt(577.7379799227957);
        this.input[17].setgLat(0);
        this.input[17].setgLong(-29.263328150215145);
        this.input[17].setLst(21.940587169372446);
        this.input[17].setF107A(140);
        this.input[17].setF107(140);
        final double[] inputAP = { 15, 15, 15, 15, 15, 15, 15 };
        this.input[17].setApA(new ApCoef(inputAP));

        // *** TEST *** //
        // evaluate 0 to 14
        for (int i = 0; i < 15; i++) {
            this.atmosModel.gtd7d(this.input[i], flags, this.output[i]);
        }
        // evaluate 15 to 17
        flags.setSwitches(9, -1);
        for (int i = 15; i < 18; i++) {
            this.atmosModel.gtd7d(this.input[i], flags, this.output[i]);
        }

        // *** COMPARISON *** //
        // Effective Results
        final double[][] dOutput = new double[18][9];

        for (int i = 0; i < 18; i++) {
            dOutput[i] = this.output[i].getD();
        }
        // Expected Results
        final double[] dExpected = { 4.07542188218729E-15, 5.00203105649744E-15, 3.38741096436555E-18,
            3.58442737851468E-10, 4.80966386169162E-15, 4.35657418238857E-15, 2.47135906183262E-15,
            1.57213094286178E-15, 4.56512849903136E-15, 4.97528788286201E-15, 1.26106560865974E-03,
            4.05913967149238E-04, 1.95082221339858E-05, 1.29470892702538E-06, 1.14766742136314E-07,
            5.88267347514411E-15, 2.91430418549808E-10, 1.03450531011828E-16 };

        // check that we have the same results
        for (int i = 0; i < 18; i++) {
            Assert.assertEquals(0, (dOutput[i][5] - dExpected[i]) / dExpected[i], EPS);
            Report.printToReport("Density (case " + i + ")", dExpected[i], dOutput[i][5]);
        }

        final double[] dIn = { 0., 0., 0., 0., 0., 0., 0., 0., 0. };
        for (int i = 0; i < dIn.length; i++) {
            this.output[1].setD(i, dIn[i]);
        }

        for (int i = 0; i < 18; i++) {
            dOutput[i] = this.output[i].getD();
        }
        final double[] dExpected1 = { 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1. };

        // check that we have the good results
        for (int i = 0; i < 18; i++) {
            Assert.assertEquals(-1., (dOutput[i][5] - dExpected1[i]) / dExpected1[i], 1.E-2);
        }

        final Double[] c = this.output[1].getT();
        Assert.assertEquals(1166.7544634585938, c[0], EPS);
        // Assert.assertEquals(4000., c[1], 1.E-2);
        // c[1].setValue(1000.);
        // output[1].setT(c);
        // c = output[1].getT();
        // Assert.assertEquals(1166.7544634585938, c[0], EPS);
        // Assert.assertEquals(1000., c[1], 1.E-2);

        // ///////////// Additional test ////////////////////////

        final double jdCNES = 22369.9392107806;
        final double latitude = 0.10476823210721935;
        final double longitude = -1.8781027116334768;
        final double altitude = 220013.24495616126;
        final double tLoc = 15.294174791639533;

        // input[18] = new Input(0, 90, (jdCNES - (int) jdCNES) * 86400, altitude / 1000, FastMath.toDegrees(latitude),
        // FastMath.toDegrees(longitude), tLoc, 140, 140, 0, new ApCoef(
        // new double[] { 15, 15, 15, 15, 15, 15, 15 }));
        this.input[18].setDoy(90);
        this.input[18].setSec((jdCNES - (int) jdCNES) * 86400);
        this.input[18].setAlt(altitude / 1000);
        this.input[18].setgLat(MathLib.toDegrees(latitude));
        this.input[18].setgLong(MathLib.toDegrees(longitude));
        this.input[18].setLst(tLoc);
        this.input[18].setF107(140);
        this.input[18].setF107A(140);
        this.input[18].setAp(0);
        this.input[18].setApA(new ApCoef(new double[] { 15, 15, 15, 15, 15, 15, 15 }));

        this.atmosModel.gtd7d(this.input[18], flags, this.output[18]);
        final double density = this.output[18].getD(5) * 1000;

        // ComparisonTypes.RELATIVE);
        final double ref = 1.9308075763345875E-10;

        Assert.assertEquals(0, MathLib.abs(ref - density) / ref, EPS);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_MODEL}
     * 
     * @testedMethod {@link NRLMSISE00#gtd7d(Input, Flags, Output)}
     * 
     * @description test of several methods to compete their coverage
     * 
     * @input Input data
     * 
     * @output doubles or exceptions
     * 
     * @testPassCriteria if the outputs and exceptions are the ones expected.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @comments the threshold is set to 10<sup>-14</sup> for relative comparisons
     */
    @Test
    public void testCover() {

        // AP COEF FAIL WRONG CREATION
        try {
            final double[] apIn = { 1., 1., 1., 1., 1., 1., 1., 1., 1. };
            new ApCoef(apIn);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // Input class coverage
        final double jdCNES = 22369.9392107806;
        final double latitude = 0.10476823210721935;
        final double longitude = -1.8781027116334768;
        final double altitude = 220013.24495616126;
        final double tLoc = 15.294174791639533;

        // final Input input2 = new Input(0, 90, (jdCNES - (int) jdCNES)
        // * 86400, altitude / 1000, FastMath.toDegrees(latitude),
        // FastMath.toDegrees(longitude), tLoc, 140, 140, 0, new ApCoef(
        // new double[] { 15, 15, 15, 15, 15, 15, 15 }));
        final Input input2 = new Input();
        input2.setDoy(90);
        input2.setSec((jdCNES - (int) jdCNES) * 86400);
        input2.setAlt(altitude / 1000);
        input2.setgLat(MathLib.toDegrees(latitude));
        input2.setgLong(MathLib.toDegrees(longitude));
        input2.setLst(tLoc);
        input2.setF107(140);
        input2.setF107A(140);
        input2.setAp(0);
        input2.setApA(new ApCoef(new double[] { 15, 15, 15, 15, 15, 15, 15 }));

        // NRLMSISE00Common class coverage

        final double expR = this.atmosModel.ccor(1000., 15., 1., 2000.);
        // final double expR = this.atmosModel.ccor(1000., 15., 1., 2000.);
        Assert.assertEquals(MathLib.exp(15.), expR, EPS);

        final double res1 = this.atmosModel.ccor2(1000., 15., 1., 2000., -1);
        // final double res1 = this.atmosModel.ccor2(1000., 15., 1., 2000., -1);
        Assert.assertEquals(1., res1, EPS);
        final double res2 = this.atmosModel.ccor2(1000., 15., 1., 2000., 1);
        // final double res2 = this.atmosModel.ccor2(1000., 15., 1., 2000., 1);
        Assert.assertEquals(MathLib.exp(15.), res2, EPS);

        final double res3 = this.atmosModel.dnet(-1., 0., 1., 1., 1.);
        // final double res3 = this.atmosModel.dnet(-1., 0., 1., 1., 1.);
        Assert.assertEquals(-1, res3, EPS);
        final double res4 = this.atmosModel.dnet(0., -2., 1., 1., 1.);
        // final double res4 = this.atmosModel.dnet(0., -2., 1., 1., 1.);
        Assert.assertEquals(-2, res4, EPS);
        final double res5 = this.atmosModel.dnet(0., 0., 1., 1., 1.);
        // final double res5 = this.atmosModel.dnet(0., 0., 1., 1., 1.);
        Assert.assertEquals(1., res5, EPS);
        final double res6 = this.atmosModel.dnet(1., 100., 1., 1., 1.);
        //final double res6 = this.atmosModel.dnet(1., 100., 1., 1., 1.);
        Assert.assertEquals(100., res6, EPS);

        final double[] xa = { 1. };
        final double[] ya = { 1. };
        final double[] y2a = { 1. };
        this.atmosModel.splint(xa, ya, y2a, 1, 1.);

        final double[] x = { 1., 1., 1. };
        final double[] y = { 1., 1., 1. };
        final double[] y2 = { 1., 1., 1. };
        this.atmosModel.spline(x, y, 3, 1.e30, 1.e30, y2);
        // this.atmosModel.spline(x, y, 3, 1.e30, 1.e30, y2);

        final double alt = 7000.;
        final double d0 = 1.;
        final double xm = 1.;
        final Double tz = 0.;
        final int mn3 = 3;
        final double[] zn3 = { 7., 2., 9. };
        final double[] tn3 = { 1., 4., 3. };
        final double[] tgn3 = { 4., 7., 3. };
        final int mn2 = 3;
        final double[] zn2 = { 2., 7., 3. };
        final double[] tn2 = { 9., 8., 5. };
        final double[] tgn2 = { 5., 1., 4., 3. };
        double[] res = this.atmosModel.densm(alt, d0, 0., tz, mn3, zn3, tn3, tgn3, mn2, zn2, tn2, tgn2);
        //double res = this.atmosModel.densm(alt, d0, 0., tz, mn3, zn3, tn3, tgn3, mn2, zn2, tn2, tgn2);
        Assert.assertEquals(0., res[0], EPS);
        res = this.atmosModel.densm(alt, d0, xm, tz, mn3, zn3, tn3, tgn3, mn2, zn2, tn2, tgn2);
        //res = this.atmosModel.densm(alt, d0, xm, tz, mn3, zn3, tn3, tgn3, mn2, zn2, tn2, tgn2);
        Assert.assertEquals(1., res[0], EPS);
        zn2[0] = 7500.;
        zn3[0] = 7500.;
        final Flags flags = new Flags();
        this.atmosModel.gtd7(input2, flags, new Output());
        //this.atmosModel.gtd7(input2, flags, new Output());
        res = this.atmosModel.densm(alt, d0, -xm, tz, mn3, zn3, tn3, tgn3, mn2, zn2, tn2, tgn2);
        //res = this.atmosModel.densm(alt, d0, -xm, tz, mn3, zn3, tn3, tgn3, mn2, zn2, tn2, tgn2);
        Assert.assertEquals(1.9626880965900512E-38, res[0], EPS);
        final Double[] th = { 0., 0. };
        final double res7 = this.atmosModel.densu(alt, 2., 3., 4., -d0, -xm, th, 4., 5., mn3, zn3, tn3, tgn3);
        //res = this.atmosModel.densu(alt, 2., 3., 4., -d0, -xm, th, 4., 5., mn3, zn3, tn3, tgn3);
        Assert.assertEquals(1.8856631396960566E-20, res7, EPS);

        // Cover the Flag methods:
        flags.setSwitches(new int[] { 1, 0 });
        Assert.assertEquals(1, flags.getSwitches()[0]);
        Assert.assertEquals(0, flags.getSwitches()[1]);

        // Cover the Input methods:
        final Input inputs = new Input();
        inputs.setApA(new double[] { 0.0, 0.1 });
        Assert.assertEquals(0, inputs.getApA().getAp()[0], 0.0);
    }

}
