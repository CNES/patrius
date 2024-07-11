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
 * @history created 15/11/17
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:922:15/11/2017: Serializable interface implementation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link Phenomenon}.<br>
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
public class PhenomenonTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the phenomenon
         * 
         * @featureDescription Validate the phenomenon
         * 
         * @coveredRequirements DV-EVT_60, DV-EVT_20
         */
        VALIDATE_PHENOMENON
    }

    /**
     * The start event.
     */
    private static CodedEvent event0;

    /**
     * The end event.
     */
    private static CodedEvent event1;

    /**
     * Another event.
     */
    private static CodedEvent event2;

    /**
     * A {@link AbsoluteDate} for the start event.
     */
    private static AbsoluteDate date0;

    /**
     * A {@link AbsoluteDate} for the end event.
     */
    private static AbsoluteDate date1;

    /**
     * Another {@link AbsoluteDate}.
     */
    private static AbsoluteDate date2;

    /**
     * A comment string.
     */
    private static String noComment;

    /**
     * A code string.
     */
    private static String umbra = "UMBRA_CODE";

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

        final String code = "Eclipse";
        final String comment = "total eclipse";
        noComment = "No comment";

        // Set up the start event:
        date0 = new AbsoluteDate("2011-11-09T12:00:00Z",
            TimeScalesFactory.getTT());
        event0 = new CodedEvent(code, comment, date0, true);

        // Set up the end event:
        date1 = new AbsoluteDate("2011-11-09T12:15:30Z",
            TimeScalesFactory.getTT());
        event1 = new CodedEvent(code, comment, date1, false);

        // Set up the third event:
        date2 = new AbsoluteDate("2012-01-01T00:00:00Z",
            TimeScalesFactory.getTT());
        event2 = new CodedEvent("Visibility", noComment, date2, true);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENON}
     * 
     * @testedMethod {@link Phenomenon#Phenomenon(CodedEvent, boolean, CodedEvent, boolean, String, String)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : two {@link CodedEvent}, two
     *        booleans, two strings
     * 
     * @output a {@link Phenomenon}
     * 
     * @testPassCriteria the {@link Phenomenon} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testPhenomenon() {
        // The Phenomenon is created:
        final Phenomenon phen1 = new Phenomenon(event0, true, event1, true,
            umbra, noComment);
        // Check the constructor did not crash:
        Assert.assertNotNull(phen1);
        // The Phenomenon is created (event0 and event1 are switched):
        final Phenomenon phen2 = new Phenomenon(event1, false, event0, true,
            umbra, noComment);
        // Check the constructor did not crash:
        Assert.assertNotNull(phen2);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENON}
     * 
     * @testedMethod {@link Phenomenon#getStartingEvent()}
     * @testedMethod {@link Phenomenon#getEndingEvent()}
     * @testedMethod {@link Phenomenon#getStartingIsDefined()}
     * @testedMethod {@link Phenomenon#getEndingIsDefined()}
     * @testedMethod {@link Phenomenon#getTimespan()}
     * @testedMethod {@link Phenomenon#getCode()}
     * @testedMethod {@link Phenomenon#getComment()}
     * 
     * @description tests all the getters of the class
     * 
     * @input constructor parameters : two {@link CodedEvent}, two
     *        booleans, two strings and two {@link AbsoluteDate}
     * 
     * @output a {@link Phenomenon}
     * 
     * @testPassCriteria the methods return the expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetters() {
        // The Phenomenon is created:
        final Phenomenon phen = new Phenomenon(event0, false, event1, true,
            umbra, noComment);
        // Compare the starting event
        Assert.assertEquals(event0, phen.getStartingEvent());
        // Compare the ending event
        Assert.assertEquals(event1, phen.getEndingEvent());
        // Compare the flags on the boundaries
        Assert.assertEquals(false, phen.getStartingIsDefined());
        Assert.assertEquals(true, phen.getEndingIsDefined());
        // Compare the phenomenon interval
        final AbsoluteDateInterval interval =
            new AbsoluteDateInterval(IntervalEndpointType.OPEN, date0,
                date1, IntervalEndpointType.OPEN);
        Assert.assertEquals(interval.getLowerData(), phen.getTimespan()
            .getLowerData());
        Assert.assertEquals(interval.getUpperData(), phen.getTimespan()
            .getUpperData());
        Assert.assertEquals(interval.getLowerEndpoint(), phen.getTimespan()
            .getLowerEndpoint());
        Assert.assertEquals(interval.getUpperEndpoint(), phen.getTimespan()
            .getUpperEndpoint());
        // Compare the code
        Assert.assertEquals(umbra, phen.getCode());
        // Compare the comment
        Assert.assertEquals(noComment, phen.getComment());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENON}
     * 
     * @testedMethod {@link Phenomenon#equals(Object)}
     * 
     * @description tests {@link Phenomenon#equals(Object)}
     * 
     * @input constructor parameters for several {@link Phenomenon}
     * 
     * @output equality results
     * 
     * @testPassCriteria the results are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testEquals() {
        // The Phenomenon 1 is created:
        final Phenomenon phen1 = new Phenomenon(event0, true, event1, false,
            umbra, noComment);
        // The Phenomenon 2 is created (event0 and event1 are switched):
        final Phenomenon phen2 = new Phenomenon(event1, false, event0, true,
            umbra, noComment);
        // The phenomena should be the same:
        Assert.assertTrue(phen1.equals(phen1));
        Assert.assertFalse(phen1.equals(null));
        Assert.assertTrue(phen1.equals(phen2));
        Assert.assertTrue(phen2.equals(phen1));
        // The Phenomenon 3 is created:
        final Phenomenon phen3 = new Phenomenon(event0, true, event2, false,
            umbra, noComment);
        // The phenomena 1 and 3 should not be the same:
        Assert.assertFalse(phen1.equals(phen3));
        Assert.assertFalse(phen3.equals(phen1));
        // The Phenomenon 4 is created:
        final Phenomenon phen4 = new Phenomenon(event1, true, event2, false,
            umbra, noComment);
        // The phenomena 3 and 4 should not be the same:
        Assert.assertFalse(phen3.equals(phen4));
        Assert.assertFalse(phen4.equals(phen3));
        // The Phenomenon 5 is created:
        final Phenomenon phen5 = new Phenomenon(event0, true, event2, true,
            umbra, noComment);
        // The phenomena 3 and 5 should not be the same:
        Assert.assertFalse(phen3.equals(phen5));
        Assert.assertFalse(phen5.equals(phen3));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENON}
     * 
     * @testedMethod {@link Phenomenon#compareTo(Phenomenon)}
     * 
     * @description tests {@link Phenomenon#compareTo(Phenomenon)}
     * 
     * @input constructor parameters for several {@link Phenomenon}
     * 
     * @output comparison results
     * 
     * @testPassCriteria the results are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testCompareTo() {
        // The Phenomenon 1 is created:
        final Phenomenon phen1 = new Phenomenon(event0, true, event1, false,
            umbra, noComment);
        // The Phenomenon 2 is created (event0 and event1 are switched):
        final Phenomenon phen2 = new Phenomenon(event1, false, event0, true,
            umbra, noComment);
        // The phenomena should be the same:
        Assert.assertEquals(0, phen1.compareTo(phen2));
        Assert.assertEquals(0, phen2.compareTo(phen1));
        // The Phenomenon 3 is created:
        final Phenomenon phen3 = new Phenomenon(event0, true, event2, false,
            umbra, noComment);
        // Phenomenon 1 is before 3:
        Assert.assertEquals(-1, phen1.compareTo(phen3));
        Assert.assertEquals(1, phen3.compareTo(phen1));
        // The Phenomenon 4 is created:
        final Phenomenon phen4 = new Phenomenon(event1, true, event2, false,
            umbra, noComment);
        // Phenomenon 4 is after 3:
        Assert.assertEquals(1, phen4.compareTo(phen3));
        Assert.assertEquals(-1, phen3.compareTo(phen4));
        // The Phenomenon 5 is created:
        final Phenomenon phen5 = new Phenomenon(event0, true, event2, true,
            umbra, noComment);
        // Phenomenon 5 is before 3:
        Assert.assertEquals(1, phen3.compareTo(phen5));
        Assert.assertEquals(-1, phen5.compareTo(phen3));
        // The Phenomenon 6 is created:
        final Phenomenon phen6 = new Phenomenon(event0, false, event2, true,
            umbra, noComment);
        // Phenomenon 6 is before 5:
        Assert.assertEquals(-1, phen5.compareTo(phen6));
        Assert.assertEquals(1, phen6.compareTo(phen5));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENON}
     * 
     * @testedMethod {@link Phenomenon#toString()}
     * 
     * @description tests {@link Phenomenon#toString()}
     * 
     * @input constructor parameters
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
        // The Phenomenon is created:
        final Phenomenon phen = new Phenomenon(event0, true, event1, true,
            "Umbra", noComment);
        // Expected string:
        final String expectedString = "Umbra [ " + event0.toString() + " ; "
            + event1.toString() + " ] : " + noComment;
        Assert.assertEquals(expectedString, phen.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_PHENOMENON}
     * 
     * @testedMethod {@link Phenomenon#hashCode()}
     * 
     * @description tests {@link Phenomenon#hashCode()}
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
    public final void testHashCode() {
        // Expected hash code
        final int expectedHC = 819854713;
        // The CodedEvent is created:
        final Phenomenon pheno = new Phenomenon(event0, true, event1, false,
            umbra, noComment);
        Assert.assertEquals(expectedHC, pheno.hashCode());
    }

    @Test
    public final void testEqual() {

        final String comment = "eclipse";
        final AbsoluteDate date = new AbsoluteDate(2012, 2, 1, TimeScalesFactory.getTAI());

        final CodedEvent event1 = new CodedEvent("eclipse entrance", comment, date, true);
        final CodedEvent event2 = new CodedEvent("eclipse exit", comment, date.shiftedBy(3600), false);

        final Phenomenon phenomenon1 = new Phenomenon(event1, true, event2, true, "eclipse", "inside eclispe");
        final Phenomenon phenomenon2 = new Phenomenon(event1, true, event2, true, "eclipse", "inside eclispe");

        Assert.assertTrue(phenomenon1.equals(phenomenon2));

        final Phenomenon phenomenon3 = new Phenomenon(event1, true, event2, true, "eclipse", "inside the eclispe");

        Assert.assertFalse(phenomenon1.equals(phenomenon3));
    }

    @Test
    public final void testSerialization() {

        // random phenomenon
        final String comment = "eclipse";
        final AbsoluteDate date = new AbsoluteDate(2012, 2, 1, TimeScalesFactory.getTAI());

        final CodedEvent event1 = new CodedEvent("eclipse entrance", comment, date, true);
        final Phenomenon phenomenon1 = new Phenomenon(event1, true, event2, true, "eclipse", "inside eclispe");
        final Phenomenon phenomenon2 = TestUtils.serializeAndRecover(phenomenon1);

        // test serialization between the 2 objects
        Assert.assertEquals(true, phenomenon1.equals(phenomenon2));
    }
}
