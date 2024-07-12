/**
 * HISTORY
 * VERSION:4.11:DM:DM-3268:22/05/2023:[PATRIUS] Creation d'une classe GeodeticTargetDirection
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for {@link BodyPointTargetDirection}.
 * </p>
 * 
 * @author Pierre Préault
 * 
 */

public class BodyPointTargetDirectionTest {

    /**
     * @testType UT
     * 
     * @testedMethod {@link BodyPointTargetDirection(final BodyShape body, final GeodeticPoint targetIn)} (FIXME)
     * 
     * @description Test that the created direction contains the GeodeticPoint. FIXME
     * 
     * @input a frame, an body shape, a date and a GeodeticPoint. FIXME
     * 
     * @output a line containing the input point.
     * 
     * @testPassCriteria the line contain the input GeodeticPoint. FIXME
     * @throws PatriusException
     *         if a frame problem occurs
     */

    @Test
    public void testBodyPointTargetDirectionCreation() throws PatriusException {
        // Initialization of the date and frame
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();

        // Creation of the body shape
        final OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
        // Creation of the GeodeticPoint
        final GeodeticPoint point = new GeodeticPoint(0.9, 2.5, 0);

        // Construction of the direction
        final BodyPointTargetDirection direction = new BodyPointTargetDirection(model, point);
        // Definition of the origin
        final ConstantPVCoordinatesProvider origin = new ConstantPVCoordinatesProvider(new Vector3D(0, 0, 0), frame);

        // Creation of the line that shall contain the point
        final Vector3D cartesianPoint = model.transform(point);
        final Line line = direction.getLine(origin, date, frame);
        Assert.assertTrue(line.contains(cartesianPoint));
    }
}
