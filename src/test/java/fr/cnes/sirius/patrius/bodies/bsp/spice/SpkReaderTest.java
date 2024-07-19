/**
 * Copyright 2023-2023 CNES
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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SpkReaderTest {
    
    /**
     * Test the SpkObjects method from the SpkReader class.
     * Inspired by the example in spkobj.for of the 
     * SPICE library
     * 
     * @throws URISyntaxException
     * @throws IOException 
     * @throws PatriusException 
     */
    @Test
    public void testSpkobjects() throws URISyntaxException, PatriusException, IOException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"mar097_20160314_20300101.bsp").toURI()).getAbsolutePath(); 
        final Set<Integer> ids = new TreeSet<Integer>();
        final int[] expectedIds = {3, 4, 10, 399, 401, 402, 499};
        
        SpkReader.spkObjects(file, ids);
        Iterator<Integer> it = ids.iterator();
        int i = 0;
        while (it.hasNext()) {
            Assert.assertEquals(expectedIds[i], it.next().intValue());
            i++;
        }
    }
    
    /**
     * Test reading the segment that contains an epoch and retrieving position from an SPK type 2.
     * Based on the example presented in spkpvn.for of the SPICE library.
     * 
     * {@link SpkReader#getStateRelativeToCenterOfMotion(int, double[], double, int[], int[])}
     * {@link SpkRecord#readType2(int, double[], double)}
     * {@link SpkRecord#evaluateType2(double, double[])}
     * {@link SpkRecord#interpolateChebyshevExpansion(double[], int, double[], double)}
     * @throws IOException
     * @throws PatriusException
     * @throws URISyntaxException 
     */
    @Test
    public void testSearchSegments() throws IOException, PatriusException, URISyntaxException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final int nd = 2;
        final int ni = 6;
        final double epoch = 10E7;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"type2_borrelly.bsp").toURI());

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
            if (dc[0] <= epoch && epoch <= dc[1]) {
                final int[] ref = new int[1];
                final int[] center = new int[1];
                final double[] state = SpkReader.getStateRelativeToCenterOfMotion(handle, sum, epoch, ref, center);
                final String ident = FindArraysDAF.getNameOfArray();
                final String frame = SpiceFrame.frameId2Name(ref[0]);
                
                Assert.assertEquals(1000005, ic[0]);
                Assert.assertEquals(10, center[0]);
                Assert.assertEquals("K014/40", ident.trim());
                Assert.assertEquals("J2000", frame);
                Assert.assertEquals(6.574612289039739E8, (new ArrayRealVector(state,0,3)).getNorm(),0);
                break;
            }

            // Check for another segment
            found = FindArraysDAF.findNextArray();
        }

        // Safely close the DAF.
        DafHandle.closeDAF(handle);
        Assert.assertTrue(DafHandle.getHandleList().isEmpty());
    }
    
    @Test
    public void testGetBoundaryDates() throws URISyntaxException, PatriusException, IOException{
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final int nd = 2;
        final int ni = 6;
        final double[] dc = new double[nd];
        final int[] ic = new int[ni];
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"mar097_20160314_20300101.bsp").toURI()).getAbsolutePath();
        
        SpiceKernelManager.loadSpiceKernel(file);
        
        final int[] handle = new int[1];
        DafHandle.isLoaded(file, handle);
        
        double start = Double.MAX_VALUE;
        double end = Double.MIN_VALUE;
        
        FindArraysDAF.beginForwardSearch(handle[0]);
       // Search until DAF array is found
        boolean found = FindArraysDAF.findNextArray();
        while (found) {
            final double[] sum = FindArraysDAF.getSummaryOfArray();
            SpiceCommon.unpackSummary(sum, nd, ni, dc, ic);
            start = MathLib.min(start, dc[0]);
            end = MathLib.max(end, dc[1]);
            

            // Check for another segment
            found = FindArraysDAF.findNextArray();
        }
        Assert.assertEquals(5.111856681855543E8, start, 0);
        Assert.assertEquals(9.46728069183919E8, end, 0);
        SpiceKernelManager.clearAllKernels();
    }
    
    @Test
    public void testGetState() throws URISyntaxException, PatriusException, IOException{
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final double epoch = 10E7;
        final Set<Integer> ids = new TreeSet<Integer>();
        final File spk = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"type2_borrelly.bsp").toURI());
        double[] lightTime = new double[1];
        
        SpiceKernelManager.loadSpiceKernel(spk.getAbsolutePath());
        double[] state;
        try {
            state = SpkReader.getStateRelativeToBody("K014/40", epoch, "J2000", "K014/40", lightTime);
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
        SpkReader.spkObjects(spk.getAbsolutePath(), ids);
        Iterator<Integer> it = ids.iterator();
        int i = 0;
        String[] bodies = new String[ids.size()];
        while (it.hasNext()) {
            bodies[i] = SpiceBody.bodyCode2Name(it.next().intValue());
            i++;
        }
        
        state = SpkReader.getStateRelativeToBody(bodies[0], epoch, "J2000", bodies[0], lightTime);
        double[] state0 = {0,0,0,0,0,0};
        Assert.assertArrayEquals(state0, state,0);
        
        state = SpkReader.getStateRelativeToBody(bodies[0], epoch, "IAU_EARTH", bodies[0], lightTime);
        Assert.assertArrayEquals(state0, state,0);
        
        try {
            state = SpkReader.getStateRelativeToBody(bodies[0], epoch, "IAU_EARTH", "EARTH", lightTime);
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
        // Another file to play with both
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"plu058.bsp").toURI()).getAbsolutePath();
        final Set<Integer> ids2 = new TreeSet<Integer>();
        
        SpiceKernelManager.loadSpiceKernel(file);
        SpkReader.spkObjects(file, ids2);
        Iterator<Integer> it2 = ids2.iterator();
        int j = 0;
        String[] bodies2 = new String[ids2.size()];
        while (it2.hasNext()) {
            bodies2[j] = SpiceBody.bodyCode2Name(it2.next().intValue());
            j++;
        }
        
        // Not inertial frame should be an exception
        try {
            state = SpkReader.getStateRelativeToBody(bodies[0], epoch, "IAU_EARTH", bodies2[0], lightTime);
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
        // Not enough data
        try {
            state = SpkReader.getStateRelativeToBody(bodies[0], epoch, "ECLIPJ2000", bodies2[0], lightTime);
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
        // Frame not recognised
        try {
            state = SpkReader.getStateRelativeToBody(bodies[0], epoch, "ECLI J2000", bodies2[0], lightTime);
        } catch (PatriusException e) {
            Assert.assertTrue(true);
        }
        
        state = SpkReader.getStateRelativeToBody(bodies[0], epoch, "ECLIPJ2000", bodies2[0], lightTime);

        
        SpiceKernelManager.clearAllKernels();
    }
    
    /**
     * Test chaining bodies in the same file and after, changing the frame.
     * @throws URISyntaxException
     * @throws PatriusException
     * @throws IOException
     */
    @Test
    public void testChainedBodies() throws URISyntaxException, PatriusException, IOException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        // Another file to play with both
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"plu058.bsp").toURI()).getAbsolutePath();
        double[] lightTime = new double[1];
        double epoch = 1E8;
        
        SpiceKernelManager.loadSpiceKernel(file);
        // We want to have Pluto (999) around the Sun Barycenter (0) in a frame different to J2000. Lets take GALACTIC.
        
        double[] state = SpkReader.getStateRelativeToBody("PLUTO", epoch, "ECLIPJ2000", "SOLAR SYSTEM BARYCENTER", lightTime);
        
        //Lets load the same file again
        SpiceKernelManager.loadSpiceKernel(file);
        SpiceKernelManager.clearAllKernels();
    }
    
    /**
     * Test the reading of the file extracting the body in it and verifying it is not recognised
     * @throws URISyntaxException
     * @throws PatriusException
     * @throws IOException
     */
    @Test
    public void checkBodies() throws URISyntaxException, PatriusException, IOException {
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final Set<Integer> ids = new TreeSet<Integer>();
        String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"mer1_ls_040128_iau2000_v1.bsp").toURI()).getAbsolutePath();
        SpkReader.spkObjects(file, ids);
        
        Iterator<Integer> it = ids.iterator();
        while (it.hasNext()) {
            final int id = it.next().intValue();
            String name = SpiceBody.bodyCode2Name(id);
            Assert.assertEquals(-253900, id);
            Assert.assertTrue(name.equals(""));
        }
    }
    
    
    /**
     * Test the evalution of type 3 SPK record. The record is already fixed (not read). This tests : 
     * {@link SpkRecord#evaluateType3(double, double[])}
     * {@link SpkRecord#evaluateChebyshevExpansion(double[], int, double[], double)}
     * 
     * Test based on the example presented in chbval.for in the SPICE library.
     * @throws PatriusException
     */
    @Test
    public void testEvaluateType3() throws PatriusException {
        double[] record = {44.0, 0.5, 3.0, 1.0, 3.0, 0.5, 1.0, 0.5, -1.0, 1.0, 
                                           1.0, 3.0, 0.5, 1.0, 0.5, -1.0, 1.0, 
                                           1.0, 3.0, 0.5, 1.0, 0.5, -1.0, 1.0, 
                                           1.0, 3.0, 0.5, 1.0, 0.5, -1.0, 1.0, 
                                           1.0, 3.0, 0.5, 1.0, 0.5, -1.0, 1.0, 
                                           1.0, 3.0, 0.5, 1.0, 0.5, -1.0, 1.0};
        double epoch = 1.0;
        
        double[] state = SpkRecord.evaluateType3(epoch, record);
        
        Assert.assertEquals( -0.340878, state[0], 0.0000001);       
    }

}
