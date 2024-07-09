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

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class DfpTest {

    private DfpField field;
    private Dfp pinf;
    private Dfp ninf;
    private Dfp nan;
    private Dfp snan;
    private Dfp qnan;

    @Before
    public void setUp() {
        // Some basic setup. Define some constants and clear the status flags
        this.field = new DfpField(20);
        this.pinf = this.field.newDfp("1").divide(this.field.newDfp("0"));
        this.ninf = this.field.newDfp("-1").divide(this.field.newDfp("0"));
        this.nan = this.field.newDfp("0").divide(this.field.newDfp("0"));
        this.snan = this.field.newDfp((byte) 1, Dfp.SNAN);
        this.qnan = this.field.newDfp((byte) 1, Dfp.QNAN);
        this.ninf.getField().clearIEEEFlags();
    }

    @After
    public void tearDown() {
        this.field = null;
        this.pinf = null;
        this.ninf = null;
        this.nan = null;
        this.snan = null;
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

        if (x.equals(this.field.newDfp("0"))) {
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
    public void testByteConstructor() {
        Assert.assertEquals("0.", new Dfp(this.field, (byte) 0).toString());
        Assert.assertEquals("1.", new Dfp(this.field, (byte) 1).toString());
        Assert.assertEquals("-1.", new Dfp(this.field, (byte) -1).toString());
        Assert.assertEquals("-128.", new Dfp(this.field, Byte.MIN_VALUE).toString());
        Assert.assertEquals("127.", new Dfp(this.field, Byte.MAX_VALUE).toString());
    }

    @Test
    public void testIntConstructor() {
        Assert.assertEquals("0.", new Dfp(this.field, 0).toString());
        Assert.assertEquals("1.", new Dfp(this.field, 1).toString());
        Assert.assertEquals("-1.", new Dfp(this.field, -1).toString());
        Assert.assertEquals("1234567890.", new Dfp(this.field, 1234567890).toString());
        Assert.assertEquals("-1234567890.", new Dfp(this.field, -1234567890).toString());
        Assert.assertEquals("-2147483648.", new Dfp(this.field, Integer.MIN_VALUE).toString());
        Assert.assertEquals("2147483647.", new Dfp(this.field, Integer.MAX_VALUE).toString());
    }

    @Test
    public void testLongConstructor() {
        Assert.assertEquals("0.", new Dfp(this.field, 0l).toString());
        Assert.assertEquals("1.", new Dfp(this.field, 1l).toString());
        Assert.assertEquals("-1.", new Dfp(this.field, -1l).toString());
        Assert.assertEquals("1234567890.", new Dfp(this.field, 1234567890l).toString());
        Assert.assertEquals("-1234567890.", new Dfp(this.field, -1234567890l).toString());
        Assert.assertEquals("-9223372036854775808.", new Dfp(this.field, Long.MIN_VALUE).toString());
        Assert.assertEquals("9223372036854775807.", new Dfp(this.field, Long.MAX_VALUE).toString());
    }

    /*
     * Test addition
     */
    @Test
    public void testAdd()
    {
        this.test(this.field.newDfp("1").add(this.field.newDfp("1")), // Basic tests 1+1 = 2
            this.field.newDfp("2"),
            0, "Add #1");

        this.test(this.field.newDfp("1").add(this.field.newDfp("-1")), // 1 + (-1) = 0
            this.field.newDfp("0"),
            0, "Add #2");

        this.test(this.field.newDfp("-1").add(this.field.newDfp("1")), // (-1) + 1 = 0
            this.field.newDfp("0"),
            0, "Add #3");

        this.test(this.field.newDfp("-1").add(this.field.newDfp("-1")), // (-1) + (-1) = -2
            this.field.newDfp("-2"),
            0, "Add #4");

        // rounding mode is round half even

        this.test(this.field.newDfp("1").add(this.field.newDfp("1e-16")), // rounding on add
            this.field.newDfp("1.0000000000000001"),
            0, "Add #5");

        this.test(this.field.newDfp("1").add(this.field.newDfp("1e-17")), // rounding on add
            this.field.newDfp("1"),
            DfpField.FLAG_INEXACT, "Add #6");

        this.test(this.field.newDfp("0.90999999999999999999").add(this.field.newDfp("0.1")), // rounding on add
            this.field.newDfp("1.01"),
            DfpField.FLAG_INEXACT, "Add #7");

        this.test(this.field.newDfp(".10000000000000005000").add(this.field.newDfp(".9")), // rounding on add
            this.field.newDfp("1."),
            DfpField.FLAG_INEXACT, "Add #8");

        this.test(this.field.newDfp(".10000000000000015000").add(this.field.newDfp(".9")), // rounding on add
            this.field.newDfp("1.0000000000000002"),
            DfpField.FLAG_INEXACT, "Add #9");

        this.test(this.field.newDfp(".10000000000000014999").add(this.field.newDfp(".9")), // rounding on add
            this.field.newDfp("1.0000000000000001"),
            DfpField.FLAG_INEXACT, "Add #10");

        this.test(this.field.newDfp(".10000000000000015001").add(this.field.newDfp(".9")), // rounding on add
            this.field.newDfp("1.0000000000000002"),
            DfpField.FLAG_INEXACT, "Add #11");

        this.test(this.field.newDfp(".11111111111111111111").add(this.field.newDfp("11.1111111111111111")), // rounding
                                                                                                            // on add
            this.field.newDfp("11.22222222222222222222"),
            DfpField.FLAG_INEXACT, "Add #12");

        this.test(this.field.newDfp(".11111111111111111111").add(this.field.newDfp("1111111111111111.1111")), // rounding
                                                                                                              // on add
            this.field.newDfp("1111111111111111.2222"),
            DfpField.FLAG_INEXACT, "Add #13");

        this.test(this.field.newDfp(".11111111111111111111").add(this.field.newDfp("11111111111111111111")), // rounding
                                                                                                             // on add
            this.field.newDfp("11111111111111111111"),
            DfpField.FLAG_INEXACT, "Add #14");

        this.test(this.field.newDfp("9.9999999999999999999e131071").add(this.field.newDfp("-1e131052")), // overflow on
                                                                                                         // add
            this.field.newDfp("9.9999999999999999998e131071"),
            0, "Add #15");

        this.test(this.field.newDfp("9.9999999999999999999e131071").add(this.field.newDfp("1e131052")), // overflow on
                                                                                                        // add
            this.pinf,
            DfpField.FLAG_OVERFLOW, "Add #16");

        this.test(this.field.newDfp("-9.9999999999999999999e131071").add(this.field.newDfp("-1e131052")), // overflow on
                                                                                                          // add
            this.ninf,
            DfpField.FLAG_OVERFLOW, "Add #17");

        this.test(this.field.newDfp("-9.9999999999999999999e131071").add(this.field.newDfp("1e131052")), // overflow on
                                                                                                         // add
            this.field.newDfp("-9.9999999999999999998e131071"),
            0, "Add #18");

        this.test(this.field.newDfp("1e-131072").add(this.field.newDfp("1e-131072")), // underflow on add
            this.field.newDfp("2e-131072"),
            0, "Add #19");

        this.test(this.field.newDfp("1.0000000000000001e-131057").add(this.field.newDfp("-1e-131057")), // underflow on
                                                                                                        // add
            this.field.newDfp("1e-131073"),
            DfpField.FLAG_UNDERFLOW, "Add #20");

        this.test(this.field.newDfp("1.1e-131072").add(this.field.newDfp("-1e-131072")), // underflow on add
            this.field.newDfp("1e-131073"),
            DfpField.FLAG_UNDERFLOW, "Add #21");

        this.test(this.field.newDfp("1.0000000000000001e-131072").add(this.field.newDfp("-1e-131072")), // underflow on
                                                                                                        // add
            this.field.newDfp("1e-131088"),
            DfpField.FLAG_UNDERFLOW, "Add #22");

        this.test(this.field.newDfp("1.0000000000000001e-131078").add(this.field.newDfp("-1e-131078")), // underflow on
                                                                                                        // add
            this.field.newDfp("0"),
            DfpField.FLAG_UNDERFLOW, "Add #23");

        this.test(this.field.newDfp("1.0").add(this.field.newDfp("-1e-20")), // loss of precision on alignment?
            this.field.newDfp("0.99999999999999999999"),
            0, "Add #23.1");

        this.test(this.field.newDfp("-0.99999999999999999999").add(this.field.newDfp("1")), // proper normalization?
            this.field.newDfp("0.00000000000000000001"),
            0, "Add #23.2");

        this.test(this.field.newDfp("1").add(this.field.newDfp("0")), // adding zeros
            this.field.newDfp("1"),
            0, "Add #24");

        this.test(this.field.newDfp("0").add(this.field.newDfp("0")), // adding zeros
            this.field.newDfp("0"),
            0, "Add #25");

        this.test(this.field.newDfp("-0").add(this.field.newDfp("0")), // adding zeros
            this.field.newDfp("0"),
            0, "Add #26");

        this.test(this.field.newDfp("0").add(this.field.newDfp("-0")), // adding zeros
            this.field.newDfp("0"),
            0, "Add #27");

        this.test(this.field.newDfp("-0").add(this.field.newDfp("-0")), // adding zeros
            this.field.newDfp("-0"),
            0, "Add #28");

        this.test(this.field.newDfp("1e-20").add(this.field.newDfp("0")), // adding zeros
            this.field.newDfp("1e-20"),
            0, "Add #29");

        this.test(this.field.newDfp("1e-40").add(this.field.newDfp("0")), // adding zeros
            this.field.newDfp("1e-40"),
            0, "Add #30");

        this.test(this.pinf.add(this.ninf), // adding infinities
            this.nan,
            DfpField.FLAG_INVALID, "Add #31");

        this.test(this.ninf.add(this.pinf), // adding infinities
            this.nan,
            DfpField.FLAG_INVALID, "Add #32");

        this.test(this.ninf.add(this.ninf), // adding infinities
            this.ninf,
            0, "Add #33");

        this.test(this.pinf.add(this.pinf), // adding infinities
            this.pinf,
            0, "Add #34");

        this.test(this.pinf.add(this.field.newDfp("0")), // adding infinities
            this.pinf,
            0, "Add #35");

        this.test(this.pinf.add(this.field.newDfp("-1e131071")), // adding infinities
            this.pinf,
            0, "Add #36");

        this.test(this.pinf.add(this.field.newDfp("1e131071")), // adding infinities
            this.pinf,
            0, "Add #37");

        this.test(this.field.newDfp("0").add(this.pinf), // adding infinities
            this.pinf,
            0, "Add #38");

        this.test(this.field.newDfp("-1e131071").add(this.pinf), // adding infinities
            this.pinf,
            0, "Add #39");

        this.test(this.field.newDfp("1e131071").add(this.pinf), // adding infinities
            this.pinf,
            0, "Add #40");

        this.test(this.ninf.add(this.field.newDfp("0")), // adding infinities
            this.ninf,
            0, "Add #41");

        this.test(this.ninf.add(this.field.newDfp("-1e131071")), // adding infinities
            this.ninf,
            0, "Add #42");

        this.test(this.ninf.add(this.field.newDfp("1e131071")), // adding infinities
            this.ninf,
            0, "Add #43");

        this.test(this.field.newDfp("0").add(this.ninf), // adding infinities
            this.ninf,
            0, "Add #44");

        this.test(this.field.newDfp("-1e131071").add(this.ninf), // adding infinities
            this.ninf,
            0, "Add #45");

        this.test(this.field.newDfp("1e131071").add(this.ninf), // adding infinities
            this.ninf,
            0, "Add #46");

        this.test(this.field.newDfp("9.9999999999999999999e131071").add(this.field.newDfp("5e131051")), // overflow
            this.pinf,
            DfpField.FLAG_OVERFLOW, "Add #47");

        this.test(
            this.field.newDfp("9.9999999999999999999e131071").add(this.field.newDfp("4.9999999999999999999e131051")), // overflow
            this.field.newDfp("9.9999999999999999999e131071"),
            DfpField.FLAG_INEXACT, "Add #48");

        this.test(this.nan.add(this.field.newDfp("1")),
            this.nan,
            0, "Add #49");

        this.test(this.field.newDfp("1").add(this.nan),
            this.nan,
            0, "Add #50");

        this.test(this.field.newDfp("12345678123456781234").add(this.field.newDfp("0.12345678123456781234")),
            this.field.newDfp("12345678123456781234"),
            DfpField.FLAG_INEXACT, "Add #51");

        this.test(this.field.newDfp("12345678123456781234").add(this.field.newDfp("123.45678123456781234")),
            this.field.newDfp("12345678123456781357"),
            DfpField.FLAG_INEXACT, "Add #52");

        this.test(this.field.newDfp("123.45678123456781234").add(this.field.newDfp("12345678123456781234")),
            this.field.newDfp("12345678123456781357"),
            DfpField.FLAG_INEXACT, "Add #53");

        this.test(this.field.newDfp("12345678123456781234").add(this.field.newDfp(".00001234567812345678")),
            this.field.newDfp("12345678123456781234"),
            DfpField.FLAG_INEXACT, "Add #54");

        this.test(this.field.newDfp("12345678123456781234").add(this.field.newDfp(".00000000123456781234")),
            this.field.newDfp("12345678123456781234"),
            DfpField.FLAG_INEXACT, "Add #55");

        this.test(this.field.newDfp("-0").add(this.field.newDfp("-0")),
            this.field.newDfp("-0"),
            0, "Add #56");

        this.test(this.field.newDfp("0").add(this.field.newDfp("-0")),
            this.field.newDfp("0"),
            0, "Add #57");

        this.test(this.field.newDfp("-0").add(this.field.newDfp("0")),
            this.field.newDfp("0"),
            0, "Add #58");

        this.test(this.field.newDfp("0").add(this.field.newDfp("0")),
            this.field.newDfp("0"),
            0, "Add #59");
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////

    // Test comparisons

    // utility function to help test comparisons
    private void cmptst(final Dfp a, final Dfp b, final String op, final boolean result, final double num)
    {
        if (op == "equal") {
            if (a.equals(b) != result) {
                Assert.fail("assersion failed.  " + op + " compare #" + num);
            }
        }

        if (op == "unequal") {
            if (a.unequal(b) != result) {
                Assert.fail("assersion failed.  " + op + " compare #" + num);
            }
        }

        if (op == "lessThan") {
            if (a.lessThan(b) != result) {
                Assert.fail("assersion failed.  " + op + " compare #" + num);
            }
        }

        if (op == "greaterThan") {
            if (a.greaterThan(b) != result) {
                Assert.fail("assersion failed.  " + op + " compare #" + num);
            }
        }
    }

    @Test
    public void testCompare()
    {
        // test equal() comparison
        // check zero vs. zero
        this.field.clearIEEEFlags();

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("0"), "equal", true, 1); // 0 == 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-0"), "equal", true, 2); // 0 == -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("-0"), "equal", true, 3); // -0 == -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("0"), "equal", true, 4); // -0 == 0

        // check zero vs normal numbers

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1"), "equal", false, 5); // 0 == 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("0"), "equal", false, 6); // 1 == 0
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("0"), "equal", false, 7); // -1 == 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-1"), "equal", false, 8); // 0 == -1
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131072"), "equal", false, 9); // 0 == 1e-131072
        // check flags
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131078"), "equal", false, 10); // 0 == 1e-131078

        // check flags -- underflow should be set
        if (this.field.getIEEEFlags() != DfpField.FLAG_UNDERFLOW) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.field.clearIEEEFlags();

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e+131071"), "equal", false, 11); // 0 == 1e+131071

        // check zero vs infinities

        this.cmptst(this.field.newDfp("0"), this.pinf, "equal", false, 12); // 0 == pinf
        this.cmptst(this.field.newDfp("0"), this.ninf, "equal", false, 13); // 0 == ninf
        this.cmptst(this.field.newDfp("-0"), this.pinf, "equal", false, 14); // -0 == pinf
        this.cmptst(this.field.newDfp("-0"), this.ninf, "equal", false, 15); // -0 == ninf
        this.cmptst(this.pinf, this.field.newDfp("0"), "equal", false, 16); // pinf == 0
        this.cmptst(this.ninf, this.field.newDfp("0"), "equal", false, 17); // ninf == 0
        this.cmptst(this.pinf, this.field.newDfp("-0"), "equal", false, 18); // pinf == -0
        this.cmptst(this.ninf, this.field.newDfp("-0"), "equal", false, 19); // ninf == -0
        this.cmptst(this.ninf, this.pinf, "equal", false, 19.10); // ninf == pinf
        this.cmptst(this.pinf, this.ninf, "equal", false, 19.11); // pinf == ninf
        this.cmptst(this.pinf, this.pinf, "equal", true, 19.12); // pinf == pinf
        this.cmptst(this.ninf, this.ninf, "equal", true, 19.13); // ninf == ninf

        // check some normal numbers
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1"), "equal", true, 20); // 1 == 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("-1"), "equal", false, 21); // 1 == -1
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("-1"), "equal", true, 22); // -1 == -1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1.0000000000000001"), "equal", false, 23); // 1 ==
                                                                                                          // 1.0000000000000001

        // The tests below checks to ensure that comparisons don't set FLAG_INEXACT
        // 100000 == 1.0000000000000001
        this.cmptst(this.field.newDfp("1e20"), this.field.newDfp("1.0000000000000001"), "equal", false, 24);
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0.000001"), this.field.newDfp("1e-6"), "equal", true, 25);

        // check some nans -- nans shouldnt equal anything

        this.cmptst(this.snan, this.snan, "equal", false, 27);
        this.cmptst(this.qnan, this.qnan, "equal", false, 28);
        this.cmptst(this.snan, this.qnan, "equal", false, 29);
        this.cmptst(this.qnan, this.snan, "equal", false, 30);
        this.cmptst(this.qnan, this.field.newDfp("0"), "equal", false, 31);
        this.cmptst(this.snan, this.field.newDfp("0"), "equal", false, 32);
        this.cmptst(this.field.newDfp("0"), this.snan, "equal", false, 33);
        this.cmptst(this.field.newDfp("0"), this.qnan, "equal", false, 34);
        this.cmptst(this.qnan, this.pinf, "equal", false, 35);
        this.cmptst(this.snan, this.pinf, "equal", false, 36);
        this.cmptst(this.pinf, this.snan, "equal", false, 37);
        this.cmptst(this.pinf, this.qnan, "equal", false, 38);
        this.cmptst(this.qnan, this.ninf, "equal", false, 39);
        this.cmptst(this.snan, this.ninf, "equal", false, 40);
        this.cmptst(this.ninf, this.snan, "equal", false, 41);
        this.cmptst(this.ninf, this.qnan, "equal", false, 42);
        this.cmptst(this.qnan, this.field.newDfp("-1"), "equal", false, 43);
        this.cmptst(this.snan, this.field.newDfp("-1"), "equal", false, 44);
        this.cmptst(this.field.newDfp("-1"), this.snan, "equal", false, 45);
        this.cmptst(this.field.newDfp("-1"), this.qnan, "equal", false, 46);
        this.cmptst(this.qnan, this.field.newDfp("1"), "equal", false, 47);
        this.cmptst(this.snan, this.field.newDfp("1"), "equal", false, 48);
        this.cmptst(this.field.newDfp("1"), this.snan, "equal", false, 49);
        this.cmptst(this.field.newDfp("1"), this.qnan, "equal", false, 50);
        this.cmptst(this.snan.negate(), this.snan, "equal", false, 51);
        this.cmptst(this.qnan.negate(), this.qnan, "equal", false, 52);

        //
        // Tests for un equal -- do it all over again
        //

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("0"), "unequal", false, 1); // 0 == 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-0"), "unequal", false, 2); // 0 == -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("-0"), "unequal", false, 3); // -0 == -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("0"), "unequal", false, 4); // -0 == 0

        // check zero vs normal numbers

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1"), "unequal", true, 5); // 0 == 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("0"), "unequal", true, 6); // 1 == 0
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("0"), "unequal", true, 7); // -1 == 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-1"), "unequal", true, 8); // 0 == -1
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131072"), "unequal", true, 9); // 0 == 1e-131072
        // check flags
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131078"), "unequal", true, 10); // 0 == 1e-131078

        // check flags -- underflow should be set
        if (this.field.getIEEEFlags() != DfpField.FLAG_UNDERFLOW) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.field.clearIEEEFlags();

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e+131071"), "unequal", true, 11); // 0 == 1e+131071

        // check zero vs infinities

        this.cmptst(this.field.newDfp("0"), this.pinf, "unequal", true, 12); // 0 == pinf
        this.cmptst(this.field.newDfp("0"), this.ninf, "unequal", true, 13); // 0 == ninf
        this.cmptst(this.field.newDfp("-0"), this.pinf, "unequal", true, 14); // -0 == pinf
        this.cmptst(this.field.newDfp("-0"), this.ninf, "unequal", true, 15); // -0 == ninf
        this.cmptst(this.pinf, this.field.newDfp("0"), "unequal", true, 16); // pinf == 0
        this.cmptst(this.ninf, this.field.newDfp("0"), "unequal", true, 17); // ninf == 0
        this.cmptst(this.pinf, this.field.newDfp("-0"), "unequal", true, 18); // pinf == -0
        this.cmptst(this.ninf, this.field.newDfp("-0"), "unequal", true, 19); // ninf == -0
        this.cmptst(this.ninf, this.pinf, "unequal", true, 19.10); // ninf == pinf
        this.cmptst(this.pinf, this.ninf, "unequal", true, 19.11); // pinf == ninf
        this.cmptst(this.pinf, this.pinf, "unequal", false, 19.12); // pinf == pinf
        this.cmptst(this.ninf, this.ninf, "unequal", false, 19.13); // ninf == ninf

        // check some normal numbers
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1"), "unequal", false, 20); // 1 == 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("-1"), "unequal", true, 21); // 1 == -1
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("-1"), "unequal", false, 22); // -1 == -1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1.0000000000000001"), "unequal", true, 23); // 1 ==
                                                                                                           // 1.0000000000000001

        // The tests below checks to ensure that comparisons don't set FLAG_INEXACT
        // 100000 == 1.0000000000000001
        this.cmptst(this.field.newDfp("1e20"), this.field.newDfp("1.0000000000000001"), "unequal", true, 24);
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0.000001"), this.field.newDfp("1e-6"), "unequal", false, 25);

        // check some nans -- nans shouldnt be unequal to anything

        this.cmptst(this.snan, this.snan, "unequal", false, 27);
        this.cmptst(this.qnan, this.qnan, "unequal", false, 28);
        this.cmptst(this.snan, this.qnan, "unequal", false, 29);
        this.cmptst(this.qnan, this.snan, "unequal", false, 30);
        this.cmptst(this.qnan, this.field.newDfp("0"), "unequal", false, 31);
        this.cmptst(this.snan, this.field.newDfp("0"), "unequal", false, 32);
        this.cmptst(this.field.newDfp("0"), this.snan, "unequal", false, 33);
        this.cmptst(this.field.newDfp("0"), this.qnan, "unequal", false, 34);
        this.cmptst(this.qnan, this.pinf, "unequal", false, 35);
        this.cmptst(this.snan, this.pinf, "unequal", false, 36);
        this.cmptst(this.pinf, this.snan, "unequal", false, 37);
        this.cmptst(this.pinf, this.qnan, "unequal", false, 38);
        this.cmptst(this.qnan, this.ninf, "unequal", false, 39);
        this.cmptst(this.snan, this.ninf, "unequal", false, 40);
        this.cmptst(this.ninf, this.snan, "unequal", false, 41);
        this.cmptst(this.ninf, this.qnan, "unequal", false, 42);
        this.cmptst(this.qnan, this.field.newDfp("-1"), "unequal", false, 43);
        this.cmptst(this.snan, this.field.newDfp("-1"), "unequal", false, 44);
        this.cmptst(this.field.newDfp("-1"), this.snan, "unequal", false, 45);
        this.cmptst(this.field.newDfp("-1"), this.qnan, "unequal", false, 46);
        this.cmptst(this.qnan, this.field.newDfp("1"), "unequal", false, 47);
        this.cmptst(this.snan, this.field.newDfp("1"), "unequal", false, 48);
        this.cmptst(this.field.newDfp("1"), this.snan, "unequal", false, 49);
        this.cmptst(this.field.newDfp("1"), this.qnan, "unequal", false, 50);
        this.cmptst(this.snan.negate(), this.snan, "unequal", false, 51);
        this.cmptst(this.qnan.negate(), this.qnan, "unequal", false, 52);

        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare unequal flags = " + this.field.getIEEEFlags());
        }

        //
        // Tests for lessThan -- do it all over again
        //

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("0"), "lessThan", false, 1); // 0 < 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-0"), "lessThan", false, 2); // 0 < -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("-0"), "lessThan", false, 3); // -0 < -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("0"), "lessThan", false, 4); // -0 < 0

        // check zero vs normal numbers

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1"), "lessThan", true, 5); // 0 < 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("0"), "lessThan", false, 6); // 1 < 0
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("0"), "lessThan", true, 7); // -1 < 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-1"), "lessThan", false, 8); // 0 < -1
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131072"), "lessThan", true, 9); // 0 < 1e-131072
        // check flags
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131078"), "lessThan", true, 10); // 0 < 1e-131078

        // check flags -- underflow should be set
        if (this.field.getIEEEFlags() != DfpField.FLAG_UNDERFLOW) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }
        this.field.clearIEEEFlags();

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e+131071"), "lessThan", true, 11); // 0 < 1e+131071

        // check zero vs infinities

        this.cmptst(this.field.newDfp("0"), this.pinf, "lessThan", true, 12); // 0 < pinf
        this.cmptst(this.field.newDfp("0"), this.ninf, "lessThan", false, 13); // 0 < ninf
        this.cmptst(this.field.newDfp("-0"), this.pinf, "lessThan", true, 14); // -0 < pinf
        this.cmptst(this.field.newDfp("-0"), this.ninf, "lessThan", false, 15); // -0 < ninf
        this.cmptst(this.pinf, this.field.newDfp("0"), "lessThan", false, 16); // pinf < 0
        this.cmptst(this.ninf, this.field.newDfp("0"), "lessThan", true, 17); // ninf < 0
        this.cmptst(this.pinf, this.field.newDfp("-0"), "lessThan", false, 18); // pinf < -0
        this.cmptst(this.ninf, this.field.newDfp("-0"), "lessThan", true, 19); // ninf < -0
        this.cmptst(this.ninf, this.pinf, "lessThan", true, 19.10); // ninf < pinf
        this.cmptst(this.pinf, this.ninf, "lessThan", false, 19.11); // pinf < ninf
        this.cmptst(this.pinf, this.pinf, "lessThan", false, 19.12); // pinf < pinf
        this.cmptst(this.ninf, this.ninf, "lessThan", false, 19.13); // ninf < ninf

        // check some normal numbers
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1"), "lessThan", false, 20); // 1 < 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("-1"), "lessThan", false, 21); // 1 < -1
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("-1"), "lessThan", false, 22); // -1 < -1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1.0000000000000001"), "lessThan", true, 23); // 1 <
                                                                                                            // 1.0000000000000001

        // The tests below checks to ensure that comparisons don't set FLAG_INEXACT
        // 100000 < 1.0000000000000001
        this.cmptst(this.field.newDfp("1e20"), this.field.newDfp("1.0000000000000001"), "lessThan", false, 24);
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0.000001"), this.field.newDfp("1e-6"), "lessThan", false, 25);

        // check some nans -- nans shouldnt be lessThan to anything
        this.cmptst(this.snan, this.snan, "lessThan", false, 27);
        this.cmptst(this.qnan, this.qnan, "lessThan", false, 28);
        this.cmptst(this.snan, this.qnan, "lessThan", false, 29);
        this.cmptst(this.qnan, this.snan, "lessThan", false, 30);
        this.cmptst(this.qnan, this.field.newDfp("0"), "lessThan", false, 31);
        this.cmptst(this.snan, this.field.newDfp("0"), "lessThan", false, 32);
        this.cmptst(this.field.newDfp("0"), this.snan, "lessThan", false, 33);
        this.cmptst(this.field.newDfp("0"), this.qnan, "lessThan", false, 34);
        this.cmptst(this.qnan, this.pinf, "lessThan", false, 35);
        this.cmptst(this.snan, this.pinf, "lessThan", false, 36);
        this.cmptst(this.pinf, this.snan, "lessThan", false, 37);
        this.cmptst(this.pinf, this.qnan, "lessThan", false, 38);
        this.cmptst(this.qnan, this.ninf, "lessThan", false, 39);
        this.cmptst(this.snan, this.ninf, "lessThan", false, 40);
        this.cmptst(this.ninf, this.snan, "lessThan", false, 41);
        this.cmptst(this.ninf, this.qnan, "lessThan", false, 42);
        this.cmptst(this.qnan, this.field.newDfp("-1"), "lessThan", false, 43);
        this.cmptst(this.snan, this.field.newDfp("-1"), "lessThan", false, 44);
        this.cmptst(this.field.newDfp("-1"), this.snan, "lessThan", false, 45);
        this.cmptst(this.field.newDfp("-1"), this.qnan, "lessThan", false, 46);
        this.cmptst(this.qnan, this.field.newDfp("1"), "lessThan", false, 47);
        this.cmptst(this.snan, this.field.newDfp("1"), "lessThan", false, 48);
        this.cmptst(this.field.newDfp("1"), this.snan, "lessThan", false, 49);
        this.cmptst(this.field.newDfp("1"), this.qnan, "lessThan", false, 50);
        this.cmptst(this.snan.negate(), this.snan, "lessThan", false, 51);
        this.cmptst(this.qnan.negate(), this.qnan, "lessThan", false, 52);

        // lessThan compares with nans should raise FLAG_INVALID
        if (this.field.getIEEEFlags() != DfpField.FLAG_INVALID) {
            Assert.fail("assersion failed.  compare lessThan flags = " + this.field.getIEEEFlags());
        }
        this.field.clearIEEEFlags();

        //
        // Tests for greaterThan -- do it all over again
        //

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("0"), "greaterThan", false, 1); // 0 > 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-0"), "greaterThan", false, 2); // 0 > -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("-0"), "greaterThan", false, 3); // -0 > -0
        this.cmptst(this.field.newDfp("-0"), this.field.newDfp("0"), "greaterThan", false, 4); // -0 > 0

        // check zero vs normal numbers

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1"), "greaterThan", false, 5); // 0 > 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("0"), "greaterThan", true, 6); // 1 > 0
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("0"), "greaterThan", false, 7); // -1 > 0
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("-1"), "greaterThan", true, 8); // 0 > -1
        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131072"), "greaterThan", false, 9); // 0 > 1e-131072
        // check flags
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e-131078"), "greaterThan", false, 10); // 0 > 1e-131078

        // check flags -- underflow should be set
        if (this.field.getIEEEFlags() != DfpField.FLAG_UNDERFLOW) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }
        this.field.clearIEEEFlags();

        this.cmptst(this.field.newDfp("0"), this.field.newDfp("1e+131071"), "greaterThan", false, 11); // 0 > 1e+131071

        // check zero vs infinities

        this.cmptst(this.field.newDfp("0"), this.pinf, "greaterThan", false, 12); // 0 > pinf
        this.cmptst(this.field.newDfp("0"), this.ninf, "greaterThan", true, 13); // 0 > ninf
        this.cmptst(this.field.newDfp("-0"), this.pinf, "greaterThan", false, 14); // -0 > pinf
        this.cmptst(this.field.newDfp("-0"), this.ninf, "greaterThan", true, 15); // -0 > ninf
        this.cmptst(this.pinf, this.field.newDfp("0"), "greaterThan", true, 16); // pinf > 0
        this.cmptst(this.ninf, this.field.newDfp("0"), "greaterThan", false, 17); // ninf > 0
        this.cmptst(this.pinf, this.field.newDfp("-0"), "greaterThan", true, 18); // pinf > -0
        this.cmptst(this.ninf, this.field.newDfp("-0"), "greaterThan", false, 19); // ninf > -0
        this.cmptst(this.ninf, this.pinf, "greaterThan", false, 19.10); // ninf > pinf
        this.cmptst(this.pinf, this.ninf, "greaterThan", true, 19.11); // pinf > ninf
        this.cmptst(this.pinf, this.pinf, "greaterThan", false, 19.12); // pinf > pinf
        this.cmptst(this.ninf, this.ninf, "greaterThan", false, 19.13); // ninf > ninf

        // check some normal numbers
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1"), "greaterThan", false, 20); // 1 > 1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("-1"), "greaterThan", true, 21); // 1 > -1
        this.cmptst(this.field.newDfp("-1"), this.field.newDfp("-1"), "greaterThan", false, 22); // -1 > -1
        this.cmptst(this.field.newDfp("1"), this.field.newDfp("1.0000000000000001"), "greaterThan", false, 23); // 1 >
        // 1.0000000000000001

        // The tests below checks to ensure that comparisons don't set FLAG_INEXACT
        // 100000 > 1.0000000000000001
        this.cmptst(this.field.newDfp("1e20"), this.field.newDfp("1.0000000000000001"), "greaterThan", true, 24);
        if (this.field.getIEEEFlags() != 0) {
            Assert.fail("assersion failed.  compare flags = " + this.field.getIEEEFlags());
        }

        this.cmptst(this.field.newDfp("0.000001"), this.field.newDfp("1e-6"), "greaterThan", false, 25);

        // check some nans -- nans shouldnt be greaterThan to anything
        this.cmptst(this.snan, this.snan, "greaterThan", false, 27);
        this.cmptst(this.qnan, this.qnan, "greaterThan", false, 28);
        this.cmptst(this.snan, this.qnan, "greaterThan", false, 29);
        this.cmptst(this.qnan, this.snan, "greaterThan", false, 30);
        this.cmptst(this.qnan, this.field.newDfp("0"), "greaterThan", false, 31);
        this.cmptst(this.snan, this.field.newDfp("0"), "greaterThan", false, 32);
        this.cmptst(this.field.newDfp("0"), this.snan, "greaterThan", false, 33);
        this.cmptst(this.field.newDfp("0"), this.qnan, "greaterThan", false, 34);
        this.cmptst(this.qnan, this.pinf, "greaterThan", false, 35);
        this.cmptst(this.snan, this.pinf, "greaterThan", false, 36);
        this.cmptst(this.pinf, this.snan, "greaterThan", false, 37);
        this.cmptst(this.pinf, this.qnan, "greaterThan", false, 38);
        this.cmptst(this.qnan, this.ninf, "greaterThan", false, 39);
        this.cmptst(this.snan, this.ninf, "greaterThan", false, 40);
        this.cmptst(this.ninf, this.snan, "greaterThan", false, 41);
        this.cmptst(this.ninf, this.qnan, "greaterThan", false, 42);
        this.cmptst(this.qnan, this.field.newDfp("-1"), "greaterThan", false, 43);
        this.cmptst(this.snan, this.field.newDfp("-1"), "greaterThan", false, 44);
        this.cmptst(this.field.newDfp("-1"), this.snan, "greaterThan", false, 45);
        this.cmptst(this.field.newDfp("-1"), this.qnan, "greaterThan", false, 46);
        this.cmptst(this.qnan, this.field.newDfp("1"), "greaterThan", false, 47);
        this.cmptst(this.snan, this.field.newDfp("1"), "greaterThan", false, 48);
        this.cmptst(this.field.newDfp("1"), this.snan, "greaterThan", false, 49);
        this.cmptst(this.field.newDfp("1"), this.qnan, "greaterThan", false, 50);
        this.cmptst(this.snan.negate(), this.snan, "greaterThan", false, 51);
        this.cmptst(this.qnan.negate(), this.qnan, "greaterThan", false, 52);

        // greaterThan compares with nans should raise FLAG_INVALID
        if (this.field.getIEEEFlags() != DfpField.FLAG_INVALID) {
            Assert.fail("assersion failed.  compare greaterThan flags = " + this.field.getIEEEFlags());
        }
        this.field.clearIEEEFlags();
    }

    //
    // Test multiplication
    //
    @Test
    public void testMultiply()
    {
        this.test(this.field.newDfp("1").multiply(this.field.newDfp("1")), // Basic tests 1*1 = 1
            this.field.newDfp("1"),
            0, "Multiply #1");

        this.test(this.field.newDfp("1").multiply(1), // Basic tests 1*1 = 1
            this.field.newDfp("1"),
            0, "Multiply #2");

        this.test(this.field.newDfp("-1").multiply(this.field.newDfp("1")), // Basic tests -1*1 = -1
            this.field.newDfp("-1"),
            0, "Multiply #3");

        this.test(this.field.newDfp("-1").multiply(1), // Basic tests -1*1 = -1
            this.field.newDfp("-1"),
            0, "Multiply #4");

        // basic tests with integers
        this.test(this.field.newDfp("2").multiply(this.field.newDfp("3")),
            this.field.newDfp("6"),
            0, "Multiply #5");

        this.test(this.field.newDfp("2").multiply(3),
            this.field.newDfp("6"),
            0, "Multiply #6");

        this.test(this.field.newDfp("-2").multiply(this.field.newDfp("3")),
            this.field.newDfp("-6"),
            0, "Multiply #7");

        this.test(this.field.newDfp("-2").multiply(3),
            this.field.newDfp("-6"),
            0, "Multiply #8");

        this.test(this.field.newDfp("2").multiply(this.field.newDfp("-3")),
            this.field.newDfp("-6"),
            0, "Multiply #9");

        this.test(this.field.newDfp("-2").multiply(this.field.newDfp("-3")),
            this.field.newDfp("6"),
            0, "Multiply #10");

        // multiply by zero

        this.test(this.field.newDfp("-2").multiply(this.field.newDfp("0")),
            this.field.newDfp("-0"),
            0, "Multiply #11");

        this.test(this.field.newDfp("-2").multiply(0),
            this.field.newDfp("-0"),
            0, "Multiply #12");

        this.test(this.field.newDfp("2").multiply(this.field.newDfp("0")),
            this.field.newDfp("0"),
            0, "Multiply #13");

        this.test(this.field.newDfp("2").multiply(0),
            this.field.newDfp("0"),
            0, "Multiply #14");

        this.test(this.field.newDfp("2").multiply(this.pinf),
            this.pinf,
            0, "Multiply #15");

        this.test(this.field.newDfp("2").multiply(this.ninf),
            this.ninf,
            0, "Multiply #16");

        this.test(this.field.newDfp("-2").multiply(this.pinf),
            this.ninf,
            0, "Multiply #17");

        this.test(this.field.newDfp("-2").multiply(this.ninf),
            this.pinf,
            0, "Multiply #18");

        this.test(this.ninf.multiply(this.field.newDfp("-2")),
            this.pinf,
            0, "Multiply #18.1");

        this.test(this.field.newDfp("5e131071").multiply(2),
            this.pinf,
            DfpField.FLAG_OVERFLOW, "Multiply #19");

        this.test(this.field.newDfp("5e131071").multiply(this.field.newDfp("1.999999999999999")),
            this.field.newDfp("9.9999999999999950000e131071"),
            0, "Multiply #20");

        this.test(this.field.newDfp("-5e131071").multiply(2),
            this.ninf,
            DfpField.FLAG_OVERFLOW, "Multiply #22");

        this.test(this.field.newDfp("-5e131071").multiply(this.field.newDfp("1.999999999999999")),
            this.field.newDfp("-9.9999999999999950000e131071"),
            0, "Multiply #23");

        this.test(this.field.newDfp("1e-65539").multiply(this.field.newDfp("1e-65539")),
            this.field.newDfp("1e-131078"),
            DfpField.FLAG_UNDERFLOW, "Multiply #24");

        this.test(this.field.newDfp("1").multiply(this.nan),
            this.nan,
            0, "Multiply #25");

        this.test(this.nan.multiply(this.field.newDfp("1")),
            this.nan,
            0, "Multiply #26");

        this.test(this.nan.multiply(this.pinf),
            this.nan,
            0, "Multiply #27");

        this.test(this.pinf.multiply(this.nan),
            this.nan,
            0, "Multiply #27");

        this.test(this.pinf.multiply(this.field.newDfp("0")),
            this.nan,
            DfpField.FLAG_INVALID, "Multiply #28");

        this.test(this.field.newDfp("0").multiply(this.pinf),
            this.nan,
            DfpField.FLAG_INVALID, "Multiply #29");

        this.test(this.pinf.multiply(this.pinf),
            this.pinf,
            0, "Multiply #30");

        this.test(this.ninf.multiply(this.pinf),
            this.ninf,
            0, "Multiply #31");

        this.test(this.pinf.multiply(this.ninf),
            this.ninf,
            0, "Multiply #32");

        this.test(this.ninf.multiply(this.ninf),
            this.pinf,
            0, "Multiply #33");

        this.test(this.pinf.multiply(1),
            this.pinf,
            0, "Multiply #34");

        this.test(this.pinf.multiply(0),
            this.nan,
            DfpField.FLAG_INVALID, "Multiply #35");

        this.test(this.nan.multiply(1),
            this.nan,
            0, "Multiply #36");

        this.test(this.field.newDfp("1").multiply(10000),
            this.field.newDfp("10000"),
            0, "Multiply #37");

        this.test(this.field.newDfp("2").multiply(1000000),
            this.field.newDfp("2000000"),
            0, "Multiply #38");

        this.test(this.field.newDfp("1").multiply(-1),
            this.field.newDfp("-1"),
            0, "Multiply #39");
    }

    @Test
    public void testDivide()
    {
        this.test(this.field.newDfp("1").divide(this.nan), // divide by NaN = NaN
            this.nan,
            0, "Divide #1");

        this.test(this.nan.divide(this.field.newDfp("1")), // NaN / number = NaN
            this.nan,
            0, "Divide #2");

        this.test(this.pinf.divide(this.field.newDfp("1")),
            this.pinf,
            0, "Divide #3");

        this.test(this.pinf.divide(this.field.newDfp("-1")),
            this.ninf,
            0, "Divide #4");

        this.test(this.pinf.divide(this.pinf),
            this.nan,
            DfpField.FLAG_INVALID, "Divide #5");

        this.test(this.ninf.divide(this.pinf),
            this.nan,
            DfpField.FLAG_INVALID, "Divide #6");

        this.test(this.pinf.divide(this.ninf),
            this.nan,
            DfpField.FLAG_INVALID, "Divide #7");

        this.test(this.ninf.divide(this.ninf),
            this.nan,
            DfpField.FLAG_INVALID, "Divide #8");

        this.test(this.field.newDfp("0").divide(this.field.newDfp("0")),
            this.nan,
            DfpField.FLAG_DIV_ZERO, "Divide #9");

        this.test(this.field.newDfp("1").divide(this.field.newDfp("0")),
            this.pinf,
            DfpField.FLAG_DIV_ZERO, "Divide #10");

        this.test(this.field.newDfp("1").divide(this.field.newDfp("-0")),
            this.ninf,
            DfpField.FLAG_DIV_ZERO, "Divide #11");

        this.test(this.field.newDfp("-1").divide(this.field.newDfp("0")),
            this.ninf,
            DfpField.FLAG_DIV_ZERO, "Divide #12");

        this.test(this.field.newDfp("-1").divide(this.field.newDfp("-0")),
            this.pinf,
            DfpField.FLAG_DIV_ZERO, "Divide #13");

        this.test(this.field.newDfp("1").divide(this.field.newDfp("3")),
            this.field.newDfp("0.33333333333333333333"),
            DfpField.FLAG_INEXACT, "Divide #14");

        this.test(this.field.newDfp("1").divide(this.field.newDfp("6")),
            this.field.newDfp("0.16666666666666666667"),
            DfpField.FLAG_INEXACT, "Divide #15");

        this.test(this.field.newDfp("10").divide(this.field.newDfp("6")),
            this.field.newDfp("1.6666666666666667"),
            DfpField.FLAG_INEXACT, "Divide #16");

        this.test(this.field.newDfp("100").divide(this.field.newDfp("6")),
            this.field.newDfp("16.6666666666666667"),
            DfpField.FLAG_INEXACT, "Divide #17");

        this.test(this.field.newDfp("1000").divide(this.field.newDfp("6")),
            this.field.newDfp("166.6666666666666667"),
            DfpField.FLAG_INEXACT, "Divide #18");

        this.test(this.field.newDfp("10000").divide(this.field.newDfp("6")),
            this.field.newDfp("1666.6666666666666667"),
            DfpField.FLAG_INEXACT, "Divide #19");

        this.test(this.field.newDfp("1").divide(this.field.newDfp("1")),
            this.field.newDfp("1"),
            0, "Divide #20");

        this.test(this.field.newDfp("1").divide(this.field.newDfp("-1")),
            this.field.newDfp("-1"),
            0, "Divide #21");

        this.test(this.field.newDfp("-1").divide(this.field.newDfp("1")),
            this.field.newDfp("-1"),
            0, "Divide #22");

        this.test(this.field.newDfp("-1").divide(this.field.newDfp("-1")),
            this.field.newDfp("1"),
            0, "Divide #23");

        this.test(this.field.newDfp("1e-65539").divide(this.field.newDfp("1e65539")),
            this.field.newDfp("1e-131078"),
            DfpField.FLAG_UNDERFLOW, "Divide #24");

        this.test(this.field.newDfp("1e65539").divide(this.field.newDfp("1e-65539")),
            this.pinf,
            DfpField.FLAG_OVERFLOW, "Divide #24");

        this.test(this.field.newDfp("2").divide(this.field.newDfp("1.5")), // test trial-divisor too high
            this.field.newDfp("1.3333333333333333"),
            DfpField.FLAG_INEXACT, "Divide #25");

        this.test(this.field.newDfp("2").divide(this.pinf),
            this.field.newDfp("0"),
            0, "Divide #26");

        this.test(this.field.newDfp("2").divide(this.ninf),
            this.field.newDfp("-0"),
            0, "Divide #27");

        this.test(this.field.newDfp("0").divide(this.field.newDfp("1")),
            this.field.newDfp("0"),
            0, "Divide #28");
    }

    @Test
    public void testReciprocal()
    {
        this.test(this.nan.reciprocal(),
            this.nan,
            0, "Reciprocal #1");

        this.test(this.field.newDfp("0").reciprocal(),
            this.pinf,
            DfpField.FLAG_DIV_ZERO, "Reciprocal #2");

        this.test(this.field.newDfp("-0").reciprocal(),
            this.ninf,
            DfpField.FLAG_DIV_ZERO, "Reciprocal #3");

        this.test(this.field.newDfp("3").reciprocal(),
            this.field.newDfp("0.33333333333333333333"),
            DfpField.FLAG_INEXACT, "Reciprocal #4");

        this.test(this.field.newDfp("6").reciprocal(),
            this.field.newDfp("0.16666666666666666667"),
            DfpField.FLAG_INEXACT, "Reciprocal #5");

        this.test(this.field.newDfp("1").reciprocal(),
            this.field.newDfp("1"),
            0, "Reciprocal #6");

        this.test(this.field.newDfp("-1").reciprocal(),
            this.field.newDfp("-1"),
            0, "Reciprocal #7");

        this.test(this.pinf.reciprocal(),
            this.field.newDfp("0"),
            0, "Reciprocal #8");

        this.test(this.ninf.reciprocal(),
            this.field.newDfp("-0"),
            0, "Reciprocal #9");
    }

    @Test
    public void testDivideInt()
    {
        this.test(this.nan.divide(1), // NaN / number = NaN
            this.nan,
            0, "DivideInt #1");

        this.test(this.pinf.divide(1),
            this.pinf,
            0, "DivideInt #2");

        this.test(this.field.newDfp("0").divide(0),
            this.nan,
            DfpField.FLAG_DIV_ZERO, "DivideInt #3");

        this.test(this.field.newDfp("1").divide(0),
            this.pinf,
            DfpField.FLAG_DIV_ZERO, "DivideInt #4");

        this.test(this.field.newDfp("-1").divide(0),
            this.ninf,
            DfpField.FLAG_DIV_ZERO, "DivideInt #5");

        this.test(this.field.newDfp("1").divide(3),
            this.field.newDfp("0.33333333333333333333"),
            DfpField.FLAG_INEXACT, "DivideInt #6");

        this.test(this.field.newDfp("1").divide(6),
            this.field.newDfp("0.16666666666666666667"),
            DfpField.FLAG_INEXACT, "DivideInt #7");

        this.test(this.field.newDfp("10").divide(6),
            this.field.newDfp("1.6666666666666667"),
            DfpField.FLAG_INEXACT, "DivideInt #8");

        this.test(this.field.newDfp("100").divide(6),
            this.field.newDfp("16.6666666666666667"),
            DfpField.FLAG_INEXACT, "DivideInt #9");

        this.test(this.field.newDfp("1000").divide(6),
            this.field.newDfp("166.6666666666666667"),
            DfpField.FLAG_INEXACT, "DivideInt #10");

        this.test(this.field.newDfp("10000").divide(6),
            this.field.newDfp("1666.6666666666666667"),
            DfpField.FLAG_INEXACT, "DivideInt #20");

        this.test(this.field.newDfp("1").divide(1),
            this.field.newDfp("1"),
            0, "DivideInt #21");

        this.test(this.field.newDfp("1e-131077").divide(10),
            this.field.newDfp("1e-131078"),
            DfpField.FLAG_UNDERFLOW, "DivideInt #22");

        this.test(this.field.newDfp("0").divide(1),
            this.field.newDfp("0"),
            0, "DivideInt #23");

        this.test(this.field.newDfp("1").divide(10000),
            this.nan,
            DfpField.FLAG_INVALID, "DivideInt #24");

        this.test(this.field.newDfp("1").divide(-1),
            this.nan,
            DfpField.FLAG_INVALID, "DivideInt #25");
    }

    @Test
    public void testNextAfter()
    {
        this.test(this.field.newDfp("1").nextAfter(this.pinf),
            this.field.newDfp("1.0000000000000001"),
            0, "NextAfter #1");

        this.test(this.field.newDfp("1.0000000000000001").nextAfter(this.ninf),
            this.field.newDfp("1"),
            0, "NextAfter #1.5");

        this.test(this.field.newDfp("1").nextAfter(this.ninf),
            this.field.newDfp("0.99999999999999999999"),
            0, "NextAfter #2");

        this.test(this.field.newDfp("0.99999999999999999999").nextAfter(this.field.newDfp("2")),
            this.field.newDfp("1"),
            0, "NextAfter #3");

        this.test(this.field.newDfp("-1").nextAfter(this.ninf),
            this.field.newDfp("-1.0000000000000001"),
            0, "NextAfter #4");

        this.test(this.field.newDfp("-1").nextAfter(this.pinf),
            this.field.newDfp("-0.99999999999999999999"),
            0, "NextAfter #5");

        this.test(this.field.newDfp("-0.99999999999999999999").nextAfter(this.field.newDfp("-2")),
            this.field.newDfp("-1"),
            0, "NextAfter #6");

        this.test(this.field.newDfp("2").nextAfter(this.field.newDfp("2")),
            this.field.newDfp("2"),
            0, "NextAfter #7");

        this.test(this.field.newDfp("0").nextAfter(this.field.newDfp("0")),
            this.field.newDfp("0"),
            0, "NextAfter #8");

        this.test(this.field.newDfp("-2").nextAfter(this.field.newDfp("-2")),
            this.field.newDfp("-2"),
            0, "NextAfter #9");

        this.test(this.field.newDfp("0").nextAfter(this.field.newDfp("1")),
            this.field.newDfp("1e-131092"),
            DfpField.FLAG_UNDERFLOW, "NextAfter #10");

        this.test(this.field.newDfp("0").nextAfter(this.field.newDfp("-1")),
            this.field.newDfp("-1e-131092"),
            DfpField.FLAG_UNDERFLOW, "NextAfter #11");

        this.test(this.field.newDfp("-1e-131092").nextAfter(this.pinf),
            this.field.newDfp("-0"),
            DfpField.FLAG_UNDERFLOW | DfpField.FLAG_INEXACT, "Next After #12");

        this.test(this.field.newDfp("1e-131092").nextAfter(this.ninf),
            this.field.newDfp("0"),
            DfpField.FLAG_UNDERFLOW | DfpField.FLAG_INEXACT, "Next After #13");

        this.test(this.field.newDfp("9.9999999999999999999e131078").nextAfter(this.pinf),
            this.pinf,
            DfpField.FLAG_OVERFLOW | DfpField.FLAG_INEXACT, "Next After #14");
    }

    @Test
    public void testToString()
    {
        Assert.assertEquals("toString #1", "Infinity", this.pinf.toString());
        Assert.assertEquals("toString #2", "-Infinity", this.ninf.toString());
        Assert.assertEquals("toString #3", "NaN", this.nan.toString());
        Assert.assertEquals("toString #4", "NaN", this.field.newDfp((byte) 1, Dfp.QNAN).toString());
        Assert.assertEquals("toString #5", "NaN", this.field.newDfp((byte) 1, Dfp.SNAN).toString());
        Assert.assertEquals("toString #6", "1.2300000000000000e100", this.field.newDfp("1.23e100").toString());
        Assert.assertEquals("toString #7", "-1.2300000000000000e100", this.field.newDfp("-1.23e100").toString());
        Assert.assertEquals("toString #8", "12345678.1234", this.field.newDfp("12345678.1234").toString());
        Assert.assertEquals("toString #9", "0.00001234", this.field.newDfp("0.00001234").toString());
    }

    @Test
    public void testRound()
    {
        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_DOWN);

        // Round down
        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.9")),
            this.field.newDfp("12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #1");

        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.99999999")),
            this.field.newDfp("12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #2");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.99999999")),
            this.field.newDfp("-12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #3");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_UP);

        // Round up
        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.1")),
            this.field.newDfp("12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #4");

        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.0001")),
            this.field.newDfp("12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #5");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.1")),
            this.field.newDfp("-12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #6");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.0001")),
            this.field.newDfp("-12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #7");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_UP);

        // Round half up
        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.4999")),
            this.field.newDfp("12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #8");

        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.5000")),
            this.field.newDfp("12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #9");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.4999")),
            this.field.newDfp("-12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #10");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.5000")),
            this.field.newDfp("-12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #11");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_DOWN);

        // Round half down
        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.5001")),
            this.field.newDfp("12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #12");

        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.5000")),
            this.field.newDfp("12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #13");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.5001")),
            this.field.newDfp("-12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #14");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.5000")),
            this.field.newDfp("-12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #15");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_ODD);

        // Round half odd
        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.5000")),
            this.field.newDfp("12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #16");

        this.test(this.field.newDfp("12345678901234567891").add(this.field.newDfp("0.5000")),
            this.field.newDfp("12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #17");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.5000")),
            this.field.newDfp("-12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #18");

        this.test(this.field.newDfp("-12345678901234567891").add(this.field.newDfp("-0.5000")),
            this.field.newDfp("-12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #19");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_CEIL);

        // Round ceil
        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.0001")),
            this.field.newDfp("12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #20");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.9999")),
            this.field.newDfp("-12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #21");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_FLOOR);

        // Round floor
        this.test(this.field.newDfp("12345678901234567890").add(this.field.newDfp("0.9999")),
            this.field.newDfp("12345678901234567890"),
            DfpField.FLAG_INEXACT, "Round #22");

        this.test(this.field.newDfp("-12345678901234567890").add(this.field.newDfp("-0.0001")),
            this.field.newDfp("-12345678901234567891"),
            DfpField.FLAG_INEXACT, "Round #23");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_EVEN); // reset
    }

    @Test
    public void testCeil()
    {
        this.test(this.field.newDfp("1234.0000000000000001").ceil(),
            this.field.newDfp("1235"),
            DfpField.FLAG_INEXACT, "Ceil #1");
    }

    @Test
    public void testFloor()
    {
        this.test(this.field.newDfp("1234.9999999999999999").floor(),
            this.field.newDfp("1234"),
            DfpField.FLAG_INEXACT, "Floor #1");
    }

    @Test
    public void testRint()
    {
        this.test(this.field.newDfp("1234.50000000001").rint(),
            this.field.newDfp("1235"),
            DfpField.FLAG_INEXACT, "Rint #1");

        this.test(this.field.newDfp("1234.5000").rint(),
            this.field.newDfp("1234"),
            DfpField.FLAG_INEXACT, "Rint #2");

        this.test(this.field.newDfp("1235.5000").rint(),
            this.field.newDfp("1236"),
            DfpField.FLAG_INEXACT, "Rint #3");
    }

    @Test
    public void testCopySign()
    {
        this.test(Dfp.copysign(this.field.newDfp("1234."), this.field.newDfp("-1")),
            this.field.newDfp("-1234"),
            0, "CopySign #1");

        this.test(Dfp.copysign(this.field.newDfp("-1234."), this.field.newDfp("-1")),
            this.field.newDfp("-1234"),
            0, "CopySign #2");

        this.test(Dfp.copysign(this.field.newDfp("-1234."), this.field.newDfp("1")),
            this.field.newDfp("1234"),
            0, "CopySign #3");

        this.test(Dfp.copysign(this.field.newDfp("1234."), this.field.newDfp("1")),
            this.field.newDfp("1234"),
            0, "CopySign #4");
    }

    @Test
    public void testIntValue()
    {
        Assert.assertEquals("intValue #1", 1234, this.field.newDfp("1234").intValue());
        Assert.assertEquals("intValue #2", -1234, this.field.newDfp("-1234").intValue());
        Assert.assertEquals("intValue #3", 1234, this.field.newDfp("1234.5").intValue());
        Assert.assertEquals("intValue #4", 1235, this.field.newDfp("1234.500001").intValue());
        Assert.assertEquals("intValue #5", 2147483647, this.field.newDfp("1e1000").intValue());
        Assert.assertEquals("intValue #6", -2147483648, this.field.newDfp("-1e1000").intValue());
    }

    @Test
    public void testLog10K()
    {
        Assert.assertEquals("log10K #1", 1, this.field.newDfp("123456").log10K());
        Assert.assertEquals("log10K #2", 2, this.field.newDfp("123456789").log10K());
        Assert.assertEquals("log10K #3", 0, this.field.newDfp("2").log10K());
        Assert.assertEquals("log10K #3", 0, this.field.newDfp("1").log10K());
        Assert.assertEquals("log10K #4", -1, this.field.newDfp("0.1").log10K());
    }

    @Test
    public void testPower10K()
    {
        final Dfp d = this.field.newDfp();

        this.test(d.power10K(0), this.field.newDfp("1"), 0, "Power10 #1");
        this.test(d.power10K(1), this.field.newDfp("10000"), 0, "Power10 #2");
        this.test(d.power10K(2), this.field.newDfp("100000000"), 0, "Power10 #3");

        this.test(d.power10K(-1), this.field.newDfp("0.0001"), 0, "Power10 #4");
        this.test(d.power10K(-2), this.field.newDfp("0.00000001"), 0, "Power10 #5");
        this.test(d.power10K(-3), this.field.newDfp("0.000000000001"), 0, "Power10 #6");
    }

    @Test
    public void testLog10()
    {

        Assert.assertEquals("log10 #1", 1, this.field.newDfp("12").log10());
        Assert.assertEquals("log10 #2", 2, this.field.newDfp("123").log10());
        Assert.assertEquals("log10 #3", 3, this.field.newDfp("1234").log10());
        Assert.assertEquals("log10 #4", 4, this.field.newDfp("12345").log10());
        Assert.assertEquals("log10 #5", 5, this.field.newDfp("123456").log10());
        Assert.assertEquals("log10 #6", 6, this.field.newDfp("1234567").log10());
        Assert.assertEquals("log10 #6", 7, this.field.newDfp("12345678").log10());
        Assert.assertEquals("log10 #7", 8, this.field.newDfp("123456789").log10());
        Assert.assertEquals("log10 #8", 9, this.field.newDfp("1234567890").log10());
        Assert.assertEquals("log10 #9", 10, this.field.newDfp("12345678901").log10());
        Assert.assertEquals("log10 #10", 11, this.field.newDfp("123456789012").log10());
        Assert.assertEquals("log10 #11", 12, this.field.newDfp("1234567890123").log10());

        Assert.assertEquals("log10 #12", 0, this.field.newDfp("2").log10());
        Assert.assertEquals("log10 #13", 0, this.field.newDfp("1").log10());
        Assert.assertEquals("log10 #14", -1, this.field.newDfp("0.12").log10());
        Assert.assertEquals("log10 #15", -2, this.field.newDfp("0.012").log10());
    }

    @Test
    public void testPower10()
    {
        final Dfp d = this.field.newDfp();

        this.test(d.power10(0), this.field.newDfp("1"), 0, "Power10 #1");
        this.test(d.power10(1), this.field.newDfp("10"), 0, "Power10 #2");
        this.test(d.power10(2), this.field.newDfp("100"), 0, "Power10 #3");
        this.test(d.power10(3), this.field.newDfp("1000"), 0, "Power10 #4");
        this.test(d.power10(4), this.field.newDfp("10000"), 0, "Power10 #5");
        this.test(d.power10(5), this.field.newDfp("100000"), 0, "Power10 #6");
        this.test(d.power10(6), this.field.newDfp("1000000"), 0, "Power10 #7");
        this.test(d.power10(7), this.field.newDfp("10000000"), 0, "Power10 #8");
        this.test(d.power10(8), this.field.newDfp("100000000"), 0, "Power10 #9");
        this.test(d.power10(9), this.field.newDfp("1000000000"), 0, "Power10 #10");

        this.test(d.power10(-1), this.field.newDfp(".1"), 0, "Power10 #11");
        this.test(d.power10(-2), this.field.newDfp(".01"), 0, "Power10 #12");
        this.test(d.power10(-3), this.field.newDfp(".001"), 0, "Power10 #13");
        this.test(d.power10(-4), this.field.newDfp(".0001"), 0, "Power10 #14");
        this.test(d.power10(-5), this.field.newDfp(".00001"), 0, "Power10 #15");
        this.test(d.power10(-6), this.field.newDfp(".000001"), 0, "Power10 #16");
        this.test(d.power10(-7), this.field.newDfp(".0000001"), 0, "Power10 #17");
        this.test(d.power10(-8), this.field.newDfp(".00000001"), 0, "Power10 #18");
        this.test(d.power10(-9), this.field.newDfp(".000000001"), 0, "Power10 #19");
        this.test(d.power10(-10), this.field.newDfp(".0000000001"), 0, "Power10 #20");
    }

    @Test
    public void testRemainder()
    {
        this.test(this.field.newDfp("10").remainder(this.field.newDfp("3")),
            this.field.newDfp("1"),
            DfpField.FLAG_INEXACT, "Remainder #1");

        this.test(this.field.newDfp("9").remainder(this.field.newDfp("3")),
            this.field.newDfp("0"),
            0, "Remainder #2");

        this.test(this.field.newDfp("-9").remainder(this.field.newDfp("3")),
            this.field.newDfp("-0"),
            0, "Remainder #3");
    }

    @Test
    public void testSqrt()
    {
        this.test(this.field.newDfp("0").sqrt(),
            this.field.newDfp("0"),
            0, "Sqrt #1");

        this.test(this.field.newDfp("-0").sqrt(),
            this.field.newDfp("-0"),
            0, "Sqrt #2");

        this.test(this.field.newDfp("1").sqrt(),
            this.field.newDfp("1"),
            0, "Sqrt #3");

        this.test(this.field.newDfp("2").sqrt(),
            this.field.newDfp("1.4142135623730950"),
            DfpField.FLAG_INEXACT, "Sqrt #4");

        this.test(this.field.newDfp("3").sqrt(),
            this.field.newDfp("1.7320508075688773"),
            DfpField.FLAG_INEXACT, "Sqrt #5");

        this.test(this.field.newDfp("5").sqrt(),
            this.field.newDfp("2.2360679774997897"),
            DfpField.FLAG_INEXACT, "Sqrt #6");

        this.test(this.field.newDfp("500").sqrt(),
            this.field.newDfp("22.3606797749978970"),
            DfpField.FLAG_INEXACT, "Sqrt #6.2");

        this.test(this.field.newDfp("50000").sqrt(),
            this.field.newDfp("223.6067977499789696"),
            DfpField.FLAG_INEXACT, "Sqrt #6.3");

        this.test(this.field.newDfp("-1").sqrt(),
            this.nan,
            DfpField.FLAG_INVALID, "Sqrt #7");

        this.test(this.pinf.sqrt(),
            this.pinf,
            0, "Sqrt #8");

        this.test(this.field.newDfp((byte) 1, Dfp.QNAN).sqrt(),
            this.nan,
            0, "Sqrt #9");

        this.test(this.field.newDfp((byte) 1, Dfp.SNAN).sqrt(),
            this.nan,
            DfpField.FLAG_INVALID, "Sqrt #9");
    }

    @Test
    public void testIssue567() {
        final DfpField field = new DfpField(100);
        Assert.assertEquals(0.0, field.getZero().toDouble(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, field.newDfp(0.0).toDouble(), Precision.SAFE_MIN);
        Assert.assertEquals(-1, MathLib.copySign(1, field.newDfp(-0.0).toDouble()), Precision.EPSILON);
        Assert.assertEquals(+1, MathLib.copySign(1, field.newDfp(+0.0).toDouble()), Precision.EPSILON);
    }

    @Test
    public void testIsZero() {
        Assert.assertTrue(this.field.getZero().isZero());
        Assert.assertTrue(this.field.getZero().negate().isZero());
        Assert.assertTrue(this.field.newDfp(+0.0).isZero());
        Assert.assertTrue(this.field.newDfp(-0.0).isZero());
        Assert.assertFalse(this.field.newDfp(1.0e-90).isZero());
        Assert.assertFalse(this.nan.isZero());
        Assert.assertFalse(this.nan.negate().isZero());
        Assert.assertFalse(this.pinf.isZero());
        Assert.assertFalse(this.pinf.negate().isZero());
        Assert.assertFalse(this.ninf.isZero());
        Assert.assertFalse(this.ninf.negate().isZero());
    }

    @Test
    public void testSignPredicates() {

        Assert.assertTrue(this.field.getZero().negativeOrNull());
        Assert.assertTrue(this.field.getZero().positiveOrNull());
        Assert.assertFalse(this.field.getZero().strictlyNegative());
        Assert.assertFalse(this.field.getZero().strictlyPositive());

        Assert.assertTrue(this.field.getZero().negate().negativeOrNull());
        Assert.assertTrue(this.field.getZero().negate().positiveOrNull());
        Assert.assertFalse(this.field.getZero().negate().strictlyNegative());
        Assert.assertFalse(this.field.getZero().negate().strictlyPositive());

        Assert.assertFalse(this.field.getOne().negativeOrNull());
        Assert.assertTrue(this.field.getOne().positiveOrNull());
        Assert.assertFalse(this.field.getOne().strictlyNegative());
        Assert.assertTrue(this.field.getOne().strictlyPositive());

        Assert.assertTrue(this.field.getOne().negate().negativeOrNull());
        Assert.assertFalse(this.field.getOne().negate().positiveOrNull());
        Assert.assertTrue(this.field.getOne().negate().strictlyNegative());
        Assert.assertFalse(this.field.getOne().negate().strictlyPositive());

        Assert.assertFalse(this.nan.negativeOrNull());
        Assert.assertFalse(this.nan.positiveOrNull());
        Assert.assertFalse(this.nan.strictlyNegative());
        Assert.assertFalse(this.nan.strictlyPositive());

        Assert.assertFalse(this.nan.negate().negativeOrNull());
        Assert.assertFalse(this.nan.negate().positiveOrNull());
        Assert.assertFalse(this.nan.negate().strictlyNegative());
        Assert.assertFalse(this.nan.negate().strictlyPositive());

        Assert.assertFalse(this.pinf.negativeOrNull());
        Assert.assertTrue(this.pinf.positiveOrNull());
        Assert.assertFalse(this.pinf.strictlyNegative());
        Assert.assertTrue(this.pinf.strictlyPositive());

        Assert.assertTrue(this.pinf.negate().negativeOrNull());
        Assert.assertFalse(this.pinf.negate().positiveOrNull());
        Assert.assertTrue(this.pinf.negate().strictlyNegative());
        Assert.assertFalse(this.pinf.negate().strictlyPositive());

        Assert.assertTrue(this.ninf.negativeOrNull());
        Assert.assertFalse(this.ninf.positiveOrNull());
        Assert.assertTrue(this.ninf.strictlyNegative());
        Assert.assertFalse(this.ninf.strictlyPositive());

        Assert.assertFalse(this.ninf.negate().negativeOrNull());
        Assert.assertTrue(this.ninf.negate().positiveOrNull());
        Assert.assertFalse(this.ninf.negate().strictlyNegative());
        Assert.assertTrue(this.ninf.negate().strictlyPositive());

    }

}
