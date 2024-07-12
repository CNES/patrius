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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:05/11/2013:Added constructors
 * VERSION::DM:524:10/03/2016:serialization test
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPEntry.DtType;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test new constructors
 * 
 * @see EOPEntry
 * @see EOP1980Entry
 * @see EOP2000Entry
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public class EOPEntryTest {

    @Test
    public void testEOP2000EntryIntDoubleDoubleDoubleDoubleDoubleDoubleDtType() throws PatriusException {
        Utils.setDataRoot("regular-data");
        EOP2000Entry entry = new EOP2000Entry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        entry = new EOP2000Entry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void
            testEOP2000EntryDateComponentsDoubleDoubleDoubleDoubleDoubleDoubleDtType()
                                                                                      throws IllegalArgumentException,
                                                                                      PatriusException {
        Utils.setDataRoot("regular-data");
        EOP2000Entry entry = new EOP2000Entry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        entry = new EOP2000Entry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testEOP2000EntryAbsoluteDateDoubleDoubleDoubleDoubleDoubleDoubleDtType() throws PatriusException {

        EOP2000Entry entry = new EOP2000Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        Utils.setDataRoot("regular-data");
        entry = new EOP2000Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testEOP1980EntryIntDoubleDoubleDoubleDoubleDoubleDoubleDtType() throws PatriusException {
        Utils.setDataRoot("regular-data");
        EOP1980Entry entry = new EOP1980Entry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        entry = new EOP1980Entry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void
            testEOP1980EntryDateComponentsDoubleDoubleDoubleDoubleDoubleDoubleDtType()
                                                                                      throws IllegalArgumentException,
                                                                                      PatriusException {
        Utils.setDataRoot("regular-data");
        EOP1980Entry entry = new EOP1980Entry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        entry = new EOP1980Entry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testEOP1980EntryAbsoluteDateDoubleDoubleDoubleDoubleDoubleDoubleDtType() throws PatriusException {
        EOP1980Entry entry = new EOP1980Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        Utils.setDataRoot("regular-data");
        entry = new EOP1980Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testEOPEntryIntDoubleDoubleDoubleDoubleDoubleDoubleDtType() throws PatriusException {
        Utils.setDataRoot("regular-data");
        EOPEntry entry = new EOPEntry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        entry = new EOPEntry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testEOPEntryDateComponentsDoubleDoubleDoubleDoubleDoubleDoubleDtType() throws IllegalArgumentException,
                                                                                      PatriusException {
        Utils.setDataRoot("regular-data");
        EOPEntry entry = new EOPEntry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        entry = new EOPEntry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testEOPEntryAbsoluteDateDoubleDoubleDoubleDoubleDoubleDoubleDtType() throws PatriusException {
        EOPEntry entry = new EOPEntry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        Assert.assertEquals(1, entry.getUT1MinusTAI(), Precision.DOUBLE_COMPARISON_EPSILON);

        Utils.setDataRoot("regular-data");
        entry = new EOPEntry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        Assert.assertEquals(1 + TimeScalesFactory.getUTC().offsetFromTAI(entry.getDate()), entry.getUT1MinusTAI(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void serializationTest() throws PatriusException {
        final EOPEntry entry1 = new EOPEntry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        final EOPEntry entry2 = new EOPEntry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        final EOPEntry entry3 = new EOPEntry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        final EOP2000Entry entry4 = new EOP2000Entry(10, 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        final EOP2000Entry entry5 = new EOP2000Entry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        final EOP2000Entry entry6 = new EOP2000Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        final EOP2000Entry entry7 = new EOP2000Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        final EOP1980Entry entry8 = new EOP1980Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);
        final EOP1980Entry entry9 = new EOP1980Entry(new AbsoluteDate(), 1, 2, 3, 4, 5, 6, DtType.UT1_UTC);
        final EOP1980Entry entry10 = new EOP1980Entry(new DateComponents(2000, 1), 1, 2, 3, 4, 5, 6, DtType.UT1_TAI);

        final EOPEntry[] entries = { entry1, entry2, entry3, entry4, entry5, entry6, entry7, entry8, entry9, entry10 };

        for (final EOPEntry entry : entries) {
            final EOPEntry entryBis = TestUtils.serializeAndRecover(entry);
            assertEqualsEOPEntry(entry, entryBis);
        }
    }

    private static void assertEqualsEOPEntry(final EOPEntry entry1, final EOPEntry entry2) {
        Assert.assertEquals(entry1.getDate(), entry2.getDate());
        Assert.assertEquals(entry1.getUT1MinusTAI(), entry2.getUT1MinusTAI(), 0);
        Assert.assertEquals(entry1.getLOD(), entry2.getLOD(), 0);
        Assert.assertEquals(entry1.getX(), entry2.getX(), 0);
        Assert.assertEquals(entry1.getY(), entry2.getY(), 0);
        Assert.assertEquals(entry1.getDX(), entry2.getDX(), 0);
        Assert.assertEquals(entry1.getDY(), entry2.getDY(), 0);
    }
}
