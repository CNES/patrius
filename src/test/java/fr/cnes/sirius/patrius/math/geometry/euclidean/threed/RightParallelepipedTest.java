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
 * @history creation 21/05/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION:4.1.1:DM:1796:10/09/2018:remove Parallelepiped extension
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import org.junit.Test;
import org.testng.Assert;

/**
 * Test class for {@link RightParallelepiped}.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 4.1
 */
public class RightParallelepipedTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Parallelepiped shape
         * 
         * @featureDescription Creation of a rectangle plate shape, computation of distances and
         *                     intersections with lines and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120,
         *                      DV-GEOMETRIE_130, DV-GEOMETRIE_140
         */
        PARALLELEPIPED_SHAPE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLELEPIPED_SHAPE}
     * 
     * @testedMethod {@link RightParallelepiped#getCrossSection(Vector3D)}
     * 
     * @description Check all methods of {@link RightParallelepiped} class.
     * 
     * @input A {@link RightParallelepiped}
     * 
     * @output output of {@link RightParallelepiped} methods
     * 
     * @testPassCriteria output of methods is as expected (reference computed manually since a
     *                   parallelepiped is a simple geometric form)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testRightParallelepiped() {

        // Initialization
        final RightParallelepiped parallelepiped = new RightParallelepiped(2., 3., 4.);

        // Checks
        Assert.assertEquals(2., parallelepiped.getCrossSection(Vector3D.PLUS_I), 0.);
        Assert.assertEquals(0., parallelepiped.getSurfX(), 2);
        Assert.assertEquals(0., parallelepiped.getSurfY(), 3);
        Assert.assertEquals(0., parallelepiped.getSurfZ(), 4.);
        Assert.assertEquals(new RightParallelepiped(2., 3., 4.), parallelepiped);

    }
}
