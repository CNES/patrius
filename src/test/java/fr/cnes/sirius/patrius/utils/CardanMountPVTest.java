/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 18/10/2011
 *
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPV;

/**
 * Cardan pv mount test class.
 * 
 * @author Julie Anton
 * 
 */
public class CardanMountPVTest {
    /** Features description. */
    public enum features {
        /**
         * @featureTitle Cardan pv mount
         * 
         * @featureDescription Cardan pv mount test
         * 
         * @coveredRequirements DV-COORD_140
         */
        PV_COORDINATES_CARDAN
    }

    /** Smallest positive number such that 1 - EPSILON is not numerically equal to 1. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PV_COORDINATES_CARDAN}
     * 
     * @testedMethod {@link CardanMountPV#getXangle()}
     * @testedMethod {@link CardanMountPV#getYangle()}
     * @testedMethod {@link CardanMountPV#getRange()}
     * @testedMethod {@link CardanMountPV#getPosition()}
     * 
     * @description test the constructor and the assessors
     * 
     * @input CardanMountPV cardan = (0.1,0.5,2.,1.3,1.2,0.3) : cardan pv coordinates
     * 
     * @output the components
     * 
     * @testPassCriteria the x angle should be 0.1, the y angle should be 0.5, the range should be 2.0, the x angle rate
     *                   should be 1.3, the y angle rate should be 1.2 and the range rate should be 0.3 with an epsilon
     *                   of 1e-16 due to machine errors only (getters on values given at the construction).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void constructorTest() {
        final CardanMountPV cardan = new CardanMountPV(0.1, 0.5, 2., 1.3, 1.2, 0.3);

        Assert.assertEquals(0.1, cardan.getXangle(), this.machineEpsilon);
        Assert.assertEquals(0.5, cardan.getYangle(), this.machineEpsilon);
        Assert.assertEquals(2., cardan.getRange(), this.machineEpsilon);
        Assert.assertEquals(1.3, cardan.getXangleRate(), this.machineEpsilon);
        Assert.assertEquals(1.2, cardan.getYangleRate(), this.machineEpsilon);
        Assert.assertEquals(0.3, cardan.getRangeRate(), this.machineEpsilon);

        Assert.assertEquals(0.1, cardan.getPosition().getX(), this.machineEpsilon);
        Assert.assertEquals(0.5, cardan.getPosition().getY(), this.machineEpsilon);
        Assert.assertEquals(2., cardan.getPosition().getZ(), this.machineEpsilon);
        Assert.assertEquals(1.3, cardan.getVelocity().getX(), this.machineEpsilon);
        Assert.assertEquals(1.2, cardan.getVelocity().getY(), this.machineEpsilon);
        Assert.assertEquals(0.3, cardan.getVelocity().getZ(), this.machineEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PV_COORDINATES_CARDAN}
     * 
     * @testedMethod {@link CardanMountPV#toString()}
     * 
     * @description test the method toString()
     * 
     * @input CardanMountPV cardan = (0.1,0.5,2.,1.3,1.2,0.3) : Cardan coordinates
     * 
     * @output the String representation of the coordinates
     * 
     * @testPassCriteria the String representation should be "{P(0.1, 0.5, 2.0), V(1.3, 1.2, 0.3)}"
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void toStringTest() {
        final CardanMountPV cardan = new CardanMountPV(0.1, 0.5, 2., 1.3, 1.2, 0.3);

        Assert.assertEquals("{P(0.1, 0.5, 2.0), V(1.3, 1.2, 0.3)}", cardan.toString());
    }

    @Test
    public void testEqualsAndHashCode() {

        // New instance
        final CardanMountPV instance = new CardanMountPV(0.1, 0.5, 2., 1.3, 1.2, 0.3);

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
        CardanMountPV other = new CardanMountPV(0.1, 0.5, 2., 1.3, 1.2, 0.3);

        Assert.assertEquals(other, instance);
        Assert.assertEquals(instance, other);
        Assert.assertEquals(other.hashCode(), instance.hashCode());

        // Different x angle
        other = new CardanMountPV(1., 0.5, 2., 1.3, 1.2, 0.3);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different y angle
        other = new CardanMountPV(0.1, 1., 2., 1.3, 1.2, 0.3);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different range
        other = new CardanMountPV(0.1, 0.5, 1., 1.3, 1.2, 0.3);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different x angle rate
        other = new CardanMountPV(0.1, 0.5, 2., 1., 1.2, 0.3);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different y angle rate
        other = new CardanMountPV(0.1, 0.5, 2., 1.3, 1., 0.3);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different range rate
        other = new CardanMountPV(0.1, 0.5, 2., 1.3, 1.2, 1.);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

    @Test
    public void testSerialization() {
        final CardanMountPV cardan = new CardanMountPV(0.1, 0.5, 2., 1.3, 1.2, 0.3);
        final CardanMountPV deserializedCardan = TestUtils.serializeAndRecover(cardan);
        Assert.assertEquals(cardan, deserializedCardan);
    }
}
