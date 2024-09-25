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
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:303:02/04/2015: addition of constant outside history EOP
 * VERSION::FA:981:01/09/2017: addition of testOutsideInterval
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Test for EOP2000HistoryConstantOutsideInterval
 * 
 * @author fiorentinoa
 * 
 * @version $Id: EOP2000HistoryConstantOutsideIntervalTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 3.0
 * 
 */
public class EOP2000HistoryConstantOutsideIntervalTest {

    @Test
    public void testStartEndDates() throws PatriusException {

        final EOP2000HistoryConstantOutsideInterval history = EOPHistoryFactory.getEOP2000HistoryConstant();
        final AbsoluteDate startDate = history.getStartDate();
        final AbsoluteDate endDate = history.getEndDate();

        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate,
            endDate, IntervalEndpointType.OPEN);

        // Verify that interval is ]-infinity, +infinity[
        Assert.assertTrue(interval.compareTo(new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            AbsoluteDate.PAST_INFINITY,
            AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN)) == 0);

    }

    @Test
    public void testUT1minusTAI() throws PatriusException {

        // Only check exception case. Other cases are tested when testing UT1 - UTC
        // Create an (empty) EOP history
        final EOPInterpolators interpMeth = EOPInterpolators.LAGRANGE4;
        final EOP2000HistoryConstantOutsideInterval history = EOPHistoryFactory.getEOP2000HistoryConstant(interpMeth,
            new EOP2000HistoryLoader(){

                @Override
                public boolean stillAcceptsData() {
                    return false;
                }

                @Override
                public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                    PatriusException {
                    // nothing to do
                }

                @Override
                public void fillHistory(final EOP2000History history) throws PatriusException {
                    // nothing to do
                }
            });
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new MyUTCTAILoader());

        try {
            history.getUT1MinusTAI(AbsoluteDate.JAVA_EPOCH);
            Assert.fail();
        } catch (final PatriusExceptionWrapper e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testUT1minusUTC() throws PatriusException {

        final EOP2000HistoryConstantOutsideInterval history = EOPHistoryFactory.getEOP2000HistoryConstant();

        // EOP2000 => to verify behavior in the definition interval [startDate, endDate]
        final EOP2000History historyRef = EOPHistoryFactory.getEOP2000History();
        final AbsoluteDate startDate = historyRef.getStartDate();
        final AbsoluteDate endDate = historyRef.getEndDate();

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = endDate.shiftedBy(t);
            final double dt = history.getUT1MinusUTC(date);
            if (t <= 0) {
                Assert.assertEquals(historyRef.getUT1MinusUTC(date), dt, 0);
            } else {
                Assert.assertEquals(historyRef.getUT1MinusUTC(endDate), dt, 0);
            }
        }

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = startDate.shiftedBy(t);
            final double dt = history.getUT1MinusUTC(date);
            if (t <= 0) {
                Assert.assertEquals(historyRef.getUT1MinusUTC(startDate), dt, 0);
            } else {
                Assert.assertEquals(historyRef.getUT1MinusUTC(date), dt, 0);
            }
        }
    }

    @Test
    public void testGetLoD() throws PatriusException {

        final EOP2000HistoryConstantOutsideInterval history = EOPHistoryFactory.getEOP2000HistoryConstant();

        // EOP2000 => to verify behavior in the definition interval [startDate, endDate]
        final EOP2000History historyRef = EOPHistoryFactory.getEOP2000History();
        final AbsoluteDate startDate = historyRef.getStartDate();
        final AbsoluteDate endDate = historyRef.getEndDate();

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = endDate.shiftedBy(t);
            final double lod = history.getLOD(date);
            if (t <= 0) {
                Assert.assertEquals(historyRef.getLOD(date), lod, 0);
            } else {
                Assert.assertEquals(historyRef.getLOD(endDate), lod, 0);
            }
        }

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = startDate.shiftedBy(t);
            final double lod = history.getLOD(date);
            if (t <= 0) {
                Assert.assertEquals(historyRef.getLOD(startDate), lod, 0);
            } else {
                Assert.assertEquals(historyRef.getLOD(date), lod, 0);
            }
        }
    }

    @Test
    public void testPoleCorrection() throws PatriusException {

        final EOP2000HistoryConstantOutsideInterval history = EOPHistoryFactory.getEOP2000HistoryConstant();

        // EOP2000 => to verify behavior in the definition interval [startDate, endDate]
        final EOP2000History historyRef = EOPHistoryFactory.getEOP2000History();
        final AbsoluteDate startDate = historyRef.getStartDate();
        final AbsoluteDate endDate = historyRef.getEndDate();

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = endDate.shiftedBy(t);
            final PoleCorrection poleCorr = history.getPoleCorrection(date);
            if (t <= 0) {
                final PoleCorrection poleCorr2 = historyRef.getPoleCorrection(date);
                Assert.assertEquals(poleCorr2.getXp(), poleCorr.getXp(), 0);
                Assert.assertEquals(poleCorr2.getYp(), poleCorr.getYp(), 0);
            } else {
                final PoleCorrection poleCorr2 = historyRef.getPoleCorrection(endDate);
                Assert.assertEquals(poleCorr2.getXp(), poleCorr.getXp(), 0);
                Assert.assertEquals(poleCorr2.getYp(), poleCorr.getYp(), 0);
            }
        }

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = startDate.shiftedBy(t);
            final PoleCorrection poleCorr = history.getPoleCorrection(date);
            if (t <= 0) {
                final PoleCorrection poleCorr2 = historyRef.getPoleCorrection(startDate);
                Assert.assertEquals(poleCorr2.getXp(), poleCorr.getXp(), 0);
                Assert.assertEquals(poleCorr2.getYp(), poleCorr.getYp(), 0);
            } else {
                final PoleCorrection poleCorr2 = historyRef.getPoleCorrection(date);
                Assert.assertEquals(poleCorr2.getXp(), poleCorr.getXp(), 0);
                Assert.assertEquals(poleCorr2.getYp(), poleCorr.getYp(), 0);
            }
        }
    }

    @Test
    public void testNutationCorrection() throws PatriusException {

        final EOP2000HistoryConstantOutsideInterval history = EOPHistoryFactory.getEOP2000HistoryConstant();

        // EOP2000 => to verify behavior in the definition interval [startDate, endDate]
        final EOP2000History historyRef = EOPHistoryFactory.getEOP2000History();
        final AbsoluteDate startDate = historyRef.getStartDate();
        final AbsoluteDate endDate = historyRef.getEndDate();

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = endDate.shiftedBy(t);
            final NutationCorrection nutCorr = history.getNutationCorrection(date);
            if (t <= 0) {
                final NutationCorrection nutCorr2 = historyRef.getNutationCorrection(date);
                Assert.assertEquals(nutCorr2.getDX(), nutCorr.getDX(), 0);
                Assert.assertEquals(nutCorr2.getDY(), nutCorr.getDY(), 0);
                Assert.assertEquals(nutCorr2.getDdeps(), nutCorr.getDdeps(), 0);
                Assert.assertEquals(nutCorr2.getDdpsi(), nutCorr.getDdpsi(), 0);
            } else {
                final NutationCorrection nutCorr2 = historyRef.getNutationCorrection(endDate);
                Assert.assertEquals(nutCorr2.getDX(), nutCorr.getDX(), 0);
                Assert.assertEquals(nutCorr2.getDY(), nutCorr.getDY(), 0);
                Assert.assertEquals(nutCorr2.getDdeps(), nutCorr.getDdeps(), 0);
                Assert.assertEquals(nutCorr2.getDdpsi(), nutCorr.getDdpsi(), 0);
            }
        }

        for (double t = -1000; t < 1000; t += 3) {
            final AbsoluteDate date = startDate.shiftedBy(t);
            final NutationCorrection nutCorr = history.getNutationCorrection(date);
            if (t <= 0) {
                final NutationCorrection nutCorr2 = historyRef.getNutationCorrection(startDate);
                Assert.assertEquals(nutCorr2.getDX(), nutCorr.getDX(), 1e-10);
                Assert.assertEquals(nutCorr2.getDY(), nutCorr.getDY(), 1e-10);
                Assert.assertEquals(nutCorr2.getDdeps(), nutCorr.getDdeps(), 1e-10);
                Assert.assertEquals(nutCorr2.getDdpsi(), nutCorr.getDdpsi(), 1e-10);
            } else {
                final NutationCorrection nutCorr2 = historyRef.getNutationCorrection(date);
                Assert.assertEquals(nutCorr2.getDX(), nutCorr.getDX(), 1e-10);
                Assert.assertEquals(nutCorr2.getDY(), nutCorr.getDY(), 1e-10);
                Assert.assertEquals(nutCorr2.getDdeps(), nutCorr.getDdeps(), 1e-10);
                Assert.assertEquals(nutCorr2.getDdpsi(), nutCorr.getDdpsi(), 1e-10);
            }
        }
    }

    @Test
    public void testNoEOP() throws PatriusException {

        // Create an (empty) EOP history
        final EOPInterpolators interpMeth = EOPInterpolators.LAGRANGE4;
        final EOP2000HistoryConstantOutsideInterval history = EOPHistoryFactory.getEOP2000HistoryConstant(interpMeth,
            new EOP2000HistoryLoader(){

                @Override
                public boolean stillAcceptsData() {
                    return false;
                }

                @Override
                public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                    PatriusException {
                    // nothing to do
                }

                @Override
                public void fillHistory(final EOP2000History history) throws PatriusException {
                    // nothing to do
                }
            });

        // verify that history does not contain any EOP
        if (history.size() == 0) {
            final NoEOP2000History noeophistory = new NoEOP2000History();

            // Check that methods behave as in NoEOP2000History
            // This is the start date of the EOP
            final AbsoluteDate date = new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getUTC());

            // Check
            Assert.assertEquals(noeophistory.getUT1MinusUTC(date), history.getUT1MinusUTC(date), 0);
            Assert.assertEquals(noeophistory.getLOD(date), history.getLOD(date), 0);
            Assert.assertEquals(noeophistory.getPoleCorrection(date).getXp(), history.getPoleCorrection(date).getXp(),
                0);
            Assert.assertEquals(noeophistory.getPoleCorrection(date).getYp(), history.getPoleCorrection(date).getYp(),
                0);
            Assert.assertEquals(noeophistory.getNutationCorrection(date).getDX(), history.getNutationCorrection(date)
                .getDX(), 0);
            Assert.assertEquals(noeophistory.getNutationCorrection(date).getDY(), history.getNutationCorrection(date)
                .getDY(), 0);
            Assert.assertEquals(noeophistory.getNutationCorrection(date).getDdeps(), history
                .getNutationCorrection(date).getDdeps(), 0);
            Assert.assertEquals(noeophistory.getNutationCorrection(date).getDdpsi(), history
                .getNutationCorrection(date).getDdpsi(), 0);
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testOutsideInterval() throws PatriusException {

        Utils.setDataRoot("patriusDataSet");
        myFramesConfigure();

        // dates and frame definition to look out
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate date1 = new AbsoluteDate("2016-12-31T12:00:00.000", tai);
        final AbsoluteDate date2 = new AbsoluteDate("2017-01-01T12:00:00.000", tai);

        // Dates to check through EOP
        final double tu1MinusUtc1 = FramesFactory.getConfiguration().getEOPHistory().getUT1MinusUTC(date1);
        final double tu1MinusUtc2 = FramesFactory.getConfiguration().getEOPHistory().getUT1MinusUTC(date2);

        // Check
        Assert.assertEquals(tu1MinusUtc1, -0.3471023, 0.000001);
        Assert.assertEquals(tu1MinusUtc2, 0.6528976, 0.000001);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    public static void myFramesConfigure() throws PatriusException {
        // Configurations builder
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        // Patrius default configuration
        final FramesConfiguration config2010 = FramesConfigurationFactory.getIERS2010Configuration();

        // Deactivation of the additional corrections due to tides and to luni-solar terms
        builder.setDiurnalRotation(new DiurnalRotation(TidalCorrectionModelFactory.NO_TIDE,
            LibrationCorrectionModelFactory.NO_LIBRATION));
        builder.setPolarMotion(config2010.getPolarMotionModel());
        builder.setCIRFPrecessionNutation(config2010.getCIRFPrecessionNutationModel());

        // EOP2000 configuration
        builder.setEOPHistory(EOPHistoryFactory.getEOP2000HistoryConstant());

        FramesFactory.setConfiguration(builder.getConfiguration());
    }

    /**
     * Constant UTC-TAI loader.
     */
    private class MyUTCTAILoader implements UTCTAILoader {

        public MyUTCTAILoader() {
        }

        @Override
        public boolean stillAcceptsData() {
            return false;
        }

        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException, ParseException, PatriusException {
            // nothing to do
        }

        @Override
        public SortedMap<DateComponents, Integer> loadTimeSteps() {
            final SortedMap<DateComponents, Integer> entries = new TreeMap<>();
            for (int i = 1971; i < 200; i++) {
                entries.put(new DateComponents(i, 1, 1), 35);
            }
            return entries;
        }

        @Override
        public String getSupportedNames() {
            return "";
        }
    }
}
