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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:305:17/10/2014:Partial derivatives of constant thrust error wrt thrust parameters
 * VERSION::DM:284:06/01/2015:New architecture for parameterizable Parameters
 * VERSION::FA:452:12/06/2015:Partial derivatives computation only when firing
 * VERSION::FA:462:12/06/2015:Thrust error frame defined with a LOF type
 * VERSION::FA:500:22/09/2015:New management of frames in acceleration computation
 * VERSION::FA:487:06/11/2015:Start/Stop maneuver correction
 * VERSION::DM:424:10/11/2015:Event detectors for maneuvers start and end
 * VERSION::FA:453:13/11/2015:Handling propagation starting during a maneuver
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::FA:1851:18/10/2018:Update the massModel from a SimpleMassModel to an Assembly builder for some tests
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.ApsideDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.detectors.NthOccurrenceDetector;
import fr.cnes.sirius.patrius.events.detectors.NullMassDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.maneuvers.ConstantThrustError;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Tests for the ConstantThrustError class.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 2.3
 * 
 */
public class ConstantThrustErrorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle ConstantThrustError test
         * 
         * @featureDescription tests the ConstantThrustError model
         * 
         * @coveredRequirements
         */
        CONSTANT_THRUST_ERROR
    }

    /** The zero value date. */
    AbsoluteDate date0;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, IParamDiffFunction, IParamDiffFunction, IParamDiffFunction)
     *               )}
     * 
     * @description Test the constant thrust error class when the fx, fy, fz functions are IParamDiffFunction objects
     * 
     * @input a constant thrust error class
     * 
     * @output the value of the constant thrust error at a date t and its derivative wrt to the thrust parameters
     * 
     * @testPassCriteria the output values are the expected ones
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testParamDiffFunction() throws PatriusException {
        final Parameter param1 = new Parameter("parameter1", 5.0);
        final Parameter param2 = new Parameter("parameter2", -3.0);
        // The IParamDiffFunction for the x component of the constant thrust error:
        final IParamDiffFunction functionx = new IParamDiffFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6877995896899808264L;

            @Override
            public double value(final SpacecraftState state) {
                final double x =
                    state.getDate().durationFrom(ConstantThrustErrorTest.this.date0) / (3600.0 * 24) * FastMath.PI;
                return param1.getValue() * MathLib.sin(x) + param2.getValue() * MathLib.cos(x);
            }

            @Override
            public boolean supportsParameter(final Parameter param) {
                if (param.getName().equals(param1.getName()) || param.getName().equals(param2.getName())) {
                    return true;
                }
                return false;
            }

            @Override
            public ArrayList<Parameter> getParameters() {
                final ArrayList<Parameter> list = new ArrayList<>();
                list.add(param1);
                list.add(param2);
                return list;
            }

            @Override
            public double derivativeValue(final Parameter p, final SpacecraftState state) {
                final double x =
                    state.getDate().durationFrom(ConstantThrustErrorTest.this.date0) / (3600.0 * 24) * FastMath.PI;
                if (p.getName().equals(param1.getName())) {
                    return MathLib.sin(x);
                } else if (p.getName().equals(param2.getName())) {
                    return MathLib.cos(x);
                }
                return 0.0;
            }

            @Override
            public boolean isDifferentiableBy(final Parameter p) {
                return false;
            }
        };

        // The IParamDiffFunction for the y component of the constant thrust error:
        final IParamDiffFunction functiony = new IParamDiffFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7967002494148842126L;

            @Override
            public double value(final SpacecraftState state) {
                final double x =
                    state.getDate().durationFrom(ConstantThrustErrorTest.this.date0) / (3600.0 * 24) * FastMath.PI;
                return 2.0 * param1.getValue() * MathLib.sin(x);
            }

            @Override
            public boolean supportsParameter(final Parameter param) {
                if (param.getName().equals(param1.getName())) {
                    return true;
                }
                return false;
            }

            @Override
            public ArrayList<Parameter> getParameters() {
                final ArrayList<Parameter> list = new ArrayList<>();
                list.add(param1);
                return list;
            }

            @Override
            public double derivativeValue(final Parameter p, final SpacecraftState state) {
                if (p.getName().equals(param1.getName())) {
                    final double x =
                        state.getDate().durationFrom(ConstantThrustErrorTest.this.date0) / (3600.0 * 24) * FastMath.PI;
                    return 2.0 * MathLib.sin(x);
                }
                return 0.0;
            }

            @Override
            public boolean isDifferentiableBy(final Parameter p) {
                return false;
            }
        };

        // The IParamDiffFunction for the z component of the constant thrust error:
        final IParamDiffFunction functionz = new IParamDiffFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8842622542849111022L;

            @Override
            public double value(final SpacecraftState state) {
                final double x =
                    state.getDate().durationFrom(ConstantThrustErrorTest.this.date0) / (3600.0 * 24) * FastMath.PI;
                return 2.0 * param2.getValue() * MathLib.cos(x);
            }

            @Override
            public boolean supportsParameter(final Parameter param) {
                if (param.getName().equals(param2.getName())) {
                    return true;
                }
                return false;
            }

            @Override
            public ArrayList<Parameter> getParameters() {
                final ArrayList<Parameter> list = new ArrayList<>();
                list.add(param2);
                return list;
            }

            @Override
            public double derivativeValue(final Parameter p, final SpacecraftState state) {
                if (p.getName().equals(param2.getName())) {
                    final double x =
                        state.getDate().durationFrom(ConstantThrustErrorTest.this.date0) / (3600.0 * 24) * FastMath.PI;
                    return 2.0 * MathLib.cos(x);
                }
                return 0.0;
            }

            @Override
            public boolean isDifferentiableBy(final Parameter p) {
                return false;
            }
        };

        final ConstantThrustError errorModel =
            new ConstantThrustError(this.date0, 3700.0 * 24, FramesFactory.getEME2000(),
                functionx, functiony, functionz);

        ArrayList<Parameter> list = errorModel.getParameters();
        // check that 2 parameters are associated with the model:
        Assert.assertEquals(2, list.size());
        AbsoluteDate date = this.date0.shiftedBy((3600.0 * 24));
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), date, Constants.EGM96_EARTH_MU);
        final SpacecraftState s = new SpacecraftState(orbit);

        final EventDetector[] detectors = errorModel.getEventsDetectors();
        detectors[0].eventOccurred(s, true, true);
        detectors[0].resetState(s);

        Vector3D acc = errorModel.computeAcceleration(s);
        double expx = -param2.getValue();
        Assert.assertEquals(expx, acc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        double expy = 0.0;
        Assert.assertEquals(expy, acc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        double expz = -2.0 * param2.getValue();
        Assert.assertEquals(expz, acc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        double[] dAccdParam = new double[3];
        errorModel.addDAccDParam(s, param1, dAccdParam);
        Vector3D dacc = new Vector3D(dAccdParam);
        double expdx = 0.0;
        Assert.assertEquals(expdx, dacc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        double expdy = 0.0;
        Assert.assertEquals(expdy, dacc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        double expdz = 0.0;
        Assert.assertEquals(expdz, dacc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
        errorModel.addDAccDParam(s, param2, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        expdx = -1.0;
        Assert.assertEquals(expdx, dacc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        expdy = 0.0;
        Assert.assertEquals(expdy, dacc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        expdz = -2.0;
        Assert.assertEquals(expdz, dacc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // same test with a LOF frame
        final ConstantThrustError errorModel2 = new ConstantThrustError(this.date0, 3700.0 * 24, LOFType.QSW,
            functionx, functiony, functionz);

        list = errorModel2.getParameters();
        // check that 2 parameters are associated with the model:
        Assert.assertEquals(2, list.size());
        date = this.date0.shiftedBy((3600.0 * 24));
        final Transform eme2000ToLOF = LOFType.QSW.transformFromInertial(date, orbit.getPVCoordinates());

        final EventDetector[] detectors2 = errorModel2.getEventsDetectors();
        detectors2[0].eventOccurred(s, true, true);
        detectors2[0].resetState(s);

        acc = eme2000ToLOF.transformVector(errorModel2.computeAcceleration(s));
        expx = -param2.getValue();
        Assert.assertEquals(expx, acc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        expy = 0.0;
        Assert.assertEquals(expy, acc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        expz = -2.0 * param2.getValue();
        Assert.assertEquals(expz, acc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        errorModel2.addDAccDParam(s, param1, dAccdParam);
        dacc = eme2000ToLOF.transformVector(new Vector3D(dAccdParam));
        expdx = 0.0;
        Assert.assertEquals(expdx, dacc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        expdy = 0.0;
        Assert.assertEquals(expdy, dacc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        expdz = 0.0;
        Assert.assertEquals(expdz, dacc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
        errorModel2.addDAccDParam(s, param2, dAccdParam);
        dacc = eme2000ToLOF.transformVector(new Vector3D(dAccdParam));
        expdx = -1.0;
        Assert.assertEquals(expdx, dacc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        expdy = 0.0;
        Assert.assertEquals(expdy, dacc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        expdz = -2.0;
        Assert.assertEquals(expdz, dacc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, Parameter, Parameter, Parameter, Parameter, Parameter, Parameter, AbsoluteDate)}
     * 
     * @description Test the constant thrust error class when the fx, fy, fz functions are linear functions
     * 
     * @input a constant thrust error class
     * 
     * @output the value of the constant thrust error at a date t and its derivative wrt to the thrust parameters
     * 
     * @testPassCriteria the output values are the expected ones
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testLinearFunction() throws PatriusException {
        final Parameter slopex = new Parameter("slopex", 4.5);
        final Parameter slopey = new Parameter("slopey", -7.0);
        final Parameter slopez = new Parameter("slopez", -0.5);
        final Parameter zerox = new Parameter("zerox", 10.0);
        final Parameter zeroy = new Parameter("zeroy", -0.01);
        final Parameter zeroz = new Parameter("zeroz", 1.0);
        final ConstantThrustError errorModel = new ConstantThrustError(this.date0, 1000.0, FramesFactory.getEME2000(),
            slopex, zerox, slopey, zeroy, slopez, zeroz, this.date0);

        final AbsoluteDate date = this.date0.shiftedBy((10.0));
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), date, Constants.EGM96_EARTH_MU);
        final SpacecraftState s = new SpacecraftState(orbit, new Attitude(date, FramesFactory.getEME2000(),
            AngularCoordinates.IDENTITY));
        Assert.assertEquals(6, errorModel.getParameters().size());

        // check the error function value:
        final Vector3D acc = errorModel.computeAcceleration(s);
        Assert.assertEquals(55.0, acc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-70.01, acc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-4.0, acc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // check the error function derivative value:
        final double[] dAccdParam = new double[3];
        final EventDetector[] detectors = errorModel.getEventsDetectors();
        detectors[0].eventOccurred(s, true, true);
        detectors[0].resetState(s);
        errorModel.addDAccDParam(s, slopex, dAccdParam);
        Vector3D dacc = new Vector3D(dAccdParam);
        Vector3D exp = Vector3D.PLUS_I.scalarMultiply(10.0);
        Assert.assertEquals(exp, dacc);
        errorModel.addDAccDParam(s, zerox, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        exp = exp.add(Vector3D.PLUS_I);
        Assert.assertEquals(exp, dacc);
        errorModel.addDAccDParam(s, slopey, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        exp = exp.add(Vector3D.PLUS_J.scalarMultiply(10.0));
        Assert.assertEquals(exp, dacc);
        errorModel.addDAccDParam(s, zeroy, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        exp = exp.add(Vector3D.PLUS_J);
        Assert.assertEquals(exp, dacc);
        errorModel.addDAccDParam(s, slopez, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        exp = exp.add(Vector3D.PLUS_K.scalarMultiply(10.0));
        Assert.assertEquals(exp, dacc);
        errorModel.addDAccDParam(s, zeroz, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        exp = exp.add(Vector3D.PLUS_K);
        Assert.assertEquals(exp, dacc);

        // Check linear function in spacecraft frame
        final ConstantThrustError errorModelExp = new ConstantThrustError(this.date0, 1000.0, 1., 2., 3.);
        final ConstantThrustError errorModelActParam = new ConstantThrustError(this.date0, 1000.0,
            new Parameter("ax", 0.), new Parameter("bx", 1.),
            new Parameter("ay", 0.), new Parameter("by", 2.),
            new Parameter("az", 0.), new Parameter("bz", 3.),
            this.date0);
        final ConstantThrustError errorModelAct =
            new ConstantThrustError(this.date0, 1000.0, 0., 1., 0., 2., 0., 3., this.date0);
        final Vector3D ref = errorModelExp.computeAcceleration(s);
        final Vector3D act = errorModelAct.computeAcceleration(s);
        final Vector3D actParam = errorModelActParam.computeAcceleration(s);
        Assert.assertEquals(ref.getX(), act.getX(), 0.);
        Assert.assertEquals(ref.getY(), act.getY(), 0.);
        Assert.assertEquals(ref.getZ(), act.getZ(), 0.);
        Assert.assertEquals(ref.getX(), actParam.getX(), 0.);
        Assert.assertEquals(ref.getY(), actParam.getY(), 0.);
        Assert.assertEquals(ref.getZ(), actParam.getZ(), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, Parameter, Parameter, Parameter)
     *               )}
     * 
     * @description Test the constant thrust error class when the fx, fy, fz functions are constant functions
     * 
     * @input a constant thrust error class
     * 
     * @output the value of the constant thrust error at a date t and its derivative wrt to the thrust parameters
     * 
     * @testPassCriteria the output values are the expected ones
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testConstantFunction() throws PatriusException {

        final Parameter param1 = new Parameter("parameter1", 8.5);
        final Parameter param2 = new Parameter("parameter2", 0.1);
        final Parameter param3 = new Parameter("parameter3", -0.5);
        final ConstantThrustError errorModel = new ConstantThrustError(this.date0, 100.0, FramesFactory.getEME2000(),
            param1, param2, param3);

        final AbsoluteDate date = this.date0.shiftedBy((50.0));
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), date, Constants.EGM96_EARTH_MU);
        final SpacecraftState s = new SpacecraftState(orbit);
        Assert.assertEquals(3, errorModel.getParameters().size());

        // check the error function value:
        final Vector3D acc = errorModel.computeAcceleration(s);
        Assert.assertEquals(8.5, acc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.1, acc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-0.5, acc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // check the error function derivative value:
        final double[] dAccdParam = new double[3];
        final EventDetector[] detectors = errorModel.getEventsDetectors();
        detectors[0].eventOccurred(s, true, true);
        detectors[0].resetState(s);
        errorModel.addDAccDParam(s, param1, dAccdParam);
        Vector3D dacc = new Vector3D(dAccdParam);
        Vector3D exp = Vector3D.PLUS_I;
        Assert.assertEquals(exp, dacc);
        errorModel.addDAccDParam(s, param2, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        exp = exp.add(Vector3D.PLUS_J);
        Assert.assertEquals(exp, dacc);
        errorModel.addDAccDParam(s, param3, dAccdParam);
        dacc = new Vector3D(dAccdParam);
        exp = exp.add(Vector3D.PLUS_K);
        Assert.assertEquals(exp, dacc);
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, double, double, double)}
     * 
     * @description Test the propagation with constant thrust error defined with a frame different from the propagation
     *              one
     * 
     * @input two propagators with different frame
     * 
     * @output the final state of the two propagations with different frame
     * 
     * @testPassCriteria the output values of the two propagations should be different
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testPropagation() throws PatriusException, IOException, ParseException {
        final TimeScale tai = TimeScalesFactory.getTAI();

        // Initial date:
        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, 6, 0, 0, tai);
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // Reference frame:
        final Frame gcrf = FramesFactory.getGCRF();

        // Initial orbit:
        final double a = 6902e3;
        final double e = .001;
        final double i = 51.4 * FastMath.PI / 180;
        final double pa = 270 * FastMath.PI / 180;
        final double raan = 170 * FastMath.PI / 180;
        final double w = 30 * FastMath.PI / 180;
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(a, e, i, pa,
            raan, w, PositionAngle.TRUE, gcrf, initialDate, mu);
        final double T = initialOrbit.getKeplerianPeriod();
        // Final date:
        final AbsoluteDate finalDate = initialDate.shiftedBy(T * 20);
        // Pointing law:
        final AttitudeProvider attitude = new LofOffset(gcrf, LOFType.TNW);
        // Initial attitude:
        final Attitude initialAttitude = attitude.getAttitude(initialOrbit,
            initialDate, initialOrbit.getFrame());

        // Mass model:
        final MassProvider massModel = new SimpleMassModel(10000, "main");

        // Constant thrust maneuver:
        final AbsoluteDate start = initialDate.shiftedBy(T * 2);
        final double DT = 400;
        // Constant thrust maneuver error with reference frame:
        final ConstantThrustError errorManGCRF = new ConstantThrustError(start, DT, gcrf, 0.01, 0.01, 0.01);
        // Constant thrust maneuver error with other frame:
        final ConstantThrustError errorManQSW = new ConstantThrustError(start, DT, LOFType.QSW, 0.01,
            0.01, 0.01);

        // Propagator:
        final double[][] tol = NumericalPropagator.tolerances(1, initialOrbit, OrbitType.CARTESIAN);
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1,
            7200, tol[0], tol[1]);


        // bulletin initial
        final SpacecraftState state = new SpacecraftState(initialOrbit, initialAttitude, massModel);

        final NumericalPropagator propagator = new NumericalPropagator(dop853, state.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        // Propagator initialization:
        propagator.setInitialState(state);
        propagator.addForceModel(errorManGCRF);
        propagator.setAttitudeProvider(attitude);
        propagator.setMassProviderEquation(massModel);

        // Propagation with thrust error:
        final SpacecraftState stateWithGCRF = propagator.propagate(finalDate);

        // Second propagation:
        final NumericalPropagator propagator2 = new NumericalPropagator(dop853);
        // Mass model:
        final MassProvider massModel2 = new SimpleMassModel(10000, "main");
        final SpacecraftState state2 = new SpacecraftState(initialOrbit, initialAttitude, massModel2);
        propagator2.resetInitialState(state2);
        // Add constant maneuver:
        propagator2.addForceModel(errorManQSW);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu())));
        propagator2.setAttitudeProvider(attitude);
        // Add mass model:
        propagator2.setMassProviderEquation(massModel);
        // Propagation without thrust error:
        final SpacecraftState stateWithITRF = propagator2.propagate(finalDate);

        // The final state should have changed:
        Assert.assertNotSame(stateWithGCRF.getA(), stateWithITRF.getA());
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, double, double, double)}
     * 
     * @description Test the propagation with constant thrust error
     * 
     * @input two propagators, one with constant thrust error and the other one without
     * 
     * @output the final state of the two propagations: with and without constant thrust error
     * 
     * @testPassCriteria the output values of the two propagations should be different
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testPropagationWithDifferentFrame() throws PatriusException, IOException, ParseException {
        final TimeScale tai = TimeScalesFactory.getTAI();

        // Initial date:
        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, 6, 0, 0, tai);
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // Reference frame:
        final Frame gcrf = FramesFactory.getGCRF();

        // Initial orbit:
        final double a = 6902e3;
        final double e = .001;
        final double i = 51.4 * FastMath.PI / 180;
        final double pa = 270 * FastMath.PI / 180;
        final double raan = 170 * FastMath.PI / 180;
        final double w = 30 * FastMath.PI / 180;
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(a, e, i, pa,
            raan, w, PositionAngle.TRUE, gcrf, initialDate, mu);
        final double T = initialOrbit.getKeplerianPeriod();
        // Final date:
        final AbsoluteDate finalDate = initialDate.shiftedBy(T * 20);
        // Pointing law:
        final AttitudeProvider attitude = new LofOffset(gcrf, LOFType.TNW);
        // Initial attitude:
        final Attitude initialAttitude = attitude.getAttitude(initialOrbit,
            initialDate, initialOrbit.getFrame());

        // Mass model:
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder1.addProperty(new MassProperty(0.), "Main");
        final TankProperty tank1 = new TankProperty(10000.);
        builder1.addPart("Tank", "Main", Transform.IDENTITY);
        builder1.addProperty(tank1, "Tank");
        final Assembly assembly1 = builder1.returnAssembly();
        final MassProvider massModel1 = new MassModel(assembly1);

        // Constant thrust maneuver:
        final AbsoluteDate start = initialDate.shiftedBy(T * 2);
        final double DT = 400;
        final double F = 300;
        final double isp = 300;
        final ContinuousThrustManeuver continuousMan1 = new ContinuousThrustManeuver(start, DT,
            new PropulsiveProperty(F, isp), new Vector3D(1, 0, 0), massModel1, tank1);
        // Constant thrust maneuver error:
        final ConstantThrustError errorMan = new ConstantThrustError(start, DT, gcrf, 0.01, 0.01, 0.01);

        // Propagator:
        final double[][] tol = NumericalPropagator.tolerances(1, initialOrbit, OrbitType.CARTESIAN);
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1,
            7200, tol[0], tol[1]);

        // bulletin initial
        final SpacecraftState state = new SpacecraftState(initialOrbit, initialAttitude, massModel1);

        // Propagator initialization:
        final NumericalPropagator propagator1 = new NumericalPropagator(dop853, state.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        propagator1.setInitialState(state);
        propagator1.addForceModel(continuousMan1);
        propagator1.addForceModel(errorMan);
        propagator1.setAttitudeProvider(attitude);
        propagator1.setMassProviderEquation(massModel1);

        // Propagation with thrust error:
        final SpacecraftState stateWithError = propagator1.propagate(finalDate);


        // Mass model:
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main2");
        builder2.addProperty(new MassProperty(0.), "Main2");
        final TankProperty tank2 = new TankProperty(10000.);
        builder2.addPart("Tank", "Main2", Transform.IDENTITY);
        builder2.addProperty(tank2, "Tank");
        final Assembly assembly2 = builder2.returnAssembly();
        final MassProvider massModel2 = new MassModel(assembly2);
        final SpacecraftState state2 = new SpacecraftState(initialOrbit, initialAttitude, massModel2);
        // Second propagation:
        final NumericalPropagator propagator2 = new NumericalPropagator(dop853, state2.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);
        propagator2.resetInitialState(state2);
        // Add constant maneuver:
        final ContinuousThrustManeuver continuousMan2 = new ContinuousThrustManeuver(start, DT,
            new PropulsiveProperty(F, isp), new Vector3D(1, 0, 0), massModel2, tank2);
        propagator2.addForceModel(continuousMan2);
        propagator2.setAttitudeProvider(attitude);
        // Add mass model:
        propagator2.setMassProviderEquation(massModel2);
        // Propagation without thrust error:
        final SpacecraftState stateWithoutError = propagator2.propagate(finalDate);

        // The final state should have changed:
        Assert.assertNotSame(stateWithError.getA(), stateWithoutError.getA());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#addDAccDParam(SpacecraftState, Parameter, double[])}
     * 
     * @description Test exception thrown in case of unsupported parameter
     * 
     * @input an unsupported parameter
     * 
     * @output an exception
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testUnsupportedParam() {
        final double[] dAccdParam = new double[3];
        // Initial date:
        final double mu = Constants.GRIM5C1_EARTH_MU;
        // Reference frame:
        final Frame gcrf = FramesFactory.getGCRF();
        // State
        final double a = 6902e3;
        final double e = .001;
        final double i = 51.4 * FastMath.PI / 180;
        final double pa = 270 * FastMath.PI / 180;
        final double raan = 170 * FastMath.PI / 180;
        final double w = 30 * FastMath.PI / 180;
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(a, e, i, pa,
            raan, w, PositionAngle.TRUE, gcrf, this.date0, mu);
        final SpacecraftState state = new SpacecraftState(initialOrbit);
        // Constant thrust maneuver error:
        final Parameter fx = new Parameter("fx", 0.01);
        final Parameter fy = new Parameter("fy", 0.01);
        final Parameter fz = new Parameter("fz", 0.01);
        final ConstantThrustError errorMan = new ConstantThrustError(this.date0, 0, LOFType.QSW, fx, fy, fz);
        // Throw an exception
        try {
            errorMan.setFiring(true);
            errorMan.addDAccDParam(state, new Parameter("toto", 1.), dAccdParam);
            Assert.fail();
        } catch (final PatriusException e1) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, Parameter, Parameter, Parameter)}
     * 
     * @description Test ConstantThrustError defined with a negative duration
     * 
     * @input a ConstantThrustError with a positive duration and a ConstantThrustError with a negative duration
     * 
     * @output Start date of the ConstantThrustError with a positive duration should equal the stop date of the
     *         ConstantThrustError with a negative duration
     * 
     * @testPassCriteria everything goes well
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testNegativeDuration() {
        // Reference frame:
        final Frame gcrf = FramesFactory.getGCRF();
        // Constant thrust maneuver error:
        final ConstantThrustError errorManPositiveDuration = new ConstantThrustError(this.date0, 100, gcrf, 0.01, 0.01,
            0.01, 0.01, 0.01, 0.01, this.date0);
        final Parameter slopex = new Parameter("slopex", 0.01);
        final Parameter slopey = new Parameter("slopey", 0.01);
        final Parameter slopez = new Parameter("slopez", 0.01);
        final Parameter zerox = new Parameter("zerox", 0.01);
        final Parameter zeroy = new Parameter("zeroy", 0.01);
        final Parameter zeroz = new Parameter("zeroz", 0.01);
        final ConstantThrustError errorManNegativeDuration =
            new ConstantThrustError(this.date0.shiftedBy(100.), -100.0,
                LOFType.QSW,
                slopex, zerox, slopey, zeroy, slopez, zeroz, this.date0);
        // Check start date and end date have switched
        Assert.assertEquals(errorManPositiveDuration.getStartDate(), errorManNegativeDuration.getStartDate());
        Assert.assertEquals(errorManPositiveDuration.getEndDate(), errorManNegativeDuration.getEndDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#addDAccDParam(SpacecraftState, Parameter, double[])}
     * 
     * @description During a propagation, the partial derivatives are computed only when "firing".
     * 
     * @input a propagator, an orbit and a ConstantThrustError.
     * 
     * @output The partial derivatives
     * 
     * @testPassCriteria the derivatives are computed only during the firing
     * 
     * @referenceVersion 3.0.1
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void derivativesComputationOnlyWhenFiring() throws PatriusException {

        // Constant thrust maneuver error:
        final ConstantThrustError errorManPositiveDuration =
            new ConstantThrustError(this.date0, 100, LOFType.QSW, 0.01,
                0.01,
                0.01, 0.01, 0.01, 0.01, this.date0);
        // inital orbit
        final Orbit orbit =
            new KeplerianOrbit(7000000, 0.1, 0., 0., 0., 0., PositionAngle.TRUE, FramesFactory.getGCRF(),
                this.date0.shiftedBy(-10), Utils.mu);
        // propagator
        final Propagator propagator = new KeplerianPropagator(orbit);

        final EventDetector[] events = errorManPositiveDuration.getEventsDetectors();
        propagator.addEventDetector(events[0]);
        propagator.addEventDetector(events[1]);

        for (int i = 0; i < 200; i++) {

            final SpacecraftState state = propagator.propagate(this.date0.shiftedBy(-10 + i * 0.8));
            final double[] dAccdParam = new double[3];

            // derivatives at date
            final ArrayList<Parameter> paramList = errorManPositiveDuration.getParameters();

            // derivatives computation
            for (int j = 0; j < paramList.size(); j++) {
                errorManPositiveDuration.addDAccDParam(state, paramList.get(j), dAccdParam);
            }

            // check : derivatives computation only if firing
            if (this.date0.compareTo(state.getDate()) > 0 || this.date0.shiftedBy(100).compareTo(state.getDate()) < 0) {
                Assert.assertEquals(0., dAccdParam[0], 0.);
                Assert.assertEquals(0., dAccdParam[1], 0.);
                Assert.assertEquals(0., dAccdParam[2], 0.);
            } else {
                Assert.assertTrue(MathLib.abs(dAccdParam[0]) > Utils.epsilonTest);
                Assert.assertTrue(MathLib.abs(dAccdParam[1]) > Utils.epsilonTest);
                Assert.assertTrue(MathLib.abs(dAccdParam[2]) > Utils.epsilonTest);
            }
        }

        // Constant thrust maneuver error:
        final LOFType nullFrame = null;
        final ConstantThrustError errorManPositiveDurationNullFrame =
            new ConstantThrustError(this.date0, 100, nullFrame,
                0.01, 0.01,
                0.01, 0.01, 0.01, 0.01, this.date0);
        // inital orbit
        final Orbit orbit2 =
            new KeplerianOrbit(7000000, 0.1, 0., 0., 0., 0., PositionAngle.TRUE, FramesFactory.getGCRF(),
                this.date0.shiftedBy(-10), Utils.mu);
        // propagator
        final Propagator propagator2 = new KeplerianPropagator(orbit2);

        final EventDetector[] events2 = errorManPositiveDurationNullFrame.getEventsDetectors();
        propagator2.addEventDetector(events2[0]);
        propagator2.addEventDetector(events2[1]);

        // Pointing law:
        final AttitudeProvider attitude = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        propagator2.setAttitudeProvider(attitude);

        for (int i = 0; i < 200; i++) {

            final SpacecraftState state2 = propagator2.propagate(this.date0.shiftedBy(-10 + i * 0.8));
            final double[] dAccdParam = new double[3];

            // derivatives at date
            final ArrayList<Parameter> paramList = errorManPositiveDurationNullFrame.getParameters();

            // derivatives computation
            for (int j = 0; j < paramList.size(); j++) {
                errorManPositiveDurationNullFrame.addDAccDParam(state2, paramList.get(j), dAccdParam);
            }

            // check : derivatives computation only if firing
            if (this.date0.compareTo(state2.getDate()) > 0 || this.date0.shiftedBy(100).compareTo(state2.getDate()) < 0) {
                Assert.assertEquals(0., dAccdParam[0], 0.);
                Assert.assertEquals(0., dAccdParam[1], 0.);
                Assert.assertEquals(0., dAccdParam[2], 0.);
            } else {
                Assert.assertTrue(MathLib.abs(dAccdParam[0]) > Utils.epsilonTest);
                Assert.assertTrue(MathLib.abs(dAccdParam[1]) > Utils.epsilonTest);
                Assert.assertTrue(MathLib.abs(dAccdParam[2]) > Utils.epsilonTest);
            }
        }
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @throws PatriusException
     * @testType UT
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, Frame, double, double, double)}
     * @testedMethod {@link ConstantThrustError#ConstantThrustError(AbsoluteDate, double, LOFType, double, double, double)}
     * @description Test propagation with error with constant thrust in TNW frame following velocity in spacecraft frame
     *              is equal
     *              to propagation with error with constant thrust maneuver in TNW.
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testManeuverWithLOF() throws PatriusException, IOException, ParseException {

        // Echelle de temps TAI
        final TimeScale tai = TimeScalesFactory.getTAI();

        // Initial date:
        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, 6, 0, 0, tai);
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // Reference frame:
        final Frame gcrf = FramesFactory.getGCRF();

        // Initial orbit:
        final double a = 6902e3;
        final double e = .001;
        final double i = 51.4 * FastMath.PI / 180;
        final double pa = 270 * FastMath.PI / 180;
        final double raan = 170 * FastMath.PI / 180;
        final double w = 30 * FastMath.PI / 180;
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(a, e, i, pa,
            raan, w, PositionAngle.TRUE, gcrf, initialDate, mu);
        final double T = initialOrbit.getKeplerianPeriod();
        // Final date:
        final AbsoluteDate finalDate = initialDate.shiftedBy(T * 20);
        // Pointing law:
        final AttitudeProvider attitude = new LofOffset(gcrf, LOFType.TNW);
        // Initial attitude:
        final Attitude initialAttitude = attitude.getAttitude(initialOrbit,
            initialDate, initialOrbit.getFrame());

        // Mass model:
        final MassProvider massModel = new SimpleMassModel(10000, "main");

        // Constant thrust maneuver in satellite frame:
        final AbsoluteDate start = initialDate.shiftedBy(T * 2);
        final double DT = 400;
        final double F = 300;
        final double isp = 300;
        new ContinuousThrustManeuver(start, DT, new PropulsiveProperty(F, isp),
            new Vector3D(1, 0, 0), massModel, new TankProperty(10000));

        final ConstantThrustError errorMan = new ConstantThrustError(start, DT, new Parameter("x", 0.01),
            new Parameter("y", 0.01), new Parameter("z", 0.01));

        // Propagator:
        final double[][] tol = NumericalPropagator.tolerances(1, initialOrbit, OrbitType.CARTESIAN);
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1,
            7200, tol[0], tol[1]);

        // bulletin initial
        final SpacecraftState state = new SpacecraftState(initialOrbit, initialAttitude, massModel);

        final NumericalPropagator propagator = new NumericalPropagator(dop853, state.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        // Propagator initialization:
        propagator.setInitialState(state);
        propagator.addForceModel(errorMan);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(state.getMu())));
        propagator.setAttitudeProvider(attitude);
        propagator.setMassProviderEquation(massModel);

        // Propagation with thrust error:
        final SpacecraftState stateWithError = propagator.propagate(finalDate);

        // Mass model:
        final MassProvider massModel2 = new SimpleMassModel(10000, "main2");

        // Constant thrust maneuver error in ROL frame:
        final ConstantThrustError errorMan2 = new ConstantThrustError(start, DT, LOFType.TNW, 0.01, 0.01, 0.01);

        // Propagator:
        final double[][] tol2 = NumericalPropagator.tolerances(1, initialOrbit, OrbitType.CARTESIAN);
        final FirstOrderIntegrator dop8532 = new DormandPrince853Integrator(.1,
            7200, tol2[0], tol2[1]);
        // bulletin initial
        final SpacecraftState state2 = new SpacecraftState(initialOrbit, initialAttitude, massModel2);

        final NumericalPropagator propagator2 = new NumericalPropagator(dop8532, state2.getFrame(),
            OrbitType.CARTESIAN, PositionAngle.TRUE);

        // Propagator 2 initialization:
        propagator2.setInitialState(state2);
        propagator2.addForceModel(errorMan2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(state2.getMu())));
        propagator2.setAttitudeProvider(attitude);
        propagator2.setMassProviderEquation(massModel2);

        // Propagation with thrust error:
        final SpacecraftState stateWithError2 = propagator2.propagate(finalDate);

        // Comparison
        // --------------------------------------------
        Assert.assertEquals(stateWithError.getA(), stateWithError2.getA(), 1.0e-14);
        Assert.assertEquals(stateWithError.getEquinoctialEx(), stateWithError2.getEquinoctialEx(), 1.0e-14);
        Assert.assertEquals(stateWithError.getEquinoctialEy(), stateWithError2.getEquinoctialEy(), 1.0e-14);
        Assert.assertEquals(stateWithError.getHx(), stateWithError2.getHx(), 1.0e-14);
        Assert.assertEquals(stateWithError.getHy(), stateWithError2.getHy(), 1.0e-14);
        Assert.assertEquals(stateWithError.getLM(), stateWithError2.getLM(), 1.0e-14);
        Assert.assertEquals(stateWithError.getMass("main"), stateWithError2.getMass("main2"), 1.0e-14);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#addDAccDParam(SpacecraftState, Parameter, double[])}
     * 
     * @description During a propagation, the partial derivatives are computed only when "firing".
     * 
     * @input a propagator, an orbit and a ConstantThrustError.
     * 
     * @output The partial derivatives
     * 
     * @testPassCriteria the derivatives are computed only during the firing
     * 
     * @referenceVersion 3.0.1
     * 
     * @nonRegressionVersion 3.0.1
     */
    @Test
    public void testStepHandler() throws PatriusException {

        // FA-487

        // Initialization
        final AbsoluteDate t0 = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate date = t0.shiftedBy(1800);
        final double duration = 360;
        final Parameter ax = new Parameter("ax", 1.e-12);
        final Parameter ay = new Parameter("ay", 2.e-12);
        final Parameter az = new Parameter("az", 3.e-12);
        final Parameter bx = new Parameter("bx", 4.e-12);
        final Parameter by = new Parameter("by", 5.e-12);
        final Parameter bz = new Parameter("bz", 6.e-12);
        final Parameter cx = new Parameter("cx", 7.e-12);
        final Parameter cy = new Parameter("cy", 8.e-12);
        final Parameter cz = new Parameter("cz", 9.e-12);
        final IParamDiffFunction fx = new QuadraticFunction(t0, ax, bx, cx);
        final IParamDiffFunction fy = new QuadraticFunction(t0, ay, by, cy);
        final IParamDiffFunction fz = new QuadraticFunction(t0, az, bz, cz);
        final ConstantThrustError error = new ConstantThrustError(date, duration, fx, fy, fz);
        final Orbit orbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 0., PositionAngle.TRUE,
            FramesFactory.getGCRF(),
            t0, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState s = new SpacecraftState(orbit);

        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(1);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        final MySH handler = new MySH(error, ax, ay, az, bx, by, bz, cx, cy, cz);

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator.addForceModel(error);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.setInitialState(s);
        propagator.setMasterMode(10, handler);

        // Propagation
        propagator.propagate(t0.shiftedBy(3600));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @description Test propagation with constant thrust error and 2 date event detectors should return the same result
     *              when compared to
     *              constant thrust maneuver with a date and a duration
     * 
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustError
     * 
     * @output final states
     * 
     * @testPassCriteria final states are the same
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testEventDetectorsDate() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final AbsoluteDate maneuverDate = initialDate.shiftedBy(1800);
        final double maneuverDuration = 360;
        final AbsoluteDate endManeuverDate = maneuverDate.shiftedBy(maneuverDuration);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 0.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final double propagationDuration = 3600;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // First propagation
        final MassProvider massModel1 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final ConstantThrustError maneuver1 = new ConstantThrustError(maneuverDate, maneuverDuration,
            FramesFactory.getGCRF(), 1., 1., 1.);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(propagationDuration));

        // Second propagation
        final MassProvider massModel2 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final DateDetector detector1 = new DateDetector(maneuverDate);
        final DateDetector detector2 = new DateDetector(endManeuverDate);
        final ConstantThrustError maneuver2 = new ConstantThrustError(detector1, detector2, FramesFactory.getGCRF(),
            new Parameter("x", 1.), new Parameter("y", 1.), new Parameter("z", 1.));
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2.propagate(initialDate.shiftedBy(propagationDuration));

        // Check final states are equals
        Assert.assertEquals(state1.getDate().durationFrom(state2.getDate()), 0., 0.);
        Assert.assertEquals(state1.getA(), state2.getA(), 0.);
        Assert.assertEquals(state1.getEquinoctialEx(), state2.getEquinoctialEx(), 0.);
        Assert.assertEquals(state1.getEquinoctialEy(), state2.getEquinoctialEy(), 0.);
        Assert.assertEquals(state1.getHx(), state2.getHx(), 0.);
        Assert.assertEquals(state1.getHy(), state2.getHy(), 0.);
        Assert.assertEquals(state1.getLM(), state2.getLM(), 0.);
        Assert.assertEquals(state1.getMass("Satellite"), state2.getMass("Satellite"), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @description Test long propagation with constant thrust error and 2 event detectors (apogee/perigee) included in
     *              nth occurrence
     *              detectors (detection on 1st occurrence) should return the same result than a short propagation with
     *              constant thrust error and 2 event detectors (apogee/perigee)
     *              with one occurrence of each event. Second test: thrust on second occurrence with short propagation
     *              should not perform any thrust
     * 
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustError
     * 
     * @output final states
     * 
     * @testPassCriteria final states are the same
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testEventDetectorsOnce() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // First propagation
        final MassProvider massModel1 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ConstantThrustError maneuver1 = new ConstantThrustError(startDetector1, endDetector1,
            FramesFactory.getGCRF(), new Parameter("x", 1.), new Parameter("y", 1.), new Parameter("z", 1.));
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));

        // Second propagation
        final MassProvider massModel2 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final EventDetector startDetector2 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.APOGEE), 1, Action.STOP);
        final EventDetector endDetector2 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.PERIGEE), 1, Action.STOP);
        final ConstantThrustError maneuver2 = new ConstantThrustError(startDetector2, endDetector2,
            FramesFactory.getGCRF(), new Parameter("x", 1.), new Parameter("y", 1.), new Parameter("z", 1.));
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2
            .propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod() * 3.));

        // Check final states are equals
        Assert.assertEquals(state1.getA(), state2.getA(), 0.);
        Assert.assertEquals(state1.getEquinoctialEx(), state2.getEquinoctialEx(), 0.);
        Assert.assertEquals(state1.getEquinoctialEy(), state2.getEquinoctialEy(), 0.);
        Assert.assertEquals(state1.getHx(), state2.getHx(), 0.);
        Assert.assertEquals(state1.getHy(), state2.getHy(), 0.);
        Assert.assertEquals(state1.getMass("Satellite"), state2.getMass("Satellite"), 0.);

        // Third propagation: maneuver should not start since maneuver is planned in second orbital period
        final MassProvider massModel3 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState3 = new SpacecraftState(initialOrbit, massModel3);
        final EventDetector startDetector3 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.APOGEE), 2, Action.STOP);
        final EventDetector endDetector3 = new NthOccurrenceDetector(new ApsideDetector(initialOrbit,
            ApsideDetector.PERIGEE), 2, Action.STOP);
        final ConstantThrustError maneuver3 = new ConstantThrustError(startDetector3, endDetector3,
            FramesFactory.getGCRF(), new ConstantFunction(1.), new ConstantFunction(1.), new ConstantFunction(1.));
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator);
        propagator3.addForceModel(maneuver3);
        propagator3.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState3.getMu())));
        propagator3.setInitialState(initialState3);
        propagator3.setMassProviderEquation(massModel3);
        propagator3.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state3 = propagator3.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));

        // Check final state is equal to initial state
        Assert.assertEquals(state3.getA(), initialState3.getA(), 0.);
        Assert.assertEquals(state3.getEquinoctialEx(), initialState3.getEquinoctialEx(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state3.getEquinoctialEy(), initialState3.getEquinoctialEy(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state3.getHx(), initialState3.getHx(), 0.);
        Assert.assertEquals(state3.getHy(), initialState3.getHy(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(state3.getMass("Satellite"), initialState3.getMass("Satellite"), 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @description Test propagation with several occurrence of constant thrust error. It is checked that thrust is
     *              performed several times:
     *              First propagation with thrust between apogee and perigee over one period.
     *              Second propagation with thrust between apogee and perigee over two period.
     * 
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustErrorr
     * 
     * @output final state
     * 
     * @testPassCriteria 2nd maneuver has been performed
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testEventDetectorsSeveral() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        // First propagation over one period
        final MassProvider massModel1 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final EventDetector startDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector1 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ConstantThrustError maneuver1 = new ConstantThrustError(startDetector1, endDetector1, new Parameter("x",
            1.), new Parameter("y", 1.), new Parameter("z", 1.));
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.setInitialState(initialState1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator1
            .getInitialState().getMu())));
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state1 = propagator1.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));

        // Second propagation over two periods
        final MassProvider massModel2 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final EventDetector startDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        final ConstantThrustError maneuver2 = new ConstantThrustError(startDetector2, endDetector2,
            new ConstantFunction(1.), new ConstantFunction(1.), new ConstantFunction(1.));
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.setInitialState(initialState2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator2
            .getInitialState().getMu())));
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));

        final SpacecraftState state2 = propagator2
            .propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod() * 2.));

        // Check final states are not equals (because 2nd propagation had one more maneuver) (mass is unchanged)
        Assert.assertFalse(state1.getA() - state2.getA() == 0.);
        Assert.assertFalse(state1.getEquinoctialEx() - state2.getEquinoctialEx() == 0.);
        Assert.assertFalse(state1.getEquinoctialEy() - state2.getEquinoctialEy() == 0.);
        Assert.assertFalse(state1.getLM() - state2.getLM() == 0.);
        Assert.assertTrue(state1.getMass("Satellite") - state2.getMass("Satellite") == 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @description Test start/end dates of maneuvers.
     * 
     * @input a constant thrust error
     * 
     * @output maneuver start and end dates
     * 
     * @testPassCriteria date are those expected, null if dates have not been provided
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testStartEndDates() throws PatriusException {

        // Initialization
        final MassProvider massModel = new SimpleMassModel(1000., "Satellite");
        final AbsoluteDate startDate = AbsoluteDate.GALILEO_EPOCH.shiftedBy(1800);
        final AbsoluteDate endDate = startDate.shiftedBy(1800);

        // Case with date and duration
        final ConstantThrustError maneuver1 = new ConstantThrustError(startDate, endDate.durationFrom(startDate), 1.,
            1., 1.);
        Assert.assertEquals(maneuver1.getStartDate().durationFrom(startDate), 0., 0.);
        Assert.assertEquals(maneuver1.getEndDate().durationFrom(endDate), 0., 0.);

        // Case with date detectors
        final DateDetector dateDetector1 = new DateDetector(startDate);
        final DateDetector dateDetector2 = new DateDetector(endDate);
        final ConstantThrustError maneuver2 = new ConstantThrustError(dateDetector1, dateDetector2, LOFType.TNW,
            new Parameter("x", 1.), new Parameter("y", 1.), new Parameter("z", 1.));
        Assert.assertEquals(maneuver2.getStartDate().durationFrom(startDate), 0., 0.);
        Assert.assertEquals(maneuver2.getEndDate().durationFrom(endDate), 0., 0.);

        // Case with other detectors
        final EventDetector otherDetector1 = new NullMassDetector(massModel);
        final EventDetector otherDetector2 = new NullMassDetector(massModel);
        final ConstantThrustError maneuver3 = new ConstantThrustError(otherDetector1, otherDetector2, LOFType.TNW,
            new ConstantFunction(1.), new ConstantFunction(1.), new ConstantFunction(1.));
        Assert.assertNull(maneuver3.getStartDate());
        Assert.assertNull(maneuver3.getStartDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_MANEUVER}
     * 
     * @description Test propagation starting with a maneuver in the middle: results should be the same if the
     *              propagation stops/restart during the maneuver.
     *              Test performed either in forward and retro propagation
     * 
     * @input a propagator, an orbit, 2 event detectors and a ConstantThrustError
     * 
     * @output ephemeris
     * 
     * @testPassCriteria ephemeris are identical
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testPropagationSplitDuringManeuver() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = initialDate.shiftedBy(3600);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);

        final IParamDiffFunction fx =
            new QuadraticFunction(initialDate, new Parameter("ax", 1.e-12), new Parameter("bx",
                4.e-12), new Parameter("cx", 7.e-12));
        final IParamDiffFunction fy =
            new QuadraticFunction(initialDate, new Parameter("ay", 2.e-12), new Parameter("by",
                5.e-12), new Parameter("cy", 8.e-12));
        final IParamDiffFunction fz =
            new QuadraticFunction(initialDate, new Parameter("az", 3.e-12), new Parameter("bz",
                6.e-12), new Parameter("cz", 9.e-12));

        final List<SpacecraftState> res1 = new ArrayList<>();
        final List<SpacecraftState> res2 = new ArrayList<>();
        final List<SpacecraftState> res3 = new ArrayList<>();

        // First propagation without split ("reference")
        final MassProvider massModel1 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState1 = new SpacecraftState(initialOrbit, massModel1);
        final ConstantThrustError maneuver1 = new ConstantThrustError(initialDate.shiftedBy(1000), 1000,
            FramesFactory.getGCRF(), fx, fy, fz);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.addForceModel(maneuver1);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState1.getMu())));
        propagator1.setInitialState(initialState1);
        propagator1.setMassProviderEquation(massModel1);
        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator1.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2927287886736522450L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator, final boolean isLast) {
                try {
                    res1.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        propagator1.propagate(finalDate);

        // Second propagation with split in the middle
        final MassProvider massModel2 = new SimpleMassModel(1000., "Satellite");
        final SpacecraftState initialState2 = new SpacecraftState(initialOrbit, massModel2);
        final ConstantThrustError maneuver2 = new ConstantThrustError(initialDate.shiftedBy(1000), 1000,
            FramesFactory.getGCRF(), fx, fy, fz);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.addForceModel(maneuver2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState2.getMu())));
        propagator2.setInitialState(initialState2);
        propagator2.setMassProviderEquation(massModel2);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator2.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1488334793488729775L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res2.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        final SpacecraftState state = propagator2.propagate(initialDate.shiftedBy(1800));

        final MassProvider massModel3 = new SimpleMassModel(state.getMass("Satellite"), "Satellite");
        final SpacecraftState initialState3 = state;
        final ConstantThrustError maneuver3 = new ConstantThrustError(initialDate.shiftedBy(1000), 1000,
            FramesFactory.getGCRF(), fx, fy, fz);
        maneuver3.setFiring(maneuver2.isFiring());
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator);
        propagator3.addForceModel(maneuver3);
        propagator3.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState3.getMu())));
        propagator3.setInitialState(initialState3);
        propagator3.setMassProviderEquation(massModel3);
        propagator3.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator3.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 9119460374705959138L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res2.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        propagator3.propagate(finalDate);

        // Check ephemeris are equals
        for (int i = 0; i < res1.size(); i++) {
            Assert.assertEquals(res1.get(i).getA(), res2.get(i).getA(), 0.);
            Assert.assertEquals(res1.get(i).getEquinoctialEx(), res2.get(i).getEquinoctialEx(), 0.);
            Assert.assertEquals(res1.get(i).getEquinoctialEy(), res2.get(i).getEquinoctialEy(), 0.);
            Assert.assertEquals(res1.get(i).getLM(), res2.get(i).getLM(), 0.);
            Assert.assertEquals(res1.get(i).getMass("Satellite"), res2.get(i).getMass("Satellite"), 0.);
        }

        // Test in retropolation
        propagator3.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -2250682422659250719L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res3.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        // Propagate to middle interval
        final SpacecraftState retroState = propagator3.propagate(initialDate.shiftedBy(1800));

        // Check forward and retro-propagation states
        final double[] stateArray = new double[7];
        final double[] retroStateArray = new double[7];
        state.mapStateToArray(initialOrbit.getType(), PositionAngle.TRUE, stateArray);
        retroState.mapStateToArray(initialOrbit.getType(), PositionAngle.TRUE, retroStateArray);

        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(stateArray[i], retroStateArray[i], Precision.DOUBLE_COMPARISON_EPSILON);
        }

        // Propagate to initialDate
        final MassProvider massModel4 = new SimpleMassModel(state.getMass("Satellite"), "Satellite");
        final SpacecraftState initialState4 = retroState;
        final ConstantThrustError maneuver4 = new ConstantThrustError(initialDate.shiftedBy(1000), 1000,
            FramesFactory.getGCRF(), fx, fy, fz);
        maneuver4.setFiring(maneuver3.isFiring());
        final NumericalPropagator propagator4 = new NumericalPropagator(integrator);
        propagator4.addForceModel(maneuver4);
        propagator4.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState4.getMu())));
        propagator4.setInitialState(initialState4);
        propagator4.setMassProviderEquation(massModel4);
        propagator4.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY));
        propagator4.setMasterMode(new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -3439407860197320277L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                    throws PropagationException {
                try {
                    res3.add(interpolator.getInterpolatedState());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        });

        propagator4.propagate(initialDate);

        // Check ephemeris are equals :
        // thresholds are ajusted for each orbital parameter
        for (int i = 0; i < res1.size(); i++) {

            Assert.assertEquals(res1.get(res1.size() - 1 - i).getA(), res3.get(i).getA(), 0.);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getEquinoctialEx(), res3.get(i).getEquinoctialEx(),
                1.0E-16);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getEquinoctialEy(), res3.get(i).getEquinoctialEy(),
                1.0E-16);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getLM(), res3.get(i).getLM(), 1.0E-2);
            Assert.assertEquals(res1.get(res1.size() - 1 - i).getMass("Satellite"), res3.get(i).getMass("Satellite"),
                0.);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#computeGradientPosition()}
     * @testedMethod {@link ConstantThrustError#computeGradientVelocity()}
     * 
     * @description check that no acceleration partial derivatives are handled by this class
     * 
     * @input an instance of {@link ConstantThrustError}
     * 
     * @output booleans
     * 
     * @testPassCriteria since there are no partial derivatives computation, output booleans must be false
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void isComputePDTest() {
        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        initialDate.shiftedBy(3600);
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0., 0., 1.,
            PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);

        final IParamDiffFunction fx = new QuadraticFunction(initialDate, new Parameter("ax", 1.e-12),
            new Parameter("bx", 4.e-12), new Parameter("cx", 7.e-12));
        final IParamDiffFunction fy = new QuadraticFunction(initialDate, new Parameter("ay", 2.e-12),
            new Parameter("by", 5.e-12), new Parameter("cy", 8.e-12));
        final IParamDiffFunction fz = new QuadraticFunction(initialDate, new Parameter("az", 3.e-12),
            new Parameter("bz", 6.e-12), new Parameter("cz", 9.e-12));

        new SpacecraftState(initialOrbit);
        final ConstantThrustError maneuver1 = new ConstantThrustError(initialDate.shiftedBy(1000), 1000,
            FramesFactory.getGCRF(),
            fx, fy, fz);

        // No acceleration partial derivatives
        Assert.assertFalse(maneuver1.computeGradientPosition());
        Assert.assertFalse(maneuver1.computeGradientVelocity());
    }

    /**
     * @testType UT
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() {
        ConstantThrustError forceModel = new ConstantThrustError(this.date0, 1000.0,
            new Parameter("ax", 0.), new Parameter("bx", 1.), new Parameter("ay", 0.),
            new Parameter("by", 2.), new Parameter("az", 0.), new Parameter("bz", 3.),
            this.date0);

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL,
                ConstantThrustError.class));
        }

        // Check an other constructor
        final AbsoluteDate initialDate = new AbsoluteDate(2005, 03, 01, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(8000000, 0.001, MathLib.toRadians(89), 0.,
            0., 1., PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final EventDetector startDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.APOGEE);
        final EventDetector endDetector2 = new ApsideDetector(initialOrbit, ApsideDetector.PERIGEE);
        forceModel = new ConstantThrustError(startDetector2, endDetector2,
            new ConstantFunction(1.), new ConstantFunction(1.), new ConstantFunction(1.));

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL,
                ConstantThrustError.class));
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_THRUST_ERROR}
     * 
     * @testedMethod {@link ConstantThrustError#computeAcceleration(SpacecraftState)}
     * 
     * @description Test that an exception is thrown if the spacecraft State frame is not pseudo-inertial and the
     *              LofType is not null
     * 
     * @input a constant thrust error class
     * 
     * @output The frame is not pseudoInertial
     * 
     * @testPassCriteria the class return a patriusException
     * 
     * @referenceVersion 4.11
     */
    @Test
    public void testNotPseudoInertial() throws PatriusException {
        // Inputs
        final Parameter slopex = new Parameter("slopex", 4.5);
        final Parameter slopey = new Parameter("slopey", -7.0);
        final Parameter slopez = new Parameter("slopez", -0.5);
        final Parameter zerox = new Parameter("zerox", 10.0);
        final Parameter zeroy = new Parameter("zeroy", -0.01);
        final Parameter zeroz = new Parameter("zeroz", 1.0);
        // Creation of the constantThrustError without maneuver frame
        final ConstantThrustError errorModel = new ConstantThrustError(this.date0, 1000.0,
            LOFType.LVLH, slopex, zerox, slopey, zeroy, slopez, zeroz, this.date0);

        final AbsoluteDate date = this.date0.shiftedBy((10.0));
        // Creation of the orbit with a non-pseudo-inertial frame
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getITRF(), date, Constants.EGM96_EARTH_MU);
        // Creation of the spacecraftState with a non-pseudo-inertial frame
        final SpacecraftState s = new SpacecraftState(orbit, new Attitude(date, FramesFactory.getITRF(),
            AngularCoordinates.IDENTITY));

        // check the error function value:
        try {
            errorModel.computeAcceleration(s);
            Assert.fail();
        } catch (final PatriusException pe) {
            Assert.assertEquals(pe.getMessage(), PatriusMessages.NOT_INERTIAL_FRAME.getSourceString());
        }
    }

    /**
     * Quadratic function.
     */
    private class QuadraticFunction implements IParamDiffFunction {
        /** Serializable UID. */
        private static final long serialVersionUID = -1536194879895016776L;

        private final Parameter a;
        private final Parameter b;
        private final Parameter c;
        private final AbsoluteDate t0;

        public QuadraticFunction(final AbsoluteDate t0, final Parameter a, final Parameter b, final Parameter c) {
            this.t0 = t0;
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public double value(final SpacecraftState state) {
            final double dt = (state.getDate().durationFrom(this.t0));
            return this.a.getValue() * dt * dt + this.b.getValue() * dt + this.c.getValue();
        }

        @Override
        public boolean supportsParameter(final Parameter param) {
            return param.equals(this.a) || param.equals(this.b) || param.equals(this.c);
        }

        @Override
        public ArrayList<Parameter> getParameters() {
            final ArrayList<Parameter> l = new ArrayList<>(3);
            l.add(this.a);
            l.add(this.b);
            l.add(this.c);
            return l;
        }

        @Override
        public double derivativeValue(final Parameter p, final SpacecraftState s) {
            double d = 0;
            final double dt = (s.getDate().durationFrom(this.t0));
            if (p.equals(this.a)) {
                d = dt * dt;
            } else if (p.equals(this.b)) {
                d = dt;
            } else if (p.equals(this.c)) {
                d = 1;
            }
            return d;
        }

        @Override
        public boolean isDifferentiableBy(final Parameter p) {
            return p.equals(this.a) || p.equals(this.b) || p.equals(this.c);
        }
    }

    /**
     * Step handler.
     */
    private class MySH implements PatriusFixedStepHandler {
        /** Serializable UID. */
        private static final long serialVersionUID = 74400842615364899L;

        private final ConstantThrustError error;
        Parameter ax;
        Parameter ay;
        Parameter az;
        Parameter bx;
        Parameter by;
        Parameter bz;
        Parameter cx;
        Parameter cy;
        Parameter cz;
        AbsoluteDate t0;

        public MySH(final ConstantThrustError e, final Parameter ax, final Parameter ay, final Parameter az,
                    final Parameter bx, final Parameter by,
                    final Parameter bz, final Parameter cx, final Parameter cy, final Parameter cz) {
            this.error = e;
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.bx = bx;
            this.by = by;
            this.bz = bz;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.t0 = s0.getDate();
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) throws PropagationException {

            final double dt = currentState.getDate().durationFrom(this.t0);

            final double[] d = { 0., 0., 0. };
            try {
                this.error.addDAccDParam(currentState, this.ax, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.ay, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.az, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.bx, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.by, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.bz, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.cx, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.cy, d);
                this.checkData(dt, d[0]);
                this.error.addDAccDParam(currentState, this.cz, d);
                this.checkData(dt, d[0]);
            } catch (final PatriusException e) {
                Assert.fail();
            }
        }

        /**
         * Check data (0 before and after maneuver, not 0 during the maneuver).
         */
        private void checkData(final double dt, final double data) {
            if (dt < 1800 || dt >= 1800 + 360) {
                Assert.assertEquals(0., data, 0.);
            } else {
                Assert.assertTrue(data != 0.);
            }
        }
    }

    /**
     * Set up method before running the test.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(true));
        this.date0 = new AbsoluteDate(new DateComponents(2014, 06, 05), new TimeComponents(12, 0, 0.0),
            TimeScalesFactory.getUTC());
    }
}
