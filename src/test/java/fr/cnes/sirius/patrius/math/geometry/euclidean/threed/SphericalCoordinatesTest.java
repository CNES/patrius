/**
 * Copyright 2011-2021 CNES
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
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test class for {@link SphericalCoordinates} class.
 * 
 * @since 4.7
 */
public class SphericalCoordinatesTest {

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of the {@link SphericalCoordinates} class.
     * 
     * @testPassCriteria the parameters are as expected (reference from geodetic point and Vector3D classes)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testGetters() {
        // Test vs GeodeticPoint in case of (latitude, longitude, altitude) construction
        final GeodeticPoint reference1 = new GeodeticPoint(MathLib.toRadians(20), MathLib.toRadians(40), 150000, "");
        final SphericalCoordinates actual1 = new SphericalCoordinates(MathLib.toRadians(20), MathLib.toRadians(40), 150000);
        Assert.assertEquals(reference1.getLatitude(), actual1.getDelta(), 0.);
        Assert.assertEquals(reference1.getLongitude(), actual1.getAlpha(), 0.);
        Assert.assertEquals(reference1.getAltitude(), actual1.getNorm(), 0.);
        
        // Test vs Vector3D
        final Vector3D reference2 = new Vector3D(1., 2., 3.);
        final SphericalCoordinates actual2 = new SphericalCoordinates(reference2);
        Assert.assertEquals(reference2.getDelta(), actual2.getDelta(), 0);
        Assert.assertEquals(reference2.getAlpha(), actual2.getAlpha(), 0.);
        Assert.assertEquals(reference2.getNorm(), actual2.getNorm(), 0.);

        final SphericalCoordinates actual3 = reference2.getSphericalCoordinates();
        Assert.assertEquals(reference2.getDelta(), actual3.getDelta(), 0);
        Assert.assertEquals(reference2.getAlpha(), actual3.getAlpha(), 0.);
        Assert.assertEquals(reference2.getNorm(), actual3.getNorm(), 0.);
    }

    /**
     * Test serialization.
     */
    @Test
    public void testSerialization() {
        // Random test
        final Random r = new Random();
        for (int i = 0; i < 1000; ++i) {
            final double lat = r.nextDouble();
            final double longi = r.nextDouble();
            final double alti = r.nextDouble();
            final SphericalCoordinates point = new SphericalCoordinates(MathLib.toRadians(lat), MathLib.toRadians(longi), alti);
            final SphericalCoordinates point2 = TestUtils.serializeAndRecover(point);
            Assert.assertEquals(point.getNorm(), point2.getNorm(), 0);
            Assert.assertEquals(point.getAlpha(), point2.getAlpha(), 0);
            Assert.assertEquals(point.getDelta(), point2.getDelta(), 0);
        }
    }
}
