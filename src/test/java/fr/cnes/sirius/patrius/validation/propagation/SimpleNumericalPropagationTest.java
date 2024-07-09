/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history created 9/10/12
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Renamed Cunningham and Droziner
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.propagation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureCircular;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Simple numerical propagation test.
 * What is tested here is the overall result numerical stability,
 * and the duration of the propagation stability.
 * 
 * @author cardosop, tanguyy
 * 
 * @version $Id: SimpleNumericalPropagationTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class SimpleNumericalPropagationTest {

    /** validation tool */
    private static Validate validate;

    /** Constant. */
    private static final double CST_1000 = 1000.;
    /** Constant. */
    private static final double CST_298_257222101 = 298.257222101;
    /** Constant. */
    private static final double CST_298_25765 = 298.25765;
    /** File. */
    private static final String UNXP2000_405 = "unxp2000.405";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle propagation result stability
         * 
         * @featureDescription propagation result stability
         * 
         * @coveredRequirements
         */
        PROPAGATION_RESULT_STABLE,

        /**
         * @featureTitle propagation duration stability
         * 
         * @featureDescription propagation duration stability
         * 
         * @coveredRequirements
         */
        PROPAGATION_DURATION_STABLE
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#PROPAGATION_RESULT_STABLE}
     * @testedFeature {@link features#PROPAGATION_DURATION_STABLE}
     * 
     * @testedMethod misc
     * 
     * @description simple propagation
     * 
     * @input misc
     * 
     * @output propagation result and duration
     * 
     * @testPassCriteria result close to expected; duration close to expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @Test
    public final void simplePropagationTest() throws PatriusException, IOException, ParseException {
        /*
         * Exemple d'appel au propagateur numérique
         * 1) Init de l'accès aux données d'environnement
         * 2) Création d'un bulletin d'orbite
         * 3) Création d'un intégrateur numérique
         * 4) Création du propagateur
         * 5) Ajout des forces naturelles
         * 6) Critères d'arrêt de la propagation
         */

        /*
         * 2) Création d'un bulletin d'orbite
         */

        // On récupère le GCRF : il est inertiel, on peut donc définir un bulletin d'orbite dedans
        final Frame GCRF = FramesFactory.getGCRF();
        // une date égale au 2 janvier 12h, en échelle TT
        final AbsoluteDate dateInit = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 86400, TimeScalesFactory.getTT());
        final double mu = Constants.EIGEN5C_EARTH_MU;

        final Orbit bulletin = new KeplerianOrbit(7200e3, 1e-3,
            MathLib.toRadians(98), MathLib.toRadians(12.0),
            MathLib.toRadians(0.0), MathLib.toRadians(0.0),
            PositionAngle.MEAN, GCRF, dateInit, mu);

        /*
         * 3) Création d'un intégrateur numérique avec tolérances absolues réglées à 1e-5 m / 1e-8 m.s-1
         * et tolérances relatives réglées à 1e-12
         * Le pas peut varier entre 0.1 et 500 s
         */
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);

        /*
         * 4) Création du propagateur
         */
        final NumericalPropagator propag = new NumericalPropagator(dop);

        /*
         * 5) Ajout des forces naturelles
         */

        // a) Modèle de potentiel

        // add a reader for gravity fields
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr' file
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // we get the data as extracted from the file
        // degree
        final int n = 60;
        // order
        final int m = 60;
        final double[][] C = provider.getC(n, m, false);
        final double[][] S = provider.getS(n, m, false);

        // return perturbing force (ITRF2008 central body frame)
        final ForceModel potentiel = new DrozinerAttractionModel(FramesFactory.getITRF(), provider.getAe(),
            provider.getMu(), C, S);

        // b) Attraction des troisièmes corps
        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader(UNXP2000_405,
            JPLEphemeridesLoader.EphemerisType.SUN);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader(UNXP2000_405,
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader(UNXP2000_405,
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);
        final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);

        final ForceModel sunAttraction = new ThirdBodyAttraction(sun);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moon);

        // c) Pression de radiation solaire
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46,
            1.0 / CST_298_25765, FramesFactory.getITRF());
        final RadiationSensitive vehicle = new SphericalSpacecraft(10, 2.2,
            0.5, 0.5, 0.0, "default");

        final ForceModel prs = new SolarRadiationPressureCircular(sun,
            earth.getEquatorialRadius(), vehicle);

        propag.addForceModel(potentiel);
        propag.addForceModel(sunAttraction);
        propag.addForceModel(moonAttraction);

        propag.addForceModel(prs);

        // d) Frottement atmosphérique
        final SimpleExponentialAtmosphere atm =
            new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
                1.0 / CST_298_257222101, FramesFactory.getITRF()),
                0.0004, 42000.0, 7500.0);

        final ForceModel atmDrag = new DragForce(atm, (DragSensitive) vehicle);

        propag.addForceModel(atmDrag);

        /*
         * 6) Critères d'arrêt de la propagation
         */
        final SimpleMassModel massModel = new SimpleMassModel(1000., "default");
        final SpacecraftState s = new SpacecraftState(bulletin, massModel);
        propag.setMassProviderEquation(massModel);
        propag.setInitialState(s);

        final SimpleNumericalPropagation stepHandler = new SimpleNumericalPropagation();
        propag.setMasterMode(3600, stepHandler);

        final double lod = 86400.;
        final long startMillis = (new Date()).getTime();
        final SpacecraftState finalState = propag.propagate(s.getDate().shiftedBy(10 * lod));
        final long endMillis = (new Date()).getTime();
        final double duration = (endMillis - startMillis) / CST_1000;
        System.out.println("PROPAGATION DURATION : "
            + duration);

        // Validation
        // Reference duration : 40 seconds
        final double referenceDuration = 26.;
        // The delay will be 0 if the test happens to run faster than expected,
        // since we only care if it runs LONGER.
        final double delay = MathLib.max(0., duration - referenceDuration);
        // A ten-second worsening will be reason to investigate.
        final double delayEpsilon = 10.;
        validate.assertEquals(delay, 0., delayEpsilon, 0., delayEpsilon, "simple propagation duration");

        final Vector3D finalPos = finalState.getPVCoordinates().getPosition();
        final Vector3D referencePos = new Vector3D(1913237.920707766, -677862.3295450562, 6904232.852501038);
        final double[] finalPosArray = finalPos.toArray();
        final double[] referencePosArray = referencePos.toArray();
        // A huge epsilon, the validity of the final result is not the main purpose of this test.
        final double posEpsilon = 1000.;
        validate.assertEqualsArray(finalPosArray, referencePosArray, posEpsilon,
            referencePosArray, posEpsilon,
            "simple propagation position");
    }

    /**
     * @author tanguyy
     * 
     */
    private final class SimpleNumericalPropagation implements PatriusFixedStepHandler {

        /** Serial UID. */
        private static final long serialVersionUID = 6780874652620353429L;

        /** dateInit. */
        AbsoluteDate dateInit;

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.dateInit = s0.getDate();
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                        throws PropagationException {
            // System.out.println(currentState.getDate().durationFrom(dateInit) + " : " + currentState.getA());
        }

    }

    /**
     * Setup method.
     * 
     * @throws IOException
     *         should not happen
     * @throws PatriusException
     *         should not happen
     */
    @BeforeClass
    public static void setUp() throws IOException, PatriusException {
        validate = new Validate(SimpleNumericalPropagationTest.class);
        Utils.setDataRoot("regular-dataCNES-2003:potentialCNES");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
    }

    /**
     * Tear down method.
     * 
     * @throws IOException
     *         should not happen
     * @throws URISyntaxException
     *         should not happen
     */
    @AfterClass
    public static void tearDown() throws IOException, URISyntaxException {
        validate.produceLog();
    }

}
