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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.attitude;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.CelestialBodyPointed;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for CelestialBodyPointed.<br>
 * Class to be merged with the existing CelestialBodyPointingTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS
 * follows different conventions regarding unit tests.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: CelestialBodyPointingTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class CelestialBodyPointingTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of celestial body pointing attitude provider
         * 
         * @featureDescription Validation of celestial body pointing attitude provider
         * 
         * @coveredRequirements DV-ATT_340
         */
        VALIDATION_CELESTIAL_BODY_POINTING;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_CELESTIAL_BODY_POINTING}
     * 
     * @testedMethod {@link CelestialBodyPointed#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * 
     * @description simple covering test
     * 
     * @input a CelestialBodyPointed
     * 
     * @output two attitudes at a given date, computed with the two different methods getAttitude()
     * 
     * @testPassCriteria the two attitudes are equal
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testGetAttitude() throws PatriusException {

        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getTAI());
        final AttitudeProvider pointing =
            new CelestialBodyPointed(frame, sun, Vector3D.PLUS_K,
                Vector3D.PLUS_I, Vector3D.PLUS_K);
        pointing.setSpinDerivativesComputation(true);
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                new Vector3D(0, 0, 3680.853673522056));
        final Orbit orbit = new KeplerianOrbit(pv, frame, date, 3.986004415e14);
        final Frame itrf = FramesFactory.getITRF();
        final Attitude attitude1 = pointing.getAttitude(orbit, date, itrf);
        Assert.assertNotNull(attitude1.getRotation().getAngle());
        Assert.assertNotNull(attitude1.getSpin());
    }

    /** Set up */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
