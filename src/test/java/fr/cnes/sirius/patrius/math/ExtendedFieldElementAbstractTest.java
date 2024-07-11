/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;

public abstract class ExtendedFieldElementAbstractTest<T extends RealFieldElement<T>> {

    protected abstract T build(double x);

    @Test
    public void testAddField() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x + y, this.build(x).add(this.build(y)));
            }
        }
    }

    @Test
    public void testAddDouble() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x + y, this.build(x).add(y));
            }
        }
    }

    @Test
    public void testSubtractField() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x - y, this.build(x).subtract(this.build(y)));
            }
        }
    }

    @Test
    public void testSubtractDouble() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x - y, this.build(x).subtract(y));
            }
        }
    }

    @Test
    public void testMultiplyField() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x * y, this.build(x).multiply(this.build(y)));
            }
        }
    }

    @Test
    public void testMultiplyDouble() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x * y, this.build(x).multiply(y));
            }
        }
    }

    @Test
    public void testMultiplyInt() {
        for (double x = -3; x < 3; x += 0.2) {
            for (int y = -10; y < 10; y += 1) {
                this.checkRelative(x * y, this.build(x).multiply(y));
            }
        }
    }

    @Test
    public void testDivideField() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x / y, this.build(x).divide(this.build(y)));
            }
        }
    }

    @Test
    public void testDivideDouble() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(x / y, this.build(x).divide(y));
            }
        }
    }

    @Test
    public void testRemainderField() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(MathLib.IEEEremainder(x, y), this.build(x).remainder(this.build(y)));
            }
        }
    }

    @Test
    public void testRemainderDouble() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3.2; y < 3.2; y += 0.25) {
                this.checkRelative(MathLib.IEEEremainder(x, y), this.build(x).remainder(y));
            }
        }
    }

    @Test
    public void testCos() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.cos(x), this.build(x).cos());
        }
    }

    @Test
    public void testAcos() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.acos(x), this.build(x).acos());
        }
    }

    @Test
    public void testSin() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.sin(x), this.build(x).sin());
        }
    }

    @Test
    public void testAsin() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.asin(x), this.build(x).asin());
        }
    }

    @Test
    public void testTan() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.tan(x), this.build(x).tan());
        }
    }

    @Test
    public void testAtan() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.atan(x), this.build(x).atan());
        }
    }

    @Test
    public void testAtan2() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(MathLib.atan2(x, y), this.build(x).atan2(this.build(y)));
            }
        }
    }

    @Test
    public void testCosh() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.cosh(x), this.build(x).cosh());
        }
    }

    @Test
    public void testAcosh() {
        for (double x = 1.1; x < 5.0; x += 0.05) {
            this.checkRelative(MathLib.acosh(x), this.build(x).acosh());
        }
    }

    @Test
    public void testSinh() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.sinh(x), this.build(x).sinh());
        }
    }

    @Test
    public void testAsinh() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.asinh(x), this.build(x).asinh());
        }
    }

    @Test
    public void testTanh() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.tanh(x), this.build(x).tanh());
        }
    }

    @Test
    public void testAtanh() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.atanh(x), this.build(x).atanh());
        }
    }

    @Test
    public void testSqrt() {
        for (double x = 0.01; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.sqrt(x), this.build(x).sqrt());
        }
    }

    @Test
    public void testCbrt() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.cbrt(x), this.build(x).cbrt());
        }
    }

    @Test
    public void testHypot() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(MathLib.hypot(x, y), this.build(x).hypot(this.build(y)));
            }
        }
    }

    @Test
    public void testRootN() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            for (int n = 1; n < 5; ++n) {
                if (x < 0) {
                    if (n % 2 == 1) {
                        this.checkRelative(-MathLib.pow(-x, 1.0 / n), this.build(x).rootN(n));
                    }
                } else {
                    this.checkRelative(MathLib.pow(x, 1.0 / n), this.build(x).rootN(n));
                }
            }
        }
    }

    @Test
    public void testPowField() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            for (double y = 0.1; y < 4; y += 0.2) {
                this.checkRelative(MathLib.pow(x, y), this.build(x).pow(this.build(y)));
            }
        }
    }

    @Test
    public void testPowDouble() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            for (double y = 0.1; y < 4; y += 0.2) {
                this.checkRelative(MathLib.pow(x, y), this.build(x).pow(y));
            }
        }
    }

    @Test
    public void testPowInt() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            for (int n = 0; n < 5; ++n) {
                this.checkRelative(MathLib.pow(x, n), this.build(x).pow(n));
            }
        }
    }

    @Test
    public void testExp() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.exp(x), this.build(x).exp());
        }
    }

    @Test
    public void testExpm1() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.expm1(x), this.build(x).expm1());
        }
    }

    @Test
    public void testLog() {
        for (double x = 0.01; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.log(x), this.build(x).log());
        }
    }

    @Test
    public void testLog1p() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.log1p(x), this.build(x).log1p());
        }
    }

    @Test
    public void testAbs() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.abs(x), this.build(x).abs());
        }
    }

    @Test
    public void testCeil() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.ceil(x), this.build(x).ceil());
        }
    }

    @Test
    public void testFloor() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.floor(x), this.build(x).floor());
        }
    }

    @Test
    public void testRint() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.rint(x), this.build(x).rint());
        }
    }

    @Test
    public void testRound() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            Assert.assertEquals(MathLib.round(x), this.build(x).round());
        }
    }

    @Test
    public void testSignum() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            this.checkRelative(MathLib.signum(x), this.build(x).signum());
        }
    }

    @Test
    public void testCopySignField() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(MathLib.copySign(x, y), this.build(x).copySign(this.build(y)));
            }
        }
    }

    @Test
    public void testCopySignDouble() {
        for (double x = -3; x < 3; x += 0.2) {
            for (double y = -3; y < 3; y += 0.2) {
                this.checkRelative(MathLib.copySign(x, y), this.build(x).copySign(y));
            }
        }
    }

    @Test
    public void testScalb() {
        for (double x = -0.9; x < 0.9; x += 0.05) {
            for (int n = -100; n < 100; ++n) {
                this.checkRelative(MathLib.scalb(x, n), this.build(x).scalb(n));
            }
        }
    }

    @Test
    public void testLinearCombinationFaFa() {
        final RandomGenerator r = new Well1024a(0xfafal);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 10);
            final double[] bD = this.generateDouble(r, 10);
            final T[] aF = this.toFieldArray(aD);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD, bD),
                aF[0].linearCombination(aF, bF));
        }
    }

    @Test
    public void testLinearCombinationDaFa() {
        final RandomGenerator r = new Well1024a(0xdafal);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 10);
            final double[] bD = this.generateDouble(r, 10);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD, bD),
                bF[0].linearCombination(aD, bF));
        }
    }

    @Test
    public void testLinearCombinationFF2() {
        final RandomGenerator r = new Well1024a(0xff2l);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 2);
            final double[] bD = this.generateDouble(r, 2);
            final T[] aF = this.toFieldArray(aD);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD[0], bD[0], aD[1], bD[1]),
                aF[0].linearCombination(aF[0], bF[0], aF[1], bF[1]));
        }
    }

    @Test
    public void testLinearCombinationDF2() {
        final RandomGenerator r = new Well1024a(0xdf2l);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 2);
            final double[] bD = this.generateDouble(r, 2);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD[0], bD[0], aD[1], bD[1]),
                bF[0].linearCombination(aD[0], bF[0], aD[1], bF[1]));
        }
    }

    @Test
    public void testLinearCombinationFF3() {
        final RandomGenerator r = new Well1024a(0xff3l);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 3);
            final double[] bD = this.generateDouble(r, 3);
            final T[] aF = this.toFieldArray(aD);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD[0], bD[0], aD[1], bD[1], aD[2], bD[2]),
                aF[0].linearCombination(aF[0], bF[0], aF[1], bF[1], aF[2], bF[2]));
        }
    }

    @Test
    public void testLinearCombinationDF3() {
        final RandomGenerator r = new Well1024a(0xdf3l);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 3);
            final double[] bD = this.generateDouble(r, 3);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD[0], bD[0], aD[1], bD[1], aD[2], bD[2]),
                bF[0].linearCombination(aD[0], bF[0], aD[1], bF[1], aD[2], bF[2]));
        }
    }

    @Test
    public void testLinearCombinationFF4() {
        final RandomGenerator r = new Well1024a(0xff4l);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 4);
            final double[] bD = this.generateDouble(r, 4);
            final T[] aF = this.toFieldArray(aD);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD[0], bD[0], aD[1], bD[1], aD[2], bD[2], aD[3], bD[3]),
                aF[0].linearCombination(aF[0], bF[0], aF[1], bF[1], aF[2], bF[2], aF[3], bF[3]));
        }
    }

    @Test
    public void testLinearCombinationDF4() {
        final RandomGenerator r = new Well1024a(0xdf4l);
        for (int i = 0; i < 50; ++i) {
            final double[] aD = this.generateDouble(r, 4);
            final double[] bD = this.generateDouble(r, 4);
            final T[] bF = this.toFieldArray(bD);
            this.checkRelative(MathArrays.linearCombination(aD[0], bD[0], aD[1], bD[1], aD[2], bD[2], aD[3], bD[3]),
                bF[0].linearCombination(aD[0], bF[0], aD[1], bF[1], aD[2], bF[2], aD[3], bF[3]));
        }
    }

    @Test
    public void testGetField() {
        this.checkRelative(1.0, this.build(-10).getField().getOne());
        this.checkRelative(0.0, this.build(-10).getField().getZero());
    }

    private void checkRelative(final double expected, final T obtained) {
        Assert.assertEquals(expected, obtained.getReal(), 1.0e-15 * (1 + MathLib.abs(expected)));
    }

    @Test
    public void testEquals() {
        final T t1a = this.build(1.0);
        final T t1b = this.build(1.0);
        final T t2 = this.build(2.0);
        Assert.assertTrue(t1a.equals(t1a));
        Assert.assertTrue(t1a.equals(t1b));
        Assert.assertFalse(t1a.equals(t2));
        Assert.assertFalse(t1a.equals(new Object()));
    }

    @Test
    public void testHash() {
        final T t1a = this.build(1.0);
        final T t1b = this.build(1.0);
        final T t2 = this.build(2.0);
        Assert.assertEquals(t1a.hashCode(), t1b.hashCode());
        Assert.assertTrue(t1a.hashCode() != t2.hashCode());
    }

    private double[] generateDouble(final RandomGenerator r, final int n) {
        final double[] a = new double[n];
        for (int i = 0; i < n; ++i) {
            a[i] = r.nextDouble();
        }
        return a;
    }

    private T[] toFieldArray(final double[] a) {
        final T[] f = MathArrays.buildArray(this.build(0).getField(), a.length);
        for (int i = 0; i < a.length; ++i) {
            f[i] = this.build(a[i]);
        }
        return f;
    }

}
