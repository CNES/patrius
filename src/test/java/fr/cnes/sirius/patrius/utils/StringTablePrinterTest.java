/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.utils.StringTablePrinter.StringAlign;

/**
 * Unit test class for the {@link StringTablePrinter} class.
 *
 * @author bonitt
 */
public class StringTablePrinterTest {

    /**
     * @description This test evaluates the printer's default settings and basic behaviors.
     *
     * @testedMethod {@link StringTablePrinter#StringTablePrinter(String[])}
     * @testedMethod {@link StringTablePrinter#StringTablePrinter(String, String[])}
     * @testedMethod {@link StringTablePrinter#toString()}
     * @testedMethod {@link StringTablePrinter#getBoldLineSeparator()}
     * @testedMethod {@link StringTablePrinter#getStandardLineSeparator()}
     * @testedMethod {@link StringTablePrinter#getVerticalSeparator()}
     * @testedMethod {@link StringTablePrinter#getVerticalLeftSeparator()}
     * @testedMethod {@link StringTablePrinter#getVerticalRightSeparator()}
     * @testedMethod {@link StringTablePrinter#getStringAlign()}
     * @testedMethod {@link StringTablePrinter#getNbColumns()}
     * @testedMethod {@link StringTablePrinter#DEFAULT_BOLD_LINE_SEPARATOR}
     * @testedMethod {@link StringTablePrinter#DEFAULT_STANDARD_LINE_SEPARATOR}
     * @testedMethod {@link StringTablePrinter#DEFAULT_VERTICAL_SEPARATOR}
     * @testedMethod {@link StringTablePrinter#DEFAULT_LEFT_VERTICAL_SEPARATOR}
     * @testedMethod {@link StringTablePrinter#DEFAULT_RIGHT_VERTICAL_SEPARATOR}
     * @testedMethod {@link StringTablePrinter#addLine(String[])}
     * @testedMethod {@link StringTablePrinter#addLine(double[], String)}
     * @testedMethod {@link StringTablePrinter#addLine(String, double[], String)}
     * @testedMethod {@link StringTablePrinter#addBoldLineSeparator()}
     * @testedMethod {@link StringTablePrinter#addStandardLineSeparator()}
     * @testedMethod {@link StringTablePrinter#switchLastLineBold()}
     * @testedMethod {@link StringTablePrinter#clear()}
     *
     * @testPassCriteria The tables should be printed as expected, evaluated with/without lines and title.
     */
    @Test
    public void testStandardTablePrinter() {

        final String[] header = { "First col", "Second", "third" };
        StringTablePrinter printer = new StringTablePrinter(header);

        // Evaluate the default printer settings
        Assert.assertEquals(StringTablePrinter.DEFAULT_BOLD_LINE_SEPARATOR, printer.getBoldLineSeparator());
        Assert.assertEquals(StringTablePrinter.DEFAULT_STANDARD_LINE_SEPARATOR, printer.getStandardLineSeparator());
        Assert.assertEquals(StringTablePrinter.DEFAULT_VERTICAL_SEPARATOR, printer.getVerticalSeparator());
        Assert.assertEquals(StringTablePrinter.DEFAULT_LEFT_VERTICAL_SEPARATOR, printer.getVerticalLeftSeparator());
        Assert.assertEquals(StringTablePrinter.DEFAULT_RIGHT_VERTICAL_SEPARATOR, printer.getVerticalRightSeparator());
        Assert.assertEquals(StringAlign.RIGHT, printer.getStringAlign());
        Assert.assertEquals(3, printer.getNbColumns());

        // Non-regression on the default printer settings
        Assert.assertEquals("=", StringTablePrinter.DEFAULT_BOLD_LINE_SEPARATOR);
        Assert.assertEquals("-", StringTablePrinter.DEFAULT_STANDARD_LINE_SEPARATOR);
        Assert.assertEquals(" | ", StringTablePrinter.DEFAULT_VERTICAL_SEPARATOR);
        Assert.assertEquals("| ", StringTablePrinter.DEFAULT_LEFT_VERTICAL_SEPARATOR);
        Assert.assertEquals(" |", StringTablePrinter.DEFAULT_RIGHT_VERTICAL_SEPARATOR);

        // --- Without additional lines ---

        // Evaluate the standard constructor without title
        String expectedString = "==============================\n" + "| First col | Second | third |\n";
        Assert.assertEquals(expectedString, printer.toString());

        // Evaluate the standard constructor with a title
        final String title = "Table title";
        printer = new StringTablePrinter(title, header);

        expectedString = "==============================\n" +
                "|         Table title        |\n" +
                "==============================\n" +
                "| First col | Second | third |\n";
        Assert.assertEquals(expectedString, printer.toString());

        // --- With additional lines ---
        final String[] row1 = new String[] { "val1", "value2", "nextVal" }; // The headers dimension is 3
        final double[] row2 = new double[] { 1.2, 2.332114, -3.14 };
        final String row3LeftColumn = "LeftColName";
        final double[] row3 = new double[] { 1.4, -1233.1 };
        final String format = "%g";

        // Evaluate the standard constructor without title
        printer = new StringTablePrinter(header);
        printer.addLine(row1);
        printer.addLine(row2, format);
        printer.addLine(row3LeftColumn, row3, format);

        expectedString = "====================================\n" +
                "|   First col |  Second |    third |\n" +
                "====================================\n" +
                "|        val1 |  value2 |  nextVal |\n" +
                "|     1.20000 | 2.33211 | -3.14000 |\n" +
                "| LeftColName | 1.40000 | -1233.10 |\n";
        Assert.assertEquals(expectedString, printer.toString());

        // Evaluate the standard constructor with a title
        printer = new StringTablePrinter(title, header);

        printer.addLine(row1);
        printer.addLine(row2, format);
        printer.addLine(row3LeftColumn, row3, format);
        printer.addStandardLineSeparator();

        expectedString = "====================================\n" +
                "|            Table title           |\n" +
                "====================================\n" +
                "|   First col |  Second |    third |\n" +
                "====================================\n" +
                "|        val1 |  value2 |  nextVal |\n" +
                "|     1.20000 | 2.33211 | -3.14000 |\n" +
                "| LeftColName | 1.40000 | -1233.10 |\n" +
                "------------------------------------\n";
        Assert.assertEquals(expectedString, printer.toString());

        // Evaluate to switch the last standard line into a bold line
        printer.switchLastLineBold();

        expectedString = "====================================\n" +
                "|            Table title           |\n" +
                "====================================\n" +
                "|   First col |  Second |    third |\n" +
                "====================================\n" +
                "|        val1 |  value2 |  nextVal |\n" +
                "|     1.20000 | 2.33211 | -3.14000 |\n" +
                "| LeftColName | 1.40000 | -1233.10 |\n" +
                "====================================\n";
        Assert.assertEquals(expectedString, printer.toString());

        // Evaluate to clear the table (should clear the title and empty the lines, only the header should remain)
        printer.clear();

        expectedString = "==============================\n" +
                "| First col | Second | third |\n";
        Assert.assertEquals(expectedString, printer.toString());
    }

    /**
     * @description This test evaluates the printer's usage with custom settings.
     *
     * @testedMethod {@link StringTablePrinter#setBoldLineSeparator(String)}
     * @testedMethod {@link StringTablePrinter#setStandardLineSeparator(String)}
     * @testedMethod {@link StringTablePrinter#setVerticalSeparator(String)}
     * @testedMethod {@link StringTablePrinter#setVerticalLeftSeparator(String)}
     * @testedMethod {@link StringTablePrinter#setVerticalRightSeparator(String)}
     * @testedMethod {@link StringTablePrinter#setTitle(String)}
     * @testedMethod {@link StringTablePrinter#addBoldLineSeparator()}
     * @testedMethod {@link StringTablePrinter#addStandardLineSeparator()}
     * @testedMethod {@link StringTablePrinter#toString(int)}
     *
     * @testPassCriteria The tables printed with custom settings should be printed as expected (new separators, title set, indentation).
     */
    @Test
    public void testCustomSettingsTablePrinter() {

        final String[] header = { "First col", "Second", "third" };
        final StringTablePrinter printer = new StringTablePrinter(header);

        // Change the printer default settings
        printer.setBoldLineSeparator("+=");
        printer.setStandardLineSeparator("_");
        printer.setVerticalSeparator("||");
        printer.setVerticalLeftSeparator("^|");
        printer.setVerticalRightSeparator("|^");
        printer.setTitle("Custom title");

        final String[] row1 = new String[] { "val1", "value2", "nextVal" }; // The headers dimension is 3
        final double[] row2 = new double[] { 1.2, 2.332114, -3.14 };
        final String row3LeftColumn = "LeftColName";
        final double[] row3 = new double[] { 1.4, -1233.1 };
        final String format = "%g";

        printer.addLine(row1);
        printer.addStandardLineSeparator();
        printer.addLine(row2, format);
        printer.addLine(row3LeftColumn, row3, format);
        printer.addBoldLineSeparator();

        final String expectedString = "     +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=\n" +
                "     ^|         Custom title         |^\n" +
                "     +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=\n" +
                "     ^|  First col|| Second||   third|^\n" +
                "     +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=\n" +
                "     ^|       val1|| value2|| nextVal|^\n" +
                "     __________________________________\n" +
                "     ^|    1.20000||2.33211||-3.14000|^\n" +
                "     ^|LeftColName||1.40000||-1233.10|^\n" +
                "     +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=\n";
        Assert.assertEquals(expectedString, printer.toString(5)); // Also used indentation to generate the custom array
    }

    /**
     * @description This test evaluates that the printer can describe a table with only one column without error.
     *
     * @testedMethod {@link StringTablePrinter#StringTablePrinter(String[])}
     * @testedMethod {@link StringTablePrinter#addLine(String[])}
     * @testedMethod {@link StringTablePrinter#addLine(double[], String)}
     * @testedMethod {@link StringTablePrinter#addLine(String, double[], String)}
     * @testedMethod {@link StringTablePrinter#toString()}
     *
     * @testPassCriteria The printer can describe a table with only one column without error.
     */
    @Test
    public void testOneColumnTablePrinter() {

        final StringTablePrinter printer = new StringTablePrinter(new String[] { "One col" });

        printer.addLine(new String[] { "val1" });
        printer.addLine(new double[] { 1.29 }, "%g");
        printer.addStandardLineSeparator();

        final String expectedString = "===========\n" +
                "| One col |\n" +
                "===========\n" +
                "|    val1 |\n" +
                "| 1.29000 |\n" +
                "-----------\n";
        Assert.assertEquals(expectedString, printer.toString());
    }

    /**
     * @description This test evaluates the different text alignment settings.
     *
     * @testedMethod {@link StringTablePrinter#setStringAlign(StringAlign)}
     * @testedMethod {@link StringTablePrinter#getStringAlign()}
     * @testedMethod {@link StringTablePrinter#toString()}
     *
     * @testPassCriteria The text in the tables is well aligned as expected according to the chosen convention (LEFT/CENTER/RIGHT).
     */
    @Test
    public void testTextAlignments() {

        final String[] header = { "First col", "Second", "third" };
        final StringTablePrinter printer = new StringTablePrinter(header);

        final String[] row1 = new String[] { "val1", "value2", "nextVal" }; // The headers dimension is 3
        final double[] row2 = new double[] { 1.2, 2.332114, -3.14 };
        final String row3LeftColumn = "LeftColName";
        final double[] row3 = new double[] { 1.4, -1233.1 };
        final String format = "%g";

        printer.addLine(row1);
        printer.addLine(row2, format);
        printer.addLine(row3LeftColumn, row3, format);
        printer.addBoldLineSeparator();

        // Evaluate the text alignment: LEFT
        printer.setStringAlign(StringAlign.LEFT);
        Assert.assertEquals(StringAlign.LEFT, printer.getStringAlign());

        String expectedString = "====================================\n" +
                "| First col   | Second  | third    |\n" +
                "====================================\n" +
                "| val1        | value2  | nextVal  |\n" +
                "| 1.20000     | 2.33211 | -3.14000 |\n" +
                "| LeftColName | 1.40000 | -1233.10 |\n" +
                "====================================\n";
        Assert.assertEquals(expectedString, printer.toString());

        // Evaluate the text alignment: CENTER
        printer.setStringAlign(StringAlign.CENTER);
        Assert.assertEquals(StringAlign.CENTER, printer.getStringAlign());

        expectedString = "====================================\n" +
                "|  First col  |  Second |   third  |\n" +
                "====================================\n" +
                "|     val1    |  value2 |  nextVal |\n" +
                "|   1.20000   | 2.33211 | -3.14000 |\n" +
                "| LeftColName | 1.40000 | -1233.10 |\n" +
                "====================================\n";
        Assert.assertEquals(expectedString, printer.toString());

        // Evaluate the text alignment: RIGHT (default)
        printer.setStringAlign(StringAlign.RIGHT);
        Assert.assertEquals(StringAlign.RIGHT, printer.getStringAlign());

        expectedString = "====================================\n" +
                "|   First col |  Second |    third |\n" +
                "====================================\n" +
                "|        val1 |  value2 |  nextVal |\n" +
                "|     1.20000 | 2.33211 | -3.14000 |\n" +
                "| LeftColName | 1.40000 | -1233.10 |\n" +
                "====================================\n";
        Assert.assertEquals(expectedString, printer.toString());
    }

    /**
     * @description Try to build and use a printer the wrong way.
     *              <p>
     *              The following error cases are evaluated:
     *              <ul>
     *              <li>Try to initialize the printer with a null header or an empty header</li>
     *              <li>Try to add a line with incompatible dimension</li>
     *              <li>Try to add line with null attributes</li>
     *              <li>Try to use a negative indentation</li>
     *              <li>Try to set a title wider than the table</li>
     *              </ul>
     *              </p>
     *
     * @testedMethod {@link StringTablePrinter#StringTablePrinter(String[])}
     * @testedMethod {@link StringTablePrinter#StringTablePrinter(String, String[])}
     * @testedMethod {@link StringTablePrinter#addLine(String[])}
     * @testedMethod {@link StringTablePrinter#addLine(double[], String)}
     * @testedMethod {@link StringTablePrinter#addLine(String, double[], String)}
     * @testedMethod {@link StringTablePrinter#toString()}
     * @testedMethod {@link StringTablePrinter#setTitle(String)}
     *
     * @testPassCriteria The exceptions are returned as expected.
     */
    @Test
    public void testPrinterError() {

        // Try to initialize the printer with a null header or an empty header (should fail)
        try {
            new StringTablePrinter(null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new StringTablePrinter("title", null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new StringTablePrinter(new String[0]);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to add a line with incompatible dimension (should fail)
        final String[] header = { "First col", "Second", "third" };
        final StringTablePrinter printer = new StringTablePrinter(header);
        // The header dimension is 3, so the added line should also have 3 elements to be compatible

        try {
            printer.addLine(new String[] { "val1", "val2" });
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            printer.addLine(new double[] { 1.2, 1.1 }, "%g");
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            printer.addLine("leftCol", new double[] { 1.2 }, "%g"); // Left column title + 1 value = 2 elements
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to add line with null attributes (should fail)
        try {
            printer.addLine(null);
            Assert.fail();
        } catch (final NullPointerException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            printer.addLine(null, "%g");
            Assert.fail();
        } catch (final NullPointerException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            printer.addLine("leftCol", null, "%g");
            Assert.fail();
        } catch (final NullPointerException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use a negative indentation (should fail)
        try {
            printer.toString(-1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to set a title wider than the table (should fail)
        // Evaluate the max allowed length for the title (only depends on the header length as there is no additional line for now)
        int maxAllowedLength = 0;
        for (int i = 0; i < header.length; i++) {
            maxAllowedLength += header[i].length(); // Add each header length
        }
        // Add the column separator length for each middle column
        maxAllowedLength += (printer.getNbColumns() - 1) * printer.getVerticalSeparator().length();

        // The "maxAllowedLength" title should be printed without error
        printer.setTitle(new String(new char[maxAllowedLength]));
        try {
            printer.toString();
            Assert.assertTrue(true);
        } catch (final IllegalStateException e) {
            // not expected
            Assert.fail();
        }

        // The "maxAllowedLength + 1" title should fail to print as it is wider than the maximum length
        printer.setTitle(new String(new char[maxAllowedLength + 1]));
        try {
            printer.toString();
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
    }
}
