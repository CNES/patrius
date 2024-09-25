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
 * @history creation 18/10/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test class for {@link CIPCoordinates}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: CIPCoordinatesTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class CIPCoordinatesTest {

    private final double eps = Precision.EPSILON;
    private CIPCoordinates cip1;
    private CIPCoordinates cip2;

    @Before
    public void testCIPCoordinates() {
        this.cip1 = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 4, 5, 6);
        this.cip2 = new CIPCoordinates(AbsoluteDate.FIFTIES_EPOCH_TAI,
            new double[] { 1.1, 2.2, 3.3 }, new double[] { 4.4, 5.5, 6.6 });
    }

    @Test
    public void testGetDate() {
        Assert.assertEquals(0, AbsoluteDate.J2000_EPOCH.durationFrom(this.cip1.getDate()), this.eps);
        Assert.assertEquals(0, AbsoluteDate.FIFTIES_EPOCH_TAI.durationFrom(this.cip2.getDate()), this.eps);
        Assert.assertEquals(0, AbsoluteDate.J2000_EPOCH.durationFrom(CIPCoordinates.ZERO.getDate()), this.eps);
    }

    @Test
    public void testGetCIPMotion() {
        Assert.assertArrayEquals(new double[] { 1, 3, 5 }, this.cip1.getCIPMotion(), this.eps);
        Assert.assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, this.cip2.getCIPMotion(), this.eps);
        Assert.assertArrayEquals(new double[3], CIPCoordinates.ZERO.getCIPMotion(), this.eps);
    }

    @Test
    public void testGetCIPMotionTimeDerivatives() {
        Assert.assertArrayEquals(new double[] { 2, 4, 6 }, this.cip1.getCIPMotionTimeDerivatives(), this.eps);
        Assert.assertArrayEquals(new double[] { 4.4, 5.5, 6.6 }, this.cip2.getCIPMotionTimeDerivatives(), this.eps);
        Assert.assertArrayEquals(new double[3], CIPCoordinates.ZERO.getCIPMotionTimeDerivatives(), this.eps);
    }

    @Test
    public void testGetX() {
        Assert.assertEquals(1, this.cip1.getX(), this.eps);
    }

    @Test
    public void testGetxP() {
        Assert.assertEquals(2, this.cip1.getxP(), this.eps);
    }

    @Test
    public void testGetY() {
        Assert.assertEquals(3, this.cip1.getY(), this.eps);
    }

    @Test
    public void testGetyP() {
        Assert.assertEquals(4, this.cip1.getyP(), this.eps);
    }

    @Test
    public void testGetS() {
        Assert.assertEquals(5, this.cip1.getS(), this.eps);
    }

    @Test
    public void testGetsP() {
        Assert.assertEquals(6, this.cip1.getsP(), this.eps);
    }

    @Test
    public void testIsZero() {
        Assert.assertTrue(CIPCoordinates.ZERO.isCIPMotionZero());
        Assert.assertTrue(CIPCoordinates.ZERO.isCIPMotionTimeDerivativesZero());
        Assert.assertFalse(this.cip1.isCIPMotionZero());
        Assert.assertFalse(this.cip1.isCIPMotionTimeDerivativesZero());
    }

    @Test
    public void testToString() {
        Assert.assertEquals("CIPCoordinates{2000-01-01T11:59:27.816: x=1.000000, y=3.000000, s=5.000000, "
                + "x'=2.000000, y'=4.000000, s'=6.000000}", this.cip1.toString());
    }

    @Test
    public void testEqualsAndHashCode() {

        // New instance
        final CIPCoordinates instance = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 4, 5, 6);

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
        CIPCoordinates other = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 4, 5, 6);

        Assert.assertEquals(other, instance);
        Assert.assertEquals(instance, other);
        Assert.assertEquals(other.hashCode(), instance.hashCode());

        // Different date
        other = new CIPCoordinates(AbsoluteDate.FIFTIES_EPOCH_TAI, 1, 2, 3, 4, 5, 6);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different x
        other = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 10, 2, 3, 4, 5, 6);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different xP
        other = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 10, 3, 4, 5, 6);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different y
        other = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 10, 4, 5, 6);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different yP
        other = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 10, 5, 6);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different s
        other = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 4, 10, 6);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different sP
        other = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 4, 5, 10);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

    @Test
    public void testSerialization() {
        final CIPCoordinates cip = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 4, 5, 6);
        final CIPCoordinates deserializedCIP = TestUtils.serializeAndRecover(cip);
        Assert.assertEquals(cip, deserializedCIP);
    }
}
