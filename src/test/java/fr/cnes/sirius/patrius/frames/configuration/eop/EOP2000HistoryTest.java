/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.7:DM:DM-2710:18/05/2021:Methode d'interpolation des EOP 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

public class EOP2000HistoryTest {

    @Test
    public void testRegular() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(2004, 1, 4, TimeScalesFactory.getUTC());
        final double dt = EOPHistoryFactory.getEOP2000History().getUT1MinusUTC(date);
        Assert.assertEquals(-0.3906591, dt, 1.0e-10);
    }

    @Test
    public void testOutOfRange() throws PatriusException {
        final EOP2000History history = EOPHistoryFactory.getEOP2000History();
        final AbsoluteDate endDate = new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC());
        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = endDate.shiftedBy(t);
            double dt;
            try {
                dt = history.getUT1MinusUTC(date);

                if (t <= 0) {
                    Assert.assertTrue(dt < 0.29236);
                    Assert.assertTrue(dt > 0.29233);
                } else {
                    Assert.fail();
                }
            } catch (final PatriusExceptionWrapper e) {
                // expected!
            }
        }
    }

    @Test
    public void testUTCLeapLinear() throws PatriusException {
        final EOP2000History history = EOPHistoryFactory.getEOP2000History(EOPInterpolators.LINEAR);
        final AbsoluteDate endLeap = new AbsoluteDate(2006, 1, 1, TimeScalesFactory.getUTC());
        for (double dt = -200; dt < 200; dt += 3) {
            final AbsoluteDate date = endLeap.shiftedBy(dt);
            final double dtu1 = history.getUT1MinusUTC(date);
            if (dt <= 0) {
                Assert.assertEquals(-0.6612, dtu1, 3.0e-5);
            } else {
                Assert.assertEquals(0.3388, dtu1, 3.0e-5);
            }
        }
    }

    @Test
    public void testUTCLeapLagrange4() throws PatriusException {
        final EOP2000History history = EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4);
        final AbsoluteDate endLeap = new AbsoluteDate(2006, 1, 1, TimeScalesFactory.getUTC());
        for (double dt = -200; dt < 200; dt += 3) {
            final AbsoluteDate date = endLeap.shiftedBy(dt);
            final double dtu1 = history.getUT1MinusUTC(date);
            if (dt <= 0) {
                Assert.assertEquals(-0.6612, dtu1, 3.0e-5);
            } else {
                Assert.assertEquals(0.3388, dtu1, 3.0e-5);
            }
        }
    }

    @Test
    public void testCoverage() throws PatriusException {
        // Complimentary unit test for code coverage
        final EOP2000History history = EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4);
        Assert.assertEquals(EOPInterpolators.LAGRANGE4, history.getEOPInterpolationMethod());
        Assert.assertEquals(0, history.size());
    }

    @Test
    public void testFillHistory() throws PatriusException {
        // Filling of a EOP2000History object with a list of entries
        final TimeScale utc = TimeScalesFactory.getUTC();
        final List<EOP2000Entry> ets = new ArrayList<EOP2000Entry>();
        AbsoluteDate ad = new AbsoluteDate("2003-01-01T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2894344, 0.0004286, this.asToRad(-0.088511), this.asToRad(0.188147), this
            .asToRad(-0.000036),
            this.asToRad(0.000113)));
        final DateComponents dc = new DateComponents(2003, 1, 2);
        ets.add(new EOP2000Entry(dc, -0.2897879, 0.0003215, this.asToRad(-0.091580), this.asToRad(0.190481), this
            .asToRad(-0.000050),
            this.asToRad(0.000068)));
        ad = new AbsoluteDate("2003-01-03T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2900915, 0.0003148, this.asToRad(-0.094556), this.asToRad(0.192909), this
            .asToRad(-0.000062),
            this.asToRad(0.000035)));
        ad = new AbsoluteDate("2003-01-04T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2904503, 0.0003717, this.asToRad(-0.097617), this.asToRad(0.195434), this
            .asToRad(-0.000061),
            this.asToRad(0.000042)));
        ad = new AbsoluteDate("2003-01-05T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2908571, 0.0004585, this.asToRad(-0.100734), this.asToRad(0.197666), this
            .asToRad(-0.000048),
            this.asToRad(0.000066)));
        ad = new AbsoluteDate("2003-01-06T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2913617, 0.0006026, this.asToRad(-0.103535), this.asToRad(0.199584), this
            .asToRad(-0.000028),
            this.asToRad(0.000075)));
        ad = new AbsoluteDate("2003-01-07T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2920235, 0.0007776, this.asToRad(-0.106053), this.asToRad(0.201512), this
            .asToRad(-0.000008),
            this.asToRad(0.000054)));
        ad = new AbsoluteDate("2003-01-08T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2928453, 0.0008613, this.asToRad(-0.108629), this.asToRad(0.203603), this
            .asToRad(0.000003),
            this.asToRad(-0.000055)));
        ad = new AbsoluteDate("2003-01-09T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2937273, 0.0008817, this.asToRad(-0.111086), this.asToRad(0.205778), this
            .asToRad(0.000008),
            this.asToRad(-0.000140)));
        ad = new AbsoluteDate("2003-01-10T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2946260, 0.0009087, this.asToRad(-0.113218), this.asToRad(0.208167), this
            .asToRad(0.000009),
            this.asToRad(-0.000123)));
        ad = new AbsoluteDate("2003-01-11T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2955451, 0.0008965, this.asToRad(-0.115277), this.asToRad(0.210604), this
            .asToRad(-0.000007),
            this.asToRad(-0.000067)));
        ad = new AbsoluteDate("2003-01-12T00:00:00", utc);
        ets.add(new EOP2000Entry(ad, -0.2964381, 0.0008266, this.asToRad(-0.117323), this.asToRad(0.212882), this
            .asToRad(-0.000037),
            this.asToRad(-0.000013)));

        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        EOP2000History.fillHistory(ets, history);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, utc);
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testFillHistory1980() throws PatriusException {
        // Filling of a EOP1980History object with a list of entries
        final TimeScale utc = TimeScalesFactory.getUTC();
        final List<EOP1980Entry> ets = new ArrayList<EOP1980Entry>();
        AbsoluteDate ad = new AbsoluteDate("2003-01-01T00:00:00", utc);
        // Note : two last parameters are wrong (permutation needed)
        // but their values are not tested here so it's OK.
        ets.add(new EOP1980Entry(ad, -0.2894344, 0.0004286, this.asToRad(-0.088511), this.asToRad(0.188147), this
            .asToRad(-0.000036),
            this.asToRad(0.000113)));
        final DateComponents dc = new DateComponents(2003, 1, 2);
        ets.add(new EOP1980Entry(dc, -0.2897879, 0.0003215, this.asToRad(-0.091580), this.asToRad(0.190481), this
            .asToRad(-0.000050),
            this.asToRad(0.000068)));
        ad = new AbsoluteDate("2003-01-03T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2900915, 0.0003148, this.asToRad(-0.094556), this.asToRad(0.192909), this
            .asToRad(-0.000062),
            this.asToRad(0.000035)));
        ad = new AbsoluteDate("2003-01-04T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2904503, 0.0003717, this.asToRad(-0.097617), this.asToRad(0.195434), this
            .asToRad(-0.000061),
            this.asToRad(0.000042)));
        ad = new AbsoluteDate("2003-01-05T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2908571, 0.0004585, this.asToRad(-0.100734), this.asToRad(0.197666), this
            .asToRad(-0.000048),
            this.asToRad(0.000066)));
        ad = new AbsoluteDate("2003-01-06T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2913617, 0.0006026, this.asToRad(-0.103535), this.asToRad(0.199584), this
            .asToRad(-0.000028),
            this.asToRad(0.000075)));
        ad = new AbsoluteDate("2003-01-07T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2920235, 0.0007776, this.asToRad(-0.106053), this.asToRad(0.201512), this
            .asToRad(-0.000008),
            this.asToRad(0.000054)));
        ad = new AbsoluteDate("2003-01-08T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2928453, 0.0008613, this.asToRad(-0.108629), this.asToRad(0.203603), this
            .asToRad(0.000003),
            this.asToRad(-0.000055)));
        ad = new AbsoluteDate("2003-01-09T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2937273, 0.0008817, this.asToRad(-0.111086), this.asToRad(0.205778), this
            .asToRad(0.000008),
            this.asToRad(-0.000140)));
        ad = new AbsoluteDate("2003-01-10T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2946260, 0.0009087, this.asToRad(-0.113218), this.asToRad(0.208167), this
            .asToRad(0.000009),
            this.asToRad(-0.000123)));
        ad = new AbsoluteDate("2003-01-11T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2955451, 0.0008965, this.asToRad(-0.115277), this.asToRad(0.210604), this
            .asToRad(-0.000007),
            this.asToRad(-0.000067)));
        ad = new AbsoluteDate("2003-01-12T00:00:00", utc);
        ets.add(new EOP1980Entry(ad, -0.2964381, 0.0008266, this.asToRad(-0.117323), this.asToRad(0.212882), this
            .asToRad(-0.000037),
            this.asToRad(-0.000013)));

        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        EOP1980History.fillHistory(ets, history);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, utc);
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    /**
     * Test method isActive().
     */
    @Test
    public void testGetters() {
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        Assert.assertEquals(true, history.isActive());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private double asToRad(final double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS;
    }

}