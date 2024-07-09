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
 * @history creation 30/09/2016
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for Earth center to a celestial body direction : the central body's center is the target point.
 *              </p>
 * 
 * @author rodriguest
 * 
 * @version $Id: EarthToCelestialBodyCenterDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 3.3
 * 
 */
public class EarthToCelestialBodyCenterDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Direction "Earth center to the center of a center body"
         * 
         * @featureDescription Direction from the Earth center described by a PVCoordinatesProvider
         *                     to the center of a central body
         *                     *
         * @coveredRequirements -
         */
        EARTH_CENTER_TO_CENTRAL_BODY_CENTER_DIRECTION
    }

    /** Celestial body : the Sun. */
    private CelestialBody sun;

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_CENTER_TO_CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthToCelestialBodyCenterDirection #getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of the direction Earth center => celestial body center
     *              (the central body's center is the target point),
     *              and getting of the vector associated to a given origin expressed in a frame, at a date.
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one from the origin to the central body's center,
     *                   when expressed in the wanted frame. The 1.0e-14 epsilon is the simple double comparison
     *                   epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetVector() throws PatriusException {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        // frame creation : MOD
        final Frame mod = FramesFactory.getMOD(true);

        // Date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Sun in output frame
        final Vector3D expectedDir = this.sun.getPVCoordinates(date, mod).getPosition();

        // direction creation
        final EarthToCelestialBodyCenterDirection direction = new EarthToCelestialBodyCenterDirection(this.sun);

        // Actual direction
        final Vector3D actualDir = direction.getVector(null, date, mod);

        // Comparisons
        Assert.assertEquals(MathLib.abs(expectedDir.getX() - actualDir.getX()) / expectedDir.getX(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedDir.getY() - actualDir.getY()) / expectedDir.getY(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedDir.getZ() - actualDir.getZ()) / expectedDir.getZ(), 0.,
            this.comparisonEpsilon);
        Report.printToReport("Direction", expectedDir, actualDir);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthCenterDirection#getTargetPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by
     *              a central body (the central body's center is the target point),
     *              and getting of the target (center of the body) PV coordinates
     *              expressed in a frame, at a date.
     * 
     * @output PVCoordinates
     * 
     * @testPassCriteria the returned target coordinates are the one expected.
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because the computations
     *                   involve
     *                   here no mechanics algorithms.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetTarget() throws PatriusException {

        // frame creation : MOD
        final Frame mod = FramesFactory.getMOD(true);

        // Date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Sun is the target
        final PVCoordinates target = this.sun.getPVCoordinates(date, mod);
        final Vector3D expectedPos = target.getPosition();
        final Vector3D expectedVel = target.getVelocity();

        // direction creation
        final EarthToCelestialBodyCenterDirection direction = new EarthToCelestialBodyCenterDirection(this.sun);

        // Actual target
        final PVCoordinates actualTarget = direction.getTargetPVCoordinates(date, mod);
        final Vector3D actualPos = actualTarget.getPosition();
        final Vector3D actualVel = actualTarget.getVelocity();

        // Comparisons
        Assert.assertEquals(MathLib.abs(expectedPos.getX() - actualPos.getX()) / expectedPos.getX(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedPos.getY() - actualPos.getY()) / expectedPos.getY(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedPos.getZ() - actualPos.getZ()) / expectedPos.getZ(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedVel.getX() - actualVel.getX()) / expectedVel.getX(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedVel.getY() - actualVel.getY()) / expectedVel.getY(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedVel.getX() - actualVel.getX()) / expectedVel.getX(), 0.,
            this.comparisonEpsilon);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthCenterDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described
     *              by the direction Earth center => celestial body center (the central body's center is the target
     *              point),
     *              and getting of the line containing a given origin and the associated vector.
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the celestial body (the Sun) and the (0,0,0) point of the
     *                   gcrf frame (the Earth).
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetLine() throws PatriusException {

        // frame creation
        final Frame mod = FramesFactory.getMOD(false);

        // Date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Transform from GCRF to MOD
        final Transform outTransform = FramesFactory.getGCRF().getTransformTo(mod, date);

        // Sun in output frame
        final Vector3D sunCenter = this.sun.getPVCoordinates(date, mod).getPosition();

        // direction creation
        final EarthToCelestialBodyCenterDirection direction = new EarthToCelestialBodyCenterDirection(this.sun);

        // Actual line
        final Line line = direction.getLine(null, date, mod);

        // expected points : Earth and Sun
        final Vector3D earthCenter = outTransform.transformPosition(Vector3D.ZERO);

        // test of the points : they must be on the line ! (since Sun is far away, distance cannot be accurate)
        Assert.assertTrue(line.distance(sunCenter) < 1E-4);
        Assert.assertTrue(line.contains(sunCenter.normalize()));
        Assert.assertTrue(line.contains(earthCenter));
    }

    /**
     * Set up before class.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        this.sun = CelestialBodyFactory.getSun();
    }

}
