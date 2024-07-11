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
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link SphericalGridAttractionLoader} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class SphericalGridAttractionLoaderTest {

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
    public final void loadTest() throws PatriusException {
        // Load model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_sphere.txt";
        final SphericalGridAttractionLoader loader = new SphericalGridAttractionLoader(filename);
        final AttractionData data = loader.getData();
        // Check read data
        // GM
        Assert.assertEquals(7.126999798434674500e-04 * 1E9, data.getGM(), 0.);
        // Center of mass
        Assert.assertEquals(0, new Vector3D(0, 0, 0).distance(data.getCenterOfMass()), 0);
        // Lower corner
        Assert.assertTrue(data.getGrid().isInsideGrid(new Vector3D(8000, 0, 0)));
        Assert.assertFalse(data.getGrid().isInsideGrid(new Vector3D(7999.9, 0, 0)));
        // Upper corner
        Assert.assertTrue(data.getGrid().isInsideGrid(new Vector3D(20000, 0, 0)));
        Assert.assertFalse(data.getGrid().isInsideGrid(new Vector3D(20000.1, 0, 0)));
        // Attraction data
        Assert.assertEquals(12025, data.getData().length);
        Assert.assertEquals(new SphericalCoordinates(MathLib.toRadians(-30.), 0, 8000).getCartesianCoordinates().distance(data.getData()[0].getPosition()), 0.);
        Assert.assertEquals(0, new Vector3D(-6.935359604856378000e-00, 0, 4.307520969788253600e-00).scalarMultiply(data.getGM() / 1E9).distance(data.getData()[0].getAcceleration()), 1E-16);
        Assert.assertEquals(8.793338319128628200e-02 * data.getGM() / 1E3, data.getData()[0].getPotential(), 0.);
        
        // Failed to load (non-existent file)
        try {
            new SphericalGridAttractionLoader("DummyFileName");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }
}
