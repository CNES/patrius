/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
* VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
* VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
* VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite 
 *          de convertir les sorties de VacuumSignalPropagation 
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de AbstractEOPHistory
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8.1:FA:FA-2947:07/12/2021:[PATRIUS] Problemes lies au coefficient multiplicatif des forces gravitationnelles
 * VERSION:4.8:FA:FA-2947:15/11/2021:[PATRIUS] Problemes lies au coefficient multiplicatif des forces gravitationnelles 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2685:18/05/2021:Prise en compte d un modele de gravite complexe pour un troisieme corps
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:computeDAccDPos method set to private
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialBodyIAUOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration.PatriusVersionCompatibility;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class ThirdBodyAttractionTest {

    private double mu;

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     */
    @Test
    public void testParamList() throws PatriusException {

        final ThirdBodyAttraction thirdBody = new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel());

        double k = 5;
        Assert.assertEquals(1, thirdBody.getParameters().size());
        final ArrayList<Parameter> paramList = thirdBody.getParameters();
        for (int i = 0; i < paramList.size(); i++) {
            paramList.get(i).setValue(k);
            Assert.assertTrue(Precision.equals(k, paramList.get(i).getValue(), 0));
            k++;
        }
        Assert.assertEquals(1, thirdBody.getParameters().size());

        Assert.assertFalse(thirdBody.supportsJacobianParameter(new Parameter("toto", 1.)));
    }

    @Test(expected = PatriusException.class)
    public void testSunContrib() throws PatriusException {

        // initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
            new TimeComponents(13, 59, 27.816),
            TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3, MathLib.tan(0.001745329)
                * MathLib.cos(2 * FastMath.PI / 3), MathLib.tan(0.001745329) * MathLib.sin(2 * FastMath.PI / 3),
            0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, this.mu);
        final double period = 2 * FastMath.PI * orbit.getA() * MathLib.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        final NumericalPropagator calc = new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0,
            1.0e-5));
        final GravityModel gravityModel = CelestialBodyFactory.getSun().getGravityModel();
        final ThirdBodyAttraction forceModel = new ThirdBodyAttraction(gravityModel);
        calc.addForceModel(forceModel);
        calc.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));

        // set up step handler to perform checks
        calc.setMasterMode(MathLib.floor(period), new ReferenceChecker(date){
            private static final long serialVersionUID = 6539780121834779598L;

            @Override
            protected double hXRef(final double t) {
                return -1.06757e-3 + 0.221415e-11 * t + 18.9421e-5 * MathLib.cos(3.9820426e-7 * t) - 7.59983e-5
                        * MathLib.sin(3.9820426e-7 * t);
            }

            @Override
            protected double hYRef(final double t) {
                return 1.43526e-3 + 7.49765e-11 * t + 6.9448e-5 * MathLib.cos(3.9820426e-7 * t) + 17.6083e-5
                        * MathLib.sin(3.9820426e-7 * t);
            }
        });
        final AbsoluteDate finalDate = date.shiftedBy(365 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    @Test
    public void testMoonContrib() throws PatriusException {

        // initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
            new TimeComponents(13, 59, 27.816),
            TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3, MathLib.tan(0.001745329)
                * MathLib.cos(2 * FastMath.PI / 3), MathLib.tan(0.001745329) * MathLib.sin(2 * FastMath.PI / 3),
            0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, this.mu);
        final double period = 2 * FastMath.PI * orbit.getA() * MathLib.sqrt(orbit.getA() / orbit.getMu());

        // set up propagator
        final NumericalPropagator calc = new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0, period, 0,
            1.0e-5));
        calc.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel()));
        calc.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));

        // set up step handler to perform checks
        calc.setMasterMode(MathLib.floor(period), new ReferenceChecker(date){
            private static final long serialVersionUID = -4725658720642817168L;

            @Override
            protected double hXRef(final double t) {
                return -0.000906173 + 1.93933e-11 * t + 1.0856e-06 * MathLib.cos(5.30637e-05 * t) - 1.22574e-06
                        * MathLib.sin(5.30637e-05 * t);
            }

            @Override
            protected double hYRef(final double t) {
                return 0.00151973 + 1.88991e-10 * t - 1.25972e-06 * MathLib.cos(5.30637e-05 * t) - 1.00581e-06
                        * MathLib.sin(5.30637e-05 * t);
            }
        });
        final AbsoluteDate finalDate = date.shiftedBy(31 * period);
        calc.setInitialState(new SpacecraftState(orbit));
        calc.propagate(finalDate);

    }

    private static abstract class ReferenceChecker implements PatriusFixedStepHandler {

        private static final long serialVersionUID = -2167849325324684095L;
        private final AbsoluteDate reference;

        protected ReferenceChecker(final AbsoluteDate reference) {
            this.reference = reference;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // nothing to do
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            final double t = currentState.getDate().durationFrom(this.reference);
            Assert.assertEquals(this.hXRef(t), currentState.getHx(), 1e-4);
            Assert.assertEquals(this.hYRef(t), currentState.getHy(), 1e-4);
        }

        protected abstract double hXRef(double t);

        protected abstract double hYRef(double t);

    }

    /**
     * Test the partial derivatives computation.
     * 
     * @throws PatriusException
     *         when an error occurs.
     */
    @Test
    public void testAddDAccDState() throws PatriusException {

        final ThirdBodyAttraction thirdBody = new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel());

        final double[][] dAccdPos = new double[3][3];
        final double[][] expDAccdPos = new double[3][3];

        final AbsoluteDate date = new AbsoluteDate(1970, 3, 5, 0, 24, 0, TimeScalesFactory.getTAI());

        final Frame referenceFrame = FramesFactory.getGCRF();

        // PV coordinates in GCRF frame
        final Vector3D pos = new Vector3D(6.4688587830467382E+06, -1.8805091845627432E+06, -1.3293159229471583E+04);
        final Vector3D vel = new Vector3D(2.1471807451962504E+03, 7.3823935125280523E+03, -1.1409758242487955E+01);
        final PVCoordinates pv = new PVCoordinates(pos, vel);

        // compute partial derivatives by finite differences
        // with respect to spacecraft position

        // compute nominal acceleration
        final Vector3D nominalAcc = thirdBody.computeAcceleration(createSpacecraftState(pv.getPosition(),
            referenceFrame, date));

        // step
        final double hPos = MathLib.sqrt(Precision.EPSILON) * pos.getNorm();

        // shifted position (dX)
        final PVCoordinates shiftedPVdX = new PVCoordinates(new Vector3D(pos.getX() + hPos, pos.getY(), pos.getZ()),
            pv.getVelocity());
        // compute shifted acceleration
        Vector3D shiftedAcc = thirdBody.computeAcceleration(createSpacecraftState(shiftedPVdX.getPosition(),
            referenceFrame, date));

        expDAccdPos[0][0] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][0] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][0] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // shifted position (dY)
        final PVCoordinates shiftedPVdY = new PVCoordinates(new Vector3D(pos.getX(), pos.getY() + hPos, pos.getZ()),
            pv.getVelocity());
        // compute shifted acceleration
        shiftedAcc = thirdBody.computeAcceleration(createSpacecraftState(shiftedPVdY.getPosition(), referenceFrame,
            date));

        expDAccdPos[0][1] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][1] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][1] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // shifted position (dZ)
        final PVCoordinates shiftedPVdZ = new PVCoordinates(new Vector3D(pos.getX(), pos.getY(), pos.getZ() + hPos),
            pv.getVelocity());
        // compute shifted acceleration
        shiftedAcc = thirdBody.computeAcceleration(createSpacecraftState(shiftedPVdZ.getPosition(), referenceFrame,
            date));

        expDAccdPos[0][2] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][2] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][2] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // compute partial derivatives with addDAccDState method
        final Orbit orbit = new CartesianOrbit(pv, referenceFrame, date, this.mu);
        final SpacecraftState scr = new SpacecraftState(orbit);
        thirdBody.addDAccDState(scr.getPVCoordinates().getPosition(), scr.getFrame(), scr.getDate(), dAccdPos);

        // compare with expected acceleration
        for (int i = 0; i < dAccdPos.length; i++) {
            for (int j = 0; j < dAccdPos.length; j++) {
                Assert.assertEquals(expDAccdPos[i][j], dAccdPos[i][j], 2e-13);
            }
        }

        /** Test no-regression of the compatibility modes. */

        // Test default mode (backward compatible)
        PatriusConfiguration.setPatriusCompatibilityMode(PatriusVersionCompatibility.OLD_MODELS);
        double[][] dAccDPPosOldModels = new double[3][3];
        thirdBody.addDAccDState(pos, referenceFrame, date.shiftedBy(86400.), dAccDPPosOldModels);

        // Test mixed models mode
        PatriusConfiguration.setPatriusCompatibilityMode(PatriusVersionCompatibility.MIXED_MODELS);
        double[][] dAccDPPosMixedModels = new double[3][3];
        thirdBody.addDAccDState(pos, referenceFrame, date, dAccDPPosMixedModels);

        // Test new models mode
        PatriusConfiguration.setPatriusCompatibilityMode(PatriusVersionCompatibility.NEW_MODELS);
        double[][] dAccDPPosNewModels = new double[3][3];
        thirdBody.addDAccDState(pos, referenceFrame, date, dAccDPPosNewModels);

        final double[][] expectedOldModelsAcc =
            new double[][] { { 8.94211305859775E-14, -1.4505769393859904E-13, -7.289352428869E-14 },
                { -1.4505769393859904E-13, -5.39280629171066E-15, 5.2864916370842237E-14 },
                { -7.289352428869E-14, 5.2864916370842237E-14, -8.402832429426683E-14 } };
        final double[][] expectedNewMixedModelsAcc =
            new double[][] { { 2.5720692871039785E-15, -1.3677916354268827E-13, -7.109731199909889E-14 },
                { -1.3677916354268827E-13, 6.024530085679825E-14, 8.764935614560584E-14 },
                { -7.109731199909889E-14, 8.764935614560584E-14, -6.281737014390222E-14 } };


        // compare with expected acceleration
        Assert.assertTrue(Arrays.deepEquals(expectedOldModelsAcc, dAccDPPosOldModels));
        Assert.assertTrue(Arrays.deepEquals(expectedNewMixedModelsAcc, dAccDPPosMixedModels));
        Assert.assertTrue(Arrays.deepEquals(expectedNewMixedModelsAcc, dAccDPPosNewModels));
    }

    private SpacecraftState createSpacecraftState(final Vector3D position, final Frame frame, final AbsoluteDate date) {
        return new SpacecraftState(new CartesianOrbit(new PVCoordinates(position, Vector3D.ZERO), frame, date,
            Constants.WGS84_EARTH_MU));
    }

    /**
     * Test the partial derivatives computation.
     * 
     * @throws PatriusException
     *         when an error occurs
     */
    @Test
    public void testAddDAccDParam() throws PatriusException {

        final ThirdBodyAttraction thirdBody = new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel());

        final double[] dAccdParam = new double[3];

        try {
            thirdBody.addDAccDParam(null, null, null, new Parameter("null", 0.), dAccdParam);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ThirdBodyAttraction #ThirdBodyAttraction(CelestialPoint, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link ThirdBodyAttraction}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // Instance
        final ThirdBodyAttraction thirdBody = new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel(),
            false);

        // Check that derivatives computation is deactivated
        Assert.assertFalse(thirdBody.computeGradientPosition());
        // Partial derivatives wrt velocity are always null in this model
        Assert.assertFalse(thirdBody.computeGradientVelocity());

        // SpacecraftState
        final Vector3D pos = new Vector3D(6.4688587830467382E+06, -1.8805091845627432E+06, -1.3293159229471583E+04);
        final Vector3D vel = new Vector3D(2.1471807451962504E+03, 7.3823935125280523E+03, -1.1409758242487955E+01);
        final PVCoordinates pv = new PVCoordinates(pos, vel);
        final AbsoluteDate date = new AbsoluteDate(1970, 3, 5, 0, 24, 0, TimeScalesFactory.getTAI());
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new CartesianOrbit(pv, referenceFrame, date, this.mu);
        final SpacecraftState scr = new SpacecraftState(orbit);

        // Compute partial derivatives
        final double[][] dAccdPos = new double[3][3];
        thirdBody.addDAccDState(scr.getPVCoordinates().getPosition(), scr.getFrame(), scr.getDate(), dAccdPos);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
            }
        }
    }

    /**
     * FA 93 : added test to ensure the list of "jacobian parameters" is empty.
     * 
     * @throws PatriusException
     */
    @Test
    public void testEmptyList() throws PatriusException {
        final ThirdBodyAttraction thirdBody = new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel());

        Assert.assertEquals(1, thirdBody.getParameters().size());
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ThirdBodyAttraction#computeAcceleration(SpacecraftState)}
     * 
     * @description check that 3rd body potential taking into account harmonics is properly computed
     * 
     * @testPassCriteria acceleration is as expected (reference: NUMEXTRAP 1.4.3 with same parameters, relative
     *                   threshold: 0.)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testHarmonics() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");
        final FramesConfiguration svgConfig = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());

        // Orbit
        final AbsoluteDate date = new AbsoluteDate("2010-11-24T04:00:00.000", TimeScalesFactory.getTAI());
        final Vector3D pos = new Vector3D(7005965.506340, 1947.511809, -13879.123433);
        final Vector3D vel = new Vector3D(7.560970, -1049.308479, 7471.261608);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel), FramesFactory.getCIRF(), date,
            3.986004328969392E14);
        final SpacecraftState state = new SpacecraftState(orbit);

        // 3rd body potential
        final double[][] c = {
            { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { -2.032132919428845E-4, 6.203045246423681E-12, 2.238040265574716E-5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { -8.459745296195974E-6, 2.8480670906926614E-5, 4.840505021298896E-6, 1.7116723293401032E-6, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 9.704362633767E-6, -5.7048714741827625E-6, -1.5912246847045305E-6, -8.067820477610789E-8,
                -1.2692565519467255E-7, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { -7.422258131271801E-7, -8.662927836565018E-7, 7.119963328262973E-7, 1.5399962747513772E-8,
                2.1444655616634897E-8, 7.659532245630841E-9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0 },
            { 1.3767544189476268E-5, 1.2024358232663865E-6, -5.470394859673132E-7, -6.87855824702627E-8,
                1.2914310880035368E-9, 1.1737779995271056E-9, -1.091347575639668E-9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 2.1663127702281298E-5, 5.468709389438278E-6, -6.475535732330711E-8, 8.443472085850597E-9,
                -1.7916336059372605E-9, -7.319505307482708E-11, -7.394510915130541E-11, -3.376750601069792E-11, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 9.676232201765963E-6, 2.867312625173403E-9, 2.4716796962158465E-7, -1.909831080826545E-8,
                4.448984824645172E-9, -2.2589368844126463E-10, -4.637515505341553E-11, -7.697728683164882E-12,
                -3.168664480231593E-12, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { -1.539087213681764E-5, 1.2131293725809175E-6, 1.335342224508036E-7, -1.505801285145148E-8,
                -1.612597955232102E-9, -1.5981492917023848E-10, -2.808916886102924E-11, -7.46112116713889E-12,
                -4.288447269622324E-13, -7.229716322320922E-14, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { -4.900137606438247E-6, 5.20035769153835E-7, 2.12415533284462E-8, 2.8230768549033236E-9,
                -2.1043449890348174E-9, 4.3438191540920574E-11, -8.837114501260319E-13, -3.3657241799157953E-12,
                -4.0771034967356606E-13, -8.831967343187691E-14, 3.93831141806009E-15, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0 },
            { -4.2453068808135245E-6, -1.1488006300853672E-8, 3.621738323877203E-8, 1.8838319301742425E-9,
                -4.5759737596820187E-10, 2.0029961432728855E-12, 2.065457616755943E-12, -3.509704416471643E-14,
                -9.980439267993334E-14, -1.435860402798487E-14, -4.4412609256601075E-15, -5.844235563195496E-16, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { -1.0061314926015E-5, -4.0019396457351925E-7, -4.799615279945106E-9, 3.187365075509196E-9,
                2.523671465094325E-10, 5.6398988969343644E-15, 2.5824474014365573E-12, 4.75271996180357E-13,
                8.851191324999234E-15, -3.259602868536531E-15, -9.173450018679983E-16, -4.3178515923467676E-17,
                2.717877402893294E-18, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 1.3327125534059177E-6, 7.377858569988315E-7, -1.390639871198004E-7, -1.3262136808532256E-9,
                1.8243701698817612E-10, -2.187741993071822E-11, -4.141376507963036E-14, -2.0494574629377505E-14,
                -3.291812294300806E-15, -1.8251618897197262E-16, -7.548659609259075E-17, -1.722675253184307E-17,
                -1.0717147712853065E-18, 9.145677092813217E-19, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 2.6508567850651205E-6, 2.971540585059942E-7, 1.3279268741158567E-8, 1.4233454283531193E-9,
                -7.080867423310853E-11, -1.029137963479297E-11, -5.97680172054089E-13, -3.89873996089183E-14,
                1.866029155666684E-15, 4.176644775346401E-17, -3.6860557441725995E-17, -9.13445337937645E-18,
                -7.768811199847964E-19, -2.345759438701386E-20, -8.461428598328315E-21, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { -1.2699382575966355E-6, -4.0535772364200727E-7, -1.7795582359466233E-9, -2.9683810926572637E-9,
                -1.3636794563709775E-10, 7.44053044411145E-14, -4.275866023792063E-14, 5.306837139519565E-14,
                4.572170293711285E-15, -2.28608442714041E-17, -1.3172013318774349E-18, -2.7396738350236963E-18,
                -3.266377152529827E-19, -8.16607327102265E-21, 2.180552318500804E-21, 2.6512566640615275E-22, 0.0, 0.0,
                0.0, 0.0, 0.0 },
            { 3.1988417721853724E-6, 5.693878183765841E-8, 4.362526915192402E-8, -3.570859197058498E-10,
                7.819312063957193E-11, 5.48205542635933E-12, 3.368818434435922E-13, -8.543841795069316E-15,
                4.911455174785153E-17, -1.143245182419767E-16, 2.4663532941060632E-18, -2.8268335535570516E-19,
                -9.328275988091332E-20, -1.203880444718326E-21, -4.795985969552831E-22, -3.2681133421119733E-23,
                -1.433847817292369E-23, 0.0, 0.0, 0.0, 0.0 },
            { -6.111501961324411E-6, 2.4010847608110656E-7, -7.975022902154537E-9, -4.241113112136671E-11,
                1.1870315414015887E-10, 1.0413784787322693E-12, 2.371147194757017E-13, -2.62792813261596E-14,
                -2.7061516317202605E-16, 2.7456923436959112E-17, 2.36952845564155E-18, 4.651104251651773E-19,
                2.957372939182304E-20, -9.99387376299863E-23, 9.110602733585106E-23, -1.4837524054123162E-24,
                -8.9996323273594E-25, -1.8539881348317556E-25, 0.0, 0.0, 0.0 },
            { -3.164984424167704E-6, 1.6765189355573684E-7, -7.987357324881562E-9, 1.3696674629019669E-9,
                -6.083923623054486E-11, -2.19245167157228E-12, -4.066446750516383E-13, -3.0126889854007877E-15,
                3.6278507320412184E-16, 2.7348995042093383E-17, 1.1205353831862858E-18, 8.652493045545215E-20,
                1.1775502157320105E-20, -1.2634801447532632E-21, 5.0205777554796216E-24, 1.7390845880495733E-24,
                6.363923710198509E-25, 1.0408990954494035E-25, -2.9573452087624508E-27, 0.0, 0.0 },
            { 1.346760038932322E-6, -2.6786743752472034E-7, 4.03141879448547E-9, -7.615064195723693E-10,
                -6.240450804679777E-11, -8.555936643993643E-13, 2.8083123029001144E-14, 1.1775848094831641E-14,
                3.482146763289007E-16, -1.992273308739166E-18, -1.3999335209126571E-18, 1.0012828816269452E-21,
                -7.636746699236247E-22, 3.4418620753046887E-22, -4.138516625242145E-24, 2.112596300067532E-25,
                -1.174079791872082E-26, -2.1891774051138315E-26, 2.6753418810685636E-27, 2.3250417127040872E-28, 0.0 },
            { 2.148657761763128E-6, -8.706998834199597E-8, 1.7566159697721716E-8, 6.7470346914685E-10,
                2.4376018581078246E-11, 5.757638402195157E-13, -2.5471368146671946E-14, -4.628782913076778E-15,
                8.419197766639175E-17, -7.400914474352092E-18, 3.452633851435738E-19, 1.4756470110346618E-20,
                4.647971611417298E-22, 1.8117974436577267E-22, -2.0678496389335393E-24, -3.1686759466042485E-25,
                6.752373267137015E-26, 4.742694898963327E-27, 4.731640739487904E-28, -1.3773163930535473E-29,
                -1.3557322565570247E-30 } };
        final double[][] s = {
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 1.0741043061033635E-9, -1.3432186078773792E-10, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 5.891563735013992E-6, 1.6661486481851626E-6, -2.474272118578407E-7, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 1.5789195909622124E-6, -1.5153922581948064E-6, -8.034921708950805E-7, 8.296972451729696E-8, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, -3.527231150852108E-6, 1.7107975055561803E-7, 2.873624216414599E-7, 5.265802449753309E-10,
                -6.782385759814069E-9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, -2.0453492313983224E-6, -2.6966803989207555E-7, -7.106383170881721E-8, -1.536140360827632E-8,
                -8.346505845166885E-9, 1.6843681792164306E-9, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0 },
            { 0.0, -8.763867484310608E-8, 2.4015275739400546E-7, 3.320507187124349E-8, 1.6064885131127893E-9,
                3.7844838378275525E-10, 7.638300803693379E-11, -2.968160465893162E-11, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 7.545559276955076E-7, 1.5856502634292474E-7, 9.649873360375251E-9, -6.894537417426632E-10,
                5.282556402647851E-10, -5.906204115624172E-11, 1.6668183552441358E-11, 2.6978866227374377E-12, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 5.26581637830853E-8, -9.61133094393944E-8, 1.664022252831519E-8, -1.2201080500248064E-9,
                -3.605068948146026E-10, -3.964792804185527E-11, -2.0357695642595954E-13, -7.201878643716074E-13,
                1.9169120236992675E-13, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, -5.895364296065147E-7, -1.5763531301547396E-8, 3.89954730295545E-9, 9.299375518527187E-10,
                -1.9529723161653934E-11, -1.4543670522823714E-11, -7.66577932190213E-13, 3.263163757081832E-13,
                -9.575003615917342E-16, -7.143960632853777E-15, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 3.626084853258937E-7, -1.0189547875657334E-7, 3.3959567444351074E-9, 9.224866405234315E-10,
                1.029683444539158E-10, -5.385273229373127E-12, -1.179772905620642E-12, -1.8238337487881436E-14,
                1.5536732020116749E-15, -1.6465149902435904E-16, -3.5330065965621187E-16, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0 },
            { 0.0, 9.328724756284814E-7, 6.87883446432732E-8, -1.511925743338629E-8, -1.069711868387404E-10,
                2.3786107882516163E-11, 1.2928698025982347E-12, -2.148843011458326E-13, -4.9005458912980464E-14,
                2.653407435967304E-15, -3.799770087713893E-16, -1.444491061618822E-17, 1.1193921225551938E-17, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, -4.176281183998088E-8, 1.558937856813447E-8, -4.456805122568991E-9, -1.3659003060970728E-10,
                1.883451912001501E-11, 2.070478881305749E-12, -8.503426626183406E-14, -1.3632669639305768E-14,
                5.664198678242465E-16, 5.070362451422132E-17, 5.7924047218461595E-18, -2.8404232611862107E-18,
                -9.76586969322092E-19, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, -3.5847175165970696E-7, 1.294364213519251E-8, 4.286191104131861E-10, -4.681556850608597E-10,
                -2.841346970802546E-12, 1.825802119514691E-12, 8.785860608151132E-14, 2.6908037677521657E-15,
                -8.499336705230022E-16, 2.808876765164564E-17, 9.175875809716621E-18, -2.8931568361534713E-19,
                -7.428238251430901E-20, 1.4773114400456135E-20, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 1.7973004712112343E-7, 2.4533384382889632E-8, -1.358368839976791E-9, -1.887592162612445E-10,
                -8.591340702094539E-12, -2.1943903564571082E-13, 7.02401563948419E-14, 6.5590134214993085E-15,
                -2.713498845643879E-16, 9.25860924819456E-18, -2.8859570650807112E-18, -8.719892213799026E-20,
                6.241550368110412E-21, 1.4867575641431853E-21, 3.760806419294295E-22, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 4.0651478153679266E-7, 2.2949923394445347E-10, 9.619362699001159E-10, 3.561678688122161E-11,
                -4.6566757789122875E-12, -3.3608720376175E-13, -1.766580088653008E-14, 6.628892532794895E-16,
                -1.2057501212905402E-16, 3.641531228609837E-19, 6.585502612939255E-19, 6.601402215068439E-20,
                -1.0908378290740311E-21, 1.253056541456601E-22, -9.100595988346621E-23, -1.4550978988722474E-23, 0.0,
                0.0, 0.0, 0.0 },
            { 0.0, 3.647340640537069E-8, -2.3936300996487818E-8, -7.954625696983168E-10, 1.8820203138256064E-10,
                -3.3004380784772723E-12, -4.0959096076138164E-13, -3.148919664227844E-14, -6.020272567383484E-16,
                1.147460907778372E-16, 3.493385567923513E-18, -8.213628642883521E-20, 5.5345582437879285E-21,
                -5.594643344482999E-22, 6.883208714282455E-23, -1.1638707160104676E-23, -7.14184206441481E-25,
                7.326741201065105E-25, 0.0, 0.0, 0.0 },
            { 0.0, 1.248500591021545E-8, -1.831765561416942E-8, 5.954480531361648E-10, 8.267800870149492E-11,
                3.0899684743170577E-13, 1.7541634346337564E-13, -1.5645085753283584E-14, 2.340259182489031E-16,
                -3.16534772467291E-17, -1.7208885286475249E-18, -2.6658275915967875E-19, 3.3271199925069812E-21,
                -1.0410022523906289E-22, -9.300342946949895E-23, -7.75461680213276E-24, -1.5122305371539316E-25,
                -3.98806278463803E-26, -4.0215166351846425E-27, 0.0, 0.0 },
            { 0.0, 5.9495065479060635E-8, -8.663646693126259E-9, 1.17677800950488E-9, -6.619898973284621E-11,
                4.9103738254702255E-12, 6.725510957015348E-14, 4.727356400989581E-15, 4.634441370137035E-16,
                -1.2864645443051865E-17, -4.0432705363271305E-19, -1.9085268829332853E-20, -1.9856032256803262E-21,
                1.0310695762880584E-21, 1.7726132433226143E-23, 2.0778389739783913E-25, 1.4672824804813649E-25,
                -3.82675338143243E-26, -5.164345809084673E-29, 1.4107088195129413E-28, 0.0 },
            { 0.0, -1.6904136220819824E-7, 7.369366590587846E-11, -2.819934114264103E-10, -2.5706364188164437E-11,
                -7.970471071130263E-13, 4.3927271584442465E-14, -5.009107516836665E-15, 2.486339104574085E-17,
                2.1140414208515707E-18, 5.1788633879908555E-20, 6.794170380579684E-20, -3.485287100559392E-21,
                1.3474869526292895E-22, 1.171495214942793E-24, 8.976849904033136E-26, -4.4484353525145056E-26,
                -7.085201625470002E-27, -1.9149293363818004E-29, 1.9348625978022877E-29, -3.5967365059631985E-30 } };
        // Body is JPL Moon with NUMEXTRAP Mu
        final CelestialBody moon = new CelestialBody(){

            /** Serializable UID. */
            private static final long serialVersionUID = 6526492036122150529L;

            @Override
            public CelestialBodyEphemeris getEphemeris() {
                return null;
            }

            @Override
            public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
                // nothing to do
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                  final Frame frame) throws PatriusException {
                return CelestialBodyFactory.getMoon().getPVCoordinates(date, frame);
            }

            @Override
            public String getName() {
                return "Moon";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException {
                final CelestialBodyFrame frame;
                switch (iauPole) {
                    case CONSTANT:
                        // Get an inertially oriented, body centered frame taking into account
                        // only constant part of IAU pole data with respect to ICRF frame. The frame
                        // is always bound to the body center, and its axes have a fixed
                        // orientation with respect to other inertial frames.
                        frame = CelestialBodyFactory.getMoon().getInertialFrame(IAUPoleModelType.CONSTANT);
                        break;
                    case MEAN:
                        // Get an inertially oriented, body centered frame taking into account only
                        // constant and secular part of IAU pole data with respect to ICRF frame.
                        frame = CelestialBodyFactory.getMoon().getInertialFrame(IAUPoleModelType.MEAN);
                        break;
                    case TRUE:
                        // Get an inertially oriented, body centered frame taking into account
                        // constant, secular and harmonics part of IAU pole data with respect to
                        // ICRF frame.
                        frame = CelestialBodyFactory.getMoon().getInertialFrame(IAUPoleModelType.TRUE);
                        break;
                    default:
                        // The iauPole given as input is not implemented in this method.
                        throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
                }
                return frame;
            }

            @Override
            public double getGM() {
                return 4.90280012616E12;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
                final CelestialBodyFrame frame;
                switch (iauPole) {
                    case CONSTANT:
                        // Get a body oriented, body centered frame taking into account only constant part
                        // of IAU pole data with respect to inertially-oriented frame. The frame is always
                        // bound to the body center, and its axes have a fixed orientation with respect to
                        // the celestial body.
                        frame = CelestialBodyFactory.getMoon().getRotatingFrame(IAUPoleModelType.CONSTANT);
                        break;
                    case MEAN:
                        // Get a body oriented, body centered frame taking into account constant and secular
                        // part of IAU pole data with respect to mean equator frame. The frame is always
                        // bound to the body center, and its axes have a fixed orientation with respect to
                        // the celestial body.
                        frame = CelestialBodyFactory.getMoon().getRotatingFrame(IAUPoleModelType.MEAN);
                        break;
                    case TRUE:
                        // Get a body oriented, body centered frame taking into account constant, secular
                        // and harmonics part of IAU pole data with respect to true equator frame. The frame
                        // is always bound to the body center, and its axes have a fixed orientation with
                        // respect to the celestial body.
                        frame = CelestialBodyFactory.getMoon().getRotatingFrame(IAUPoleModelType.TRUE);
                        break;
                    default:
                        // The iauPole given as input is not implemented in this method.
                        throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
                }
                return frame;
            }

            @Override
            public CelestialBodyFrame getICRF() throws PatriusException {
                return CelestialBodyFactory.getMoon().getICRF();
            }

            @Override
            public CelestialBodyFrame getEME2000() throws PatriusException {
                return CelestialBodyFactory.getMoon().getEME2000();
            }

            @Override
            public BodyShape getShape() {
                return null;
            }

            @Override
            public void setShape(final BodyShape shapeIn) {
                // nothing to do
            }

            @Override
            public GravityModel getGravityModel() {
                GravityModel model = null;
                try {
                    model = new CunninghamGravityModel(this.getRotatingFrame(IAUPoleModelType.TRUE), 1738000.0,
                        this.getGM(), c, s);
                } catch (final PatriusException e) {
                    // Auto-generated catch block
                    e.printStackTrace();
                }
                return model;
            }

            @Override
            public void setGravityModel(final GravityModel gravityModelIn) {
                // nothing to do
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return null;
            }

            @Override
            public CelestialBodyIAUOrientation getOrientation() {
                return null;
            }

            @Override
            public void setOrientation(final CelestialBodyOrientation celestialBodyOrientation) {
                // nothing to do
            }

            @Override
            public void setGM(final double gmIn) {
                // nothing to do
            }

            @Override
            public CelestialBodyFrame getEclipticJ2000() throws PatriusException {
                // nothing to do
                return null;
            }
        };

        final GravityModel gravityModel = moon.getGravityModel();
        ((AbstractHarmonicGravityModel) gravityModel).setCentralTermContribution(true);
        final ThirdBodyAttraction force = new ThirdBodyAttraction(gravityModel);
        force.checkData(date, date);

        // Check acceleration
        final Vector3D expected = new Vector3D(-6.243954104676315E-7, -5.947904703052634E-8, -2.4623478761020065E-8);
        final Vector3D actual = force.computeAcceleration(state);
        Assert.assertEquals(0., expected.distance(actual), Precision.DOUBLE_COMPARISON_EPSILON);

        // Check partial derivatives
        final double[][] dAccdPosActual = new double[3][3];
        final double[][] dAccdPosExpected = {
            { -8.885327393451804E-14, -1.0582596895548305E-14, -4.615638595127055E-15 },
            { -1.0582596895548305E-14, 1.354422316911784E-13, 9.804460046558502E-14 },
            { -4.615638595127055E-15, 9.804460046558502E-14, -4.658895775666036E-14 } };
        force.addDAccDState(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(), dAccdPosActual);
        for (int i = 0; i < dAccdPosActual.length; i++) {
            for (int j = 0; j < dAccdPosActual[0].length; j++) {
                Assert.assertEquals(0., (dAccdPosActual[i][j] - dAccdPosExpected[i][j]) / dAccdPosExpected[i][j], 1E-7);
            }
        }
        FramesFactory.setConfiguration(svgConfig);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link ThirdBodyAttraction#computeAcceleration(SpacecraftState)}
     *
     * @description compute acceleration with multiplicative factor k and complex potential
     *
     * @testPassCriteria acceleration with k = 5 = 5 * acceleration with k = 1
     *
     * @referenceVersion 4.8.1
     *
     * @nonRegressionVersion 4.8.1
     */
    @Test
    public void testMultiplicativeFactor() throws PatriusException {
        final double mu = Constants.WGS84_EARTH_MU;
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000000, 0, 0, 0, 0, 0,
            PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, mu));
        final double[][] c = {
            { 1.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0 },
            { -2.032132919428845E-4, 6.203045246423681E-12, 2.238040265574716E-5 } };
        final double[][] s = {
            { 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0 },
            { 0.0, 1.0741043061033635E-9, -1.3432186078773792E-10 } };
        final MeeusSun sunActual = new MeeusSun();
        sunActual.setGravityModel(new CunninghamGravityModel(sunActual.getShape().getBodyFrame(), 3500000.,
            sunActual.getGM(), c, s));
        final MeeusSun sunExpected = new MeeusSun(){
            /** Serializable UID. */
            private static final long serialVersionUID = -3506441751244405864L;

            @Override
            public double getGM() {
                return 1.32712440017987E20 * 10.;
            }
        };
        sunExpected.setGravityModel(new CunninghamGravityModel(sunExpected.getShape().getBodyFrame(), 3500000.,
            1.32712440017987E20 * 10., c, s));
        ((AbstractHarmonicGravityModel) sunActual.getGravityModel()).setCentralTermContribution(false);
        final ThirdBodyAttraction actualModel = new ThirdBodyAttraction(sunActual.getGravityModel());
        actualModel.setMultiplicativeFactor(10.);
        ((AbstractHarmonicGravityModel) sunExpected.getGravityModel()).setCentralTermContribution(false);
        final ThirdBodyAttraction expectedModel = new ThirdBodyAttraction(sunExpected.getGravityModel());

        // Acceleration
        final Vector3D actual = actualModel.computeAcceleration(state);
        final Vector3D expected = expectedModel.computeAcceleration(state);
        Assert.assertEquals(expected.getX(), actual.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expected.getY(), actual.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expected.getZ(), actual.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
        // Partial derivatives
        final double[][] dAccdPosActual = new double[3][3];
        actualModel.addDAccDState(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(),
            dAccdPosActual);
        final double[][] dAccdPosExpected = new double[3][3];
        expectedModel.addDAccDState(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(),
            dAccdPosExpected);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(dAccdPosActual[i][j], dAccdPosExpected[i][j], 1E-24);
            }
        }
        // K value
        Assert.assertEquals(10., actualModel.getMultiplicativeFactor(), 0.);
    }

    /**
     * @throws PatriusException if the celestial body cannot be built
     * @testType UT
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException {
        ThirdBodyAttraction forceModel = new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel());

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL,
                ThirdBodyAttraction.class));
        }

        // Check an other constructor
        forceModel = new ThirdBodyAttraction(CelestialBodyFactory.getMoon().getGravityModel(), false);

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL,
                ThirdBodyAttraction.class));
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testGetters() throws PatriusException {
        // Build model with default constructor
        final ThirdBodyAttraction tba = new ThirdBodyAttraction(CelestialBodyFactory.getEarth().getGravityModel());
        Assert.assertTrue(tba.getGravityModel().equals(CelestialBodyFactory.getEarth().getGravityModel()));
    }

    /**
     * @description Try to build an body attraction model with a null gravity model.
     *
     * @testedMethod {@link ThirdBodyAttraction#ThirdBodyAttraction(GravityModel)}
     *
     * @testPassCriteria The {@link NullArgumentException} exception is returned as expected.
     */
    @Test
    public void testNullConstructor() {

        // Test the constructor with a null attribute
        try {
            new ThirdBodyAttraction(null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // Expected (check the message is enriched as expected with the parameter information)
            Assert.assertTrue(e.getMessage().contains("gravityModel"));
        }
    }

    @Before
    public void setUp() {
        this.mu = 3.986e14;
        Utils.setDataRoot("regular-data");
    }
}
