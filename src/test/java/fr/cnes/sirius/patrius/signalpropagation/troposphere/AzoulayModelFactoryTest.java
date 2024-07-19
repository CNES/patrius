/**
 *
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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link AzoulayModelFactory} class.
 *
 * @author bonitt
 */
public class AzoulayModelFactoryTest {

    /**
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     * @description Builds a new instance of the factory and use it.
     *
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#AbstractTroposphericCorrectionFactory()}
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#getTropoCorrection(MeteorologicalConditionsProvider, EllipsoidPoint)}
     * @testedMethod {@link AzoulayModelFactory#AzoulayModelFactory()}
     * @testedMethod {@link AzoulayModelFactory#AzoulayModelFactory(boolean)}
     * 
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testAzoulayModelFactory() throws PatriusException {

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 20.;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
            Constants.GRS80_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final EllipsoidPoint ellipsoidPoint = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 150., "");
        final double elevation = MathLib.PI / 12.;

        // Tropospheric factory and model initialization
        AbstractTroposphericCorrectionFactory tropoFactory = new AzoulayModelFactory();

        // Initialize reference tropospheric correction model and compute the reference signal delay
        final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(pressure, temperature,
            humidity);
        final MeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            meteoConditions);
        TroposphericCorrection refTropoCorr = new AzoulayModel(meteoConditionsProvider,
            ellipsoidPoint.getLLHCoordinates().getHeight());
        // Default date for this test
        final AbsoluteDate date = new AbsoluteDate();
        final double expectedTropoDelay = refTropoCorr.computeSignalDelay(date, elevation);

        // Call the method AbstractTroposphericCorrectionFactory#getTropoCorrection(...) to initialize the
        // tropospheric correction
        final TroposphericCorrection tropoCorrection1 = tropoFactory.getTropoCorrection(meteoConditionsProvider,
            ellipsoidPoint);
        Assert.assertNotNull(tropoCorrection1);

        // Call the method AzoulayModelFactory#buildTropoCorrection(Key) to initialize the tropospheric
        // correction
        final TroposphericCorrection tropoCorrection2 = tropoFactory
            .buildTropoCorrection(new TroposphericCorrectionKey(meteoConditionsProvider, ellipsoidPoint));
        Assert.assertNotNull(tropoCorrection2);

        // Compute and evaluate the signal delays
        Assert.assertEquals(expectedTropoDelay, tropoCorrection1.computeSignalDelay(date, elevation), 0.);
        Assert.assertEquals(expectedTropoDelay, tropoCorrection2.computeSignalDelay(date, elevation), 0.);

        // Test the "isGeometricElevation" boolean set to false
        tropoFactory = new AzoulayModelFactory(false);
        final TroposphericCorrection tropoCorrection3 = tropoFactory.getTropoCorrection(meteoConditionsProvider,
            ellipsoidPoint);
        refTropoCorr = new AzoulayModel(meteoConditionsProvider, ellipsoidPoint.getLLHCoordinates().getHeight(), false);

        Assert.assertEquals(refTropoCorr.computeSignalDelay(date, elevation),
            tropoCorrection3.computeSignalDelay(date, elevation), 0.);
    }
}
