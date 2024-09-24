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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceKernelManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SpiceKernelManagerTest {

    /**
     * 
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws PatriusException 
     * @throws Exception 
     */
    @Test
    public void loadFilesTest() throws URISyntaxException, PatriusException, IOException {
        // Set the file
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final String fileType = "SPK";
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"mar097_20160314_20300101.bsp").toURI()).getAbsolutePath(); 
        final String file2 = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"de421.bsp").toURI()).getAbsolutePath(); 
        final String fakeFile = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"de405-ephemerides").toURI()).getAbsolutePath();
        
        // Check there is anything
        Assert.assertEquals(0, SpiceKernelManager.totalNumberOfKernel(fileType));
        // Load the file
        SpiceKernelManager.loadSpiceKernel(file);
        // Make sure 1 file was loaded
        Assert.assertEquals(1, SpiceKernelManager.totalNumberOfKernel(fileType));
        
        // If we try to load it again, there should be 2 files
        SpiceKernelManager.loadSpiceKernel(file);
        Assert.assertEquals(2, SpiceKernelManager.totalNumberOfKernel(fileType));
        
        // Add another file
        SpiceKernelManager.loadSpiceKernel(file2);
        Assert.assertEquals(3, SpiceKernelManager.totalNumberOfKernel(fileType));
        
        // If we unload the first file, we should now only erase the last load of
        // of the first file in the system
        SpiceKernelManager.unloadKernel(file);
        
        // Try loading a non existing file
        try {
            SpiceKernelManager.loadSpiceKernel("non_existing_file");
        } catch (PatriusException | IOException e) {
            Assert.assertTrue(true);
        }
        
        try {
            SpiceKernelManager.loadSpiceKernel(fakeFile);
        } catch (PatriusException | IOException e) {
            Assert.assertTrue(true);
        }

        // Clear
        SpiceKernelManager.clearAllKernels();
        
        // Reload a file
        SpiceKernelManager.loadSpiceKernel(file2);
        Assert.assertEquals(1, SpiceKernelManager.totalNumberOfKernel(fileType));
        
        //Unload it
        SpiceKernelManager.unloadKernel(file2);
        Assert.assertEquals(0, SpiceKernelManager.totalNumberOfKernel(fileType));
        
        // There should be 0 now
        Assert.assertEquals(0, SpiceKernelManager.totalNumberOfKernel(fileType));
       
        
    }
    
    @Test
    public void testOldFile() throws URISyntaxException, PatriusException, IOException {
     // Set the file
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"plu006-4.bsp")
                                                .toURI())
                                                .getAbsolutePath();
        
        //Try using an old file (does not work bc of binary format
        try {
            SpiceKernelManager.loadSpiceKernel(file);
        } catch (PatriusException e) {
           Assert.assertTrue(true); 
        }
        
        final String file2 = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"ura027-3.bsp")
            .toURI())
            .getAbsolutePath();
        
        SpiceKernelManager.loadSpiceKernel(file2);
        SpiceKernelManager.clearAllKernels();
        
    }
    
    @Test
    public void loadCorruptedSpkFileTest() throws URISyntaxException, PatriusException, IOException {
        // Set the file
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"corrupted_spk_file.bsp")
                                                .toURI())
                                                .getAbsolutePath(); 
        
        // Load the file
        try {
            SpiceKernelManager.loadSpiceKernel(file);
            Assert.fail();
        } catch (PatriusException | IOException e) {
            Assert.assertTrue(true);
        }
    }
    
    @Test
    public void loadNonSpkFileTest() throws URISyntaxException, PatriusException, IOException {
        // Set the file
        Utils.clear();
        Utils.setDataRoot("spk_ephem_data");
        final String file = new File(ClassLoader.getSystemResource("spk_ephem_data"+File.separator+"pck_file.tpc")
                                                .toURI())
                                                .getAbsolutePath();
        
        // Load the file
        try {
            SpiceKernelManager.loadSpiceKernel(file);
            Assert.fail();
        } catch (PatriusException | IOException e) {
            Assert.assertTrue(true);
        }
        
        // Check it was not loaded
        Assert.assertEquals(0, SpiceKernelManager.totalNumberOfKernel("PCK"));
        SpiceKernelManager.unloadKernel(file);
    }
}
