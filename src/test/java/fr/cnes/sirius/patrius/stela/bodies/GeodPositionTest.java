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
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.bodies;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link GeodPosition}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class GeodPositionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Stela atmospheric drag aero model
         * 
         * @featureDescription Computation of atmospheric drag perturbations, and partial derivatives.
         * 
         * @coveredRequirements
         */
        STELA_ATMOSPHERIC_DRAG_MODEL
    }

    /** The epsilon for this test. */
    private static final double EPS = 1E-14;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(GeodPositionTest.class.getSimpleName(), "STELA Geodetic coordinates");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link GeodPosition#GeodPosition(double, double)}
     * @testedMethod {@link GeodPosition#getGeodeticAltitude(Vector3D)}
     * 
     * @description tests the computation of the geodetic altitude (used for the atmospheric drag computation)
     * 
     * @input a vector representing spacecraft position
     * 
     * @output the altitude
     * 
     * @testPassCriteria references from Satlight V0
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testGetGeodeticAltitude() throws PatriusException {
        Report.printMethodHeader("testGetGeodeticLatitude", "Geodetic altitude computation", "Satlight V0", EPS,
            ComparisonType.RELATIVE);
        final Vector3D pos = new Vector3D(909525.8284259691, -6896154.870027015, 0.0);

        final GeodPosition geod = new GeodPosition(6378136.46, 0.0033528058710313043);
        final double actual = geod.getGeodeticAltitude(pos);
        final double expected = 577737.9799227957;
        Assert.assertEquals(0.0, (expected - actual) / expected, EPS);
        Report.printToReport("Altitude", expected, actual);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link GeodPosition#GeodPosition(double, double)}
     * @testedMethod {@link GeodPosition#getGeodeticLatitude(Vector3D)}
     * 
     * @description tests the computation of the geodetic latitude (used for the atmospheric drag computation)
     * 
     * @input a vector representing spacecraft position
     * 
     * @output the latitude
     * 
     * @testPassCriteria references from Satlight V0
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testGetGeodeticLatitude() throws PatriusException {
        Report.printMethodHeader("testGetGeodeticLatitude", "Geodetic latitude computation", "Satlight V0", EPS,
            ComparisonType.ABSOLUTE);
        final Vector3D pos = new Vector3D(909525.8284259691, -6896154.870027015, 0.0);

        final GeodPosition geod = new GeodPosition(6378136.46, 0.0033528058710313043);
        final double actual = geod.getGeodeticLatitude(pos);
        final double expected = 0.;
        Assert.assertEquals(expected, actual, EPS);
        Report.printToReport("Latitude", expected, actual);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link GeodPosition#GeodPosition(double, double)}
     * @testedMethod {@link GeodPosition#getGeodeticLongitude(Vector3D, AbsoluteDate)}
     * 
     * @description tests the computation of the geodetic longitude (used for the atmospheric drag computation)
     * 
     * @input a vector representing spacecraft position
     * 
     * @output the longitude
     * 
     * @testPassCriteria references from Stela v2.6
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testGetGeodeticLongitude() throws PatriusException {
        Report.printMethodHeader("testGetGeodeticLongitude", "Geodetic longitude computation", "STELA 2.6", EPS,
            ComparisonType.ABSOLUTE);
        final Vector3D pos = new Vector3D(6396427.751410523, -6150458.5420089355, -13428.448409354902);
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(13832 * 86400 + 35);

        final GeodPosition geod = new GeodPosition(Constants.CNES_STELA_AE, 1. / 0.29825765000000E+03);
        final double actual = geod.getGeodeticLongitude(pos, date);
        final double expected = 4.580052999730927;
        Assert.assertEquals(expected, actual, EPS);
        Report.printToReport("Longitude", expected, actual);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link GeodPosition#GeodPosition(double, double)}
     * @testedMethod {@link GeodPosition#getTloc(Vector3D, Vector3D, AbsoluteDate)}
     * 
     * @description tests the computation of the local time (used for the atmospheric drag computation)
     * 
     * @input a vector representing spacecraft position
     * 
     * @output the local time
     * 
     * @testPassCriteria references from Satlight V0
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testTLoc() throws PatriusException {
        Report.printMethodHeader("testTLoc", "Local time computation", "Satlight V0", EPS, ComparisonType.ABSOLUTE);
        final Vector3D pos = new Vector3D(6396427.751410523, -6150458.5420089355, -13428.448409354902);
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1987, 11, 15),
            new TimeComponents(18, 37, 20.21273203194), TimeScalesFactory.getTAI());

        final GeodPosition geod = new GeodPosition(6378136.46, 0.0033528058710313043);
        final Vector3D positionSun = new Vector3D(1.4795463294220422E+11,
            new Vector3D(-0.6004931649183609, -0.7336373199120156, -0.31809470558097175));
        final double actual = geod.getTloc(pos, positionSun, date);
        final double expected = 17.694928235373492;
        Assert.assertEquals(expected, actual, EPS);
        Report.printToReport("Local time", expected, actual);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link GeodPosition#GeodPosition(double, double)}
     * @testedMethod {@link GeodPosition#getGeodeticLatitude(Vector3D)}
     * 
     * @description TU for code coverage
     * 
     * @input a vector representing spacecraft position
     * 
     * @output the computed latitude, or the thrown exceptions
     * 
     * @testPassCriteria code coverage
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testForCoverage() throws PatriusException {
        final GeodPosition geod = new GeodPosition(6378136.46, 0.0033528058710313043);
        boolean rez = false;
        try {
            // latitude is NaN, an exception should be raised:
            geod.getGeodeticLatitude(Vector3D.ZERO);
            Assert.fail();
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // latitude very close to 90°:
        Vector3D pos = new Vector3D(0., 0., 9000000);
        double getGeodeticLatitudeExepected = FastMath.PI / 2;
        double getGeodeticLatitudeActual = geod.getGeodeticLatitude(pos);
        Assert.assertEquals(getGeodeticLatitudeExepected, getGeodeticLatitudeActual, Precision.EPSILON);

        // latitude very close to 0°:
        pos = new Vector3D(9000000, 0., 0.);
        getGeodeticLatitudeExepected = 0.0;
        getGeodeticLatitudeActual = geod.getGeodeticLatitude(pos);
        Assert.assertEquals(getGeodeticLatitudeExepected, getGeodeticLatitudeActual, Precision.EPSILON);

        // NaN
        final GeodPosition geod2 = new GeodPosition(6378136.46, Double.NaN);
        pos = new Vector3D(9000000, 10, 10);

        try {
            geod2.getGeodeticLatitude(pos);
            Assert.fail();
        } catch (final Exception e) {
            // Expected
            Assert.assertTrue(true);
        }

    }
}
