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
 * @history creation 29/06/2017
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.GlobalDragCoefficientProvider.INTERP;
import fr.cnes.sirius.patrius.assembly.models.cook.AlphaConstant;
import fr.cnes.sirius.patrius.assembly.properties.AeroProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
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

public class GlobalAeroModelValTest {

    /**
     * @testType VT
     * 
     * @description performs two propagations: one with AeroModel and the other one with GlobalAeroModel.
     *              It compares the two propagations: position through time as well as acceleration.
     * 
     * @input AeroModel and GlobalAeroModel parametered in the closest way
     * 
     * @output ephemeris and accelerations
     * 
     * @testPassCriteria Results are close enough (position threashold: 3mm)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public final void globalAeroModelValidationTest() throws PatriusException, IOException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        // ================ Generic initialization ================ //

        // Attitude law
        final AttitudeProvider attitudeLaw = new BodyCenterPointing();

        // Initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2012, 04, 01, TimeScalesFactory.getUTC());
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(-5440066.6571969, -4459159.0836607,
            -5092.5157578855),
            new Vector3D(-657.11216045992, 805.00143291216, 7457.4345151707)), FramesFactory.getGCRF(),
            initialDate, Constants.GRIM5C1_EARTH_MU);
        final Attitude attitude = attitudeLaw.getAttitude(orbit);
        final SpacecraftState initialState = new SpacecraftState(orbit, attitude);

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

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart("Main");
        builder.addProperty(new AeroSphereProperty(MathLib.sqrt(1.53546 / FastMath.PI), 2.2), "Main");
        builder.addProperty(new MassProperty(1000.), "Main");
        builder.addProperty(new AeroProperty(0., 300, new AlphaConstant(1.)), "Main");
        builder.initMainPartFrame(initialState);

        final Assembly assembly = builder.returnAssembly();

        // Atmosphere
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getITRF());
        final MSISE2000InputParameters msisData = new ClassicalMSISE2000SolarData(new ConstantSolarActivity(180., 10.));
        final ExtendedAtmosphere atmosphere = new MSISE2000(msisData, earth, CelestialBodyFactory.getSun());

        // ================ AeroModel propagation ================ //

        // AeroModel
        final DragSensitive spacecraftAero = new AeroModel(assembly);
        final ForceModel dragAero = new DragForce(atmosphere, spacecraftAero);
        propagator.addForceModel(dragAero);

        // Propagation
        propagator.propagate(finalDate);
        final BoundedPropagator ephemerisAero = propagator.getGeneratedEphemeris();

        // ================ GlobalAeroModel propagation ================ //

        // GlobalAeroModel
        propagator.removeForceModels();
        propagator.setInitialState(initialState);

        final URL url = GlobalAeroModelValTest.class.getClassLoader().getResource("coeffaero/CoeffAeroGlobalModel.txt");
        final DragCoefficientProvider coefficientProvider = new GlobalDragCoefficientProvider(INTERP.LINEAR,
            url.getPath());
        final DragSensitive spacecraftGlobalAero = new GlobalAeroModel(assembly, coefficientProvider, atmosphere);
        final ForceModel dragGlobalAero = new DragForce(atmosphere, spacecraftGlobalAero);
        propagator.addForceModel(dragGlobalAero);

        // Propagation
        propagator.propagate(finalDate);
        final BoundedPropagator ephemerisGlobalAero = propagator.getGeneratedEphemeris();

        // ================ Comparison ================ //

        int currentDuration = 0;
        while (currentDuration <= duration) {

            // Retrieve orbital parameters from propagation
            final AbsoluteDate currentDate = initialDate.shiftedBy(currentDuration);
            final SpacecraftState stateAero = ephemerisAero.propagate(currentDate);
            final SpacecraftState stateGlobalAero = ephemerisGlobalAero.propagate(currentDate);
            final Orbit orbitAero = stateAero.getOrbit();
            final Orbit orbitGlobalAero = stateGlobalAero.getOrbit();

            final Transform tAero = LOFType.TNW.transformFromInertial(currentDate, orbitAero.getPVCoordinates());

            final Vector3D diffPos = tAero.transformPosition(orbitGlobalAero.getPVCoordinates().getPosition());

            // Compute acceleration of models
            final Vector3D accAero = tAero.transformVector(dragAero.computeAcceleration(stateAero));
            final Vector3D accGlobalAero = tAero.transformVector(dragGlobalAero.computeAcceleration(stateGlobalAero));

            // Comparison
            // System.out.println(String.format(Locale.US,
            // "%d %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f",
            // currentDuration,
            // diffPos.getX(), diffPos.getY(), diffPos.getZ(), diffPos.getNorm(),
            // accAero.getX(), accAero.getY(), accAero.getZ(), accAero.getNorm(),
            // accGlobalAero.getX(), accGlobalAero.getY(), accGlobalAero.getZ(), accGlobalAero.getNorm()));

            currentDuration += 10;

            // Check (error in pos: 3mm max, acceleration error: 2% max)
            Assert.assertEquals(0., diffPos.getNorm(), 0.0033);
            Assert.assertEquals(0., (accGlobalAero.getNorm() - accAero.getNorm()) / accAero.getNorm(), 0.02);
        }
    }

}
