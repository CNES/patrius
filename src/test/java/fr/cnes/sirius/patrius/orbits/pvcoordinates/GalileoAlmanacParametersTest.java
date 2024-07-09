/**
 *  HISTORY
* VERSION:4.6:FA:FA-2608:27/01/2021:Mauvaise date de reference pour le Galileo System Time
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GalileoAlmanacParametersTest {

    /**
     * Check the reference epoch for GST of the GalileoAlmanacParameters
     * 
     * @throws PatriusException
     *         if the UTC datas can't be loaded
     */
    @Test
    public void checkDate() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        final GalileoAlmanacParameters galileoAlmanacParam = new GalileoAlmanacParameters(
            00, 0, 00, 0, 0, 0, 0, 0, 0);
        // Get the origin date
        final AbsoluteDate date = galileoAlmanacParam.getDate(0, 0);

        Assert.assertEquals(0, date.durationFrom(AbsoluteDate.GALILEO_EPOCH), 0);

        // Evaluate date computation behavior
        final TimeScale utc = TimeScalesFactory.getUTC();
        Assert.assertEquals("1999-08-22T23:59:46.000", galileoAlmanacParam.getDate(0, 86399.0 * 1000).toString(utc));
        Assert.assertEquals("1999-09-04T23:59:47.000", galileoAlmanacParam.getDate(2, 0).toString(utc));
        Assert.assertEquals("1999-09-05T00:03:07.000", galileoAlmanacParam.getDate(2, 200000).toString(utc));
        Assert.assertEquals("1999-09-05T00:03:07.001", galileoAlmanacParam.getDate(2, 200001).toString(utc));
    }
}
