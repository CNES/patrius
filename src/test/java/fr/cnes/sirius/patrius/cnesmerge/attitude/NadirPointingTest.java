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
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.attitude;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinates;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for NadirPointing.<br>
 * Class to be merged with the existing NadirPointingTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS
 * follows different conventions regarding unit tests.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: NadirPointingTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class NadirPointingTest {

    /*
     * The following code is lifted from Orekit.
     * It should NOT BE MERGED BACK into Orekit!
     */

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF 2005C
    private CelestialBodyFrame frameITRF2005;

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Computation date
            this.date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            // Body mu
            this.mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.frameITRF2005 = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
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
         * @featureTitle Validation of the nadir pointing attitude provider
         * 
         * @featureDescription Validation of the nadir pointing attitude provider
         * 
         * @coveredRequirements DV-ATT_340
         */
        VALIDATION_NADIR_POINTING;
    }

    /**
     * Class for testing purposes only
     */
    public class NadirPointingClassTest extends NadirPointing {

        /** Serial number. */
        private static final long serialVersionUID = 6001658266102226970L;

        /**
         * Constructor
         * 
         * @param bodyShape
         *        the shape
         */
        public NadirPointingClassTest(final EllipsoidBodyShape bodyShape) {
            super(bodyShape);
        }

        /**
         * Test method.
         */
        public void test() throws PatriusException {
            // Satellite position as circular parameters
            final double raan = 270.;
            final CircularOrbit circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.), MathLib.toRadians(raan),
                    MathLib.toRadians(5.300 - raan), PositionAngle.MEAN,
                    FramesFactory.getEME2000(), NadirPointingTest.this.date, NadirPointingTest.this.mu);

            final PVCoordinates actual =
                this.getTargetPV(circ, NadirPointingTest.this.date, NadirPointingTest.this.frameITRF2005);
            // re-implement the method:

            // final Vector3D intersectionP = getTargetPoint(circ, date, frameITRF2005);
            // final double h = 0.1;
            // final double scale = 1.0 / (2 * h);
            // final Vector3D intersectionM1h = getTargetPoint(circ, date.shiftedBy(-h), frameITRF2005);
            // final Vector3D intersectionP1h = getTargetPoint(circ, date.shiftedBy( h), frameITRF2005);
            // final Vector3D intersectionV = new Vector3D(scale, intersectionP1h, -scale, intersectionM1h);
            // final PVCoordinates expected = new PVCoordinates(intersectionP, intersectionV);

            // transform from specified reference frame to body frame
            final Transform refToBody =
                NadirPointingTest.this.frameITRF2005.getTransformTo(this.getBodyShape().getBodyFrame(),
                    NadirPointingTest.this.date);

            final EllipsoidBodyShape bodyShape = (EllipsoidBodyShape) this.getBodyShape();

            // sample intersection points in current date neighborhood
            final double h = 0.01;
            final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
            final PVCoordinates pvProvM2h = circ.getPVCoordinates(NadirPointingTest.this.date.shiftedBy(-2 * h),
                NadirPointingTest.this.frameITRF2005);
            final PVCoordinates pvProvM1h = circ.getPVCoordinates(NadirPointingTest.this.date.shiftedBy(-h),
                NadirPointingTest.this.frameITRF2005);
            final PVCoordinates pvProvh = circ.getPVCoordinates(NadirPointingTest.this.date,
                NadirPointingTest.this.frameITRF2005);
            final PVCoordinates pvProvP1h = circ.getPVCoordinates(NadirPointingTest.this.date.shiftedBy(+h),
                NadirPointingTest.this.frameITRF2005);
            final PVCoordinates pvProvP2h = circ.getPVCoordinates(NadirPointingTest.this.date.shiftedBy(+2 * h),
                NadirPointingTest.this.frameITRF2005);
            sample.add(NadirPointingTest.nadirRef(
                new TimeStampedPVCoordinates(NadirPointingTest.this.date.shiftedBy(-2 * h), pvProvM2h),
                refToBody.shiftedBy(-2 * h), bodyShape, this.getBodyFrame()));
            sample.add(NadirPointingTest.nadirRef(
                new TimeStampedPVCoordinates(NadirPointingTest.this.date.shiftedBy(-h), pvProvM1h),
                refToBody.shiftedBy(-h), bodyShape, getBodyFrame()));
            sample.add(NadirPointingTest.nadirRef(new TimeStampedPVCoordinates(NadirPointingTest.this.date, pvProvh),
                refToBody, bodyShape, this.getBodyFrame()));
            sample.add(NadirPointingTest.nadirRef(
                new TimeStampedPVCoordinates(NadirPointingTest.this.date.shiftedBy(+h), pvProvP1h),
                refToBody.shiftedBy(+h), bodyShape, getBodyFrame()));
            sample.add(NadirPointingTest.nadirRef(
                new TimeStampedPVCoordinates(NadirPointingTest.this.date.shiftedBy(+2 * h), pvProvP2h),
                refToBody.shiftedBy(+2 * h), bodyShape, this.getBodyFrame()));

            // use interpolation to compute properly the time-derivatives
            final TimeStampedPVCoordinates expected = TimeStampedPVCoordinates.interpolate(NadirPointingTest.this.date,
                CartesianDerivativesFilter.USE_P, sample);

            Assert.assertEquals(expected.getPosition(), actual.getPosition());
            Assert.assertEquals(expected.getVelocity(), actual.getVelocity());
        }
    }

    /**
     * Compute ground point in nadir direction, in reference frame.
     * 
     * @param scRef
     *        spacecraft coordinates in reference frame
     * @param refToBody
     *        transform from reference frame to body frame
     * @param frame
     *        frame
     * @param bodyShape
     *        body shape
     * @return intersection point in body frame (only the position is set!)
     * @exception PatriusException
     *            if line of sight does not intersect body
     */
    private static TimeStampedPVCoordinates nadirRef(final TimeStampedPVCoordinates scRef, final Transform refToBody,
                                                     final EllipsoidBodyShape bodyShape, final Frame frame)
        throws PatriusException {

        final Vector3D satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // satellite position in geodetic coordinates
        final BodyPoint epSat = bodyShape.buildPoint(satInBodyFrame, frame, scRef.getDate(), "");

        // nadir position in geodetic coordinates
        final EllipsoidPoint gpNadir = new EllipsoidPoint(bodyShape, new LLHCoordinates(
            bodyShape.getLLHCoordinatesSystem(), epSat.getLLHCoordinates().getLatitude(), epSat.getLLHCoordinates()
                .getLongitude(), 0.0), "");

        // nadir point position in body frame
        final Vector3D pNadirBody = gpNadir.getPosition();

        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        return new TimeStampedPVCoordinates(scRef.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_NADIR_POINTING}
     * 
     * @testedMethod {@link NadirPointing#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description simple covering test
     * 
     * @input a NadirPointing
     * 
     * @output a PVCoordinates
     * 
     * @testPassCriteria the output is the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testGetTargetPV() throws PatriusException {
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);
        final NadirPointingClassTest pointing = new NadirPointingClassTest(earthShape);
        pointing.test();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_NADIR_POINTING}
     * 
     * @testedMethod {@link NadirPointing#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link NadirPointing#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * 
     * @description simple covering test; test the throwing of an exception when the satellite collides with the body
     *              shape.
     * 
     * @input a NadirPointing
     * 
     * @output two exceptions
     * 
     * @testPassCriteria the calling of the getAttitude() methods throws an exception
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testCollision() {
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136, 0.0,
            FramesFactory.getEME2000());
        final NadirPointingClassTest pointing = new NadirPointingClassTest(earthShape);
        pointing.setSpinDerivativesComputation(true);
        final CircularOrbit circ = new CircularOrbit(6378136, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.date, this.mu);
        try {
            pointing.getAttitude(circ, this.date, this.frameITRF2005);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true); // Expected
        }
    }
}
