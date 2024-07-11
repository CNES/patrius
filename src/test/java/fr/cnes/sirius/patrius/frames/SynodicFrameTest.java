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
 * VERSION:4.10.1:FA:FA-3267:02/12/2022:[PATRIUS] Anomalie dans la gestion des acceleration null du PVCoordinates (suite)
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2975:15/11/2021:[PATRIUS] creation du repere synodique via un LOF 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link SynodicFrame}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.8
 */
public class SynodicFrameTest {

    /**
     * @testType UT
     *
     * @description check that a synodic frame using LOFType.mQmSW is defined properly by checking
     *              transformation obtained in various
     *              configurations (center of main body,second body and in the middle of the two
     *              bodies).
     *
     * @testedMethod all {@link SynodicFrame} methods
     *
     * @testPassCriteria result is as expected (reference: math, threshold: 0)
     *
     * @referenceVersion 4.8
     *
     * @nonRegressionVersion 4.8
     */
    @Test
    public void testSynodicFramemQmSmW() throws PatriusException {
        testSynodicFrame(LOFType.mQmSW);
    }

    /**
     * @testType UT
     *
     * @description check that a synodic frame using LOFType.QSW is defined properly by checking
     *              transformation obtained in various
     *              configurations (center of main body,second body and in the middle of the two
     *              bodies).
     *
     * @testedMethod all {@link SynodicFrame} methods
     *
     * @testPassCriteria result is as expected (reference: math, threshold: 0)
     *
     * @referenceVersion 4.8
     *
     * @nonRegressionVersion 4.8
     */
    @Test
    public void testSynodicFrameQSW() throws PatriusException {
        testSynodicFrame(LOFType.QSW);
    }

    private void testSynodicFrame(final LOFType lofType) throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        // Initialization
        final Frame rootFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final PVCoordinatesProvider pvProv = CelestialBodyFactory.getMoon();
        final LocalOrbitalFrame lof = new LocalOrbitalFrame(rootFrame, lofType, pvProv, "");
        final PVCoordinates pvBody = CelestialBodyFactory.getMoon().getPVCoordinates(date,
                rootFrame);

        // Frame centered on main body and aligned with LOF
        final SynodicFrame frame1 = new SynodicFrame(lof, "", 0.);
        Assert.assertEquals(pvProv.getPVCoordinates(date, rootFrame).getPosition().getNorm(),
                pvProv.getPVCoordinates(date, frame1).getPosition().getNorm(), 1e-7);
        final Transform t1 = frame1.getTransformTo(lof, date);
        Assert.assertTrue(t1.getRotation().isIdentity());
        Assert.assertTrue(t1.getRotationRate().isZero());
        Assert.assertTrue(t1.getVelocity().isZero());
        final Transform t12 = frame1.getTransformTo(rootFrame, date);
        Assert.assertEquals(0., t12.getTranslation().getNorm(), 1E-6);

        // Frame centered on second body and aligned with LOF
        final SynodicFrame frame2 = new SynodicFrame(lof, "", 1.);
        Assert.assertTrue(pvProv.getPVCoordinates(date, frame2).equals(
            new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO, null)));
        final Transform t2 = frame2.getTransformTo(lof, date);
        Assert.assertTrue(t2.getRotation().isIdentity());
        Assert.assertTrue(t2.getRotationRate().isZero());
        Assert.assertTrue(t2.getTranslation().isZero());
        Assert.assertTrue(t2.getVelocity().isZero());

        // Frame centered on the middle of main body - second body segment and aligned with LOF
        final SynodicFrame frame3 = new SynodicFrame(lof, "", 0.5);
        final Transform t3 = frame3.getTransformTo(lof, date);
        Assert.assertTrue(t3.getRotation().isIdentity());
        Assert.assertTrue(t3.getRotationRate().isZero());
        Assert.assertTrue(t3.getVelocity().isZero());
        final Transform t32 = rootFrame.getTransformTo(frame3, date);
        Assert.assertEquals(0.,
                t32.getTranslation().subtract(pvBody.getPosition().scalarMultiply(0.5)).getNorm(),
                1E-6);
    }
}
