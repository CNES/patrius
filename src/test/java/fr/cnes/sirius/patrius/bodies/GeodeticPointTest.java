/**
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
public class GeodeticPointTest {

    @Test
    public void testSerialization() {
        // Random test
        final Random r = new Random();
        for (int i = 0; i < 1000; ++i) {
            final double lat = r.nextDouble();
            final double longi = r.nextDouble();
            final double alti = r.nextDouble();
            final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(lat), MathLib.toRadians(longi), alti);
            final GeodeticPoint point2 = TestUtils.serializeAndRecover(point);
            assertEqualsGeodeticPoint(point, point2);
        }
    }

    public static void assertEqualsGeodeticPoint(final GeodeticPoint point1, final GeodeticPoint point2) {
        Assert.assertEquals(point1.getAltitude(), point2.getAltitude(), 0);
        Assert.assertEquals(point1.getLongitude(), point2.getLongitude(), 0);
        Assert.assertEquals(point1.getLatitude(), point2.getLatitude(), 0);
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {
        final String namePoint = "point1";
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(20), MathLib.toRadians(40), 150000, namePoint);
        Assert.assertEquals(namePoint, point.getName());
        Assert.assertNotNull(new GeodeticPoint(1, 2, 3, "Point").toString());
    }
}
