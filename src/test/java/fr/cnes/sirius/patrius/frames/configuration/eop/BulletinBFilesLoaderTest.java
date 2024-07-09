/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
* VERSION:4.7:DM:DM-2710:18/05/2021:Methode d'interpolation des EOP 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.data.AbstractFilesLoaderTest;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BulletinBFilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testMissingMonths() throws PatriusException {
        this.setRoot("missing-months");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader(EOPHistoryFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(this.getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() throws PatriusException, ParseException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader(EOPHistoryFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2005, 12, 5, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDate() throws PatriusException, ParseException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader(EOPHistoryFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(this.getMaxGap(history) < 5);
        Assert.assertEquals(new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testNewFormatNominal() throws PatriusException, ParseException {
        this.setRoot("new-bulletinB");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader("bulletinb.270").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 6, 2, TimeScalesFactory.getUTC()),
            history.getStartDate());
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testOldFormatContent() throws PatriusException, ParseException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader(EOPHistoryFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        final AbsoluteDate date = new AbsoluteDate(2006, 1, 11, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(this.msToS((-3 * 0.073 + 27 * -0.130 + 27 * -0.244 - 3 * -0.264) / 48),
            history.getLOD(date),
            1.0e-10);
        Assert.assertEquals((-3 * 0.333275 + 27 * 0.333310 + 27 * 0.333506 - 3 * 0.333768) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.04958 + 27 * 0.04927 + 27 * 0.04876 - 3 * 0.04854) / 48), history
            .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.38117 + 27 * 0.38105 + 27 * 0.38071 - 3 * 0.38036) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testNewFormatContentLinear() throws PatriusException, ParseException {
        this.setRoot("new-bulletinB");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LINEAR);
        new BulletinBFilesLoader("bulletinb.270").fillHistory(history);
        final AbsoluteDate date = new AbsoluteDate(2010, 6, 12, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(this.msToS((0.0294 + 0.0682) / 2), history.getLOD(date), 1.0e-10);
        Assert.assertEquals(this.msToS((-57.2523 + -57.3103) / 2), history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.masToRad((1.658 + 4.926) / 2), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.masToRad((469.330 + 470.931) / 2), history.getPoleCorrection(date).getYp(), 1.0e-10);
        Assert.assertEquals(this.masToRad((-65.018 + -65.067) / 2), history.getNutationCorrection(date).getDdpsi(),
            1.0e-10);
        Assert.assertEquals(this.masToRad((-9.927 + -10.036) / 2), history.getNutationCorrection(date).getDdeps(),
            1.0e-10);
    }

    @Test
    public void testNewFormatContentLagrange4() throws PatriusException, ParseException {
        this.setRoot("new-bulletinB");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader("bulletinb.270").fillHistory(history);
        final AbsoluteDate date = new AbsoluteDate(2010, 6, 12, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(this.msToS((-3 * 0.1202 + 27 * 0.0294 + 27 * 0.0682 - 3 * 0.1531) / 48),
            history.getLOD(date),
            1.0e-10);
        Assert.assertEquals(this.msToS((-3 * -57.1711 + 27 * -57.2523 + 27 * -57.3103 - 3 * -57.4101) / 48),
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.masToRad((-3 * -1.216 + 27 * 1.658 + 27 * 4.926 - 3 * 7.789) / 48), history
            .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.masToRad((-3 * 467.780 + 27 * 469.330 + 27 * 470.931 - 3 * 472.388) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
        Assert.assertEquals(this.masToRad((-3 * -64.899 + 27 * -65.018 + 27 * -65.067 - 3 * -64.998) / 48), history
            .getNutationCorrection(date).getDdpsi(), 1.0e-10);
        Assert.assertEquals(this.masToRad((-3 * -9.955 + 27 * -9.927 + 27 * -10.036 - 3 * -10.146) / 48), history
            .getNutationCorrection(date).getDdeps(), 1.0e-10);
    }

    private double msToS(final double ms) {
        return ms / 1000.0;
    }

    private double asToRad(final double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS;
    }

    private double masToRad(final double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS / 1000.0;
    }

    @Test(expected = PatriusException.class)
    public void testNewFormatTruncated() throws PatriusException, ParseException {
        this.setRoot("new-bulletinB");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader("bulletinb-truncated.270").fillHistory(history);
    }

    @Test(expected = PatriusException.class)
    public void testNewFormatInconsistent() throws PatriusException, ParseException {
        this.setRoot("new-bulletinB");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new BulletinBFilesLoader("bulletinb-inconsistent.270").fillHistory(history);
    }

}
