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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:380:23/04/2015:correction and optimization for no precession/nutation case
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class CIRF2000ProviderTest {

    DiurnalRotation defaultDiurnalRotation;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(CIRF2000ProviderTest.class.getSimpleName(), "CIRF frame");
    }

    @Test
    public void testRotationRate() throws PatriusException {
        Report.printMethodHeader("testRotationRate", "Frame conversion", "Orekit", 1.0e-13, ComparisonType.ABSOLUTE);
        final TransformProvider provider =
            new InterpolatingTransformProvider(new CIRFProvider(), true, false,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                3, 1.0, 5, Constants.JULIAN_DAY, 100.0);
        final AbsoluteDate tMin = new AbsoluteDate(2009, 4, 7, 2, 56, 33.816, TimeScalesFactory.getUTC());
        final double minRate = provider.getTransform(tMin).getRotationRate().getNorm();
        Assert.assertEquals(1.1e-15, minRate, 1.0e-16);
        final AbsoluteDate tMax = new AbsoluteDate(2043, 12, 16, 10, 47, 20, TimeScalesFactory.getUTC());
        final double maxRate = provider.getTransform(tMax).getRotationRate().getNorm();
        Assert.assertEquals(8.6e-12, maxRate, 1.0e-13);

        Report.printToReport("Min rate", 1.1e-15, minRate);
        Report.printToReport("MAx rate", 8.6e-12, maxRate);

        // Test the Diurnalrotation methods:
        Assert.assertNotNull(this.defaultDiurnalRotation.getLibrationCorrectionModel());
        Assert.assertNotNull(this.defaultDiurnalRotation.getTidalCorrectionModel());
    }

    @Test
    public void testInterpolationAccuracy() throws PatriusException, IllegalArgumentException {

        // max interpolation error observed on a 2 months period with 60 seconds step
        // all values between 3e-15 and 8e-15 are really equivalent: it is mostly numerical noise
        //
        // number of sample points time between sample points max error
        // 6 86400s / 2 = 12h 2259.1e-15 rad
        // 6 86400s / 4 = 6h 35.6e-15 rad
        // 6 86400s / 6 = 4h 5.4e-15 rad
        // 6 86400s / 8 = 3h 3.6e-15 rad
        // 8 86400s / 2 = 12h 103.8e-15 rad
        // 8 86400s / 4 = 6h 4.8e-15 rad
        // 8 86400s / 6 = 4h 4.0e-15 rad
        // 8 86400s / 8 = 3h 4.2e-15 rad
        // 10 86400s / 2 = 12h 8.3e-15 rad
        // 10 86400s / 4 = 6h 5.3e-15 rad
        // 10 86400s / 6 = 4h 5.2e-15 rad
        // 10 86400s / 8 = 3h 6.1e-15 rad
        // 12 86400s / 2 = 12h 6.3e-15 rad
        // 12 86400s / 4 = 6h 7.8e-15 rad
        // 12 86400s / 6 = 4h 7.2e-15 rad
        // 12 86400s / 8 = 3h 6.9e-15 rad
        //
        // the two best settings are 6 points every 3 hours and 8 points every 4 hours
        final TransformProvider nonInterpolating = new CIRFProvider();
        final TransformProvider interpolating =
            new InterpolatingTransformProvider(nonInterpolating, true, false,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                8, Constants.JULIAN_DAY / 6,
                PatriusConfiguration.getCacheSlotsNumber(),
                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        final AbsoluteDate start = new AbsoluteDate(2002, 10, 3, TimeScalesFactory.getTAI());
        final AbsoluteDate end = new AbsoluteDate(2002, 10, 7, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(300)) {
            final Transform transform =
                new Transform(date,
                    interpolating.getTransform(date),
                    nonInterpolating.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = MathLib.max(maxError, error);
        }
        Assert.assertTrue(maxError < 4.0e-15);
        PatriusConfiguration.setCacheSlotsNumber(2);
        Assert.assertEquals(2, PatriusConfiguration.getCacheSlotsNumber());
    }

    @Test
    public void testNoPN() throws PatriusException {
        // Build config without precession/nutation model
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        builder.setPrecessionNutation(new PrecessionNutation(false, PrecessionNutationModelFactory.NO_PN));
        // Compute transform
        final Transform transform = new CIRFProvider().getTransform(AbsoluteDate.J2000_EPOCH,
            builder.getConfiguration());
        final AngularCoordinates angularCoordinates = transform.getAngular();
        // Check transform
        Assert.assertEquals(1, angularCoordinates.getRotation().getQuaternion().getQ0(), 0);
        Assert.assertEquals(0, angularCoordinates.getRotation().getQuaternion().getQ1(), 0);
        Assert.assertEquals(0, angularCoordinates.getRotation().getQuaternion().getQ2(), 0);
        Assert.assertEquals(0, angularCoordinates.getRotation().getQuaternion().getQ3(), 0);
        Assert.assertEquals(0, angularCoordinates.getRotationRate().getX(), 0);
        Assert.assertEquals(0, angularCoordinates.getRotationRate().getY(), 0);
        Assert.assertEquals(0, angularCoordinates.getRotationRate().getZ(), 0);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("compressed-data");
        final FramesConfigurationBuilder b = new FramesConfigurationBuilder(Utils.getIERS2003ConfigurationWOEOP(true));

        // Tides and libration
        final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;

        // Diurnal rotation
        this.defaultDiurnalRotation = new DiurnalRotation(tides, lib);

        b.setDiurnalRotation(this.defaultDiurnalRotation);

        FramesFactory.setConfiguration(b.getConfiguration());
    }

}
