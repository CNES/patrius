/**
 *
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
    private CIPCoordinates cip;

    @Before
    public void testCIPCoordinates() {
        this.cip = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testGetDate() {
        Assert.assertEquals(0, AbsoluteDate.J2000_EPOCH.durationFrom(this.cip.getDate()), this.eps);
    }

    @Test
    public void testGetCIPMotion() {
        Assert.assertArrayEquals(new double[] { 1, 3, 5 }, this.cip.getCIPMotion(), this.eps);
    }

    @Test
    public void testGetCIPMotionTimeDerivatives() {
        Assert.assertArrayEquals(new double[] { 2, 4, 6 }, this.cip.getCIPMotionTimeDerivatives(), this.eps);
    }

    @Test
    public void testGetX() {
        Assert.assertEquals(1, this.cip.getX(), this.eps);
    }

    @Test
    public void testGetxP() {
        Assert.assertEquals(2, this.cip.getxP(), this.eps);
    }

    @Test
    public void testGetY() {
        Assert.assertEquals(3, this.cip.getY(), this.eps);
    }

    @Test
    public void testGetyP() {
        Assert.assertEquals(4, this.cip.getyP(), this.eps);
    }

    @Test
    public void testGetS() {
        Assert.assertEquals(5, this.cip.getS(), this.eps);
    }

    @Test
    public void testGetsP() {
        Assert.assertEquals(6, this.cip.getsP(), this.eps);
    }

}
