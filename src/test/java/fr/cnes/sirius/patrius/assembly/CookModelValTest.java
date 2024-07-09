/**
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
 * @history creation 06/09/2017
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.cook.CnCookModel;
import fr.cnes.sirius.patrius.assembly.models.cook.CtCookModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class CookModelValTest {

    /**
     * @testType VT
     * 
     * @description performs two propagations: one with AeroFacetproperty using Cook Cn/Ct model
     *              and the other one with AeroFacetproperty using constant Cn/Ct.
     *              It compares the two propagations: position through time as well as acceleration.
     * 
     * @input AeroFacetproperty, CnCookModel and CtCookModel parametered in the closest way
     * 
     * @output ephemeris and accelerations
     * 
     * @testPassCriteria Results are close enough (position threshold: 5cm)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public final void cookModelValidationTest() throws PatriusException, IOException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        final FramesConfiguration config = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2003Configuration(true));

        // ================ Generic initialization ================ //

        // Attitude law
        final AttitudeProvider attitudeLaw = new BodyCenterPointing();

        // Initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2002, 04, 01, TimeScalesFactory.getUTC());
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(-5440066.6571969, -4459159.0836607,
            -5092.5157578855),
            new Vector3D(-657.11216045992, 805.00143291216, 7457.4345151707)), FramesFactory.getGCRF(),
            initialDate, Constants.GRIM5C1_EARTH_MU);
        final SpacecraftState initialState = new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit));

        // Propagator
        final double[] absTol = new double[] { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTol = new double[] { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(1E-6, 100, absTol, relTol);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setEphemerisMode();
        propagator.setInitialState(initialState);

        // Attitude law
        propagator.setAttitudeProvider(attitudeLaw);

        // Propagation duration (6h)
        final double duration = 6. * 3600.;
        final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

        // Atmosphere
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getITRF());
        final MSISE2000InputParameters msisData = new ClassicalMSISE2000SolarData(new ConstantSolarActivity(180., 10.));
        final ExtendedAtmosphere atmosphere = new MSISE2000(msisData, earth, CelestialBodyFactory.getSun());

        // Assembly

        // ================ Constant Cn/Ct propagation ================ //

        // Assembly with constant Cn/Ct
        final AssemblyBuilder builderConstant = new AssemblyBuilder();
        builderConstant.addMainPart("Main");
        final Facet facetConstant = new Facet(new Vector3D(1, 1, 1), 10.465);
        builderConstant.addProperty(new AeroFacetProperty(facetConstant, 0.8, 0.9), "Main");
        builderConstant.addProperty(new MassProperty(1000.), "Main");
        builderConstant.initMainPartFrame(initialState);
        final Assembly assemblyConstant = builderConstant.returnAssembly();

        // AeroModel
        final DragSensitive spacecraftConstant = new AeroModel(assemblyConstant);
        final DragForce dragConstant = new DragForce(atmosphere, spacecraftConstant);
        propagator.addForceModel(dragConstant);

        // Propagation
        propagator.propagate(finalDate);
        final BoundedPropagator ephemerisConstant = propagator.getGeneratedEphemeris();

        // ================ Cook Cn/Ct propagation ================ //

        // Assembly with Cook Cn/Ct
        final AssemblyBuilder builderCook = new AssemblyBuilder();
        builderCook.addMainPart("Main");
        builderCook.initMainPartFrame(initialState);
        final Frame facetFrame = builderCook.getPart("Main").getFrame();
        final Facet facetCook = new Facet(new Vector3D(1, 1, 1), 10.);
        final CnCookModel cn = new CnCookModel(atmosphere, facetCook, facetFrame, 0, 300.);
        final CtCookModel ct = new CtCookModel(atmosphere, facetCook, facetFrame, 0);
        builderCook.addProperty(new AeroFacetProperty(facetCook, cn, ct), "Main");
        builderCook.addProperty(new MassProperty(1000.), "Main");
        final Assembly assemblyCook = builderCook.returnAssembly();

        // AeroModel
        propagator.removeForceModels();
        propagator.setInitialState(initialState);

        final DragSensitive spacecraftCook = new AeroModel(assemblyCook);
        final DragForce dragCook = new DragForce(atmosphere, spacecraftCook);
        propagator.addForceModel(dragCook);

        // Propagation
        propagator.propagate(finalDate);
        final BoundedPropagator ephemerisCook = propagator.getGeneratedEphemeris();

        // ================ Comparison ================ //

        int currentDuration = 0;
        while (currentDuration <= duration) {

            // Retrieve orbital parameters from propagation
            final AbsoluteDate currentDate = initialDate.shiftedBy(currentDuration);
            final SpacecraftState stateConstant = ephemerisConstant.propagate(currentDate);
            final SpacecraftState stateCook = ephemerisCook.propagate(currentDate);
            final Orbit orbitConstant = stateConstant.getOrbit();
            final Orbit orbitCook = stateCook.getOrbit();

            final Transform tConstant = LOFType.TNW
                .transformFromInertial(currentDate, orbitConstant.getPVCoordinates());

            final Vector3D diffPos = tConstant.transformPosition(orbitCook.getPVCoordinates().getPosition());

            // Compute acceleration of models
            final Vector3D accConstant = tConstant.transformVector(dragConstant.computeAcceleration(stateConstant));
            final Vector3D accCook = tConstant.transformVector(dragCook.computeAcceleration(stateCook));

            // Comparison
            // System.out.println(String.format(Locale.FRANCE,
            // "%d %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f",
            // currentDuration,
            // diffPos.getX(), diffPos.getY(), diffPos.getZ(), diffPos.getNorm(),
            // accConstant.getX(), accConstant.getY(), accConstant.getZ(), accConstant.getNorm(),
            // accCook.getX(), accCook.getY(), accCook.getZ(), accCook.getNorm()));

            currentDuration += 10;

            // Check (error in pos: 5cm max, acceleration error: 2% max)
            Assert.assertEquals(0., diffPos.getNorm(), 0.05);
            Assert.assertEquals(0., (accCook.getNorm() - accConstant.getNorm()) / accConstant.getNorm(), 0.07);
        }

        FramesFactory.setConfiguration(config);
    }
}
