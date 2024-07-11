/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey;

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
     * @testedMethod {@link AbstractTroposphericCorrectionFactory#getTropoCorrection(double, double, double, GeodeticPoint)}
     * @testedMethod {@link SaastamoinenModelFactory#SaastamoinenModelFactory()}
     * @testedMethod {@link SaastamoinenModelFactory#buildTropoCorrection(AbstractTroposphericCorrectionFactory.TroposphericCorrectionKey)}
     * 
     * @testPassCriteria The instance is build without error and the factory achieves to build the expected models.
     */
    @Test
    public void testSaastamoinenModelFactory() {

        Utils.setDataRoot("atmosphereOrekit");

        // Environment initialization
        final double pressure = 102000.;
        final double temperature = 20 + 273.16;
        final double humidity = 0.2;
        final GeodeticPoint geodeticPoint =
            new GeodeticPoint(MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 150.);
        final double elevation = MathLib.PI / 12.;

        // Tropospheric factory and model initialization
        final AbstractTroposphericCorrectionFactory tropoFactory = new SaastamoinenModelFactory();

        // Initialize reference tropospheric correction model and compute the reference signal delay
        final TroposphericCorrection refTropoCorr = new SaastamoinenModel(temperature, pressure, humidity, geodeticPoint.getAltitude());
        final double expectedTropoDelay = refTropoCorr.computeSignalDelay(elevation);

        // Call the method AbstractTroposphericCorrectionFactory#getTropoCorrection(...) to initialize the tropospheric correction
        final TroposphericCorrection tropoCorrection1 = tropoFactory.getTropoCorrection(pressure, temperature, humidity, geodeticPoint);
        Assert.assertNotNull(tropoCorrection1);

        // Call the method SaastamoinenModelFactory#buildTropoCorrection(Key) to initialize the tropospheric correction
        final TroposphericCorrection tropoCorrection2 =
            tropoFactory.buildTropoCorrection(new TroposphericCorrectionKey(pressure, temperature, humidity, geodeticPoint));
        Assert.assertNotNull(tropoCorrection2);

        // Compute and evaluate the signal delays
        Assert.assertEquals(expectedTropoDelay, tropoCorrection1.computeSignalDelay(elevation), 0.);
        Assert.assertEquals(expectedTropoDelay, tropoCorrection2.computeSignalDelay(elevation), 0.);
    }
}
