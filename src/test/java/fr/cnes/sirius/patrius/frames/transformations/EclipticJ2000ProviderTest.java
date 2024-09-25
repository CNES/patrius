/**
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
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-43:08/08/2023:[PATRIUS] Introduction du repère ECLIPTIC_J2000.
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.Predefined;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link EclipticJ2000Provider} class.
 */
public class EclipticJ2000ProviderTest {

    /**
     * Test {@link EclipticJ2000Provider} transform methods.
     */
    @Test
    public void testGetTransform() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // Initialization
        final EclipticJ2000Provider provider = new EclipticJ2000Provider();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Check all transformation methods are consistent
        // Transformation value is checked separately in frame test class
        final Transform tref = provider.getTransform(date, FramesFactory.getConfiguration(), true);
        final Transform t1 = provider.getTransform(date);
        final Transform t2 = provider.getTransform(date, false);
        final Transform t4 = provider.getTransform(date, FramesFactory.getConfiguration());
        final Transform t6 = provider.getTransform(date, FramesFactory.getConfiguration(), false);
        checkTransforms(tref, t1, false);
        checkTransforms(tref, t2, false);
        checkTransforms(tref, t4, false);
        checkTransforms(tref, t6, false);
    }

    @Test
    public void testFrameConversion() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(86400. * 365 * 10);
        final Transform tact = FramesFactory.getICRF().getTransformTo(
            FramesFactory.getFrame(Predefined.ECLIPTIC_J2000), date);
        final Transform tref = FramesFactory.getGCRF().getTransformTo(
            FramesFactory.getEclipticMOD(true), AbsoluteDate.J2000_EPOCH);
        checkTransforms(tref, tact, false);
    }

    /**
     * Check transforms.
     * 
     * @param t1
     *        Reference transform
     * @param t2
     *        Transform to check
     */
    private static void checkTransforms(final Transform t1, final Transform t2, final boolean computeSpinDerivatives) {
        Assert.assertEquals(0., t1.getCartesian().getPosition().distance(t2.getCartesian().getPosition()), 0.);
        Assert.assertEquals(0., t1.getCartesian().getVelocity().distance(t2.getCartesian().getVelocity()), 0.);
        Assert.assertEquals(0., Rotation.distance(t1.getAngular().getRotation(), t2.getAngular().getRotation()), 0.);
        Assert.assertEquals(0., t1.getAngular().getRotationRate().distance(t2.getAngular().getRotationRate()), 0.);
        if (computeSpinDerivatives) {
            Assert.assertEquals(0.,
                t1.getAngular().getRotationAcceleration().distance(t2.getAngular().getRotationAcceleration()), 0.);
        } else {
            Assert.assertNull(t2.getAngular().getRotationAcceleration());
        }
    }
}
