/**
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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2856:18/05/2021:Classe de test EOP08C04FilesLoaderTest potentiellement obsolète 
 * VERSION:4.5:FA:FA-2244:27/05/2020:Evolution de la prise en compte des fichiers EOP IERS
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.data.AbstractFilesLoaderTest;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EOPC04FilesLoaderTest extends AbstractFilesLoaderTest {

    /**
     * Check file eopc04_IAU2000.03 can be read by PATRIUS.
     */
    @Test
    public void testEOPGenericFormat() throws PatriusException {
        this.setRoot("eopGenericFormat");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        final EOP2000History eop2000 = EOPHistoryFactory.getEOP2000History();
        final AbsoluteDate t = new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC());
        final double dt = eop2000.getUT1MinusUTC(t);
        Assert.assertEquals(-0.2894427, dt, 1E-14);
    }

    @Test
    public void testGenericFormat() throws PatriusException {
        this.setRoot("genericFormat");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        final EOP2000History eop2000 = EOPHistoryFactory.getEOP2000History();
        final AbsoluteDate t = new AbsoluteDate(2011, 1, 2, TimeScalesFactory.getUTC());
        final double dt = eop2000.getUT1MinusUTC(t);
        Assert.assertEquals(-0.1408005, dt, 1E-14);
    }

    @Test
    public void testMissingMonths() throws PatriusException {
        this.setRoot("missing-months");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(this.getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() throws PatriusException, ParseException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDate() throws PatriusException, ParseException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2005, 12, 31, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testContent() throws PatriusException, ParseException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testContentLagrange4() throws PatriusException, ParseException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testContentDirect() throws PatriusException, ParseException, IOException {
        this.setRoot("regular-data");
        // input stream for the data file
        final InputStream is =
            this.getClass().getResourceAsStream(
                "/regular-data/Earth-orientation-parameters/yearly/eopc04_IAU2000.03");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        // direct read from input stream
        EOPC04FilesLoader.fillHistory(history, is);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testContentLagrange4Direct() throws PatriusException, ParseException, IOException {
        this.setRoot("regular-data");
        // input stream for the data file
        final InputStream is =
            this.getClass().getResourceAsStream(
                "/regular-data/Earth-orientation-parameters/yearly/eopc04_IAU2000.03");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        EOPC04FilesLoader.fillHistory(history, is);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testFillHistory() throws PatriusException {
        this.setRoot("regular-data");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new EOPC04FilesLoader(EOPHistoryFactory.EOPC04_2000_FILENAME).fillHistory(history);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testFillHistoryDirect() throws PatriusException, IOException {
        this.setRoot("regular-data");
        // input stream for the data file
        final InputStream is =
            this.getClass().getResourceAsStream(
                "/regular-data/Earth-orientation-parameters/yearly/eopc04_IAU2000.03");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        EOPC04FilesLoader.fillHistory(history, is);
        final AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals((-3 * 0.0006026 + 27 * 0.0007776 + 27 * 0.0008613 - 3 * 0.0008817) / 48,
            history.getLOD(date), 1.0e-10);
        Assert.assertEquals((-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,
            history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * -0.103535 + 27 * -0.106053 + 27 * -0.108629 - 3 * -0.111086) / 48),
            history
                .getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(this.asToRad((-3 * 0.199584 + 27 * 0.201512 + 27 * 0.203603 - 3 * 0.205778) / 48), history
            .getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test(expected = PatriusException.class)
    public void testErrorOne() throws PatriusException, IOException {
        this.setRoot("regular-data");
        // input stream for the data file
        final InputStream is =
            this.getClass().getResourceAsStream("/eoperrors/err1");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        EOPC04FilesLoader.fillHistory(history, is);
    }

    @Test(expected = PatriusException.class)
    public void testErrorTwo() throws PatriusException, IOException {
        this.setRoot("regular-data");
        // input stream for the data file
        final InputStream is =
            this.getClass().getResourceAsStream("/eoperrors/err2");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        EOPC04FilesLoader.fillHistory(history, is);
    }

    @Test(expected = PatriusException.class)
    public void testErrorThree() throws PatriusException, IOException {
        this.setRoot("regular-data");
        // input stream for the data file
        final InputStream is =
            this.getClass().getResourceAsStream("/eoperrors/err3");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        EOPC04FilesLoader.fillHistory(history, is);
    }

    private double asToRad(final double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS;
    }

}
