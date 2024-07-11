/*
 * Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.MarshallSolarActivityFutureEstimation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.Month;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class MarshallSolarActivityFutureEstimationTest {

    @Test
    public void testFileDate() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(new DateComponents(2010, Month.NOVEMBER, 1),
            msafe.getFileDate(new AbsoluteDate("2010-05-01", this.utc)));
        Assert.assertEquals(new DateComponents(2010, Month.DECEMBER, 1),
            msafe.getFileDate(new AbsoluteDate("2010-06-01", this.utc)));
        Assert.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
            msafe.getFileDate(new AbsoluteDate("2010-07-01", this.utc)));
        Assert.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
            msafe.getFileDate(new AbsoluteDate("2030-01-01", this.utc)));

    }

    @Test
    public void testFluxStrong() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assert.assertEquals(94.2,
            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(96.6,
            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", this.utc)),
            1.0e-10);
        Assert.assertEquals(99.0,
            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", this.utc)),
            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG,
            msafe.getStrengthLevel());

    }

    @Test
    public void testFluxAverage() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(87.6,
            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(88.7,
            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", this.utc)),
            1.0e-10);
        Assert.assertEquals(89.8,
            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", this.utc)),
            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE,
            msafe.getStrengthLevel());
    }

    @Test
    public void testFluxWeak() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(80.4,
            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(80.6,
            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", this.utc)),
            1.0e-10);
        Assert.assertEquals(80.8,
            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", this.utc)),
            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", this.utc)),
            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK,
            msafe.getStrengthLevel());

    }

    private MarshallSolarActivityFutureEstimation
            loadMsafe(final MarshallSolarActivityFutureEstimation.StrengthLevel strength)
                                                                                         throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation(
                "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)",
                strength);
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.feed(msafe.getSupportedNames(), msafe);
        return msafe;
    }

    @Test
    public void testKpStrong() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assert.assertEquals(2 + 1.0 / 3.0,
            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(3.0,
            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", this.utc)),
            1.0e-14);

        // this one should get exactly to an element of the AP_ARRAY: ap = 7.0
        Assert.assertEquals(2.0,
            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", this.utc)),
            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", this.utc)),
            1.0e-14);

    }

    @Test
    public void testKpAverage() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(2 - 1.0 / 3.0,
            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(2 + 1.0 / 3.0,
            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(2.0 - 1.0 / 3.0,
            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", this.utc)),
            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", this.utc)),
            1.0e-14);

    }

    @Test
    public void testKpWeak() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(1 + 1.0 / 3.0,
            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(2.0,
            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(1 + 1.0 / 3.0,
            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", this.utc)),
            1.0e-14);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", this.utc)),
            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", this.utc)),
            1.0e-14);

    }

    @Test
    public void testMinDate() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(new AbsoluteDate("2010-05-01", this.utc), msafe.getMinDate());
        Assert.assertEquals(78.1,
            msafe.getMeanFlux(msafe.getMinDate()),
            1.0e-14);
    }

    @Test
    public void testMaxDate() throws PatriusException {

        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(new AbsoluteDate("2030-10-01", this.utc), msafe.getMaxDate());
        Assert.assertEquals(67.0,
            msafe.getMeanFlux(msafe.getMaxDate()),
            1.0e-14);
    }

    @Test(expected = PatriusException.class)
    public void testPastOutOfRange() throws PatriusException {
        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        msafe.get24HoursKp(new AbsoluteDate("1960-10-01", this.utc));
    }

    @Test(expected = PatriusException.class)
    public void testFutureOutOfRange() throws PatriusException {
        final MarshallSolarActivityFutureEstimation msafe =
            this.loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        msafe.get24HoursKp(new AbsoluteDate("2060-10-01", this.utc));
    }

    @Test(expected = PatriusException.class)
    public void testExtraData() throws PatriusException {
        final MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation("Jan2011F10-extra-data\\.txt",
                MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.feed(msafe.getSupportedNames(), msafe);
    }

    @Test(expected = PatriusException.class)
    public void testNoData() throws PatriusException {
        final MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation("Jan2011F10-no-data\\.txt",
                MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.feed(msafe.getSupportedNames(), msafe);
    }

    @Test
    public void testCheckSolarActivityData() throws PatriusException {
        // Standard  case: no exception
        final MarshallSolarActivityFutureEstimation msafe = this
                .loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        msafe.checkSolarActivityData(msafe.getMinDate(), msafe.getMaxDate());
        
        // Interval out of bounds: exception expected
        try {
            msafe.checkSolarActivityData(msafe.getMinDate().shiftedBy(-1.), msafe.getMaxDate());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            msafe.checkSolarActivityData(msafe.getMinDate(), msafe.getMaxDate().shiftedBy(1));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data:atmosphereOrekit");
        this.utc = TimeScalesFactory.getUTC();
    }

    @After
    public void tearDown() {
        this.utc = null;
    }

    private TimeScale utc;

}
