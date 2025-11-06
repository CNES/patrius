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
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * VERSION:4.14:OPENFD-310:22/08/2024: [PATRIUS] Attribut "name" dans LLHCoordinates
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.Utils;

/**
 * Unit test class for the {@link LLHCoordinates} class.
 * 
 * @author Thibaut BONIT
 *
 * @version $Id$
 *
 * @since 4.13
 * 
 */
public class LLHCoordinatesTest {

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link LLHCoordinates#LLHCoordinates(LLHCoordinatesSystem, double, double, double)}
     * @testedMethod {@link LLHCoordinates#getLLHCoordinatesSystem()}
     * @testedMethod {@link LLHCoordinates#getLatitude()}
     * @testedMethod {@link LLHCoordinates#getLongitude()}
     * @testedMethod {@link LLHCoordinates#getHeight()}
     * 
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     */
    @Test
    public void testConstructor() {

        final LLHCoordinates coordinates = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 10.);

        Assert.assertEquals(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, coordinates.getLLHCoordinatesSystem());
        Assert.assertEquals(0.1, coordinates.getLatitude(), 0.);
        Assert.assertEquals(0.2, coordinates.getLongitude(), 0.);
        Assert.assertEquals(10., coordinates.getHeight(), 0.);
    }

    /**
     * @description This test aims at testing the constructor with user-defined name of coordinates and the associated
     *              getter for the name.
     * 
     * @testedMethod {@link LLHCoordinates#LLHCoordinates(LLHCoordinatesSystem, double, double, double, String)}
     * @testedMethod {@link LLHCoordinates#getName()}
     * 
     * @testPassCriteria The instance is built without error and the getter correctly returns the expected name.
     */
    @Test
    public void testConstructorWithNameOption() {
        final double latitude = MathUtils.DEG_TO_RAD * 43;
        final double longitude = MathUtils.DEG_TO_RAD * 1;
        final double height = 35000;
        final String name = "test_name";
        final LLHCoordinates coordinates =
            new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_RADIAL, latitude, longitude, height, name);

        Assert.assertEquals(name, coordinates.getName());
    }

    /**
     * @description This test aims at testing the constructor with no name of coordinates initializes the attribute with
     *              an empty String.
     * 
     * @testedMethod {@link LLHCoordinates#LLHCoordinates(LLHCoordinatesSystem, double, double, double)}
     * @testedMethod {@link LLHCoordinates#getName()}
     * 
     * @testPassCriteria The instance is built without error and the getter correctly returns an empty String.
     */
    @Test
    public void testConstructorWithEmptyString() {
        final double latitude = MathUtils.DEG_TO_RAD * 43;
        final double longitude = MathUtils.DEG_TO_RAD * 1;
        final double height = 35000;
        final String expectedName = new String();
        final LLHCoordinates coordinates =
            new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_RADIAL, latitude, longitude, height);

        Assert.assertEquals(expectedName, coordinates.getName());
    }

    /**
     * @description Tests the equals and hashCode methods.
     *
     * @testedMethod {@link LLHCoordinates#equals(Object)}
     * @testedMethod {@link LLHCoordinates#hashCode()}
     *
     * @testPassCriteria The methods behaves as expected.
     */
    @Test
    public void testEqualsAndHashCode() {

        // New instance
        final LLHCoordinates instance = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 10.);
        final int hashCode = instance.hashCode();
        Assert.assertEquals(hashCode, instance.hashCode());
        // Compared object is null
        Assert.assertFalse(instance.equals(null));
        // Compared object is a different class
        Assert.assertFalse(instance.equals(new Object()));
        // Same instance
        Assert.assertEquals(instance, instance);

        // Same data, but different instances
        LLHCoordinates other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 10.);
        Assert.assertEquals(other, instance);
        Assert.assertEquals(instance, other);
        Assert.assertEquals(other.hashCode(), instance.hashCode());

        // Different LLH coordinates system
        other = new LLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC, 0.1, 0.2, 10.);
        checkFalseEqualsHashCode(instance, other);
        // Different latitude
        other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0., 0.2, 10.);
        checkFalseEqualsHashCode(instance, other);
        // Different longitude
        other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0., 10.);
        checkFalseEqualsHashCode(instance, other);
        // Different height
        other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 0.);
        checkFalseEqualsHashCode(instance, other);
        // Different name
        other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 10., "name");
        checkFalseEqualsHashCode(instance, other);

    }

    /**
     * @description Tests the toString() method.
     *
     * @testedMethod {@link LLHCoordinates#toString()}
     *
     * @testPassCriteria The methods behaves as expected.
     */
    @Test
    public void testToString() {

        // Instance without defined name by user
        final LLHCoordinates instance = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 10.);
        final String expStr = "surface bodycentric coord={lat=0.1, long=0.2}rad, normal height=10.0m";
        Assert.assertEquals(expStr, instance.toString());

        // Instance without defined name by user
        final LLHCoordinates instance2 =
            new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 10., "name");
        final String expStr2 = "surface bodycentric coord={lat=0.1, long=0.2}rad, normal height=10.0m, name";
        Assert.assertEquals(expStr2, instance2.toString());

    }

    /**
     * Check that two instances are not equals and that they return different hashCode value
     * 
     * @param instance instance to be tested
     * @param other other instance for check
     */
    private void checkFalseEqualsHashCode(final LLHCoordinates instance, LLHCoordinates other) {
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
