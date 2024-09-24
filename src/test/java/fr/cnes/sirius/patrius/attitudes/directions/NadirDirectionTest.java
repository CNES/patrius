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
 * @history creation 20/06/2012
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the nadir direction.
 *              </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id: NadirDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 */
public class NadirDirectionTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Position direction
         * 
         * @featureDescription nadir direction.
         * 
         * @coveredRequirements DV-ATT_400, DV-GEOMETRIE_180
         */
        NADIR_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(NadirDirectionTest.class.getSimpleName(), "Nadir direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#NADIR_DIRECTION}
     * 
     * @testedMethod {@link NadirDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of the nadir direction and computation of the direction vector for a given orbit at
     *              given date and frame.
     * 
     * @input orbit
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the direction vector is the expected direction vector of the line which is given by the
     *                   satellite position and its nadir position at a given date. The components between both vectors
     *                   should be equal with an allowed error of 1e-14 due to computation errors.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        try {
            // gcrf
            final Frame gcrf = FramesFactory.getGCRF();
            final Frame itrf = FramesFactory.getITRF();
            // orbit creation
            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new KeplerianOrbit(7500000, 0.01, 0.2, 0, 0, 0, PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = Constants.GRIM5C1_EARTH_FLATTENING;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

            // nadir direction
            final IDirection nadirDir = new NadirDirection(earth);

            final EllipsoidPoint point = earth.buildPoint(orbit.getPVCoordinates().getPosition(), gcrf, date, "");
            final EllipsoidPoint nadir = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), point
                .getLLHCoordinates().getLatitude(), point.getLLHCoordinates().getLongitude(), 0., "");

            final Vector3D pt1 = orbit.getPVCoordinates().getPosition();
            final Vector3D pt2 = itrf.getTransformTo(gcrf, date).transformPosition(nadir.getPosition());

            final Line line = new Line(pt1, pt2);

            // expected direction vector
            final Vector3D expected = line.getDirection();

            // computed direction vector
            final Vector3D result = nadirDir.getVector(orbit, date, gcrf);

            // check direction vector
            Assert.assertEquals(expected.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getZ(), result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", expected, result);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#NADIR_DIRECTION}
     * 
     * @testedMethod {@link NadirDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of the nadir direction and computation of the line for a given orbit at
     *              given date and frame.
     * 
     * @input orbit
     * 
     * @output Line
     * 
     * @testPassCriteria the line is the expected line which is given by the satellite position and its nadir position
     *                   at a given date.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetLine() {

        try {
            // gcrf
            final Frame gcrf = FramesFactory.getGCRF();
            final Frame itrf = FramesFactory.getITRF();
            // orbit creation
            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new KeplerianOrbit(7500000, 0.01, 0.2, 0, 0, 0, PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = Constants.GRIM5C1_EARTH_FLATTENING;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

            // nadir direction
            final IDirection nadirDir = new NadirDirection(earth);

            final EllipsoidPoint point = earth.buildPoint(orbit.getPVCoordinates().getPosition(), gcrf, date, "");
            final EllipsoidPoint nadir = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), point
                .getLLHCoordinates().getLatitude(), point.getLLHCoordinates().getLongitude(), 0., "");

            final Vector3D pt1 = orbit.getPVCoordinates().getPosition();
            final Vector3D pt2 = itrf.getTransformTo(gcrf, date).transformPosition(nadir.getPosition());

            // expected line
            final Line expected = new Line(pt1, pt2);

            // computed line
            final Line result = nadirDir.getLine(orbit, date, gcrf);

            // check the lines
            final double threshold = 1e-9;
            final double angle = Vector3D.angle(result.getDirection(), expected.getDirection());
            Assert.assertTrue((angle < threshold) || (angle > (FastMath.PI - threshold))
                    && (result.distance(expected.getOrigin()) < threshold));

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
