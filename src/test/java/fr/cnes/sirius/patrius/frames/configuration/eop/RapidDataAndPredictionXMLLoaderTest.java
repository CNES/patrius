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
 * VERSION:4.11:DM:DM-3260:22/05/2023:[PATRIUS] Harmonisation des EOP2000HistoryLoader
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.data.AbstractFilesLoaderTest;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class RapidDataAndPredictionXMLLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testStartDateDaily1980() throws PatriusException {
        this.setRoot("rapid-data-xml");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDateDaily1980() throws PatriusException {
        this.setRoot("rapid-data-xml");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 8, 1, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testStartDateDaily2000() throws PatriusException {
        this.setRoot("rapid-data-xml");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 5, 11, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDateDaily2000() throws PatriusException {
        this.setRoot("rapid-data-xml");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 7, 24, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testStartDateDaily2000_2() throws PatriusException, IOException {
        this.setRoot("rapid-data-xml");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader().fillHistory(history,
            getClass().getClassLoader().getResourceAsStream("rapid-data-xml/finals2000A.daily.xml"));
        Assert.assertEquals(new AbsoluteDate(2010, 5, 11, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDateDaily2000_2() throws PatriusException, IOException {
        this.setRoot("rapid-data-xml");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader().fillHistory(history,
            getClass().getClassLoader().getResourceAsStream("rapid-data-xml/finals2000A.daily.xml"));
        Assert.assertEquals(new AbsoluteDate(2010, 7, 24, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testStartDateFinals1980() throws PatriusException {
        this.setRoot("compressed-data");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals\\.1999\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(1999, 1, 1, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDateFinals1980() throws PatriusException {
        this.setRoot("compressed-data");
        final EOP1980History history = new EOP1980History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals\\.1999\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(1999, 12, 31, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testStartDateFinals2000() throws PatriusException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.2002\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDateFinals2000() throws PatriusException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.2002\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2002, 12, 31, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }

    @Test
    public void testStartDateFinals2000_2() throws PatriusException, IOException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader().fillHistory(history,
            getClass().getClassLoader().getResourceAsStream(
                "regular-data/Earth-orientation-parameters/yearly/finals2000A.2002.xml"));
        Assert.assertEquals(new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getUTC()),
            history.getStartDate());
    }

    @Test
    public void testEndDateFinals2000_2() throws PatriusException, IOException {
        this.setRoot("regular-data");
        final EOP2000History history = new EOP2000History(EOPInterpolators.LAGRANGE4);
        new RapidDataAndPredictionXMLLoader().fillHistory(history,
            getClass().getClassLoader().getResourceAsStream(
                "regular-data/Earth-orientation-parameters/yearly/finals2000A.2002.xml"));
        Assert.assertEquals(new AbsoluteDate(2002, 12, 31, TimeScalesFactory.getUTC()),
            history.getEndDate());
    }
}
