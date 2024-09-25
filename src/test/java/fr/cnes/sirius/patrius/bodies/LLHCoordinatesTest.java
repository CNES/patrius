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
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Test;

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

        // Check the hashCode consistency between calls
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

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different latitude
        other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0., 0.2, 10.);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different longitude
        other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0., 10.);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different height
        other = new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, 0.1, 0.2, 0.);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

}
