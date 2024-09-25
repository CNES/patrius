/**
 * HISTORY
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.math.fitting;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * TU de la classe LinearRegression
 *
 * @author parraudp @ CS
 */
public class LinearRegressionTest {

    /**
     * TU nominal
     *
     * Reference issue de la maquette CNES : X = { 0.0, 86400.0, 172800.0, 259200.0, 345600.0,
     * 432000.0, 518400.0,
     * 604800.0, 691200.0, 777600.0} Y = {-34.690055, -34.69159, -34.693011, -34.693987, -34.694444,
     * -34.694616,
     * -34.695195, -34.695981, -34.696765, -34.697583} Solution TU1-TAI = A1 + B.(TAI-t1) = A0 +
     * B.(TAI-t0) B =
     * -8.60150112233676E-9 s/s A1 = -34.690978436363636 s t1 = 2013-09-13T00:00:35.000 TAI A0 =
     * -30.972156971673737 s
     * t0 = 2000-01-01T00:00:00.000 TAI
     * @throws PatriusException
     */
    @Test
    public void testLR() {
        final double[] abcissa =
            {0.0, 86400.0, 172800.0, 259200.0, 345600.0, 432000.0, 518400.0, 604800.0, 691200.0, 777600.0};
        final double[] ordinate =
            {-34.690055, -34.69159, -34.693011, -34.693987, -34.694444, -34.694616, -34.695195, -34.695981, -34.696765,
                    -34.697583};
        final double originRef = -34.690978436363636;
        final double slopeRef = -8.60150112233676E-9;

        // Perform linear regression
        final LinearRegression lr = new LinearRegression(abcissa, ordinate);
        final double origin = lr.getOrigin();
        final double slope = lr.getSlope();

        // Check result wrt reference
        Assert.assertEquals(originRef, origin, 0);
        Assert.assertEquals(slopeRef, slope, 0);
    }

    /**
     * TU dégradé : Dimensions des input incohérentes
     */
    @Test(expected = PatriusRuntimeException.class)
    public void testWrongLR() {
        final double[] abcissa =
            {0.0, 86400.0, 172800.0, 259200.0, 345600.0, 432000.0, 518400.0, 604800.0, 691200.0, 777600.0};
        final double[] ordinate =
            {-34.690055, -34.69159, -34.693011, -34.693987, -34.694444, -34.694616, -34.695195, -34.695981, -34.696765};

        // Perform linear regression
        final LinearRegression lr = new LinearRegression(abcissa, ordinate);
        lr.getOrigin();
    }

}
