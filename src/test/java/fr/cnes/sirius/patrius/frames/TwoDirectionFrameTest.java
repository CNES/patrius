/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-160:22/08/2024: [PATRIUS] Repere defini par 2 directions
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.MomentumDirection;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.NadirDirection;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.VelocityDirection;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.Utils;

/**
 * Class to test the TwoDirectionFrame class.
 * 
 * @author Mathilde Lefevre
 *
 */
public class TwoDirectionFrameTest {

    /**
     * Builds a TwoDirectionFrame and tests the getters for the directions and corresponding axes of the frame.
     * 
     * @throws PatriusException
     */
    @Test
    public void testGettersForTwoDirectionFrame() throws PatriusException {
        // Builds the necessary elements to create a parent frame and a coordinates provider
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate date = new AbsoluteDate(2024, 01, 24, tt);
        final CelestialBodyFrame parent = FramesFactory.getGCRF();
        final OneAxisEllipsoid earthSpheric = new OneAxisEllipsoid(6378136.460, 0.,
            parent);

        // Builds a coordinate provider in the form of an orbit
        final PVCoordinatesProvider provider = new KeplerianOrbit(7000000, 0.0, 0, 0, 0, 0, PositionAngle.TRUE,
            parent, date, Constants.CNES_STELA_MU);

        // Creates the two directions that define the frame to be built
        final IDirection firstDir = new NadirDirection(earthSpheric);
        final IDirection secondDir = new MomentumDirection(parent);

        // Provides the axes each direction has to correspond to in the desired frame
        // For instance, here the first axis of the frame is aligned with Nadir and the third axis with the momentum
        final Vector3D frameFirstAxis = Vector3D.PLUS_I;
        final Vector3D frameSecondAxis = Vector3D.PLUS_K;

        // Builds the corresponding TwoDirectionFrame
        final TwoDirectionFrame twoDirectionFrame = new TwoDirectionFrame(parent, provider, "test", firstDir,
            secondDir, frameFirstAxis, frameSecondAxis);

        // Asserts the different axes of the built frame are the ones provided
        Assert.assertEquals(firstDir, twoDirectionFrame.getDirectionOne());
        Assert.assertEquals(secondDir, twoDirectionFrame.getDirectionTwo());
        Assert.assertEquals(frameFirstAxis, twoDirectionFrame.getAxisOne());
        Assert.assertEquals(frameSecondAxis, twoDirectionFrame.getAxisTwo());

    }

    /**
     * Builds a LocalOrbitalFrame TNW and builds the same frame using a TwoDirectionFrame with the expected directions.
     * Assesses the obtained frames are identical comparing the corresponding Transform.
     * 
     * @throws PatriusException
     */
    @Test
    public void testTransformTwoDirectionFrameLOF() throws PatriusException {
        // Builds the necessary elements to create a parent frame and coordinates provider
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate date = new AbsoluteDate(2024, 01, 24, tt);
        final CelestialBodyFrame parent = FramesFactory.getGCRF();
        final OneAxisEllipsoid earthSpheric = new OneAxisEllipsoid(6378136.460, 0.,
            parent);

        // Builds the coordinates provider as a Keplerian circular orbit
        final PVCoordinatesProvider provider = new KeplerianOrbit(7000000, 0.0, 0, 0, 0, 0, PositionAngle.TRUE,
            parent, date, Constants.CNES_STELA_MU);

        // Builds the local orbital frame to be recreated using the TwoDirectionFrame class
        final LocalOrbitalFrame lof = new LocalOrbitalFrame(parent, LOFType.TNW, provider, "lof");

        // Builds the two direction to recreate the frame
        // In the first case below, the directions used are Nadir and Momentum direction
        final IDirection firstDir = new NadirDirection(earthSpheric);
        final IDirection secondDir = new MomentumDirection(parent, provider);

        // For this case, the corresponding axes are provided
        // To recreate a NTW, Nadir direction corresponds to the second direction of the frame and the Momentum
        // direction to the third
        final Vector3D frameFirstAxis = Vector3D.PLUS_J;
        final Vector3D frameSecondAxis = Vector3D.PLUS_K;

        // Builds the equivalent TwoDirectionFrame
        final TwoDirectionFrame testFrame = new TwoDirectionFrame(parent, provider, "test", firstDir, secondDir,
            frameFirstAxis, frameSecondAxis);
        // Evaluates the Transform are identical for the LOF NTW and the built TwoDirectionFrame
        evaluateTransform(lof.getTransformProvider().getTransform(date),
            testFrame.getTransformProvider().getTransform(date));
        // Verify the other transforms which take into account additional entries (they always point to the same
        // transform based on the date only)
        evaluateTransform(lof.getTransformProvider().getTransform(date),
            testFrame.getTransformProvider().getTransform(date, null));
        evaluateTransform(lof.getTransformProvider().getTransform(date),
            testFrame.getTransformProvider().getTransform(date, false));
        evaluateTransform(lof.getTransformProvider().getTransform(date),
            testFrame.getTransformProvider().getTransform(date, null, false));
        // Expected rotation rate to be verified
        final Vector3D expRotRate = new Vector3D(0., 0., 0.0010780076123988098);
        Assert.assertEquals(expRotRate,
            testFrame.getTransformProvider().getTransform(date).getAngular().getRotationRate());

        // Same test but built from Velocity and Momentum Direction
        final IDirection firstDir1 = new VelocityDirection(parent);
        final Vector3D frameFirstAxis1 = Vector3D.PLUS_I;

        // final TwoDirectionFrame testFrameFromVelocity = new TwoDirectionFrame(parent, provider, "testVel", firstDir1,
        // secondDir, frameFirstAxis1, frameSecondAxis);
        // System.out.println(MathLib.ulp(lof.getTransformProvider().getTransform(date).getRotation().getAngle()));
        // System.out.println(MathLib.ulp(testFrameFromVelocity.getTransformProvider().getTransform(date).getRotation()
        // .getAngle()));
        // evaluateTransform(lof.getTransformProvider().getTransform(date), testFrameFromVelocity.getTransformProvider()
        // .getTransform(date));

        // Same test but from Nadir and Velocity Direction
        final TwoDirectionFrame testFrameFromVelAndNadir = new TwoDirectionFrame(parent, provider, "testVelNadir",
            firstDir1, firstDir, frameFirstAxis1, frameFirstAxis);
        // Evaluates the Transform are identical for the LOF NTW and the built TwoDirectionFrame
        evaluateTransform(lof.getTransformProvider().getTransform(date), testFrameFromVelAndNadir
            .getTransformProvider()
            .getTransform(date));
        // Verify the other transforms which take into account additional entries (they always point to the same
        // transform based on the date only)
        evaluateTransform(lof.getTransformProvider().getTransform(date), testFrameFromVelAndNadir
            .getTransformProvider()
            .getTransform(date, null));
        evaluateTransform(lof.getTransformProvider().getTransform(date), testFrameFromVelAndNadir
            .getTransformProvider()
            .getTransform(date, false));
        evaluateTransform(lof.getTransformProvider().getTransform(date), testFrameFromVelAndNadir
            .getTransformProvider()
            .getTransform(date, null, false));
        // Expected rotation rate to be verified
        final Vector3D expRotRateFromVelAndNadir = new Vector3D(0., 0., 0.0010780076123987543);
        Assert.assertEquals(expRotRateFromVelAndNadir,
            testFrameFromVelAndNadir.getTransformProvider().getTransform(date).getAngular().getRotationRate());
    }


    /**
     * A method to assert two Transform are equivalent.
     * This method was taken from another class comparing transform but adapted to the specific case of a
     * TwoDirectionFrame.
     * In this specific case, rotation rate is computed differently via finite differences in the TwoDirectionFrame
     * constructor and will necessarily but different, so this element is not compared in this test class.
     * 
     * @param t1
     *        the first transform to compare
     * 
     * @param t2
     *        the second element to compare
     */
    private static void evaluateTransform(final Transform t1, final Transform t2) {
        Assert.assertEquals(t1.getDate(), t2.getDate());
        Assert.assertEquals(t1.getCartesian(), t2.getCartesian());
        Assert.assertEquals(t1.getVelocity(), t2.getVelocity());
        Assert.assertEquals(t1.getAcceleration(), t2.getAcceleration());
        Assert.assertEquals(t1.getRotation(), t2.getRotation());
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}