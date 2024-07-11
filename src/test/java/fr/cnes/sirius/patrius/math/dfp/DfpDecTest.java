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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.dfp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DfpDecTest {

    private DfpField field;
    private Dfp pinf;
    private Dfp ninf;

    @Before
    public void setUp() {
        // Some basic setup. Define some constants and clear the status flags
        this.field = new DfpField(20);
        this.pinf = new DfpDec(this.field, 1).divide(new DfpDec(this.field, 0));
        this.ninf = new DfpDec(this.field, -1).divide(new DfpDec(this.field, 0));
        this.ninf.getField().clearIEEEFlags();
    }

    @After
    public void tearDown() {
        this.field = null;
        this.pinf = null;
        this.ninf = null;
    }

    // Generic test function. Takes params x and y and tests them for
    // equality. Then checks the status flags against the flags argument.
    // If the test fail, it prints the desc string
    private void test(final Dfp x, final Dfp y, final int flags, final String desc) {
        boolean b = x.equals(y);

        if (!x.equals(y) && !x.unequal(y)) {
            b = (x.toString().equals(y.toString()));
        }

        if (x.equals(new DfpDec(this.field, 0))) {
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
    public void testRound()
    {
        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_EVEN);

        this.test(new DfpDec(this.field, "12345678901234567890"),
            new DfpDec(this.field, "12345678901234568000"),
            DfpField.FLAG_INEXACT, "Round #1");

        this.test(new DfpDec(this.field, "0.12345678901234567890"),
            new DfpDec(this.field, "0.12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #2");

        this.test(new DfpDec(this.field, "0.12345678901234567500"),
            new DfpDec(this.field, "0.12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #3");

        this.test(new DfpDec(this.field, "0.12345678901234568500"),
            new DfpDec(this.field, "0.12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #4");

        this.test(new DfpDec(this.field, "0.12345678901234568501"),
            new DfpDec(this.field, "0.12345678901234569"),
            DfpField.FLAG_INEXACT, "Round #5");

        this.test(new DfpDec(this.field, "0.12345678901234568499"),
            new DfpDec(this.field, "0.12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #6");

        this.test(new DfpDec(this.field, "1.2345678901234567890"),
            new DfpDec(this.field, "1.2345678901234568"),
            DfpField.FLAG_INEXACT, "Round #7");

        this.test(new DfpDec(this.field, "1.2345678901234567500"),
            new DfpDec(this.field, "1.2345678901234568"),
            DfpField.FLAG_INEXACT, "Round #8");

        this.test(new DfpDec(this.field, "1.2345678901234568500"),
            new DfpDec(this.field, "1.2345678901234568"),
            DfpField.FLAG_INEXACT, "Round #9");

        this.test(new DfpDec(this.field, "1.2345678901234568000").add(new DfpDec(this.field, ".0000000000000000501")),
            new DfpDec(this.field, "1.2345678901234569"),
            DfpField.FLAG_INEXACT, "Round #10");

        this.test(new DfpDec(this.field, "1.2345678901234568499"),
            new DfpDec(this.field, "1.2345678901234568"),
            DfpField.FLAG_INEXACT, "Round #11");

        this.test(new DfpDec(this.field, "12.345678901234567890"),
            new DfpDec(this.field, "12.345678901234568"),
            DfpField.FLAG_INEXACT, "Round #12");

        this.test(new DfpDec(this.field, "12.345678901234567500"),
            new DfpDec(this.field, "12.345678901234568"),
            DfpField.FLAG_INEXACT, "Round #13");

        this.test(new DfpDec(this.field, "12.345678901234568500"),
            new DfpDec(this.field, "12.345678901234568"),
            DfpField.FLAG_INEXACT, "Round #14");

        this.test(new DfpDec(this.field, "12.345678901234568").add(new DfpDec(this.field, ".000000000000000501")),
            new DfpDec(this.field, "12.345678901234569"),
            DfpField.FLAG_INEXACT, "Round #15");

        this.test(new DfpDec(this.field, "12.345678901234568499"),
            new DfpDec(this.field, "12.345678901234568"),
            DfpField.FLAG_INEXACT, "Round #16");

        this.test(new DfpDec(this.field, "123.45678901234567890"),
            new DfpDec(this.field, "123.45678901234568"),
            DfpField.FLAG_INEXACT, "Round #17");

        this.test(new DfpDec(this.field, "123.45678901234567500"),
            new DfpDec(this.field, "123.45678901234568"),
            DfpField.FLAG_INEXACT, "Round #18");

        this.test(new DfpDec(this.field, "123.45678901234568500"),
            new DfpDec(this.field, "123.45678901234568"),
            DfpField.FLAG_INEXACT, "Round #19");

        this.test(new DfpDec(this.field, "123.456789012345685").add(new DfpDec(this.field, ".00000000000000501")),
            new DfpDec(this.field, "123.45678901234569"),
            DfpField.FLAG_INEXACT, "Round #20");

        this.test(new DfpDec(this.field, "123.45678901234568499"),
            new DfpDec(this.field, "123.45678901234568"),
            DfpField.FLAG_INEXACT, "Round #21");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_DOWN);

        // Round down
        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.9")),
            new DfpDec(this.field, "12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #22");

        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.99999999")),
            new DfpDec(this.field, "12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #23");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.99999999")),
            new DfpDec(this.field, "-12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #24");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_UP);

        // Round up
        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.1")),
            new DfpDec(this.field, "12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #25");

        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.0001")),
            new DfpDec(this.field, "12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #26");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.1")),
            new DfpDec(this.field, "-12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #27");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.0001")),
            new DfpDec(this.field, "-12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #28");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "0")),
            new DfpDec(this.field, "-12345678901234567"),
            0, "Round #28.5");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_UP);

        // Round half up
        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.499999999999")),
            new DfpDec(this.field, "12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #29");

        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.50000001")),
            new DfpDec(this.field, "12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #30");

        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.5")),
            new DfpDec(this.field, "12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #30.5");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.499999999999")),
            new DfpDec(this.field, "-12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #31");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.50000001")),
            new DfpDec(this.field, "-12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #32");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_DOWN);

        // Round half down
        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.5001")),
            new DfpDec(this.field, "12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #33");

        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.5000")),
            new DfpDec(this.field, "12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #34");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.5001")),
            new DfpDec(this.field, "-12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #35");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.6")),
            new DfpDec(this.field, "-12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #35.5");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.5000")),
            new DfpDec(this.field, "-12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #36");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_ODD);

        // Round half odd
        this.test(new DfpDec(this.field, "12345678901234568").add(new DfpDec(this.field, "0.5000")),
            new DfpDec(this.field, "12345678901234569"),
            DfpField.FLAG_INEXACT, "Round #37");

        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.5000")),
            new DfpDec(this.field, "12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #38");

        this.test(new DfpDec(this.field, "-12345678901234568").add(new DfpDec(this.field, "-0.5000")),
            new DfpDec(this.field, "-12345678901234569"),
            DfpField.FLAG_INEXACT, "Round #39");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.5000")),
            new DfpDec(this.field, "-12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #40");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_CEIL);

        // Round ceil
        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.0001")),
            new DfpDec(this.field, "12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #41");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.9999")),
            new DfpDec(this.field, "-12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #42");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_FLOOR);

        // Round floor
        this.test(new DfpDec(this.field, "12345678901234567").add(new DfpDec(this.field, "0.9999")),
            new DfpDec(this.field, "12345678901234567"),
            DfpField.FLAG_INEXACT, "Round #43");

        this.test(new DfpDec(this.field, "-12345678901234567").add(new DfpDec(this.field, "-0.0001")),
            new DfpDec(this.field, "-12345678901234568"),
            DfpField.FLAG_INEXACT, "Round #44");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_EVEN); // reset
    }

    @Test
    public void testRoundDecimal10()
    {
        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_EVEN);

        this.test(new Decimal10(this.field, "1234567891234567890"),
            new Decimal10(this.field, "1234567891000000000"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #1");

        this.test(new Decimal10(this.field, "0.1234567891634567890"),
            new Decimal10(this.field, "0.1234567892"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #2");

        this.test(new Decimal10(this.field, "0.1234567891500000000"),
            new Decimal10(this.field, "0.1234567892"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #3");

        this.test(new Decimal10(this.field, "0.1234567890500"),
            new Decimal10(this.field, "0.1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #4");

        this.test(new Decimal10(this.field, "0.1234567890501"),
            new Decimal10(this.field, "0.1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #5");

        this.test(new Decimal10(this.field, "0.1234567890499"),
            new Decimal10(this.field, "0.1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #6");

        this.test(new Decimal10(this.field, "1.234567890890"),
            new Decimal10(this.field, "1.234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #7");

        this.test(new Decimal10(this.field, "1.234567891500"),
            new Decimal10(this.field, "1.234567892"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #8");

        this.test(new Decimal10(this.field, "1.234567890500"),
            new Decimal10(this.field, "1.234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #9");

        this.test(new Decimal10(this.field, "1.234567890000").add(new Decimal10(this.field, ".000000000501")),
            new Decimal10(this.field, "1.234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #10");

        this.test(new Decimal10(this.field, "1.234567890499"),
            new Decimal10(this.field, "1.234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #11");

        this.test(new Decimal10(this.field, "12.34567890890"),
            new Decimal10(this.field, "12.34567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #12");

        this.test(new Decimal10(this.field, "12.34567891500"),
            new Decimal10(this.field, "12.34567892"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #13");

        this.test(new Decimal10(this.field, "12.34567890500"),
            new Decimal10(this.field, "12.34567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #14");

        this.test(new Decimal10(this.field, "12.34567890").add(new Decimal10(this.field, ".00000000501")),
            new Decimal10(this.field, "12.34567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #15");

        this.test(new Decimal10(this.field, "12.34567890499"),
            new Decimal10(this.field, "12.34567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #16");

        this.test(new Decimal10(this.field, "123.4567890890"),
            new Decimal10(this.field, "123.4567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #17");

        this.test(new Decimal10(this.field, "123.4567891500"),
            new Decimal10(this.field, "123.4567892"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #18");

        this.test(new Decimal10(this.field, "123.4567890500"),
            new Decimal10(this.field, "123.4567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #19");

        this.test(new Decimal10(this.field, "123.4567890").add(new Decimal10(this.field, ".0000000501")),
            new Decimal10(this.field, "123.4567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #20");

        this.test(new Decimal10(this.field, "123.4567890499"),
            new Decimal10(this.field, "123.4567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #21");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_DOWN);

        // RoundDecimal10 down
        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.9")),
            new Decimal10(this.field, "1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #22");

        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.99999999")),
            new Decimal10(this.field, "1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #23");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.99999999")),
            new Decimal10(this.field, "-1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #24");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_UP);

        // RoundDecimal10 up
        this.test(new Decimal10(this.field, 1234567890).add(new Decimal10(this.field, "0.1")),
            new Decimal10(this.field, 1234567891l),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #25");

        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.0001")),
            new Decimal10(this.field, "1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #26");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.1")),
            new Decimal10(this.field, "-1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #27");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.0001")),
            new Decimal10(this.field, "-1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #28");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "0")),
            new Decimal10(this.field, "-1234567890"),
            0, "RoundDecimal10 #28.5");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_UP);

        // RoundDecimal10 half up
        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.4999999999")),
            new Decimal10(this.field, "1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #29");

        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.50000001")),
            new Decimal10(this.field, "1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #30");

        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.5")),
            new Decimal10(this.field, "1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #30.5");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.4999999999")),
            new Decimal10(this.field, "-1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #31");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.50000001")),
            new Decimal10(this.field, "-1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #32");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_DOWN);

        // RoundDecimal10 half down
        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.5001")),
            new Decimal10(this.field, "1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #33");

        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.5000")),
            new Decimal10(this.field, "1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #34");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.5001")),
            new Decimal10(this.field, "-1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #35");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.6")),
            new Decimal10(this.field, "-1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #35.5");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.5000")),
            new Decimal10(this.field, "-1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #36");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_ODD);

        // RoundDecimal10 half odd
        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.5000")),
            new Decimal10(this.field, "1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #37");

        this.test(new Decimal10(this.field, "1234567891").add(new Decimal10(this.field, "0.5000")),
            new Decimal10(this.field, "1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #38");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.5000")),
            new Decimal10(this.field, "-1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #39");

        this.test(new Decimal10(this.field, "-1234567891").add(new Decimal10(this.field, "-0.5000")),
            new Decimal10(this.field, "-1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #40");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_CEIL);

        // RoundDecimal10 ceil
        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.0001")),
            new Decimal10(this.field, "1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #41");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.9999")),
            new Decimal10(this.field, "-1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #42");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_FLOOR);

        // RoundDecimal10 floor
        this.test(new Decimal10(this.field, "1234567890").add(new Decimal10(this.field, "0.9999")),
            new Decimal10(this.field, "1234567890"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #43");

        this.test(new Decimal10(this.field, "-1234567890").add(new Decimal10(this.field, "-0.0001")),
            new Decimal10(this.field, "-1234567891"),
            DfpField.FLAG_INEXACT, "RoundDecimal10 #44");

        this.field.setRoundingMode(DfpField.RoundingMode.ROUND_HALF_EVEN); // reset
    }

    @Test
    public void testNextAfter()
    {
        this.test(new DfpDec(this.field, 1).nextAfter(this.pinf),
            new DfpDec(this.field, "1.0000000000000001"),
            0, "NextAfter #1");

        this.test(new DfpDec(this.field, "1.0000000000000001").nextAfter(this.ninf),
            new DfpDec(this.field, 1),
            0, "NextAfter #1.5");

        this.test(new DfpDec(this.field, 1).nextAfter(this.ninf),
            new DfpDec(this.field, "0.99999999999999999"),
            0, "NextAfter #2");

        this.test(new DfpDec(this.field, "0.99999999999999999").nextAfter(new DfpDec(this.field, 2)),
            new DfpDec(this.field, 1),
            0, "NextAfter #3");

        this.test(new DfpDec(this.field, -1).nextAfter(this.ninf),
            new DfpDec(this.field, "-1.0000000000000001"),
            0, "NextAfter #4");

        this.test(new DfpDec(this.field, -1).nextAfter(this.pinf),
            new DfpDec(this.field, "-0.99999999999999999"),
            0, "NextAfter #5");

        this.test(new DfpDec(this.field, "-0.99999999999999999").nextAfter(new DfpDec(this.field, -2)),
            new DfpDec(this.field, (byte) -1),
            0, "NextAfter #6");

        this.test(new DfpDec(this.field, (byte) 2).nextAfter(new DfpDec(this.field, 2)),
            new DfpDec(this.field, 2l),
            0, "NextAfter #7");

        this.test(new DfpDec(this.field, 0).nextAfter(new DfpDec(this.field, 0)),
            new DfpDec(this.field, 0),
            0, "NextAfter #8");

        this.test(new DfpDec(this.field, -2).nextAfter(new DfpDec(this.field, -2)),
            new DfpDec(this.field, -2),
            0, "NextAfter #9");

        this.test(new DfpDec(this.field, 0).nextAfter(new DfpDec(this.field, 1)),
            new DfpDec(this.field, "1e-131092"),
            DfpField.FLAG_UNDERFLOW, "NextAfter #10");

        this.test(new DfpDec(this.field, 0).nextAfter(new DfpDec(this.field, -1)),
            new DfpDec(this.field, "-1e-131092"),
            DfpField.FLAG_UNDERFLOW, "NextAfter #11");

        this.test(new DfpDec(this.field, "-1e-131092").nextAfter(this.pinf),
            new DfpDec(this.field, "-0"),
            DfpField.FLAG_UNDERFLOW | DfpField.FLAG_INEXACT, "Next After #12");

        this.test(new DfpDec(this.field, "1e-131092").nextAfter(this.ninf),
            new DfpDec(this.field, "0"),
            DfpField.FLAG_UNDERFLOW | DfpField.FLAG_INEXACT, "Next After #13");

        this.test(new DfpDec(this.field, "9.9999999999999999e131078").nextAfter(this.pinf),
            this.pinf,
            DfpField.FLAG_OVERFLOW | DfpField.FLAG_INEXACT, "Next After #14");
    }

}
