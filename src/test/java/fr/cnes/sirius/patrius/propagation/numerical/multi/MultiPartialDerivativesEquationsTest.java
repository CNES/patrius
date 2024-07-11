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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:DM:DM-2415:27/05/2020:Gestion des PartialderivativesEquations avec MultiPropagateur 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.models.utils.AssemblySphericalSpacecraft;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.CowellIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.JacobiansMapper;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.ParameterConfiguration;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 *
 **/
/**
 * <p>
 * Validation class for {@link MultiPartialDerivativesEquations}.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.5
 */
public class MultiPartialDerivativesEquationsTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Multi-sat numerical partial derivatives propagation
         * 
         * @featureDescription Test multi-sat numerical partial derivatives propagation
         * 
         * @coveredRequirements
         */
        PARTIAL_DERIVATIVES
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#PARTIAL_DERIVATIVES}
     *
     * @testedMethod all MultiPartialDerivativesEquations methods
     *
     * @description Test multi-sat numerical orbit and partial derivatives propagation. It basically checks that
     *              propagated
     *              orbit and partial derivatives are exactly the same in 5 mono-sat propagation and 1 single multi-sat
     *              propagation
     *              state transition matrix and partial derivatives wrt drag k0 are computed
     * 
     * @input MultiNumericalPropagator and its single propagators counter-parts, forcel models (Earth potential, drag,
     *        Sun, Moon)
     *
     * @output final states and partial derivatives
     *
     * @testPassCriteria final states and partial derivatives are exactly the same
     *
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testPartialDerivatives() throws PatriusException, IOException, ParseException, ClassNotFoundException {
        // STM initialized with identity and partial derivatives initialized with empty matrix
        checkPartialDerivatives(true, new ClassicalRungeKuttaIntegrator(30.), 0, 0);
        // STM and partial derivatives initialized with random matrices
        checkPartialDerivatives(false, new ClassicalRungeKuttaIntegrator(30.), 0, 0);
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#PARTIAL_DERIVATIVES}
     *
     * @testedMethod all MultiPartialDerivativesEquations methods
     *
     * @description Test multi-sat numerical orbit and partial derivatives propagation using Cowell integrator. It
     *              basically checks that propagated
     *              orbit and partial derivatives are the same in 5 mono-sat propagation and 1 single multi-sat
     *              propagation
     *              state transition matrix and partial derivatives wrt drag k0 are computed
     *              Also check that externalisation is properly performed
     * 
     * @input MultiNumericalPropagator and its single propagators counter-parts, forcel models (Earth potential, drag,
     *        Sun, Moon)
     *
     * @output final states and partial derivatives
     *
     * @testPassCriteria final states and partial derivatives are almost the same (cannot be the same since time step
     *                   depends on state vector size)
     *                   (tolerances: 7mm on position (absolute), 1E-6 on partial derivatives (relative))
     *
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testPartialDerivativesCowell()
        throws PatriusException, IOException, ParseException, ClassNotFoundException {
        // STM initialized with identity and partial derivatives initialized with empty matrix
        checkPartialDerivatives(true, new CowellIntegrator(9, 1E-10, 1E-10), 0.007, 1E-6);
    }

    @Test
    /**
     * Evaluate methods
     * {@link MultiPartialDerivativesEquations#selectParamAndStep(List<Parameter>) selectParamAndStep(List<Parameter>)}
     * {@link MultiPartialDerivativesEquations#contains(Parameter) contains(Parameter)}
     * {@link MultiPartialDerivativesEquations#cleanSelectedParameters() cleanSelectedParameters()}
     * {@link MultiPartialDerivativesEquations#getSelectedParameters() getSelectedParameters()}
     */
    public void testCoverage() {
        final MultiPartialDerivativesEquations multiEquations = new MultiPartialDerivativesEquations();
        final Parameter parameter = new Parameter("k0", 0.1);
        multiEquations.selectParamAndStep(parameter, Double.NaN);
        Assert.assertTrue(multiEquations.contains(parameter));
        multiEquations.clearSelectedParameters();
        final List<ParameterConfiguration> parameters = multiEquations.getSelectedParameters();
        Assert.assertEquals(0, parameters.size());
    }

    /**
     * Perform partial derivatives propagation.
     * 
     * @param integrator integrator to use
     * @param tol tolerance on position
     * @param tolPD relative tolerance on partial derivatives
     * @param isDefault true if partial derivatives are initialized with default value (STM = identity matrix and PD =
     *        empty matrix)
     */
    public void checkPartialDerivatives(final boolean isDefault,
                                        final FirstOrderIntegrator integrator,
                                        final double tol,
                                        final double tolPD)
        throws PatriusException, IOException, ParseException, ClassNotFoundException {

        // PRS and adaptive step-size integrator lead to different results (normal)
        // - PRS: because eclipse events lead to reinitializing state
        // - DOPRI integrator: because error computation depends on state vector size

        // ====================== Initialization ======================

        Utils.setDataRoot("regular-dataPBASE");
        final int nPropagators = 3;

        // Mass providers
        final MassProvider[] massProviders1 = new MassProvider[nPropagators];
        final MassProvider[] massProviders2 = new MassProvider[nPropagators];
        final TankProperty[] tank1 = new TankProperty[nPropagators];
        final TankProperty[] tank2 = new TankProperty[nPropagators];

        for (int i = 0; i < nPropagators; i++) {
            final AssemblyBuilder builder1 = new AssemblyBuilder();
            builder1.addMainPart("Main" + i);
            builder1.addProperty(new MassProperty(1000.), "Main" + i);
            tank1[i] = new TankProperty(1000.);
            builder1.addPart("Tank" + i, "Main" + i, Transform.IDENTITY);
            builder1.addProperty(tank1[i], "Tank" + i);
            final Assembly assembly1 = builder1.returnAssembly();
            massProviders1[i] = new MassModel(assembly1);

            final AssemblyBuilder builder2 = new AssemblyBuilder();
            builder2.addMainPart("Main2" + i);
            builder2.addProperty(new MassProperty(1000.), "Main2" + i);
            tank2[i] = new TankProperty(1000.);
            builder2.addPart("Tank2" + i, "Main2" + i, Transform.IDENTITY);
            builder2.addProperty(tank2[i], "Tank2" + i);
            final Assembly assembly2 = builder2.returnAssembly();
            massProviders2[i] = new MassModel(assembly2);
        }

        final NumericalPropagator[] propagators = new NumericalPropagator[nPropagators];
        for (int i = 0; i < nPropagators; i++) {
            propagators[i] = new NumericalPropagator(integrator);
            if (integrator instanceof CowellIntegrator) {
                propagators[i].setOrbitType(OrbitType.CARTESIAN);
                propagators[i].setEphemerisMode();
            } else {
                propagators[i].setOrbitType(OrbitType.KEPLERIAN);
            }
        }

        final MultiNumericalPropagator multiNumericalPropagator = new MultiNumericalPropagator(
            integrator);
        if (integrator instanceof CowellIntegrator) {
            multiNumericalPropagator.setOrbitType(OrbitType.CARTESIAN);
            multiNumericalPropagator.setEphemerisMode();
        } else {
            multiNumericalPropagator.setOrbitType(OrbitType.KEPLERIAN);
        }

        // Parameters (drag coefficient)
        final Parameter[] k0 = new Parameter[nPropagators];
        for (int i = 0; i < k0.length; i++) {
            k0[i] = new Parameter("k0" + i, 1. + i / 10.);
        }

        // Initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2002, 01, 02, TimeScalesFactory.getTAI());
        final AttitudeProvider attitudeProvider = new ConstantAttitudeLaw(FramesFactory.getCIRF(),
            Rotation.IDENTITY);
        final PartialDerivativesEquations[] equations = new PartialDerivativesEquations[nPropagators];
        final MultiPartialDerivativesEquations[] multiEquations = new MultiPartialDerivativesEquations[nPropagators];
        for (int i = 0; i < nPropagators; i++) {
            final double mu = Constants.EGM96_EARTH_MU + i * 1E10;
            final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0.001, 1.5, 0, 0, i * 10,
                PositionAngle.MEAN, FramesFactory.getGCRF(), initialDate, mu);
            final SpacecraftState initialState1 = new SpacecraftState(initialOrbit,
                massProviders1[i]);
            final SpacecraftState initialState2 = new SpacecraftState(initialOrbit,
                massProviders2[i]);

            // Partial derivatives equations (state transition matrix + drag k0)
            equations[i] = new PartialDerivativesEquations("", propagators[i]);
            multiEquations[i] = new MultiPartialDerivativesEquations("equations" + i, multiNumericalPropagator, "state"
                    + i);
            final List<Parameter> list = new ArrayList<>();
            list.add(k0[i]);
            equations[i].selectParameters(list);
            multiEquations[i].selectParameters(list);

            // Initialized STM and partial derivatives with default values
            SpacecraftState monoState = equations[i].setInitialJacobians(initialState1);
            SpacecraftState multiState = multiEquations[i].setInitialJacobians(initialState2);
            if (!isDefault) {
                // Non-default case: add some "random" values
                // STM
                final double[][] dYdX0 = new double[6][6];
                for (int j = 0; j < dYdX0.length; j++) {
                    dYdX0[j][j] = 1.;
                    // Add non-identity component
                    dYdX0[0][j] = 1.;
                }
                monoState = equations[i].setInitialJacobians(monoState, dYdX0);
                multiState = multiEquations[i].setInitialJacobians(multiState, dYdX0);

                // Partial derivatives
                final double[] dYdP0 = new double[6];
                // Add component
                dYdP0[0] = 1.;
                monoState = equations[i].setInitialJacobians(monoState, k0[i], dYdP0);
                multiState = multiEquations[i].setInitialJacobians(multiState, k0[i], dYdP0);

                // Derivation step
                equations[i].setSteps(0.001);
                multiEquations[i].setSteps(0.001);
            }
            propagators[i].setInitialState(monoState);
            multiNumericalPropagator.addInitialState(multiState, "state" + i);

            // Others
            propagators[i].setAttitudeProvider(attitudeProvider);
            propagators[i].setMassProviderEquation(massProviders1[i]);
            multiNumericalPropagator.setAttitudeProvider(attitudeProvider, "state" + i);
            multiNumericalPropagator.setMassProviderEquation(massProviders2[i], "state" + i);
        }

        // Force models (some depends on spacecraft)
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
            FramesFactory.getITRF());

        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(60, 60, false);
        final double[][] S = provider.getS(60, 60, false);
        final ForceModel earthPotential = new DrozinerAttractionModel(FramesFactory.getITRF(),
            provider.getAe(), provider.getMu(), C, S);

        final ForceModel sunAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
        final ForceModel moonAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());

        for (int i = 0; i < nPropagators; i++) {
            final AssemblySphericalSpacecraft spacecraft = new AssemblySphericalSpacecraft(10.,
                2.2, 0.3, 0., 0., "Main_" + i);
            final DragForce drag = new DragForce(k0[i], new SimpleExponentialAtmosphere(earth, 0.0004,
                42000.0, 7500.0), spacecraft);

            propagators[i].addForceModel(earthPotential);
            propagators[i].addForceModel(sunAttraction);
            propagators[i].addForceModel(moonAttraction);
            propagators[i].addForceModel(drag);

            multiNumericalPropagator.addForceModel(earthPotential, "state" + i);
            multiNumericalPropagator.addForceModel(sunAttraction, "state" + i);
            multiNumericalPropagator.addForceModel(moonAttraction, "state" + i);
            multiNumericalPropagator.addForceModel(drag, "state" + i);
        }

        // ====================== Propagation (on 3h) ======================

        final AbsoluteDate finalDate = initialDate.shiftedBy(3600. * 3.);

        // n stand-alone propagations
        final List<SpacecraftState> res1 = new ArrayList<>();
        for (int i = 0; i < nPropagators; i++) {
            res1.add(propagators[i].propagate(finalDate));
        }

        // 1 multi-propagation
        final Map<String, SpacecraftState> res2 = multiNumericalPropagator.propagate(finalDate);

        // ====================== Check ======================

        // Check final state
        for (int i = 0; i < nPropagators; i++) {
            // State
            final SpacecraftState state1 = res1.get(i);
            final SpacecraftState state2 = res2.get("state" + i);
            Assert.assertEquals(state1.getDate().durationFrom(state2.getDate()), 0., 0.);
            final PVCoordinates pv1 = state1.getPVCoordinates();
            final PVCoordinates pv2 = state2.getPVCoordinates();
            Assert.assertEquals(pv1.getPosition().getX(), pv2.getPosition().getX(), tol);
            Assert.assertEquals(pv1.getPosition().getY(), pv2.getPosition().getY(), tol);
            Assert.assertEquals(pv1.getPosition().getZ(), pv2.getPosition().getZ(), tol);

            // Partial derivatives
            final JacobiansMapper monoMapper = equations[i].getMapper();
            final JacobiansMapper multiMapper = multiEquations[i].getMapper();
            final double[][] monodXdX0 = monoMapper.getStateJacobian(state1);
            final double[][] multidXdX0 = multiMapper.getStateJacobian(state2);
            for (int j = 0; j < monodXdX0.length; j++) {
                for (int k = 0; k < monodXdX0[j].length; k++) {
                    final double relDiff = MathLib.abs((monodXdX0[j][k] - multidXdX0[j][k]) / monodXdX0[j][k]);
                    Assert.assertEquals(0, relDiff, tolPD);
                }
            }
            final double[][] monodXdP = monoMapper.getParametersJacobian(state1);
            final double[][] multidXdP = multiMapper.getParametersJacobian(state2);
            for (int j = 0; j < monodXdP.length; j++) {
                for (int k = 0; k < multidXdP[j].length; k++) {
                    final double relDiff = MathLib.abs((monodXdP[j][k] - multidXdP[j][k]) / monodXdP[j][k]);
                    Assert.assertEquals(0, relDiff, tolPD);
                }
            }
        }

        // Check externalization - Cowell propagator only
        if (integrator instanceof CowellIntegrator) {
            for (int i = 0; i < nPropagators; i++) {
                checkExternalization(multiNumericalPropagator.getGeneratedEphemeris("state" + i), i);
            }
        }

        // Coverage of various methods
        for (int i = 0; i < nPropagators; i++) {
            Assert.assertEquals("equations" + i, multiEquations[i].getName());
            for (int j = 0; j < equations[i].getAvailableParameters().size(); j++) {
                Assert.assertEquals(equations[i].getAvailableParameters().get(i).getName(), multiEquations[i]
                    .getAvailableParameters().get(i).getName());
                Assert.assertEquals(equations[i].getAvailableParameters().get(i).getValue(), multiEquations[i]
                    .getAvailableParameters().get(i).getValue());
            }
        }

        // Exceptions
        // Un-initialized jacobian
        final MultiPartialDerivativesEquations multiEquationsException = new MultiPartialDerivativesEquations("",
            multiNumericalPropagator, "state0");
        try {
            multiEquationsException.getMapper();
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Inconsistent matrix sizes
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000E3, 0.001, 1.5, 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getGCRF(), initialDate, Constants.EGM96_EARTH_MU));
        try {
            multiEquationsException.setInitialJacobians(state, new double[7][7], new double[6][1]);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            multiEquationsException.setInitialJacobians(state, new double[6][6], new double[7][1]);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        // Wrong initialization (unknown parameter)
        try {
            multiEquationsException.selectParameters(new ArrayList<Parameter>(){
                /** Serializable UID. */
                private static final long serialVersionUID = -3025922814020836915L;

                {
                    add(new Parameter("", 0));
                }
            });
            multiEquationsException.computeDerivatives(state, null);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        // Inconsistent matrix sizes
        try {
            multiEquationsException.selectParameters(new ArrayList<Parameter>(){
                /** Serializable UID. */
                private static final long serialVersionUID = -5885554058495698524L;

                {
                    add(k0[0]);
                }
            });
            multiEquationsException.setInitialJacobians(state, new double[6][6], new double[6][2]);
            multiEquationsException.computeDerivatives(state, null);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Check provided ephemeris once writen (externalized) and read returns the exact same ephemeris.
     * 
     * @param ephemeris ephemeris to write
     * @param id current sat ID to check
     */
    private static void checkExternalization(final BoundedPropagator ephemeris, final int id)
        throws IOException, ClassNotFoundException, PatriusException {

        // Store ephemeris in memory
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        // Read ephemeris
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final BoundedPropagator output = (BoundedPropagator) ois.readObject();

        // Check read ephemeris vs initial ephemeris (should be the same)
        AbsoluteDate currentDate = ephemeris.getMinDate();
        while (currentDate.compareTo(ephemeris.getMaxDate()) <= 0) {
            final SpacecraftState refState = ephemeris.propagate(currentDate);
            final SpacecraftState actState = output.propagate(currentDate);
            final Vector3D ref = refState.getPVCoordinates().getPosition();
            final Vector3D act = actState.getPVCoordinates().getPosition();
            Assert.assertEquals(refState.getMass("Tank2" + id), actState.getMass("Tank2" + id), 0.);
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 7; j++) {
                    check(refState.getAdditionalState("equations" + id)[i + j * 6],
                        actState.getAdditionalState("equations" + id)[i + j * 6], 0);
                }
            }
            Assert.assertEquals(0., ref.distance(act), 0.);
            currentDate = currentDate.shiftedBy(10.);
        }
    }

    /**
     * Check two values vs relative tolerance.
     * 
     * @param value1 1st value
     * @param value2 2nd value
     * @param tol relative tolerance
     */
    private static void check(final double value1, final double value2, final double tol) {
        if (value1 == 0) {
            Assert.assertEquals(value1, value2, tol);
        } else {
            Assert.assertEquals(0., (value2 - value1) / value1, tol);
        }
    }
}
