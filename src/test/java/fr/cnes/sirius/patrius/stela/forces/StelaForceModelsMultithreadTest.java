/**
 * 
 * Copyright 2024-2024 CNES
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
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.15:OPENFD-308:21/11/2024:[STELA-PATRIUS] Duplication entre MSIS00Adapter et MSIS2000
 * VERSION:4.14:OPENFD-180:22/08/2024: [PATRIUS] Thread-safety du propagateur STELA-PATRIUS
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.stela.forces.drag.StelaConstantDragCoef;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.PotentialCoefficientsProviderTest;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAeroModel;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;

import fr.cnes.sirius.patrius.stela.forces.gravity.SolidTidesAcc;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaTesseralAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaThirdBodyAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.forces.noninertial.NonInertialContribution;
import fr.cnes.sirius.patrius.stela.forces.radiation.SRPPotential;
import fr.cnes.sirius.patrius.stela.forces.radiation.StelaSRPSquaring;
import fr.cnes.sirius.patrius.stela.propagation.StelaGTOPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * 
 * Test Stela force models multithreading.
 * 
 * @author Manuel Amouroux
 * 
 * @since 4.14
 */
public class StelaForceModelsMultithreadTest {

    /** The potential coefficients provider used for test purposes. */
    final PotentialCoefficientsProviderTest provider = new PotentialCoefficientsProviderTest();

    // Force model activations
    /** SRP */
    private static final boolean enablePRS = true;

    /** Eclipse for SRP */
    private static final boolean enableEclipseComputation = true;

    /** Earth zonal attraction */
    private static final boolean enableEarthGravityZonal = true;

    /** Earth tesseral attraction */
    private static final boolean enableEarthGravityTesseral = true;

    /** Third body attraction (Moon) */
    private static final boolean enableThirdBodyMoon = true;

    /** Third body attraction (Sun) */
    private static final boolean enableThirdBodySun = true;

    /** Atmospheric drag */
    private static final boolean enableDrag = true;

    /** Solid tides */
    private static final boolean enableSolidTides = true;

    /** Non inertial contribution*/
    private static final boolean enableNonInertialContribution = true;

    // Orbital and propagation parameters
    /** Initial date */
    private static final AbsoluteDate initialDate = new AbsoluteDate("2019-01-01T00:00:00.000");

    /** Propagation duration (s) */
    private static final double propagationDuration = 864000.;

    /** SMA (m) */
    private static final double orbA = 6915843.310968788;

    /** Eccentricity */
    private static final double orbE = 0.012168680701273082;

    /** Inclination (rad) */
    private static final double orbI = 1.5707963267948966;

    /** Perigee argument (rad) */
    private static final double orbPa = -3.141592653589793;

    /** RAAN (rad) */
    private static final double orbRaan = 0.0;

    /** True anomaly (rad) */
    private static final double orbV = 3.141592653589793;

    // Force model values
    // Sat
    /** Drag coefficient */
    private static final double cx = 2.2;

    /** Reflectivity coefficient */
    private static final double cr = 1;

    /** Surface / mass ratio */
    private static final double sm = 0.01;

    /** Mass */
    private static final double mass = 1000.;

    // Earth gravitational model
    /** degree of development for zonal perturbations */
    private static final int earthPotDegree = 7;

    /** Max order for the Kaula development (tesseral perturbations) */
    private static final int earthPotOrder = 7;

    /** Third body maximum potential degree for development */
    private static final int thirdBodyPotDegree = 4;

    /**
     * setUp.
     */
    @BeforeClass
    public static void setUp() {
        Utils.clear();
        Utils.setDataRoot("regular-dataPBASE");

        // STELA configuration
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());
        // STELA UTC-TAI shift
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){
            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name) {
                // N/A
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> entries = new TreeMap<>();
                for (int i = 1970; i < 2200; i++) {
                    entries.put(new DateComponents(i, 1, 1), 37);
                }
                return entries;
            }

            @Override
            public String getSupportedNames() {
                return "";
            }
        });
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link StelaForceModel}
     * 
     * @description unit test for Stela force model thread safety
     * 
     * @input Stela force models and orbit ephemeris
     * 
     * @output propagated PV
     * 
     * @testPassCriteria values of PV are identical across all threads
     * 
     * @referenceVersion 4.14
     */
    @Test
    public void StelaForceModelsMultiThreadTest() {
        final int nbOfThreads = 15;
        final List<Integer> list = new ArrayList<>();
        final PVCoordinates[] resultsArray = new PVCoordinates[nbOfThreads];

        for (int i = 0; i < nbOfThreads; i++) {
            list.add(i);
        }

        list.parallelStream().forEach(i -> {
            try {
                resultsArray[i] = propagate();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        Assert.assertTrue(allPVCoordinatesAreIdentical(resultsArray));
    }

    /**
     * Checks whether or not all values in the given array are identical.
     * 
     * @param pvCoordinatesArray
     *        the array to evaluate
     * @return true if all the values are identical, false otherwise
     */
    private static boolean allPVCoordinatesAreIdentical(final PVCoordinates[] pvCoordinatesArray) {
        final PVCoordinates last = pvCoordinatesArray[0];
        for (final PVCoordinates pvCoord : pvCoordinatesArray) {
            if (!pvCoord.equals(last)) {
                return false;
            }
        }
        return true;
    }

    /**
     * PATRIUS Propagation.
     * 
     * @return propagated PV coordinates
     * 
     * @throws IOException
     * @throws ParseException
     * @throws PatriusException
     */
    private static PVCoordinates propagate()
        throws IOException, ParseException, PatriusException {
        // Build propagator
        final KeplerianParameters initialParameters = new KeplerianParameters(orbA, orbE, orbI, orbPa, orbRaan,
            orbV, PositionAngle.TRUE, Constants.EGM96_EARTH_MU);
        final Orbit initialOrbit = new KeplerianOrbit(initialParameters, FramesFactory.getGCRF(),
            initialDate);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        // Sat assembly
        final Assembly assembly = buildAssembly(mass, sm * mass, cx, cr);
        final Propagator propagator = buildStelaPropagator(initialState, assembly);
        // Perform propagation
        return propagator.propagate(initialDate.shiftedBy(propagationDuration)).getPVCoordinates();
    }

    /**
     * Build assembly.
     * 
     * @param mass
     *        mass
     * @param area
     *        area
     * @param cx
     *        cx
     * @param cr
     *        cr
     * @return built assembly
     * @throws PatriusException
     */
    private static Assembly buildAssembly(final double mass, final double area, final double cx,
                                          final double cr) throws PatriusException {
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(mass), "Main");
        builder1.addProperty(new AeroSphereProperty(FastMath.sqrt(area / FastMath.PI), cx), "Main");
        builder1.addProperty(new RadiativeSphereProperty(FastMath.sqrt(area / FastMath.PI)), "Main");
        builder1.addProperty(new RadiativeProperty(0., 0., (cr - 1.) * 4. / 9.), "Main");
        return builder1.returnAssembly();
    }

    /**
     * Build STELA PATRIUS propagator.
     * 
     * @param initialState
     *        initial state
     * @param spacecraft
     *        assembly
     * @return propagator
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     */
    private static Propagator buildStelaPropagator(final SpacecraftState initialState, final Assembly spacecraft)
        throws PatriusException, IOException, ParseException {

        // Build propagator
        final RungeKutta6Integrator integrator = new RungeKutta6Integrator(Constants.JULIAN_DAY);
        final StelaGTOPropagator propagator = new StelaGTOPropagator(integrator, 10, 10);

        // Models
        final MeeusSun sun = new MeeusSun(MODEL.STELA);
        final MeeusMoonStela moon = new MeeusMoonStela(Constants.CNES_STELA_AE);
        final double surface = sm * mass;

        // Add forces
        // Earth gravity
        if (enableEarthGravityZonal) {
            GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr",
                true));
            final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();
            propagator.addForceModel(new StelaZonalAttraction(data, earthPotDegree, true, 2, 0, false));
            if (enableEarthGravityTesseral) {
                propagator.addForceModel(new StelaTesseralAttraction(data, earthPotOrder, 2, Constants.JULIAN_DAY, 5));
            }
        }

        // Solid tides
        if (enableSolidTides) {
            propagator.addForceModel(new SolidTidesAcc(sun, moon));
        }
        // Non inertial contribution
        if (enableNonInertialContribution) {
            propagator.addForceModel(new NonInertialContribution(7, FramesFactory.getGCRF()));
        }

        // Third body
        if (enableThirdBodyMoon) {
            propagator.addForceModel(new StelaThirdBodyAttraction(moon, thirdBodyPotDegree, 2, 0));
        }
        if (enableThirdBodySun) {
            propagator.addForceModel(new StelaThirdBodyAttraction(sun, thirdBodyPotDegree, 2, 0));
        }

        // Drag force
        if (enableDrag) {
            final SolarActivityDataProvider solarActivity = new ConstantSolarActivity(140., 15.);
            final DragSensitive aeroModel = new StelaAeroModel(mass, new StelaConstantDragCoef(cx), surface);
            final Atmosphere atmosphere =
                    new MSISE2000(new ContinuousMSISE2000SolarData(solarActivity),
                            new OneAxisEllipsoid(Constants.CNES_STELA_AE,
                                    Constants.WGS84_EARTH_FLATTENING, FramesFactory.getTIRF()),
                            sun);
            propagator.addForceModel(new StelaAtmosphericDrag(aeroModel, atmosphere, 33,
                Constants.CNES_STELA_AE, 2500000, 1));
        }

        // Solar radiation pressure
        if (enablePRS) {
            final StelaForceModel srp;
            if (enableEclipseComputation) {
                srp = new StelaSRPSquaring(mass, surface, cr, 11, sun);
            } else {
                srp = new SRPPotential(sun, mass, surface, cr);
            }
            propagator.addForceModel(srp);
        }

        // Mean osculating converter
        final List<StelaForceModel> converterList = new ArrayList<>();
        for (final StelaForceModel forceModel : propagator.getForceModels()) {
            if ((forceModel instanceof StelaTesseralAttraction)
                    || (forceModel instanceof StelaZonalAttraction)
                    || (forceModel instanceof StelaThirdBodyAttraction)) {
                // System.out.println(" forceModel " + forceModel.toString());
                converterList.add(forceModel);
            }
        }
        propagator.setNatureConverter(converterList);

        // Set initial state
        propagator.setInitialState(initialState, mass, true);
        // Return propagator
        return propagator;
    }
}