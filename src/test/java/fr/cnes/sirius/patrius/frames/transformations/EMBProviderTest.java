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
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link EMBProvider} class.
 */
public class EMBProviderTest {

    /**
     * Test {@link EMBProvider#getTransform(AbsoluteDate)}
     * Test {@link EMBProvider#getTransform(AbsoluteDate, boolean)}
     * Test {@link EMBProvider#getTransform(AbsoluteDate, fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration)}
     * Test {@link EMBProvider#getTransform(AbsoluteDate, fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration, boolean)}
     */
    @Test
    public void testGetTransform() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");
        
        // Initialization
        final EMBProvider provider = new EMBProvider();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        
        // Check all transformation methods are consistent
        // Transformation value is checked separately in frame test class
        final Transform tref = provider.getTransform(date, FramesFactory.getConfiguration(), true);
        final Transform t1 = provider.getTransform(date);
        final Transform t2 = provider.getTransform(date, false);
        final Transform t3 = provider.getTransform(date, true);
        final Transform t4 = provider.getTransform(date, FramesFactory.getConfiguration());
        final Transform t5 = provider.getTransform(date, FramesFactory.getConfiguration(), true);
        final Transform t6 = provider.getTransform(date, FramesFactory.getConfiguration(), false);
        checkTransforms(tref, t1, false);
        checkTransforms(tref, t2, false);
        checkTransforms(tref, t3, true);
        checkTransforms(tref, t4, false);
        checkTransforms(tref, t5, true);
        checkTransforms(tref, t6, false);
    }

    /**
     * Check transforms.
     * @param t1 a reference transform
     * @param t2 a transform to check
     * @param computeSpinDerivatives true if spin derivatives is computeds
     */
    private void checkTransforms(final Transform t1, final Transform t2, final boolean computeSpinDerivatives) {
        Assert.assertEquals(0., t1.getCartesian().getPosition().distance(t2.getCartesian().getPosition()), 0.);
        Assert.assertEquals(0., t1.getCartesian().getVelocity().distance(t2.getCartesian().getVelocity()), 0.);
        Assert.assertEquals(0., Rotation.distance(t1.getAngular().getRotation(), t2.getAngular().getRotation()), 0.);
        Assert.assertEquals(0., t1.getAngular().getRotationRate().distance(t2.getAngular().getRotationRate()), 0.);
        if (computeSpinDerivatives) {
            Assert.assertEquals(0., t1.getCartesian().getAcceleration().distance(t2.getCartesian().getAcceleration()), 0.);
            Assert.assertEquals(0., t1.getAngular().getRotationAcceleration().distance(t2.getAngular().getRotationAcceleration()), 0.);
        } else {
            Assert.assertNull(t2.getCartesian().getAcceleration());
            Assert.assertNull(t2.getAngular().getRotationAcceleration());
        }
    }
}
