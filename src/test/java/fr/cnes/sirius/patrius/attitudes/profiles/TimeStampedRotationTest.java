/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
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
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link @link TimeStampedRotation} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.4
 */
public class TimeStampedRotationTest {

    /** 
     * @testedMethod {@link AngularVelocitiesPolynomialProfileLeg#truncateSegment(AbsoluteTimeInterval)}
     * 
     * @description tests all methods of {@link @link TimeStampedRotation} class
     * 
     * @testPassCriteria result are as expected (functional test)
     *
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public final void testTimeStampedRotation() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Rotation rotation = new Rotation(new Vector3D(2, 3, 4), 5);
        final TimeStampedRotation timeStampedRotation = new TimeStampedRotation(rotation, date);
        final TimeStampedRotation timeStampedRotation2 = new TimeStampedRotation(rotation, date);
        final TimeStampedRotation timeStampedRotation3 = new TimeStampedRotation(rotation, date.shiftedBy(2));
        
        // Checks
        Assert.assertEquals(date, timeStampedRotation.getDate());
        Assert.assertFalse(timeStampedRotation.equals(date));
        Assert.assertTrue(timeStampedRotation.equals(timeStampedRotation));
        Assert.assertTrue(timeStampedRotation.equals(timeStampedRotation2));
        Assert.assertFalse(timeStampedRotation.equals(timeStampedRotation3));
        Assert.assertNotNull(timeStampedRotation.hashCode());
    }
}
