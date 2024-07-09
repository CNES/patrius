/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:490:17/11/2015:Validation tests on the possibility to create a DragForce with multiplicative coeff k
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.drag;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.utils.PatriusSphericalSpacecraft;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.gravity.tides.OceanTides;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DragTest {
    /**
     * Parameter for normal ballistic coefficient enabling jacobian processing.<br>
     * The normal ballistic coefficient is B<sub>n</sub> = S * C<sub>n</sub> / Mass
     * where C<sub>n</sub> is the normal drag coefficient of the spacecraft and S is its cross-sectional area.
     */
    public static final Parameter C_X = new Parameter("C_X", 0.);

    /**
     * Parameter for normal ballistic coefficient enabling jacobian processing.<br>
     * The normal ballistic coefficient is B<sub>n</sub> = S * C<sub>n</sub> / Mass
     * where C<sub>n</sub> is the normal drag coefficient of the spacecraft and S is its cross-sectional area.
     */
    public static final Parameter C_N = new Parameter("C_N", 0.);

    /**
     * Parameter for tangential ballistic coefficient enabling jacobian processing.<br>
     * The normal ballistic coefficient is B<sub>t</sub> = S * C<sub>t</sub> / Mass
     * where C<sub>t</sub> is the tangential drag coefficient of the spacecraft and S is its cross-sectional area.
     */
    public static final Parameter C_T = new Parameter("C_T", 0.);

    /**
     * Parameter with default name when an instance of DragForce is created using
     * a double for multiplicative coefficient k.
     */
    public static final Parameter COEFF = new Parameter("K coefficient", 0.);

    public static final String DEFAULT = "DEFAULT";

    /** List of the parameters. */
    private static final ArrayList<Parameter> parameters;
    static {
        parameters = new ArrayList<Parameter>();
        parameters.add(C_X);
        parameters.add(C_N);
        parameters.add(C_T);
    }

    public static final IParamDiffFunction k = new IParamDiffFunction(){

        @Override
        public boolean supportsParameter(final Parameter param) {
            // boolean to status
            boolean supported = false;

            final ArrayList<Parameter> params = this.getParameters();
            final int sizeList = params.size();
            // Check all parameters to see if the parameter is used, stop if found once
            for (int i = 0; i < sizeList && !supported; i++) {
                if (params.get(i).equals(param)) {
                    supported = true;
                }
            }
            return supported;
        }

        @Override
        public ArrayList<Parameter> getParameters() {
            return parameters;
        }

        @Override
        public double value(final SpacecraftState state) {
            return parameters.get(0).getValue();
        }

        @Override
        public boolean isDifferentiableBy(final Parameter p) {
            return this.supportsParameter(p);
        }

        @Override
        public double derivativeValue(final Parameter p, final SpacecraftState s) {
            return 1.0;
        }
    };

    /** Simple atmospheric exponential model. */
    private SimpleExponentialAtmosphere atm;

    /** PV coordinates. */
    private PVCoordinates pvs;

    /** Spacecraft state. */
    private SpacecraftState state;

    @Test
    public void testExpAtmosphere() throws PatriusException {
        final Vector3D posInEME2000 = new Vector3D(10000, Vector3D.PLUS_I);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D vel = this.atm.getVelocity(date, posInEME2000, FramesFactory.getEME2000());

        final Transform toBody = FramesFactory.getEME2000().getTransformTo(FramesFactory.getITRF(), date);
        Vector3D test = Vector3D.crossProduct(toBody.getRotationRate(), posInEME2000);
        test = test.subtract(vel);
        Assert.assertEquals(0, test.getNorm(), 2.9e-5);
    }

    // test the DDragAccDParam and DDragAccDState methods.
    @Test
    public void testDDragAccDerivatives() throws PatriusException {
        // this class represents a mock DragSensitive object:
        class MockDragSensitive implements DragSensitive {

            @Override
            public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                             final Vector3D relativeVelocity)
                                                                             throws PatriusException {
                return Vector3D.ZERO;
            }

            @Override
            public
                    void
                    addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                      final Vector3D relativeVelocity, final double[] dAccdParam)
                                                                                                 throws PatriusException {
                dAccdParam[0] = 1.0;
                dAccdParam[1] = 2.0;
                dAccdParam[2] = 3.0;
            }

            @Override
            public
                    void
                    addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos,
                                      final double[][] dAccdVel, final double density,
                                      final Vector3D acceleration, final Vector3D relativeVelocity,
                                      final boolean computeGradientPosition, final boolean computeGradientVelocity)
                                                                                                                   throws PatriusException {
                dAccdPos[0][0] = 0.0;
                dAccdPos[0][1] = 0.1;
                dAccdVel[2][0] = 2.0;
                dAccdVel[1][1] = 1.1;
                dAccdVel[2][1] = 2.1;
            }

            @Override
            public ArrayList<Parameter> getJacobianParameters() {
                final ArrayList<Parameter> list = new ArrayList<Parameter>();
                for (final Parameter p : parameters) {
                    list.add(p);
                }
                return list;
            }

            /** {@inheritDoc} */
            @Override
            public DragSensitive copy(final Assembly assembly) {
                // Unused
                return null;
            }
        }

        final MockDragSensitive spacecraft = new MockDragSensitive();
        final DragForce drag = new DragForce(this.atm, spacecraft);
        final DragForce drag2 = new DragForce(k, this.atm, spacecraft);

        final double[] dAccdParam = new double[3];
        drag.addDAccDParam(this.state, C_N, dAccdParam);
        final double[] dAccdParam2 = new double[3];
        drag2.addDAccDParam(this.state, C_N, dAccdParam2);

        Assert.assertEquals(1.0, dAccdParam[0], 0.0);
        Assert.assertEquals(2.0, dAccdParam[1], 0.0);
        Assert.assertEquals(3.0, dAccdParam[2], 0.0);
        Assert.assertEquals(0.0, dAccdParam2[0], 0.0);
        Assert.assertEquals(0.0, dAccdParam2[1], 0.0);
        Assert.assertEquals(0.0, dAccdParam2[2], 0.0);

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        final double[][] dAccdPos2 = new double[3][3];
        final double[][] dAccdVel2 = new double[3][3];
        drag.addDAccDState(this.state, dAccdPos, dAccdVel);
        drag2.addDAccDState(this.state, dAccdPos2, dAccdVel2);

        Assert.assertEquals(0.0, dAccdPos[0][0], 0.0);
        Assert.assertEquals(0.1, dAccdPos[0][1], 0.0);
        Assert.assertEquals(2.0, dAccdVel[2][0], 0.0);
        Assert.assertEquals(0.0, dAccdPos2[0][0], 0.0);
        Assert.assertEquals(0.1, dAccdPos2[0][1], 0.0);
        Assert.assertEquals(2.0, dAccdVel2[2][0], 0.0);
    }

    // test the computeAcceleration methods are equivalent.
    @Test
    public void testComputeAcceleration() throws PatriusException {
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 0., 0., 0., DEFAULT);
        final DragForce drag = new DragForce(this.atm, spacecraft);
        final Vector3D acc1 = drag.computeAcceleration(this.state);
        final Vector3D acc2 =
            DragForce.computeAcceleration(this.pvs, FramesFactory.getEME2000(), this.atm, new AbsoluteDate(),
                FastMath.PI * 1.5 / 2., 1000.);

        Assert.assertEquals(acc1.getX(), acc2.getX(), 0.0);
        Assert.assertEquals(acc1.getY(), acc2.getY(), 0.0);
        Assert.assertEquals(acc1.getZ(), acc2.getZ(), 0.0);
    }

    @Test
    public void coverage() throws PatriusException {
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 0., 0., 0., DEFAULT);
        final DragForce drag = new DragForce(this.atm, spacecraft);
        final ArrayList<Parameter> res = drag.getParameters();
        final String prefix = "DragForce_";
        for (final Parameter param : res) {
            final String paramName = param.getName();
            Assert.assertTrue(paramName.equals(prefix + C_N.getName())
                    || paramName.equals(prefix + C_X.getName())
                    || paramName.equals(prefix + C_T.getName())
                    || paramName.equals(prefix + COEFF.getName()));
        }

        try {
            drag.addDAccDParam(this.state, new Parameter("hahaha", 0.), null);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }
    }

    /**
     * @throws PatriusException
     * @testType VT
     * 
     * @testedMethod {@link DragForce#computeAcceleration(SpacecraftState)}
     * 
     * @description This test aims at verifying that the multiplicative coefficient k
     *              for drag force added to DragForce object is taken into account in the acceleration
     *              computation (must be equals to k*acceleration)
     * 
     * @input SphericalSpacecraft spacecraft
     *        the spacecraft of which acceleration is computed
     * @input Parameter mult =! 1.0
     *        the multiplicative coefficient
     * 
     * @output The acceleration vector
     * @testPassCriteria Acceleration acc is computed in the case where mult = 1.0 in one hand,
     *                   the acceleration computed on DragForce object created with mult =! 1.0
     *                   on the other hand must be equals to mult * acc
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testMultiplyAcc() throws PatriusException {

        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 0., 0., 0., DEFAULT);
        final Parameter mult = new Parameter("mult coeff", 3.0);
        final DragForce drag = new DragForce(this.atm, spacecraft);
        final DragForce dragMult = new DragForce(mult, this.atm, spacecraft);

        final Vector3D acc = drag.computeAcceleration(this.state);
        final Vector3D accMult = dragMult.computeAcceleration(this.state);

        Assert.assertEquals(accMult.getX(), acc.scalarMultiply(mult.getValue()).getX(), 0.0);
        Assert.assertEquals(accMult.getY(), acc.scalarMultiply(mult.getValue()).getY(), 0.0);
        Assert.assertEquals(accMult.getZ(), acc.scalarMultiply(mult.getValue()).getZ(), 0.0);

    }

/**
     * @testType UT
     *
     * @testedMethod {@link DragForce#DragForce(org.orekit.forces.atmospheres.Atmosphere, DragSensitive, boolean, boolean))
     *
     * @description compute acceleration partial derivatives wrt position and velocity
     *
     * @input instances of {@link OceanTides}
     *
     * @output partial derivatives
     *
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction
     *
     * @throws PatriusException
     *             when an Orekit error occurs
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // DragSensitive
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(FastMath.PI, 1.5, 0., 0., 0., DEFAULT);

        // Instance
        final DragForce drag = new DragForce(this.atm, spacecraft, false, false);

        // Check that derivatives computation is deactivated
        Assert.assertFalse(drag.computeGradientPosition());
        Assert.assertFalse(drag.computeGradientVelocity());

        // Compute partial derivatives
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        drag.addDAccDState(this.state, dAccdPos, dAccdVel);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
            }
        }
    }

    /**
     * @throws PatriusException
     *         get class parameters
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
        final SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(
            new ExtendedOneAxisEllipsoid(
                Constants.CNES_STELA_AE, Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "main"), 1e-15,
            150000, 1400000);

        final DragForce dragForce =
            new DragForce(10, atm, new PatriusSphericalSpacecraft(10, 0.5, 0.3, 0.2, 0.1, "main"));
        Assert.assertEquals(10, dragForce.getMultiplicativeFactor(this.state), 0);
        Assert.assertEquals(
            atm.getSpeedOfSound(AbsoluteDate.J2000_EPOCH, new Vector3D(1000, 1500, 200), FramesFactory.getGCRF()),
            dragForce.getAtmosphere().getSpeedOfSound(AbsoluteDate.J2000_EPOCH, new Vector3D(1000, 1500, 200),
                FramesFactory.getGCRF()), 0);
        Assert.assertEquals(atm.getH0(), ((SimpleExponentialAtmosphere) dragForce.getAtmosphere()).getH0(), 0);

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final Frame itrf = FramesFactory.getITRF();

        this.atm = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Utils.ae, 1.0 / 298.257222101, itrf),
            0.0004, 42000.0, 7500.0);
        final AbsoluteDate date = new AbsoluteDate();
        // mu from grim4s4_gr model
        final double mu = 0.39860043770442e+15;

        final Vector3D pos = new Vector3D(6.65e+06, -1.18e+03, -6.59e+05);
        final Vector3D vel = new Vector3D(8.57e+02, 2.95e+03, -4.07e+01);
        this.pvs = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(this.pvs, FramesFactory.getEME2000(), date, mu);
        final MassProvider massProvider = new SimpleMassModel(1000., DEFAULT);
        this.state = new SpacecraftState(orbit, massProvider);
    }
}
