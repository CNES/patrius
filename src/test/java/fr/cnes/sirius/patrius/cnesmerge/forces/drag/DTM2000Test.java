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
 *
 * @history created 14/02/12
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.forces.drag;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.SolarInputs97to05;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.DTM2000;
import fr.cnes.sirius.patrius.forces.atmospheres.DTMInputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.DTMSolarData;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Additionnal unit tests for DTM2000. Should be eventually merged to Orekit.
 * 
 * @author cardosop
 * 
 * @version $Id: DTM2000Test.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class DTM2000Test {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the DTM2000 model.
         * 
         * @featureDescription Validation of the DTM2000 model.
         * 
         * @coveredRequirements DV-MOD_260
         */
        VALIDATION_DTM2000
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DTM2000}
     * 
     * @testedMethod {@link DTM2000#getData(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description Error case for this method.
     * 
     * @input for a getDensity() call, but with an altitude too low.
     * 
     * @output none.
     * 
     * @testPassCriteria OrekitException raised.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         is expected
     */
    @Test(expected = PatriusException.class)
    public void getDensityError() throws PatriusException {
        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        final DTM2000 atm = new DTM2000(in, sun, earth);

        // Computation and results
        // Altitude (9) is way too low for the model!
        atm.getData(AbsoluteDate.J2000_EPOCH, new Vector3D(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 9., 0., 0.),
            FramesFactory.getCIRF());
        // Should never arrive here
        Assert.fail();
    }

    /**
     * @throws PatriusException
     *         DTM2000 model creation
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        final BodyShape earthBody = new OneAxisEllipsoid(Constants.CNES_STELA_AE, Constants.GRIM5C1_EARTH_FLATTENING,
            FramesFactory.getCIRF());
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("ACSOL.act"));

        final DTMInputParameters parameters = new DTMSolarData(
            SolarActivityDataFactory.getSolarActivityDataProvider());

        final DTM2000 atm = new DTM2000(parameters, CelestialBodyFactory.getSun(), earthBody);
        Assert.assertTrue(parameters.equals(atm.getParameters()));
        Assert.assertTrue(earthBody.equals(atm.getEarth()));
        Assert.assertTrue(CelestialBodyFactory.getSun().equals(atm.getSun()));
    }

    /**
     * Setup for the tests.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
    }
}
