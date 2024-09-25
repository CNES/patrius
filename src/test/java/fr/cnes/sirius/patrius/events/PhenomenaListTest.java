/**
 * 
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
 * @history created 15/11/2017
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:922:15/11/2017: Serializable interface implementation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEvent;
import fr.cnes.sirius.patrius.events.postprocessing.PhenomenaList;
import fr.cnes.sirius.patrius.events.postprocessing.Phenomenon;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link PhenomenaList}.<br>
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class PhenomenaListTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the phenomena list
         * 
         * @featureDescription Validate the phenomena list
         * 
         * @coveredRequirements DV-EVT_60, DV-EVT_100
         */
        VALIDATE_PHENOMENA_LIST
    }

    /**
     * A phenomenon.
     */
    private static Phenomenon phen1;

    /**
     * Another phenomenon.
     */
    private static Phenomenon phen2;

    /**
     * Setup for all unit tests in the class.
     * Provides two {@link Phenomenon}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");

        // Set up the CodedEvents :
        final AbsoluteDate date1 = new AbsoluteDate("2011-01-01T05:00:00Z",
            TimeScalesFactory.getTT());
        final AbsoluteDate date2 = new AbsoluteDate("2011-01-01T05:30:00Z",
            TimeScalesFactory.getTT());
        final String code1 = "Visibility 1";
        final String comment1 = "Station 1";

        final AbsoluteDate date3 = new AbsoluteDate("2011-02-01T05:00:00Z",
            TimeScalesFactory.getTT());
        final AbsoluteDate date4 = new AbsoluteDate("2011-02-01T05:30:00Z",
            TimeScalesFactory.getTT());
        final String code2 = "Visibility 2";
        final String comment2 = "Station 2";

        // Set up the CodedEvent 1:
        final CodedEvent event1 = new CodedEvent(code1, comment1, date1, true);

        // Set up the CodedEvent 2:
        final CodedEvent event2 = new CodedEvent(code1, comment1, date2, false);

        // Set up the CodedEvent 3:
        final CodedEvent event3 = new CodedEvent(code2, comment2, date3, true);

        // Set up the CodedEvent 4:
        final CodedEvent event4 = new CodedEvent(code2, comment2, date4, false);

        // Set up the Phenomenon 1:
        phen1 = new Phenomenon(event1, true, event2, true, code1, comment1);

        // Set up the Phenomenon 2:
        phen2 = new Phenomenon(event3, true, event4, true, code2, comment2);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENA_LIST}
     * 
     * @testedMethod {@link PhenomenaList#PhenomenaList()}
     * 
     * @description simple constructor test
     * 
     * @input no inputs
     * 
     * @output a {@link PhenomenaList}
     * 
     * @testPassCriteria the {@link PhenomenaList} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testPhenomenaList() {
        // The PhenomenaList is created:
        final PhenomenaList list = new PhenomenaList();
        // Check the constructor did not crash:
        Assert.assertNotNull(list);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENA_LIST}
     * 
     * @testedMethod {@link PhenomenaList#add(Phenomenon)}
     * 
     * @description tests {@link PhenomenaList#add(Phenomenon)}
     * 
     * @input two {@link Phenomenon}
     * 
     * @output size of the list
     * 
     * @testPassCriteria the phenomena are successfully added to the list
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testAdd() throws PatriusException {
        // The CodedEventsList is created:
        final PhenomenaList list = new PhenomenaList();
        list.add(phen1);
        list.add(phen2);

        Assert.assertEquals(2, list.getList().size());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENA_LIST}
     * 
     * @testedMethod {@link PhenomenaList#getList()}
     * 
     * @description tests {@link PhenomenaList#getList()}
     * 
     * @input a {@link Phenomenon}
     * 
     * @output getList() output
     * 
     * @testPassCriteria getList returns the expected list
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testGetList() throws PatriusException {
        // Expected list:
        final List<Phenomenon> expectedList = new ArrayList<>();
        expectedList.add(phen1);
        // Actual list:
        final PhenomenaList list = new PhenomenaList();
        list.add(phen1);

        Assert.assertEquals(expectedList, list.getList());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENA_LIST}
     * 
     * @testedMethod {@link PhenomenaList#getPhenomena(String, String, AbsoluteDateInterval)}
     * 
     * @description tests {@link PhenomenaList#getPhenomena(String, String, AbsoluteDateInterval)}
     * 
     * @input two {@link CodedEvent}
     * 
     * @output getEvents(String, String, AbsoluteDate) output
     * 
     * @testPassCriteria getEvents returns the expected event
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testGetPhenomena() throws PatriusException {
        // Actual list:
        final PhenomenaList list = new PhenomenaList();
        list.add(phen1);
        list.add(phen2);
        final Set<Phenomenon> phens1 = list.getPhenomena("Visibility 2", null, null);
        Assert.assertEquals(1, phens1.size());
        Assert.assertEquals(phen2, phens1.iterator().next());

        final Set<Phenomenon> phens2 = list.getPhenomena("Visibility 1", "Station 1", null);
        Assert.assertEquals(1, phens2.size());
        Assert.assertEquals(phen1, phens2.iterator().next());

        final AbsoluteDate date0 = new AbsoluteDate("2011-02-01T05:00:00Z",
            TimeScalesFactory.getTT());
        final AbsoluteDate date1 = new AbsoluteDate("2011-02-01T05:30:00Z",
            TimeScalesFactory.getTT());
        final AbsoluteDateInterval newInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            date0, date1, IntervalEndpointType.OPEN);
        final Set<Phenomenon> phens3 = list.getPhenomena("Visibility 2", "Station 2", newInterval);
        Assert.assertEquals(1, phens3.size());
        Assert.assertEquals(phen2, phens3.iterator().next());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENA_LIST}
     * 
     * @testedMethod {@link PhenomenaList#toString()}
     * 
     * @description tests {@link PhenomenaList#toString()}
     * 
     * @input two {@link Phenomenon}
     * 
     * @output a string
     * 
     * @testPassCriteria toString returns the expected string
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testToString() throws PatriusException {
        // Expected string:
        final String expectedString = "List<Phenomenon>[ "
            + phen1.toString() + " , " + phen2.toString() + " ]";
        // The List is created:
        final PhenomenaList list = new PhenomenaList();
        list.add(phen1);
        list.add(phen2);

        Assert.assertEquals(expectedString, list.toString());
    }

    @Test
    public void testSerialization() {
        // Random PhenomenaList
        final PhenomenaList list = new PhenomenaList();
        list.add(phen1);
        list.add(phen2);

        // Creation of list2 for serialization test purpose
        final PhenomenaList list2 = TestUtils.serializeAndRecover(list);

        // Test between the 2 objects
        Assert.assertEquals(list.getList(), list2.getList());
        Assert.assertEquals(list.getClass(), list2.getClass());
        Assert.assertEquals(list.toString(), list2.toString());
    }
}
