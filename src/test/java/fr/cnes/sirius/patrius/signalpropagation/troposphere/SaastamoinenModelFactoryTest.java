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
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link SaastamoinenModelFactory} class.
 *
 * @author bonitt
 */
public class SaastamoinenModelFactoryTest {

    /**
     * @throws PatriusException
     *         if the precession-nutation model data embedded in the library cannot be read
     * @description Builds a new instance of the factory and use it.
     *
     * @testedMethod {@link AbstractMeteoBasedCorrectionFactory#getCorrectionModel(MeteorologicalConditionsProvider, BodyPoint)}
     * @testedMethod {@link SaastamoinenModelFactory#SaastamoinenModelFactory()}
     * 
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testSaastamoinenModelFactory() throws PatriusException {

        Utils.setDataRoot("atmosphereOrekit");

        // Default date
        final AbsoluteDate defaultDate = new AbsoluteDate();

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 0.2;
        final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(pressure, temperature, humidity);
        final MeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            meteoConditions);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
            Constants.GRS80_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final BodyPoint ellipsoidPoint = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 150., "");
        final double elevation = MathLib.PI / 12.;

        // Tropospheric factory and model initialization
        final AbstractMeteoBasedCorrectionFactory<SaastamoinenModel> tropoFactory = new SaastamoinenModelFactory();

        // Initialize reference tropospheric correction model and compute the reference signal delay
        final SaastamoinenModel refTropoCorr =
            new SaastamoinenModel(meteoConditionsProvider, ellipsoidPoint.getLLHCoordinates().getHeight());
        final double expectedTropoDelay = refTropoCorr.computeSignalDelay(defaultDate, elevation);

        // Call the method AbstractMeteoBasedCorrectionFactory#getCorrectionModel(...) to initialize the tropospheric
        // correction
        final SaastamoinenModel tropoCorrection = tropoFactory.getCorrectionModel(meteoConditionsProvider,
            ellipsoidPoint);
        Assert.assertNotNull(tropoCorrection);

        // Compute and evaluate the signal delay
        Assert.assertEquals(expectedTropoDelay, tropoCorrection.computeSignalDelay(defaultDate, elevation), 0.);
    }
}
