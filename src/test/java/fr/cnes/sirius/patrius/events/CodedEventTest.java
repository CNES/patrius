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
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:158:24/10/2013:Changed toString in AbsoluteDate
 * VERSION::DM:922:15/11/2017: Serializable interface implementation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link CodedEvent}.<br>
 * </p>
 * 
 * @author Tiziana Sabatini
 * @author Pierre Cardoso
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class CodedEventTest {

    /** Date for the tests. */
    private static final String STR_DATE = "2011-11-09T12:00:00Z";
    /** "total eclipse" */
    private static final String STR_TOTAL_ECLIPSE = "total eclipse";
    /** "Eclipse" */
    private static final String STR_ECLIPSE = "Eclipse";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the coded event
         * 
         * @featureDescription Validate the coded event
         * 
         * @coveredRequirements DV-EVT_10, DV-TRAJ_230, DV-EVT_40
         */
        VALIDATE_CODED_EVENT
    }

    /**
     * A code for the event.
     */
    private static String code;

    /**
     * A comment for the event.
     */
    private static String comment;

    /**
     * A {@link AbsoluteDate} for the event.
     */
    private static AbsoluteDate date;

    /**
     * A boolean for the event.
     */
    private static boolean startingEvent;

    /**
     * Setup for all unit tests in the class.
     * Provides two strings, a {@link AbsoluteDate} and a boolean.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");

        // Set up the constructor parameters:
        code = STR_ECLIPSE;
        comment = STR_TOTAL_ECLIPSE;
        date = new AbsoluteDate(STR_DATE,
            TimeScalesFactory.getTT());
        startingEvent = false;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#CodedEvent(String, String, AbsoluteDate, boolean)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : code, comment, {@link AbsoluteDate},
     *        boolean
     * 
     * @output an {@link CodedEvent}
     * 
     * @testPassCriteria the {@link CodedEvent} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testCodedEvent() {
        // The CodedEvent is created:
        final CodedEvent event = new CodedEvent(code, comment, date,
            startingEvent);
        // Check the constructor did not crash:
        Assert.assertNotNull(event);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#getCode()}
     * 
     * @description tests {@link CodedEvent#getCode()}
     * 
     * @input {@link CodedEvent} constructor parameters
     * 
     * @output getCode output
     * 
     * @testPassCriteria getCode returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testGetCode() throws PatriusException {
        // Expected code:
        final String expectedCode = STR_ECLIPSE;
        // The CodedEvent is created:
        final CodedEvent event = new CodedEvent(code, comment, date,
            startingEvent);
        Assert.assertEquals(expectedCode, event.getCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#getComment()}
     * 
     * @description tests {@link CodedEvent#getComment()}
     * 
     * @input {@link CodedEvent} constructor parameters
     * 
     * @output getComment output
     * 
     * @testPassCriteria getComment returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testGetComment() throws PatriusException {
        // Expected comment:
        final String expectedComment = STR_TOTAL_ECLIPSE;
        // The CodedEvent is created:
        final CodedEvent event = new CodedEvent(code, comment, date,
            startingEvent);
        Assert.assertEquals(expectedComment, event.getComment());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#getDate()}
     * 
     * @description tests {@link CodedEvent#getDate()}
     * 
     * @input {@link CodedEvent} constructor parameters
     * 
     * @output getDate output
     * 
     * @testPassCriteria getDate returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testGetDate() throws PatriusException {
        // Expected date:
        final AbsoluteDate expectedDate = new AbsoluteDate(
            STR_DATE, TimeScalesFactory.getTT());
        // The CodedEvent is created:
        final CodedEvent event = new CodedEvent(code, comment, date,
            startingEvent);
        Assert.assertEquals(expectedDate, event.getDate());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#isStartingEvent()}
     * 
     * @description tests {@link CodedEvent#isStartingEvent()}
     * 
     * @input {@link CodedEvent} constructor parameters
     * 
     * @output isStartingEvent output
     * 
     * @testPassCriteria isStartingEvent returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testIsStartingEvent() throws PatriusException {
        // Expected boolean:
        final boolean expectedFlag = false;
        // The CodedEvent is created:
        final CodedEvent event = new CodedEvent(code, comment, date,
            startingEvent);
        Assert.assertEquals(expectedFlag, event.isStartingEvent());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#compareTo(CodedEvent)}
     * 
     * @description tests {@link CodedEvent#compareTo(CodedEvent)}, only for the date.
     * 
     * @input a {@link CodedEvent} to compare
     * 
     * @output compareTo results
     * 
     * @testPassCriteria compareTo returns the expected integer
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testCompareTo() throws PatriusException {
        // The CodedEvent 1 is created:
        final CodedEvent event1 = new CodedEvent(code, comment, date,
            startingEvent);
        // The CodedEvent 2 is created:
        final AbsoluteDate date2 = new AbsoluteDate("2011-11-09T11:59:30Z",
            TimeScalesFactory.getTT());
        final CodedEvent event2 = new CodedEvent("VISI", "station 1", date2,
            startingEvent);
        Assert.assertEquals(1, event1.compareTo(event2));
        Assert.assertEquals(0, event1.compareTo(event1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#compareTo(CodedEvent)}
     * 
     * @description tests {@link CodedEvent#compareTo(CodedEvent)} for events with the same date.
     * 
     * @input a {@link CodedEvent} to compare
     * 
     * @output compareTo results
     * 
     * @testPassCriteria compareTo returns the expected integer
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testCompareTo2() throws PatriusException {
        final String codeA = "code_A";
        final String codeB = "code_B";
        final String cmtA = "comment A";
        final String cmtB = "comment B";
        final AbsoluteDate someDate = new AbsoluteDate(2000, 5, 7, TimeScalesFactory.getUTC());
        // The CodedEvent A is created:
        final CodedEvent eventA = new CodedEvent(codeA, cmtA,
            someDate, true);
        // The CodedEvent A2 is created:
        final CodedEvent eventA2 = new CodedEvent(codeA, cmtA,
            someDate, false);
        // The CodedEvent B is created:
        final CodedEvent eventB = new CodedEvent(codeB, cmtB,
            someDate, true);
        // The CodedEvent B2 is created:
        final String cmtB2 = "comment B2";
        final CodedEvent eventB2 = new CodedEvent(codeB, cmtB2,
            someDate, true);
        // The CodedEvent B22 is created:
        final CodedEvent eventB22 = new CodedEvent(codeB, cmtB2,
            someDate, false);
        // A and A2 differ by the boolean : A2 is "after" A since A2 is an ending event.
        // A and B differ by the code : the B code is alphabetically "after" A
        // but B is "before" A2 since it's a starting event.
        // B2 is a starting event so it is before A2.
        // B and B2 differ by the comment : the B2 comment is "after" B alphabetically.
        // B2 and B22 differ by the boolean : B22 is "after" B2 since it's an ending event.
        // A2 and B22 differ by the code : B22 is "after" A2 alphabetically.
        // The complete ordering is thus :
        // A < B < B2 < A2 < B22
        Assert.assertEquals(-1, eventA.compareTo(eventB));
        Assert.assertEquals(1, eventB.compareTo(eventA));
        Assert.assertEquals(-1, eventB.compareTo(eventB2));
        Assert.assertEquals(1, eventB2.compareTo(eventB));
        Assert.assertEquals(-1, eventB2.compareTo(eventA2));
        Assert.assertEquals(1, eventA2.compareTo(eventB2));
        Assert.assertEquals(-1, eventA2.compareTo(eventB22));
        Assert.assertEquals(1, eventB22.compareTo(eventA2));
        //
        Assert.assertEquals(-1, eventA.compareTo(eventB2));
        Assert.assertEquals(-1, eventA.compareTo(eventA2));
        Assert.assertEquals(-1, eventA.compareTo(eventB22));
        Assert.assertEquals(-1, eventB.compareTo(eventA2));
        Assert.assertEquals(-1, eventB.compareTo(eventB22));
        Assert.assertEquals(-1, eventB2.compareTo(eventB22));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#buildUndefinedEvent(AbsoluteDate, boolean)}
     * 
     * @description tests {@link CodedEvent#buildUndefinedEvent(AbsoluteDate, boolean)}
     * 
     * @input constructor parameters : date, boolean
     * 
     * @output a {@link CodedEvent}
     * 
     * @testPassCriteria the {@link CodedEvent} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public void testBuildUndefinedEvent() throws PatriusException {
        // The CodedEvent is created:
        final CodedEvent undefinedEvent = CodedEvent.buildUndefinedEvent(date,
            startingEvent);
        // Check the constructor did not crash:
        Assert.assertNotNull(undefinedEvent);
        Assert.assertEquals("UNDEFINED_EVENT", undefinedEvent.getCode());
        Assert.assertEquals("undefined event", undefinedEvent.getComment());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#toString()}
     * 
     * @description tests {@link CodedEvent#toString()}
     * 
     * @input constructor parameters : date, boolean
     * 
     * @output a string
     * 
     * @testPassCriteria toString returns the expected string
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testToString() {
        // Expected string:
        final String expectedString = "2011-11-09T11:59:27.816"
            + " - (End) - Eclipse : total eclipse";
        // The CodedEvent is created:
        final CodedEvent event = new CodedEvent(code, comment, date,
            startingEvent);
        Assert.assertEquals(expectedString, event.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#hashCode()}
     * 
     * @description tests {@link CodedEvent#hashCode()}
     * 
     * @input constructor parameters
     * 
     * @output the hash code as an integer
     * 
     * @testPassCriteria hashCode returns the expected integer
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testHashCode() {
        // Expected hash code
        final int expectedHC = -934802069;
        // The CodedEvent is created:
        final CodedEvent event = new CodedEvent(code, comment, date,
            startingEvent);
        Assert.assertEquals(expectedHC, event.hashCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENT}
     * 
     * @testedMethod {@link CodedEvent#equals(Object)}
     * 
     * @description tests {@link CodedEvent#equals(Object)}
     * 
     * @input constructor parameters
     * 
     * @output boolean
     * 
     * @testPassCriteria true if the CodedEvents are equals false otherwise
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testEquals() {
        final String code = "eclipse entrance";
        final String comment = "eclipse";
        final AbsoluteDate date = new AbsoluteDate(2012, 2, 1, TimeScalesFactory.getTAI());

        final CodedEvent event1 = new CodedEvent(code, comment, date, true);
        final CodedEvent event2 = new CodedEvent(code, comment, date, true);

        Assert.assertTrue(event1.equals(event1));

        Assert.assertTrue(event1.equals(event2));

        final CodedEvent event3 = new CodedEvent(code, comment, date, false);

        Assert.assertFalse(event1.equals(event3));

        Assert.assertFalse(event1.equals(null));
    }

    @Test
    public void testSerialization() {

        // random CodedEvent
        final String code = "eclipse entrance";
        final String comment = "eclipse";
        final AbsoluteDate date = new AbsoluteDate(2012, 2, 1, TimeScalesFactory.getTAI());

        final CodedEvent event1 = new CodedEvent(code, comment, date, true);

        // Creation of codedEvent2 for serialization test purpose
        final CodedEvent event2 = TestUtils.serializeAndRecover(event1);

        // test serialization between the 2 objects
        Assert.assertEquals(true, event1.equals(event2));
    }
}
