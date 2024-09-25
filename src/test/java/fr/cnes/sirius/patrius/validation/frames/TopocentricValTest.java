/**
 * 
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
 * @history creation 18/10/2011
 *
 * HISTORY
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.frames;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPV;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPosition;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPV;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPosition;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation tests for the topocentric frame.
 * 
 * @author Julie Anton
 * 
 */
public class TopocentricValTest {

    /** Computation date. */
    private AbsoluteDate date;

    /** Reference frame = ITRF 2005. */
    private CelestialBodyFrame frameITRF2005;

    /** North topocentric frame. */
    private final String northFrame = "north topocentric frame";

    /** Validate tool */
    private Validate validate;

    /** Earth shape. */
    OneAxisEllipsoid earthSpheric;

    /** Epsilon used for distance comparison. */
    private final double epsilonDistance = Utils.epsilonTest;

    /** Epsilon used for velocity comparison. */
    private final double epsilonVelocity = Utils.epsilonTest;

    /** Epsilon used for angular velocity comparison. */
    private final double epsilonAngVelocity = Utils.epsilonTest * 1e5;

    /** Epsilon used for distance comparison. */
    private final double epsilonAngle = Utils.epsilonTest;

    /** Non regression Epsilon */
    private final double epsilonNonReg = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle transformation
         * 
         * @featureDescription transformation validation
         * 
         * @coveredRequirements DV-COORD_140, DV-COORD_150
         */
        TRANSFORMATION
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToTopocentric(PVCoordinates, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#transformFromPositionToTopocentric(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description we compare the results of the transformation from PV coordinates or position coordinates to
     *              topocentric coordinates with those given by the MSLIB.
     * 
     * @input the input is a set of position-velocity coordinates with the following values :
     *        <p>
     *        double x = 100 m
     *        </p>
     *        <p>
     *        double y = -65 m
     *        </p>
     *        <p>
     *        double z = 35 m
     *        </p>
     *        <p>
     *        double xDot = -23 m/s
     *        </p>
     *        <p>
     *        double yDot = -86 m/s
     *        </p>
     *        <p>
     *        double zDot = 12 m/s
     *        </p>
     * 
     * @output topocentric coordinates
     * 
     * @testPassCriteria the expected results are the following ones :
     *                   <p>
     *                   double elevation = 0.285441688475132 rad
     *                   </p>
     *                   <p>
     *                   double bearing = 5.70681008658840 rad
     *                   </p>
     *                   <p>
     *                   double range = 124.298028946561 m
     *                   </p>
     *                   <p>
     *                   double elevationRate = 0.0301459824501621 rad/s
     *                   </p>
     *                   <p>
     *                   double bearingRate = 0.709666080843585 rad/s
     *                   </p>
     *                   <p>
     *                   double rangeRate = 29.8476173069087 m/s
     *                   </p>
     *                   <p>
     *                   with an epsilon of 1e-12 on the distances and 1e-7 on the angles (100 times the epsilon for
     *                   double comparisons for the distance epsilon and 1e7 times the epsilon for double comparison for
     *                   the angle epsilon due to the fact that we compare physical measures and that the reference
     *                   comes from another software). As for the epsilon of non regression, it is set to 1e-14 (double
     *                   comparison).
     *                   </p>
     * 
     * @comments the references come from the MSLIB; the initial pv coordinates are directly given in the topocentric
     *           frame in which the corresponding topocentric coordinates will be expressed (this test does not aim at
     *           validating the transformation from a frame to another one).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    @Test
    public void testTransformFromPVToTopocentric() throws PatriusException {

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final Vector3D position = new Vector3D(100, -65, 35);
        final Vector3D velocity = new Vector3D(-23, -86, 12);
        final PVCoordinates pv = new PVCoordinates(position, velocity);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        final TopocentricPV topoCoordPV = topoNorth.transformFromPVToTopocentric(pv, topoNorth, this.date);

        // elevation angle
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPV.getElevation(), 0.2854416884751322,
            this.epsilonNonReg,
            0.285441688475132, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(topoNorth.getElevation(pv.getPosition(), topoNorth, this.date),
            0.2854416884751322, this.epsilonNonReg, 0.285441688475132, this.epsilonAngle, "elevation deviation");
        // azimuth angle (the reference gives the bearing : azimuth = 2PI - bearing)
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPV.getAzimuth(), 0.5763752205911837,
            this.epsilonNonReg,
            MathUtils.TWO_PI - 5.70681008658840, this.epsilonAngle, "azimuth deviation");
        this.validate.assertEqualsWithRelativeTolerance(topoNorth.getAzimuth(pv.getPosition(), topoNorth, this.date),
            0.5763752205911837, this.epsilonNonReg, MathUtils.TWO_PI - 5.70681008658840, this.epsilonAngle,
            "azimuth deviation");
        // distance from the center of the frame
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPV.getRange(), 124.29802894656054, this.epsilonNonReg,
            124.298028946561, this.epsilonDistance, "range deviation");
        this.validate.assertEqualsWithRelativeTolerance(topoNorth.getRange(pv.getPosition(), topoNorth, this.date),
            124.29802894656054, this.epsilonNonReg, 124.298028946561, this.epsilonDistance, "range deviation");

        // elevation rate
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPV.getElevationRate(), 0.030145982450162066,
            this.epsilonNonReg,
            0.0301459824501621, this.epsilonAngle, "elevation rate deviation");
        this.validate
            .assertEqualsWithRelativeTolerance(topoNorth.getElevationRate(pv, topoNorth, this.date),
                0.030145982450162066, this.epsilonNonReg, 0.0301459824501621, this.epsilonAngle,
                "elevation rate deviation");
        // azimuth rate
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPV.getAzimuthRate(), 0.7096660808435853,
            this.epsilonNonReg,
            0.709666080843585, this.epsilonAngle, "azimuth rate deviation");
        this.validate.assertEqualsWithRelativeTolerance(topoNorth.getAzimuthRate(pv, topoNorth, this.date),
            0.7096660808435853,
            this.epsilonNonReg, 0.709666080843585, this.epsilonAngle, "azimuth rate deviation");
        // range rate
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPV.getRangeRate(), 29.84761730690871,
            this.epsilonNonReg,
            29.8476173069087, this.epsilonDistance, "range rate deviation");
        this.validate.assertEqualsWithRelativeTolerance(topoNorth.getRangeRate(pv, topoNorth, this.date),
            29.84761730690871,
            this.epsilonNonReg, 29.8476173069087, this.epsilonDistance, "range rate deviation");

        // Conversion from Cartesian coordinates (only the position) to topocentric coordinates
        final TopocentricPosition topoCoordPosition = topoNorth.transformFromPositionToTopocentric(pv.getPosition(),
            topoNorth, this.date);

        // elevation angle
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPosition.getElevation(), 0.2854416884751322,
            this.epsilonNonReg,
            0.285441688475132, this.epsilonAngle, "elevation deviation");
        // azimuth angle (the reference gives the bearing : azimuth = 2PI - bearing)
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPosition.getAzimuth(), 0.5763752205911837,
            this.epsilonNonReg,
            MathUtils.TWO_PI - 5.70681008658840, this.epsilonAngle, "azimuth deviation");
        // distance from the center of the frame
        this.validate.assertEqualsWithRelativeTolerance(topoCoordPosition.getRange(), 124.29802894656054,
            this.epsilonNonReg,
            124.298028946561, this.epsilonDistance, "range deviation");
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromTopocentricToPV(TopocentricPV)}
     * @testedMethod {@link TopocentricFrame#transformFromTopocentricToPosition(TopocentricPosition)}
     * 
     * @description we compare the results of the transformation from topocentric coordinates to PV or position
     *              coordinates to with those given by the MSLIB.
     * 
     * @input the input is a set of topocentric coordinates (TopocentricCoordinates) with the following values :
     *        <p>
     *        double elevation = 0.285441688475 rad
     *        </p>
     *        <p>
     *        double bearing = 5.70681008659 rad
     *        </p>
     *        <p>
     *        double range = 124.298028947 m
     *        </p>
     *        <p>
     *        double elevationRate = 0.0301459824502 rad/s
     *        </p>
     *        <p>
     *        double bearingRate = 0.709666080844 rad/s
     *        </p>
     *        <p>
     *        double rangeRate = 29.8476173069 m/s
     *        </p>
     *        <p>
     * 
     * @output pv coordinates
     * 
     * @testPassCriteria the expected results are the following ones :
     *                   <p>
     *                   double x = 100.000000000461 m
     *                   </p>
     *                   <p>
     *                   double y = -65.0000000000726 m
     *                   </p>
     *                   <p>
     *                   double z = 35.0000000001080 m
     *                   </p>
     *                   <p>
     *                   double xDot = -23.0000000000644 m/s
     *                   </p>
     *                   <p>
     *                   double yDot = -86.0000000003254 m/s
     *                   </p>
     *                   <p>
     *                   double zDot = 12.0000000000111 m/s
     *                   </p>
     *                   <p>
     *                   with an epsilon of 1e-12 on the distances and 1e-7 on the angles (100 times the epsilon for
     *                   double comparisons for the distance epsilon and 1e7 times the epsilon for double comparison for
     *                   the angle epsilon due to the fact that we compare physical measures and that the reference
     *                   comes from another software). As for the epsilon of non regression, it is set to 1e-14 (double
     *                   comparison).
     *                   </p>
     * 
     * @comments the references come from the MSLIB
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformFromTopocentricToPV() {

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Topocentric coordinates
        final TopocentricPV topoCoord = new TopocentricPV(0.285441688475, MathUtils.TWO_PI - 5.70681008659,
            124.298028947, 0.0301459824502, 0.709666080844, 29.8476173069);

        // Conversion from topocentric coordinates to pv coordinates
        final PVCoordinates pv = topoNorth.transformFromTopocentricToPV(topoCoord);

        // x component
        this.validate.assertEqualsWithRelativeTolerance(pv.getPosition().getX(), 100.00000000046127,
            this.epsilonNonReg,
            100.000000000461, this.epsilonDistance, "x component");
        // y component
        this.validate.assertEqualsWithRelativeTolerance(pv.getPosition().getY(), -65.0000000000726, this.epsilonNonReg,
            -65.0000000000726, this.epsilonDistance, "y component");
        // z component
        this.validate.assertEqualsWithRelativeTolerance(pv.getPosition().getZ(), 35.00000000010798, this.epsilonNonReg,
            35.0000000001080, this.epsilonDistance, "z component");
        // x dot component
        this.validate.assertEqualsWithRelativeTolerance(pv.getVelocity().getX(), -23.000000000064393,
            this.epsilonNonReg,
            -23.0000000000644, this.epsilonDistance, "x dot component");
        // y dot compoment
        this.validate.assertEqualsWithRelativeTolerance(pv.getVelocity().getY(), -86.00000000032543,
            this.epsilonNonReg,
            -86.0000000003254, this.epsilonDistance, "y dot component");
        // z dot component
        this.validate.assertEqualsWithRelativeTolerance(pv.getVelocity().getZ(), 12.000000000011138,
            this.epsilonNonReg,
            12.0000000000111, this.epsilonDistance, "z dot component");

        // Conversion from topocentric coordinates to position coordinates
        final Vector3D position = topoNorth.transformFromTopocentricToPosition(topoCoord.getTopocentricPosition());

        // x component
        this.validate.assertEqualsWithRelativeTolerance(position.getX(), 100.00000000046127, this.epsilonNonReg,
            100.000000000461, this.epsilonDistance, "x component");
        // y component
        this.validate.assertEqualsWithRelativeTolerance(position.getY(), -65.0000000000726, this.epsilonNonReg,
            -65.0000000000726, this.epsilonDistance, "y component");
        // z component
        this.validate.assertEqualsWithRelativeTolerance(position.getZ(), 35.00000000010798, this.epsilonNonReg,
            35.0000000001080,
            this.epsilonDistance, "z component");
    }

    /**
     * @testType DTC
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToTopocentric(PVCoordinates, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#transformFromPositionToTopocentric(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description test when the azimuth cannot be defined : the satellite is located at zenith
     * 
     * @input the input values are the following :
     *        <p>
     *        double x = 0 m
     *        </p>
     *        <p>
     *        double y = 0 m
     *        </p>
     *        <p>
     *        double z = 1 m
     *        </p>
     * 
     * @output topocentric coordinates
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @comments the MSLIB put an arbitrary value (0.0) for the bearing in this case
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationLimitCaseTopocentric() {

        final String errorMessage = "the topocentric coordinates are not defined because the azimuth is undefined.";

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Satellite at zenith
        final Vector3D position = new Vector3D(0, 0, 1);
        try {
            topoNorth.transformFromPositionToTopocentric(position, topoNorth, this.date);
            Assert.fail(errorMessage);
        } catch (final PatriusException ex) {
            // expected
        }
        try {
            topoNorth.transformFromPVToTopocentric(new PVCoordinates(position, Vector3D.NaN), topoNorth, this.date);
            Assert.fail(errorMessage);
        } catch (final PatriusException ex) {
            // expected
        }
    }

    /**
     * @testType DTC
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToTopocentric(PVCoordinates, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#transformFromPositionToTopocentric(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description test when the cardan mounting cannot be defined : the satellite is located close to the
     *              north axis
     * 
     * @input the input values are the following :
     *        <p>
     *        double x = 1 m
     *        </p>
     *        <p>
     *        double y = 0 m
     *        </p>
     *        <p>
     *        double z = 0 m
     *        </p>
     * 
     * @output cardan coordinates
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationLimitCaseCardan() {

        final String errorMessage = "the cardan mounting is not defined.";

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Satellite close to the north axis (low passage)
        final Vector3D position = new Vector3D(1, 0, 0);
        try {
            topoNorth.transformFromPositionToCardan(position, topoNorth, this.date);
            Assert.fail(errorMessage);
        } catch (final PatriusException ex) {
            // expected
        }
        try {
            topoNorth.transformFromPVToCardan(new PVCoordinates(position, Vector3D.NaN), topoNorth, this.date);
            Assert.fail(errorMessage);
        } catch (final PatriusException ex) {
            // expected
        }
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToCardan(PVCoordinates, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#getXangleCardan(Vector3D, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#getXangleCardanRate(PVCoordinates, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#getYangleCardan(Vector3D, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#getYangleCardanRate(PVCoordinates, Frame, AbsoluteDate)}
     * 
     * 
     * @throws PatriusException
     *         if conversion fails
     * 
     * @description we test the transformation from PVCoordinates to Cardan coordinates
     * 
     * @input the input values are the following :
     *        <p>
     *        pos_car(1) = 100. m pos_car(2) = -65. m pos_car(3) = 35. m vit_car(1) = -23. m/s vit_car(2) = -86. m/s
     *        vit_car(3) = 12. m/s
     *        </p>
     * 
     * @output cardan coordinates
     * 
     * @testPassCriteria outputs from computation should be equal to expected values :
     *                   <p>
     *                   X (cardan) = 1.076854957875316 rad Y (cardan) = 0.9348634533745954 rad distance =
     *                   124.29802894656054 m Vit_X = 0.4091743119266047 rad/s Vit_Y = -0.6368236827766715 rad/s
     *                   Vit_distance = 29.84761730690871 m/s
     *                   </p>
     * 
     *                   The epsilon used for angles is 1e-7 (1e7 times the epsilon for double
     *                   comparison for the angle epsilon due to the fact that we compare
     *                   physical measures and that the reference comes from another software).
     *                   The epsilon used for distances is 1e-12 on the distances (100 times the
     *                   doubles comparison epsilon).
     *                   The epsilon used for velocities is the same as the epsilon used for
     *                   distances, 1e-12
     *                   The epsilon used for angular velocities is the same as the epsilon used for
     *                   angles, 1e-7
     * 
     *                   Note the actual used X angle value is the opposite of the X (cardan) one because
     *                   the software used to generate the reference results uses a different convention.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationPVtoCardan() throws PatriusException {

        // Reference Inputs
        final double XRef = 100;
        final double YRef = -65;
        final double ZRef = 35;
        final double XRateRef = -23;
        final double YRateRef = -86;
        final double ZRateRef = 12;

        // Reference Outputs
        final double XangleRef = 1.076854957875316;
        final double YangleRef = 0.9348634533745954;
        final double rangeRef = 124.29802894656054;
        final double XangleRateRef = 0.4091743119266047;
        final double YangleRateRef = -0.6368236827766715;
        final double rangeRateRef = 29.84761730690871;

        // Non regression
        final double XangleNReg = 1.0768549578753155;
        final double YangleNReg = 0.9348634533745958;
        final double rangeNReg = 124.29802894656054;
        final double XangleRateNReg = 0.4091743119266055;
        final double YangleRateNReg = -0.6368236827766712;
        final double rangeRateNReg = 29.84761730690871;

        final double XangleNReg1 = 1.0768549578753155;
        final double YangleNReg1 = 0.9348634533745958;
        final double XangleRateNReg1 = 0.4091743119266055;
        final double YangleRateNReg1 = -0.6368236827766712;

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final Vector3D position = new Vector3D(XRef, YRef, ZRef);
        final Vector3D velocity = new Vector3D(XRateRef, YRateRef, ZRateRef);
        final PVCoordinates pv = new PVCoordinates(position, velocity);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        final CardanMountPV cardanCoordPV = topoNorth.transformFromPVToCardan(pv, topoNorth, this.date);

        // Computed results
        double Xangle = cardanCoordPV.getXangle();
        double Yangle = cardanCoordPV.getYangle();
        final double range = cardanCoordPV.getRange();
        double XangleRate = cardanCoordPV.getXangleRate();
        double YangleRate = cardanCoordPV.getYangleRate();
        final double rangeRate = cardanCoordPV.getRangeRate();

        // Values
        this.validate.assertEqualsWithRelativeTolerance(Xangle, XangleNReg, this.epsilonNonReg,
            XangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(Yangle, YangleNReg, this.epsilonNonReg,
            YangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(range, rangeNReg, this.epsilonNonReg,
            rangeRef, this.epsilonDistance, "elevation deviation");

        // Rates
        this.validate.assertEqualsWithRelativeTolerance(XangleRate, XangleRateNReg, this.epsilonNonReg,
            XangleRateRef, this.epsilonAngVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(YangleRate, YangleRateNReg, this.epsilonNonReg,
            YangleRateRef, this.epsilonAngVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(rangeRate, rangeRateNReg, this.epsilonNonReg,
            rangeRateRef, this.epsilonVelocity, "elevation deviation");

        Xangle = topoNorth.getXangleCardan(position, topoNorth, this.date);
        Yangle = topoNorth.getYangleCardan(position, topoNorth, this.date);
        XangleRate = topoNorth.getXangleCardanRate(pv, topoNorth, this.date);
        YangleRate = topoNorth.getYangleCardanRate(pv, topoNorth, this.date);

        // Rates
        this.validate.assertEqualsWithRelativeTolerance(Xangle, XangleNReg1, this.epsilonNonReg,
            XangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(Yangle, YangleNReg1, this.epsilonNonReg,
            YangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(XangleRate, XangleRateNReg1, this.epsilonNonReg,
            XangleRateRef, this.epsilonAngVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(YangleRate, YangleRateNReg1, this.epsilonNonReg,
            YangleRateRef, this.epsilonAngVelocity, "elevation deviation");

    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromCardanToPV(CardanMountPV)}
     * 
     * @description we test the transformation from Cardan to PVCoordinates
     * 
     * @input the input values are the following :
     *        <p>
     *        Xcar = 1.076854957875316 rad Ycar = 0.9348634533745954 rad d = 124.29802894656054 m VitX =
     *        0.4091743119266047 rad/s VitY = -0.6368236827766715 rad/s Vitd = 29.84761730690871 m/s
     *        </p>
     * 
     *        Note the actual used X angle value is the opposite of the X (cardan) one because
     *        the software used to generate the reference results uses a different convention.
     * 
     * @output cardan coordinates
     * 
     * @testPassCriteria outputs from computation should be equal to expected values :
     *                   <p>
     *                   X = 99.99999999999996 m Y = 65.00000000000007 m Z = 35.0 m Vx = -23.00000000000006 m/s Vy =
     *                   85.99999999999999 m/s Vz = 12.0 m/s
     *                   </p>
     * 
     *                   The epsilon used for distances is 1e-12 on the distances (100 times the
     *                   doubles comparison epsilon).
     *                   The epsilon used for velocities is the same as the epsilon used for
     *                   distances, 1e-12
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationCardantoPV() {

        /*
         * REFERENCE DATA
         */

        // Reference Inputs
        final double Xangle = -1.076854957875316;
        final double Yangle = 0.9348634533745954;
        final double range = 124.29802894656054;
        final double XangleRate = -0.4091743119266047;
        final double YangleRate = -0.6368236827766715;
        final double rangeRate = 29.84761730690871;

        // Reference outputs
        final double XRef = 99.99999999999996;
        final double YRef = 65.00000000000007;
        final double ZRef = 35.0;
        final double XRateRef = -23.00000000000006;
        final double YRateRef = 85.99999999999999;
        final double ZRateRef = 12.0;

        // Non regression
        final double XNReg = 99.99999999999996;
        final double YNReg = 65.00000000000007;
        final double ZNReg = 34.999999999999986;
        final double XRateNReg = -23.000000000000068;
        final double YRateNReg = 86.00000000000001;
        final double ZRateNReg = 11.999999999999995;

        /*
         * TEST
         */

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final CardanMountPV cPV = new CardanMountPV(Xangle, Yangle, range, XangleRate, YangleRate, rangeRate);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        final PVCoordinates CoordPV = topoNorth.transformFromCardanToPV(cPV);

        // Computed results
        final Vector3D myPosition = CoordPV.getPosition();
        final Vector3D myVelocity = CoordPV.getVelocity();
        final double x = myPosition.getX();
        final double y = myPosition.getY();
        final double z = myPosition.getZ();
        final double xd = myVelocity.getX();
        final double yd = myVelocity.getY();
        final double zd = myVelocity.getZ();

        // Values
        this.validate.assertEqualsWithRelativeTolerance(x, XNReg, this.epsilonNonReg,
            XRef, this.epsilonDistance, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(y, YNReg, this.epsilonNonReg,
            YRef, this.epsilonDistance, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(z, ZNReg, this.epsilonNonReg,
            ZRef, this.epsilonDistance, "elevation deviation");

        // Rates
        this.validate.assertEqualsWithRelativeTolerance(xd, XRateNReg, this.epsilonNonReg,
            XRateRef, this.epsilonVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(yd, YRateNReg, this.epsilonNonReg,
            YRateRef, this.epsilonVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(zd, ZRateNReg, this.epsilonNonReg,
            ZRateRef, this.epsilonVelocity, "elevation deviation");

    }

    /**
     * @testType DTC
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToCardan(PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @description we test the transformation from Cardan to PVCoordinates
     * 
     * @input the input values are the following :
     *        <p>
     *        pos_car(1) = 100. m pos_car(2) = 0. m pos_car(3) = 0. m vit_car(1) = -23. m/s vit_car(2) = -86. m/s
     *        vit_car(3) = 12. m/s
     *        </p>
     * 
     * @output an exception should be raised
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     */
    @Test
    public void testTransformationPVToCardanDegrade1() {

        final String errorMessage = "the topocentric coordinates are not defined because the azimuth is undefined.";

        /*
         * REFERENCE DATA
         */

        // Reference Inputs
        final double XRef = 100;
        final double YRef = 0;
        final double ZRef = 0;
        final double XRateRef = -23;
        final double YRateRef = -86;
        final double ZRateRef = 12;

        /*
         * TEST
         */

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final Vector3D position = new Vector3D(XRef, YRef, ZRef);
        final Vector3D velocity = new Vector3D(XRateRef, YRateRef, ZRateRef);
        final PVCoordinates cPV = new PVCoordinates(position, velocity);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        try {
            topoNorth.transformFromPVToCardan(cPV, topoNorth, this.date);
            Assert.fail(errorMessage);
        } catch (final PatriusException ex) {
            // expected
        }

    }

    /**
     * @testType DTC
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToCardan(PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if conversion fails
     * 
     * @description we test the transformation from Cardan to PVCoordinates
     * 
     * @input the input values are the following :
     *        <p>
     *        pos_car(1) = 100. m pos_car(2) = 50. m pos_car(3) = 0. m vit_car(1) = -23. m/s vit_car(2) = -86. m/s
     *        vit_car(3) = 12. m/s
     *        </p>
     * 
     * @output cardan coordinates
     * 
     * @testPassCriteria outputs from computation should be equal to expected values
     *                   <p>
     *                   X (cardan) = -pi/2 | OK Y (cardan) = 1.1071487177940904 rad | OK distance = 111.80339887498948m
     *                   | OK Vit_X = 0.24 rad/s | OK Vit_Y = 0.596 rad/s | OK Vit_distance = -59.03219460599445 m/s |
     *                   OK
     *                   </p>
     * 
     *                   The epsilon used for angles is 1e-7 (1e7 times the epsilon for double
     *                   comparison for the angle epsilon due to the fact that we compare
     *                   physical measures and that the reference comes from another software).
     *                   The epsilon used for distances is 1e-12 on the distances (100 times the
     *                   doubles comparison epsilon).
     *                   The epsilon used for velocities is the same as the epsilon used for
     *                   distances, 1e-12
     *                   The epsilon used for angular velocities is the same as the epsilon used for
     *                   angles, 1e-7
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationPVToCardanDegrade2() throws PatriusException {

        /*
         * REFERENCE DATA
         */

        // Reference Inputs
        final double XRef = 100;
        final double YRef = 50;
        final double ZRef = 0;
        final double XRateRef = -23;
        final double YRateRef = -86;
        final double ZRateRef = 12;

        // Reference Outputs
        final double XangleRef = -FastMath.PI / 2;
        final double YangleRef = 1.1071487177940904;
        final double rangeRef = 111.80339887498948;
        final double XangleRateRef = 0.24;
        final double YangleRateRef = 0.596;
        final double rangeRateRef = -59.03219460599445;

        // Non regression
        final double XangleNReg = -1.5707963267948966;
        final double YangleNReg = 1.1071487177940906;
        final double rangeNReg = 111.80339887498948;
        final double XangleRateNReg = 0.24;
        final double YangleRateNReg = 0.596;
        final double rangeRateNReg = -59.03219460599445;

        /*
         * TEST
         */

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNor = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final Vector3D position = new Vector3D(XRef, YRef, ZRef);
        final Vector3D velocity = new Vector3D(XRateRef, YRateRef, ZRateRef);
        final PVCoordinates cPV = new PVCoordinates(position, velocity);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        final CardanMountPV cardanCoordPV = topoNor.transformFromPVToCardan(cPV, topoNor, this.date);

        // Computed results
        final double Xangle = cardanCoordPV.getXangle();
        final double Yangle = cardanCoordPV.getYangle();
        final double range = cardanCoordPV.getRange();
        final double XangleRate = cardanCoordPV.getXangleRate();
        final double YangleRate = cardanCoordPV.getYangleRate();
        final double rangeRate = cardanCoordPV.getRangeRate();

        // Values
        this.validate.assertEqualsWithRelativeTolerance(Xangle, XangleNReg, this.epsilonNonReg,
            XangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(Yangle, YangleNReg, this.epsilonNonReg,
            YangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(range, rangeNReg, this.epsilonNonReg,
            rangeRef, this.epsilonDistance, "elevation deviation");

        // Rates
        this.validate.assertEqualsWithRelativeTolerance(XangleRate, XangleRateNReg, this.epsilonNonReg,
            XangleRateRef, this.epsilonAngVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(YangleRate, YangleRateNReg, this.epsilonNonReg,
            YangleRateRef, this.epsilonAngVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(rangeRate, rangeRateNReg, this.epsilonNonReg,
            rangeRateRef, this.epsilonVelocity, "elevation deviation");
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToCardan(PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if conversion fails
     * 
     * @description we test the transformation from PVCoordinates to Cardan coordinates
     * 
     * @input the input values are the following :
     *        <p>
     *        pos_car(1) = 100. m pos_car(2) = -65. m pos_car(3) = 35. m vit_car(1) = -23. m/s vit_car(2) = -86. m/s
     *        vit_car(3) = 12. m/s
     *        </p>
     * 
     * @output cardan coordinates
     * 
     * @testPassCriteria outputs from computation should be equal to expected values
     *                   <p>
     *                   X (cardan) = 1.076854957875316 rad Y (cardan) = 0.9348634533745954 rad distance =
     *                   124.29802894656054 m Vit_X = 0.4091743119266047 rad/s Vit_Y = -0.6368236827766715 rad/s
     *                   Vit_distance = 29.84761730690871 m/s
     *                   </p>
     * 
     *                   The epsilon used for angles is 1e-7 (1e7 times the epsilon for double
     *                   comparison for the angle epsilon due to the fact that we compare
     *                   physical measures and that the reference comes from another software).
     *                   The epsilon used for distances is 1e-12 on the distances (100 times the
     *                   doubles comparison epsilon).
     *                   The epsilon used for velocities is the same as the epsilon used for
     *                   distances, 1e-12
     *                   The epsilon used for angular velocities is the same as the epsilon used for
     *                   angles, 1e-7
     * 
     *                   Note the actual used X angle value is the opposite of the X (cardan) one because
     *                   the software used to generate the reference results uses a different convention.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationPositionToCardan() throws PatriusException {

        // Reference Inputs
        final double XRef = 100;
        final double YRef = -65;
        final double ZRef = 35;
        final double XRateRef = -23;
        final double YRateRef = -86;
        final double ZRateRef = 12;

        // Reference Outputs
        final double XangleRef = 1.076854957875316;
        final double YangleRef = 0.9348634533745954;
        final double rangeRef = 124.29802894656054;
        final double XangleRateRef = 0.4091743119266047;
        final double YangleRateRef = -0.6368236827766715;
        final double rangeRateRef = 29.84761730690871;

        // Non regression
        final double XangleNReg = 1.0768549578753155;
        final double YangleNReg = 0.9348634533745958;
        final double rangeNReg = 124.29802894656054;
        final double XangleRateNReg = 0.4091743119266055;
        final double YangleRateNReg = -0.6368236827766712;
        final double rangeRateNReg = 29.84761730690871;

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final Vector3D pos = new Vector3D(XRef, YRef, ZRef);
        final Vector3D vel = new Vector3D(XRateRef, YRateRef, ZRateRef);
        final PVCoordinates pv = new PVCoordinates(pos, vel);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        final CardanMountPV cardanCoordPV = topoNorth.transformFromPVToCardan(pv, topoNorth, this.date);

        // Computed results
        final double Xangle = cardanCoordPV.getXangle();
        final double Yangle = cardanCoordPV.getYangle();
        final double range = cardanCoordPV.getRange();
        final double XangleRate = cardanCoordPV.getXangleRate();
        final double YangleRate = cardanCoordPV.getYangleRate();
        final double rangeRate = cardanCoordPV.getRangeRate();

        // Values
        this.validate.assertEqualsWithRelativeTolerance(Xangle, XangleNReg, this.epsilonNonReg,
            XangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(Yangle, YangleNReg, this.epsilonNonReg,
            YangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(range, rangeNReg, this.epsilonNonReg,
            rangeRef, this.epsilonDistance, "elevation deviation");

        // Rates
        this.validate.assertEqualsWithRelativeTolerance(XangleRate, XangleRateNReg, this.epsilonNonReg,
            XangleRateRef, this.epsilonAngVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(YangleRate, YangleRateNReg, this.epsilonNonReg,
            YangleRateRef, this.epsilonAngVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(rangeRate, rangeRateNReg, this.epsilonNonReg,
            rangeRateRef, this.epsilonVelocity, "elevation deviation");

    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromCardanToPV(CardanMountPV)}
     * 
     * @description we test the transformation from Cardan to PVCoordinates
     * 
     * @input the input values are the following :
     *        <p>
     *        Xcar = 1.076854957875316 rad Ycar = 0.9348634533745954 rad d = 124.29802894656054 m VitX =
     *        0.4091743119266047 rad/s VitY = -0.6368236827766715 rad/s Vitd = 29.84761730690871 m/s
     *        </p>
     * 
     * @output cardan coordinates
     * 
     * @testPassCriteria outputs from computation should be equal to expected values
     *                   <p>
     *                   X = 99.99999999999996 m Y = 65.00000000000007 m Z = 35.0 m Vx = -23.00000000000006 m/s Vy =
     *                   85.99999999999999 m/s Vz = 12.0 m/s
     *                   </p>
     * 
     *                   The epsilon used for distances is 1e-12 on the distances (100 times the
     *                   doubles comparison epsilon).
     *                   The epsilon used for velocities is the same as the epsilon used for
     *                   distances, 1e-12
     * 
     *                   Note the actual used X angle value is the opposite of the X (cardan) one because
     *                   the software used to generate the reference results uses a different convention.
     * 
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationCardanToPosition() {

        /*
         * REFERENCE DATA
         */

        // Reference Inputs
        final double Xangle = -1.076854957875316;
        final double Yangle = 0.9348634533745954;
        final double range = 124.29802894656054;
        final double XangleRate = -0.4091743119266047;
        final double YangleRate = -0.6368236827766715;
        final double rangeRate = 29.84761730690871;

        // Reference outputs
        final double XRef = 99.99999999999996;
        final double YRef = 65.00000000000007;
        final double ZRef = 35.0;
        final double XRateRef = -23.00000000000006;
        final double YRateRef = 85.99999999999999;
        final double ZRateRef = 12.0;

        /*
         * TEST
         */

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final CardanMountPV cPV = new CardanMountPV(Xangle, Yangle, range, XangleRate, YangleRate, rangeRate);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        final PVCoordinates CoordPV = topoNorth.transformFromCardanToPV(cPV);

        // Computed results
        final Vector3D myPosition = CoordPV.getPosition();
        final Vector3D myVelocity = CoordPV.getVelocity();
        final double x = myPosition.getX();
        final double y = myPosition.getY();
        final double z = myPosition.getZ();
        final double xd = myVelocity.getX();
        final double yd = myVelocity.getY();
        final double zd = myVelocity.getZ();

        // Values
        this.validate.assertEqualsWithRelativeTolerance(x, XRef, this.epsilonNonReg,
            XRef, this.epsilonDistance, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(y, YRef, this.epsilonNonReg,
            YRef, this.epsilonDistance, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(z, ZRef, this.epsilonNonReg,
            ZRef, this.epsilonDistance, "elevation deviation");

        // Rates
        this.validate.assertEqualsWithRelativeTolerance(xd, XRateRef, this.epsilonNonReg,
            XRateRef, this.epsilonVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(yd, YRateRef, this.epsilonNonReg,
            YRateRef, this.epsilonVelocity, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(zd, ZRateRef, this.epsilonNonReg,
            ZRateRef, this.epsilonVelocity, "elevation deviation");
    }

    /**
     * @testType DTC
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPVToCardan(PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @description we test the transformation from Cardan to PVCoordinates
     * 
     * @input the input values are the following :
     *        <p>
     *        pos_car(1) = 100. m pos_car(2) = 0. m pos_car(3) = 0. m vit_car(1) = -23. m/s vit_car(2) = -86. m/s
     *        vit_car(3) = 12. m/s
     *        </p>
     * 
     * @output an exception should be raised
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationPositionToCardanDegrade1() {

        final String errorMessage = "the topocentric coordinates are not defined because the azimuth is undefined.";

        /*
         * REFERENCE DATA
         */

        // Reference Inputs - Position only because exception expected
        final double XRef = 100;
        final double YRef = 0;
        final double ZRef = 0;

        /*
         * TEST
         */

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final Vector3D position = new Vector3D(XRef, YRef, ZRef);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        try {
            topoNorth.transformFromPositionToCardan(position, topoNorth, this.date);
            Assert.fail(errorMessage);
        } catch (final PatriusException ex) {
            // expected
        }

    }

    /**
     * @testType DTC
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPositionToCardan(Vector3D, Frame, AbsoluteDate)}
     * 
     * @throws PatriusException
     *         if conversion fails
     * 
     * @description we test the transformation from Position to Cardan
     * 
     * @input the input values are the following :
     *        <p>
     *        pos_car(1) = 100. m pos_car(2) = 50. m pos_car(3) = 0. m
     *        </p>
     * 
     * @output cardan coordinates
     * 
     * @testPassCriteria outputs from computation should be equal to expected values
     *                   <p>
     *                   X (cardan) = -pi/2 | OK Y (cardan) = 1.1071487177940904 rad | OK distance = 111.80339887498948m
     *                   | OK
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testTransformationPositionToCardanDegrade2() throws PatriusException {

        /*
         * REFERENCE DATA
         */

        // Reference Inputs
        final double XRef = 100;
        final double YRef = 50;
        final double ZRef = 0;

        // Reference Outputs
        final double XangleRef = -FastMath.PI / 2;
        final double YangleRef = 1.1071487177940904;
        final double rangeRef = 111.80339887498948;

        // Non regression
        final double XangleNReg = -1.5707963267948966;
        final double YangleNReg = 1.1071487177940906;
        final double rangeNReg = 111.80339887498948;

        /*
         * TEST
         */

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., this.northFrame);

        // Cartesian coordinates expressed in the North topocentric frame
        final Vector3D position = new Vector3D(XRef, YRef, ZRef);

        // Conversion from Cartesian coordinates (position and velocity) to topocentric coordinates
        final CardanMountPosition cardanCoordPV =
            topoNorth.transformFromPositionToCardan(position, topoNorth, this.date);

        // Computed results
        final double Xangle = cardanCoordPV.getXangle();
        final double Yangle = cardanCoordPV.getYangle();
        final double range = cardanCoordPV.getRange();

        // Values
        this.validate.assertEqualsWithRelativeTolerance(Xangle, XangleNReg, this.epsilonNonReg,
            XangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(Yangle, YangleNReg, this.epsilonNonReg,
            YangleRef, this.epsilonAngle, "elevation deviation");
        this.validate.assertEqualsWithRelativeTolerance(range, rangeNReg, this.epsilonNonReg,
            rangeRef, this.epsilonDistance, "elevation deviation");
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link TopocentricFrame#getDElevation(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description unit test for the elevation partial derivatives computation by finite difference.
     *              <p>
     *              For each partial derivatives:
     *              <ul>
     *              <li>Display the results: absolute and relative differences</li>
     *              <li>Non regression validation test wrt reference values</li>
     *              </ul>
     *              </p>
     * 
     * @testPassCriteria the absolute and relative differences are displayed to be evaluated by the user.
     *                   When they are OK (close to 0), the values are saved to be evaluated with a non-regression test
     *                   (1e-16 threshold).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * @throws PatriusException if frames transformations cannot be computed
     */
    @Test
    public void testDelevation() throws PatriusException {

        // Surface point at latitude 0°, longitude 0°
        final EllipsoidPoint groundPoint = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(0.), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(groundPoint,
            "lon 0 lat 0");
        final Frame itrf = this.earthSpheric.getBodyFrame();

        // Determine the offset in [m]
        final double hElev = 1;

        // Case #1: Target point express in the earthSpheric's frame (itrf)
        final EllipsoidPoint targetPoint = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(5.), MathLib.toRadians(5.), 800_000., "");
        final Vector3D extPoint = targetPoint.getPosition();

        // Build the offset target points
        final Vector3D extPointPlusHX = extPoint.add(new Vector3D(+hElev, 0., 0.));
        final Vector3D extPointMinusHX = extPoint.add(new Vector3D(-hElev, 0., 0.));

        final Vector3D extPointPlusHY = extPoint.add(new Vector3D(0., +hElev, 0.));
        final Vector3D extPointMinusHY = extPoint.add(new Vector3D(0., -hElev, 0.));

        final Vector3D extPointPlusHZ = extPoint.add(new Vector3D(0., 0., +hElev));
        final Vector3D extPointMinusHZ = extPoint.add(new Vector3D(0., 0., -hElev));

        // Compute the offset target points elevations
        double elevPlusHX = topoFrame.getElevation(extPointPlusHX, itrf, this.date);
        double elevMinusHX = topoFrame.getElevation(extPointMinusHX, itrf, this.date);

        double elevPlusHY = topoFrame.getElevation(extPointPlusHY, itrf, this.date);
        double elevMinusHY = topoFrame.getElevation(extPointMinusHY, itrf, this.date);

        double elevPlusHZ = topoFrame.getElevation(extPointPlusHZ, itrf, this.date);
        double elevMinusHZ = topoFrame.getElevation(extPointMinusHZ, itrf, this.date);

        // Compute the numerical values
        double numValX = (elevPlusHX - elevMinusHX) / (2. * hElev);
        double numValY = (elevPlusHY - elevMinusHY) / (2. * hElev);
        double numValZ = (elevPlusHZ - elevMinusHZ) / (2. * hElev);

        // Compute the derivatives elevation vector
        Vector3D dElev = topoFrame.getDElevation(extPoint, itrf, this.date);

        // Non regression evaluation
        final double validityThreshold = 1e-16;

        Assert.assertEquals(6.61205847907187E-7, numValX, validityThreshold);
        Assert.assertEquals(-3.9393839385004625E-7, numValY, validityThreshold);
        Assert.assertEquals(-3.95443174650012E-7, numValZ, validityThreshold);

        Assert.assertEquals(6.6120584785882E-7, dElev.getX(), validityThreshold);
        Assert.assertEquals(-3.939383939701935E-7, dElev.getY(), validityThreshold);
        Assert.assertEquals(-3.9544317463724705E-7, dElev.getZ(), validityThreshold);

        // ---------------------------------------------------------------

        // Case #2: Target point express in the topocentric frame

        // Compute the offset target points elevations
        elevPlusHX = topoFrame.getElevation(extPointPlusHX, topoFrame, this.date);
        elevMinusHX = topoFrame.getElevation(extPointMinusHX, topoFrame, this.date);

        elevPlusHY = topoFrame.getElevation(extPointPlusHY, topoFrame, this.date);
        elevMinusHY = topoFrame.getElevation(extPointMinusHY, topoFrame, this.date);

        elevPlusHZ = topoFrame.getElevation(extPointPlusHZ, topoFrame, this.date);
        elevMinusHZ = topoFrame.getElevation(extPointMinusHZ, topoFrame, this.date);

        // Compute the numerical values
        numValX = (elevPlusHX - elevMinusHX) / (2. * hElev);
        numValY = (elevPlusHY - elevMinusHY) / (2. * hElev);
        numValZ = (elevPlusHZ - elevMinusHZ) / (2. * hElev);

        // Compute the derivatives elevation vector
        dElev = topoFrame.getDElevation(extPoint, topoFrame, this.date);

        // Non regression evaluation
        Assert.assertEquals(-1.209563085311771E-8, numValX, validityThreshold);
        Assert.assertEquals(-1.0582305778422006E-9, numValY, validityThreshold);
        Assert.assertEquals(1.387817999341497E-7, numValZ, validityThreshold);

        Assert.assertEquals(-1.2095630851083339E-8, dElev.getX(), validityThreshold);
        Assert.assertEquals(-1.0582305726147823E-9, dElev.getY(), validityThreshold);
        Assert.assertEquals(1.3878179993404016E-7, dElev.getZ(), validityThreshold);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link TopocentricFrame#getDAzimuth(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description unit test for the elevation partial derivatives computation by finite difference.
     *              <p>
     *              For each partial derivatives:
     *              <ul>
     *              <li>Display the results: absolute and relative differences</li>
     *              <li>Non regression validation test wrt reference values</li>
     *              </ul>
     *              </p>
     * 
     * @testPassCriteria the absolute and relative differences are displayed to be evaluated by the user.
     *                   When they are OK (close to 0), the values are saved to be evaluated with a non-regression test
     *                   (1e-16 threshold).
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * @throws PatriusException if frames transformations cannot be computed
     */
    @Test
    public void testDazimuth() throws PatriusException {

        // Surface point at latitude 0°, longitude 0°
        final EllipsoidPoint groundPoint = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(0.), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(groundPoint,
            "lon 0 lat 0");
        final Frame itrf = this.earthSpheric.getBodyFrame();

        // Determine the offset in [m]
        final double hAzim = 1;

        // Case #1: Target point express in the earthSpheric's frame (itrf)
        final EllipsoidPoint targetPoint = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(5.), MathLib.toRadians(5.), 800_000., "");
        final Vector3D extPoint = targetPoint.getPosition();

        // Build the offset target points
        Vector3D extPointPlusHX = extPoint.add(new Vector3D(+hAzim, 0., 0.));
        Vector3D extPointMinusHX = extPoint.add(new Vector3D(-hAzim, 0., 0.));

        Vector3D extPointPlusHY = extPoint.add(new Vector3D(0., +hAzim, 0.));
        Vector3D extPointMinusHY = extPoint.add(new Vector3D(0., -hAzim, 0.));

        Vector3D extPointPlusHZ = extPoint.add(new Vector3D(0., 0., +hAzim));
        Vector3D extPointMinusHZ = extPoint.add(new Vector3D(0., 0., -hAzim));

        // Compute the offset target points azimuths
        double azimPlusHX = topoFrame.getAzimuth(extPointPlusHX, itrf, this.date);
        double azimMinusHX = topoFrame.getAzimuth(extPointMinusHX, itrf, this.date);

        double azimPlusHY = topoFrame.getAzimuth(extPointPlusHY, itrf, this.date);
        double azimMinusHY = topoFrame.getAzimuth(extPointMinusHY, itrf, this.date);

        double azimPlusHZ = topoFrame.getAzimuth(extPointPlusHZ, itrf, this.date);
        double azimMinusHZ = topoFrame.getAzimuth(extPointMinusHZ, itrf, this.date);

        // Compute the numerical values
        double numValX = (azimPlusHX - azimMinusHX) / (2. * hAzim);
        double numValY = (azimPlusHY - azimMinusHY) / (2. * hAzim);
        double numValZ = (azimPlusHZ - azimMinusHZ) / (2. * hAzim);

        // Compute the derivatives azimuth vector
        Vector3D dAzim = topoFrame.getDAzimuth(extPoint, itrf, this.date);

        // Non regression evaluation
        final double validityThreshold = 1e-16;

        Assert.assertEquals(0.0, numValX, validityThreshold);
        Assert.assertEquals(8.022595644474606E-7, numValY, validityThreshold);
        Assert.assertEquals(-7.992067245776724E-7, numValZ, validityThreshold);

        Assert.assertEquals(0.0, dAzim.getX(), validityThreshold);
        Assert.assertEquals(8.022595644210772E-7, dAzim.getY(), validityThreshold);
        Assert.assertEquals(-7.9920672456967E-7, dAzim.getZ(), validityThreshold);

        // ---------------------------------------------------------------

        // Case #2: Target point express in the topocentric frame

        // Build the offset target points
        extPointPlusHX = extPoint.add(new Vector3D(+hAzim, 0., 0.));
        extPointMinusHX = extPoint.add(new Vector3D(-hAzim, 0., 0.));

        extPointPlusHY = extPoint.add(new Vector3D(0., +hAzim, 0.));
        extPointMinusHY = extPoint.add(new Vector3D(0., -hAzim, 0.));

        extPointPlusHZ = extPoint.add(new Vector3D(0., 0., +hAzim));
        extPointMinusHZ = extPoint.add(new Vector3D(0., 0., -hAzim));

        // Compute the offset target points azimuths
        azimPlusHX = topoFrame.getAzimuth(extPointPlusHX, topoFrame, this.date);
        azimMinusHX = topoFrame.getAzimuth(extPointMinusHX, topoFrame, this.date);

        azimPlusHY = topoFrame.getAzimuth(extPointPlusHY, topoFrame, this.date);
        azimMinusHY = topoFrame.getAzimuth(extPointMinusHY, topoFrame, this.date);

        azimPlusHZ = topoFrame.getAzimuth(extPointPlusHZ, topoFrame, this.date);
        azimMinusHZ = topoFrame.getAzimuth(extPointMinusHZ, topoFrame, this.date);

        // Compute the numerical values
        numValX = (azimPlusHX - azimMinusHX) / (2. * hAzim);
        numValY = (azimPlusHY - azimMinusHY) / (2. * hAzim);
        numValZ = (azimPlusHZ - azimMinusHZ) / (2. * hAzim);

        // Compute the derivatives azimuth vector
        dAzim = topoFrame.getDAzimuth(extPoint, topoFrame, this.date);

        // Non regression evaluation
        Assert.assertEquals(1.2188214032075394E-8, numValX, validityThreshold);
        Assert.assertEquals(-1.3931192386387147E-7, numValY, validityThreshold);
        Assert.assertEquals(0., numValZ, validityThreshold);

        Assert.assertEquals(1.2188213993624686E-8, dAzim.getX(), validityThreshold);
        Assert.assertEquals(-1.39311923885721E-7, dAzim.getY(), validityThreshold);
        Assert.assertEquals(0., dAzim.getZ(), validityThreshold);
    }

    /**
     * before the tests
     */
    @Before
    public void setUp() {
        try {
            this.validate = new Validate(TopocentricFrame.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        try {

            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

            // Elliptic earth shape
            this.earthSpheric = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

            // Reference date
            this.date =
                new AbsoluteDate(new DateComponents(2008, 04, 07), TimeComponents.H00, TimeScalesFactory.getUTC());

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    /**
     * after the tests
     */
    @After
    public void tearDown() {
        this.date = null;
        this.frameITRF2005 = null;
        this.earthSpheric = null;
    }
}
