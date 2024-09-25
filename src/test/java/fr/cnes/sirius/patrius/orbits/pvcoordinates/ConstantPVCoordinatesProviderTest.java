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
 * @history creation 10/05/2022
 *
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the class {@link ConstantPVCoordinatesProvider}
 * 
 * @author Hugo Barrere
 */
public class ConstantPVCoordinatesProviderTest {

    /** Arbitrary date for testing purpose. */
    private final static AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;

    /** Relative tolerance factor for comparison. */
    private final static double RELTOL = 1E-15;

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-dataCNES-2003");
    }

    /**
     * Check the pv coordinates frame transformation between gcrf and a translated frame.
     * @throws PatriusException
     */
    @Test
    public void checkPVTranslatedFrame() throws PatriusException {

        // position / velocity of the object
        final Frame gcrf = FramesFactory.getGCRF();
        final Vector3D position = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D velocity = new Vector3D(-8.657, 657.5764, 567.1596);

        // PV coordinates computed in gcrf frame
        final ConstantPVCoordinatesProvider constantPV = new ConstantPVCoordinatesProvider(
                new PVCoordinates(position, velocity), gcrf);

        // Simple translation of the input frame
        // origin and target creation
        final Vector3D translation = new Vector3D(10, 10, 10);
        final Transform t = new Transform(DATE, translation);
        final Frame translated = new Frame(gcrf, t, "gcrf translated", true);

        final PVCoordinates pvOut = constantPV.getPVCoordinates(DATE, translated);

        Assert.assertEquals(position.add(new Vector3D(-10, -10, -10)), pvOut.getPosition());
        Assert.assertEquals(velocity, pvOut.getVelocity());
    }

    /**
     * Check the pv coordinates computation of the moon between the inertial moon frame
     * and the GCRF frame.
     * 
     * @throws PatriusException
     */
    @Test
    public void checkMoonPVCoordinates() throws PatriusException {

        // coordinates computed in non-inertial frame
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final Frame moonFrame = moon.getInertialFrame(IAUPoleModelType.CONSTANT);

        // moon center position in its attached frame {0, 0, 0}
        final ConstantPVCoordinatesProvider moonPvProvider = new ConstantPVCoordinatesProvider(
                Vector3D.ZERO, moonFrame);

        // moon center coordinates in gcrf
        final Frame gcrf = FramesFactory.getGCRF();

        final PVCoordinates moonPv = moonPvProvider.getPVCoordinates(DATE, gcrf);
        final PVCoordinates expected = moon.getPVCoordinates(DATE, gcrf);

        checkVector3DEquality(expected.getPosition(), moonPv.getPosition(), 1E-6, RELTOL);
        checkVector3DEquality(expected.getVelocity(), moonPv.getVelocity(), 1E-12, RELTOL);
    }

    /**
     * @throws PatriusException
     *         if date of interpolation is too near from min and max input dates compare to Lagrange
     *         order
     * @description Evaluate the ephemeris serialization / deserialization process.
     *
     * @testPassCriteria The ephemeris can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final Frame gcrf = FramesFactory.getGCRF();
        final Vector3D position = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D velocity = new Vector3D(-8.657, 657.5764, 567.1596);

        // PV coordinates computed in gcrf frame
        final ConstantPVCoordinatesProvider constantPV = new ConstantPVCoordinatesProvider(
                new PVCoordinates(position, velocity), gcrf);
        final ConstantPVCoordinatesProvider deserializedConstantPV = TestUtils
                .serializeAndRecover(constantPV);

        Assert.assertEquals(constantPV.getPVCoordinates(DATE, gcrf),
                deserializedConstantPV.getPVCoordinates(DATE, gcrf));
    }

    /**
     * Check the equality between two {@link Vector3D}.
     * 
     * @param expected
     *        the expected value
     * @param actual
     *        the actual value
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    private static void checkVector3DEquality(final Vector3D expected, final Vector3D actual,
            final double absTol, final double relTol) {
        // absolute difference vector
        final Vector3D delta = actual.subtract(expected);

        // check absolute difference
        Assert.assertEquals(0, MathLib.abs(delta.getX()), absTol);
        Assert.assertEquals(0, MathLib.abs(delta.getY()), absTol);
        Assert.assertEquals(0, MathLib.abs(delta.getZ()), absTol);
        // check relative difference
        Assert.assertEquals(0, MathLib.abs(delta.getX()) / actual.getX(), relTol);
        Assert.assertEquals(0, MathLib.abs(delta.getY()) / actual.getY(), relTol);
        Assert.assertEquals(0, MathLib.abs(delta.getZ()) / actual.getZ(), relTol);
    }
}
