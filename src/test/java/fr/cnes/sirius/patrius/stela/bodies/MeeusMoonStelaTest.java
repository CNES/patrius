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
 * @history creation 20/02/2013
 *
 *
 * HISTORY
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:591:05/04/2016:add IAU pole data
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.forces.gravity.AttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianAttraction;
import fr.cnes.sirius.patrius.forces.gravity.PointAttractionModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the coverage of the class MeeusSunStela.
 * 
 * @author Cedric Dental
 * 
 * @version
 * 
 * @since 1.3
 * 
 */
public class MeeusMoonStelaTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Meeus Moon test class
         * 
         * @featureDescription perform simple tests in accordance with Stela ones
         * 
         * @coveredRequirements
         */
        MEEUS_MOON_STELA
    }

    /** Moon */
    private CelestialBody moon;
    /** Date */
    private AbsoluteDate date;
    /** Radius */
    private double radius;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MeeusMoonStelaTest.class.getSimpleName(), "STELA Meeus Moon model");
    }

    /**
     * setUp.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {

        final double earthRadius = 6378136.46;
        final double moonRadius = 1737400;
        this.date = new AbsoluteDate(new DateComponents(2000, 01, 01),
            new TimeComponents(11, 59, 27.816), TimeScalesFactory.getTAI());
        this.moon = new MeeusMoonStela(earthRadius);
        this.radius = moonRadius;
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());
    }

    /**
     * Test update position.
     * 
     * @throws PatriusException
     */
    @Test
    public void testUpdatePosition() throws PatriusException {
        MeeusMoonStela.resetTransform();
        final double eps = 1e-15;
        final Vector3D position = this.moon.getPVCoordinates(this.date, FramesFactory.getMOD(false)).getPosition();
        final double normP = position.getNorm();
        final double x = position.getX();
        final double y = position.getY();
        final double z = position.getZ();
        // ExpectedValues
        final double xex = -291767.2045062491 * 1000;
        final double yex = -266633.22246664413 * 1000;
        final double zex = -75823.03640746597 * 1000;
        final double rex = 402455.599785102 * 1000;

        final double absX = (xex - x) / xex;
        final double absY = (yex - y) / yex;
        final double absZ = (zex - z) / zex;
        final double absR = (rex - normP) / rex;

        Assert.assertEquals(0, absX, eps);
        Assert.assertEquals(0, absY, eps);
        Assert.assertEquals(0, absZ, eps);
        Assert.assertEquals(0, absR, eps);

    }

    /**
     * Tests method getPVCoordinate with cached CIRF transformation
     * 
     * @testPassCriteria results close from Stela v2.6 (relative tolerance: 1E-12)
     * @throws PatriusException
     *         thrown if transformation computation failed
     * @since 3.0
     */
    @Test
    public void testUpdatePositionCIRF() throws PatriusException {

        Report.printMethodHeader("testUpdatePositionCIRF", "Position computation (CIRF frame)", "STELA 2.6", 7E-12,
            ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22370 * 86400 - 67.184 + 35);
        // Computation
        MeeusMoonStela.updateTransform(date, FramesFactory.getCIRF());
        final Vector3D position = this.moon.getPVCoordinates(date, FramesFactory.getCIRF()).getPosition();
        final double normP = position.getNorm();
        final double xp = position.getX() / normP;
        final double yp = position.getY() / normP;
        final double zp = position.getZ() / normP;

        final double[] exp = { 0.9489921307955463, -3.124074135502212801E-01, -4.260919674231677251E-02 };
        // Check result
        Assert.assertEquals((exp[0] - xp) / exp[0], 0, 5E-12);
        Assert.assertEquals((exp[1] - yp) / exp[1], 0, 5E-12);
        Assert.assertEquals((exp[2] - zp) / exp[2], 0, 7E-12);

        Report.printToReport("Position", new Vector3D(exp), new Vector3D(xp, yp, zp));

        MeeusMoonStela.resetTransform();
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_MOON_STELA}
     * 
     * @description check that IAU data have been initialised and body frame have been defined (through parent method)
     * 
     * @input None
     * 
     * @output transformation
     * 
     * @testPassCriteria returned transformation is not null.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3?2
     */
    @Test
    public final void testTransformation() throws PatriusException {
        Assert.assertNotNull(this.moon.getInertialEquatorFrame().getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));
        Assert.assertNotNull(this.moon.getTrueRotatingFrame().getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));

        // Test GetNativeFrame
        Assert.assertEquals(this.moon.getICRF().getParent(), moon.getNativeFrame(null, null));
    }
    
    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_MOON_STELA}
     * 
     * @description check that the default {@link GeometricBodyShape} is correctly built.
     * 
     * @input None
     * 
     * @output {@link GeometricBodyShape}
     */
    @Test
    public final void testGetShape() throws PatriusException{
    	
    	final Frame bodyRotatingFrame = this.moon.getTrueRotatingFrame();
    	final Frame bodyInertialFrame = this.moon.getInertialEquatorFrame();
    	
    	final GeometricBodyShape actualShape = this.moon.getShape();
    	final GeometricBodyShape expectedShape = new ExtendedOneAxisEllipsoid(this.radius, 0, bodyRotatingFrame, "Moon"); 
    	
    	// check the body names are the same
    	Assert.assertEquals(expectedShape.getName(), actualShape.getName());
    	
    	double actual;
    	double expected;
    	
    	// getIntersection method 
    	final Line line1 = new Line(new Vector3D(4 * this.radius, 0, 0), new Vector3D(0, 3 * this.radius, 0));
    	actual = actualShape.distanceTo(line1, bodyInertialFrame, this.date);
    	expected = expectedShape.distanceTo(line1, bodyInertialFrame, this.date);
    	Assert.assertEquals(expected, actual, 0.);
    	
        // getLocalRadius method
        final Vector3D posInFrame = new Vector3D(2.5 * this.radius, 0, 1.5 * this.radius);
        final PVCoordinates pv = new PVCoordinates(posInFrame, Vector3D.ZERO);
        final PVCoordinatesProvider provider = new ConstantPVCoordinatesProvider(pv, bodyInertialFrame);
        actual = actualShape.getLocalRadius(posInFrame.scalarMultiply(5), bodyInertialFrame, this.date, provider);
        expected = expectedShape.getLocalRadius(posInFrame.scalarMultiply(5), bodyInertialFrame, this.date, provider);
        Assert.assertEquals(expected, actual, 0.);

        // getIntersections method
        final Line line2 = new Line(new Vector3D(0.9 * this.radius, 0., 0.), Vector3D.ZERO);
        final Vector3D[] intersections = actualShape.getIntersectionPoints(line2, bodyInertialFrame, this.date);
        final Vector3D[] expectedInter = expectedShape.getIntersectionPoints(line2, bodyInertialFrame, this.date);
        Assert.assertEquals(intersections.length, expectedInter.length);
        Assert.assertEquals(intersections[0], expectedInter[0]);
        Assert.assertEquals(intersections[intersections.length - 1], expectedInter[expectedInter.length - 1]);
    }
    
    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_MOON_STELA}
     * 
     * @description check that the setter method of {@link CelestialBody} is correct.
     * 
     * @input {@link GeometricBodyShape}
     * 
     * @output {@link GeometricBodyShape}
     */
    @Test
    public void testSetShape() throws PatriusException{
    	
    	final Frame bodyRotatingFrame = this.moon.getTrueRotatingFrame();
    	final Frame bodyInertialFrame = this.moon.getInertialEquatorFrame();
    	
    	// the default shape
    	final CelestialBody moon = this.moon;
    	final GeometricBodyShape defaultShape = moon.getShape(); 
    	
    	// the new body shape
    	final GeometricBodyShape newShape = new ExtendedOneAxisEllipsoid(this.radius * 1.05, 0.01, bodyRotatingFrame, "moon modified");
    	
    	moon.setShape(newShape);
    	
    	// check the body name is modified
    	Assert.assertEquals(moon.getShape().getName(), newShape.getName());
    	// getIntersection method 
    	final Line line1 = new Line(new Vector3D(4 * this.radius, 0, 0), new Vector3D(0, 3 * this.radius, 0));
    	final double oldDistance = defaultShape.distanceTo(line1, bodyInertialFrame, this.date);
    	final double newDistance = moon.getShape().distanceTo(line1, bodyInertialFrame, this.date);
    	Assert.assertTrue(newDistance < oldDistance);
    }
    
    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#MEEUS_MOON_COVERAGE}
     * 
     * @description check the default construction of {@link AttractionModel} attached to
     * a celestial body. 
     */
    @Test
    public void testAttractionModel(){
        // test the default attraction model instance
        final double mu = 4.902777900000E12;
        final AttractionModel model = this.moon.getAttractionModel();
        Assert.assertEquals(mu, this.moon.getGM(), 0);
        Assert.assertNotNull(model.getClass().equals(PointAttractionModel.class));
        
        // set a new AttractionModel
        final AttractionModel newModel = new NewtonianAttraction(2 * mu);
        this.moon.setAttractionModel(newModel);
        Assert.assertEquals(2 * mu, this.moon.getGM(), 0.);
        
        // set a new central attraction coefficient, the attraction model must me updated
        this.moon.setGM(1.5 * mu);
        Assert.assertEquals(1.5 * mu, this.moon.getAttractionModel().getMu(), 0.);
    }
}
