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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.attitude;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AbstractGroundPointing;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.attitudes.YawCompensation;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for YawCompensation.<br>
 * Class to be merged with the existing YawCompensationTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS
 * follows different conventions regarding unit tests.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: YawCompensationTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class YawCompensationTest {

    /*
     * The following code is lifted from Orekit.
     * It should NOT BE MERGED BACK into Orekit!
     */

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF 2005C
    private Frame frameITRF2005;

    // Satellite position
    CircularOrbit circOrbit;

    // Earth shape
    OneAxisEllipsoid earthShape;

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

            // Computation date
            this.date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            // Body mu
            final double mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

            // Satellite position
            this.circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(270.),
                    MathLib.toRadians(5.300), PositionAngle.MEAN,
                    FramesFactory.getEME2000(), this.date, mu);

            // Elliptic earth shape */
            this.earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF2005);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        this.date = null;
        this.frameITRF2005 = null;
        this.circOrbit = null;
        this.earthShape = null;
    }

    /*
     * ****
     * The unit tests below are to be merged within Orekit eventually.
     * ****
     */

    /*
     * _____. __ .______ __ __ __ _____.
     * / || | | _ \ | | | | | | / |
     * | (--`| | | |_) | | | | | | | | (--`
     * \ \ | | | / | | | | | | \ \
     * .--) | | | | |\ \ | | | `--' | .--) |
     * |_____/ |__| | _| `__| |__| \______/ |_____/
     * Copyright 2011-2022 CNES
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     * http://www.apache.org/licenses/LICENSE-2.0
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the yaw compensation attitude provider
         * 
         * @featureDescription Validation of the yaw compensation attitude provider
         * 
         * @coveredRequirements DV-ATT_340, DV-ATT_240
         */
        VALIDATION_YAW_COMPENSATION;
    }

    /**
     * Class for testing purposes only
     */
    public class YawCompensationClassTest extends YawCompensation {

        /** Serial number. */
        private static final long serialVersionUID = 4997965554494753654L;

        /**
         * Constructor
         * 
         * @param pointing
         *        the ground pointing
         */
        public YawCompensationClassTest(final AbstractGroundPointing pointing) {
            super(pointing);
        }

        /**
         * Test method.
         */
        public void test() throws PatriusException {
            // Satellite position as circular parameters
            final double raan = 270.;
            final double mu = 3.9860047e14;
            final CircularOrbit circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(raan),
                    MathLib.toRadians(5.300 - raan), PositionAngle.MEAN,
                    FramesFactory.getEME2000(), YawCompensationTest.this.date, mu);

            final PVCoordinates actual =
                this.getTargetPV(circ, YawCompensationTest.this.date, YawCompensationTest.this.frameITRF2005);
            // re-implement the method:
            final Vector3D intersectionP =
                this.getTargetPosition(circ, YawCompensationTest.this.date, YawCompensationTest.this.frameITRF2005);
            final double h = 0.1;
            final double scale = 1.0 / (2 * h);
            final Vector3D intersectionM1h =
                this.getTargetPosition(circ, YawCompensationTest.this.date.shiftedBy(-h),
                    YawCompensationTest.this.frameITRF2005);
            final Vector3D intersectionP1h =
                this.getTargetPosition(circ, YawCompensationTest.this.date.shiftedBy(h),
                    YawCompensationTest.this.frameITRF2005);
            final Vector3D intersectionV = new Vector3D(scale, intersectionP1h, -scale, intersectionM1h);
            final PVCoordinates expected = new PVCoordinates(intersectionP, intersectionV);
            Assert.assertEquals(expected.getPosition(), actual.getPosition());
        }
    };

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_YAW_COMPENSATION}
     * 
     * @testedMethod {@link YawCompensation#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description simple covering test
     * 
     * @input a Yaw Compensation law
     * 
     * @output a PVCoordinates
     * 
     * @testPassCriteria the output is the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testGetTargetPV() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final NadirPointing nadir = new NadirPointing(this.earthShape);
        final YawCompensationClassTest pointing = new YawCompensationClassTest(nadir);
        pointing.test();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_YAW_COMPENSATION}
     * 
     * @testedMethod {@link YawCompensation#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * 
     * @description simple covering test
     * 
     * @input a YawCompensation
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

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final Frame frame = FramesFactory.getEME2000();
        final NadirPointing nadir = new NadirPointing(this.earthShape);
        nadir.setSpinDerivativesComputation(true);
        final AttitudeProvider pointing = new YawCompensation(nadir);
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                new Vector3D(0, 0, 3680.853673522056));
        final Orbit orbit = new KeplerianOrbit(pv, frame, this.date, 3.986004415e14);
        final Attitude attitude1 = pointing.getAttitude(orbit, this.date, this.frameITRF2005);
        Assert.assertNotNull(attitude1.getRotation().getAngle());
        Assert.assertNotNull(attitude1.getSpin());
    }
}
