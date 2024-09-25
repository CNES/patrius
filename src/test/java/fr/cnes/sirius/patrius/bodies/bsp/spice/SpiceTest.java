/**
 * Copyright 2023-2023 CNESv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SpiceTest {

    /**
     * For the ephemeris coordinates in m and m/s, the precision of the test (assertEquals) is delta.
     */
    private static final double DELTA = 1e-4; // We define the precision here as 0.1 mm = 1e-4 m = 1e-7 km

    /**
     * Test file for data type 2
     */
    private final String type02TestFile = "type2_borrelly.bsp";

    /**
     * Test file for data type 3
     */
    private final String type03TestFile = "type3_2_mar033_5.bsp";

    /**
     * Corrupted SPK file for test : copy of another valid SPK file which
     * has been modified by deleting random lines inside the file.
     */
    private final String corruptedBSPFile = "corrupted_spk_file.bsp";

    /**
     * Empty file for test
     */
    private final String emptyTestFile = "empty_file.bsp";

    /**
     * Empty file for test- with wrong extension
     */
    private final String pckTPCTestFile = "pck_file.tpc";

    /**
     * File which has a SPK data type which is not currently handled by BIM.
     * Either this type is not implemented or just unexpected.
     * For this first test we use type 01, which is currently not implemented.
     * This must be updated when type 01 is implemented.
     */
    private final String notHandledDataTypeTestFile = "type1_ison.bsp";

    /**
     * Assert that we can open a file, check the path is fine and retrieve the summary format.
     * Tests : 
     * {@link DafHandle#openReadDAF(String)}
     * {@link DafHandle#handleToFilenameDAF(int)}
     * {@link DafHandle#getSummaryFormatDAF(int)}
     * {@link DafHandle#closeDAF(int)}
     * @throws IOException
     * @throws PatriusException 
     * @throws URISyntaxException 
     */
    @Test
    public void testSimple() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        // Declaring the path of the file
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"plu058.bsp").toURI());

        final int handle = DafHandle.openReadDAF(spk.getPath());
        final String pathF = DafHandle.handleToFilenameDAF(handle);

        Assert.assertEquals(spk.getAbsolutePath(), pathF);

        final int[] ndni = DafHandle.getSummaryFormatDAF(handle);
        final int[] expndni = { 2, 6 };
        Assert.assertArrayEquals(expndni, ndni);
        
        // Begin a forward search on the file.
        FindArraysDAF.beginForwardSearch(handle);

        // Search until DAF array is found
        boolean found = FindArraysDAF.findNextArray();

        // Loop while the search finds subsequent DAF arrays.
        while (found) {
            final double[] sum = FindArraysDAF.getSummaryOfArray();
            // Check for another segment
            found = FindArraysDAF.findNextArray();
        }
        
        Assert.assertFalse(found);

        // Trigger errors
        try {
            DafHandle.checkHandleAccess(handle, "REED");
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
        DafHandle.closeDAF(handle);
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
        
        // Trigger error
        try {
            DafHandle.checkHandleAccess(handle, "READ");
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
    }
    
    /**
     * @throws PatriusException 
     * 
     */
    @Test
    public void testCounterArraysLimits() throws PatriusException {
        // Invalide type
        try {
            CounterArray c = new CounterArray("blabla");
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
        //Valid type
        CounterArray c = new CounterArray("USER");
        
        //Try to increment
        try{
            c.increment();
        } catch(PatriusException e) {
            Assert.assertTrue(true);
        }
        
        //Now do a subsystem counter and increment it to the limit of the first component
        CounterArray s = new CounterArray("SUBSYSTEM");
        
    }

    /**
     * Tests to proof the well functioning of the comment reading.
     * Test inspired by the example presented in DAFEC.for
     * Tests : 
     * {@link DafReader#readComments(int ,int, int, int[], String[], boolean[])}
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     */
    @Test
    public void testDAFEC() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");

        final String[] expectedLine = { "Created by:                           Nat Bachman  (NAIF/JPL)",
                "This file provides geocentric states---locations and velocities---for the",
                "which differs from this one only in that it uses the reference" };

        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"earthstns_itrf93_201023.bsp").toURI());

        final int handle = DafHandle.openReadDAF(spk.getPath());
        final int[] nRead = new int[1];
        final String[] buffer = new String[10];
        final boolean[] done = new boolean[1];
        for (int i = 0; i <= 2; i++) {
            DafReader.readComments(handle, 10, 1000, nRead, buffer, done);
            // We compare a portion of all the comment lines read
            Assert.assertTrue(Arrays.toString(buffer).contains(expectedLine[i]));
        }
        // We didn't finish to read the comments
        Assert.assertFalse(done[0]);

        //Finish reading the comment section
        while (!done[0]) {
            DafReader.readComments(handle, 1, 1000, nRead, buffer, done);
        }
        
        DafHandle.closeDAF(handle);
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
    }
    
    /**
     * Tests the read of the summaries of a bsp file.
     * Inspired by the example presented in the routine DAFUS in the file dafps.for
     * It tests : 
     * {@link FindArraysDAF#getSummaryOfArray()}
     * {@link SpiceCommon#unpackSummary(double[], int, int, double[], int[])}
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     */
    @Test
    public void testSegmentDescriptor() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final int nd = 2;
        final int ni = 6;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];
        final double[] expectdc = { -3169195200.0000000, 1696852800.0000000 };
        final int[] expecticfirst = { 1, 0, 1, 2, 641, 310404 };
        final int[] expecticlast = { 499, 4, 1, 2, 2098633, 2098644 };
        boolean first = true;
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"de421.bsp").toURI());

        // Open a DAF for read. Return a HANDLE referring to the file.
        final int handle = DafHandle.openReadDAF(spk.getPath());

        // Begin a forward search on the file.
        FindArraysDAF.beginForwardSearch(handle);

        // Search until DAF array is found
        boolean found = FindArraysDAF.findNextArray();

        // Loop while the search finds subsequent DAF arrays.
        while (found) {
            final double[] sum = FindArraysDAF.getSummaryOfArray();
            SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);

            Assert.assertArrayEquals(expectdc, dc, 0.0);
            if (first) {
                first = false;
                Assert.assertArrayEquals(expecticfirst, ic);
            }
            // Check for another segment
            found = FindArraysDAF.findNextArray();
        }
        // for the last
        Assert.assertArrayEquals(expecticlast, ic);

        // Safely close the DAF.
        DafHandle.closeDAF(handle);
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
    }
    
    /**
     * Tests the simultaneous read of the summaries of 2 bsp files.
     * It tests : 
     * {@link FindArraysDAF#selectDaf(int)}
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     */
    @Test
    public void testChangeDaf() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");

        final int nd = 2;
        final int ni = 6;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];
        final int[] expecticfirst = { 1, 0, 1, 2, 641, 310404 };
        final int[] expecticlast = { 399065, 399, 13000, 8, 3073, 3088 };
        boolean first = true;
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"de421.bsp").toURI());
        final File spk1 = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"earthstns_itrf93_201023.bsp").toURI());

        // Open a DAF for read. Return a HANDLE referring to the file.
        final int handle = DafHandle.openReadDAF(spk.getPath());
        final int handle1 = DafHandle.openReadDAF(spk1.getPath());

        // Begin a forward search on both files.
        FindArraysDAF.beginForwardSearch(handle);
        FindArraysDAF.beginForwardSearch(handle1);
        
        // Select the first DAF to find the first summary
        FindArraysDAF.selectDaf(handle);

        // Search until DAF array is found
        boolean found = FindArraysDAF.findNextArray();

        // Loop while the search finds subsequent DAF arrays.
        while (found && first) {
            final double[] sum = FindArraysDAF.getSummaryOfArray();
            SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);

            if (first) {
                first = false;
                Assert.assertArrayEquals(expecticfirst, ic);
            }
            // Check for another segment
            found = FindArraysDAF.findNextArray();
        }
        
        // Let's go to the second file and find the last array:
        FindArraysDAF.selectDaf(handle1);
        found = FindArraysDAF.findNextArray();
        
        // Loop while the search finds subsequent DAF arrays.
        while (found) {
            final double[] sum = FindArraysDAF.getSummaryOfArray();
            SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);

            // Check for another segment
            found = FindArraysDAF.findNextArray();
        }
        
        // Restart the first file
        FindArraysDAF.beginForwardSearch(handle);
        
        // Select the first DAF to find the first summary
        FindArraysDAF.selectDaf(handle);

        // Search until DAF array is found
        found = FindArraysDAF.findNextArray();
        
        // for the last
        Assert.assertArrayEquals(expecticlast, ic);

        // Restart the second file but backwards
        FindArraysDAF.beginBackwardSearch(handle1);
        
        // Safely close the DAF.
        DafHandle.closeDAF(handle);
        DafHandle.closeDAF(handle1);
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
        
        // Now, what happens if we try to begin a forward or a backward search?
        try {
            FindArraysDAF.beginBackwardSearch(handle1);
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
    }


    /**
     * Tests the read of the summaries of a bsp file in a backwards search
     * Inspired by the example presented in the routine DAFFPA in the file daffa.for
     * It tests : 
     * {@link FindArraysDAF#getSummaryOfArray()}
     * {@link SpiceCommon#unpackSummary(double[], int, int, double[], int[])}
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     */
    @Test
    public void testSegmentDescriptorBackwards() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final int nd = 2;
        final int ni = 6;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];

        final double[] expectdc = { -3169195200.0000000, 1696852800.0000000 };
        final int[] expecticfirst = { 499, 4, 1, 2, 2098633, 2098644 };
        final int[] expecticlast = { 1, 0, 1, 2, 641, 310404 };
        boolean first = true;
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"de421.bsp").toURI());

        // Open a DAF for read. Return a HANDLE referring to the file.
        final int handle = DafHandle.openReadDAF(spk.getPath());

        // Begin a backward search on the file.
        FindArraysDAF.beginBackwardSearch(handle);

        // Search until DAF array is found
        boolean found = FindArraysDAF.findPreviousArray();

        // Loop while the search finds subsequent DAF arrays.
        while (found) {
            final double[] sum = FindArraysDAF.getSummaryOfArray();
            SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);

            Assert.assertArrayEquals(expectdc, dc, 0.0);
            if (first) {
                first = false;
                Assert.assertArrayEquals(expecticfirst, ic);
            }
            // Check for another segment
            found = FindArraysDAF.findPreviousArray();
        }
        // for the last
        Assert.assertArrayEquals(expecticlast, ic);

        // Safely close the DAF.
        DafHandle.closeDAF(handle);
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
    }
    
    /**
     * Tests the read of the summaries of a bsp file in a backwards search
     * Inspired by the example presented in the routine DAFFPA in the file daffa.for
     * It tests : 
     * {@link FindArraysDAF#getSummaryOfArray()}
     * {@link SpiceCommon#unpackSummary(double[], int, int, double[], int[])}
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     */
    @Test
    public void testErrorsFindArrays() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final int nd = 2;
        final int ni = 6;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];

        final double[] expectdc = { -3169195200.0000000, 1696852800.0000000 };
        final int[] expecticfirst = { 499, 4, 1, 2, 2098633, 2098644 };
        final int[] expecticlast = { 1, 0, 1, 2, 641, 310404 };
        boolean first = true;
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"de421.bsp").toURI());
        final File spk2 = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"mer1_ls_040128_iau2000_v1.bsp").toURI());

        try {
            FindArraysDAF.beginBackwardSearch(1);
        } catch(PatriusException e) {
            Assert.assertTrue(true);
        }
        
        try {
            FindArraysDAF.beginForwardSearch(1);
        } catch(PatriusException e) {
            Assert.assertTrue(true);
        }
        
        try {
            boolean found = FindArraysDAF.findPreviousArray();
        } catch(PatriusException e) {
            Assert.assertTrue(true);
        }
        
        try {
            boolean found = FindArraysDAF.findNextArray();
        } catch(PatriusException e) {
            Assert.assertTrue(true);
        }
        
        
        
        
        
        
        
//        
//        // Open a DAF for read. Return a HANDLE referring to the file.
//        final int handle = DafHandle.openReadDAF(spk.getPath());
//
//        // Begin a backward search on the file.
//        FindArraysDAF.beginBackwardSearch(handle);
//
//        // Search until DAF array is found
//        boolean found = FindArraysDAF.findPreviousArray();
//
//        // Loop while the search finds subsequent DAF arrays.
//        while (found) {
//            final double[] sum = FindArraysDAF.getSummaryOfArray();
//            SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);
//
//            Assert.assertArrayEquals(expectdc, dc, 0.0);
//            if (first) {
//                first = false;
//                Assert.assertArrayEquals(expecticfirst, ic);
//            }
//            // Check for another segment
//            found = FindArraysDAF.findPreviousArray();
//        }
//        // for the last
//        Assert.assertArrayEquals(expecticlast, ic);
//
//        // Safely close the DAF.
//        DafHandle.closeDAF(handle);
//        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
    }

    /**
     * Tests la lecture des nameRecord.
     * This is done in the method {@link FindArraysDAF#getNameOfArray()}
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     */
    @Test
    public void testNameArray() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"de421.bsp").toURI());
        final String expected = "DE-0421LE-0421";

        // Open a DAF for read. Return a HANDLE referring to the file.
        final int handle = DafHandle.openReadDAF(spk.getPath());

        // Begin a forward search on the file.
        FindArraysDAF.beginForwardSearch(handle);

        // Search until DAF array is found
        boolean found = FindArraysDAF.findNextArray();

        // Loop while the search finds subsequent DAF arrays.
        while (found) {
            Assert.assertEquals(expected, FindArraysDAF.getNameOfArray().trim());
            // Check for another segment
            found = FindArraysDAF.findNextArray();
        }

        // Safely close the DAF.
        DafHandle.closeDAF(handle);
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
    }

    /**
     * Test inspired by the example in dafgda.for
     * This tests several DAF reading methods including : 
     * {@link DafHandle#openReadDAF(String)}
     * {@link FindArraysDAF#beginForwardSearch(int)}
     * {@link FindArraysDAF#findNextArray()}
     * {@link FindArraysDAF#getSummaryOfArray()}
     * {@link SpiceCommon#unpackSummary(double[], int, int, double[], int[])}
     * {@link DafReader#readDataDaf(int, int, int)}
     * 
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     * @throws SpiceErrorException 
     */
    @Test
    public void testReadDataDaf() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"mer1_ls_040128_iau2000_v1.bsp").toURI());
        final int nd = 2;
        final int ni = 6;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];

        // Open a DAF for read. Return a HANDLE referring to the file.
        final int handle = DafHandle.openReadDAF(spk.getPath());

        // Begin a forward search on the file; find the first segment; read the segment summary.
        FindArraysDAF.beginForwardSearch(handle);
        final boolean found = FindArraysDAF.findNextArray();
        Assert.assertTrue(found);
        
        final double[] sum = FindArraysDAF.getSummaryOfArray();
        SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);
        
        // Assert the summary
        final double[] expectedDc = {3.15792E7, 3.1557168E9};
        final int[] expectedIc = {-253900,499,10014,8,769,784};
        Assert.assertArrayEquals(expectedDc, dc, 1E-6);
        Assert.assertArrayEquals(expectedIc, ic);
        
        // Retrieve the data begin and end addresses.
        final int begin = ic[4];
        final int end = ic[5];

        // Extract all data bounded by the begin and end addresses.
        final double[] data = DafReader.readDataDaf(handle, begin, end);
        
        //Expected data : 
        double[] expectedData = {3376.422,  -326.649,  -115.392,     0.000,     0.000,     0.000};
        
        Assert.assertArrayEquals(expectedData, Arrays.copyOfRange(data, 0, 6), 9E-4);
        Assert.assertArrayEquals(expectedData, Arrays.copyOfRange(data, 6, 12), 9E-4);
        
        // Safely close the DAF.
        DafHandle.closeDAF(handle);
        
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
    }    
}
