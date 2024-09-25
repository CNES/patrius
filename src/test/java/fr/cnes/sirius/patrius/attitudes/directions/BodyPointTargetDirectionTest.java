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
 * HISTORY
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3268:22/05/2023:[PATRIUS] Creation d'une classe GeodeticTargetDirection
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link BodyPointTargetDirection}.
 * 
 * @author Pierre Préault
 */
public class BodyPointTargetDirectionTest {

    /**
     * @testType UT
     * 
     * @testedMethod {@link BodyPointTargetDirection#BodyPointTargetDirection(BodyPoint)}
     * 
     * @description Test that the created direction contains the BodyPoint.
     * 
     * @input a frame, an body shape, a date and a BodyPoint.
     * 
     * @output a line containing the input point.
     * 
     * @testPassCriteria the line contain the input BodyPoint.
     * @throws PatriusException
     *         if a frame problem occurs
     */
    @Test
    public void testBodyPointTargetDirectionCreation() throws PatriusException {
        // Initialization of the date and frame
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final CelestialBodyFrame frame = FramesFactory.getITRF();

        // Creation of the body shape
        final OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame, "spheroid");

        // Creation of the EllipsoidPoint
        final EllipsoidPoint point = new EllipsoidPoint(model, model.getLLHCoordinatesSystem(), 0.9, 2.5, 0, "");

        // Construction of the direction
        final BodyPointTargetDirection direction = new BodyPointTargetDirection(point);
        // Definition of the origin
        final ConstantPVCoordinatesProvider origin = new ConstantPVCoordinatesProvider(new Vector3D(0, 0, 0), frame);

        // Creation of the line that shall contain the point
        final Vector3D cartesianPoint = point.getPosition();
        final Line line = direction.getLine(origin, date, frame);
        Assert.assertTrue(line.contains(cartesianPoint));
    }
}
