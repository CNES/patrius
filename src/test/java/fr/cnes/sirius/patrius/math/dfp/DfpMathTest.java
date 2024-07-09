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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.dfp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DfpMathTest {

    private DfpField factory;
    private Dfp pinf;
    private Dfp ninf;
    private Dfp nan;
    private Dfp qnan;

    @Before
    public void setUp() {
        // Some basic setup. Define some constants and clear the status flags
        this.factory = new DfpField(20);
        this.pinf = this.factory.newDfp("1").divide(this.factory.newDfp("0"));
        this.ninf = this.factory.newDfp("-1").divide(this.factory.newDfp("0"));
        this.nan = this.factory.newDfp("0").divide(this.factory.newDfp("0"));
        this.qnan = this.factory.newDfp((byte) 1, Dfp.QNAN);
        this.ninf.getField().clearIEEEFlags();

        // force loading of dfpmath
        final Dfp pi = this.factory.getPi();
        pi.getField().clearIEEEFlags();
    }

    @After
    public void tearDown() {
        this.pinf = null;
        this.ninf = null;
        this.nan = null;
        this.qnan = null;
    }

    // Generic test function. Takes params x and y and tests them for
    // equality. Then checks the status flags against the flags argument.
    // If the test fail, it prints the desc string
    private void test(final Dfp x, final Dfp y, final int flags, final String desc)
    {
        boolean b = x.equals(y);

        if (!x.equals(y) && !x.unequal(y)) {
            b = (x.toString().equals(y.toString()));
        }

        if (x.equals(this.factory.newDfp("0"))) {
            b = (b && (x.toString().equals(y.toString())));
        }

        b = (b && x.getField().getIEEEFlags() == flags);

        if (!b) {
            Assert.assertTrue("assersion failed " + desc + " x = " + x.toString() + " flags = "
                + x.getField().getIEEEFlags(), b);
        }

        x.getField().clearIEEEFlags();
    }

    @Test
    public void testPow()
    {
        // Test special cases exponent of zero
        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("0")),
            this.factory.newDfp("1"),
            0, "pow #1");

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("-0")),
            this.factory.newDfp("1"),
            0, "pow #2");

        this.test(DfpMath.pow(this.factory.newDfp("2"), this.factory.newDfp("0")),
            this.factory.newDfp("1"),
            0, "pow #3");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.factory.newDfp("-0")),
            this.factory.newDfp("1"),
            0, "pow #4");

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("-0")),
            this.factory.newDfp("1"),
            0, "pow #5");

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("0")),
            this.factory.newDfp("1"),
            0, "pow #6");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("-0")),
            this.factory.newDfp("1"),
            0, "pow #7");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("0")),
            this.factory.newDfp("1"),
            0, "pow #8");

        this.test(DfpMath.pow(this.qnan, this.factory.newDfp("0")),
            this.factory.newDfp("1"),
            0, "pow #8");

        // exponent of one
        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("1")),
            this.factory.newDfp("0"),
            0, "pow #9");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("1")),
            this.factory.newDfp("-0"),
            0, "pow #10");

        this.test(DfpMath.pow(this.factory.newDfp("2"), this.factory.newDfp("1")),
            this.factory.newDfp("2"),
            0, "pow #11");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.factory.newDfp("1")),
            this.factory.newDfp("-2"),
            0, "pow #12");

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("1")),
            this.pinf,
            0, "pow #13");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("1")),
            this.ninf,
            0, "pow #14");

        this.test(DfpMath.pow(this.qnan, this.factory.newDfp("1")),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #14.1");

        // exponent of NaN
        this.test(DfpMath.pow(this.factory.newDfp("0"), this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #15");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #16");

        this.test(DfpMath.pow(this.factory.newDfp("2"), this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #17");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #18");

        this.test(DfpMath.pow(this.pinf, this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #19");

        this.test(DfpMath.pow(this.ninf, this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #20");

        this.test(DfpMath.pow(this.qnan, this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #21");

        // radix of NaN
        this.test(DfpMath.pow(this.qnan, this.factory.newDfp("1")),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #22");

        this.test(DfpMath.pow(this.qnan, this.factory.newDfp("-1")),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #23");

        this.test(DfpMath.pow(this.qnan, this.pinf),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #24");

        this.test(DfpMath.pow(this.qnan, this.ninf),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #25");

        this.test(DfpMath.pow(this.qnan, this.qnan),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #26");

        // (x > 1) ^ pinf = pinf, (x < -1) ^ pinf = pinf
        this.test(DfpMath.pow(this.factory.newDfp("2"), this.pinf),
            this.pinf,
            0, "pow #27");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.pinf),
            this.pinf,
            0, "pow #28");

        this.test(DfpMath.pow(this.pinf, this.pinf),
            this.pinf,
            0, "pow #29");

        this.test(DfpMath.pow(this.ninf, this.pinf),
            this.pinf,
            0, "pow #30");

        // (x > 1) ^ ninf = +0, (x < -1) ^ ninf = +0
        this.test(DfpMath.pow(this.factory.newDfp("2"), this.ninf),
            this.factory.getZero(),
            0, "pow #31");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.ninf),
            this.factory.getZero(),
            0, "pow #32");

        this.test(DfpMath.pow(this.pinf, this.ninf),
            this.factory.getZero(),
            0, "pow #33");

        this.test(DfpMath.pow(this.ninf, this.ninf),
            this.factory.getZero(),
            0, "pow #34");

        // (-1 < x < 1) ^ pinf = 0
        this.test(DfpMath.pow(this.factory.newDfp("0.5"), this.pinf),
            this.factory.getZero(),
            0, "pow #35");

        this.test(DfpMath.pow(this.factory.newDfp("-0.5"), this.pinf),
            this.factory.getZero(),
            0, "pow #36");

        // (-1 < x < 1) ^ ninf = pinf
        this.test(DfpMath.pow(this.factory.newDfp("0.5"), this.ninf),
            this.pinf,
            0, "pow #37");

        this.test(DfpMath.pow(this.factory.newDfp("-0.5"), this.ninf),
            this.pinf,
            0, "pow #38");

        // +/- 1 ^ +/-inf = NaN
        this.test(DfpMath.pow(this.factory.getOne(), this.pinf),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #39");

        this.test(DfpMath.pow(this.factory.getOne(), this.ninf),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #40");

        this.test(DfpMath.pow(this.factory.newDfp("-1"), this.pinf),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #41");

        this.test(DfpMath.pow(this.factory.getOne().negate(), this.ninf),
            this.qnan,
            DfpField.FLAG_INVALID, "pow #42");

        // +0 ^ +anything except 0, NAN = +0

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("1")),
            this.factory.newDfp("0"),
            0, "pow #43");

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("1e30")),
            this.factory.newDfp("0"),
            0, "pow #44");

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("1e-30")),
            this.factory.newDfp("0"),
            0, "pow #45");

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.pinf),
            this.factory.newDfp("0"),
            0, "pow #46");

        // -0 ^ +anything except 0, NAN, odd integer = +0

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("2")),
            this.factory.newDfp("0"),
            0, "pow #47");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("1e30")),
            this.factory.newDfp("0"),
            0, "pow #48");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("1e-30")),
            this.factory.newDfp("0"),
            DfpField.FLAG_INEXACT, "pow #49");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.pinf),
            this.factory.newDfp("0"),
            0, "pow #50");

        // +0 ^ -anything except 0, NAN = +INF

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("-1")),
            this.pinf,
            0, "pow #51");

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("-1e30")),
            this.pinf,
            0, "pow #52");

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.factory.newDfp("-1e-30")),
            this.pinf,
            0, "pow #53");

        this.test(DfpMath.pow(this.factory.newDfp("0"), this.ninf),
            this.pinf,
            0, "pow #54");

        // -0 ^ -anything except 0, NAN, odd integer = +INF

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("-2")),
            this.pinf,
            0, "pow #55");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("-1e30")),
            this.pinf,
            0, "pow #56");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("-1e-30")),
            this.pinf,
            DfpField.FLAG_INEXACT, "pow #57");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.ninf),
            this.pinf,
            0, "pow #58");

        // -0 ^ -odd integer = -INF
        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("-1")),
            this.ninf,
            DfpField.FLAG_INEXACT, "pow #59");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("-12345")),
            this.ninf,
            DfpField.FLAG_INEXACT, "pow #60");

        // -0 ^ +odd integer = -0
        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("3")),
            this.factory.newDfp("-0"),
            DfpField.FLAG_INEXACT, "pow #61");

        this.test(DfpMath.pow(this.factory.newDfp("-0"), this.factory.newDfp("12345")),
            this.factory.newDfp("-0"),
            DfpField.FLAG_INEXACT, "pow #62");

        // pinf ^ +anything = pinf
        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("3")),
            this.pinf,
            0, "pow #63");

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("1e30")),
            this.pinf,
            0, "pow #64");

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("1e-30")),
            this.pinf,
            0, "pow #65");

        this.test(DfpMath.pow(this.pinf, this.pinf),
            this.pinf,
            0, "pow #66");

        // pinf ^ -anything = +0

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("-3")),
            this.factory.getZero(),
            0, "pow #67");

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("-1e30")),
            this.factory.getZero(),
            0, "pow #68");

        this.test(DfpMath.pow(this.pinf, this.factory.newDfp("-1e-30")),
            this.factory.getZero(),
            0, "pow #69");

        this.test(DfpMath.pow(this.pinf, this.ninf),
            this.factory.getZero(),
            0, "pow #70");

        // ninf ^ anything = -0 ^ -anything
        // ninf ^ -anything except 0, NAN, odd integer = +0

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("-2")),
            this.factory.newDfp("0"),
            0, "pow #71");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("-1e30")),
            this.factory.newDfp("0"),
            0, "pow #72");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("-1e-30")),
            this.factory.newDfp("0"),
            DfpField.FLAG_INEXACT, "pow #73");

        this.test(DfpMath.pow(this.ninf, this.ninf),
            this.factory.newDfp("0"),
            0, "pow #74");

        // ninf ^ +anything except 0, NAN, odd integer = +INF

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("2")),
            this.pinf,
            0, "pow #75");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("1e30")),
            this.pinf,
            0, "pow #76");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("1e-30")),
            this.pinf,
            DfpField.FLAG_INEXACT, "pow #77");

        this.test(DfpMath.pow(this.ninf, this.pinf),
            this.pinf,
            0, "pow #78");

        // ninf ^ +odd integer = -INF
        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("3")),
            this.ninf,
            DfpField.FLAG_INEXACT, "pow #79");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("12345")),
            this.ninf,
            DfpField.FLAG_INEXACT, "pow #80");

        // ninf ^ -odd integer = -0
        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("-3")),
            this.factory.newDfp("-0"),
            DfpField.FLAG_INEXACT, "pow #81");

        this.test(DfpMath.pow(this.ninf, this.factory.newDfp("-12345")),
            this.factory.newDfp("-0"),
            DfpField.FLAG_INEXACT, "pow #82");

        // -anything ^ integer
        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.factory.newDfp("3")),
            this.factory.newDfp("-8"),
            DfpField.FLAG_INEXACT, "pow #83");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.factory.newDfp("16")),
            this.factory.newDfp("65536"),
            0, "pow #84");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.factory.newDfp("-3")),
            this.factory.newDfp("-0.125"),
            DfpField.FLAG_INEXACT, "pow #85");

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.factory.newDfp("-4")),
            this.factory.newDfp("0.0625"),
            0, "pow #86");

        // -anything ^ noninteger = NaN

        this.test(DfpMath.pow(this.factory.newDfp("-2"), this.factory.newDfp("-4.1")),
            this.qnan,
            DfpField.FLAG_INVALID | DfpField.FLAG_INEXACT, "pow #87");

        // Some fractional cases.
        this.test(DfpMath.pow(this.factory.newDfp("2"), this.factory.newDfp("1.5")),
            this.factory.newDfp("2.8284271247461901"),
            DfpField.FLAG_INEXACT, "pow #88");
    }

    @Test
    public void testSin()
    {
        this.test(DfpMath.sin(this.pinf),
            this.nan,
            DfpField.FLAG_INVALID | DfpField.FLAG_INEXACT, "sin #1");

        this.test(DfpMath.sin(this.nan),
            this.nan,
            DfpField.FLAG_INVALID | DfpField.FLAG_INEXACT, "sin #2");

        this.test(DfpMath.sin(this.factory.getZero()),
            this.factory.getZero(),
            DfpField.FLAG_INEXACT, "sin #3");

        this.test(DfpMath.sin(this.factory.getPi()),
            this.factory.getZero(),
            DfpField.FLAG_INEXACT, "sin #4");

        this.test(DfpMath.sin(this.factory.getPi().negate()),
            this.factory.newDfp("-0"),
            DfpField.FLAG_INEXACT, "sin #5");

        this.test(DfpMath.sin(this.factory.getPi().multiply(2)),
            this.factory.getZero(),
            DfpField.FLAG_INEXACT, "sin #6");

        this.test(DfpMath.sin(this.factory.getPi().divide(2)),
            this.factory.getOne(),
            DfpField.FLAG_INEXACT, "sin #7");

        this.test(DfpMath.sin(this.factory.getPi().divide(2).negate()),
            this.factory.getOne().negate(),
            DfpField.FLAG_INEXACT, "sin #8");

        this.test(DfpMath.sin(DfpMath.atan(this.factory.getOne())), // pi/4
            this.factory.newDfp("0.5").sqrt(),
            DfpField.FLAG_INEXACT, "sin #9");

        this.test(DfpMath.sin(DfpMath.atan(this.factory.getOne())).negate(), // -pi/4
            this.factory.newDfp("0.5").sqrt().negate(),
            DfpField.FLAG_INEXACT, "sin #10");

        this.test(DfpMath.sin(DfpMath.atan(this.factory.getOne())).negate(), // -pi/4
            this.factory.newDfp("0.5").sqrt().negate(),
            DfpField.FLAG_INEXACT, "sin #11");

        this.test(DfpMath.sin(this.factory.newDfp("0.1")),
            this.factory.newDfp("0.0998334166468281523"),
            DfpField.FLAG_INEXACT, "sin #12");

        this.test(DfpMath.sin(this.factory.newDfp("0.2")),
            this.factory.newDfp("0.19866933079506121546"),
            DfpField.FLAG_INEXACT, "sin #13");

        this.test(DfpMath.sin(this.factory.newDfp("0.3")),
            this.factory.newDfp("0.2955202066613395751"),
            DfpField.FLAG_INEXACT, "sin #14");

        this.test(DfpMath.sin(this.factory.newDfp("0.4")),
            this.factory.newDfp("0.38941834230865049166"),
            DfpField.FLAG_INEXACT, "sin #15");

        this.test(DfpMath.sin(this.factory.newDfp("0.5")),
            this.factory.newDfp("0.47942553860420300026"), // off by one ULP
            DfpField.FLAG_INEXACT, "sin #16");

        this.test(DfpMath.sin(this.factory.newDfp("0.6")),
            this.factory.newDfp("0.56464247339503535721"), // off by one ULP
            DfpField.FLAG_INEXACT, "sin #17");

        this.test(DfpMath.sin(this.factory.newDfp("0.7")),
            this.factory.newDfp("0.64421768723769105367"),
            DfpField.FLAG_INEXACT, "sin #18");

        this.test(DfpMath.sin(this.factory.newDfp("0.8")),
            this.factory.newDfp("0.71735609089952276163"),
            DfpField.FLAG_INEXACT, "sin #19");

        this.test(DfpMath.sin(this.factory.newDfp("0.9")), // off by one ULP
            this.factory.newDfp("0.78332690962748338847"),
            DfpField.FLAG_INEXACT, "sin #20");

        this.test(DfpMath.sin(this.factory.newDfp("1.0")),
            this.factory.newDfp("0.84147098480789650666"),
            DfpField.FLAG_INEXACT, "sin #21");

        this.test(DfpMath.sin(this.factory.newDfp("1.1")),
            this.factory.newDfp("0.89120736006143533995"),
            DfpField.FLAG_INEXACT, "sin #22");

        this.test(DfpMath.sin(this.factory.newDfp("1.2")),
            this.factory.newDfp("0.93203908596722634968"),
            DfpField.FLAG_INEXACT, "sin #23");

        this.test(DfpMath.sin(this.factory.newDfp("1.3")),
            this.factory.newDfp("0.9635581854171929647"),
            DfpField.FLAG_INEXACT, "sin #24");

        this.test(DfpMath.sin(this.factory.newDfp("1.4")),
            this.factory.newDfp("0.98544972998846018066"),
            DfpField.FLAG_INEXACT, "sin #25");

        this.test(DfpMath.sin(this.factory.newDfp("1.5")),
            this.factory.newDfp("0.99749498660405443096"),
            DfpField.FLAG_INEXACT, "sin #26");

        this.test(DfpMath.sin(this.factory.newDfp("1.6")),
            this.factory.newDfp("0.99957360304150516323"),
            DfpField.FLAG_INEXACT, "sin #27");
    }

}
