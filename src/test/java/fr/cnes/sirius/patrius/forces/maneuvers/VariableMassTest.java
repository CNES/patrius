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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.InertiaComputedModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.InertiaSimpleProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassEquation;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for variable mass models
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public class VariableMassTest {

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * other part's name
     */
    private final String part2 = "part2";

    /** Features description. */
    public enum features {

        /**
         * @featureTitle maneuvers
         * 
         * @featureDescription test maneuvers impact on mass
         * 
         * @coveredRequirements DV-PROPU_110, DV-PROPU_120, DV-PROPU_130
         */
        MANEUVERS_VALIDATION
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MANEUVERS_VALIDATION}
     * 
     * @testedMethod {@link MassModel#addMassDerivative(double)}
     * @testedMethod {@link MassEquation#addMassDerivative(double)}
     * @testedMethod {@link MassEquation#computeDerivatives(SpacecraftState, org.orekit.propagation.numerical.TimeDerivativesEquations, double[])}
     * 
     * @description tests the use of independent fuel tanks
     * 
     * @input some continuous maneuvers
     * 
     * @output masses of reservoir
     * 
     * @testPassCriteria masses of reservoirs are as expected, to 1e-14
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testMassModel() throws PatriusException {

        // -------------------- ASSEMBLY RADIATIVE WRENCH MODEL ---------------- //

        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        // inertia simple prop
        final TankProperty mp = new TankProperty(1500);
        builder.addProperty(mp, this.mainBody);
        final TankProperty mp2 = new TankProperty(1500);
        builder.addProperty(mp2, this.part2);

        // assembly creation
        final Assembly assembly = builder.returnAssembly();
        final MassModel massProvider = new MassModel(assembly);

        // --------------------------------- A SPACECRAFT STATE ---------------------- //

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.00001, MathLib.toRadians(75), .5, 0, .2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // spacecraft
        final SpacecraftState state = new SpacecraftState(orbit, massProvider);

        // --------------------------------- Maneuver -------------- //

        final double f = 80;
        final AbsoluteDate t0 = date.shiftedBy(1000.);
        final double dt = 150;
        final double isp = 300;
        final Vector3D direction = new Vector3D(.2, .5, -.3);

        final ContinuousThrustManeuver man =
            new ContinuousThrustManeuver(t0, dt, new PropulsiveProperty(f, isp), direction, massProvider, mp2);
        final ContinuousThrustManeuver man2 =
            new ContinuousThrustManeuver(t0.shiftedBy(500), 350, new PropulsiveProperty(f, 250),
                direction.negate(), massProvider, mp2);

        // --------------------------------- Propagator -------------- //
        final double[] absTol = { 1e-6, 1e-6, 1e-6, 1e-9, 1e-9, 1e-9 };
        final double[] relTol = { 1e-9, 1e-9, 1e-9, 1e-12, 1e-12, 1e-12 };
        final DormandPrince853Integrator integ = new DormandPrince853Integrator(.01, 60, absTol, relTol);
        final NumericalPropagator prop = new NumericalPropagator(integ);

        prop.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        prop.setInitialState(state);
        prop.addForceModel(man);
        prop.addForceModel(man2);

        prop.setMassProviderEquation(massProvider);

        // System.out.println(massProvider.getTotalMass() + "   =   " + mp.getMass() + "   +   " + mp2.getMass());

        final SpacecraftState result = prop.propagate(t0.shiftedBy(2000.));

        // System.out.println(massProvider.getTotalMass() + "   =   " + mp.getMass() + "   +   " + mp2.getMass());
        // System.out.println(result.getMass("default"));

        Assert.assertEquals(0,
            ((result.getMass("mainBody") + (result.getMass("part2")) - massProvider.getTotalMass()) / (result
                .getMass("mainBody") + (result.getMass("part2")))), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertTrue(MathLib.abs(mp.getMass() - mp2.getMass()) > Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MANEUVERS_VALIDATION}
     * 
     * @testedMethod {@link MassModel#addMassDerivative(double)}
     * @testedMethod {@link MassEquation#addMassDerivative(double)}
     * @testedMethod {@link MassEquation#computeDerivatives(SpacecraftState, org.orekit.propagation.numerical.TimeDerivativesEquations, double[])}
     * 
     * @description tests the use of independent fuel tanks
     * 
     * @input some continuous maneuvers
     * 
     * @output masses of reservoir
     * 
     * @testPassCriteria masses of reservoirs are as expected, to 1e-14
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     */
    @Test
    public void testInertiaModel() throws PatriusException {

        // -------------------- ASSEMBLY RADIATIVE WRENCH MODEL ---------------- //

        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        // inertia simple prop
        final Matrix3D id = new Matrix3D(new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } });

        final TankProperty mp1 = new TankProperty(1500);
        builder.addProperty(mp1, this.mainBody);
        final InertiaSimpleProperty prop1 = new InertiaSimpleProperty(Vector3D.MINUS_J, id, mp1.getMassProperty());
        builder.addProperty(prop1, this.mainBody);

        final TankProperty mp2 = new TankProperty(1500);
        builder.addProperty(mp2, this.part2);
        final InertiaSimpleProperty prop2 = new InertiaSimpleProperty(Vector3D.MINUS_J, id, mp2.getMassProperty());
        builder.addProperty(prop2, this.part2);

        // assembly creation
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massProvider = new InertiaComputedModel(assembly);

        // --------------------------------- A SPACECRAFT STATE ---------------------- //

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.00001, MathLib.toRadians(75), .5, 0, .2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // spacecraft
        final SpacecraftState state = new SpacecraftState(orbit, massProvider);

        // --------------------------------- Maneuver -------------- //

        final double f = 80;
        final AbsoluteDate t0 = date.shiftedBy(1000.);
        final double dt = 150;
        final double isp = 300;
        final Vector3D direction = new Vector3D(.2, .5, -.3);

        final ContinuousThrustManeuver man =
            new ContinuousThrustManeuver(t0, dt, new PropulsiveProperty(f, isp), direction, massProvider, mp2);
        final ContinuousThrustManeuver man2 =
            new ContinuousThrustManeuver(t0.shiftedBy(500), 350, new PropulsiveProperty(f, 250),
                direction.negate(), massProvider, mp2);

        // --------------------------------- Propagator -------------- //
        final double[] absTol = { 1e-6, 1e-6, 1e-6, 1e-9, 1e-9, 1e-9 };
        final double[] relTol = { 1e-9, 1e-9, 1e-9, 1e-12, 1e-12, 1e-12 };
        final DormandPrince853Integrator integ = new DormandPrince853Integrator(.01, 60, absTol, relTol);
        final NumericalPropagator prop = new NumericalPropagator(integ);

        prop.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        prop.setInitialState(state);
        prop.addForceModel(man);
        prop.addForceModel(man2);

        prop.setMassProviderEquation(massProvider);

        // System.out.println("before   : " + massProvider.getTotalMass() + "   =   " + prop1.getMass() + "   +   " +
        // prop2.getMass());

        final SpacecraftState result = prop.propagate(t0.shiftedBy(2000.));

        // System.out.println("after    : " + massProvider.getTotalMass() + "   =   " + prop1.getMass() + "   +   " +
        // prop2.getMass());

        Assert.assertEquals(0,
            ((result.getMass("mainBody") + (result.getMass("part2")) - massProvider.getTotalMass()) / (result
                .getMass("mainBody") + (result.getMass("part2")))), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertTrue(MathLib.abs(mp1.getMass() - mp2.getMass()) > Precision.DOUBLE_COMPARISON_EPSILON);

        Assert.assertEquals(0, (mp1.getMass() - prop1.getMass()) / mp1.getMass(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (mp2.getMass() - prop2.getMass()) / mp2.getMass(), Precision.DOUBLE_COMPARISON_EPSILON);

    }

}
