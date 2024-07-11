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
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey;

/**
 * Unit test class for the {@link MariniMurrayModelFactory} class.
 *
 * @author bonitt
 */
public class MariniMurrayModelFactoryTest {

    /**
     * @description Builds a new instance of the factory and use it.
     *
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#AbstractTroposphericCorrectionFactory()}
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#getTropoCorrection(double, double, double, GeodeticPoint)}
     * @testedMethod {@link MariniMurrayModelFactory#MariniMurrayModelFactory()}
     * @testedMethod {@link MariniMurrayModelFactory#buildTropoCorrection(AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey)}
     * 
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testMariniMurrayModelFactory() {

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 20.;
        final GeodeticPoint geodeticPoint =
            new GeodeticPoint(MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 150.);
        final double lambda = 1.5; // [nm]
        final double elevation = MathLib.PI / 12.;

        // Tropospheric factory and model initialization
        final AbstractTroposphericCorrectionFactory tropoFactory = new MariniMurrayModelFactory(lambda);

        // Initialize reference tropospheric correction model and compute the reference signal delay
        final TroposphericCorrection refTropoCorr =
            new MariniMurrayModel(pressure, geodeticPoint.getLatitude(), humidity, temperature, lambda, geodeticPoint.getAltitude());
        final double expectedTropoDelay = refTropoCorr.computeSignalDelay(elevation);

        // Call the method MariniMurrayModelFactory#buildTropoCorrection(Key) to initialize the tropospheric correction
        final TroposphericCorrection tropoCorrection =
            tropoFactory.buildTropoCorrection(new TroposphericCorrectionKey(pressure, temperature, humidity, geodeticPoint));
        Assert.assertNotNull(tropoCorrection);

        // Compute and evaluate the signal delays
        Assert.assertEquals(expectedTropoDelay, tropoCorrection.computeSignalDelay(elevation), 0.);
    }

    /**
     * @description Try to build a Marini Murray model with a non-positive wavelength.
     *
     * @testedMethod {@link MariniMurrayModelFactory#MariniMurrayModelFactory(double)}
     * 
     * @testPassCriteria The factory should throw an exception.
     */
    @Test
    public void testNotPositiveWavelengthException() {
        try {
            new MariniMurrayModelFactory(-1.);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }
    }
}
