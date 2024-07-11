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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.attitude;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for LofOffset.<br>
 * Class to be merged with the existing LofOffsetTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS
 * follows different conventions regarding unit tests.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: LofOffsetTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class LofOffsetTest {

    /*
     * The following code is lifted from Orekit.
     * It should NOT BE MERGED BACK into Orekit!
     */

    /** Computation date */
    private AbsoluteDate date;

    /** Body mu */
    private double mu;

    /** Reference frame = ITRF 2005C */
    private Frame frameITRF2005;

    /** Earth shape */
    OneAxisEllipsoid earthSpheric;

    /** Orbit */
    CircularOrbit orbit;

    /** Satellite position */
    PVCoordinates pvSatEME2000;

    /**
     * Set up
     */
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
            this.mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

            // Elliptic earth shape
            this.earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

            // Satellite position
            this.orbit =
                new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, MathLib.toRadians(50.), MathLib.toRadians(150.),
                    MathLib.toRadians(5.300), PositionAngle.MEAN,
                    FramesFactory.getEME2000(), this.date, this.mu);
            this.pvSatEME2000 = this.orbit.getPVCoordinates();

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    /**
     * Clean up
     */
    @After
    public void tearDown() {
        this.date = null;
        this.frameITRF2005 = null;
        this.earthSpheric = null;
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
         * @featureTitle Validation of the LOF offset class
         * 
         * @featureDescription Validation of the LOF offset class
         * 
         * @coveredRequirements DV-ATT_340
         */
        VALIDATION_LOF_OFFSET;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_LOF_OFFSET}
     * 
     * @testedMethod {@link LofOffset#LofOffset(Frame, LOFType, RotationOrder, double, double, double)}
     * 
     * @description simple covering test; test the throwing of an exception when the frame is not a pseudo
     *              inertial frame.
     * 
     * @input a LofOffset
     * 
     * @output an exception
     * 
     * @testPassCriteria the calling of the constructor throws an exception
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testNonPseudoInertialFrame() throws PatriusException {

        try {
            new LofOffset(this.frameITRF2005, LOFType.LVLH);
            Assert.fail();
        } catch (final PatriusException e1) {
        }
    }
}
