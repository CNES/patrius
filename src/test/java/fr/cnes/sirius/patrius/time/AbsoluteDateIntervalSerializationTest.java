/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3155:10/05/2022:[PATRIUS] Ajout d'une methode public contains (AbsoluteDate) a la classe AbsoluteDateInterval
 * VERSION:4.4:FA:FA-2108:04/10/2019:[PATRIUS] Incoherence hash code/equals dans ComparableInterval
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
package fr.cnes.sirius.patrius.time;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;

/**
 * This class verify the behavior of equals and hashCode method on serialized {@link AbsoluteDateInterval}
 */
public class AbsoluteDateIntervalSerializationTest {

    @Rule
    public TestName testName = new TestName();

    @Before
    public void printBeginLine() {
        System.out.println("==== Begin " + testName.getMethodName() + "====");
    }

    @After
    public void printEndLine() {
        System.out.println("=== End " + testName.getMethodName() + "===");
    }

    /**
     * <p>
     * Create 2 {@link AbsoluteDateInterval} with the same bounds and dates: <br>
     * [ 2009-05-01T00:00:00.000 ; 2009-05-01T00:01:00.000 [
     * </p>
     * Asserts:
     * <ul>
     * <li>the two objects are equals</li>
     * <li>the two objects have the same hash code</li>
     * <li>After adding the two elements in an HashMap, the map contains only one element</li>
     * </ul>
     * 
     */
    @Test
    public void nominalTest() throws Exception {

        final AbsoluteDateInterval interval1 = createReferenceInterval();
        final AbsoluteDateInterval interval2 = createReferenceInterval();

        System.out.println("Interval 1: " + interval1);
        System.out.println("Interval 2: " + interval2);
        System.out.println("Interval 1 HC: " + interval1.hashCode());
        System.out.println("Interval 2 HC: " + interval2.hashCode());

        Assert.assertEquals(interval1, interval2);
        Assert.assertEquals(interval1.hashCode(), interval2.hashCode());

        HashMap<AbsoluteDateInterval, String> map = new HashMap<>();
        map.put(interval1, "Interval 1");
        map.put(interval2, "Interval 2");
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("Interval 2", map.get(interval2));

    }

    /**
     * <p>
     * Load 2 {@link AbsoluteDateInterval} from files with the same bounds and dates: <br>
     * [ 2009-05-01T00:00:00.000 ; 2009-05-01T00:01:00.000 [ <br>
     * Note: the two objects have been serialized in two separate files during the execution of two different JVMs
     * </p>
     * Asserts:
     * <ul>
     * <li>the two objects are equals</li>
     * <li>the two objects have the same hash code (as in nominal case)</li>
     * </ul>
     * 
     */
    @Test
    public void testLoadFromFiles() throws Exception {
    	
        InputStream inputFile1 = getClass().getClassLoader().getResourceAsStream("intervals/interval1.bin");
        InputStream inputFile2 = getClass().getClassLoader().getResourceAsStream("intervals/interval2.bin");

        AbsoluteDateInterval interval1 = readFile(inputFile1);
        AbsoluteDateInterval interval2 = readFile(inputFile2);

        System.out.println("Interval 1: " + interval1);
        System.out.println("Interval 2: " + interval2);
        System.out.println("Interval 1 HC: " + interval1.hashCode());
        System.out.println("Interval 2 HC: " + interval2.hashCode());

        Assert.assertEquals(interval1, interval2);
        Assert.assertEquals(interval1.hashCode(), interval2.hashCode());

    }

    /**
     * <p>
     * Load 2 {@link AbsoluteDateInterval} from files with the same bounds and dates: <br>
     * [ 2009-05-01T00:00:00.000 ; 2009-05-01T00:01:00.000 [ <br>
     * Note: the two objects have been serialized in two separate files during the execution of two different JVMs
     * </p>
     * Asserts:
     * <ul>
     * <li>the two objects are equals</li>
     * <li>After adding the two elements in an HashMap, the map contains only one element (as in nominal case)</li>
     * </ul>
     * 
     */
    @Test
    public void testLoadFromFilesToHashMap() throws Exception {
    	
        InputStream inputFile1 = getClass().getClassLoader().getResourceAsStream("intervals/interval1.bin");
        InputStream inputFile2 = getClass().getClassLoader().getResourceAsStream("intervals/interval2.bin");

        AbsoluteDateInterval interval1 = readFile(inputFile1);
        AbsoluteDateInterval interval2 = readFile(inputFile2);

        System.out.println("Interval 1: " + interval1);
        System.out.println("Interval 2: " + interval2);
        System.out.println("Interval 1 HC: " + interval1.hashCode());
        System.out.println("Interval 2 HC: " + interval2.hashCode());

        HashMap<AbsoluteDateInterval, String> map = new HashMap<>();
        map.put(interval1, "Interval 1");
        map.put(interval2, "Interval 2");
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("Interval 2", map.get(interval2));
    }

    /**
     * <p>
     * Create 1 {@link AbsoluteDateInterval} and load another 1 from file, with the same bounds and dates: <br>
     * [ 2009-05-01T00:00:00.000 ; 2009-05-01T00:01:00.000 [<br>
     * Note: the serialized object have been previous serialized in a file
     * </p>
     * </p>
     * Asserts:
     * <ul>
     * <li>the two objects are equals</li>
     * <li>the two objects have the same hash code</li>
     * </ul>
     * 
     */
    @Test
    public void testLoadFromFilesVsCreated() throws Exception {
    	
        InputStream inputFile1 = getClass().getClassLoader().getResourceAsStream("intervals/interval1.bin");

        AbsoluteDateInterval interval1 = readFile(inputFile1);
        AbsoluteDateInterval interval2 = createReferenceInterval();

        System.out.println("Interval Loaded: " + interval1);
        System.out.println("Interval Created: " + interval2);
        System.out.println("Interval Loaded HC: " + interval1.hashCode());
        System.out.println("Interval Created HC: " + interval2.hashCode());

        Assert.assertEquals(interval1, interval2);
        Assert.assertEquals(interval1.hashCode(), interval2.hashCode());

    }

    private AbsoluteDateInterval readFile(InputStream inputFile) throws IOException, ClassNotFoundException {
        AbsoluteDateInterval data = null;
        try (final InputStream bs = inputFile; final ObjectInputStream os = new ObjectInputStream(bs)) {
            data = (AbsoluteDateInterval) os.readObject();
        }
        return data;
    }

    public static AbsoluteDateInterval createReferenceInterval() {
        return new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
                new AbsoluteDate("2009-05-01T00:00:00.000", TimeScalesFactory.getTAI()),
                new AbsoluteDate("2009-05-01T00:01:00.000", TimeScalesFactory.getTAI()), IntervalEndpointType.OPEN);
    }

    /**
     * <p>
     * Create different intervals and dates to check whether a specific interval contains a specific date.
     * </p>
     * Asserts:
     * <ul>
     * <li>Check that the date is considered to be contained in the interval when the date is inside the interval</li>
     * <li>Check that the date is considered to be contained in the interval when the date is outside the interval</li>
     * <li>Check that the date is considered to be contained in the interval when the date coincides with the lower
     * interval point, whose type is closed</li>
     * <li>Check that the date is considered to be contained in the interval when the date coincides with the upper
     * interval point, whose type is closed</li>
     * <li>Check that the date is not considered to be contained in the interval when the date coincides with the lower
     * interval point, whose type is open</li>
     * <li>Check that the date is not considered to be contained in the interval when the date coincides with the upper
     * interval point, whose type is open</li>
     * </ul>
     * 
     * @testedMethod {@link AbsoluteDateInterval#contains(AbsoluteDate)}
     */
    @Test
    public void testContains() {
        // Check that the date is considered to be contained in the interval when the date is inside the interval
        final String lowerEndpoint = "2009-05-01T00:00:00.000";
        final String upperEndpoint = "2009-05-01T00:01:00.000";
        AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, new AbsoluteDate(
            lowerEndpoint, TimeScalesFactory.getTAI()), new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI()),
            IntervalEndpointType.CLOSED);
        AbsoluteDate date = new AbsoluteDate("2009-05-01T00:00:30.000", TimeScalesFactory.getTAI());
        Assert.assertTrue(interval.contains(date));
        // Check that the date is considered to be contained in the interval when the date is outside the interval
        date = new AbsoluteDate("2009-05-01T00:01:30.000", TimeScalesFactory.getTAI());
        Assert.assertFalse(interval.contains(date));
        // Check that the date is considered to be contained in the interval when the date coincides with the lower
        // interval point, whose type is closed
        date = new AbsoluteDate(lowerEndpoint, TimeScalesFactory.getTAI());
        Assert.assertTrue(interval.contains(date));
        // Check that the date is considered to be contained in the interval when the date coincides with the upper
        // interval point, whose type is closed
        date = new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI());
        Assert.assertTrue(interval.contains(date));
        // Check that the date is not considered to be contained in the interval when the date coincides with the lower
        // interval point, whose type is open
        interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, new AbsoluteDate(lowerEndpoint,
            TimeScalesFactory.getTAI()), new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI()),
            IntervalEndpointType.CLOSED);
        date = new AbsoluteDate(lowerEndpoint, TimeScalesFactory.getTAI());
        Assert.assertFalse(interval.contains(date));
        // Check that the date is not considered to be contained in the interval when the date coincides with the upper
        // interval point, whose type is open
        interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, new AbsoluteDate(lowerEndpoint,
            TimeScalesFactory.getTAI()), new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI()),
            IntervalEndpointType.OPEN);
        date = new AbsoluteDate(upperEndpoint, TimeScalesFactory.getTAI());
        Assert.assertFalse(interval.contains(date));
    }
}
