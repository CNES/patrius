/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Unit test class for the {@link AzoulayModelFactory} class.
 *
 * @author bonitt
 */
public class AzoulayModelFactoryTest {

    /**
     * @description Builds a new instance of the factory and use it.
     *
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#AbstractTroposphericCorrectionFactory()}
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#getTropoCorrection(MeteorologicalConditionsProvider, GeodeticPoint)}
     * @testedMethod {@link AzoulayModelFactory#AzoulayModelFactory()}
     * @testedMethod {@link AzoulayModelFactory#AzoulayModelFactory(boolean)}
     * 
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testAzoulayModelFactory() {

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 20.;
        final GeodeticPoint geodeticPoint = new GeodeticPoint(MathLib.toRadians(67.8805741),
            MathLib.toRadians(21.0310484), 150.);
        final double elevation = MathLib.PI / 12.;

        // Tropospheric factory and model initialization
        AbstractTroposphericCorrectionFactory tropoFactory = new AzoulayModelFactory();

        // Initialize reference tropospheric correction model and compute the reference signal delay
        final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(pressure, temperature,
            humidity);
        final MeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            meteoConditions);
        TroposphericCorrection refTropoCorr = new AzoulayModel(meteoConditionsProvider,
            geodeticPoint.getAltitude());
        // Default date for this test
        final AbsoluteDate date = new AbsoluteDate();
        final double expectedTropoDelay = refTropoCorr.computeSignalDelay(date, elevation);

        // Call the method AbstractTroposphericCorrectionFactory#getTropoCorrection(...) to initialize the
        // tropospheric correction
        final TroposphericCorrection tropoCorrection1 = tropoFactory.getTropoCorrection(meteoConditionsProvider,
            geodeticPoint);
        Assert.assertNotNull(tropoCorrection1);

        // Call the method AzoulayModelFactory#buildTropoCorrection(Key) to initialize the tropospheric
        // correction
        final TroposphericCorrection tropoCorrection2 = tropoFactory
            .buildTropoCorrection(new TroposphericCorrectionKey(meteoConditionsProvider, geodeticPoint));
        Assert.assertNotNull(tropoCorrection2);

        // Compute and evaluate the signal delays
        Assert.assertEquals(expectedTropoDelay, tropoCorrection1.computeSignalDelay(date, elevation), 0.);
        Assert.assertEquals(expectedTropoDelay, tropoCorrection2.computeSignalDelay(date, elevation), 0.);

        // Test the "isGeometricElevation" boolean set to false
        tropoFactory = new AzoulayModelFactory(false);
        final TroposphericCorrection tropoCorrection3 = tropoFactory.getTropoCorrection(meteoConditionsProvider,
            geodeticPoint);
        refTropoCorr = new AzoulayModel(meteoConditionsProvider, geodeticPoint.getAltitude(), false);

        Assert.assertEquals(refTropoCorr.computeSignalDelay(date, elevation),
            tropoCorrection3.computeSignalDelay(date, elevation), 0.);
    }

    /**
     * Tests covering deprecated constructors and methods in {@link AbstractTroposphericCorrectionFactory}. Old and new
     * methods should return same results.
     * 
     * This test can be removed once the deprecated elements are definitely deleted.
     * 
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#getTropoCorrection(double, double, double, GeodeticPoint)}
     * @testedMethod {@link TroposphericCorrectionKey#TroposphericCorrectionKey(double, double, double, GeodeticPoint)}
     * 
     */
    @Test
    public void testAbstractDeprecated() {

        final AbsoluteDate date = new AbsoluteDate();
        final double elevation = 0.0;

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 20.;
        final GeodeticPoint geodeticPoint = new GeodeticPoint(MathLib.toRadians(67.8805741),
            MathLib.toRadians(21.0310484), 150.);

        final AbstractTroposphericCorrectionFactory tropoFactory = new AzoulayModelFactory();

        final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(pressure, temperature,
            humidity);
        final MeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
            meteoConditions);

        final TroposphericCorrection tropoCorrection = tropoFactory.getTropoCorrection(pressure, temperature, humidity,
            geodeticPoint);
        final TroposphericCorrection newTropoCorrection = tropoFactory.getTropoCorrection(meteoConditionsProvider,
            geodeticPoint);

        final double delay = tropoCorrection.computeSignalDelay(date, elevation);
        final double newDelay = newTropoCorrection.computeSignalDelay(date, elevation);

        Assert.assertEquals(delay, newDelay, Precision.DOUBLE_COMPARISON_EPSILON);

        // TEST DEPRECATED CONSTRUCTOR SUB CLASS TroposphericCorrectionKey
        final TroposphericCorrectionKey key = new TroposphericCorrectionKey(pressure, temperature, humidity,
            geodeticPoint);
        final TroposphericCorrectionKey newKey = new TroposphericCorrectionKey(meteoConditionsProvider, geodeticPoint);

        Assert.assertEquals(newKey.getMeteoConditionsProvider().getMeteorologicalConditions(date),
            key.getMeteoConditionsProvider().getMeteorologicalConditions(date));
    }
}
