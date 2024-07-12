/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Unit test class for the {@link SaastamoinenModelFactory} class.
 *
 * @author bonitt
 */
public class SaastamoinenModelFactoryTest {

    /**
     * @description Builds a new instance of the factory and use it.
     *
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#AbstractTroposphericCorrectionFactory()}
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#getTropoCorrection(MeteorologicalConditionsProvider, GeodeticPoint)}
     * @testedMethod {@link SaastamoinenModelFactory#SaastamoinenModelFactory()}
     * 
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testSaastamoinenModelFactory() {

        Utils.setDataRoot("atmosphereOrekit");

        // Default date
        final AbsoluteDate defaultDate = new AbsoluteDate();

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 0.2;
        final GeodeticPoint geodeticPoint = new GeodeticPoint(MathLib.toRadians(67.8805741),
            MathLib.toRadians(21.0310484), 150.);
        final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(pressure, temperature,
            humidity);
        final MeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            meteoConditions);

        final double elevation = MathLib.PI / 12.;

        // Tropospheric factory and model initialization
        final AbstractTroposphericCorrectionFactory tropoFactory = new SaastamoinenModelFactory();

        // Initialize reference tropospheric correction model and compute the reference signal delay
        final TroposphericCorrection refTropoCorr = new SaastamoinenModel(meteoConditionsProvider,
            geodeticPoint.getAltitude());
        final double expectedTropoDelay = refTropoCorr.computeSignalDelay(defaultDate, elevation);

        // Call the method AbstractTroposphericCorrectionFactory#getTropoCorrection(...) to initialize the
        // tropospheric correction
        final TroposphericCorrection tropoCorrection1 = tropoFactory.getTropoCorrection(meteoConditionsProvider,
            geodeticPoint);
        Assert.assertNotNull(tropoCorrection1);

        // Call the method SaastamoinenModelFactory#buildTropoCorrection(Key) to initialize the tropospheric
        // correction
        final TroposphericCorrection tropoCorrection2 = tropoFactory
            .buildTropoCorrection(new TroposphericCorrectionKey(meteoConditionsProvider, geodeticPoint));
        Assert.assertNotNull(tropoCorrection2);

        // Compute and evaluate the signal delays
        Assert.assertEquals(expectedTropoDelay, tropoCorrection1.computeSignalDelay(defaultDate, elevation),
            0.);
        Assert.assertEquals(expectedTropoDelay, tropoCorrection2.computeSignalDelay(defaultDate, elevation),
            0.);
    }
}
