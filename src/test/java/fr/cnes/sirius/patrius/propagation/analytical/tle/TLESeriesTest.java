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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:468:22/10/2015:Proper handling of ephemeris mode for analytical propagators
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.Predefined;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TLESeriesTest {

    @Test(expected = PatriusException.class)
    public void testNoData() throws IOException, PatriusException {
        final TLESeries series = new TLESeries("^inexistant\\.tle$", false);
        series.loadTLEData();
    }

    @Test(expected = PatriusException.class)
    public void testNoTopexPoseidonNumber() throws IOException, PatriusException {
        final TLESeries series = new TLESeries("^spot-5\\.tle$", false);
        series.loadTLEData(22076);
    }

    @Test(expected = PatriusException.class)
    public void testNoTopexPoseidonLaunchElements() throws IOException, PatriusException {
        final TLESeries series = new TLESeries("^spot-5\\.tle$", false);
        series.loadTLEData(1992, 52, "A");
    }

    @Test
    public void testAvailableSatNums() throws IOException, PatriusException {
        final int[] refIds = {
            5, 4632, 6251, 8195, 9880, 9998, 11801, 14128, 16925,
            20413, 21897, 22312, 22674, 23177, 23333, 23599, 24208, 25954, 26900,
            26975, 28057, 28129, 28350, 28623, 28626, 28872, 29141, 29238, 88888 };

        Utils.setDataRoot("tle/extrapolationTest-data:regular-data");
        final TLESeries series = new TLESeries(".*-entry$", true);
        final Set<Integer> available = series.getAvailableSatelliteNumbers();
        Assert.assertEquals(refIds.length, available.size());
        for (final int ref : refIds) {
            Assert.assertTrue(available.contains(ref));
        }
    }

    @Test
    public void testSpot5Available() throws IOException, PatriusException {
        final TLESeries series = new TLESeries("^spot-5\\.tle$", false);
        final Set<Integer> available = series.getAvailableSatelliteNumbers();
        Assert.assertEquals(1, available.size());
        Assert.assertTrue(available.contains(27421));
    }

    @Test
    public void testSpot5WithExtraLines() throws IOException, PatriusException {
        final TLESeries series = new TLESeries("^spot-5-with-extra-lines\\.tle$", true);
        series.loadTLEData(-1);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());
        final AbsoluteDate referenceFirst =
            new AbsoluteDate(2002, 5, 4, 11, 45, 15.695136, TimeScalesFactory.getUTC());
        Assert.assertEquals(0, series.getFirstDate().durationFrom(referenceFirst), 1e-13);
        final AbsoluteDate referenceLast =
            new AbsoluteDate(2002, 5, 4, 19, 10, 59.114784, TimeScalesFactory.getUTC());
        Assert.assertEquals(0, series.getLastDate().durationFrom(referenceLast), 1e-13);
    }

    @Test
    public void testPVStart() throws IOException, PatriusException {
        final TLESeries series = new TLESeries("^spot-5\\.tle$", false);
        series.loadTLEData();

        final AbsoluteDate t0 = new AbsoluteDate(2002, 5, 4, 11, 0, 0.0, TimeScalesFactory.getUTC());

        // this model is a rough fit on first 3 days of current tle with respect to first tle
        // there are 1500m amplitude variations around a quadratic evolution that grows up to 90km
        final PolynomialFunction errorModel =
            new PolynomialFunction(new double[] { -135.98, 0.010186, 1.3115e-06 });

        final Propagator propagator = TLEPropagator.selectExtrapolator(series.getFirst());
        for (double dt = 0; dt < 3 * Constants.JULIAN_DAY; dt += 600) {
            final AbsoluteDate date = t0.shiftedBy(dt);
            final PVCoordinates delta = new PVCoordinates(propagator.getPVCoordinates(date, FramesFactory.getTEME()),
                series.getPVCoordinates(date));
            Assert.assertEquals(errorModel.value(dt), delta.getPosition().getNorm(), 1500.0);
        }

    }

    @Test
    public void testPVEnd() throws IOException, PatriusException {

        /** Default mass. */
        final MassProvider mass = new SimpleMassModel(1000.0, "DEFAULT");
        final TLESeries series = new TLESeries("^spot-5\\.tle$", false);
        series.loadTLEData();

        final AbsoluteDate t0 =
            new AbsoluteDate(2002, 6, 21, 20, 0, 0.0, TimeScalesFactory.getUTC());

        final TLEPropagator propagator =
            TLEPropagator.selectExtrapolator(series.getLast(), null, new ConstantAttitudeLaw(
                FramesFactory.getEME2000(), Rotation.IDENTITY), mass);
        for (double dt = 3 * Constants.JULIAN_DAY; dt >= 0; dt -= 600) {
            final AbsoluteDate date = t0.shiftedBy(dt);
            final PVCoordinates delta =
                new PVCoordinates(propagator.getPVCoordinates(date), series.getPVCoordinates(date));
            Assert.assertEquals(0, delta.getPosition().getNorm(), 660.0);
        }
    }

    @Test
    public void testSpot5() throws IOException, PatriusException {

        final TLESeries series = new TLESeries("^spot-5\\.tle$", false);

        series.loadTLEData(-1);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());

        series.loadTLEData(27421);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());

        series.loadTLEData(-1, -1, null);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());

        series.loadTLEData(2002, -1, null);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());

        series.loadTLEData(2002, 21, "A");
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());
        Assert.assertEquals(2002, series.getFirst().getLaunchYear());
        Assert.assertEquals(21, series.getFirst().getLaunchNumber());
        Assert.assertEquals("A", series.getFirst().getLaunchPiece());
        Assert.assertEquals(27421, series.getLast().getSatelliteNumber());
        Assert.assertEquals(2002, series.getLast().getLaunchYear());
        Assert.assertEquals(21, series.getLast().getLaunchNumber());
        Assert.assertEquals("A", series.getLast().getLaunchPiece());

        final AbsoluteDate referenceFirst =
            new AbsoluteDate(2002, 5, 4, 11, 45, 15.695136, TimeScalesFactory.getUTC());
        Assert.assertEquals(0, series.getFirstDate().durationFrom(referenceFirst), 1e-13);
        final AbsoluteDate referenceLast =
            new AbsoluteDate(2002, 6, 24, 18, 12, 44.591616, TimeScalesFactory.getUTC());
        Assert.assertEquals(0, series.getLastDate().durationFrom(referenceLast), 1e-13);

        final AbsoluteDate inside = new AbsoluteDate(2002, 06, 02, 11, 12, 15, TimeScalesFactory.getUTC());
        final AbsoluteDate referenceInside =
            new AbsoluteDate(2002, 6, 2, 10, 8, 25.401, TimeScalesFactory.getUTC());
        Assert.assertEquals(0, series.getClosestTLE(inside).getDate().durationFrom(referenceInside), 1e-3);

        final AbsoluteDate oneYearBefore = new AbsoluteDate(2001, 06, 02, 11, 12, 15, TimeScalesFactory.getUTC());
        Assert.assertTrue(series.getClosestTLE(oneYearBefore).getDate().equals(series.getFirstDate()));

        final AbsoluteDate oneYearAfter = new AbsoluteDate(2003, 06, 02, 11, 12, 15, TimeScalesFactory.getUTC());
        Assert.assertTrue(series.getClosestTLE(oneYearAfter).getDate().equals(series.getLastDate()));

    }

    @Test
    public void testTLEFrame() throws PatriusException {

        final TLE tle = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
            "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");
        final Frame tleFrame = TLEPropagator.selectExtrapolator(tle).getFrame();
        Assert.assertEquals(tleFrame.getName(), FramesFactory.getFrame(Predefined.TEME).getName());
    }

    @Test
    public void testProp() throws IOException, PatriusException {
        final TLE tleInit = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
            "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62");

        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleInit);

        final TLE tleRes = propagator.getTLE();
        Assert.assertEquals(tleInit.getLine1(), tleRes.getLine1());
        Assert.assertEquals(tleInit.getLine2(), tleRes.getLine2());

        // tle with e near 1.
        final TLE tleInit2 = new TLE("1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20",
            "2 27421  98.7490 199.5121 9999999 133.9522 226.1918 14.26113993    62");

        try {
            TLEPropagator.selectExtrapolator(tleInit2);
            Assert.fail();
        } catch (final PatriusException oe) {
            // expected
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
