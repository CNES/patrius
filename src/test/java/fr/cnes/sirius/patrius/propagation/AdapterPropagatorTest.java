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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Renamed Droziner to UnnormalizedDroziner
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::FA:1851:18/10/2018:Update the massModel from a SimpleMassModel to an Assembly builder
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.gravity.CunninghamAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.ICGEMFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.maneuvers.SmallManeuverAnalyticalModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980Entry;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980History;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980HistoryLoader;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.analytical.AdapterPropagator;
import fr.cnes.sirius.patrius.propagation.analytical.J2DifferentialEffect;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AdapterPropagatorTest {
    private static final String THRUSTER = "thruster";

    @Test
    public void testLowEarthOrbit() throws PatriusException, ParseException, IOException {
        final Orbit leo = new CircularOrbit(7200000.0, -1.0e-5, 2.0e-4, MathLib.toRadians(98.0),
            MathLib.toRadians(123.456), 0.0, PositionAngle.MEAN, FramesFactory.getEME2000(), new AbsoluteDate(
                new DateComponents(2004, 01, 01), new TimeComponents(23, 30, 00.000),
                TimeScalesFactory.getUTC()), Constants.EIGEN5C_EARTH_MU);
        final double mass = 5600.0;
        final AbsoluteDate t0 = leo.getDate().shiftedBy(1000.0);
        final Vector3D dV = new Vector3D(-0.1, 0.2, 0.3);
        final double f = 20.0;
        final double isp = 315.0;
        final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        final double dt = -(mass * vExhaust / f) * MathLib.expm1(-dV.getNorm() / vExhaust);
        final BoundedPropagator withoutManeuver =
            this.getEphemeris(leo, mass, 5, new LofOffset(leo.getFrame(), LOFType.LVLH,
                RotationOrder.XZY, FastMath.PI / 2.0, -FastMath.PI / 2.0, 0.0), t0,
                Vector3D.ZERO, f, isp, false, false, null, 0, 0);
        final BoundedPropagator withManeuver =
            this.getEphemeris(leo, mass, 5, new LofOffset(leo.getFrame(), LOFType.LVLH,
                RotationOrder.XZY, FastMath.PI / 2.0, -FastMath.PI / 2.0, 0.0), t0,
                dV, f, isp, false, false, null, 0, 0);

        // we set up a model that reverts the maneuvers
        final AdapterPropagator adapterPropagator = new AdapterPropagator(withManeuver);
        final AdapterPropagator.DifferentialEffect effect =
            new SmallManeuverAnalyticalModel(adapterPropagator.propagate(t0),
                dV.negate(), isp, THRUSTER);
        adapterPropagator.addEffect(effect);

        for (AbsoluteDate t = t0.shiftedBy(0.5 * dt); t.compareTo(withoutManeuver.getMaxDate()) < 0; t = t
            .shiftedBy(60.0)) {
            final PVCoordinates pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
            final PVCoordinates pvReverted = adapterPropagator.getPVCoordinates(t, leo.getFrame());
            final double revertError = new PVCoordinates(pvWithout, pvReverted).getPosition().getNorm();
            Assert.assertEquals(0, revertError, 0.45);
        }

    }

    @Test
    public void testEccentricOrbit() throws PatriusException, ParseException, IOException {
        final Orbit heo = new KeplerianOrbit(90000000.0, 0.92, MathLib.toRadians(98.0), MathLib.toRadians(12.3456),
            MathLib.toRadians(123.456), MathLib.toRadians(1.23456), PositionAngle.MEAN,
            FramesFactory.getEME2000(), new AbsoluteDate(new DateComponents(2004, 01, 01), new TimeComponents(23,
                30, 00.000), TimeScalesFactory.getUTC()), Constants.EIGEN5C_EARTH_MU);
        final double mass = 5600.0;
        final AbsoluteDate t0 = heo.getDate().shiftedBy(1000.0);
        final Vector3D dV = new Vector3D(-0.01, 0.02, 0.03);
        final double f = 20.0;
        final double isp = 315.0;
        final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        final double dt = -(mass * vExhaust / f) * MathLib.expm1(-dV.getNorm() / vExhaust);
        final BoundedPropagator withoutManeuver =
            this.getEphemeris(heo, mass, 5, new LofOffset(heo.getFrame(), LOFType.LVLH,
                RotationOrder.XZY, FastMath.PI / 2.0, -FastMath.PI / 2.0, 0.0), t0,
                Vector3D.ZERO, f, isp, false, false, null, 0, 0);
        final BoundedPropagator withManeuver =
            this.getEphemeris(heo, mass, 5, new LofOffset(heo.getFrame(), LOFType.LVLH,
                RotationOrder.XZY, FastMath.PI / 2.0, -FastMath.PI / 2.0, 0.0), t0,
                dV, f, isp, false, false, null, 0, 0);

        // we set up a model that reverts the maneuvers
        final AdapterPropagator adapterPropagator = new AdapterPropagator(withManeuver);
        final AdapterPropagator.DifferentialEffect effect =
            new SmallManeuverAnalyticalModel(adapterPropagator.propagate(t0),
                dV.negate(), isp, THRUSTER);
        adapterPropagator.addEffect(effect);

        for (AbsoluteDate t = t0.shiftedBy(0.5 * dt); t.compareTo(withoutManeuver.getMaxDate()) < 0; t = t
            .shiftedBy(300.0)) {
            final PVCoordinates pvWithout = withoutManeuver.getPVCoordinates(t, heo.getFrame());
            final PVCoordinates pvReverted = adapterPropagator.getPVCoordinates(t, heo.getFrame());
            final double revertError = new PVCoordinates(pvWithout, pvReverted).getPosition().getNorm();
            Assert.assertEquals(0, revertError, 180.0);
        }

    }

    @Test
    public void testNonKeplerian() throws PatriusException, ParseException, IOException {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 9, 16), new TimeComponents(23, 11, 20.264),
            TimeScalesFactory.getUTC());

        // create EOP entries
        final EOP1980Entry eopEntry1 = new EOP1980Entry((int) (date.shiftedBy(86400 * -2).offsetFrom(
            AbsoluteDate.MODIFIED_JULIAN_EPOCH, TimeScalesFactory.getTAI()) / 86400), 0., 0., 0., 0., 0., 0.);
        final EOP1980Entry eopEntry2 = new EOP1980Entry((int) (date.shiftedBy(86400 * -1).offsetFrom(
            AbsoluteDate.MODIFIED_JULIAN_EPOCH, TimeScalesFactory.getTAI()) / 86400), 0., 0., 0., 0., 0., 0.);
        final EOP1980Entry eopEntry3 = new EOP1980Entry((int) (date.shiftedBy(86400 * 0).offsetFrom(
            AbsoluteDate.MODIFIED_JULIAN_EPOCH, TimeScalesFactory.getTAI()) / 86400), 0., 0., 0., 0., 0., 0.);
        final EOP1980Entry eopEntry4 = new EOP1980Entry((int) (date.shiftedBy(86400 * 1).offsetFrom(
            AbsoluteDate.MODIFIED_JULIAN_EPOCH, TimeScalesFactory.getTAI()) / 86400), 0., 0., 0., 0., 0., 0.);
        final EOP1980Entry eopEntry5 = new EOP1980Entry((int) (date.shiftedBy(86400 * 2).offsetFrom(
            AbsoluteDate.MODIFIED_JULIAN_EPOCH, TimeScalesFactory.getTAI()) / 86400), 0., 0., 0., 0., 0., 0.);

        // Create custom EOP loader
        final EOP1980HistoryLoader customLoader = new EOP1980HistoryLoader(){

            @Override
            public boolean stillAcceptsData() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name)
                                                                            throws IOException, ParseException,
                                                                            PatriusException {
                // TODO Auto-generated method stub

            }

            @Override
            public void fillHistory(final EOP1980History history) throws PatriusException {
                // TODO Auto-generated method stub
                // add user values
                history.addEntry(eopEntry1);
                history.addEntry(eopEntry2);
                history.addEntry(eopEntry3);
                history.addEntry(eopEntry4);
                history.addEntry(eopEntry5);
            }
        };

        // add to EOPHistoryFactory
        EOPHistoryFactory.clearEOP1980HistoryLoaders();
        EOPHistoryFactory.addEOP1980HistoryLoader(customLoader);

        final Orbit leo = new CircularOrbit(7204319.233600575, 4.434564637450575E-4, 0.0011736728299091088,
            1.7211611441767323, 5.5552084166959474, 24950.321259193086, PositionAngle.TRUE,
            FramesFactory.getEME2000(), new AbsoluteDate(new DateComponents(2003, 9, 16), new TimeComponents(23,
                11, 20.264), TimeScalesFactory.getUTC()), Constants.EIGEN5C_EARTH_MU);
        final double mass = 4093.0;
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2003, 9, 16), new TimeComponents(23, 14, 40.264),
            TimeScalesFactory.getUTC());
        final Vector3D dV = new Vector3D(0.0, 3.0, 0.0);
        final double f = 40.0;
        final double isp = 300.0;
        final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        final double dt = -(mass * vExhaust / f) * MathLib.expm1(-dV.getNorm() / vExhaust);
        // setup a specific coefficient file for gravity potential as it will also
        // try to read a corrupted one otherwise
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        final PotentialCoefficientsProvider gravityField = GravityFieldFactory.getPotentialProvider();
        final BoundedPropagator withoutManeuver =
            this.getEphemeris(leo, mass, 10, new LofOffset(leo.getFrame(), LOFType.VNC), t0,
                Vector3D.ZERO, f, isp, true, true, gravityField, 8, 8);
        final BoundedPropagator withManeuver =
            this.getEphemeris(leo, mass, 10, new LofOffset(leo.getFrame(), LOFType.VNC), t0,
                dV, f, isp, true, true, gravityField, 8, 8);

        // we set up a model that reverts the maneuvers
        final AdapterPropagator adapterPropagator = new AdapterPropagator(withManeuver);
        final SpacecraftState state0 = adapterPropagator.propagate(t0);
        final AdapterPropagator.DifferentialEffect directEffect =
            new SmallManeuverAnalyticalModel(state0, dV.negate(), isp,
                THRUSTER);
        final AdapterPropagator.DifferentialEffect derivedEffect =
            new J2DifferentialEffect(state0, directEffect, false,
                gravityField);
        adapterPropagator.addEffect(directEffect);
        adapterPropagator.addEffect(derivedEffect);

        double maxDelta = 0;
        double maxNominal = 0;
        for (AbsoluteDate t = t0.shiftedBy(0.5 * dt); t.compareTo(withoutManeuver.getMaxDate()) < 0; t = t
            .shiftedBy(600.0)) {
            final PVCoordinates pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
            final PVCoordinates pvWith = withManeuver.getPVCoordinates(t, leo.getFrame());
            final PVCoordinates pvReverted = adapterPropagator.getPVCoordinates(t, leo.getFrame());
            final double nominal = new PVCoordinates(pvWithout, pvWith).getPosition().getNorm();
            final double revertError = new PVCoordinates(pvWithout, pvReverted).getPosition().getNorm();
            maxDelta = MathLib.max(maxDelta, revertError);
            maxNominal = MathLib.max(maxNominal, nominal);
        }
        Assert.assertTrue(maxDelta < 120);
        Assert.assertTrue(maxNominal > 2800);

    }

    private BoundedPropagator getEphemeris(final Orbit orbit, final double mass, final int nbOrbits,
                                           final AttitudeProvider law, final AbsoluteDate t0, final Vector3D dV,
                                           final double f, final double isp,
                                           final boolean sunAttraction, final boolean moonAttraction,
                                           final PotentialCoefficientsProvider gravityField, final int degree,
                                           final int order)
                                                           throws PatriusException, ParseException, IOException {

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(0.), "Main");

        final TankProperty tank = new TankProperty(mass);
        builder.addPart(THRUSTER, "Main", Transform.IDENTITY);
        builder.addProperty(tank, THRUSTER);
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);

        final SpacecraftState initialState = new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(),
            orbit.getFrame()), massModel);

        // set up numerical propagator
        final double dP = 1.0;
        final double[][] tolerances = NumericalPropagator.tolerances(dP, orbit, orbit.getType());
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tolerances[0],
            tolerances[1]);
        integrator.setInitialStepSize(orbit.getKeplerianPeriod() / 100.0);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
        propagator.setMassProviderEquation(massModel);
        propagator.setAttitudeProvider(law);

        if (dV.getNorm() > 1.0e-6) {
            // set up maneuver
            final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
            final double dt = -(mass * vExhaust / f) * MathLib.expm1(-dV.getNorm() / vExhaust);
            final ContinuousThrustManeuver maneuver =
                new ContinuousThrustManeuver(t0.shiftedBy(-0.5 * dt), dt, new PropulsiveProperty(f, isp),
                    dV.normalize(), massModel, tank);
            propagator.addForceModel(maneuver);
        }

        if (sunAttraction) {
            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        }

        if (moonAttraction) {
            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        }

        if (gravityField != null) {
            propagator.addForceModel(new CunninghamAttractionModel(FramesFactory.getGTOD(false), gravityField.getAe(),
                gravityField.getMu(), gravityField.getC(degree, order, false), gravityField.getS(degree, order,
                    false)));
        }

        propagator.setEphemerisMode();
        propagator.propagate(t0.shiftedBy(nbOrbits * orbit.getKeplerianPeriod()));
        return propagator.getGeneratedEphemeris();

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
