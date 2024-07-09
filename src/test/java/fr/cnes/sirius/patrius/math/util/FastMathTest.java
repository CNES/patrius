/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
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
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:FA:FA-2447:27/05/2020:Mathlib.divide() incomplète 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.dfp.Dfp;
import fr.cnes.sirius.patrius.math.dfp.DfpField;
import fr.cnes.sirius.patrius.math.dfp.DfpMath;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.framework.MathLibraryType;
import fr.cnes.sirius.patrius.math.random.MersenneTwister;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;

public class FastMathTest {

    private static final double MAX_ERROR_ULP = 0.51;
    private static final int NUMBER_OF_TRIALS = 1000;

    private DfpField field;
    private RandomGenerator generator;

    @Before
    public void setUp() {
        this.field = new DfpField(40);
        this.generator = new MersenneTwister(6176597458463500194l);

        // MathLib is used in all test but calls FastMath
        MathLib.setMathLibrary(MathLibraryType.FASTMATH);
    }

    @Test
    public void testMinMaxDouble() {
        final double[][] pairs = {
            { -50.0, 50.0 },
            { Double.POSITIVE_INFINITY, 1.0 },
            { Double.NEGATIVE_INFINITY, 1.0 },
            { Double.NaN, 1.0 },
            { Double.POSITIVE_INFINITY, 0.0 },
            { Double.NEGATIVE_INFINITY, 0.0 },
            { Double.NaN, 0.0 },
            { Double.NaN, Double.NEGATIVE_INFINITY },
            { Double.NaN, Double.POSITIVE_INFINITY },
            { Precision.SAFE_MIN, Precision.EPSILON }
        };
        for (final double[] pair : pairs) {
            try {
                Assert.assertEquals("min(" + pair[0] + ", " + pair[1] + ")",
                    Math.min(pair[0], pair[1]),
                    MathLib.min(pair[0], pair[1]),
                    Precision.EPSILON);
                Assert.assertEquals("min(" + pair[1] + ", " + pair[0] + ")",
                    Math.min(pair[1], pair[0]),
                    MathLib.min(pair[1], pair[0]),
                    Precision.EPSILON);
                Assert.assertEquals("max(" + pair[0] + ", " + pair[1] + ")",
                    Math.max(pair[0], pair[1]),
                    MathLib.max(pair[0], pair[1]),
                    Precision.EPSILON);
                Assert.assertEquals("max(" + pair[1] + ", " + pair[0] + ")",
                    Math.max(pair[1], pair[0]),
                    MathLib.max(pair[1], pair[0]),
                    Precision.EPSILON);
            } catch (final ArithmeticException e) {
                if (Double.isNaN(pair[0]) || Double.isNaN(pair[1])) {
                    Assert.assertTrue(true);
                } else {
                    Assert.fail();
                }
            }
        }
        Assert.assertTrue(Double.isNaN(FastMath.min(0, Double.NaN)));
        Assert.assertTrue(Double.isNaN(FastMath.max(0, Double.NaN)));
    }

    @Test
    public void testMinMaxFloat() {
        final float[][] pairs = {
            { -50.0f, 50.0f },
            { Float.POSITIVE_INFINITY, 1.0f },
            { Float.NEGATIVE_INFINITY, 1.0f },
            { Float.NaN, 1.0f },
            { Float.POSITIVE_INFINITY, 0.0f },
            { Float.NEGATIVE_INFINITY, 0.0f },
            { Float.NaN, 0.0f },
            { Float.NaN, Float.NEGATIVE_INFINITY },
            { Float.NaN, Float.POSITIVE_INFINITY }
        };
        for (final float[] pair : pairs) {
            try {
                Assert.assertEquals("min(" + pair[0] + ", " + pair[1] + ")",
                    Math.min(pair[0], pair[1]),
                    MathLib.min(pair[0], pair[1]),
                    Precision.EPSILON);
                Assert.assertEquals("min(" + pair[1] + ", " + pair[0] + ")",
                    Math.min(pair[1], pair[0]),
                    MathLib.min(pair[1], pair[0]),
                    Precision.EPSILON);
                Assert.assertEquals("max(" + pair[0] + ", " + pair[1] + ")",
                    Math.max(pair[0], pair[1]),
                    MathLib.max(pair[0], pair[1]),
                    Precision.EPSILON);
                Assert.assertEquals("max(" + pair[1] + ", " + pair[0] + ")",
                    Math.max(pair[1], pair[0]),
                    MathLib.max(pair[1], pair[0]),
                    Precision.EPSILON);
            } catch (final ArithmeticException e) {
                if (Double.isNaN(pair[0]) || Double.isNaN(pair[1])) {
                    Assert.assertTrue(true);
                } else {
                    Assert.fail();
                }
            }
        }
        Assert.assertTrue(Float.isNaN(FastMath.min(0, Float.NaN)));
        Assert.assertTrue(Float.isNaN(FastMath.max(0, Float.NaN)));
    }

    @Test
    public void testConstants() {
        Assert.assertEquals(Math.PI, FastMath.PI, 1.0e-20);
        Assert.assertEquals(Math.E, FastMath.E, 1.0e-20);
    }

    @Test
    public void testAtan2() {
        final double y1 = 1.2713504628280707e10;
        final double x1 = -5.674940885228782e-10;
        Assert.assertEquals(Math.atan2(y1, x1), MathLib.atan2(y1, x1), 2 * Precision.EPSILON);
        final double y2 = 0.0;
        final double x2 = Double.POSITIVE_INFINITY;
        Assert.assertEquals(Math.atan2(y2, x2), MathLib.atan2(y2, x2), Precision.SAFE_MIN);
    }

    @Test
    public void testHyperbolic() {
        double maxErr = 0;
        for (double x = -30; x < 30; x += 0.001) {
            final double tst = MathLib.sinh(x);
            final double ref = Math.sinh(x);
            maxErr = MathLib.max(maxErr, MathLib.abs(ref - tst) / MathLib.ulp(ref));
        }
        Assert.assertEquals(0, maxErr, 2);

        maxErr = 0;
        for (double x = -30; x < 30; x += 0.001) {
            final double tst = MathLib.cosh(x);
            final double ref = Math.cosh(x);
            maxErr = MathLib.max(maxErr, MathLib.abs(ref - tst) / MathLib.ulp(ref));
        }
        Assert.assertEquals(0, maxErr, 2);

        maxErr = 0;
        for (double x = -0.5; x < 0.5; x += 0.001) {
            final double tst = MathLib.tanh(x);
            final double ref = Math.tanh(x);
            maxErr = MathLib.max(maxErr, MathLib.abs(ref - tst) / MathLib.ulp(ref));
        }
        Assert.assertEquals(0, maxErr, 4);

    }

    @Test
    public void testMath904() {
        final double x = -1;
        final double y = (5 + 1e-15) * 1e15;
        Assert.assertEquals(Math.pow(x, y),
            MathLib.pow(x, y), 0);
        Assert.assertEquals(Math.pow(x, -y),
            MathLib.pow(x, -y), 0);
    }

    @Test
    public void testMath905LargePositive() {
        final double start = StrictMath.log(Double.MAX_VALUE);
        final double endT = StrictMath.sqrt(2) * StrictMath.sqrt(Double.MAX_VALUE);
        final double end = 2 * StrictMath.log(endT);

        double maxErr = 0;
        for (double x = start; x < end; x += 1e-3) {
            final double tst = MathLib.cosh(x);
            final double ref = Math.cosh(x);
            maxErr = MathLib.max(maxErr, MathLib.abs(ref - tst) / MathLib.ulp(ref));
        }
        Assert.assertEquals(0, maxErr, 3);

        for (double x = start; x < end; x += 1e-3) {
            final double tst = MathLib.sinh(x);
            final double ref = Math.sinh(x);
            maxErr = MathLib.max(maxErr, MathLib.abs(ref - tst) / MathLib.ulp(ref));
        }
        Assert.assertEquals(0, maxErr, 3);
    }

    @Test
    public void testMath905LargeNegative() {
        final double start = -StrictMath.log(Double.MAX_VALUE);
        final double endT = StrictMath.sqrt(2) * StrictMath.sqrt(Double.MAX_VALUE);
        final double end = -2 * StrictMath.log(endT);

        double maxErr = 0;
        for (double x = start; x > end; x -= 1e-3) {
            final double tst = MathLib.cosh(x);
            final double ref = Math.cosh(x);
            maxErr = MathLib.max(maxErr, MathLib.abs(ref - tst) / MathLib.ulp(ref));
        }
        Assert.assertEquals(0, maxErr, 3);

        for (double x = start; x > end; x -= 1e-3) {
            final double tst = MathLib.sinh(x);
            final double ref = Math.sinh(x);
            maxErr = MathLib.max(maxErr, MathLib.abs(ref - tst) / MathLib.ulp(ref));
        }
        Assert.assertEquals(0, maxErr, 3);

        try {
            FastMath.sqrt(Math.nextDown(0.));
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testHyperbolicInverses() {
        double maxErr = 0;
        for (double x = -30; x < 30; x += 0.01) {
            maxErr = MathLib.max(maxErr, MathLib.abs(x - MathLib.sinh(MathLib.asinh(x))) / (2 * MathLib.ulp(x)));
        }
        Assert.assertEquals(0, maxErr, 3);

        maxErr = 0;
        for (double x = 1; x < 30; x += 0.01) {
            maxErr = MathLib.max(maxErr, MathLib.abs(x - MathLib.cosh(MathLib.acosh(x))) / (2 * MathLib.ulp(x)));
        }
        Assert.assertEquals(0, maxErr, 2);

        maxErr = 0;
        for (double x = -1 + Precision.EPSILON; x < 1 - Precision.EPSILON; x += 0.0001) {
            maxErr = MathLib.max(maxErr, MathLib.abs(x - MathLib.tanh(MathLib.atanh(x))) / (2 * MathLib.ulp(x)));
        }
        Assert.assertEquals(0, maxErr, 2);

        try {
            FastMath.acosh(Math.nextDown(-1.));
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            FastMath.atanh(Math.nextDown(-1.));
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            FastMath.atanh(Math.nextUp(1.));
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testLogAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            final double x = Math.exp(this.generator.nextDouble() * 1416.0 - 708.0) * this.generator.nextDouble();
            // double x = generator.nextDouble()*2.0;
            final double tst = MathLib.log(x);
            final double ref = DfpMath.log(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0.0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double
                        .doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.log(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("log() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testLog10Accuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            final double x = Math.exp(this.generator.nextDouble() * 1416.0 - 708.0) * this.generator.nextDouble();
            // double x = generator.nextDouble()*2.0;
            final double tst = MathLib.log10(x);
            final double ref =
                DfpMath.log(this.field.newDfp(x)).divide(DfpMath.log(this.field.newDfp("10"))).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0.0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp = this.field.newDfp(tst)
                    .subtract(DfpMath.log(this.field.newDfp(x)).divide(DfpMath.log(this.field.newDfp("10"))))
                    .divide(this.field.newDfp(ulp)).toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("log10() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testLog1pAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            final double x = Math.exp(this.generator.nextDouble() * 10.0 - 5.0) * this.generator.nextDouble();
            // double x = generator.nextDouble()*2.0;
            final double tst = MathLib.log1p(x);
            final double ref = DfpMath.log(this.field.newDfp(x).add(this.field.getOne())).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0.0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.log(this.field.newDfp(x).add(this.field.getOne())))
                        .divide(this.field.newDfp(ulp)).toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("log1p() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testLog1pSpecialCases() {

        Assert.assertTrue("Logp of -1.0 should be -Inf", Double.isInfinite(MathLib.log1p(-1.0)));
        Assert.assertTrue("Logp of +Inf should be +Inf", Double.isInfinite(MathLib.log1p(Double.POSITIVE_INFINITY)));

    }

    @Test
    public void testLogSpecialCases() {

        Assert.assertTrue("Log of zero should be -Inf", Double.isInfinite(MathLib.log(0.0)));

        Assert.assertTrue("Log of -zero should be -Inf", Double.isInfinite(MathLib.log(-0.0)));

        try {
            MathLib.log(Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.log(-1.0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        Assert.assertEquals("Log of Double.MIN_VALUE should be -744.4400719213812", -744.4400719213812,
            MathLib.log(Double.MIN_VALUE), Precision.EPSILON);

        Assert.assertTrue("Log of infinity should be infinity",
            Double.isInfinite(MathLib.log(Double.POSITIVE_INFINITY)));
    }

    @Test
    public void testExpSpecialCases() {

        // Smallest value that will round up to Double.MIN_VALUE
        Assert.assertEquals(Double.MIN_VALUE, MathLib.exp(-745.1332191019411), Precision.EPSILON);

        Assert.assertEquals("exp(-745.1332191019412) should be 0.0", 0.0, MathLib.exp(-745.1332191019412),
            Precision.EPSILON);

        try {
            MathLib.exp(Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        Assert.assertTrue("exp of infinity should be infinity",
            Double.isInfinite(MathLib.exp(Double.POSITIVE_INFINITY)));

        Assert.assertEquals("exp of -infinity should be 0.0", 0.0, MathLib.exp(Double.NEGATIVE_INFINITY),
            Precision.EPSILON);

        Assert.assertEquals("exp(1) should be Math.E", Math.E, MathLib.exp(1.0), Precision.EPSILON);
    }

    @Test
    public void testPowSpecialCases() {

        Assert.assertEquals("pow(-1, 0) should be 1.0", 1.0, MathLib.pow(-1.0, 0.0), Precision.EPSILON);

        Assert.assertEquals("pow(-1, -0) should be 1.0", 1.0, MathLib.pow(-1.0, -0.0), Precision.EPSILON);

        Assert.assertEquals("pow(PI, 1.0) should be PI", FastMath.PI, MathLib.pow(FastMath.PI, 1.0), Precision.EPSILON);

        Assert.assertEquals("pow(-PI, 1.0) should be -PI", -FastMath.PI, MathLib.pow(-FastMath.PI, 1.0),
            Precision.EPSILON);

        try {
            MathLib.pow(Math.PI, Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.pow(Double.NaN, Math.PI);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        Assert.assertTrue("pow(2.0, Infinity) should be Infinity",
            Double.isInfinite(MathLib.pow(2.0, Double.POSITIVE_INFINITY)));

        Assert.assertTrue("pow(0.5, -Infinity) should be Infinity",
            Double.isInfinite(MathLib.pow(0.5, Double.NEGATIVE_INFINITY)));

        Assert.assertEquals("pow(0.5, Infinity) should be 0.0", 0.0, MathLib.pow(0.5, Double.POSITIVE_INFINITY),
            Precision.EPSILON);

        Assert.assertEquals("pow(2.0, -Infinity) should be 0.0", 0.0, MathLib.pow(2.0, Double.NEGATIVE_INFINITY),
            Precision.EPSILON);

        Assert.assertEquals("pow(0.0, 0.5) should be 0.0", 0.0, MathLib.pow(0.0, 0.5), Precision.EPSILON);

        Assert.assertEquals("pow(Infinity, -0.5) should be 0.0", 0.0, MathLib.pow(Double.POSITIVE_INFINITY, -0.5),
            Precision.EPSILON);

        Assert.assertTrue("pow(0.0, -0.5) should be Inf", Double.isInfinite(MathLib.pow(0.0, -0.5)));

        Assert.assertTrue("pow(Inf, 0.5) should be Inf", Double.isInfinite(MathLib.pow(Double.POSITIVE_INFINITY, 0.5)));

        Assert.assertTrue("pow(-0.0, -3.0) should be -Inf", Double.isInfinite(MathLib.pow(-0.0, -3.0)));

        Assert.assertTrue("pow(-Inf, -3.0) should be -Inf",
            Double.isInfinite(MathLib.pow(Double.NEGATIVE_INFINITY, 3.0)));

        Assert.assertTrue("pow(-0.0, -3.5) should be Inf", Double.isInfinite(MathLib.pow(-0.0, -3.5)));

        Assert.assertTrue("pow(Inf, 3.5) should be Inf", Double.isInfinite(MathLib.pow(Double.POSITIVE_INFINITY, 3.5)));

        Assert.assertEquals("pow(-2.0, 3.0) should be -8.0", -8.0, MathLib.pow(-2.0, 3.0), Precision.EPSILON);
        
        try {
            MathLib.pow(-2.0, 3.5);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        // Added tests for a 100% coverage

        try {
            MathLib.pow(Double.POSITIVE_INFINITY, Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.pow(1.0, Double.POSITIVE_INFINITY);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.pow(Double.NEGATIVE_INFINITY, Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        Assert.assertEquals("pow(-Inf, -1.0) should be 0.0", 0.0, MathLib.pow(Double.NEGATIVE_INFINITY, -1.0),
            Precision.EPSILON);

        Assert.assertEquals("pow(-Inf, -2.0) should be 0.0", 0.0, MathLib.pow(Double.NEGATIVE_INFINITY, -2.0),
            Precision.EPSILON);

        Assert.assertTrue("pow(-Inf, 1.0) should be -Inf",
            Double.isInfinite(MathLib.pow(Double.NEGATIVE_INFINITY, 1.0)));

        Assert.assertTrue("pow(-Inf, 2.0) should be +Inf",
            Double.isInfinite(MathLib.pow(Double.NEGATIVE_INFINITY, 2.0)));

        Assert.assertEquals("pow(1.0, -Inf) should be 1.0", 1.0, MathLib.pow(1.0, Double.NEGATIVE_INFINITY),
            Precision.EPSILON);
    }

    @Test
    public void testAtan2SpecialCases() {

        try {
            MathLib.atan2(Double.NaN, 0.0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.atan2(0.0, Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        Assert.assertEquals("atan2(0.0, 0.0) should be 0.0", 0.0, MathLib.atan2(0.0, 0.0), Precision.EPSILON);

        Assert.assertEquals("atan2(0.0, 0.001) should be 0.0", 0.0, MathLib.atan2(0.0, 0.001), Precision.EPSILON);

        Assert.assertEquals("atan2(0.1, +Inf) should be 0.0", 0.0, MathLib.atan2(0.1, Double.POSITIVE_INFINITY),
            Precision.EPSILON);

        Assert.assertEquals("atan2(-0.0, 0.0) should be -0.0", -0.0, MathLib.atan2(-0.0, 0.0), Precision.EPSILON);

        Assert.assertEquals("atan2(-0.0, 0.001) should be -0.0", -0.0, MathLib.atan2(-0.0, 0.001), Precision.EPSILON);

        Assert.assertEquals("atan2(-0.0, +Inf) should be -0.0", -0.0, MathLib.atan2(-0.1, Double.POSITIVE_INFINITY),
            Precision.EPSILON);

        Assert.assertEquals("atan2(0.0, -0.0) should be PI", FastMath.PI, MathLib.atan2(0.0, -0.0), Precision.EPSILON);

        Assert.assertEquals("atan2(0.1, -Inf) should be PI", FastMath.PI,
            MathLib.atan2(0.1, Double.NEGATIVE_INFINITY), Precision.EPSILON);

        Assert.assertEquals("atan2(-0.0, -0.0) should be -PI", -FastMath.PI, MathLib.atan2(-0.0, -0.0),
            Precision.EPSILON);

        Assert.assertEquals("atan2(0.1, -Inf) should be -PI", -FastMath.PI,
            MathLib.atan2(-0.1, Double.NEGATIVE_INFINITY), Precision.EPSILON);

        Assert.assertEquals("atan2(0.1, 0.0) should be PI/2", FastMath.PI / 2.0, MathLib.atan2(0.1, 0.0),
            Precision.EPSILON);

        Assert.assertEquals("atan2(0.1, -0.0) should be PI/2", FastMath.PI / 2.0, MathLib.atan2(0.1, -0.0),
            Precision.EPSILON);

        Assert.assertEquals("atan2(Inf, 0.1) should be PI/2", FastMath.PI / 2.0,
            MathLib.atan2(Double.POSITIVE_INFINITY, 0.1), Precision.EPSILON);

        Assert.assertEquals("atan2(Inf, -0.1) should be PI/2", FastMath.PI / 2.0,
            MathLib.atan2(Double.POSITIVE_INFINITY, -0.1), Precision.EPSILON);

        Assert.assertEquals("atan2(-0.1, 0.0) should be -PI/2", -FastMath.PI / 2.0, MathLib.atan2(-0.1, 0.0),
            Precision.EPSILON);

        Assert.assertEquals("atan2(-0.1, -0.0) should be -PI/2", -FastMath.PI / 2.0, MathLib.atan2(-0.1, -0.0),
            Precision.EPSILON);

        Assert.assertEquals("atan2(-Inf, 0.1) should be -PI/2", -FastMath.PI / 2.0,
            MathLib.atan2(Double.NEGATIVE_INFINITY, 0.1), Precision.EPSILON);

        Assert.assertEquals("atan2(-Inf, -0.1) should be -PI/2", -FastMath.PI / 2.0,
            MathLib.atan2(Double.NEGATIVE_INFINITY, -0.1), Precision.EPSILON);

        Assert.assertEquals("atan2(Inf, Inf) should be PI/4", FastMath.PI / 4.0,
            MathLib.atan2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
            Precision.EPSILON);

        Assert.assertEquals("atan2(Inf, -Inf) should be PI * 3/4", FastMath.PI * 3.0 / 4.0,
            MathLib.atan2(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), Precision.EPSILON);

        Assert.assertEquals("atan2(-Inf, Inf) should be -PI/4", -FastMath.PI / 4.0,
            MathLib.atan2(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
            Precision.EPSILON);

        Assert.assertEquals("atan2(-Inf, -Inf) should be -PI * 3/4", -FastMath.PI * 3.0 / 4.0,
            MathLib.atan2(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), Precision.EPSILON);
    }

    @Test
    public void testPowAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            final double x = (this.generator.nextDouble() * 2.0 + 0.25);
            final double y = (this.generator.nextDouble() * 1200.0 - 600.0) * this.generator.nextDouble();
            /*
             * double x = MathLib.floor(generator.nextDouble()*1024.0 - 512.0); double
             * y; if (x != 0) y = MathLib.floor(512.0 / MathLib.abs(x)); else
             * y = generator.nextDouble()*1200.0; y = y - y/2; x = MathLib.pow(2.0, x) *
             * generator.nextDouble(); y = y * generator.nextDouble();
             */

            // double x = generator.nextDouble()*2.0;
            final double tst = MathLib.pow(x, y);
            final double ref = DfpMath.pow(this.field.newDfp(x), this.field.newDfp(y)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double
                        .doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.pow(this.field.newDfp(x), this.field.newDfp(y)))
                        .divide(this.field.newDfp(ulp)).toDouble();
                // System.out.println(x + "\t" + y + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("pow() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testExpAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            /* double x = 1.0 + i/1024.0/2.0; */
            final double x = ((this.generator.nextDouble() * 1416.0) - 708.0) * this.generator.nextDouble();
            // double x = (generator.nextDouble() * 20.0) - 10.0;
            // double x = ((generator.nextDouble() * 2.0) - 1.0) * generator.nextDouble();
            /* double x = 3.0 / 512.0 * i - 3.0; */
            final double tst = MathLib.exp(x);
            final double ref = DfpMath.exp(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.exp(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("exp() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testSinAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            /* double x = 1.0 + i/1024.0/2.0; */
            // double x = ((generator.nextDouble() * 1416.0) - 708.0) * generator.nextDouble();
            final double x = ((this.generator.nextDouble() * Math.PI) - Math.PI / 2.0) *
                Math.pow(2, 21) * this.generator.nextDouble();
            // double x = (generator.nextDouble() * 20.0) - 10.0;
            // double x = ((generator.nextDouble() * 2.0) - 1.0) * generator.nextDouble();
            /* double x = 3.0 / 512.0 * i - 3.0; */
            final double tst = MathLib.sin(x);
            final double ref = DfpMath.sin(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.sin(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("sin() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testCosAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            /* double x = 1.0 + i/1024.0/2.0; */
            // double x = ((generator.nextDouble() * 1416.0) - 708.0) * generator.nextDouble();
            final double x = ((this.generator.nextDouble() * Math.PI) - Math.PI / 2.0) *
                Math.pow(2, 21) * this.generator.nextDouble();
            // double x = (generator.nextDouble() * 20.0) - 10.0;
            // double x = ((generator.nextDouble() * 2.0) - 1.0) * generator.nextDouble();
            /* double x = 3.0 / 512.0 * i - 3.0; */
            final double tst = MathLib.cos(x);
            final double ref = DfpMath.cos(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.cos(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("cos() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testTanAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            /* double x = 1.0 + i/1024.0/2.0; */
            // double x = ((generator.nextDouble() * 1416.0) - 708.0) * generator.nextDouble();
            final double x = ((this.generator.nextDouble() * Math.PI) - Math.PI / 2.0) *
                Math.pow(2, 12) * this.generator.nextDouble();
            // double x = (generator.nextDouble() * 20.0) - 10.0;
            // double x = ((generator.nextDouble() * 2.0) - 1.0) * generator.nextDouble();
            /* double x = 3.0 / 512.0 * i - 3.0; */
            final double tst = MathLib.tan(x);
            final double ref = DfpMath.tan(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.tan(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("tan() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testAtanAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            /* double x = 1.0 + i/1024.0/2.0; */
            // double x = ((generator.nextDouble() * 1416.0) - 708.0) * generator.nextDouble();
            // double x = ((generator.nextDouble() * Math.PI) - Math.PI/2.0) *
            // generator.nextDouble();
            final double x = ((this.generator.nextDouble() * 16.0) - 8.0) * this.generator.nextDouble();

            // double x = (generator.nextDouble() * 20.0) - 10.0;
            // double x = ((generator.nextDouble() * 2.0) - 1.0) * generator.nextDouble();
            /* double x = 3.0 / 512.0 * i - 3.0; */
            final double tst = MathLib.atan(x);
            final double ref = DfpMath.atan(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.atan(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("atan() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testAtan2Accuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            /* double x = 1.0 + i/1024.0/2.0; */
            // double x = ((generator.nextDouble() * 1416.0) - 708.0) * generator.nextDouble();
            final double x = this.generator.nextDouble() - 0.5;
            final double y = this.generator.nextDouble() - 0.5;
            // double x = (generator.nextDouble() * 20.0) - 10.0;
            // double x = ((generator.nextDouble() * 2.0) - 1.0) * generator.nextDouble();
            /* double x = 3.0 / 512.0 * i - 3.0; */
            final double tst = MathLib.atan2(y, x);
            Dfp refdfp = DfpMath.atan(this.field.newDfp(y)
                .divide(this.field.newDfp(x)));
            /* Make adjustments for sign */
            if (x < 0.0) {
                if (y > 0.0) {
                    refdfp = this.field.getPi().add(refdfp);
                } else {
                    refdfp = refdfp.subtract(this.field.getPi());
                }
            }

            final double ref = refdfp.toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double
                        .doubleToLongBits(ref) ^ 1)));
                final double errulp = this.field.newDfp(tst).subtract(refdfp).divide(this.field.newDfp(ulp)).toDouble();
                // System.out.println(x + "\t" + y + "\t" + tst + "\t" + ref + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("atan2() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testExpm1Accuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            /* double x = 1.0 + i/1024.0/2.0; */
            // double x = (generator.nextDouble() * 20.0) - 10.0;
            final double x = ((this.generator.nextDouble() * 16.0) - 8.0) * this.generator.nextDouble();
            /* double x = 3.0 / 512.0 * i - 3.0; */
            final double tst = MathLib.expm1(x);
            final double ref = DfpMath.exp(this.field.newDfp(x)).subtract(this.field.getOne()).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double
                        .doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.exp(this.field.newDfp(x)).subtract(this.field.getOne()))
                        .divide(this.field.newDfp(ulp)).toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("expm1() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testAsinAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < 10000; i++) {
            final double x = ((this.generator.nextDouble() * 2.0) - 1.0) * this.generator.nextDouble();

            final double tst = MathLib.asin(x);
            final double ref = DfpMath.asin(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref - Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.asin(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x+"\t"+tst+"\t"+ref+"\t"+err+"\t"+errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("asin() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testAcosAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < 10000; i++) {
            final double x = ((this.generator.nextDouble() * 2.0) - 1.0) * this.generator.nextDouble();

            final double tst = MathLib.acos(x);
            final double ref = DfpMath.acos(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref - Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.acos(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x+"\t"+tst+"\t"+ref+"\t"+err+"\t"+errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("acos() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    /**
     * Added tests for a 100% coverage of acos().
     */
    @Test
    public void testAcosSpecialCases() {

        try {
            MathLib.acos(Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.acos(-1.1);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.acos(1.1);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        Assert.assertEquals("acos(-1.0) should be PI", MathLib.acos(-1.0), FastMath.PI, Precision.EPSILON);

        Assert.assertEquals("acos(1.0) should be 0.0", MathLib.acos(1.0), 0.0, Precision.EPSILON);

        Assert.assertEquals("acos(0.0) should be PI/2", MathLib.acos(0.0), FastMath.PI / 2.0, Precision.EPSILON);
    }

    /**
     * Added tests for a 100% coverage of asin().
     */
    @Test
    public void testAsinSpecialCases() {

        try {
            MathLib.asin(Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.asin(1.1);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.asin(-1.1);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        Assert.assertEquals("asin(1.0) should be PI/2", MathLib.asin(1.0), FastMath.PI / 2.0, Precision.EPSILON);

        Assert.assertEquals("asin(-1.0) should be -PI/2", MathLib.asin(-1.0), -FastMath.PI / 2.0, Precision.EPSILON);

        Assert.assertEquals("asin(0.0) should be 0.0", MathLib.asin(0.0), 0.0, Precision.EPSILON);
    }

    /**
     * Tests exact operation methods.
     */
    @Test
    public void testExactOperations() {

    	// Standard cases
    	Assert.assertEquals(120, FastMath.multiplyExact(10, 12), 0.);
    	Assert.assertEquals(120, FastMath.multiplyExact((long) 10, (long) 12), 0.);
    	Assert.assertEquals(22, FastMath.addExact(10, 12), 0.);
    	Assert.assertEquals(22, FastMath.addExact((long) 10, (long) 12), 0.);
    	Assert.assertEquals(-2, FastMath.subtractExact(10, 12), 0.);
    	Assert.assertEquals(-2, FastMath.subtractExact((long) 10, (long) 12), 0.);
    	Assert.assertEquals(11, FastMath.incrementExact(10), 0.);
    	Assert.assertEquals(11, FastMath.incrementExact((long) 10), 0.);
    	Assert.assertEquals(9, FastMath.decrementExact(10), 0.);
    	Assert.assertEquals(9, FastMath.decrementExact((long) 10), 0.);
        Assert.assertEquals(10, FastMath.toIntExact((long) 10), 0.);

    	// Exception
        try {
        	FastMath.multiplyExact(Integer.MAX_VALUE, Integer.MAX_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.multiplyExact(Long.MAX_VALUE, Long.MAX_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.addExact(Integer.MAX_VALUE, Integer.MAX_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.addExact(Long.MAX_VALUE, Long.MAX_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.subtractExact(Integer.MIN_VALUE, 1);
            Assert.fail();
        } catch (final MathArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.subtractExact(Long.MIN_VALUE, 1);
            Assert.fail();
        } catch (final MathArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.incrementExact(Integer.MAX_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.incrementExact(Long.MAX_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.decrementExact(Integer.MIN_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.decrementExact(Long.MIN_VALUE);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        // Exception
        try {
            FastMath.toIntExact(12345678910L);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Tests exact operation methods.
     */
    @Test
    public void testFloor() {

    	// Standard cases
    	Assert.assertEquals(2, FastMath.floorDiv(12, 5), 0.);
    	Assert.assertEquals(2, FastMath.floorDiv((long) 12, (long) 5), 0.);
    	Assert.assertEquals(-3, FastMath.floorDiv(12, -5), 0.);
    	Assert.assertEquals(-3, FastMath.floorDiv((long) 12, (long) -5), 0.);
    	Assert.assertEquals(2, FastMath.floorMod(12, 5), 0.);
    	Assert.assertEquals(2, FastMath.floorMod((long) 12, (long) 5), 0.);
    	Assert.assertEquals(-3, FastMath.floorMod(12, -5), 0.);
    	Assert.assertEquals(-3, FastMath.floorMod((long) 12, (long) -5), 0.);
    	
    	// Exception
        try {
        	FastMath.floorDiv(1, 0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.floorDiv((long) 1, (long) 0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.floorMod(1, 0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
        	FastMath.floorMod((long) 1, (long) 0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }


    private Dfp cosh(final Dfp x) {
        return DfpMath.exp(x).add(DfpMath.exp(x.negate())).divide(2);
    }

    private Dfp sinh(final Dfp x) {
        return DfpMath.exp(x).subtract(DfpMath.exp(x.negate())).divide(2);
    }

    private Dfp tanh(final Dfp x) {
        return this.sinh(x).divide(this.cosh(x));
    }

    @Test
    public void testNaNInput() {

        if (Double.isNaN(FastMath.cosh(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.sinh(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.tanh(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.expm1(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        try {
            FastMath.log(Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        
        try {
            FastMath.log(Double.NaN, 1.);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            FastMath.log(1., Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        if (Double.isNaN(FastMath.atan2(Double.NaN, 1.))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.atan2(1., Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.asin(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.acos(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.floor(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
        
        if (Double.isNaN(FastMath.ceil(Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }

        if (Double.isNaN(FastMath.nextAfter(Double.NaN, 1.))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }

        if (Double.isNaN(FastMath.nextAfter(1., Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }

        if (Double.isNaN(FastMath.nextAfter(Float.NaN, 1.))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }

        if (Double.isNaN(FastMath.nextAfter(1f, Double.NaN))) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }

    }

    @Test
    public void testSinhAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < 10000; i++) {
            final double x = ((this.generator.nextDouble() * 16.0) - 8.0) * this.generator.nextDouble();

            final double tst = MathLib.sinh(x);
            final double ref = this.sinh(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref - Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(this.sinh(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x+"\t"+tst+"\t"+ref+"\t"+err+"\t"+errulp);
                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("sinh() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);

        Assert.assertEquals(0., FastMath.sinh(0.), 0.);
    }

    @Test
    public void testCoshAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < 10000; i++) {
            final double x = ((this.generator.nextDouble() * 16.0) - 8.0) * this.generator.nextDouble();

            final double tst = MathLib.cosh(x);
            final double ref = this.cosh(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref - Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(this.cosh(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x+"\t"+tst+"\t"+ref+"\t"+err+"\t"+errulp);
                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("cosh() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    @Test
    public void testTanhAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < 10000; i++) {
            final double x = ((this.generator.nextDouble() * 16.0) - 8.0) * this.generator.nextDouble();

            final double tst = MathLib.tanh(x);
            final double ref = this.tanh(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref - Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(this.tanh(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x+"\t"+tst+"\t"+ref+"\t"+err+"\t"+errulp);
                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("tanh() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);

        Assert.assertEquals(1., FastMath.tanh(Math.nextUp(20.)), 0.);
        Assert.assertEquals(-1., FastMath.tanh(Math.nextDown(-20.)), 0.);
        Assert.assertEquals(0., FastMath.tanh(0.), 0.);
    }

    @Test
    public void testCbrtAccuracy() {
        double maxerrulp = 0.0;

        for (int i = 0; i < 10000; i++) {
            final double x = ((this.generator.nextDouble() * 200.0) - 100.0) * this.generator.nextDouble();

            final double tst = MathLib.cbrt(x);
            final double ref = this.cbrt(this.field.newDfp(x)).toDouble();
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref - Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(this.cbrt(this.field.newDfp(x))).divide(this.field.newDfp(ulp))
                        .toDouble();
                // System.out.println(x+"\t"+tst+"\t"+ref+"\t"+err+"\t"+errulp);
                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }

        Assert.assertTrue("cbrt() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);
    }

    private Dfp cbrt(Dfp x) {
        boolean negative = false;

        if (x.lessThan(this.field.getZero())) {
            negative = true;
            x = x.negate();
        }

        Dfp y = DfpMath.pow(x, this.field.getOne().divide(3));

        if (negative) {
            y = y.negate();
        }

        return y;
    }

    @Test
    public void testToDegrees() {
        double maxerrulp = 0.0;
        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            final double x = this.generator.nextDouble();
            final double tst = this.field.newDfp(x).multiply(180).divide(this.field.getPi()).toDouble();
            final double ref = MathLib.toDegrees(x);
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double.doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.exp(this.field.newDfp(x)).subtract(this.field.getOne()))
                        .divide(this.field.newDfp(ulp)).toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }
        Assert.assertTrue("toDegrees() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);

    }

    @Test
    public void testToRadians() {
        double maxerrulp = 0.0;
        for (int i = 0; i < NUMBER_OF_TRIALS; i++) {
            final double x = this.generator.nextDouble();
            final double tst = this.field.newDfp(x).multiply(this.field.getPi()).divide(180).toDouble();
            final double ref = MathLib.toRadians(x);
            final double err = (tst - ref) / ref;

            if (err != 0) {
                final double ulp = Math.abs(ref -
                    Double.longBitsToDouble((Double
                        .doubleToLongBits(ref) ^ 1)));
                final double errulp =
                    this.field.newDfp(tst).subtract(DfpMath.exp(this.field.newDfp(x)).subtract(this.field.getOne()))
                        .divide(this.field.newDfp(ulp)).toDouble();
                // System.out.println(x + "\t" + tst + "\t" + ref + "\t" + err + "\t" + errulp);

                maxerrulp = Math.max(maxerrulp, Math.abs(errulp));
            }
        }
        Assert.assertTrue("toRadians() had errors in excess of " + MAX_ERROR_ULP + " ULP", maxerrulp < MAX_ERROR_ULP);

    }

    @Test
    public void testNextAfter() {
        // 0x402fffffffffffff 0x404123456789abcd -> 4030000000000000
        Assert.assertEquals(16.0, MathLib.nextAfter(15.999999999999998, 34.27555555555555), 0.0);

        // 0xc02fffffffffffff 0x404123456789abcd -> c02ffffffffffffe
        Assert.assertEquals(-15.999999999999996, MathLib.nextAfter(-15.999999999999998, 34.27555555555555), 0.0);

        // 0x402fffffffffffff 0x400123456789abcd -> 402ffffffffffffe
        Assert.assertEquals(15.999999999999996, MathLib.nextAfter(15.999999999999998, 2.142222222222222), 0.0);

        // 0xc02fffffffffffff 0x400123456789abcd -> c02ffffffffffffe
        Assert.assertEquals(-15.999999999999996, MathLib.nextAfter(-15.999999999999998, 2.142222222222222), 0.0);

        // 0x4020000000000000 0x404123456789abcd -> 4020000000000001
        Assert.assertEquals(8.000000000000002, MathLib.nextAfter(8.0, 34.27555555555555), 0.0);

        // 0xc020000000000000 0x404123456789abcd -> c01fffffffffffff
        Assert.assertEquals(-7.999999999999999, MathLib.nextAfter(-8.0, 34.27555555555555), 0.0);

        // 0x4020000000000000 0x400123456789abcd -> 401fffffffffffff
        Assert.assertEquals(7.999999999999999, MathLib.nextAfter(8.0, 2.142222222222222), 0.0);

        // 0xc020000000000000 0x400123456789abcd -> c01fffffffffffff
        Assert.assertEquals(-7.999999999999999, MathLib.nextAfter(-8.0, 2.142222222222222), 0.0);

        // 0x3f2e43753d36a223 0x3f2e43753d36a224 -> 3f2e43753d36a224
        Assert.assertEquals(2.308922399667661E-4, MathLib.nextAfter(2.3089223996676606E-4, 2.308922399667661E-4), 0.0);

        // 0x3f2e43753d36a223 0x3f2e43753d36a223 -> 3f2e43753d36a223
        Assert.assertEquals(2.3089223996676606E-4, MathLib.nextAfter(2.3089223996676606E-4, 2.3089223996676606E-4),
            0.0);

        // 0x3f2e43753d36a223 0x3f2e43753d36a222 -> 3f2e43753d36a222
        Assert.assertEquals(2.3089223996676603E-4, MathLib.nextAfter(2.3089223996676606E-4, 2.3089223996676603E-4),
            0.0);

        // 0x3f2e43753d36a223 0xbf2e43753d36a224 -> 3f2e43753d36a222
        Assert.assertEquals(2.3089223996676603E-4, MathLib.nextAfter(2.3089223996676606E-4, -2.308922399667661E-4),
            0.0);

        // 0x3f2e43753d36a223 0xbf2e43753d36a223 -> 3f2e43753d36a222
        Assert.assertEquals(2.3089223996676603E-4, MathLib.nextAfter(2.3089223996676606E-4, -2.3089223996676606E-4),
            0.0);

        // 0x3f2e43753d36a223 0xbf2e43753d36a222 -> 3f2e43753d36a222
        Assert.assertEquals(2.3089223996676603E-4, MathLib.nextAfter(2.3089223996676606E-4, -2.3089223996676603E-4),
            0.0);

        // 0xbf2e43753d36a223 0x3f2e43753d36a224 -> bf2e43753d36a222
        Assert.assertEquals(-2.3089223996676603E-4, MathLib.nextAfter(-2.3089223996676606E-4, 2.308922399667661E-4),
            0.0);

        // 0xbf2e43753d36a223 0x3f2e43753d36a223 -> bf2e43753d36a222
        Assert.assertEquals(-2.3089223996676603E-4, MathLib.nextAfter(-2.3089223996676606E-4, 2.3089223996676606E-4),
            0.0);

        // 0xbf2e43753d36a223 0x3f2e43753d36a222 -> bf2e43753d36a222
        Assert.assertEquals(-2.3089223996676603E-4, MathLib.nextAfter(-2.3089223996676606E-4, 2.3089223996676603E-4),
            0.0);

        // 0xbf2e43753d36a223 0xbf2e43753d36a224 -> bf2e43753d36a224
        Assert.assertEquals(-2.308922399667661E-4, MathLib.nextAfter(-2.3089223996676606E-4, -2.308922399667661E-4),
            0.0);

        // 0xbf2e43753d36a223 0xbf2e43753d36a223 -> bf2e43753d36a223
        Assert.assertEquals(-2.3089223996676606E-4, MathLib.nextAfter(-2.3089223996676606E-4, -2.3089223996676606E-4),
            0.0);

        // 0xbf2e43753d36a223 0xbf2e43753d36a222 -> bf2e43753d36a222
        Assert.assertEquals(-2.3089223996676603E-4, MathLib.nextAfter(-2.3089223996676606E-4, -2.3089223996676603E-4),
            0.0);

        // f = direction
        Assert.assertEquals(1f, MathLib.nextAfter(1f, 1.), 0.0);

        // Next down
        Assert.assertEquals(FastMath.nextAfter(12.5, Double.NEGATIVE_INFINITY), FastMath.nextDown(12.5), 0.0);
        Assert.assertEquals(FastMath.nextAfter((float) 12.5, Float.NEGATIVE_INFINITY), FastMath.nextDown((float) 12.5), 0.0);
    }

    @Test
    public void testDoubleNextAfterSpecialCases() {
        Assert.assertEquals(-Double.MAX_VALUE, MathLib.nextAfter(Double.NEGATIVE_INFINITY, 0D), 0D);
        Assert.assertEquals(Double.MAX_VALUE, MathLib.nextAfter(Double.POSITIVE_INFINITY, 0D), 0D);
        try {
            MathLib.nextAfter(Double.NaN, 0D);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathLib.nextAfter(Double.MAX_VALUE, Double.POSITIVE_INFINITY),
            0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.nextAfter(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY),
            0D);
        Assert.assertEquals(Double.MIN_VALUE, MathLib.nextAfter(0D, 1D), 0D);
        Assert.assertEquals(-Double.MIN_VALUE, MathLib.nextAfter(0D, -1D), 0D);
        Assert.assertEquals(0D, MathLib.nextAfter(Double.MIN_VALUE, -1), 0D);
        Assert.assertEquals(0D, MathLib.nextAfter(-Double.MIN_VALUE, 1), 0D);
    }

    @Test
    public void testFloatNextAfterSpecialCases() {
        Assert.assertEquals(-Float.MAX_VALUE, MathLib.nextAfter(Float.NEGATIVE_INFINITY, 0F), 0F);
        Assert.assertEquals(Float.MAX_VALUE, MathLib.nextAfter(Float.POSITIVE_INFINITY, 0F), 0F);
        try {
            MathLib.nextAfter(Float.NaN, 0F);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        Assert.assertEquals(Float.POSITIVE_INFINITY, MathLib.nextAfter(Float.MAX_VALUE, Float.POSITIVE_INFINITY), 0F);
        Assert.assertEquals(Float.NEGATIVE_INFINITY, MathLib.nextAfter(-Float.MAX_VALUE, Float.NEGATIVE_INFINITY), 0F);
        Assert.assertEquals(Float.MIN_VALUE, MathLib.nextAfter(0F, 1F), 0F);
        Assert.assertEquals(-Float.MIN_VALUE, MathLib.nextAfter(0F, -1F), 0F);
        Assert.assertEquals(0F, MathLib.nextAfter(Float.MIN_VALUE, -1F), 0F);
        Assert.assertEquals(0F, MathLib.nextAfter(-Float.MIN_VALUE, 1F), 0F);
    }

    @Test
    public void testDoubleScalbSpecialCases() {
        Assert.assertEquals(2.5269841324701218E-175, MathLib.scalb(2.2250738585072014E-308, 442), 0D);
        Assert.assertEquals(1.307993905256674E297, MathLib.scalb(1.1102230246251565E-16, 1040), 0D);
        Assert.assertEquals(7.2520887996488946E-217, MathLib.scalb(Double.MIN_VALUE, 356), 0D);
        Assert.assertEquals(8.98846567431158E307, MathLib.scalb(Double.MIN_VALUE, 2097), 0D);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathLib.scalb(Double.MIN_VALUE, 2098), 0D);
        Assert.assertEquals(1.1125369292536007E-308, MathLib.scalb(2.225073858507201E-308, -1), 0D);
        Assert.assertEquals(1.0E-323, MathLib.scalb(Double.MAX_VALUE, -2097), 0D);
        Assert.assertEquals(Double.MIN_VALUE, MathLib.scalb(Double.MAX_VALUE, -2098), 0D);
        Assert.assertEquals(0, MathLib.scalb(Double.MAX_VALUE, -2099), 0D);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathLib.scalb(Double.POSITIVE_INFINITY, -1000000), 0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.scalb(-1.1102230246251565E-16, 1078), 0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.scalb(-1.1102230246251565E-16, 1079), 0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.scalb(-2.2250738585072014E-308, 2047), 0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.scalb(-2.2250738585072014E-308, 2048), 0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.scalb(-1.7976931348623157E308, 2147483647), 0D);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathLib.scalb(1.7976931348623157E308, 2147483647), 0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.scalb(-1.1102230246251565E-16, 2147483647), 0D);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathLib.scalb(1.1102230246251565E-16, 2147483647), 0D);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, MathLib.scalb(-2.2250738585072014E-308, 2147483647), 0D);
        Assert.assertEquals(Double.POSITIVE_INFINITY, MathLib.scalb(2.2250738585072014E-308, 2147483647), 0D);
    }

    @Test
    public void testFloatScalbSpecialCases() {
        Assert.assertEquals(0f, MathLib.scalb(Float.MIN_VALUE, -30), 0F);
        Assert.assertEquals(2 * Float.MIN_VALUE, MathLib.scalb(Float.MIN_VALUE, 1), 0F);
        Assert.assertEquals(7.555786e22f, MathLib.scalb(Float.MAX_VALUE, -52), 0F);
        Assert.assertEquals(1.7014118e38f, MathLib.scalb(Float.MIN_VALUE, 276), 0F);
        Assert.assertEquals(Float.POSITIVE_INFINITY, MathLib.scalb(Float.MIN_VALUE, 277), 0F);
        Assert.assertEquals(5.8774718e-39f, MathLib.scalb(1.1754944e-38f, -1), 0F);
        Assert.assertEquals(2 * Float.MIN_VALUE, MathLib.scalb(Float.MAX_VALUE, -276), 0F);
        Assert.assertEquals(Float.MIN_VALUE, MathLib.scalb(Float.MAX_VALUE, -277), 0F);
        Assert.assertEquals(0, MathLib.scalb(Float.MAX_VALUE, -278), 0F);
        Assert.assertEquals(Float.POSITIVE_INFINITY, MathLib.scalb(Float.POSITIVE_INFINITY, -1000000), 0F);
        Assert.assertEquals(-3.13994498e38f, MathLib.scalb(-1.1e-7f, 151), 0F);
        Assert.assertEquals(Float.NEGATIVE_INFINITY, MathLib.scalb(-1.1e-7f, 152), 0F);
        Assert.assertEquals(Float.POSITIVE_INFINITY, MathLib.scalb(3.4028235E38f, 2147483647), 0F);
        Assert.assertEquals(Float.NEGATIVE_INFINITY, MathLib.scalb(-3.4028235E38f, 2147483647), 0F);
    }

    @Test
    public void testSignumDouble() {
        final double delta = 0.0;
        Assert.assertEquals(1.0, MathLib.signum(2.0), delta);
        Assert.assertEquals(0.0, MathLib.signum(0.0), delta);
        Assert.assertEquals(-1.0, MathLib.signum(-2.0), delta);
        try {
            MathLib.signum(Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testSignumFloat() {
        final float delta = 0.0F;
        Assert.assertEquals(1.0F, MathLib.signum(2.0F), delta);
        Assert.assertEquals(0.0F, MathLib.signum(0.0F), delta);
        Assert.assertEquals(-1.0F, MathLib.signum(-2.0F), delta);

        try {
            MathLib.signum(Float.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testLogWithBase() {
        Assert.assertEquals(2.0, FastMath.log(2, 4), 0);
        Assert.assertEquals(3.0, FastMath.log(2, 8), 0);
        try {
            FastMath.log(-1, 1);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            FastMath.log(1, -1);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        try {
            FastMath.log(0, 0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
        Assert.assertEquals(0, FastMath.log(0, 10), 0);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, FastMath.log(10, 0), 0);
        try {
            Assert.assertEquals(0, FastMath.log(1, 1), 0);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testIndicatorDouble() {
        final double delta = 0.0;
        Assert.assertEquals(1.0, MathLib.copySign(1d, 2.0), delta);
        Assert.assertEquals(1.0, MathLib.copySign(1d, 0.0), delta);
        Assert.assertEquals(-1.0, MathLib.copySign(1d, -2.0), delta);
    }

    @Test
    public void testIndicatorFloat() {
        final float delta = 0.0F;
        Assert.assertEquals(1.0F, MathLib.copySign(1d, 2.0F), delta);
        Assert.assertEquals(1.0F, MathLib.copySign(1d, 0.0F), delta);
        Assert.assertEquals(-1.0F, MathLib.copySign(1d, -2.0F), delta);
    }

    @Test
    public void testIntPow() {
        final int maxExp = 300;
        final DfpField field = new DfpField(40);
        final double base = 1.23456789;
        final Dfp baseDfp = field.newDfp(base);
        Dfp dfpPower = field.getOne();
        for (int i = 0; i < maxExp; i++) {
            Assert.assertEquals("exp=" + i, dfpPower.toDouble(), MathLib.pow(base, i),
                0.6 * MathLib.ulp(dfpPower.toDouble()));
            dfpPower = dfpPower.multiply(baseDfp);
        }
    }

    // Test for method MathLib.divide(x, y)
    @Test
    public void testDivide() {

        // ArithmeticException expected if x = NaN or/and y = NaN
        try {
            MathLib.divide(Double.NaN, 4.);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.divide(4., Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        try {
            MathLib.divide(Double.NaN, Double.NaN);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        // ArithmeticException if y = +/-0
        try {
            MathLib.divide(2., -0.);
            Assert.fail();
        } catch (final ArithmeticException e) {
            Assert.assertTrue(true);
        }

        // Result is x / y otherwise
        Assert.assertEquals(4. / 3., MathLib.divide(4, 3), 0.);

    }

}
