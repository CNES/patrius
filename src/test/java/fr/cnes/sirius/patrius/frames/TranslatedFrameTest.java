/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.8:DM:DM-2968:15/11/2021:[PATRIUS] gestion du centre des reperes 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for class {@link TranslatedFrame}.
 */
public class TranslatedFrameTest {

    private static final String translatedFrameName = "Translated frame";

    /**
     * Test transformation of TranslatedFrame being a translation of EME2000 frame:
     * Axis should remain identical, only center of frame should have changed (by translation).
     */
    @Test
    public void testTransformFromPseudoInertialFrame() throws PatriusException {
        // Build translated frame compared to EME2000 frame
        final Frame frame = FramesFactory.getEME2000();
        final PVCoordinatesProvider translationProvider = new MeeusSun();
        final TranslatedFrame translatedFrame = new TranslatedFrame(frame, translationProvider,
            translatedFrameName);

        // Transform
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Transform transform = frame.getTransformTo(translatedFrame, date);

        // Check built frame as same axis than EME2000 but with translated center
        Assert.assertTrue(transform.getRotation().isIdentity());
        Assert.assertTrue(transform.getRotationRate().isZero());
        Assert.assertTrue(transform.getTranslation().equals(
            new MeeusSun().getPVCoordinates(date, frame).getPosition()));
        Assert.assertEquals(translatedFrame.getCenter(), translationProvider);
    }

    /**
     * Test transformation of TranslatedFrame being a translation of ITRF frame:
     * Axis should remain identical, only center of frame should have changed (by translation).
     */
    @Test
    public void testTransformFromNonPseudoInertialFrame() throws PatriusException {
        // Build translated frame compared to ITRF frame
        final Frame frame = FramesFactory.getITRF();
        final PVCoordinatesProvider translationProvider = new MeeusSun();
        final TranslatedFrame translatedFrame = new TranslatedFrame(frame, translationProvider,
            translatedFrameName);

        // Transform
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Transform transform = frame.getTransformTo(translatedFrame, date);

        // Check built frame as same axis than ITRF but with translated center
        Assert.assertTrue(transform.getRotation().isIdentity());
        Assert.assertTrue(transform.getRotationRate().isZero());
        Assert
            .assertTrue(transform.getTranslation().equals(new MeeusSun().getPVCoordinates(date, frame).getPosition()));
        Assert.assertEquals(translatedFrame.getCenter(), translationProvider);
    }

    /**
     * Test transformation of TranslatedFrame being a translation of topocentric frame:
     * Axis should remain identical, only center of frame should have changed (by translation).
     */
    @Test
    public void testTransformFromTopocentricFrame() throws PatriusException {
        // Build translated frame compared to the topocentric frame
        final OneAxisEllipsoid earthSpheric = new OneAxisEllipsoid(6378136.460, 0.,
            FramesFactory.getITRF());
        final EllipsoidPoint point1 = new EllipsoidPoint(earthSpheric, earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(45.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame frame = new TopocentricFrame(point1, "lat 45");
        final PVCoordinatesProvider translationProvider = new MeeusSun();
        final TranslatedFrame translatedFrame = new TranslatedFrame(frame, translationProvider,
            translatedFrameName);

        // Transform
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Transform transform = frame.getTransformTo(translatedFrame, date);

        // Check built frame as same axis than the topocentric frame but with translated center
        Assert.assertTrue(transform.getRotation().isIdentity());
        Assert.assertTrue(transform.getRotationRate().isZero());
        Assert.assertTrue(transform.getTranslation().equals(
            new MeeusSun().getPVCoordinates(date, frame).getPosition()));
        Assert.assertEquals(translatedFrame.getCenter(), translationProvider);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        FramesFactory.clear();
    }
}
