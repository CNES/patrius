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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2902:18/05/2021:Anomalie dans la gestion du JacobiansMapper
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Renamed Droziner to UnnormalizedDroziner
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::DM:441:12/05/2015:add methods to set and retrieve partial derivatives
 * VERSION::DM:483:20/10/2015: Modification of signature of some methods JaccobianMapper
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::FA:1851:18/10/2018:Update the massModel from a SimpleMassModel to an Assembly builder
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

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
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.numerical.JacobiansMapper;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class PartialDerivativesTest {

    /**
     * Check that returned Jacobian at initialization is identity even if propagation frame is different from state
     * frame.
     * 
     * @throws PatriusException
     */
    @Test
    public void testFT2902() throws PatriusException {
        // Integrator 
        final DormandPrince853Integrator integ = new DormandPrince853Integrator(0.0, 300, 1e-5, 1e-8); 
         
        // Propagator 
        final NumericalPropagator prop = new NumericalPropagator(integ); 
        prop.setOrbitFrame(FramesFactory.getGCRF()); 
                 
        // Initial orbit 
        final Orbit orbitGcrf = new KeplerianOrbit(7000.0e3, 0.01, (98 * Math.PI) / 180, 0, 0, 0, PositionAngle.TRUE, 
                        FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.GRIM5C1_EARTH_MU); 
        final Orbit orbitITRF = OrbitType.CARTESIAN.convertOrbit(orbitGcrf, FramesFactory.getITRF()); 
        final SpacecraftState initState = new SpacecraftState(orbitITRF); 
         
        // PartialDerivativesEquation 
        final PartialDerivativesEquations partials = new PartialDerivativesEquations("PDE", prop); 
        final SpacecraftState initialStateWithAdditionalStates = partials.setInitialJacobians(initState); 
         
        final JacobiansMapper mapper1 = partials.getMapper(); 
        prop.setInitialState(initialStateWithAdditionalStates); 
         
        final SpacecraftState propState = prop.propagate(AbsoluteDate.J2000_EPOCH); 

        // Check matrix is identity
        final double[][] res = mapper1.getStateJacobian(propState);
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(1., res[i][i], 1E-14);
            for (int j = 0; j < res.length; j++) {
                if (i != j) {
                    Assert.assertEquals(0., res[i][j], 1E-9);
                }
            }
        }
    }

    @Test
    public void testJacobianIssue18() throws PatriusException {

        // Body mu
        final double mu = 3.9860047e14;

        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = MathLib.toRadians(7);
        final double omega = MathLib.toRadians(180);
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final Parameter fParam = new Parameter("f", f);
        final Parameter flowRateParam = new Parameter("FLOW_RATE", -f
            / (Constants.G0_STANDARD_GRAVITY * isp));
        final double delta = MathLib.toRadians(-7.4978);
        final double alpha = MathLib.toRadians(351);
        final AttitudeProvider law = new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            (new Rotation(Vector3D.PLUS_I, new Vector3D(alpha, delta))));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
            new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
            FramesFactory.getEME2000(), initDate, mu);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(1000.), "Main");

        final TankProperty tankProp = new TankProperty(mass);
        builder.addPart("Tank", "Main", Transform.IDENTITY);
        builder.addProperty(tankProp, "Tank");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massModel = new MassModel(assembly);

        SpacecraftState initialState = new SpacecraftState(orbit, law.getAttitude(orbit,
            orbit.getDate(), orbit.getFrame()), massModel);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
            new TimeComponents(04, 15, 34.080), TimeScalesFactory.getUTC());
        // NB: the part name in the maneuver should refer to an existent spacecraft part, with a
        // mass!
        final PropulsiveProperty engineProp = new PropulsiveProperty(fParam, new Parameter("ISP",
            -fParam.getValue() / (Constants.G0_STANDARD_GRAVITY * flowRateParam.getValue())));
        final ContinuousThrustManeuver maneuver = new ContinuousThrustManeuver(fireDate, duration,
            engineProp, Vector3D.PLUS_I, massModel, tankProp);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000,
            absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);
        propagator.setMassProviderEquation(massModel);

        propagator.setOrbitType(OrbitType.CARTESIAN);
        final PartialDerivativesEquations pde = new PartialDerivativesEquations("derivatives",
            propagator);
        pde.selectParamAndStep(fParam, Double.NaN);
        // NB: don't forget to set the initial state with partial derivatives in the propagator!
        initialState = pde.setInitialJacobians(initialState);
        propagator.setInitialState(initialState);

        final AbsoluteDate finalDate = fireDate.shiftedBy(3800);
        final SpacecraftState finalorb = propagator.propagate(finalDate);
        Assert.assertEquals(0, finalDate.durationFrom(finalorb.getDate()), 1.0e-11);
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
