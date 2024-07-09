/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link CodedEventsList}.<br>
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class CodedEventsListTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the coded events list
         * 
         * @featureDescription Validate the coded events list
         * 
         * @coveredRequirements DV-TRAJ_190, DV-TRAJ_210
         */
        VALIDATE_CODED_EVENTS_LIST
    }

    /**
     * An event.
     */
    private static CodedEvent event1;

    /**
     * Another event.
     */
    private static CodedEvent event2;

    /**
     * Setup for all unit tests in the class.
     * Provides two {@link CodedEvent}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");

        // Set up the CodedEvent constructor parameters:
        final String code = "Visibility";
        final String comment1 = "Station 1";
        final String comment2 = "Station 2";
        final AbsoluteDate date1 = new AbsoluteDate("2011-01-01T05:00:00Z",
            TimeScalesFactory.getTT());
        final AbsoluteDate date2 = new AbsoluteDate("2011-01-01T15:30:00Z",
            TimeScalesFactory.getTT());
        final boolean startingEvent = true;

        // Set up the CodedEvent 1:
        event1 = new CodedEvent(code, comment1, date1, startingEvent);

        // Set up the CodedEvent 2:
        event2 = new CodedEvent(code, comment2, date2, startingEvent);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LIST}
     * 
     * @testedMethod {@link CodedEventsList#CodedEventsList()}
     * 
     * @description simple constructor test
     * 
     * @input no inputs
     * 
     * @output an {@link CodedEventsList}
     * 
     * @testPassCriteria the {@link CodedEventsList} is successfully
     *                   created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testCodedEventsList() {
        // The CodedEventsList is created:
        final CodedEventsList list = new CodedEventsList();
        // Check the constructor did not crash:
        Assert.assertNotNull(list);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LIST}
     * 
     * @testedMethod {@link CodedEventsList#add(CodedEvent)}
     * 
     * @description tests {@link CodedEventsList#add(CodedEvent)}
     * 
     * @input two {@link CodedEvent}
     * 
     * @output size of the list
     * 
     * @testPassCriteria the events are successfully added to the list
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testAdd() throws PatriusException {
        // The CodedEventsList is created:
        final CodedEventsList list = new CodedEventsList();
        list.add(event1);
        list.add(event2);

        Assert.assertEquals(2, list.getList().size());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LIST}
     * 
     * @testedMethod {@link CodedEventsList#getList()}
     * 
     * @description tests {@link CodedEventsList#getList()}
     * 
     * @input a {@link CodedEvent}
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
    public final void testGetList() throws PatriusException {
        // Expected list:
        final List<CodedEvent> expectedList = new ArrayList<CodedEvent>();
        expectedList.add(event1);
        // Actual list:
        final CodedEventsList list = new CodedEventsList();
        list.add(event1);

        Assert.assertEquals(expectedList, list.getList());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LIST}
     * 
     * @testedMethod {@link CodedEventsList#getEvents(String, String, AbsoluteDate)}
     * 
     * @description tests {@link CodedEventsList#getEvents(String, String, AbsoluteDate)}
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
    public final void testGetEvents() throws PatriusException {
        // Actual list:
        final CodedEventsList list = new CodedEventsList();
        list.add(event1);
        list.add(event2);
        final Set<CodedEvent> events1 = list.getEvents("Visibility", null, null);
        Assert.assertEquals(2, events1.size());
        final Iterator<CodedEvent> iterator = events1.iterator();
        Assert.assertEquals(event1, iterator.next());
        Assert.assertEquals(event2, iterator.next());

        final Set<CodedEvent> events2 = list.getEvents("Visibility", "Station 1", null);
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals(event1, events2.iterator().next());

        final Set<CodedEvent> events3 = list.getEvents("Visibility", "Station 1",
            new AbsoluteDate("2011-01-01T05:00:00Z", TimeScalesFactory.getTT()));
        Assert.assertEquals(1, events3.size());
        Assert.assertEquals(event1, events3.iterator().next());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LIST}
     * 
     * @testedMethod {@link CodedEventsList#toString()}
     * 
     * @description tests {@link CodedEventsList#toString()}
     * 
     * @input two {@link CodedEvent}
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
    public final void testToString() throws PatriusException {
        // Expected string:
        final String expectedString = "List<CodedEvent>[ "
            + event1.toString() + " , " + event2.toString() + " ]";
        // The List is created:
        final CodedEventsList list = new CodedEventsList();
        list.add(event1);
        list.add(event2);

        Assert.assertEquals(expectedString, list.toString());
    }
}
