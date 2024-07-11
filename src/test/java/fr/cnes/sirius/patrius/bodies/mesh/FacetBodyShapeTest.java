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
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.8:FA:FA-3091:15/11/2021:[PATRIUS] Corriger la methode bodyShapeMaskingDistance de SensorModel
 * VERSION:4.7:DM:DM-2870:18/05/2021:Complements pour la manipulation des coordonnees cart.
 * VERSION:4.7:FA:FA-2821:18/05/2021:Refus injustifié de calcul de l incidence solaire lorsqu elle dépasse 90 degres 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape.MarginType;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape.EllipsoidType;
import fr.cnes.sirius.patrius.events.sensor.SensorVisibilityDetector;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Unit tests for {@link FacetCelestialBody} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class FacetBodyShapeTest
 {
    /** Epsilon for double comparison. */
    private static final double EPS = 1E-14;

    /** Body radius (m). */
    private double bodyRadius = 10000.;

    /** User celestial body used for tests. */
    private UserCelestialBody celestialBody;
    
    /** Facet celestial body used for tests. */
    private FacetBodyShape body;
    
    /** Mesh loader. */
    private MeshProvider meshProv;

    /**
     * Builds a theoretical spherical celestial whose poles are aligned with GCRF and whose PVCoordinates are (0, 0, 0)
     * in GCRF frame.
     */
    @Before
    public void setUpBeforeClass() throws PatriusException, IOException {

        // Build body file
        final String modelFile = System.getProperty("user.dir") + File.separator + "SphericalBody.obj";
        writeBodyFile(modelFile, 51, 100, bodyRadius / 1E3);

        // Retrieve body
        final PVCoordinatesProvider pvCoordinates = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                return PVCoordinates.ZERO;
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        celestialBody = new UserCelestialBody("My body", pvCoordinates, 0, IAUPoleFactory.getIAUPole(null), FramesFactory.getGCRF(), null);
        meshProv = new ObjMeshLoader(modelFile);
        body = new FacetBodyShape("My body", celestialBody.getTrueRotatingFrame(), pvCoordinates, EllipsoidType.INNER_SPHERE, new ObjMeshLoader(modelFile));
    }

    /**
     * Build spherical model and write it in file
     * @param modelFile output model file name
     * @param latitudeNumber number of latitude points (should be odd)
     * @param longitudeNumber number longitude points
     * @param radius body radius (km)
     */
    private void writeBodyFile(final String modelFile,
            final int latitudeNumber,
            final int longitudeNumber,
            final double radius) throws IOException {
        // Initialization, open resources
        final FileOutputStream fileOutputStream = new FileOutputStream(modelFile);
        final OutputStreamWriter fileWriter = new OutputStreamWriter(fileOutputStream, Charset.forName("UTF-8")
                .newEncoder());
        final PrintWriter printWriter = new PrintWriter(fileWriter);

        // Build body
        final int latitudeNumber2 = (latitudeNumber - 1) / 2 - 1;
        final int numberPoints = (2 * latitudeNumber2 + 1) * longitudeNumber + 2;

        // Points
        
        // South pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., -radius));

        // Regular points excluding poles
        for (int i = -latitudeNumber2; i <= latitudeNumber2; i++) {
            final double latitude = (double) i / (latitudeNumber2 + 1) * MathLib.PI / 2.;
            for (int j = 0; j < longitudeNumber; j++) {
                final double longitude = (double) j / longitudeNumber * 2. * MathLib.PI;
                final double coslat = MathLib.cos(latitude);
                final double sinlat = MathLib.sin(latitude);
                final double coslon = MathLib.cos(longitude);
                final double sinlon = MathLib.sin(longitude);
                final Vector3D pv = new Vector3D(coslat * coslon, coslat * sinlon, sinlat).scalarMultiply(radius);
                printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", pv.getX(), pv.getY(),
                        pv.getZ()));
            }
        }

        // North pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., radius));

        // Triangles

        // South pole
        for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, j + 2, j + 3));
        }
        printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, longitudeNumber + 1, 2));

        // Regular points excluding poles
        for (int i = 0; i < latitudeNumber - 3; i++) {
            for (int j = 0; j < longitudeNumber - 1; j++) {
                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 
                        i * longitudeNumber + 2 + j, i * longitudeNumber + 2 + j + 1, (i + 1) * longitudeNumber + 2 + j));
                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 
                        i * longitudeNumber + 2 + j + 1, (i + 1) * longitudeNumber + 2 + j, (i + 1) * longitudeNumber + 2 + j + 1));
            }
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 
                    i * longitudeNumber + 2 + longitudeNumber - 1, i * longitudeNumber + 2, (i + 1) * longitudeNumber + 2 + longitudeNumber - 1));
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 
                    i * longitudeNumber + 2, (i + 1) * longitudeNumber + 2 + longitudeNumber - 1, (i + 1) * longitudeNumber + 2));
        }

        // North pole
        for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints, numberPoints - j - 1, numberPoints - j - 2));
        }
        printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints, numberPoints - longitudeNumber, numberPoints - 1));

        // Close resources
        printWriter.close();
        fileWriter.close();
        fileOutputStream.close();
    }

    /**
     * @testType UT
     * 
     * @description check that body surface is as expected.
     * 
     * @testPassCriteria body surface is equal to 4Pi.r.r (reference: math, relative threshold: 1E-3, limited due to
     *                   limited number of vertices in the model)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void surfaceTest() {
        double actual = 0;
        for (final Triangle triangle : body.getTriangles()) {
            actual += triangle.getSurface();
        }
        final double reference = 4. * MathLib.PI * bodyRadius * bodyRadius;
        Assert.assertEquals(0., (reference - actual) / reference, 1E-3);
    }

    /**
     * @testType UT
     * 
     * @description check that intersection between a line of sight and the body is properly computed for various
     *              configurations:
     *              <ul>
     *              <li>Body center pointing case</li>
     *              <li>Anti-body center pointing case</li>
     *              <li>No intersection case</li>
     *              <li>Tangent case</li>
     *              </ul>
     * 
     * @testPassCriteria intersection between a line of sight and the body is properly computed for various
     *                   configurations (reference: math, relative threshold: 1E-3, limited due to number of vertices in the
     *                   model)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void intersectionTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final Intersection actual1 = body.getIntersection(lineOfSight1, position1, frame, date);
        final Vector3D reference1 = position1.normalize().scalarMultiply(bodyRadius);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual1.getPoint()) / reference1.getNorm(), 1E-3);

        // Intersection (basic case: opposite to body center pointing, line is still intersecting)
        final Vector3D position2 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight2 = new Line(position2, position2.scalarMultiply(2.));
        final Intersection actual2 = body.getIntersection(lineOfSight2, position2, frame, date);
        final Vector3D reference2 = position2.normalize().scalarMultiply(bodyRadius);
        Assert.assertEquals(0., Vector3D.distance(reference2, actual2.getPoint()) / reference2.getNorm(), 1E-3);

        // Intersection (no intersection but close)
        final Vector3D position3 = new Vector3D(10001, 0, 0);
        final Line lineOfSight3 = new Line(position3, position3.add(Vector3D.PLUS_J));
        final Intersection actual3 = body.getIntersection(lineOfSight3, position3, frame, date);
        Assert.assertNull(actual3);

        // Intersection (tangent case)
        final Vector3D position4 = new Vector3D(0, 0, 10000);
        final Line lineOfSight4 = new Line(position4, position4.add(Vector3D.PLUS_I));
        final Frame bodyFrame = body.getBodyFrame();
        final Intersection actual4 = body.getIntersection(lineOfSight4, position4, bodyFrame, date);
        final Vector3D reference4 = position4;
        Assert.assertEquals(0., Vector3D.distance(reference4, actual4.getPoint()) / reference4.getNorm(), 0.);
    }

    /**
     * @testType UT
     * 
     * @description check that intersection between a line of sight and the body is properly computed for various
     *              configurations:
     *              <ul>
     *              <li>Body center pointing case</li>
     *              <li>Anti-body center pointing case</li>
     *              <li>No intersection case</li>
     *              <li>Tangent case</li>
     *              </ul>
     * 
     * @testPassCriteria intersection between a line of sight and the body is properly computed for various
     *                   configurations (reference: math, relative threshold: 1E-3, limited due to number of vertices in the
     *                   model)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void intersectionGeodeticTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final GeodeticPoint actual1 = body.getIntersectionPoint(lineOfSight1, position1, frame, date);
        final GeodeticPoint reference1 = body.transform(position1, frame, date);
        Assert.assertEquals(reference1.getLatitude(), actual1.getLatitude(), 1E-12);
        Assert.assertEquals(reference1.getLongitude(), actual1.getLongitude(), EPS);
        Assert.assertEquals(0., actual1.getAltitude() / position1.getNorm(), 1E-3);

        // Intersection (basic case: opposite to body center pointing, line is still intersecting)
        final Vector3D position2 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight2 = new Line(position2, position2.scalarMultiply(2.));
        final GeodeticPoint actual2 = body.getIntersectionPoint(lineOfSight2, position2, frame, date);
        final GeodeticPoint reference2 = body.transform(position2, frame, date);
        Assert.assertEquals(reference2.getLatitude(), actual2.getLatitude(), 1E-12);
        Assert.assertEquals(reference2.getLongitude(), actual2.getLongitude(), EPS);
        Assert.assertEquals(0., actual2.getAltitude() / position2.getNorm(), 1E-3);

        // Intersection (no intersection but close)
        final Vector3D position3 = new Vector3D(10001, 0, 0);
        final Line lineOfSight3 = new Line(position3, position3.add(Vector3D.PLUS_J));
        final GeodeticPoint actual3 = body.getIntersectionPoint(lineOfSight3, position3, frame, date);
        Assert.assertNull(actual3);

        // Intersection (tangent case)
        final Vector3D position4 = new Vector3D(0, 0, 10000);
        final Line lineOfSight4 = new Line(position4, position4.add(Vector3D.PLUS_I));
        final Frame bodyFrame = body.getBodyFrame();
        final GeodeticPoint actual4 = body.getIntersectionPoint(lineOfSight4, position4, bodyFrame, date);
        final GeodeticPoint reference4 = body.transform(position4, body.getBodyFrame(), date);
        Assert.assertEquals(reference4.getLatitude(), actual4.getLatitude(), 0);
        Assert.assertEquals(reference4.getLongitude(), actual4.getLongitude(), 0);
        Assert.assertEquals(0., actual4.getAltitude() / position4.getNorm(), 1E-11);
    }

    /**
     * @testType UT
     * 
     * @description check that intersections between a line of sight and the body is properly computed for various
     *              configurations:
     *              <ul>
     *              <li>Body center pointing case</li>
     *              <li>Anti-body center pointing case</li>
     *              <li>No intersection case</li>
     *              <li>Tangent case</li>
     *              </ul>
     * 
     * @testPassCriteria intersections between a line of sight and the body is properly computed for various
     *                   configurations (reference: math, relative threshold: 1E-3, limited due to number of vertices in the
     *                   model)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void intersectionsTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();

        // Intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final Vector3D[] actual1 = body.getIntersectionPoints(lineOfSight1, frame, date);
        final Vector3D[] reference1 = new Vector3D[] { position1.normalize().scalarMultiply(-bodyRadius),
                position1.normalize().scalarMultiply(bodyRadius) };
        Assert.assertEquals(0., Vector3D.distance(reference1[0], actual1[0]) / reference1[0].getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference1[1], actual1[1]) / reference1[1].getNorm(), 1E-3);

        // Intersection (basic case: opposite to body center pointing, line is still intersecting)
        final Vector3D position2 = new Vector3D(100E3, 200E3, 300E3);
        final Line lineOfSight2 = new Line(position2, position2.scalarMultiply(2.));
        final Vector3D[] actual2 = body.getIntersectionPoints(lineOfSight2, frame, date);
        final Vector3D[] reference2 = new Vector3D[] { position2.normalize().scalarMultiply(-bodyRadius),
                position2.normalize().scalarMultiply(bodyRadius) };
        Assert.assertEquals(0., Vector3D.distance(reference2[0], actual2[0]) / reference2[0].getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference2[1], actual2[1]) / reference2[1].getNorm(), 1E-3);

        // Intersection (no intersection but close)
        final Vector3D position3 = new Vector3D(10001, 0, 0);
        final Line lineOfSight3 = new Line(position3, position3.add(Vector3D.PLUS_J));
        final Vector3D[] actual3 = body.getIntersectionPoints(lineOfSight3, frame, date);
        Assert.assertEquals(0, actual3.length);

        // Intersection (tangent case at north pole, 1 point expected since duplicates are eliminated)
        final Vector3D position4 = new Vector3D(0, 0, 10000);
        final Line lineOfSight4 = new Line(position4, position4.add(Vector3D.PLUS_I));
        final Frame bodyFrame = body.getBodyFrame();
        final Vector3D[] actual4 = body.getIntersectionPoints(lineOfSight4, bodyFrame, date);
        final Vector3D[] reference4 = new Vector3D[] { position4 };
        Assert.assertEquals(1, actual4.length);
        Assert.assertEquals(0., Vector3D.distance(reference4[0], actual4[0]) / reference4[0].getNorm(), 0.);

        // Intersection (no valid intersection because of abscissa too low)
        final Vector3D position5 = new Vector3D(100E3, 200E3, 300E3);
        final Vector3D pointMinAbscissa = new Vector3D(-100E3, -200E3, -300E3);
        final Line lineOfSight5 = new Line(position5, Vector3D.ZERO, pointMinAbscissa);
        final Vector3D[] actual5 = body.getIntersectionPoints(lineOfSight5, frame, date);
        Assert.assertEquals(0, actual5.length);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link FacetCelestialBody#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}
     * 
     * @description Checks intersection between a line and an ellipsoid at a given altitude on two cases:
     *              <ul>
     *              <li>Intersection at altitude = 100m: altitude of computed points should be 100m (accuracy: 1E-3m)</li>
     *              <li>Intersection at altitude = 0m: altitude of computed points should be 0m (accuracy: 0m)</li>
     *              </ul>
     * 
     * @testPassCriteria altitude of computed points are as expected. Points are on the initial line.
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void intersectionPointsAltitudeTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        
        // Test with a random point and altitude = 100m
        final Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction = new Vector3D(0.0, 1.0, 1.0);
        final Line line = new Line(point, point.add(direction));
        try {
            body.getIntersectionPoint(line, point, body.getBodyFrame(), date, 100);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Test with a random point and altitude = 0m. Exact result is expected
        final Vector3D point2 = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction2 = new Vector3D(0.0, 1.0, 1.0);
        final Line line2 = new Line(point2, point2.add(direction2));
        final Frame bodyFrame = body.getBodyFrame();
        final GeodeticPoint gp2 = body.getIntersectionPoint(line2, point2, bodyFrame, date, 0);
        final GeodeticPoint gpRef = body.getIntersectionPoint(line2, point2, bodyFrame, date);
        Assert.assertEquals(gp2.getAltitude(), gpRef.getAltitude(), 0);
        Assert.assertEquals(gp2.getLatitude(), gpRef.getLatitude(), 0);
        Assert.assertEquals(gp2.getLongitude(), gpRef.getLongitude(), 0);
    }

    /**
     * @testType UT
     * 
     * @description check that eclipse is properly detected for various configurations:
     *              <li>In eclipse</li>
     *              <li>Satellite between Sun and Body (no eclipse)</li>
     *              <li>Satellite outside of segment [Sun, Body] (no eclipse)</li>
     *              </ul>
     * 
     * @testPassCriteria eclipse is properly detected for various configurations (reference: math)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void eclipseTest() throws PatriusException {
        // Initialization
        final PVCoordinatesProvider sun = new MeeusSun();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getGCRF();

        // Eclipse
        final Vector3D position1 = sun.getPVCoordinates(date, frame).getPosition().negate();
        final boolean actual1 = body.isInEclipse(date, position1, frame, sun);
        Assert.assertTrue(actual1);

        // No eclipse (body not on the path Sun - satellite)
        final Vector3D position2 = sun.getPVCoordinates(date, frame).getPosition().add(Vector3D.PLUS_I);
        final boolean actual2 = body.isInEclipse(date, position2, frame, sun);
        Assert.assertFalse(actual2);

        // No eclipse (body not on the path Sun - satellite but satellite on the path Sun - body)
        final Vector3D position3 = sun.getPVCoordinates(date, frame).getPosition().scalarMultiply(0.5);
        final boolean actual3 = body.isInEclipse(date, position3, frame, sun);
        Assert.assertFalse(actual3);
    }

    /**
     * @testType UT
     * 
     * @description check that cartesian - geodetic coordinates are properly performed.
     * 
     * @testPassCriteria cartesian - geodetic coordinates are properly performed (reference: OneAxisEllipsoid,
     *                   threshold: 1E-11, limited because file does contain numbers with only 11 digits)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void transformTest() throws PatriusException {
        // builds the map of the ellipsoid type and the expected GeometricBodyShape
        final Map<EllipsoidType, GeometricBodyShape> map = new HashMap<>();
        map.put(EllipsoidType.FITTED_ELLIPSOID, body.getFittedEllipsoid());
        map.put(EllipsoidType.INNER_SPHERE, body.getInnerSphere());
        map.put(EllipsoidType.OUTER_ELLIPSOID, body.getOuterEllipsoid());
        
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D position = new Vector3D(11E3, 12E3, 13E3);
        final Frame frame = FramesFactory.getEME2000();
        
        for(final Map.Entry<EllipsoidType, GeometricBodyShape> entry : map.entrySet()){
            // tests for each enumerate, get a copy of the original FacetBodyShape
            final FacetBodyShape bodyShape = new FacetBodyShape(body.getName(), body.getBodyFrame(), body, entry.getKey(), meshProv);
            
            // reference ellipsoid 
            final GeometricBodyShape ellipsoid = entry.getValue();
            
            // Check cartesian => geodetic coordinates
            final GeodeticPoint actual1 = bodyShape.transform(position, frame, date);
            final GeodeticPoint expected1 = ellipsoid.transform(position, frame, date);
            Assert.assertEquals(expected1.getLatitude(), actual1.getLatitude(), 1E-13);
            Assert.assertEquals(expected1.getLongitude(), actual1.getLongitude(), EPS);
            Assert.assertEquals(0., (expected1.getAltitude() - actual1.getAltitude()) / expected1.getAltitude(), 1E-11);

            // Check geodetic coordinates => cartesian
            final Vector3D actual2 = bodyShape.transform(actual1);
            final Vector3D expected2 = ellipsoid.transform(actual1);
            Assert.assertEquals(0., Vector3D.distance(expected2, actual2), EPS);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that distance to body is properly computed for various configurations:
     *              <ul>
     *              <li>Line at 10km from the body</li>
     *              <li>Body crossing (distance = 0)</li>
     *              </ul>
     * 
     * @testPassCriteria distance to body is properly computed for various configurations (reference: math)
     *
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void distanceToTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        
        // Case 1: tangent line 10km further
        final Vector3D position = new Vector3D(0, 0, 20E3);
        final Line line1 = new Line(position, position.add(Vector3D.PLUS_I));
        final double actual1 = body.distanceTo(line1, body.getBodyFrame(), date);
        Assert.assertEquals(10000, actual1, 0);
        
        // Case 2: crossing case
        final Line line2 = new Line(position, Vector3D.ZERO);
        final double actual2 = body.distanceTo(line2, body.getBodyFrame(), date);
        Assert.assertEquals(0, actual2, 0);
    }

    /**
     * @testType UT
     * 
     * @description check that the getNeighbors method properly returns the neighbors triangles of provided triangle for various configurations:
     *              <ul>
     *              <li>0m distance: only initial triangle is included</li>
     *              <li>10m distance: only initial triangle is included</li>
     *              <li>100m distance: only initial triangle and neighbors are included</li>
     *              <li>14000m distance: half of all triangles are included</li>
     *              <li>20000m distance: all triangles are included</li>
     *              </ul>
     *              Also in the case of a provided order of "neighborhood":
     *              <ul>
     *              <li>Order 0: provided triangle is expected</li>
     *              <li>Order 1: provided triangle and immediate neighbors are expected</li>
     *              <li>Order 2: provided triangle and immediate neighbors as well as their immediate neighbors are expected</li>
     *              <li>Order 1 000 000: all triangles are expected</li>
     *              </ul>
     * 
     * @testPassCriteria returned neighbors are as expected (reference: math)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void neighborsTest() throws PatriusException {
        // Initialization
        final Triangle triangle = body.getTriangles()[0];

        // getNeighbors(Triangle, distance)

        // Max distance = 0m: only initial triangle is included
        final List<Triangle> actual1 = body.getNeighbors(triangle, 0.);
        Assert.assertEquals(1, actual1.size());
        checkTriangles(actual1, triangle, 0);

        // Max distance = 10m: only initial triangle is included
        final List<Triangle> actual2 = body.getNeighbors(triangle, 10.);
        Assert.assertEquals(1, actual2.size());
        checkTriangles(actual2, triangle, 10);

        // Max distance = 20000m: all triangles are included
        final List<Triangle> actual3 = body.getNeighbors(triangle, 20000.);
        Assert.assertEquals(9800, actual3.size());
        checkTriangles(actual3, triangle, 20000);

        // Max distance = 14 000m: half of the bodies triangles are included
        final List<Triangle> actual3bis = body.getNeighbors(triangle, 10000 * MathLib.sqrt(2));
        Assert.assertEquals(4911, actual3bis.size());
        checkTriangles(actual3bis, triangle, 10000 * MathLib.sqrt(2));

        // Max distance = 100m: only first neighbors are included
        final List<Triangle> actual4 = body.getNeighbors(triangle, 100.);
        Assert.assertEquals(7, actual4.size());
        checkTriangles(actual4, triangle, 100);

        // getNeighbors(GeodeticPoint, distance)

        // Max distance = 100m with far geodetic point: no neighbors are included
        final GeodeticPoint point5 = new GeodeticPoint(1, 2, 101);
        final List<Triangle> actual5 = body.getNeighbors(point5, 100.);
        Assert.assertEquals(0, actual5.size());
        checkTriangles(actual5, point5, 100);

        // Max distance = 100m with geodetic point 100m from north pole: no neighbors are included (since triangle center is taken into account)
        final GeodeticPoint point6 = new GeodeticPoint(MathLib.PI / 2., 0, 100);
        final List<Triangle> actual6 = body.getNeighbors(point6, 100.);
        Assert.assertEquals(0, actual6.size());
        checkTriangles(actual6, point6, 100);

        // Max distance = 600m with geodetic point 100m from north pole: all north pole neighbors are included
        final GeodeticPoint point7 = new GeodeticPoint(MathLib.PI / 2., 0, 100);
        final List<Triangle> actual7 = body.getNeighbors(point7, 600.);
        Assert.assertEquals(100, actual7.size());
        checkTriangles(actual7, point7, 600);

        // getNeighbors(Triangle, order)

        // Max distance = 0 order: only initial triangle is included
        final List<Triangle> actual8 = body.getNeighbors(triangle, 0);
        Assert.assertEquals(1, actual8.size());
        checkTriangles(actual8, triangle, 0);

        // Max distance = 1 order: only initial triangle and first neighbors are included
        final List<Triangle> actual9 = body.getNeighbors(triangle, 1);
        Assert.assertEquals(4, actual9.size());
        checkTriangles(actual9, triangle, 500.);

        // Max distance = 2 orders: only initial triangle and first neighbors and their neightbors are included
        final List<Triangle> actual10 = body.getNeighbors(triangle, 2);
        Assert.assertEquals(10, actual10.size());
        checkTriangles(actual10, triangle, 1000.);

        // Max distance = 1 000 000 orders: all triangles are included
        final List<Triangle> actual11 = body.getNeighbors(triangle, 1000000);
        Assert.assertEquals(9800, actual11.size());
        checkTriangles(actual11, triangle, 20000);

        // getNeighbors(GeodeticPoint, order)

        // Max distance = 0 order: only closest triangle is included
        final GeodeticPoint point12 = new GeodeticPoint(-FastMath.PI / 2., 0, 10);
        final List<Triangle> actual12 = body.getNeighbors(point12, 0);
        Assert.assertEquals(1, actual12.size());
        checkTriangles(actual12, triangle, 0);

        // Max distance = 1 order: only initial triangle and first neighbors are included
        final List<Triangle> actual13 = body.getNeighbors(point12, 1);
        Assert.assertEquals(4, actual13.size());
        checkTriangles(actual13, triangle, 500.);

        // Max distance = 2 orders: only initial triangle and first neighbors and their neightbors are included
        final List<Triangle> actual14 = body.getNeighbors(point12, 2);
        Assert.assertEquals(10, actual14.size());
        checkTriangles(actual14, triangle, 1000.);

        // Max distance = 1 000 000 orders: all triangles are included
        final List<Triangle> actual15 = body.getNeighbors(point12, 1000000);
        Assert.assertEquals(9800, actual15.size());
        checkTriangles(actual15, triangle, 20000);

        // getNeighbors(Vector3D, distance)

        // Max distance = 100m with far geodetic point: no neighbors are included
        final Vector3D point16 = new Vector3D(10101, 0, 0);
        final List<Triangle> actual16 = body.getNeighbors(point16, 100.);
        Assert.assertEquals(0, actual16.size());
        checkTriangles(actual5, point16, 100);

        // Max distance = 100m with geodetic point 100m from north pole: no neighbors are included (since triangle center is taken into account)
        final Vector3D point17 = new Vector3D(10100, 0, 0);
        final List<Triangle> actual17 = body.getNeighbors(point17, 100.);
        Assert.assertEquals(0, actual17.size());
        checkTriangles(actual17, point17, 100);

        // Max distance = 600m with geodetic point 100m from north pole: all north pole neighbors are included
        final Vector3D point18 = new Vector3D(0, 0, 10100);
        final List<Triangle> actual18 = body.getNeighbors(point18, 600.);
        Assert.assertEquals(100, actual18.size());
        checkTriangles(actual18, point18, 600);

        // getNeighbors(Vector3D, order)

        // Max distance = 0 order: only closest triangle is included
        // Pos is exactly the same as GeodeticPoint(-Pi / 2, 0, 10). Exact value should be (0, 0, -10010)
        final Vector3D point19 = new Vector3D(6.1293572297325E-13, 0, -10009.999999999996);
        final List<Triangle> actual19 = body.getNeighbors(point19, 0);
        Assert.assertEquals(1, actual19.size());
        checkTriangles(actual19, triangle, 0);

        // Max distance = 1 order: only initial triangle and first neighbors are included
        final List<Triangle> actual20 = body.getNeighbors(point19, 1);
        Assert.assertEquals(4, actual20.size());
        checkTriangles(actual20, triangle, 500.);

        // Max distance = 2 orders: only initial triangle and first neighbors and their neightbors are included
        final List<Triangle> actual21 = body.getNeighbors(point19, 2);
        Assert.assertEquals(10, actual21.size());
        checkTriangles(actual21, triangle, 1000.);

        // Max distance = 1 000 000 orders: all triangles are included
        final List<Triangle> actual22 = body.getNeighbors(point19, 1000000);
        Assert.assertEquals(9800, actual22.size());
        checkTriangles(actual22, triangle, 20000);
   }

    /**
     * @testType UT
     * 
     * @description check that field data are properly computed: list of visible triangles, visible surface and contour
     *              in different cases (infinite field of view, narrow field of view and standard field of view):
     *              <ul>
     *              <li>Infinite field of view: half of the body is supposed to be seen</li>
     *              <li>Infinitely small field of view: none of the body should be seen</li>
     *              <li>Standard field of view (from North pole with a field of view such that latitudes larger than 60&deg; are covered), Fast algorithm</li>
     *              </ul>
     * 
     * @testPassCriteria returned field data are as expected (reference: math, threshold 1E-2, limited due to limited number of vertices)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void fieldDataTest() throws PatriusException {
        // State
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 1E5);
        final Attitude attitude = attitudeProvider.getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);
        
        // Case 1: infinite field of view: half of the body should be seen
        final IFieldOfView fieldOfView1 = new CircularField("", MathLib.PI / 2., Vector3D.PLUS_K);
        final FieldData actual1 = body.getFieldData(state, fieldOfView1, null);
        // Checks
        Assert.assertEquals(body.getTriangles().length / 2, actual1.getVisibleTriangles().size());
        final double referenceSurface = 4. * MathLib.PI * bodyRadius * bodyRadius / 2.;
        Assert.assertEquals(0., (referenceSurface - actual1.getVisibleSurface()) / referenceSurface, 1E-3);
        Assert.assertEquals(0, actual1.getDate().durationFrom(state.getDate()), 0.);
        final List<GeodeticPoint> contour1 = actual1.getContour();
        Assert.assertEquals(100, contour1.size());
        for (final GeodeticPoint point : contour1) {
            Assert.assertEquals(0., point.getLatitude(), 0.);
            Assert.assertEquals(0., point.getAltitude() / bodyRadius, 1E-10);
        }

        // Case 2: 0deg field of view: none of the body should be seen
        final IFieldOfView fieldOfView2 = new CircularField("", 1.0e-13, Vector3D.PLUS_K);
        final FieldData actual2 = body.getFieldData(state, fieldOfView2, null);
        // Checks
        Assert.assertEquals(0, actual2.getVisibleTriangles().size());
        Assert.assertEquals(0., actual2.getVisibleSurface(), 0);
        
        // Case 3: 5000/20000E3 field of view : half of the visible body should be seen (annulus of latitude 60deg)
        final IFieldOfView fieldOfView3 = new CircularField("", MathLib.atan(5000. / 20000E3), Vector3D.PLUS_K);
        final FieldData actual3 = body.getFieldData(state, fieldOfView3, Vector3D.PLUS_K);
        // Checks (first point)
        Assert.assertEquals(1500, actual3.getVisibleTriangles().size());
        final double referenceSurface3 = 2. * MathLib.PI * bodyRadius * bodyRadius * (1 - MathLib.sqrt(3) / 2.);
        Assert.assertEquals(0., (referenceSurface3 - actual3.getVisibleSurface()) / referenceSurface, 2E-2);
        // Visible triangles should have a Z coordinate > 8600m (above annulus of latitude 60deg)
        for (int i = 0; i < actual3.getVisibleTriangles().size(); i++) {
            // 5m margin to account for limited number of vertices (center of triangle is not exactly on the body sphere but a few meters outside)
            Assert.assertTrue(actual3.getVisibleTriangles().get(i).getCenter().getZ() >= MathLib.sqrt(3) / 2. * bodyRadius - 5);
        }
        // Contour should have 100 points of latitude 60deg and altitude 10 000m
        final List<GeodeticPoint> contour3 = actual3.getContour();
        Assert.assertEquals(100, contour3.size());
        final double referenceLatitude3 = MathLib.toRadians(60.);
        for (final GeodeticPoint point : contour3) {
            Assert.assertEquals(0., (point.getLatitude() - referenceLatitude3) / referenceLatitude3, 5E-2);
            Assert.assertEquals(0., point.getAltitude() / bodyRadius, 1E-10);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that surface pointed data are properly computed in different cases:
     *              <ul>
     *              <li>Line of sight intersects with body and Sun is visible from the target triangle</li>
     *              <li>Line of sight intersects with body and Sun is not visible from the target triangle</li>
     *              <li>No intersection: exception thrown</li>
     *              </ul>
     * 
     * @testPassCriteria returned surface data are as expected (reference: math, threshold 1E-2, limited due to limited number of vertices)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void surfacePointedDataTest() throws PatriusException {
        // Ephemeris with 1 point
        final List<SpacecraftState> statesList = new ArrayList<SpacecraftState>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 1E5).shiftedBy(i * 5000);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
        }
        
        // Case 1: intersection, Sun visible from the triangle
        final PVCoordinatesProvider sun1 = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(1E6, 0, 1E6), Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        final List<SurfacePointedData> ephemeris1 = body.getSurfacePointedDataEphemeris(statesList, Vector3D.PLUS_K, sun1, 0.1);
        // Checks (1st point)
        Assert.assertEquals(statesList.size(), ephemeris1.size());
        final SurfacePointedData actual1 = ephemeris1.get(0);
        Assert.assertEquals(0., (actual1.getDistance() - 10000) / 10000, EPS);
        Assert.assertEquals(0, actual1.getIncidence(), 5E-2);
        Assert.assertEquals(MathLib.PI / 4., actual1.getSolarIncidence(), 5E-2);
        Assert.assertEquals(MathLib.PI / 4., actual1.getPhaseAngle(), 1E-2);
        Assert.assertEquals(0, actual1.getDate().durationFrom(statesList.get(0).getDate()), 0.);
        final Vector3D referenceIntersection1 = statesList.get(0).getPVCoordinates().getPosition().normalize().scalarMultiply(bodyRadius);
        Assert.assertEquals(0, actual1.getIntersection().getPoint().distance(referenceIntersection1), 1E-11);
        // Checks (2nd point, a quarter of an orbit later)
        final SurfacePointedData actual11 = ephemeris1.get(1);
        Assert.assertEquals(0., (actual11.getDistance() - 10000) / 10000, 2E-4);

        // Case 1: intersection, Sun not visible from the triangle
        final PVCoordinatesProvider sun2 = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(1E6, 0, -1E6), Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        final List<SurfacePointedData> ephemeris2 = body.getSurfacePointedDataEphemeris(statesList, Vector3D.PLUS_K, sun2, 0.1);
        // Checks
        Assert.assertEquals(statesList.size(), ephemeris1.size());
        final SurfacePointedData actual2 = ephemeris2.get(0);
        Assert.assertEquals(0., (actual2.getDistance() - 10000) / 10000, EPS);
        Assert.assertEquals(0, actual2.getIncidence(), 5E-2);
        Assert.assertTrue(actual2.getSolarIncidence() > MathLib.PI / 2.);
        Assert.assertTrue(Double.isNaN(actual2.getPhaseAngle()));

        // Case 3: no intersection, but line of sight in line with body
        try {
            body.getSurfacePointedDataEphemeris(statesList, Vector3D.MINUS_K, sun1, 0.1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Case 4: no intersection at all
        try {
            body.getSurfacePointedDataEphemeris(statesList, Vector3D.PLUS_J, sun1, 0.1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that list of never visible triangles is properly computed in some various cases:
     *              <ul>
     *              <li>1 point ephemeris with far satellite: half of the body should be seen</li>
     *              <li>2 opposite points ephemeris with far satellite: no points are hidden</li>
     *              <li>2 points with Pi/2 between each: 3/4 of the body should be seen</li>
     *              </ul>
     * 
     * @testPassCriteria returned lists of never visible triangles are as expected (reference: math)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void neverVisibleTrianglesTest() throws PatriusException {
        // Case 1: one point ephemeris: half of the body should be seen

        // Ephemeris with 1 point
        final List<SpacecraftState> statesList = new ArrayList<SpacecraftState>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < 1; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU).shiftedBy(i);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
        }
        
        final List<Triangle> ephemeris1 = body.getNeverVisibleTriangles(statesList, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(4900, ephemeris1.size());

        // Case 2: two opposite points ephemeris: all of the body should be seen, no permanently hidden triangle 

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList2 = new ArrayList<SpacecraftState>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i, PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList2.add(new SpacecraftState(orbit, attitude));
        }

        final List<Triangle> ephemeris2 = body.getNeverVisibleTriangles(statesList2, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(0, ephemeris2.size());

        // Case 3: two points ephemeris with Pi/2 angle between each: 3/4 of the body should be seen 

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList3 = new ArrayList<SpacecraftState>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI / 2. * i, PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList3.add(new SpacecraftState(orbit, attitude));
        }

        final List<Triangle> ephemeris3 = body.getNeverVisibleTriangles(statesList3, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(2450, ephemeris3.size());
    }

    /**
     * @testType UT
     * 
     * @description check that list of never enlightened triangles is properly computed in some various cases:
     *              <ul>
     *              <li>1 point ephemeris with far satellite: half of the body should be seen</li>
     *              <li>2 opposite points ephemeris with far satellite: no points are hidden</li>
     *              <li>2 points with Pi/2 between each: 3/4 of the body should be seen</li>
     *              </ul>
     * 
     * @testPassCriteria returned lists of never enlightened triangles are as expected (reference: math)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void neverEnlightenedTrianglesTest() throws PatriusException {
        // Case 1: one point ephemeris: half of the body should be seen

        // Ephemeris with 1 point
        final List<SpacecraftState> statesList = new ArrayList<SpacecraftState>();
        final List<AbsoluteDate> datesList = new ArrayList<AbsoluteDate>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < 1; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU).shiftedBy(i);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
            datesList.add(orbit.getDate());
        }
        final PVCoordinatesProvider sun1 = statesList.get(0).getOrbit();
        
        final List<Triangle> ephemeris1 = body.getNeverEnlightenedTriangles(datesList, sun1);
        // Checks
        Assert.assertEquals(4900, ephemeris1.size());

        // Case 2: two opposite points ephemeris: all of the body should be seen, no permanently hidden triangle 

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList2 = new ArrayList<SpacecraftState>();
        final List<AbsoluteDate> datesList2 = new ArrayList<AbsoluteDate>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i, PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i), Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList2.add(new SpacecraftState(orbit, attitude));
            datesList2.add(orbit.getDate());
        }
        final PVCoordinatesProvider sun2 = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI), 0, MathLib.cos(dt * MathLib.PI)).scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris2 = body.getNeverEnlightenedTriangles(datesList2, sun2);
        // Checks
        Assert.assertEquals(0, ephemeris2.size());

        // Case 3: two points ephemeris with Pi/2 angle between each: 3/4 of the body should be seen 

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList3 = new ArrayList<SpacecraftState>();
        final List<AbsoluteDate> datesList3 = new ArrayList<AbsoluteDate>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI / 2. * i, PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i), Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList3.add(new SpacecraftState(orbit, attitude));
            datesList3.add(orbit.getDate());
        }
        final PVCoordinatesProvider sun3 = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI / 2), 0, MathLib.cos(dt * MathLib.PI / 2)).scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris3 = body.getNeverEnlightenedTriangles(datesList3, sun3);
        // Checks
        Assert.assertEquals(2450, ephemeris3.size());
    }

    /**
     * @testType UT
     * 
     * @description check that list of visible and enlightened triangles is properly computed in some various cases:
     *              <ul>
     *              <li>1 point ephemeris with far satellite, Sun at same location: half of the body should be seen</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at same location, omni-directional FOV: no points are hidden</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at opposite location, omni-directional FOV: no points are visible</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at same location, 0deg circular field of view: no points are visible</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at same location, 90deg circular field of view with opposite direction: no points are visible</li>
     *              <li>2 opposite points ephemeris with far satellite, Sun at initial ephemeris location, standard field of view (from North pole with a field of view such that latitudes larger than 60&deg; are covered): half of the body should be seen</li>
     *              </ul>
     * 
     * @testPassCriteria returned lists of visible and enlightened triangles are as expected (reference: math)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void visibleAndEnlightenedTrianglesTest() throws PatriusException {
        // Case 1: one point ephemeris, Sun at same location: half of the body should be seen

        // Ephemeris with 1 point
        final List<SpacecraftState> statesList = new ArrayList<SpacecraftState>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < 1; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU).shiftedBy(i);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun1 = statesList.get(0).getOrbit();
        
        final List<Triangle> ephemeris1 = body.getVisibleAndEnlightenedTriangles(statesList, sun1, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(4900, ephemeris1.size());

        // Case 2: two opposite points ephemeris, Sun at same location: all of the body should be seen, no permanently hidden triangle 

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList2 = new ArrayList<SpacecraftState>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i, PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i), Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList2.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun2 = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI), 0, MathLib.cos(dt * MathLib.PI)).scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris2 = body.getVisibleAndEnlightenedTriangles(statesList2, sun2, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(9800, ephemeris2.size());

        // Same check with 0deg field of view: 0 visible and enlightened triangles at the same time 
        final List<Triangle> ephemeris2bis = body.getVisibleAndEnlightenedTriangles(statesList2, sun2, new CircularField("", 1E-6, Vector3D.PLUS_K));
        Assert.assertEquals(0, ephemeris2bis.size());
        // Same check with opposite field of view: 0 visible and enlightened triangles at the same time 
        final List<Triangle> ephemeris2ter = body.getVisibleAndEnlightenedTriangles(statesList2, sun2, new CircularField("", MathLib.PI / 2., Vector3D.MINUS_K));
        Assert.assertEquals(0, ephemeris2ter.size());

        
        // Case 3: two opposite points ephemeris, Sun at opposite location: no visible and enlightened triangles at the same time

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList3 = new ArrayList<SpacecraftState>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i, PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i), Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList3.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun3 = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(MathLib.sin(dt * MathLib.PI), 0, -MathLib.cos(dt * MathLib.PI)).scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final List<Triangle> ephemeris3 = body.getVisibleAndEnlightenedTriangles(statesList3, sun3, new OmnidirectionalField(""));
        // Checks
        Assert.assertEquals(0, ephemeris3.size());

        // Case 4: two opposite points ephemeris with far satellite, Sun at initial ephemeris location, standard field of view (from North pole with a field of view such that latitudes larger than 60&deg; are covered): half of the body should be seen 

        // Ephemeris with 2 points
        final List<SpacecraftState> statesList4 = new ArrayList<SpacecraftState>();
        for (int i = 0; i < 2; i++) {
            final Orbit orbit = new KeplerianOrbit(20000E3, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2. + MathLib.PI * i, PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(i), Constants.CNES_STELA_MU);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList4.add(new SpacecraftState(orbit, attitude));
        }
        final PVCoordinatesProvider sun4 = new PVCoordinatesProvider() {
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                    Frame frame) throws PatriusException {
                final double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                final Vector3D pos = new Vector3D(0, 0, 1).scalarMultiply(1E9);
                return new PVCoordinates(pos, Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final IFieldOfView fieldOfView4 = new CircularField("", MathLib.atan(5000. / 20000E3), Vector3D.PLUS_K);

        final List<Triangle> ephemeris4 = body.getVisibleAndEnlightenedTriangles(statesList4, sun4, fieldOfView4);
        // Checks
        Assert.assertEquals(1500, ephemeris4.size());
    }

    /**
     * @testType UT
     * 
     * @description check that the local altitude method properly computes the local altitude.
     *              In any case, here local altitude is expected to be 0 (since the body is a sphere)
     * 
     * @testPassCriteria local altitude is as expected (reference: math)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void localAltitudeTest() throws PatriusException {
        // Method with arguments (latitude, longitude)
        Assert.assertEquals(0., body.getLocalAltitude(0, 0), 1E-11);
        Assert.assertEquals(0., body.getLocalAltitude(MathLib.PI / 2., 0), 1E-11);
        // Local altitude is always negative in this case
        Assert.assertTrue(body.getLocalAltitude(0.1, 0.1) < 0);
        Assert.assertTrue(body.getLocalAltitude(-0.3, 0.4) < 0);

        // Method with arguments (direction)
        Assert.assertEquals(0., body.getLocalAltitude(Vector3D.PLUS_K), 1E-11);
        Assert.assertEquals(0., body.getLocalAltitude(Vector3D.PLUS_I), 1E-11);
    }

    /**
     * @testType UT
     * 
     * @description check all class exceptions: failed to load mesh, unavailable methods.
     * 
     * @testPassCriteria exceptions are thrown as expected
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void exceptionsTest() {
        try {
            new FacetBodyShape("", this.celestialBody.getTrueRotatingFrame(), null, EllipsoidType.INNER_SPHERE, new ObjMeshLoader("Dummy.obj"));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new FacetBodyShape("", this.celestialBody.getTrueRotatingFrame(), null, EllipsoidType.INNER_SPHERE, new GeodeticMeshLoader("Dummy.tab"));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that {@link EclipseDetector} can be used together with {@link Propagator} and
     *              {@link FacetCelestialBody}.
     *              Detected eclipse dates must be similar to dates obtained with a same-size {@link OneAxisEllipsoid}.
     * 
     * @testPassCriteria eclipse dates are as expected (reference: propagation with {@link OneAxisEllipsoid}, relative
     *                   threshold: 1E-14)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void eclipseDetectorTest() throws PatriusException {
        
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(15000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = date.shiftedBy(Constants.JULIAN_DAY);
        
        // Reference: a = 10 000m, f = 0
        final ExtendedOneAxisEllipsoid reference = new ExtendedOneAxisEllipsoid(10000, 0, FramesFactory.getGCRF(), "");
        final KeplerianPropagator propagatorRef = new KeplerianPropagator(orbit);
        final List<AbsoluteDate> referenceDates = new ArrayList<AbsoluteDate>();
        propagatorRef.addEventDetector(new EclipseDetector(new MeeusSun(), 700000E3, reference, 0, 600, 1E-14, Action.CONTINUE, Action.CONTINUE) {
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                    final boolean forward) throws PatriusException {
                referenceDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorRef.propagate(finalDate);
        
        // Actual data
        final KeplerianPropagator propagatorAct = new KeplerianPropagator(orbit);
        final List<AbsoluteDate> actualDates = new ArrayList<AbsoluteDate>();
        propagatorAct.addEventDetector(new EclipseDetector(new MeeusSun(), 700000E3, body, 0, 600, 1E-14, Action.CONTINUE, Action.CONTINUE) {
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                    final boolean forward) throws PatriusException {
                actualDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorAct.propagate(finalDate);
        
        // Check detected dates
        for (int i = 0; i < referenceDates.size(); i++) {
            Assert.assertEquals(0, referenceDates.get(i).durationFrom(actualDates.get(i)), 1E-14);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that {@link SensorModel} can be used together with {@link Propagator} and {@link FacetCelestialBody}.
     *              Detected sensor visibility dates must be similar to dates obtained with a same-size {@link OneAxisEllipsoid}.
     * 
     * @testPassCriteria sensor visibility dates are as expected (reference: propagation with {@link OneAxisEllipsoid}, 
     *                   absolute threshold: 1E-4, limited due to limited number of vertices)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void sensorModelTest() throws PatriusException {
        
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(15000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = date.shiftedBy(Constants.JULIAN_DAY / 2.);
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        final SpacecraftState state = new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit));
        
        // Reference: a = 10 000m, f = 0
        final ExtendedOneAxisEllipsoid reference = new ExtendedOneAxisEllipsoid(10000, 0, FramesFactory.getGCRF(), "");

        // Assembly reference
        final AssemblyBuilder builderRef = new AssemblyBuilder();
        builderRef.addMainPart("Main");
        final SensorProperty sensorPropertyRef = new SensorProperty(Vector3D.PLUS_K);
        sensorPropertyRef.setMainFieldOfView(new CircularField("", MathLib.PI / 4., Vector3D.PLUS_K));
        sensorPropertyRef.setMainTarget(new MeeusSun(), new ConstantRadiusProvider(700000E3));
        builderRef.addProperty(sensorPropertyRef, "Main");
        builderRef.initMainPartFrame(state);
        final Assembly assemblyRef = builderRef.returnAssembly();
        
        final KeplerianPropagator propagatorRef = new KeplerianPropagator(orbit);
        propagatorRef.setAttitudeProvider(attitudeProvider);
        final SensorModel sensorModelRef = new SensorModel(assemblyRef, "Main");
        sensorModelRef.addMaskingCelestialBody(reference);
        final List<AbsoluteDate> referenceDates = new ArrayList<AbsoluteDate>();
        propagatorRef.addEventDetector(new SensorVisibilityDetector(sensorModelRef, 600, 1E-14, Action.CONTINUE, Action.CONTINUE) {
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                    final boolean forward) throws PatriusException {
                referenceDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorRef.propagate(finalDate);
        
        // Actual data

        // Assembly actual
        final AssemblyBuilder builderAct = new AssemblyBuilder();
        builderAct.addMainPart("Main");
        final SensorProperty sensorPropertyAct = new SensorProperty(Vector3D.PLUS_K);
        sensorPropertyAct.setMainFieldOfView(new CircularField("", MathLib.PI / 4., Vector3D.PLUS_K));
        sensorPropertyAct.setMainTarget(new MeeusSun(), new ConstantRadiusProvider(700000E3));
        builderAct.addProperty(sensorPropertyAct, "Main");
        builderAct.initMainPartFrame(state);
        final Assembly assemblyAct = builderAct.returnAssembly();

        final KeplerianPropagator propagatorAct = new KeplerianPropagator(orbit);
        propagatorAct.setAttitudeProvider(attitudeProvider);
        final SensorModel sensorModelAct = new SensorModel(assemblyAct, "Main");
        sensorModelAct.addMaskingCelestialBody(body);
        final List<AbsoluteDate> actualDates = new ArrayList<AbsoluteDate>();
        propagatorAct.addEventDetector(new SensorVisibilityDetector(sensorModelAct, 600, 1E-14, Action.CONTINUE, Action.CONTINUE) {
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                    final boolean forward) throws PatriusException {
                actualDates.add(s.getDate());
                return super.eventOccurred(s, increasing, forward);
            }
        });
        propagatorAct.propagate(finalDate);
        
        // Check detected dates
        for (int i = 0; i < referenceDates.size(); i++) {
            Assert.assertEquals(0, referenceDates.get(i).durationFrom(actualDates.get(i)), 1E-4);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that min/max norm of body is properly computed
     * 
     * @testPassCriteria min/max norm is as expected (10000m) (reference: math)
     *
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public final void minmaxNormTest() throws PatriusException {
        final double expected = 10000;
        Assert.assertEquals(0, (expected - body.getMinNorm()) / expected, EPS);
        Assert.assertEquals(0, (expected - body.getMaxNorm()) / expected, EPS);
    }

    /**
     * @testType UT
     * 
     * @description check that inner ellipsoid is as expected.
     * 
     * @testPassCriteria inner ellipsoid is an ellipsoid of radius, the radius and the flattening of the ellipsoidal
     *                   body (reference math)
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void innerEllipsoidTest() {
        final ExtendedOneAxisEllipsoid innerEllipsoid = body.getInnerEllipsoid();
        Assert.assertEquals(0., (innerEllipsoid.getEquatorialRadius() - bodyRadius) / bodyRadius, EPS);
        Assert.assertEquals(0., innerEllipsoid.getFlattening(), EPS);
        Assert.assertEquals(0., innerEllipsoid.getFlattening(), EPS);
        Assert.assertEquals(body.getFittedEllipsoid().getFlattening(), innerEllipsoid.getFlattening(), EPS);
    }

    /**
     * @testType UT
     * 
     * @description check that outer ellipsoid is as expected.
     * 
     * @testPassCriteria outer ellipsoid is an ellipsoid of radius, the radius and the flattening of the ellipsoidal
     *                   body (reference math)
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void outerEllipsoidTest() {
        final ExtendedOneAxisEllipsoid outerEllipsoid = body.getOuterEllipsoid();
        Assert.assertEquals(0., (outerEllipsoid.getEquatorialRadius() - bodyRadius) / bodyRadius, EPS);
        Assert.assertEquals(0., outerEllipsoid.getFlattening(), EPS);
        Assert.assertEquals(body.getFittedEllipsoid().getFlattening(), outerEllipsoid.getFlattening(), EPS);
    }

    /**
     * @testType UT
     * 
     * @description check that inner sphere is as expected.
     * 
     * @testPassCriteria inner sphere is a sphere of radius, the radius and the flattening of the spherical body
     *                   (reference math)
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void innerSphereTest() {
        final ExtendedOneAxisEllipsoid innerSphere = body.getInnerSphere();
        Assert.assertEquals(0., (innerSphere.getEquatorialRadius() - bodyRadius) / bodyRadius, EPS);
        Assert.assertEquals(0., innerSphere.getFlattening(), EPS);
    }

    /**
     * @testType UT
     * 
     * @description check that outer sphere is as expected.
     * 
     * @testPassCriteria outer sphere is a sphere of radius, the radius and the flattening of the spherical body
     *                   (reference math)
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public final void outerSphereTest() {
        final ExtendedOneAxisEllipsoid outerSphere = body.getOuterSphere();
        Assert.assertEquals(0., (outerSphere.getEquatorialRadius() - bodyRadius) / bodyRadius, EPS);
        Assert.assertEquals(0., outerSphere.getFlattening(), EPS);
    }
    
    /**
     * @testType UT
     * 
     * @description check that the resized body sphere is as expected.
     * 
     * @testPassCriteria resized body differs from the original body by the defined margin value and type (reference
     *                   math)
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     * 
     * @throws PatriusException if the margin value is invalid
     */
    @Test
    public final void resizeTest() throws PatriusException {
        // Case with a zero margin distance
         final double marginValue2 = 0.;
         final FacetBodyShape body2 = body.resize(MarginType.DISTANCE, marginValue2);
         checkTriangles(body2.getTriangles(), MarginType.DISTANCE, marginValue2);
        // Case with a positive margin distance
        final double marginValue3 = 1E3;
        final FacetBodyShape body3 = body.resize(MarginType.DISTANCE, marginValue3);
        checkTriangles(body3.getTriangles(), MarginType.DISTANCE, marginValue3);
        // Case with a negative margin distance whose absolute value is smaller than the minimal norm
        final double marginValue4 = -body.getMinNorm() + 1E3;
        final FacetBodyShape body4 = body.resize(MarginType.DISTANCE, marginValue4);
        checkTriangles(body4.getTriangles(), MarginType.DISTANCE, marginValue4);
        // Case with a negative margin distance whose absolute value is larger than the minimal norm
        final double marginValue4b = -body.getMinNorm() - 1E3;
        try {
            body.resize(MarginType.DISTANCE, marginValue4b);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Case with a zero margin scale factor
        final double marginValue5 = 0.;
        try {
            body.resize(MarginType.SCALE_FACTOR, marginValue5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Case with a positive margin scale factor smaller than 1
        final double marginValue6 = 0.5;
        final FacetBodyShape body6 = body.resize(MarginType.SCALE_FACTOR, marginValue6);
        checkTriangles(body6.getTriangles(), MarginType.SCALE_FACTOR, marginValue6);
        Assert.assertEquals(body.getInnerSphere().getEquatorialRadius() * marginValue6, body6.getInnerSphere()
            .getEquatorialRadius());
        Assert.assertEquals(body.getInnerSphere().getFlattening(), body6.getInnerSphere().getFlattening());
        Assert.assertEquals(body.getOuterSphere().getEquatorialRadius() * marginValue6, body6.getOuterSphere()
            .getEquatorialRadius());
        Assert.assertEquals(body.getOuterSphere().getFlattening(), body6.getOuterSphere().getFlattening());
        Assert.assertEquals(body.getInnerEllipsoid().getEquatorialRadius() * marginValue6, body6.getInnerEllipsoid()
            .getEquatorialRadius());
        Assert.assertEquals(body.getInnerEllipsoid().getFlattening(), body6.getInnerEllipsoid().getFlattening());
        Assert.assertEquals(body.getOuterEllipsoid().getEquatorialRadius() * marginValue6, body6.getOuterEllipsoid()
            .getEquatorialRadius());
        Assert.assertEquals(body.getOuterEllipsoid().getFlattening(), body6.getOuterEllipsoid().getFlattening());
        // Case with a positive margin scale factor larger than 1
        final double marginValue7 = 2.;
        final FacetBodyShape body7 = body.resize(MarginType.SCALE_FACTOR, marginValue7);
        checkTriangles(body7.getTriangles(), MarginType.SCALE_FACTOR, marginValue7);
        Assert.assertEquals(body.getInnerSphere().getEquatorialRadius() * marginValue7, body7.getInnerSphere()
            .getEquatorialRadius());
        Assert.assertEquals(body.getInnerSphere().getFlattening(), body7.getInnerSphere().getFlattening());
        Assert.assertEquals(body.getOuterSphere().getEquatorialRadius() * marginValue7, body7.getOuterSphere()
            .getEquatorialRadius());
        Assert.assertEquals(body.getOuterSphere().getFlattening(), body7.getOuterSphere().getFlattening());
        Assert.assertEquals(body.getInnerEllipsoid().getEquatorialRadius() * marginValue7, body7.getInnerEllipsoid()
            .getEquatorialRadius());
        Assert.assertEquals(body.getInnerEllipsoid().getFlattening(), body7.getInnerEllipsoid().getFlattening());
        Assert.assertEquals(body.getOuterEllipsoid().getEquatorialRadius() * marginValue7, body7.getOuterEllipsoid()
            .getEquatorialRadius());
        Assert.assertEquals(body.getOuterEllipsoid().getFlattening(), body7.getOuterEllipsoid().getFlattening());
        // Case with a negative margin scale
        final double marginValue8 = -0.5;
        try {
            body.resize(MarginType.SCALE_FACTOR, marginValue8);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that the returned apparent radius is as expected.
     * 
     * @testPassCriteria returned apparent radius coincides with the radius of the outer sphere (reference math)
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     * 
     * @throws PatriusException if the {@link PVCoordinatesProvider} computation fails, if the position cannot be
     *         computed in the given frame or if the line cannot be converted to the body frame
     */
    @Test
    public final void localRadiusTest() throws PatriusException {
        // Set data root
        Utils.setDataRoot("regular-data");
        // Define date
        final AbsoluteDate date = new AbsoluteDate();
        // Define frame
        final Frame frame = body.getBodyFrame();
        // Define body position
        final Vector3D bodyPosition = body.getPVCoordinates(date, frame).getPosition();
        // Define distance vector between body and spacecraft
        final Vector3D distance = new Vector3D(1E4, 2E4, 3E4);
        // Define spacecraft position
        final Vector3D scPosition = bodyPosition.add(distance);
        // Retrieve celestial body
        final PVCoordinatesProvider pvCoordinatesCel = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(11E3, 12E3, 13E3), Vector3D.ZERO);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        // Compute the apparent radius
        final double apparentRadius = body.getLocalRadius(scPosition, frame, date, pvCoordinatesCel);
        // Retrieve the expected radius
        final double expectedRadius = body.getOuterSphere().getEquatorialRadius();
        // Check that the apparent radius coincides with the radius of the outer sphere
        Assert.assertEquals(0., (apparentRadius - expectedRadius) / expectedRadius, EPS);
    }

    /**
     * Check that provided list of triangles is within provided distance of provided triangle.
     * 
     * @param triangles triangles list to check
     * @param triangle triangle
     * @param maxDistance max distance
     */
    private void checkTriangles(final List<Triangle> triangles, final Triangle triangle, final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(triangle.getCenter()) <= maxDistance);
        }
    }

    /**
     *  Check that provided list of triangles is within provided distance of provided geodetic point.
     * @param triangles triangles list to check
     * @param point geodetic point
     * @param maxDistance max distance
     */
    private void checkTriangles(final List<Triangle> triangles, final GeodeticPoint point, final double maxDistance) {
        final Vector3D position = body.transform(point);
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(position) <= maxDistance);
        }
    }

    /**
     *  Check that provided list of triangles is within provided distance of provided 3D point.
     * @param triangles triangles list to check
     * @param position 3D position
     * @param maxDistance max distance
     */
    private void checkTriangles(final List<Triangle> triangles, final Vector3D position, final double maxDistance) {
        for (final Triangle t : triangles) {
            Assert.assertTrue(t.getCenter().distance(position) <= maxDistance);
        }
    }

    /**
     * Check that the provided array of modified triangles differ with respect to the original one by the given margin
     * value of the specified margin type.
     * 
     * @param modifiedTriangles array of modified triangles to check
     * @param marginType margin type
     * @param marginValue margin value
     */
    private void checkTriangles(final Triangle[] modifiedTriangles, final MarginType marginType,
                                final double marginValue) {
        final Triangle[] originalTriangles = body.getTriangles();
        if (marginType.equals(MarginType.DISTANCE)) {
            for (int indexTriangle = 0; indexTriangle < modifiedTriangles.length; indexTriangle++) {
                final Triangle originalTriangle = originalTriangles[indexTriangle];
                final Triangle modifiedTriangle = modifiedTriangles[indexTriangle];
                final Vertex[] originalVertices = originalTriangle.getVertices();
                final Vertex[] modifiedVertices = modifiedTriangle.getVertices();
                for (int indexVertex = 0; indexVertex < modifiedVertices.length; indexVertex++) {
                    Assert
                        .assertEquals(
                            0.,
                            modifiedVertices[indexVertex].getPosition().getNorm()
                                    - originalVertices[indexVertex]
                                        .getPosition()
                                        .normalize()
                                        .scalarMultiply(
                                            originalVertices[indexVertex].getPosition().getNorm() + marginValue)
                                        .getNorm(), 1E-12);
                }
            }
        } else {
            for (int indexTriangle = 0; indexTriangle < modifiedTriangles.length; indexTriangle++) {
                final Triangle originalTriangle = originalTriangles[indexTriangle];
                final Triangle modifiedTriangle = modifiedTriangles[indexTriangle];
                final Vertex[] originalVertices = originalTriangle.getVertices();
                final Vertex[] modifiedVertices = modifiedTriangle.getVertices();
                for (int indexVertex = 0; indexVertex < modifiedVertices.length; indexVertex++) {
                    if (originalVertices[indexVertex].getPosition().getX() != 0) {
                        Assert.assertEquals(marginValue, modifiedVertices[indexVertex].getPosition().getX()
                                / originalVertices[indexVertex].getPosition().getX(), 1E-12);
                    } else {
                        Assert.assertEquals(0., modifiedVertices[indexVertex].getPosition().getX(), 1E-12);
                    }
                    if (originalVertices[indexVertex].getPosition().getY() != 0) {
                        Assert.assertEquals(marginValue, modifiedVertices[indexVertex].getPosition().getY()
                                / originalVertices[indexVertex].getPosition().getY(), 1E-12);
                    } else {
                        Assert.assertEquals(0., modifiedVertices[indexVertex].getPosition().getY(), 1E-12);
                    }
                    if (originalVertices[indexVertex].getPosition().getZ() != 0) {
                        Assert.assertEquals(marginValue, modifiedVertices[indexVertex].getPosition().getZ()
                                / originalVertices[indexVertex].getPosition().getZ(), 1E-12);
                    } else {
                        Assert.assertEquals(0., modifiedVertices[indexVertex].getPosition().getZ(), 1E-12);
                    }
                }
            }
        }
    }
}
