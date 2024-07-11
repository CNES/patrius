/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey;

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
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#getTropoCorrection(double, double, double, GeodeticPoint)}
     * @testedMethod {@link AzoulayModelFactory#AzoulayModelFactory()}
     * @testedMethod {@link AzoulayModelFactory#AzoulayModelFactory(boolean)}
     * @testedMethod {@link AzoulayModelFactory#buildTropoCorrection(AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey)}
     * 
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testAzoulayModelFactory() {

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 20.;
        final GeodeticPoint geodeticPoint =
            new GeodeticPoint(MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 150.);
        final double elevation = MathLib.PI / 12.;

        // Tropospheric factory and model initialization
        AbstractTroposphericCorrectionFactory tropoFactory = new AzoulayModelFactory();

        // Initialize reference tropospheric correction model and compute the reference signal delay
        TroposphericCorrection refTropoCorr = new AzoulayModel(pressure, temperature, humidity, geodeticPoint.getAltitude());
        final double expectedTropoDelay = refTropoCorr.computeSignalDelay(elevation);

        // Call the method AbstractTroposphericCorrectionFactory#getTropoCorrection(...) to initialize the tropospheric correction
        final TroposphericCorrection tropoCorrection1 = tropoFactory.getTropoCorrection(pressure, temperature, humidity, geodeticPoint);
        Assert.assertNotNull(tropoCorrection1);

        // Call the method AzoulayModelFactory#buildTropoCorrection(Key) to initialize the tropospheric correction
        final TroposphericCorrection tropoCorrection2 =
            tropoFactory.buildTropoCorrection(new TroposphericCorrectionKey(pressure, temperature, humidity, geodeticPoint));
        Assert.assertNotNull(tropoCorrection2);

        // Compute and evaluate the signal delays
        Assert.assertEquals(expectedTropoDelay, tropoCorrection1.computeSignalDelay(elevation), 0.);
        Assert.assertEquals(expectedTropoDelay, tropoCorrection2.computeSignalDelay(elevation), 0.);

        // Test the "isGeometricElevation" boolean set to false
        tropoFactory = new AzoulayModelFactory(false);
        final TroposphericCorrection tropoCorrection3 = tropoFactory.getTropoCorrection(pressure, temperature, humidity, geodeticPoint);
        refTropoCorr = new AzoulayModel(pressure, temperature, humidity, geodeticPoint.getAltitude(), false);

        Assert.assertEquals(refTropoCorr.computeSignalDelay(elevation), tropoCorrection3.computeSignalDelay(elevation), 0.);
    }
}
