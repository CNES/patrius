/**
 *
 * Copyright 2011-2024 CNES
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
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-136:22/08/2024: [PATRIUS] Fitting d'un ThreeAxisEllipsoid sur un FacetBodyShape
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.AbstractEllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.ThreeAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitter.EllipsoidType;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link BodyShapeFitter} class.
 *
 * @author Manuel Amouroux
 *
 * @since 4.6
 */
public class BodyShapeFitterTest {

    /** Radius a (m). */
    private static final double aRadius = 2100000.;

    /** Radius b (m). */
    private static final double bRadius = 1700000.;

    /** Radius c (m). */
    private static final double cRadius = 1100000.;

    /** Flattening */
    private static final double flattening = 0.2;

    /** Sphere used by tests. */
    private static OneAxisEllipsoid sphere;

    /** Body shape fitter based on sphere.*/
    private static BodyShapeFitter fitterOnSphere;
    
    /** One-axis ellipsoid used by tests. */
    private static OneAxisEllipsoid oneAxisEllipsoid;

    /** Body shape fitter based on one-axis ellipsoid. */
    private static BodyShapeFitter fitterOnOneAxisEllipsoid;

    /** Three-axis ellipsoid used by tests. */
    private static ThreeAxisEllipsoid threeAxisEllipsoid;

    /** Body shape fitter based on three-axis ellipsoid. */
    private static BodyShapeFitter fitterOnThreeAxisEllipsoid;

    /** FacetBodyShape used by tests. */
    private static FacetBodyShape facetBodyShape;

    /** Body shape fitter based on facet body shape. */
    private static BodyShapeFitter fitterOnFacetBodyShape;

    /**
     * Setting up context before running the tests
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException, URISyntaxException {
        // Patrius data set
        Utils.setDataRoot("regular-dataPBASE");

        // Load Phobos .obj mesh
        final String modelFilePhobos = "mnt" + File.separator + "Phobos_Ernst_HD.obj";
        final String fullNamePhobos = FacetBodyShape.class.getClassLoader().getResource(modelFilePhobos).toURI()
            .getPath();

        // Shapes definitions
        sphere = new OneAxisEllipsoid(aRadius, 0, FramesFactory.getGCRF(), "Sphere");
        oneAxisEllipsoid = new OneAxisEllipsoid(aRadius, flattening, FramesFactory.getGCRF(), "One-axis Ellipsoid");
        threeAxisEllipsoid =
            new ThreeAxisEllipsoid(aRadius, bRadius, cRadius, FramesFactory.getGCRF(), "Three-axis ellipsoid");
        facetBodyShape = new FacetBodyShape("Facet body shape", FramesFactory.getGCRF(), new ObjMeshLoader(
            fullNamePhobos));
        
        // Fitters definitions
        fitterOnSphere = new BodyShapeFitter(sphere);
        fitterOnOneAxisEllipsoid = new BodyShapeFitter(oneAxisEllipsoid);
        fitterOnThreeAxisEllipsoid = new BodyShapeFitter(threeAxisEllipsoid);
        fitterOnFacetBodyShape = new BodyShapeFitter(facetBodyShape);
    }

    /**
     * @testType UT
     * 
     * @description check that fitted, inner or outer spheres are created as expected from supported objects.
     * 
     * @testPassCriteria Spheres have expected dimensions
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     */
    @Test
    public void testGetSphere() {
        // From sphere, should return the same object for inner, outer and fitted spheres
        OneAxisEllipsoid innerSphere = fitterOnSphere.getEllipsoid(EllipsoidType.SPHERE_INNER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, innerSphere));

        OneAxisEllipsoid outerSphere = fitterOnSphere.getEllipsoid(EllipsoidType.SPHERE_OUTER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, outerSphere));

        OneAxisEllipsoid fittedSphere = fitterOnSphere.getEllipsoid(EllipsoidType.SPHERE_FITTED);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, fittedSphere));

        // From one-axis ellipsoid
        innerSphere = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.SPHERE_INNER);
        Assert.assertEquals(oneAxisEllipsoid.getPolarRadius(), innerSphere.getPolarRadius(), 0.);
        Assert.assertEquals(oneAxisEllipsoid.getPolarRadius(), innerSphere.getEquatorialRadius(), 0.);

        outerSphere = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.SPHERE_OUTER);
        Assert.assertEquals(oneAxisEllipsoid.getEquatorialRadius(), outerSphere.getPolarRadius(), 0.);
        Assert.assertEquals(oneAxisEllipsoid.getEquatorialRadius(), outerSphere.getEquatorialRadius(), 0.);

        double expectedRadius = aRadius * MathLib.pow(1. - flattening, 1. / 3.);
        fittedSphere = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.SPHERE_FITTED);
        Assert.assertEquals(expectedRadius, fittedSphere.getPolarRadius(), 0.);
        Assert.assertEquals(expectedRadius, fittedSphere.getEquatorialRadius(), 0.);
        
        //From three-axis ellipsoid
        expectedRadius = MathLib.min(aRadius, MathLib.min(bRadius, cRadius));
        innerSphere = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.SPHERE_INNER);
        Assert.assertEquals(expectedRadius, innerSphere.getPolarRadius(), 0.);
        Assert.assertEquals(expectedRadius, innerSphere.getEquatorialRadius(), 0.);

        expectedRadius = MathLib.max(aRadius, MathLib.max(bRadius, cRadius));
        outerSphere = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.SPHERE_OUTER);
        Assert.assertEquals(expectedRadius, outerSphere.getPolarRadius(), 0.);
        Assert.assertEquals(expectedRadius, outerSphere.getEquatorialRadius(), 0.);

        expectedRadius = MathLib.pow(aRadius * bRadius * cRadius, 1. / 3.);
        fittedSphere = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.SPHERE_FITTED);
        Assert.assertEquals(expectedRadius, fittedSphere.getPolarRadius(), 0.);
        Assert.assertEquals(expectedRadius, fittedSphere.getEquatorialRadius(), 0.);

        // From Facet body shape
        innerSphere = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.SPHERE_INNER);
        Assert.assertEquals(8084.396633664135, innerSphere.getPolarRadius(), 0.);
        Assert.assertEquals(8084.396633664135, innerSphere.getEquatorialRadius(), 0.);

        outerSphere = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.SPHERE_OUTER);
        Assert.assertEquals(13966.315007903839, outerSphere.getPolarRadius(), 0.);
        Assert.assertEquals(13966.315007903839, outerSphere.getEquatorialRadius(), 0.);

        fittedSphere = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.SPHERE_FITTED);
        Assert.assertEquals(11144.18114730635, fittedSphere.getPolarRadius(), 0.);
        Assert.assertEquals(11144.18114730635, fittedSphere.getEquatorialRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @description Check that fitted, inner or outer one-axis ellipsoids are created as expected from supported
     *              objects.
     * 
     * @testPassCriteria One-axis ellipsoids have expected dimensions
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     */
    @Test
    public void testGetOneAxisEllipsoid() {
        // From sphere, should return same object
        OneAxisEllipsoid innerOAE =
            fitterOnSphere.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_INNER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, innerOAE));

        OneAxisEllipsoid outerOAE =
            fitterOnSphere.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_OUTER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, outerOAE));

        OneAxisEllipsoid fittedOAE =
            fitterOnSphere.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_FITTED);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, fittedOAE));

        // From one-axis ellipsoid, should return same object
        innerOAE = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_INNER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(oneAxisEllipsoid, innerOAE));

        outerOAE = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_OUTER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(oneAxisEllipsoid, outerOAE));

        fittedOAE = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_FITTED);
        Assert.assertTrue(checkEllipsoidsAreSimilar(oneAxisEllipsoid, fittedOAE));

        // From three-axis ellipsoid
        double expectedA = MathLib.min(aRadius, bRadius);
        double expectedF = 1. - cRadius / expectedA;
        innerOAE = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_INNER);
        Assert.assertEquals(expectedA, innerOAE.getEquatorialRadius(), 0.);
        Assert.assertEquals(expectedF, innerOAE.getFlattening(), 0.);

        expectedA = MathLib.max(aRadius, bRadius);
        expectedF = 1. - cRadius / expectedA;
        outerOAE = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_OUTER);
        Assert.assertEquals(expectedA, outerOAE.getEquatorialRadius(), 0.);
        Assert.assertEquals(expectedF, outerOAE.getFlattening(), 0.);

        expectedA = MathLib.sqrt(aRadius * bRadius);
        expectedF = 1. - cRadius / expectedA;
        fittedOAE = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_FITTED);
        Assert.assertEquals(expectedA, fittedOAE.getEquatorialRadius(), 0.);
        Assert.assertEquals(expectedF, fittedOAE.getFlattening(), 0.);

        // From Facet body shape
        innerOAE = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_INNER);
        Assert.assertEquals(7888.178854899496, innerOAE.getPolarRadius(), 0.);
        Assert.assertEquals(10424.120296826843, innerOAE.getEquatorialRadius(), 0.);

        outerOAE = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_OUTER);
        Assert.assertEquals(10725.85825543582, outerOAE.getPolarRadius(), 0.);
        Assert.assertEquals(14174.074751351569, outerOAE.getEquatorialRadius(), 0.);

        fittedOAE = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_FITTED);
        Assert.assertEquals(9258.45172753887, fittedOAE.getPolarRadius(), 0.);
        Assert.assertEquals(12234.91712669326, fittedOAE.getEquatorialRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @description Check that fitted, inner or outer three-axis ellipsoids are created as expected from supported
     *              objects.
     * 
     * @testPassCriteria Three-axis ellipsoids have expected dimensions
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     */
    @Test
    public void testGetThreeAxisEllipsoid() {
        // From sphere, should return same object
        ThreeAxisEllipsoid innerTAE = fitterOnSphere.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_INNER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, innerTAE));

        ThreeAxisEllipsoid outerTAE = fitterOnSphere.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_OUTER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, outerTAE));

        ThreeAxisEllipsoid fittedTAE = fitterOnSphere.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_FITTED);
        Assert.assertTrue(checkEllipsoidsAreSimilar(sphere, fittedTAE));

        // From one-axis ellipsoid, should return same object
        innerTAE = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_INNER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(oneAxisEllipsoid, innerTAE));

        outerTAE = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_OUTER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(oneAxisEllipsoid, outerTAE));

        fittedTAE = fitterOnOneAxisEllipsoid.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_FITTED);
        Assert.assertTrue(checkEllipsoidsAreSimilar(oneAxisEllipsoid, fittedTAE));

        // From three-axis ellipsoid, should return same object
        innerTAE = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_INNER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(threeAxisEllipsoid, innerTAE));

        outerTAE = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_OUTER);
        Assert.assertTrue(checkEllipsoidsAreSimilar(threeAxisEllipsoid, outerTAE));

        fittedTAE = fitterOnThreeAxisEllipsoid.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_FITTED);
        Assert.assertTrue(checkEllipsoidsAreSimilar(threeAxisEllipsoid, fittedTAE));

        // From Facet body shape
        innerTAE = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_INNER);
        Assert.assertEquals(11328.770606206204, innerTAE.getARadius(), 0.);
        Assert.assertEquals(9959.35284899827, innerTAE.getBRadius(), 0.);
        Assert.assertEquals(8063.711974528461, innerTAE.getCRadius(), 0.);

        outerTAE = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_OUTER);
        Assert.assertEquals(14878.439826299136, outerTAE.getARadius(), 0.);
        Assert.assertEquals(13079.939317645361, outerTAE.getBRadius(), 0.);
        Assert.assertEquals(10590.333016709132, outerTAE.getCRadius(), 0.);

        fittedTAE = fitterOnFacetBodyShape.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_FITTED);
        Assert.assertEquals(13027.41001154101, fittedTAE.getARadius(), 0.);
        Assert.assertEquals(11452.661327825983, fittedTAE.getBRadius(), 0.);
        Assert.assertEquals(9272.78746818356, fittedTAE.getCRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @description Check that the caching works as expected
     * 
     * @testPassCriteria Computation is faster the second time, as it relies on caching
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     */
    @Test
    public void cachingTest() {
        final BodyShapeFitter dummyFitter = new BodyShapeFitter(facetBodyShape);


            // Time first execution
            double tic = System.currentTimeMillis();
            dummyFitter.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_FITTED);
            final double durationWithoutCache = System.currentTimeMillis() - tic;

            // Time second execution
            tic = System.currentTimeMillis();
            dummyFitter.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_FITTED);
            final double durationWithCache = System.currentTimeMillis() - tic;

            Assert.assertTrue(durationWithCache < durationWithoutCache);

    }

    /**
     * @testType UT
     * 
     * @description Check that the methods throw the expected exceptions
     * 
     * @testPassCriteria Exceptions messages are as expected
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     */
    @Test
    public void exceptionsTest() {
        final BodyShapeFitter fitter = new BodyShapeFitter(new DummyBodyShape(null, null, false, null));
        try {
            fitter.getEllipsoid(EllipsoidType.SPHERE_INNER);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting an inner sphere to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.SPHERE_OUTER);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting an outer sphere to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.SPHERE_FITTED);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting a fitted sphere to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_INNER);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting an inner one-axis ellipsoid to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_OUTER);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting an outer one-axis ellipsoid to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_FITTED);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting a fitted one-axis ellipsoid to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_INNER);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting an inner three-axis ellipsoid to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_OUTER);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting an outer three-axis ellipsoid to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
        try {
            fitter.getEllipsoid(EllipsoidType.THREE_AXIS_ELLIPSOID_FITTED);
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(
                "Fitting a fitted three-axis ellipsoid to a class fr.cnes.sirius.patrius.bodies.mesh.BodyShapeFitterTest$DummyBodyShape object is not supported at the moment.",
                e.getMessage());
        }
    }

    /**
     * Compares two ellipsoids' dimensions.
     * 
     * @param first
     *        first ellipsoid
     * @param second
     *        second ellipsoid
     * 
     * @return true if the ellipsoids have the same a, b and c radiuses, false otherwise
     */
    private static boolean checkEllipsoidsAreSimilar(final AbstractEllipsoidBodyShape first,
                                              final AbstractEllipsoidBodyShape second) {
        if (first.getARadius() == second.getARadius() && first.getBRadius() == second.getBRadius()
                && first.getCRadius() == second.getCRadius()) {
            return true;
        }

        return false;
    }

    /**
     * Dummy implementation of BodyShape for tests.
     */
    private class DummyBodyShape extends AbstractEllipsoidBodyShape {

        /**
         * Serial
         */
        private static final long serialVersionUID = 3667291029582815267L;

        /**
         * Dummy constructor for dummy type.
         * 
         * @param ellipsoid
         * @param bodyFrame
         * @param isSpherical
         * @param name
         */
        public DummyBodyShape(final IEllipsoid ellipsoid, final CelestialBodyFrame bodyFrame, final boolean isSpherical,
                              final String name) {
            super(ellipsoid, bodyFrame, isSpherical, name);
        }

        @Override
        public double getEquatorialRadius() {
            return 0;
        }

        @Override
        public double getTransverseRadius() {
            return 0;
        }

        @Override
        public double getConjugateRadius() {
            return 0;
        }

        @Override
        public double getFlattening() {
            return 0;
        }

        @Override
        public double getE2() {
            return 0;
        }

        @Override
        public double getG2() {
            return 0;
        }

        @Override
        public EllipsoidPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                                   final AbsoluteDate date,
                                                   final double altitude) {
            return null;
        }

        @Override
        public BodyShape resize(final MarginType marginType, final double marginValue) {
            return null;
        }

    }

    @Before
    public void setUp() {
        Utils.clear();
    }
}
