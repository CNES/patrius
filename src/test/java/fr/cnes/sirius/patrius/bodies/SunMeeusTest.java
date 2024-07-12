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
 * @history creation 01/08/2012
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2257:27/05/2020:Le modèle Meeus bord semble faux 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:591:05/04/2016:add IAU pole data
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.forces.gravity.AbstractGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the coverage of the class MeeusSun.
 *
 * @author Julie Anton
 *
 * @version $Id: SunMeeusTest.java 17910 2017-09-11 11:58:16Z bignon $
 *
 * @since 1.2
 *
 */
public class SunMeeusTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Meeus Sun test class for coverage purposes
         *
         * @featureDescription perform simple tests for coverage, the validation tests are located in pbase-tools.
         *
         * @coveredRequirements DV-MOD_470
         */
        MEEUS_SUN_COVERAGE
    }

    /** Sun */
    private CelestialBody sun;

    /** Sun's radius */
    private double radius;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SunMeeusTest.class.getSimpleName(), "Sun Meeus model");
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MEEUS_SUN_COVERAGE}
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
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testTransformation() throws PatriusException {
        Assert.assertNotNull(sun.getInertialFrame(IAUPoleModelType.CONSTANT).getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));
        Assert.assertNotNull(sun.getRotatingFrame(IAUPoleModelType.TRUE).getTransformTo(FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH));
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MEEUS_SUN_COVERAGE}
     *
     * @testedMethod {@link MeeusSun#getPVCoordinates(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     *
     * @description test on one point the results of Meeus algorithm with respect to DE405 ephemerides (JPL)
     *
     * @input DE405 ephemerides
     *
     * @output position of the Sun
     *
     * @testPassCriteria the difference in position should be lower than 25593km, the angular difference between both
     *                   results should be lower 34.6''
     *
     * @comments the thresholds are taken from the following document : Modèles d'éphémérides luni-solaires, DCT/SB/MS,
     *           14/03/2011.
     *
     * @referenceVersion 1.2
     *
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetPVCoordinates() throws PatriusException {

        Report.printMethodHeader("testGetPVCoordinates", "PV coordinates computation", "DE405 ephemeris", 25.593e6,
            ComparisonType.ABSOLUTE);
        // start date
        final AbsoluteDate date = new AbsoluteDate(2012, 8, 1, TimeScalesFactory.getTT());
        final Frame gcrf = FramesFactory.getGCRF();

        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SUN);

        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SUN, loader);

        final CelestialBody sunDE = CelestialBodyFactory.getSun();

        final Vector3D pos1 = sun.getPVCoordinates(date, gcrf).getPosition();

        final Vector3D pos2 = sunDE.getPVCoordinates(date, gcrf).getPosition();

        // difference of the distance from Earth to Sun between DE405 and Meeus results
        Assert.assertTrue(Vector3D.distance(pos1, pos2) < 25.593e6);

        Report.printToReport("Position", pos2, pos1);

        // angular distance between DE405 and Meeus results
        Assert.assertTrue(Vector3D.angle(pos1, pos2) < 34.6 * Constants.ARC_SECONDS_TO_RADIANS);

        // Test GetNativeFrame
        Assert.assertEquals(sunDE.getICRF(), sunDE.getNativeFrame(null, null));
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MEEUS_SUN_COVERAGE}
     *
     * @testedMethod {@link MeeusSun#getPVCoordinates(AbsoluteDate, fr.cnes.sirius.patrius.frames.Frame)}
     *
     * @description test Meeus Board model (non-regression test)
     *
     * @input Sun
     *
     * @output position of the Sun
     *
     * @testPassCriteria position is as expected (reference: PATRIUS v3.3, threshold: 0)
     *
     * @referenceVersion 3.3
     *
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testBoardModel() throws PatriusException {

        Report.printMethodHeader("testBoardModel", "Board model PV coordinates computation", "PATRIUS v4.5", 0.,
            ComparisonType.ABSOLUTE);
        final MeeusSun sun = new MeeusSun(MODEL.BOARD);
        final AbsoluteDate date = new AbsoluteDate(2012, 8, 1, TimeScalesFactory.getTT());
        final Vector3D actual = sun.getPVCoordinates(date, FramesFactory.getGCRF()).getPosition().normalize();
        final Vector3D expected = new Vector3D(-0.6291078963967323, 0.7131841552873542, 0.3091789374105901);

        Report.printToReport("Position", expected, actual);
        Assert.assertEquals(expected.getX(), actual.getX(), 0.);
        Assert.assertEquals(expected.getY(), actual.getY(), 0.);
        Assert.assertEquals(expected.getZ(), actual.getZ(), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MEEUS_MOON_COVERAGE}
     *
     * @description check that the default {@link BodyShape} is correctly built.
     *
     * @input None
     *
     * @output {@link BodyShape}
     */
    @Test
    public void testGetShape() throws PatriusException {

    	// arbitrary date
    	final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    	final Frame bodyRotatingFrame = sun.getRotatingFrame(IAUPoleModelType.TRUE);
        final Frame bodyInertialFrame = sun.getInertialFrame(IAUPoleModelType.CONSTANT);

    	final BodyShape actualShape = sun.getShape();
    	final BodyShape expectedShape = new OneAxisEllipsoid(radius, 0, bodyRotatingFrame, "Sun");

    	// check the body names are the same
    	Assert.assertEquals(expectedShape.getName(), actualShape.getName());

    	double actual;
    	double expected;

    	// getIntersection method
    	final Line line1 = new Line(new Vector3D(4 * radius, 0, 0), new Vector3D(0, 3 * radius, 0));
    	actual = actualShape.distanceTo(line1, bodyInertialFrame, date);
    	expected = expectedShape.distanceTo(line1, bodyInertialFrame, date);
    	Assert.assertEquals(expected, actual, 0.);

        // getApparentRadius method
        final Vector3D posInFrame = new Vector3D(2.5 * radius, 0, 1.5 * radius);
        final PVCoordinates pv = new PVCoordinates(posInFrame, Vector3D.ZERO);
        final PVCoordinatesProvider provider = new ConstantPVCoordinatesProvider(pv, bodyInertialFrame);
        actual = actualShape.getApparentRadius(new ConstantPVCoordinatesProvider(posInFrame.scalarMultiply(5), bodyInertialFrame), date, provider, PropagationDelayType.INSTANTANEOUS);
        expected = expectedShape.getApparentRadius(new ConstantPVCoordinatesProvider(posInFrame.scalarMultiply(5), bodyInertialFrame), date, provider, PropagationDelayType.INSTANTANEOUS);
        Assert.assertEquals(expected, actual, 0.);

        // getIntersections method
        final Line line2 = new Line(new Vector3D(0.9 * radius,  0., 0.), Vector3D.ZERO);
        final Vector3D[] intersections = actualShape.getIntersectionPoints(line2, bodyInertialFrame, date);
        final Vector3D[] expectedInter = expectedShape.getIntersectionPoints(line2, bodyInertialFrame, date);
        Assert.assertEquals(intersections.length, expectedInter.length);
        Assert.assertEquals(intersections[0], expectedInter[0]);
        Assert.assertEquals(intersections[intersections.length-1], expectedInter[expectedInter.length -1]);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MEEUS_SUN_COVERAGE}
     *
     * @description check that the setter method of {@link CelestialBody} is correct.
     *
     * @input {@link BodyShape}
     *
     * @output {@link BodyShape}
     */
    @Test
    public void testSetShape() throws PatriusException{

    	// arbitrary date
    	final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    	// inertial and rotating frame
    	final Frame bodyRotatingFrame = sun.getRotatingFrame(IAUPoleModelType.TRUE);
    	final Frame bodyInertialFrame = sun.getInertialFrame(IAUPoleModelType.CONSTANT);

    	// the default shape
    	final CelestialBody moon = sun;
    	final BodyShape defaultShape = moon.getShape();

    	// the new body shape
    	final BodyShape newShape = new OneAxisEllipsoid(radius * 1.05, 0.01, bodyRotatingFrame, "sun modified");

    	moon.setShape(newShape);

    	// check the body name is modified
    	Assert.assertEquals(moon.getShape().getName(), newShape.getName());
    	// getIntersection method
    	final Line line1 = new Line(new Vector3D(4 * radius, 0, 0), new Vector3D(0, 3 * radius, 0));
    	final double oldDistance = defaultShape.distanceTo(line1, bodyInertialFrame, date);
    	final double newDistance = moon.getShape().distanceTo(line1, bodyInertialFrame, date);
    	Assert.assertTrue(newDistance < oldDistance);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#MEEUS_SUN_COVERAGE}
     *
     * @description check the default construction of {@link AbstractGravityModel} attached to
     *              a celestial body.
     */
    @Test
    public void testAttractionModel(){
        // test the default attraction model instance
        final double mu = Constants.JPL_SSD_SUN_GM;
        final GravityModel model = sun.getGravityModel();
        Assert.assertEquals(mu, sun.getGM(), 0);
        Assert.assertNotNull(model.getClass().equals(NewtonianGravityModel.class));

        // set a new AttractionModel
        final AbstractGravityModel newModel = new NewtonianGravityModel(2 * mu);
        sun.setGravityModel(newModel);
        Assert.assertEquals(2 * mu, sun.getGM(), 0.);

        // set a new central attraction coefficient, the attraction model must me updated
        sun.setGM(1.5 * mu);
        Assert.assertEquals(1.5 * mu, sun.getGravityModel().getMu(), 0.);
    }

    /**
     * Set up
     *
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        sun = new MeeusSun();
        radius = 696000000.;
    }
}
