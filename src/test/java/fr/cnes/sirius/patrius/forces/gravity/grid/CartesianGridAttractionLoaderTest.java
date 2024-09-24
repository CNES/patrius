/**
 * 
 * Copyright 2021-2021 CNES
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link CartesianGridAttractionLoader} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class CartesianGridAttractionLoaderTest {

    /**
     * @testType UT
     * 
     * @description check that data from file is properly read: GM, center of mass, grid min/max, data points
     * 
     * @testPassCriteria read data is as expected (reference: from file)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void loadTest() throws PatriusException {
        // Load model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final AttractionData data = loader.getData();
        // Check read data
        // GM
        Assert.assertEquals(+0.712699979843467446E-03 * 1E9, data.getGM(), 0.);
        // Center of mass
        Assert.assertEquals(0, new Vector3D(-0.669425176616474365E01, -0.316119700588464586E02, +0.469912119313153876E-00).distance(data.getCenterOfMass()), 0);
        // Lower corner
        Assert.assertTrue(data.getGrid().isInsideGrid(new Vector3D(-40000, -40000, -20000)));
        Assert.assertFalse(data.getGrid().isInsideGrid(new Vector3D(-40000.1, -40000, -20000)));
        // Upper corner
        Assert.assertTrue(data.getGrid().isInsideGrid(new Vector3D(40000, 40000, 20000)));
        Assert.assertFalse(data.getGrid().isInsideGrid(new Vector3D(40000, 40000.1, 20000)));
        // Attraction data
        Assert.assertEquals(2601, data.getData().length);
        Assert.assertEquals(new Vector3D(-40000, -40000, -20000).distance(data.getData()[0].getPosition()), 0.);
        Assert.assertEquals(0, new Vector3D(+0.1852773432E-00, +0.1865302678E-00, +0.9398867396E-01).scalarMultiply(data.getGM() / 1E9).distance(data.getData()[0].getAcceleration()), 1E-16);
        Assert.assertEquals(+0.1669673549E-01 * data.getGM() / 1E3, data.getData()[0].getPotential(), 0.);
        
        // Failed to load (non-existent file)
        try {
            new CartesianGridAttractionLoader("DummyFileName");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }
}
