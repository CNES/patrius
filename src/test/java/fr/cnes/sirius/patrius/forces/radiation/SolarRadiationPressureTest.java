/**
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
 * VERSION:4.11:DM:DM-3306:22/05/2023:[PATRIUS] Rayon du soleil dans le calcul de la PRS
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2899:15/11/2021:[PATRIUS] Autres corps occultants que la Terre pour la SRP 
 * VERSION:4.8:DM:DM-2900:15/11/2021:[PATRIUS] Possibilite de desactiver les eclipses pour la SRP 
 * VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * VERSION:4.7:FA:FA-2897:18/05/2021:Alignement Soleil-Sat-Terre non supporté pour la SRP 
 * VERSION:4.3:FA:FA-2096:15/05/2019:[PATRIUS] Attribut coefficient multiplicatif non mis a jour dans SolarRadiationPressure
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:01/04/2014:change partial derivatives API
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:412:05/05/2015:Deleted massParam in DirectRadiativeModel
 * VERSION::FA:439:12/06/2015:Corrected partial derivatives computation for srp
 * VERSION::DM:457:09/11/2015: Move extendedOneAxisEllipsoid from patrius to orekit addons
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:1279:15/11/2017:add getSolarFlux() method
 * VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
 * VERSION::FA:1448:20/04/2018:PATRIUS 4.0 minor corrections
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.models.utils.AssemblySphericalSpacecraft;
import fr.cnes.sirius.patrius.assembly.models.utils.PatriusSphericalSpacecraft;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link SolarRadiationPressure}
 *
 * @author Rami Houdroge
 *
 * @version $Id: PatriusSolarRadiationPressureTest.java 18109 2017-10-04
 *          06:48:22Z bignon $
 *
 * @since 1.2
 *
 */
public class SolarRadiationPressureTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Tests for SRP with ellipsoid
         *
         * @featureDescription tests the correctness of the srp and body shape
         *                     algorithms
         *
         * @coveredRequirements DV-MOD_310
         */
        SRP_WITH_ELLIPSOID,

        /**
         * @featureTitle Radiative model.
         *
         * @featureDescription Computation of the radiation pressure
         *                     acceleration and partial derivatives.
         *
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_430,
         *                      DV-VEHICULE_431, DV-MOD_280
         */
        RADIATIVE_MODEL
    }

    /** ref pressure */
    private final double pRef = 4.56e-6;
    /** ref distance */
    private final double dRef = 149597870000.0;
    /** CIRF2000 */
    private Frame cirf;
    /** date */
    private AbsoluteDate date;
    /** pos */
    private Vector3D pos;
    /** vel */
    private Vector3D vel;
    /** mu */
    private final double mu = 3.9860043770442000E+14;
    /** sun */
    private CelestialBody sun;
    /** Earth */
    private EllipsoidBodyShape earth;
    /** eps */
    private final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
    /** eps for finite differences */
    private final double epsFD = 1E-10;
    /** The orbit. */
    private Orbit orbit;
    /** Default name for SimpleMassModel. */
    private static final String DEFAULT = "Default";
    /** Sun radius constant before 4.11 */
    private final double sunRadius = 6.95E8;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SolarRadiationPressureTest.class.getSimpleName(),
                "Patrius solar radiation pressure force");
    }

    /**
     * Creates a new Assembly with only a facet as main.
     *
     * @param mass
     *        the mass of the spacecraft.
     * @param ka
     *        the absorption coefficient.
     * @param ks
     *        the specular coefficient.
     * @param kd
     *        the diffusion coefficient.
     * @return an assembly
     * @throws PatriusException
     */
    private Assembly
            createAssemblySphereAndFacets(final double mass, final double ka, final double ks, final double kd)
                    throws PatriusException {
        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        final String mainPart = "Satellite";
        final String array1 = "array1";
        final String array2 = "array2";
        builder.addMainPart(mainPart);
        builder.addPart(array1, mainPart, new Transform(this.date, new Rotation(Vector3D.PLUS_I, 0.25)));
        builder.addPart(array2, mainPart, new Transform(this.date, new Rotation(Vector3D.PLUS_K, 0.18)));

        builder.addProperty(new RadiativeProperty(ka, ks, kd), mainPart);
        builder.addProperty(new RadiativeProperty(ka, ks, kd), array1);
        builder.addProperty(new RadiativeProperty(ka, ks, kd), array2);
        builder.addProperty(new MassProperty(mass / 3.), mainPart);
        builder.addProperty(new MassProperty(mass / 3.), array1);
        builder.addProperty(new MassProperty(mass / 3.), array2);
        builder.addProperty(new RadiativeSphereProperty(1), mainPart);
        final Facet faceArray1 = new Facet(Vector3D.MINUS_K, 2.);
        builder.addProperty(new RadiativeFacetProperty(faceArray1), array1);
        final Facet faceArray2 = new Facet(Vector3D.PLUS_K, 3.);
        builder.addProperty(new RadiativeFacetProperty(faceArray2), array2);

        // Assembly creation:
        return builder.returnAssembly();
    }

    /**
     * Creates a new Assembly with only a facet as main.
     *
     * @param mass
     *        the mass of the spacecraft.
     * @param kaMain
     *        the absorption coefficient parameter for main part.
     * @param ksMain
     *        the specular coefficient parameter for main part.
     * @param kdMain
     *        the diffusion coefficient parameter for main part.
     * @param ka1
     *        the absorption coefficient parameter for array 1.
     * @param ks1
     *        the specular coefficient parameter for array 1.
     * @param kd1
     *        the diffusion coefficient parameter for array 1.
     * @param ka2
     *        the absorption coefficient parameter for array 2.
     * @param ks2
     *        the specular coefficient parameter for array 2.
     * @param kd2
     *        the diffusion coefficient parameter for array 2.
     * @return an assembly
     * @throws PatriusException
     */
    private Assembly createAssemblySphereAndFacets(final Parameter massMain, final Parameter mass1,
            final Parameter mass2, final Parameter kaMain, final Parameter ksMain, final Parameter kdMain,
            final Parameter ka1, final Parameter ks1, final Parameter kd1, final Parameter ka2, final Parameter ks2,
            final Parameter kd2) throws PatriusException {
        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        final String mainPart = "Satellite";
        final String array1 = "array1";
        final String array2 = "array2";
        builder.addMainPart(mainPart);
        builder.addPart(array1, mainPart, new Transform(this.date, new Rotation(Vector3D.PLUS_I, 0.25)));
        builder.addPart(array2, mainPart, new Transform(this.date, new Rotation(Vector3D.PLUS_K, 0.18)));

        builder.addProperty(new RadiativeProperty(kaMain, ksMain, kdMain), mainPart);
        builder.addProperty(new RadiativeProperty(ka1, ks1, kd1), array1);
        builder.addProperty(new RadiativeProperty(ka2, ks2, kd2), array2);
        builder.addProperty(new MassProperty(massMain), mainPart);
        builder.addProperty(new MassProperty(mass1), array1);
        builder.addProperty(new MassProperty(mass2), array2);
        builder.addProperty(new RadiativeSphereProperty(1), mainPart);
        final Facet faceArray1 = new Facet(Vector3D.MINUS_K, 2.);
        builder.addProperty(new RadiativeFacetProperty(faceArray1), array1);
        final Facet faceArray2 = new Facet(Vector3D.PLUS_K, 3.);
        builder.addProperty(new RadiativeFacetProperty(faceArray2), array2);

        // Assembly creation:
        return builder.returnAssembly();
    }

    /**
     * Test SRP with occulting body different from Earth (here Moon). Two cases are tested:
     * <ul>
     * <li>Occulting body = Moon, state centered on Moon, state hidden behind Moon</li>
     * <li>Occulting body = Moon, state centered on Earth, state hidden behind Moon</li>
     * </ul>
     */
    @Test
    public void testOtherOccultingBody() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // Initialization with occulting body = Moon
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final MassProvider massModel = new SimpleMassModel(1500., DEFAULT);
        final BodyShape moonModel = new OneAxisEllipsoid(1500000, 0, moon.getRotatingFrame(IAUPoleModelType.TRUE), "Moon");

        /*
         * CIRCULAR
         */
        SolarRadiationPressure srp = new SolarRadiationPressure(sun, 1500000,
                moon.getInertialFrame(IAUPoleModelType.CONSTANT),
                new SphericalSpacecraft(50.0, 0.5, 1, 0., 0., DEFAULT), false);

        // Case 1: occulting body = Moon, state centered on Moon, state hidden behind Moon: acc = 0
        Vector3D pos1 = sun
                .getPVCoordinates(AbsoluteDate.J2000_EPOCH, moon.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition().scalarMultiply(-0.001);
        SpacecraftState state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos1, Vector3D.ZERO),
                moon.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        Vector3D actualAcc = srp.computeAcceleration(state1);
        Assert.assertEquals(0, actualAcc.getNorm(), 0.);

        // Case 2: occulting body = Moon, state centered on Earth, state hidden behind Moon: acc = 0
        Vector3D pos2 = moon.getInertialFrame(IAUPoleModelType.CONSTANT)
                .getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH).transformPosition(pos1);
        SpacecraftState state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos2, Vector3D.ZERO),
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        Vector3D actualAcc2 = srp.computeAcceleration(state2);
        Assert.assertEquals(0, actualAcc2.getNorm(), 0.);

        /*
         * ELLIPSOID
         */
        srp = new SolarRadiationPressure(sun, moonModel, new SphericalSpacecraft(50.0, 0.5, 1, 0., 0., DEFAULT));

        // Case 1: occulting body = Moon, state centered on Moon, state hidden behind Moon: acc = 0
        pos1 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, moon.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition()
                .scalarMultiply(-0.001);
        state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos1, Vector3D.ZERO),
                moon.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        actualAcc = srp.computeAcceleration(state1);
        Assert.assertEquals(0, actualAcc.getNorm(), 0.);

        // Case 2: occulting body = Moon, state centered on Earth, state hidden behind Moon: acc = 0
        pos2 = moon.getInertialFrame(IAUPoleModelType.CONSTANT)
                .getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH)
                .transformPosition(pos1);
        state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos2, Vector3D.ZERO),
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        actualAcc2 = srp.computeAcceleration(state2);
        Assert.assertEquals(0, actualAcc2.getNorm(), 0.);
    }

    /**
     * Test SRP with multiple occulting bodies. 4 cases are tested:
     * <ul>
     * <li>Satellite behind first body</li>
     * <li>Satellite behind second body</li>
     * <li>Satellite not behind any body</li>
     * <li>Satellite behind first and second body</li>
     * </ul>
     */
    @Test
    public void testMultipleOccultingBodies() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // Case 1, 2, 3: two separate occulting bodies, 3 configuration:
        // - Satellite behind first body
        // - Satellite behind second body
        // - Satellite not behind any body
        // Initialization with occulting body = Moon
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody body1 = CelestialBodyFactory.getEarth();
        final CelestialBody body2 = CelestialBodyFactory.getMoon();
        final BodyShape body1Model = new OneAxisEllipsoid(6378000, 0, FramesFactory.getGCRF(), "Body1");
        final MassProvider massModel = new SimpleMassModel(1500., DEFAULT);

        /*
         * CIRCULAR
         */
        SolarRadiationPressure srp = new SolarRadiationPressure(sun, 6378000, new SphericalSpacecraft(50.0, 0.5, 1, 0.,
                0., DEFAULT));
        srp.addOccultingBody(new OneAxisEllipsoid(1500000, 0, body2.getInertialFrame(IAUPoleModelType.CONSTANT),
                "Body2"));

        // Case 1: state behind first body: acc = 0
        Vector3D pos1 = sun
                .getPVCoordinates(AbsoluteDate.J2000_EPOCH, body1.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition().scalarMultiply(-0.001);
        SpacecraftState state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos1, Vector3D.ZERO),
                body1.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        Vector3D actualAcc = srp.computeAcceleration(state1);
        Assert.assertEquals(0, actualAcc.getNorm(), 0.);

        // Case 2: state behind second body: acc = 0
        Vector3D pos2 = sun
                .getPVCoordinates(AbsoluteDate.J2000_EPOCH, body2.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition().scalarMultiply(-0.001);
        SpacecraftState state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos2, Vector3D.ZERO),
                body2.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        Vector3D actualAcc2 = srp.computeAcceleration(state2);
        Assert.assertEquals(0, actualAcc2.getNorm(), 0.);

        // Case 3: state not behind any body: acc != 0
        Vector3D pos3 = Vector3D.PLUS_I.scalarMultiply(1E8);
        SpacecraftState state3 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos3, Vector3D.ZERO),
                body1.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        Vector3D actualAcc3 = srp.computeAcceleration(state3);
        Assert.assertFalse(actualAcc3.getNorm() == 0);

        // Case 4: state behind two bodies (same body): acc = 0
        CelestialBody body3 = CelestialBodyFactory.getEarth();
        SolarRadiationPressure srp2 = new SolarRadiationPressure(sun, 6378000, new SphericalSpacecraft(50.0, 0.5, 1,
                0., 0., DEFAULT));
        srp.addOccultingBody(new OneAxisEllipsoid(6378000, 0, body3.getInertialFrame(IAUPoleModelType.CONSTANT),
                "Body3"));

        Vector3D pos4 = sun
                .getPVCoordinates(AbsoluteDate.J2000_EPOCH, body1.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition().scalarMultiply(-0.001);
        SpacecraftState state4 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos4, Vector3D.ZERO),
                body1.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        Vector3D actualAcc4 = srp2.computeAcceleration(state4);
        Assert.assertEquals(0, actualAcc4.getNorm(), 0.);

        /*
         * ELLIPSOID
         */
        srp = new SolarRadiationPressure(sun, body1Model, new SphericalSpacecraft(50.0, 0.5, 1, 0., 0., DEFAULT));
        srp.addOccultingBody(new OneAxisEllipsoid(1500000, 0, body2.getInertialFrame(IAUPoleModelType.CONSTANT),
                "Body2"));

        // Case 1: state behind first body: acc = 0
        pos1 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, body1.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition()
                .scalarMultiply(-0.001);
        state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos1, Vector3D.ZERO),
                body1.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        actualAcc = srp.computeAcceleration(state1);
        Assert.assertEquals(0, actualAcc.getNorm(), 0.);

        // Case 2: state behind second body: acc = 0
        pos2 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, body2.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition()
                .scalarMultiply(-0.001);
        state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos2, Vector3D.ZERO),
                body2.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        actualAcc2 = srp.computeAcceleration(state2);
        Assert.assertEquals(0, actualAcc2.getNorm(), 0.);

        // Case 3: state not behind any body: acc != 0
        pos3 = Vector3D.PLUS_I.scalarMultiply(1E8);
        state3 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos3, Vector3D.ZERO),
                body1.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        actualAcc3 = srp.computeAcceleration(state3);
        Assert.assertFalse(actualAcc3.getNorm() == 0);

        // Case 4: state behind two bodies (same body): acc = 0
        body3 = CelestialBodyFactory.getEarth();
        srp2 = new SolarRadiationPressure(sun, body1Model, new SphericalSpacecraft(50.0, 0.5, 1, 0., 0., DEFAULT));
        srp2.addOccultingBody(new OneAxisEllipsoid(6378000, 0, body3.getInertialFrame(IAUPoleModelType.CONSTANT),
                "Body2"));

        pos4 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, body1.getInertialFrame(IAUPoleModelType.CONSTANT))
                .getPosition()
                .scalarMultiply(-0.001);
        state4 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos4, Vector3D.ZERO),
                body1.getInertialFrame(IAUPoleModelType.CONSTANT), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU),
                massModel);
        actualAcc4 = srp2.computeAcceleration(state4);
        Assert.assertEquals(0, actualAcc4.getNorm(), 0.);
    }

    /**
     * Test heliocentric propagation with SRP (orbit around the sun).
     */
    @Test
    public void testHeliocentric() throws PatriusException {
        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody body2 = CelestialBodyFactory.getMoon();
        final SolarRadiationPressure srp = new SolarRadiationPressure(Constants.SEIDELMANN_UA,
                Constants.CONST_SOL_N_M2, sun, sunRadius, 6378000, new SphericalSpacecraft(50.0, 0.5, 1, 0., 0.,
                        DEFAULT));
        srp.addOccultingBody(new OneAxisEllipsoid(1500000, 0, body2.getInertialFrame(IAUPoleModelType.CONSTANT),
                "Body2"));
        final MassProvider massModel = new SimpleMassModel(1500., DEFAULT);


        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(150E9, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                sun.getInertialFrame(IAUPoleModelType.CONSTANT), initialDate,
                Constants.IERS92_SUN_GRAVITATIONAL_PARAMETER);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.),
            initialState.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator.setInitialState(initialState);
        propagator.addForceModel(srp);
        propagator.setMassProviderEquation(massModel);

        // Propagation and check result is not null
        final SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(3600.));
        Assert.assertNotNull(finalState);
    }

    @Test
    public void testLightning() throws PatriusException {
        // Initialization

        final SolarRadiationPressure srp = new SolarRadiationPressure(this.sun, this.earth.getEquatorialRadius(),
                new SphericalSpacecraft(50.0, 0.5, 0.5, 0.5, 0., DEFAULT));
        final SolarRadiationPressure srp2 = new SolarRadiationPressure(this.sun, this.earth.getEquatorialRadius(),
                new SphericalSpacecraft(50.0, 0.5, 0.5, 0.5, 0., DEFAULT));
        srp2.setEclipsesComputation(false);
        Assert.assertFalse(srp2.isEclipseComputation());
        Assert.assertEquals(1, srp.getParameters().size());

        final double period = 2 * FastMath.PI
                * MathLib.sqrt(this.orbit.getA() * this.orbit.getA() * this.orbit.getA() / this.orbit.getMu());
        Assert.assertEquals(86164, period, 1);

        // creation of the propagator
        final KeplerianPropagator k = new KeplerianPropagator(this.orbit);

        // intermediate variables
        AbsoluteDate currentDate;
        double changed = 1;
        int count = 0;

        for (int t = 1; t < 3 * period; t += 1000) {
            currentDate = this.date.shiftedBy(t);
            try {

                final double ratio = srp.getLightningRatio(k.propagate(currentDate).getPVCoordinates().getPosition(),
                        this.earth,
                        new ConstantPVCoordinatesProvider(this.sun.getPVCoordinates(this.date, FramesFactory.getGCRF())
                                .getPosition().scalarMultiply(-1), FramesFactory.getGCRF()), FramesFactory.getGCRF(),
                        this.date);
                final double ratio2 = srp2.getLightningRatio(k.propagate(currentDate).getPVCoordinates().getPosition(),
                        this.earth,
                        new ConstantPVCoordinatesProvider(this.sun.getPVCoordinates(this.date, FramesFactory.getGCRF())
                                .getPosition().scalarMultiply(-1), FramesFactory.getGCRF()), FramesFactory.getGCRF(),
                        this.date);

                if (MathLib.floor(ratio) != changed) {
                    changed = MathLib.floor(ratio);
                    if (changed == 0) {
                        count++;
                    }
                }

                // When eclipses are disabled, lightning ratio should always be one
                Assert.assertEquals(1., ratio2, 0.);
            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        }
        Assert.assertTrue(4 == count);
    }

    /**
     * Check that lightning ratio in case of Earth exactly between Sun and satellite returns 0.
     */
    @Test
    public void testLightningAlignment() throws PatriusException {

        /*
         * CIRCULAR
         */
        // Initialization
        SolarRadiationPressure srp = new SolarRadiationPressure(this.sun, this.earth.getEquatorialRadius(),
                new SphericalSpacecraft(50.0, 0.5, 0.5, 0.5, 0., DEFAULT));

        // Define aligned spacecraft (Earth exactly between Sun and satellite)
        final Vector3D satSunVector = this.sun.getPVCoordinates(this.date, FramesFactory.getGCRF()).getPosition()
                .scalarMultiply(-1);

        // Check lightning ratio is equal to 0
        Assert.assertEquals(0,
                srp.getLightningRatio(satSunVector, this.earth, this.sun, FramesFactory.getGCRF(), this.date), 0.);

        /*
         * ELLIPSOID
         */
        // Define aligned spacecraft (Earth exactly between Sun and satellite)
        srp = new SolarRadiationPressure(this.sun, this.earth, null);
        // Check lightning ratio is equal to 0
        Assert.assertEquals(0,
                srp.getLightningRatio(satSunVector, this.earth, this.sun, FramesFactory.getGCRF(), this.date), 0.);

        // Check lightning ratio is equal to 1 because eclipses are not computed
        srp.setEclipsesComputation(false);
        Assert.assertEquals(1.,
                srp.getLightningRatio(satSunVector, this.earth, this.sun, FramesFactory.getGCRF(), this.date), 0.);
        Assert.assertFalse(srp.isEclipseComputation());
    }

    // Test exception thrown in case of unsupported parameter (Kref)
    // test the addDAccDParam method of SolarRadiationPressure throws an exception.
    @Test(expected = PatriusException.class)
    public void testException() throws PatriusException {
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(10.0, 1.7, 0, 0, 0, DEFAULT);
        final Parameter kRef = new Parameter("toto", 1.);
        final SolarRadiationPressure srp = new SolarRadiationPressure(kRef, this.sun, this.earth.getEquatorialRadius(),
                spacecraft);
        srp.addDAccDParam(null, kRef, new double[] {});
        Assert.assertFalse(srp.supportsJacobianParameter(kRef));
    }

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     *
     * @testType UT
     *
     * @testedFeature {@link features#RADIATIVE_MODEL}
     *
     * @testedMethod {@link DirectRadiativeModel#getParameters()}
     * @testedMethod {@link SolarRadiationPressure#getParameters()}
     *
     * @description Test for the parameters
     *
     * @input a parameter
     *
     * @output its value
     *
     * @testPassCriteria the parameter value is as expected exactly (0 ulp
     *                   difference)
     *
     * @referenceVersion 2.2
     *
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testParamListSRP() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");

        // create the assembly
        final Assembly assembly = this.createAssemblySphereAndFacets(1000, 1, 1, 1);

        // create an AeroModel for the assembly
        final DirectRadiativeModel model = new DirectRadiativeModel(assembly);
        Assert.assertEquals(10, model.getParameters().size());
        final SolarRadiationPressure srp = new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                this.earth.getConjugateRadius(), model);

        double k = 5;
        for (final Parameter p : srp.getParameters()) {
            p.setValue(k);
            Assert.assertTrue(Precision.equals(k, p.getValue(), 0));
            k++;
        }
        Assert.assertEquals(11, srp.getParameters().size());
    }

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     *
     * @testType UT
     *
     * @testedFeature {@link features#RADIATIVE_MODEL}
     *
     * @testedMethod {@link DirectRadiativeModel#getParametersNames()}
     * @testedMethod {@link DirectRadiativeModel#getParameter(String)}
     * @testedMethod {@link DirectRadiativeModel#setParameter()}
     * @testedMethod {@link SolarRadiationPressure#getParametersNames()}
     * @testedMethod {@link SolarRadiationPressure#getParameter(String)}
     * @testedMethod {@link SolarRadiationPressure#setParameter()}
     *
     * @description Test for the parameters
     *
     * @input a parameter
     *
     * @output its value
     *
     * @testPassCriteria the parameter value is as expected exactly (0 ulp
     *                   difference)
     *
     * @referenceVersion 2.2
     *
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testParamList() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");

        // create the assembly
        final Assembly assembly = this.createAssemblySphereAndFacets(1000, 1, 1, 1);

        // create an AeroModel for the assembly
        final DirectRadiativeModel model = new DirectRadiativeModel(assembly);
        Assert.assertEquals(10, model.getParameters().size());
        final SolarRadiationPressure srp = new SolarRadiationPressure(CelestialBodyFactory.getSun(), this.earth, model);

        // final SpacecraftState s = new SpacecraftState(new
        // KeplerianOrbit(300000, 0, 0, 0, 0, 0, PositionAngle.MEAN,
        // itrf, AbsoluteDate.J2000_EPOCH, Constants.GRIM5C1_EARTH_MU),
        // new Attitude(AbsoluteDate.J2000_EPOCH, itrf, Rotation.IDENTITY,
        // Vector3D.ZERO));
        // srp.computeAcceleration(s);
        // srp.computeAcceleration(s);

        double k = 5;
        for (final Parameter p : srp.getParameters()) {
            p.setValue(k);
            Assert.assertTrue(Precision.equals(k, p.getValue(), 0));
            k++;
        }
        Assert.assertEquals(11, srp.getParameters().size());

        Assert.assertFalse(srp.supportsJacobianParameter(new Parameter("toto", 0.)));
    }

    /**
     * @throws PatriusException
     *         if ephemeris fail
     * @testType UT
     *
     * @testedFeature {@link features#SRP_WITH_ELLIPSOID}
     *
     * @testedMethod {@link SolarRadiationPressure#computeAcceleration(PVCoordinates, Frame, AbsoluteDate, double)}
     * @testedMethod {@link SolarRadiationPressure#computeAcceleration(SpacecraftState)}
     *
     * @description test acceleration computation
     *
     * @input PVCoordinates, frame, date and mass
     *
     * @output acceleration as vector3D
     *
     * @testPassCriteria acceleration is the same as the expected one, with a
     *                   1e-18 threshold (limited due to frame conversion to Earth-centered frame)
     *
     * @referenceVersion 4.8
     *
     * @nonRegressionVersion 4.8
     */
    @Test
    public void testSRP() throws PatriusException {

        Report.printMethodHeader("testSRP", "Acceleration computation", "Unknwon", this.eps, ComparisonType.RELATIVE);

        PVCoordinates pv;
        Vector3D expected;
        Vector3D result;
        final double mass = 4000;

        SpacecraftState scs;

        final AssemblyBuilder builder = new AssemblyBuilder();
        final String mainPart = "satellite";
        builder.addMainPart(mainPart);
        builder.addProperty(new RadiativeProperty(1, 0, 0), mainPart);
        builder.addProperty(new MassProperty(mass), mainPart);
        builder.addProperty(new RadiativeSphereProperty(1), mainPart);
        final Assembly assembly = builder.returnAssembly();
        final DirectRadiativeModel sc = new DirectRadiativeModel(assembly);
        final MassProvider massProvider = new MassModel(assembly);

        // SRP
        final SolarRadiationPressure srp = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius,
                this.earth, sc);

        this.pos = new Vector3D(-3051443.873181594, 0.08004112908428662, -6299896.119758646);
        this.vel = new Vector3D(6791.335823416453, 4.623958780615747E-5, -3289.4796714774893);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-0.0, -0.0, -0.0);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, this.eps);

        Report.printToReport("Acceleration (shadow)", expected, result);

        this.pos = new Vector3D(-3017443.03254199, 0.08027122004181056, -6316251.924802766);
        this.vel = new Vector3D(6808.967505417617, 4.579652856753137E-5, -3252.8265069236845);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-0.0, -0.0, -0.0);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, this.eps);

        this.pos = new Vector3D(-2983354.527822928, 0.08049909171346586, -6332424.227323494);
        this.vel = new Vector3D(6826.401370304048, 4.535187542297378E-5, -3216.0788398278287);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-0.0, -0.0, -0.0);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, this.eps);

        this.pos = new Vector3D(-2949179.349378623, 0.08072473615900333, -6348412.5574760055);
        this.vel = new Vector3D(6843.636911578195, 4.4905640058221286E-5, -3179.237737799262);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-4.051681754196597E-10, -1.9874333261941255E-13, -1.0093398114753024E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        Report.printToReport("Acceleration (light)", expected, result);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2914918.49008138, 0.08094814549656688, -6364216.450760323);
        this.vel = new Vector3D(6860.67362850433, 4.445783422729028E-5, -3142.304271161838);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-2.893565907654407E-9, -1.4220166119104913E-12, -7.222957840073693E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2880572.9452927453, 0.08116931190443193, -6379835.448034807);
        this.vel = new Vector3D(6877.511026124235, 4.4008470313641964E-5, -3105.279512922826);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-3.6103324586755545E-9, -1.7775876307772434E-12, -9.030350220816393E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2846143.712834581, 0.0813882276246434, -6395269.095529501);
        this.vel = new Vector3D(6894.148615271639, 4.355756136776451E-5, -3068.16453874174);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-3.610334007328543E-9, -1.7809110225098502E-12, -9.048499352435866E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2811631.792960075, 0.08160488496501007, -6410516.944859311);
        this.vel = new Vector3D(6910.5859125853185, 4.310512055443129E-5, -3030.960426899085);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-3.6103355599893424E-9, -1.7842343947634942E-12, -9.066603386144998E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2777038.1883246796, 0.0818192762987035, -6425578.553037031);
        this.vel = new Vector3D(6926.822440522512, 4.265116084505521E-5, -2993.6682582650337);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-3.610337116607478E-9, -1.787557800982937E-12, -9.084662446218315E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2742363.903956986, 0.08203139406334341, -6440453.482486216);
        this.vel = new Vector3D(6942.857727372894, 4.219569506360693E-5, -2956.2891162680216);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-3.610338677135245E-9, -1.7908812144162532E-12, -9.102676310547437E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2707609.9472295274, 0.0822412307604966, -6455141.301053894);
        this.vel = new Vector3D(6958.691307272457, 4.1738735976655487E-5, -2918.824086863274);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-3.6103402415248136E-9, -1.7942046083110903E-12, -9.120644758335267E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);

        this.pos = new Vector3D(-2672777.327829512, 0.08244877895554736, -6469641.582023118);
        this.vel = new Vector3D(6974.322720217183, 4.128029635339913E-5, -2881.274258501254);
        pv = new PVCoordinates(this.pos, this.vel);
        expected = new Vector3D(-3.610341809725377E-9, -1.7975280361112809E-12, -9.138567917791417E-13);
        scs = new SpacecraftState(new CartesianOrbit(pv, this.cirf, this.date, this.mu), massProvider);
        result = srp.computeAcceleration(scs);
        this.assertEqualsVector3D(expected, result, 1E-8);
    }

    /**
     * @throws PatriusException
     *         if ephemeris fail
     * @throws IOException
     *         if ephemeris fail
     * @testType UT
     *
     * @testedFeature {@link features#SRP_WITH_ELLIPSOID}
     *
     * @testedMethod {@link SolarRadiationPressure#getEventsDetectors()}
     * @testedMethod {@link SolarRadiationPressure#getParameters()}
     *
     * @description test acceleration computation
     *
     * @input PVCoordinates, frame, date and mass
     *
     * @output acceleration as vector3D
     *
     * @testPassCriteria acceleration is the same as the expected one, with a
     *                   1e-14 threshold
     *
     * @referenceVersion 1.2
     *
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testEvents() throws PatriusException, IOException {

        this.setupClassic();

        final AssemblySphericalSpacecraft sc = new AssemblySphericalSpacecraft(FastMath.PI, 2.2, 1., 0., 0., "Main");

        final SolarRadiationPressure srp = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius,
                this.earth, sc);

        Assert.assertEquals(5, sc.getJacobianParameters().size());
        Assert.assertEquals(6, srp.getParameters().size());

        EventDetector[] events = srp.getEventsDetectors();

        Assert.assertEquals(2, events.length);

        final Action ac1 = events[0].eventOccurred(null, true, true);
        final Action ac2 = events[1].eventOccurred(null, true, true);
        Assert.assertTrue(ac1.equals(Action.RESET_DERIVATIVES));
        Assert.assertTrue(ac2.equals(Action.RESET_DERIVATIVES));

        // Test the event detectors computation with no eclipse
        srp.setEclipsesComputation(false);
        events = srp.getEventsDetectors();

        Assert.assertEquals(0, events.length);
    }

    /**
     * @throws PatriusException
     *         if ephemeris fail
     * @testType UT
     *
     * @testedFeature {@link features#SRP_WITH_ELLIPSOID}
     * @testedFeature {@link features#RADIATIVE_MODEL}
     *
     * @testedMethod {@link SolarRadiationPressure#addDAccDParam(SpacecraftState, String, double[])}
     * @testedMethod {@link SolarRadiationPressure#computeAcceleration(SpacecraftState)}
     *
     * @description test the computation of partial derivatives with respect to
     *              : - thermo-optical parameters for a complex assembly. - K0
     *              coefficient - state
     *
     * @input SpacecraftState, arrays
     *
     * @output partial derivatives with respect to the thermo-optical
     *         parameters, k0 and the state
     *
     * @testPassCriteria the test is successful if - the computed partial
     *                   derivatives wrt thermo-optical coefficients and k0 are
     *                   equal to the analytical ones (1e-14 threshold on a
     *                   relative scale) and to the finite differences one
     *                   (1e-10 on a relative scale). - the computed partial
     *                   derivatives wrt state are zero
     *
     * @referenceVersion 1.3
     *
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testPDerivatives() throws PatriusException {
        // containers for partial derivatives
        double[] dAccdParam = new double[3];

        final double mass = 2000.;
        final MassProvider massProvider = new SimpleMassModel(mass, DEFAULT);
        // params and computation
        this.pos = new Vector3D(-1.5698127651877177E+06, -4.0571199222644530E+06, 5.1323187886121357E+06);
        this.vel = new Vector3D(2.6780951868939924E+03, -5.2461908201159367E+03, -4.9566405945152592E+03);
        final PVCoordinates pv = new PVCoordinates(this.pos, this.vel);
        final Orbit orbit = new CartesianOrbit(pv, this.cirf, this.date, this.mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
                orbit.getFrame());
        final SpacecraftState scs = new SpacecraftState(orbit, attitude, massProvider);

        final Parameter massMain = new Parameter("mass", mass / 3);
        final Parameter mass1 = new Parameter("mass", mass / 3);
        final Parameter mass2 = new Parameter("mass", mass / 3);
        final Parameter kaMain = new Parameter("ka", 0.6);
        final Parameter ksMain = new Parameter("ks", 0.6);
        final Parameter kdMain = new Parameter("kd", 0.6);
        final Parameter ka1 = new Parameter("ka", 0.6);
        final Parameter ks1 = new Parameter("ks", 0.6);
        final Parameter kd1 = new Parameter("kd", 0.6);
        final Parameter ka2 = new Parameter("ka", 0.6);
        final Parameter ks2 = new Parameter("ks", 0.6);
        final Parameter kd2 = new Parameter("kd", 0.6);

        final Assembly assembly = this.createAssemblySphereAndFacets(massMain, mass1, mass2, kaMain, ksMain, kdMain,
                ka1, ks1, kd1, ka2, ks2, kd2);
        assembly.initMainPartFrame(scs);
        final Parameter k0 = new Parameter("k0", 1.);
        final DirectRadiativeModel sc = new DirectRadiativeModel(assembly, k0);

        // Tests
        Assert.assertEquals(10, sc.getJacobianParameters().size());
        Assert.assertEquals(10, sc.getParameters().size());

        // coefficient difference:
        final double diff = .00001;

        Assembly assembly1 = this.createAssemblySphereAndFacets(mass, 0.6 - diff / 2., 0.1, 0.3);
        assembly1.initMainPartFrame(scs);
        DirectRadiativeModel sc1 = new DirectRadiativeModel(assembly1);

        // Assembly 2
        Assembly assembly2 = this.createAssemblySphereAndFacets(mass, 0.6 + diff / 2., 0.1, 0.3);
        assembly2.initMainPartFrame(scs);
        DirectRadiativeModel sc2 = new DirectRadiativeModel(assembly2);

        // SRP
        final Parameter refFlux = new Parameter("refFlux", this.pRef * this.dRef * this.dRef);
        SolarRadiationPressure srp = new SolarRadiationPressure(refFlux, this.sun, this.earth, sc);
        SolarRadiationPressure srp1 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth,
                sc1);
        SolarRadiationPressure srp2 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth,
                sc2);

        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), kaMain, dAccdParam);
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), ka1, dAccdParam);
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), ka2, dAccdParam);

        // finite differences:
        Vector3D acc1 = srp1.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly1)));
        Vector3D acc2 = srp2.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly2)));
        final double[] damFabs = { (acc2.getX() - acc1.getX()) / diff, (acc2.getY() - acc1.getY()) / diff,
                (acc2.getZ() - acc1.getZ()) / diff };
        // partial derivatives wrt absorption coefficient:
        Assert.assertArrayEquals(damFabs, dAccdParam, this.epsFD);

        // SPECULAR COEFFICIENT
        // Assembly 1
        assembly1 = this.createAssemblySphereAndFacets(mass, 0.6, 0.1 - diff / 2., 0.3);
        sc1 = new DirectRadiativeModel(assembly1);
        // Assembly 2
        assembly2 = this.createAssemblySphereAndFacets(mass, 0.6, 0.1 + diff / 2., 0.3);
        sc2 = new DirectRadiativeModel(assembly2);

        // SRP
        srp = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc);
        srp1 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc1);
        srp2 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc2);

        dAccdParam = new double[3];
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), ksMain, dAccdParam);
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), ks1, dAccdParam);
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), ks2, dAccdParam);

        assembly1.initMainPartFrame(new SpacecraftState(orbit, attitude, new MassModel(assembly1)));
        assembly2.initMainPartFrame(new SpacecraftState(orbit, attitude, new MassModel(assembly2)));
        // finite differences:
        acc1 = srp1.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly1)));
        acc2 = srp2.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly2)));
        final double[] damFspec = { (acc2.getX() - acc1.getX()) / diff, (acc2.getY() - acc1.getY()) / diff,
                (acc2.getZ() - acc1.getZ()) / diff };
        // partial derivatives wrt specular coefficient:
        Assert.assertArrayEquals(damFspec, dAccdParam, this.epsFD);

        // DIFFUSE COEFFICIENT
        // Assembly 1
        assembly1 = this.createAssemblySphereAndFacets(mass, 0.6, 0.1, 0.3 - diff / 2.);
        sc1 = new DirectRadiativeModel(assembly1);
        // Assembly 2
        assembly2 = this.createAssemblySphereAndFacets(mass, 0.6, 0.1, 0.3 + diff / 2.);
        sc2 = new DirectRadiativeModel(assembly2);

        // SRP
        srp = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc);
        srp1 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc1);
        srp2 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc2);

        dAccdParam = new double[3];
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), kdMain, dAccdParam);
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), kd1, dAccdParam);
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), kd2, dAccdParam);

        assembly1.initMainPartFrame(new SpacecraftState(orbit, attitude, new MassModel(assembly1)));
        assembly2.initMainPartFrame(new SpacecraftState(orbit, attitude, new MassModel(assembly2)));

        // finite differences:
        acc1 = srp1.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly1)));
        acc2 = srp2.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly2)));
        final double[] damFdiff = { (acc2.getX() - acc1.getX()) / diff, (acc2.getY() - acc1.getY()) / diff,
                (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt diffuse coefficient:
        Assert.assertArrayEquals(damFdiff, dAccdParam, 1E-8);

        // k0
        // Assembly 1
        assembly1 = this.createAssemblySphereAndFacets(mass, 0.6, 0.1, 0.3);
        final Parameter k01 = new Parameter("k01", 1. - diff / 2.);
        sc1 = new DirectRadiativeModel(assembly1, k01);
        // Assembly 2
        assembly2 = this.createAssemblySphereAndFacets(mass, 0.6, 0.1, 0.3);
        final Parameter k02 = new Parameter("k02", 1. + diff / 2.);
        sc2 = new DirectRadiativeModel(assembly2, k02);

        // SRP
        srp = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc);
        srp1 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc1);
        srp2 = new SolarRadiationPressure(this.dRef, this.pRef, this.sun, sunRadius, this.earth, sc2);

        dAccdParam = new double[3];
        // computeAcceleration should be called before calling addDAccDParam in
        // order to compute de partial derivatives.
        srp.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly)));
        srp.addDAccDParam(new SpacecraftState(orbit, attitude, new MassModel(assembly)), k0, dAccdParam);

        assembly1.initMainPartFrame(new SpacecraftState(orbit, attitude, new MassModel(assembly1)));
        assembly2.initMainPartFrame(new SpacecraftState(orbit, attitude, new MassModel(assembly2)));

        // finite differences:
        acc1 = srp1.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly)));
        acc2 = srp2.computeAcceleration(new SpacecraftState(orbit, attitude, new MassModel(assembly)));
        final double[] damFK0 = { (acc2.getX() - acc1.getX()) / diff, (acc2.getY() - acc1.getY()) / diff,
                (acc2.getZ() - acc1.getZ()) / diff };

        // partial derivatives wrt diffuse coefficient:
        Assert.assertArrayEquals(damFK0, dAccdParam, 1E-8);

        /*
         * Partial derivatives with respect to the state
         */
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        srp.addDAccDState(scs, dAccdPos, dAccdVel);
        for (int i = 0; i < dAccdPos.length; i++) {
            for (int j = 0; j < dAccdPos.length; j++) {
                Assert.assertEquals(0., dAccdPos[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
                Assert.assertEquals(0., dAccdVel[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
            }
        }
    }

    // test the DsrpAccDParam and DsrpAccDState methods.
    @Test
    public void testDsrpAccDerivatives() throws PatriusException {

        final String ABSORPTION_COEFFICIENT = "absorption coefficient";
        final String SPECULAR_COEFFICIENT = "specular reflection coefficient";
        final String DIFFUSION_COEFFICIENT = "diffusion reflection coefficient";
        final Parameter KA = new Parameter(ABSORPTION_COEFFICIENT, 0.);

        final ArrayList<Parameter> parameters = new ArrayList<>();
        parameters.add(KA);
        parameters.add(new Parameter(SPECULAR_COEFFICIENT, 0.));
        parameters.add(new Parameter(DIFFUSION_COEFFICIENT, 0.));

        // this class represents a mock RadiationSensitive object:
        class MockRadiationSensitive implements RadiationSensitive {
            /** Serializable UID. */
            private static final long serialVersionUID = 2636688453993455357L;

            @Override
            public Vector3D radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux)
                    throws PatriusException {
                return Vector3D.ZERO;
            }

            @Override
            public void addDSRPAccDParam(final SpacecraftState s, final Parameter param, final double[] dAccdParam,
                    final Vector3D satSunVector) throws PatriusException {
                dAccdParam[0] = 1.0;
                dAccdParam[1] = 2.0;
                dAccdParam[2] = 3.0;
            }

            @Override
            public void addDSRPAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                    final Vector3D satSunVector) throws PatriusException {
                dAccdPos[0][0] = 1.0;
                dAccdPos[0][1] = 0.1;
                dAccdVel[0][0] = 1.0;
                dAccdVel[1][1] = 1.1;
                dAccdVel[2][1] = 2.1;
            }

            /** {@inheritDoc} */
            @Override
            public ArrayList<Parameter> getJacobianParameters() {
                // return parameters
                return parameters;
            }
        }

        final MockRadiationSensitive spacecraft = new MockRadiationSensitive();
        final SolarRadiationPressure srp = new SolarRadiationPressure(this.sun, this.earth.getEquatorialRadius(),
                spacecraft);
        final double[] dAccdParam = new double[3];
        srp.addDAccDParam(new SpacecraftState(this.orbit), KA, dAccdParam);
        Assert.assertTrue(KA.getName().contains(ABSORPTION_COEFFICIENT));
        Assert.assertEquals(2.0, dAccdParam[1] / dAccdParam[0], 0.0);
        Assert.assertEquals(3.0, dAccdParam[2] / dAccdParam[0], 0.0);

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        srp.addDAccDState(new SpacecraftState(this.orbit), dAccdPos, dAccdVel);
        Assert.assertEquals(0.1, dAccdPos[0][1] / dAccdPos[0][0], 0.0);
        Assert.assertEquals(0.0, dAccdVel[0][0], 0.0);
        Assert.assertEquals(0.0, dAccdVel[2][1], 0.0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#SRP_WITH_ELLIPSOID}
     * @testedFeature {@link features#RADIATIVE_MODEL}
     *
     * @testedMethod {@link SolarRadiationPressure#addDAccDParam(SpacecraftState, String, double[])}
     *
     * @description Test exception thrown in case of unsupported parameter
     *
     * @input an unsupported parameter (kref)
     *
     * @output an exception
     *
     * @testPassCriteria an exception should be raised
     *
     * @referenceVersion 2.3.1
     *
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testUnsupportedParam() throws PatriusException {
        final double[] dAccdParam = new double[3];
        // params and computation
        this.pos = new Vector3D(-1.5698127651877177E+06, -4.0571199222644530E+06, 5.1323187886121357E+06);
        this.vel = new Vector3D(2.6780951868939924E+03, -5.2461908201159367E+03, -4.9566405945152592E+03);
        final PVCoordinates pv = new PVCoordinates(this.pos, this.vel);
        final Orbit orbit = new CartesianOrbit(pv, this.cirf, this.date, this.mu);
        final SpacecraftState scs = new SpacecraftState(orbit);
        final Assembly assembly = this.createAssemblySphereAndFacets(1000, 0.8, 0.1, 0.1);

        final Parameter k0 = new Parameter("k0", 1.);
        final DirectRadiativeModel sc = new DirectRadiativeModel(assembly, k0);
        final Parameter refFlux = new Parameter("refFlux", this.pRef * this.dRef * this.dRef);
        final SolarRadiationPressure srp = new SolarRadiationPressure(refFlux, this.sun, this.earth, sc);

        try {
            srp.addDAccDParam(scs, refFlux, dAccdParam);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link SolarRadiationPressure#PatriusSolarRadiationPressure(PVCoordinatesProvider, fr.cnes.sirius.patrius.bodies.BodyShape, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressure#PatriusSolarRadiationPressure(Parameter, PVCoordinatesProvider, fr.cnes.sirius.patrius.bodies.BodyShape, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressure#PatriusSolarRadiationPressure(double, double, PVCoordinatesProvider, fr.cnes.sirius.patrius.bodies.BodyShape, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressure#computeGradientPosition()}
     * @testedMethod {@link SolarRadiationPressure#computeGradientVelocity()}
     *
     * @description compute acceleration partial derivatives wrt position
     *
     * @input instances of {@link SolarRadiationPressure}
     *
     * @output partial derivatives
     *
     * @testPassCriteria partial derivatives must be all null, since computation
     *                   is deactivated at construction
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // Instance
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(10.0, 1.7, 0, 0, 0, "");
        final EllipsoidBodyShape body = new OneAxisEllipsoid(6378000, 0.001, FramesFactory.getITRF(), "Earth");
        final SolarRadiationPressure srp = new SolarRadiationPressure(new Parameter("toto", 1.), this.sun, sunRadius,
                body, spacecraft, false);
        final SolarRadiationPressure srp2 = new SolarRadiationPressure(this.sun, body, spacecraft, false);
        final SolarRadiationPressure srp3 = new SolarRadiationPressure(1, 1, this.sun, sunRadius, body, spacecraft,
                false);

        // Spacecraft state
        final Orbit orbit = new KeplerianOrbit(7E7, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
                this.date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Check partial derivatives are not computed
        Assert.assertFalse(srp.computeGradientPosition());
        Assert.assertFalse(srp.computeGradientVelocity());

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        final double[][] dAccdPos2 = new double[3][3];
        final double[][] dAccdVel2 = new double[3][3];
        final double[][] dAccdPos3 = new double[3][3];
        final double[][] dAccdVel3 = new double[3][3];

        // Compute partial derivatives
        srp.addDAccDState(state, dAccdPos, dAccdVel);
        srp2.addDAccDState(state, dAccdPos2, dAccdVel2);
        srp3.addDAccDState(state, dAccdPos3, dAccdVel3);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdPos2[i][j], 0);
                Assert.assertEquals(0, dAccdPos3[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
                Assert.assertEquals(0, dAccdVel2[i][j], 0);
                Assert.assertEquals(0, dAccdVel3[i][j], 0);
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#SRP_WITH_ELLIPSOID}
     *
     * @testedMethod {@link SolarRadiationPressure#getSolarFlux()}
     *
     * @description Test computation for solar flux
     *
     * @input SolarRadiationPressure
     *
     * @output solar flux
     *
     * @testPassCriteria solar flux is as expected (reference computed manually)
     *
     * @referenceVersion 4.0
     *
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testSolarFlux() throws PatriusException {

        // Initialization
        final SpacecraftState state = new SpacecraftState(this.orbit);
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(1, 2, 0.1, 0.2, 0.3, "Spacecraft");
        final CelestialBody sun = new MeeusSun();
        final SolarRadiationPressure srp = new SolarRadiationPressure(sun, this.earth, spacecraft);

        // Computation
        final Vector3D actual = srp.getSolarFlux(state);
        final Vector3D expectedRelPos = state.getPVCoordinates().getPosition()
                .subtract(sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()).getPosition());
        final double expectedDistance = expectedRelPos.getNorm();
        final Vector3D expectedDirection = expectedRelPos.normalize();
        final double expectedCst = Constants.CONST_SOL_N_M2 * Constants.SEIDELMANN_UA * Constants.SEIDELMANN_UA
                / (expectedDistance * expectedDistance);
        final Vector3D expected = expectedDirection.scalarMultiply(expectedCst);

        // Check
        Assert.assertEquals(0., expected.subtract(actual).getNorm() / expected.getNorm(), 1E-15);
    }

    /**
     * @throws PatriusException
     *         SolarRadiationPressure model creation
     * @testType UT
     *
     *
     * @description Test the getters of a class.
     *
     * @input the class parameters
     *
     * @output the class parameters
     *
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     *
     * @referenceVersion 4.1
     *
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        final EllipsoidBodyShape earthBody = new OneAxisEllipsoid(Constants.CNES_STELA_AE,
                Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getCIRF(), "earth");
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("ACSOL.act"));
        final RadiationSensitive spacecraft = new PatriusSphericalSpacecraft(20, 0.3, 0.4, 0.1, 0.1, "mainpart");
        final SolarRadiationPressure SolarRadiationPressure = new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                earthBody, spacecraft);
        Assert.assertTrue(earthBody.equals(SolarRadiationPressure.getOccultingBodies().get(0)));
        Assert.assertTrue(CelestialBodyFactory.getSun().equals(SolarRadiationPressure.getSunBody()));

        final AssemblyBuilder builder = new AssemblyBuilder();
        final String mainPart = "Satellite";
        final String array1 = "array1";
        final String array2 = "array2";
        builder.addMainPart(mainPart);

        builder.addPart(array1, mainPart, new Transform(AbsoluteDate.J2000_EPOCH, new Rotation(Vector3D.PLUS_I, 0.25)));
        builder.addPart(array2, mainPart, new Transform(AbsoluteDate.J2000_EPOCH, new Rotation(Vector3D.PLUS_K, 0.18)));
        builder.addProperty(new MassProperty(10), array1);
        builder.addProperty(new RadiativeProperty(0.4, 0.3, 0.5), array2);
        builder.addProperty(new RadiativeCrossSectionProperty(new Sphere(new Vector3D(1, 2, 3), 15)), array2);
        // adding of a simple mass property
        final SolidShape sphere = new Sphere(Vector3D.ZERO, 1);
        final IPartProperty shapeProp = new GeometricProperty(sphere);
        builder.addProperty(shapeProp, mainPart);
        final SolarRadiationPressure solarRadiationPressure2 = new SolarRadiationPressure(200, 10,
                CelestialBodyFactory.getSun(), sunRadius, earthBody, builder.returnAssembly(), 3);
        Assert.assertEquals(3, solarRadiationPressure2.getMultiplicativeFactor(), 0);
        final AssemblyBuilder builder2 = new AssemblyBuilder();

        builder2.addMainPart(mainPart);

        builder2.addPart(array1, mainPart, new Transform(AbsoluteDate.J2000_EPOCH, new Rotation(Vector3D.PLUS_I, 0.25)));
        builder2.addPart(array2, mainPart, new Transform(AbsoluteDate.J2000_EPOCH, new Rotation(Vector3D.PLUS_K, 0.18)));
        builder2.addProperty(new MassProperty(10), array1);
        builder2.addProperty(new RadiativeProperty(0.4, 0.3, 0.5), array2);
        builder2.addProperty(new RadiativeCrossSectionProperty(new Sphere(new Vector3D(1, 2, 3), 15)), array2);
        // adding of a simple mass property
        builder2.addProperty(shapeProp, mainPart);
        final Assembly assembly = builder2.returnAssembly();
        final SolarRadiationPressure srp2 = new SolarRadiationPressure(this.dRef, this.pRef,
                CelestialBodyFactory.getSun(), sunRadius, earthBody, assembly, 12.);

        final CelestialBody body2 = CelestialBodyFactory.getMoon();
        final OneAxisEllipsoid moon = new OneAxisEllipsoid(1500000, 0,
                body2.getInertialFrame(IAUPoleModelType.CONSTANT), "Body2");
        srp2.addOccultingBody(moon);

        final SolarRadiationPressure srp3 = new SolarRadiationPressure(srp2, assembly);

        Assert.assertEquals(12, srp3.getMultiplicativeFactor(), 0);
        Assert.assertEquals(2, srp3.getOccultingBodies().size());
        Assert.assertEquals(moon, srp3.getOccultingBodies().get(1));
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link ForceModel#enrichParameterDescriptors()}
     *
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     *
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // Initialization with occulting body = Moon
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final BodyShape moonModel = new OneAxisEllipsoid(1500000, 0, moon.getRotatingFrame(IAUPoleModelType.TRUE), "Moon");
        final SolarRadiationPressure forceModel = new SolarRadiationPressure(sun, moonModel, new SphericalSpacecraft(
                50.0, 0.5, 1, 0., 0., DEFAULT));

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link SolarRadiationPressure#getLightningRatio(Vector3D, BodyShape, PVCoordinatesProvider, Frame, AbsoluteDate)}
     * @testedMethod {@link SolarRadiationPressure#SolarRadiationPressure(double, double, PVCoordinatesProvider, double, double, RadiationSensitive)}
     *
     * @description check that changing the solar radius in the constructor changes the result of
     *              the lightning ratio
     *
     * @testPassCriteria the
     *                   {@link SolarRadiationPressure#getLightningRatio(Vector3D, BodyShape, PVCoordinatesProvider, Frame, AbsoluteDate)}
     *                   returns two different values for two different solar radiuses
     */
    @Test
    public void testSolarRadiusInConstructor() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(10.0, 1.7, 0, 0, 0, "");

        // SRP1 and SRP2 are made with different Sun Radiuses
        final SolarRadiationPressure srp1 = new SolarRadiationPressure(Constants.SEIDELMANN_UA,
                Constants.CONST_SOL_N_M2, sun, 6.95E8, 100., spacecraft);
        final SolarRadiationPressure srp2 = new SolarRadiationPressure(Constants.SEIDELMANN_UA,
                Constants.CONST_SOL_N_M2, sun, Constants.SUN_RADIUS, 100., spacecraft);

        // Common Earth-Sun position to get the usual distance
        final Vector3D earthSunPos = this.sun.getPVCoordinates(this.date, FramesFactory.getGCRF()).getPosition();

        // PV of satellite in GCRF on the Earth-Sun axis, but the distance is 1% behind Earth
        final double ratioDistSatEarthSatSun = 1E-2;
        final PVCoordinatesProvider pvProv = new ConstantPVCoordinatesProvider(
                earthSunPos.scalarMultiply(-ratioDistSatEarthSatSun), FramesFactory.getGCRF());

        // A slight shift off the axis for the Sun position so the satellite is in penumbra
        final double shiftFromAxis = 1E7;
        // Sat-Sun vector is giving the Sun actual position for the computation of the lightning
        // ratio
        final Vector3D satSunVector = earthSunPos.scalarMultiply(1 + ratioDistSatEarthSatSun).add(
                new Vector3D(0, 0, shiftFromAxis));
        final double ratio1 = srp1.getLightningRatio(satSunVector, this.earth, pvProv, FramesFactory.getGCRF(),
                this.date);
        final double ratio2 = srp2.getLightningRatio(satSunVector, this.earth, pvProv, FramesFactory.getGCRF(),
                this.date);

        Assert.assertTrue(Math.abs(ratio2 - ratio1) > Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * setup
     *
     * @throws PatriusException
     *         if missing files
     * @throws IOException
     *         file error
     */
    public void setupClassic() throws PatriusException, IOException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataPBASE");
    }

    /**
     * setup
     *
     * @throws PatriusException
     *         if missing files
     * @throws IOException
     *         file error
     */
    @Before
    public void setup() throws PatriusException, IOException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("SRPwithFlattening");

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        this.cirf = FramesFactory.getCIRF();

        // bodies
        Utils.setDataRoot("regular-data");
        this.sun = CelestialBodyFactory.getSun();

        this.earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765, FramesFactory.getITRF());

        this.date = new AbsoluteDate(new DateComponents(2003, 3, 21), new TimeComponents(13, 59, 27.816),
                TimeScalesFactory.getUTC());
        this.orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3, MathLib.tan(0.001745329)
                * MathLib.cos(2 * FastMath.PI / 3), MathLib.tan(0.001745329) * MathLib.sin(2 * FastMath.PI / 3), 0.1,
                PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);
    }

    /**
     * Assert two vectors are equal
     *
     * @param expected
     *        expected vector
     * @param actual
     *        actual vector
     * @param thr
     *        threshold
     */
    public void assertEqualsVector3D(final Vector3D expected, final Vector3D actual, final double thr) {
        if (expected.getNorm() < Precision.EPSILON) {
            Assert.assertTrue(actual.subtract(expected).getNorm() < Precision.EPSILON);
        } else {
            Assert.assertTrue(MathLib.abs(expected.getX() - actual.getX()) <= thr);
            Assert.assertTrue(MathLib.abs(expected.getY() - actual.getY()) <= thr);
            Assert.assertTrue(MathLib.abs(expected.getZ() - actual.getZ()) <= thr);
        }
    }
}
