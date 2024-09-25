/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialPoint.BodyNature;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.EME2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.EclipticJ2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the class BasicCelestialPoint.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.13
 * 
 */
public class BasicCelestialPointTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Basic celestial point
         * 
         * @featureDescription Test basic celestial point.
         * 
         * @coveredRequirements
         */
        BASIC_CELESTIAL_POINT
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#BASIC_CELESTIAL_POINT}
     * 
     * @description functional test (getters only)
     * 
     * @testPassCriteria result is as expected: getters returns the provided values
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testBasicDefinedCelestialBody() throws PatriusException {
        final PVCoordinatesProvider ephemeris = new MeeusSun();
        final CelestialPoint point = new BasicCelestialPoint("Sun", ephemeris, 123, FramesFactory.getGCRF());
        Assert.assertEquals("Sun", point.getName());
        Assert.assertEquals(123, point.getGM(), 0.);
        Assert.assertNotNull(point.toString());
        Assert.assertEquals(ephemeris.getPVCoordinates(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF()), point
                .getEphemeris().getPVCoordinates(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF()));

        Assert.assertEquals(BodyNature.POINT, point.getBodyNature());
    }

    /**
     * @testType UT
     *
     * @description check the various methods of {@link CelestialBarycenter} class (functional test) in case of JPL
     *              ephemeris.
     *
     * @testPassCriteria result is as expected (functional)
     *
     * @referenceVersion 4.13
     *
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testCelestialBarycenterJPL() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // Build SSB and EMB
        final CelestialPoint ssb = CelestialBodyFactory.getSolarSystemBarycenter();
        final CelestialPoint emb = CelestialBodyFactory.getEarthMoonBarycenter();

        // Checks

        // toString
        Assert.assertNotNull(ssb.toString());
        Assert.assertNotNull(emb.toString());

        // Getters
        Assert.assertEquals(ssb.getGM(), 1.328905186988048E20, 1e-14);
        Assert.assertEquals(emb.getGM(), 4.0350323347908694E14, 1e-14);
        ssb.setGM(1.23456789);
        emb.setGM(2.3456789);
        Assert.assertEquals(ssb.getGM(), 1.23456789, 1e-7);
        Assert.assertEquals(emb.getGM(), 2.3456789, 1e-7);

        Assert.assertEquals(ssb.getNativeFrame(null), FramesFactory.getICRF());
        Assert.assertEquals(emb.getNativeFrame(null), FramesFactory.getEMB());

        Assert.assertEquals(ssb.getICRF(), FramesFactory.getICRF());
        Assert.assertEquals(emb.getICRF(), FramesFactory.getEMB());

        final Transform t7 = ssb.getICRF().getTransformTo(ssb.getEME2000(), AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(t7.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertTrue(t7.getAngular().getRotation()
            .isEqualTo(new EME2000Provider().getTransform(AbsoluteDate.J2000_EPOCH).getRotation()));

        final Transform t8 = ssb.getICRF().getTransformTo(ssb.getEclipticJ2000(), AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(t8.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertTrue(t8.getAngular().getRotation()
            .isEqualTo(new EclipticJ2000Provider().getTransform(AbsoluteDate.J2000_EPOCH).getRotation()));

        Assert.assertEquals(ssb.getName(), "solar system barycenter");
        Assert.assertEquals(emb.getName(), "Earth-Moon barycenter");

        Assert.assertNotNull(ssb.getEphemeris());
        Assert.assertNotNull(emb.getEphemeris());
        ssb.setEphemeris(null);
        emb.setEphemeris(null);
        Assert.assertNull(ssb.getEphemeris());
        Assert.assertNull(emb.getEphemeris());
    }

    /**
     * @testType UT
     *
     * @description check the various methods of {@link CelestialBarycenter} class (functional test) in case of BSP
     *              ephemeris.
     *
     * @testPassCriteria result is as expected (functional)
     *
     * @referenceVersion 4.13
     *
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testCelestialBarycenterBSP() throws PatriusException {
        Utils.setDataRoot("bsp");
        // Build EMB
        final CelestialPoint emb = CelestialBodyFactory.getEarthMoonBarycenter();

        // Checks

        // toString
        Assert.assertNotNull(emb.toString());

        // Getters
        Assert.assertEquals(emb.getGM(), 4.035032356254802E14, 1e-14);
        emb.setGM(2.3456789);
        Assert.assertEquals(emb.getGM(), 2.3456789, 1e-7);

        Assert.assertEquals(emb.getICRF(), FramesFactory.getEMB());

        final Transform t7 = emb.getICRF().getTransformTo(emb.getEME2000(), AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(t7.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertTrue(t7.getAngular().getRotation()
            .isEqualTo(new EME2000Provider().getTransform(AbsoluteDate.J2000_EPOCH).getRotation()));

        final Transform t8 = emb.getICRF().getTransformTo(emb.getEclipticJ2000(), AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(t8.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertTrue(t8.getAngular().getRotation()
            .isEqualTo(new EclipticJ2000Provider().getTransform(AbsoluteDate.J2000_EPOCH).getRotation()));

        Assert.assertEquals(emb.getName(), "Earth-Moon barycenter");

        Assert.assertNotNull(emb.getEphemeris());
        emb.setEphemeris(null);
        Assert.assertNull(emb.getEphemeris());
    }
}
