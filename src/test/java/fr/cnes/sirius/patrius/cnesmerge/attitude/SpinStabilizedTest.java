/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 26/4/2013
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
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
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.SpinStabilized;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
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
 * Unit tests for SpinStabilized.<br>
 * Class to be merged with the existing SpinStabilized in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS
 * follows different conventions regarding unit tests.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: SpinStabilizedTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class SpinStabilizedTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the spin stabilized
         * 
         * @featureDescription Validation of the spin stabilized
         * 
         * @coveredRequirements DV-ATT_340
         */
        VALIDATION_SPIN_STABILIZED;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_SPIN_STABILIZED}
     * 
     * @testedMethod {@link SpinStabilized#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * 
     * @description simple covering test
     * 
     * @input a SpinStabilized
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
    public final void testGetAttitude() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789), TimeScalesFactory.getUTC());
        final double rate = 2.0 * FastMath.PI / (12 * 60);
        final AttitudeProvider law = new SpinStabilized(new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY),
            date, Vector3D.PLUS_K, rate);

        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                new Vector3D(0, 0, 3680.853673522056));
        final Orbit orbit = new KeplerianOrbit(pv, FramesFactory.getGCRF(), date, 3.986004415e14);
        final Frame itrf = FramesFactory.getITRF();
        law.setSpinDerivativesComputation(true);
        final Attitude attitude1 = law.getAttitude(orbit, date, itrf);
        final Attitude attitude2 = law.getAttitude(orbit, date, itrf);
        Assert.assertEquals(attitude1.getRotation().getAngle(), attitude2.getRotation().getAngle(), 0.0);
        Assert.assertEquals(attitude1.getSpin(), attitude2.getSpin());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_SPIN_STABILIZED}
     * 
     * @testedMethod {@link SpinStabilized#getUnderlyingAttitudeLaw()}
     * 
     * @description simple covering test
     * 
     * @input a SpinStabilized
     * 
     * @output an attitude provider
     * 
     * @testPassCriteria the attitude provider is the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetAttitudeProvider() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789), TimeScalesFactory.getUTC());
        final double rate = 2.0 * FastMath.PI / (12 * 60);
        final SpinStabilized law = new SpinStabilized(new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY),
            date, Vector3D.PLUS_K, rate);
        Assert.assertEquals(ConstantAttitudeLaw.class, law.getUnderlyingAttitudeLaw().getClass());
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public final void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
