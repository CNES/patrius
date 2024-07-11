/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TODProviderTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TODProviderTest.class.getSimpleName(), "TOD frame");
    }

    @Test
    public void testEQESmallDiscontinuity() throws PatriusException {
        final AbsoluteDate switchDate = new AbsoluteDate(1997, 2, 27, TimeScalesFactory.getUTC());
        double currentEQE = Double.NaN;
        final double h = 0.01;
        for (double dt = -1.0 - h / 2; dt <= 1.0 + h / 2; dt += h) {
            final AbsoluteDate d = switchDate.shiftedBy(dt);
            final double previousEQE = currentEQE;
            currentEQE = TODProvider.getEquationOfEquinoxes(d);
            if (!Double.isNaN(previousEQE)) {
                final double deltaMicroAS = 3.6e9 * MathLib.toDegrees(currentEQE - previousEQE);
                if ((dt - h) * dt > 0) {
                    // away from switch date, equation of equinox should decrease at
                    // about 1.06 micro arcsecond per second
                    Assert.assertEquals(-1.06 * h, deltaMicroAS, 0.0003 * h);
                } else {
                    // around switch date, there should be a -1.63 micro arcsecond discontinuity
                    Assert.assertEquals(-1.63, deltaMicroAS, 0.01);
                }
            }
        }
    }

    @Test
    public void testRotationRate() throws PatriusException {
        final TransformProvider provider =
            new InterpolatingTransformProvider(new TODProvider(false), true, false,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                3, 1.0, 5, Constants.JULIAN_DAY, 100.0);
        final AbsoluteDate tMin = new AbsoluteDate(2035, 3, 2, 15, 58, 59, TimeScalesFactory.getUTC());
        final double minRate = provider.getTransform(tMin).getRotationRate().getNorm();
        Assert.assertEquals(6.4e-14, minRate, 1.0e-15);
        final AbsoluteDate tMax = new AbsoluteDate(2043, 12, 16, 14, 18, 9, TimeScalesFactory.getUTC());
        final double maxRate = provider.getTransform(tMax).getRotationRate().getNorm();
        Assert.assertEquals(1.4e-11, maxRate, 1.0e-12);
        // Cover the getLOD and getPoleCorrection methods of the TODProvider class:
        final AbsoluteDate t = new AbsoluteDate(2005, 3, 2, 15, 58, 59, TimeScalesFactory.getUTC());
        Assert.assertNotNull(new TODProvider(false).getLOD(t));
        Assert.assertNotNull(new TODProvider(false).getPoleCorrection(t));
        // Cover the getTransform method of the TODProvider class:
        Assert.assertNotNull(new TODProvider(false).getTransform(tMax, FramesFactory.getConfiguration(), false));
    }

    @Test
    public void testAASReferenceLEO() throws PatriusException {

        Report.printMethodHeader("testAASReferenceLEO", "Frame conversion", "Vallado paper", 1.1e-3,
            ComparisonType.ABSOLUTE);

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        final Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        final Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        // TOD iau76
        final PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(5094514.7804, 6127366.4612, 6380344.5328),
                new Vector3D(-4746.088567, 786.077222, 5531.931288));
        // MOD iau76
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                new Vector3D(-4746.262495, 786.014149, 5531.791025));
        // MOD iau76 w corr
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                new Vector3D(-4746.263052, 786.014045, 5531.790562));

        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76Wcorr), 1.8, 1.7e-3);
        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76), 2.3, 1.6e-3);
        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), 1.1e-3, 5.3e-5);
        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76Wcorr), 0.91, 7.4e-4);

        Report.printToReport("Position", pvTODiau76.getPosition(), tf.transformPVCoordinates(pvMODiau76).getPosition());
        Report.printToReport("Velocity", pvTODiau76.getVelocity(), tf.transformPVCoordinates(pvMODiau76).getVelocity());
    }

    @Test
    public void testAASReferenceGEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf

        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        final Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        // TOD iau76
        final PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(-40577427.7501, -11500096.1306, 10293.2583),
                new Vector3D(837.552338, -2957.524176, -0.928772));
        // MOD iau76
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                new Vector3D(837.708020, -2957.480118, -0.814275));
        // MOD iau76 w corr
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                new Vector3D(837.708020, -2957.480117, -0.814253));

        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76Wcorr), 1.4, 8.1e-4);
        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76Wcorr), 4.5, 7.2e-5);

        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), 5.2e-4, 6.4e-5);
        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76), 3.5, 7.9e-4);

    }

    @Test
    public void testInterpolationAccuracyWithEOP() throws PatriusException, FileNotFoundException {

        // max interpolation error observed on a one month period with 60 seconds step
        //
        // number of sample points time between sample points max error
        // 6 86400s / 8 = 3h 19.56e-12 rad
        // 6 86400s / 12 = 2h 13.02e-12 rad
        // 6 86400s / 16 = 1h30 9.75e-12 rad
        // 6 86400s / 20 = 1h12 7.79e-12 rad
        // 6 86400s / 24 = 1h 6.48e-12 rad
        // 8 86400s / 8 = 3h 20.91e-12 rad
        // 8 86400s / 12 = 2h 13.91e-12 rad
        // 8 86400s / 16 = 1h30 10.42e-12 rad
        // 8 86400s / 20 = 1h12 8.32e-12 rad
        // 8 86400s / 24 = 1h 6.92e-12 rad
        // 10 86400s / 8 = 3h 21.65e-12 rad
        // 10 86400s / 12 = 2h 14.41e-12 rad
        // 10 86400s / 16 = 1h30 10.78e-12 rad
        // 10 86400s / 20 = 1h12 8.61e-12 rad
        // 10 86400s / 24 = 1h 7.16e-12 rad
        // 12 86400s / 8 = 3h 22.12e-12 rad
        // 12 86400s / 12 = 2h 14.72e-12 rad
        // 12 86400s / 16 = 1h30 11.02e-12 rad
        // 12 86400s / 20 = 1h12 8.80e-12 rad
        // 12 86400s / 24 = 1h 7.32e-12 rad
        //
        // looking at error behavior during along the sample show the max error is
        // a peak at 00h00 each day for all curves, which matches the EOP samples
        // points used for correction (applyEOPCorr is set to true at construction here).
        // So looking only at max error does not allow to select an interpolation
        // setting as they all fall in a similar 6e-12 to 8e-12 range. Looking at
        // the error behavior between these peaks however shows that there is still
        // some signal if the time interval is between sample points is too large,
        // in order to get only numerical noise, we have to go as far as 1h between
        // the points.
        // We finally select 6 interpolation points separated by 1 hour each
        final TransformProvider nonInterpolating = new TODProvider(true);
        final TransformProvider interpolating =
            new InterpolatingTransformProvider(nonInterpolating, true, false,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                6, Constants.JULIAN_DAY / 24,
                PatriusConfiguration.getCacheSlotsNumber(),
                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        final AbsoluteDate start = new AbsoluteDate(2002, 11, 11, 0, 0, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate end = new AbsoluteDate(2002, 11, 15, 6, 0, 0.0, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(60)) {
            final Transform transform =
                new Transform(date,
                    interpolating.getTransform(date),
                    nonInterpolating.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = MathLib.max(maxError, error);
        }

        Assert.assertTrue(maxError < 7e-12);

    }

    @Test
    public void testInterpolationAccuracyWithoutEOP() throws PatriusException, FileNotFoundException {

        // max interpolation error observed on a one month period with 60 seconds step
        //
        // number of sample points time between sample points max error
        // 5 86400s / 3 = 8h 3286.90e-15 rad
        // 5 86400s / 6 = 4h 103.90e-15 rad
        // 5 86400s / 8 = 3h 24.74e-15 rad
        // 5 86400s / 12 = 2h 4.00e-15 rad
        // 6 86400s / 3 = 8h 328.91e-15 rad
        // 6 86400s / 6 = 4h 5.92e-15 rad
        // 6 86400s / 8 = 3h 3.95e-15 rad
        // 6 86400s / 12 = 2h 3.94e-15 rad
        // 8 86400s / 3 = 8h 5.87e-15 rad
        // 8 86400s / 6 = 4h 4.73e-15 rad
        // 8 86400s / 8 = 3h 4.45e-15 rad
        // 8 86400s / 12 = 2h 3.87e-15 rad
        // 10 86400s / 3 = 8h 5.29e-15 rad
        // 10 86400s / 6 = 4h 5.36e-15 rad
        // 10 86400s / 8 = 3h 5.86e-15 rad
        // 10 86400s / 12 = 2h 5.76e-15 rad
        //
        //
        // We don't see anymore the peak at 00h00 so this confirms it is related to EOP
        // sampling. All values between 3e-15 and 6e-15 are really equivalent: it is
        // mostly numerical noise. The best settings are 6 or 8 points every 2 or 3 hours.
        // We finally select 6 interpolation points separated by 3 hours each
        final TransformProvider nonInterpolating = new TODProvider(false);
        final TransformProvider interpolating =
            new InterpolatingTransformProvider(nonInterpolating, true, false,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                6, Constants.JULIAN_DAY / 8,
                PatriusConfiguration.getCacheSlotsNumber(),
                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        final AbsoluteDate start = new AbsoluteDate(2002, 11, 11, 0, 0, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate end = new AbsoluteDate(2002, 11, 15, 6, 0, 0.0, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(60)) {
            final Transform transform =
                new Transform(date,
                    interpolating.getTransform(date),
                    nonInterpolating.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = MathLib.max(maxError, error);
        }

        Assert.assertTrue(maxError < 4.0e-15);

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("compressed-data");

        // Add EOP data
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder(FramesFactory.getConfiguration());
        builder.setEOPHistory(EOPHistoryFactory.getEOP1980History());
        FramesFactory.setConfiguration(builder.getConfiguration());
    }

    private void checkPV(final PVCoordinates reference,
                         final PVCoordinates result, final double positionThreshold,
                         final double velocityThreshold) {

        final Vector3D dP = result.getPosition().subtract(reference.getPosition());
        final Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
