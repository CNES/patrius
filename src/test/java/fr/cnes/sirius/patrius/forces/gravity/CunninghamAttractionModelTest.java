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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Renamed Cunningham to UnnormalizedCunningham
 * VERSION::FA:228:26/03/2014:Corrected partial derivatives computation
 * VERSION::FA:93:31/03/
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:505:19/08/2015:corrected addDAccDParam exception
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1267:09/03/2018: Addition of getters for C and CS tables
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class CunninghamAttractionModelTest {

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testParamList() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");

        final Frame itrf = FramesFactory.getITRF();

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);

        final CunninghamAttractionModel att = new CunninghamAttractionModel(itrf, this.aeParam, this.muParam,
            c, s);

        // OK because computes derivatives wrt position
        Assert.assertEquals(2, att.getParameters().size());
        Assert.assertTrue(att.getParameters().contains(this.muParam));
        Assert.assertTrue(att.getParameters().contains(this.aeParam));
        Assert.assertFalse(att.getParameters().contains(
            new Parameter("mu", Constants.GRIM5C1_EARTH_MU)));

        // check MU
        Assert.assertTrue(Precision.equals(this.mu, att.getMu(), 0));

        // change mu value
        this.muParam.setValue(1.5);
        Assert.assertTrue(Precision.equals(1.5, att.getMu(), 0));

        Assert.assertEquals(2, att.getParameters().size());

    }

    /**
     * Additional partial derivatives tests to ensure the jacobian is correctly taken into account.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPartialDerivatives() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // date
        final AbsoluteDate date = new AbsoluteDate();

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE, gcrf,
            date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);

        final CunninghamAttractionModel att = new CunninghamAttractionModel(itrf, ae, mu, c, s);

        // partial derivatives
        final double[][] dAccdPos = new double[6][6];
        att.addDAccDState(state, dAccdPos, new double[6][6]);

        System.out.println("ANALYTICAL");

        this.print(dAccdPos);
        System.out.println();

        /*
         * ====================================== finite diff _ DELTAS IN GCRF
         */
        final Vector3D pos = orbit.getPVCoordinates(gcrf).getPosition();
        final Vector3D vel = orbit.getPVCoordinates(gcrf).getVelocity();

        /* ===================================== */

        final double dh = .5;

        // positions
        final Vector3D ppx = pos.add(Vector3D.PLUS_I.scalarMultiply(dh));
        final Vector3D ppy = pos.add(Vector3D.PLUS_J.scalarMultiply(dh));
        final Vector3D ppz = pos.add(Vector3D.PLUS_K.scalarMultiply(dh));

        final Vector3D pmx = pos.add(Vector3D.PLUS_I.scalarMultiply(-dh));
        final Vector3D pmy = pos.add(Vector3D.PLUS_J.scalarMultiply(-dh));
        final Vector3D pmz = pos.add(Vector3D.PLUS_K.scalarMultiply(-dh));

        // pv coordinates
        final PVCoordinates pvpx = new PVCoordinates(ppx, vel);
        final PVCoordinates pvpy = new PVCoordinates(ppy, vel);
        final PVCoordinates pvpz = new PVCoordinates(ppz, vel);

        final PVCoordinates pvmx = new PVCoordinates(pmx, vel);
        final PVCoordinates pvmy = new PVCoordinates(pmy, vel);
        final PVCoordinates pvmz = new PVCoordinates(pmz, vel);

        // orbits
        final CartesianOrbit opx = new CartesianOrbit(pvpx, gcrf, date, mu);
        final CartesianOrbit opy = new CartesianOrbit(pvpy, gcrf, date, mu);
        final CartesianOrbit opz = new CartesianOrbit(pvpz, gcrf, date, mu);

        final CartesianOrbit omx = new CartesianOrbit(pvmx, gcrf, date, mu);
        final CartesianOrbit omy = new CartesianOrbit(pvmy, gcrf, date, mu);
        final CartesianOrbit omz = new CartesianOrbit(pvmz, gcrf, date, mu);

        // states
        final SpacecraftState sspx = new SpacecraftState(opx);
        final SpacecraftState sspy = new SpacecraftState(opy);
        final SpacecraftState sspz = new SpacecraftState(opz);

        final SpacecraftState ssmx = new SpacecraftState(omx);
        final SpacecraftState ssmy = new SpacecraftState(omy);
        final SpacecraftState ssmz = new SpacecraftState(omz);

        // acc
        final Vector3D apx = att.computeAcceleration(sspx);
        final Vector3D apy = att.computeAcceleration(sspy);
        final Vector3D apz = att.computeAcceleration(sspz);

        final Vector3D amx = att.computeAcceleration(ssmx);
        final Vector3D amy = att.computeAcceleration(ssmy);
        final Vector3D amz = att.computeAcceleration(ssmz);

        // pds
        final Vector3D pdx = apx.subtract(amx).scalarMultiply(1 / (2 * dh));
        final Vector3D pdy = apy.subtract(amy).scalarMultiply(1 / (2 * dh));
        final Vector3D pdz = apz.subtract(amz).scalarMultiply(1 / (2 * dh));

        final double[][] acc = { pdx.toArray(), pdy.toArray(), pdz.toArray() };
        final double[][] tacc = this.transpose(acc);

        System.out.println("FINITE DIFFERENCES");
        this.print(tacc);

        final double[][] diff = new double[3][3];
        for (int ii = 0; ii < diff.length; ii++) {
            for (int j = 0; j < diff[ii].length; j++) {
                diff[ii][j] = (dAccdPos[ii][j] - tacc[ii][j]) / dAccdPos[ii][j];
                Assert.assertEquals(0, diff[ii][j], 5e-5);
            }
        }
        System.out.println();
        System.out.println("RELATIVE DIFFRENCE");
        this.print(diff);
    }

    void print(final double[][] d) {
        for (final double[] row : d) {
            for (final double e : row) {
                System.out.printf("%.16e\t", e);
            }
            System.out.println();
        }
    }

    double[][] transpose(final double[][] d) {

        final double[][] dt = new double[d[0].length][d.length];

        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                dt[j][i] = d[i][j];
            }
        }

        return dt;

    }

    // rough test to determine if J2 alone creates heliosynchronism
    @Test
    public void testHelioSynchronous() throws ParseException, FileNotFoundException,
                                      PatriusException {

        // initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
            new TimeComponents(13, 59, 27.816), TimeScalesFactory.getUTC());
        final Transform itrfToEME2000 = this.ITRF2005.getTransformTo(FramesFactory.getEME2000(), date);
        final Vector3D pole = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        final Frame poleAligned = new Frame(FramesFactory.getEME2000(), new Transform(date,
            new Rotation(Vector3D.PLUS_K, pole)), "pole aligned", true);

        final double i = MathLib.toRadians(98.7);
        final double omega = MathLib.toRadians(93.0);
        final double OMEGA = MathLib.toRadians(15.0 * 22.5);
        final Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i, omega, OMEGA, 0,
            PositionAngle.MEAN, poleAligned, date, this.mu);

        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = this.c20;
        double[][] s = new double[3][1];
        this.propagator.addForceModel(new CunninghamAttractionModel(this.ITRF2005, 6378136.460, this.mu, c, s));

        // let the step handler perform the test
        this.propagator.setMasterMode(Constants.JULIAN_DAY, new SpotStepHandler(date, this.mu));
        this.propagator.setInitialState(new SpacecraftState(orbit));
        this.propagator.propagate(date.shiftedBy(7 * Constants.JULIAN_DAY));
        Assert.assertTrue(this.propagator.getCalls() < 9200);

        // coverage tests:
        final CunninghamAttractionModel model = new CunninghamAttractionModel(this.ITRF2005, this.aeParam,
            this.muParam, c, s);
        this.muParam.setValue(5.);
        Assert.assertEquals(5., model.getMu(), 0.0);

        c = new double[1][0];
        s = new double[3][1];
        boolean rez = false;
        try {
            new CunninghamAttractionModel(this.ITRF2005, 6378136.460, this.mu, c, s);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

    }

    private static class SpotStepHandler implements PatriusFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 6818305166004802991L;

        public SpotStepHandler(final AbsoluteDate date, final double mu) throws PatriusException {
            this.sun = CelestialBodyFactory.getSun();
            this.previous = Double.NaN;
        }

        private final PVCoordinatesProvider sun;
        private double previous;

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                        throws PropagationException {

            final AbsoluteDate current = currentState.getDate();
            Vector3D sunPos;
            try {
                sunPos = this.sun.getPVCoordinates(current, FramesFactory.getEME2000()).getPosition();
            } catch (final PatriusException e) {
                throw new PropagationException(e);
            }
            final Vector3D normal = currentState.getPVCoordinates().getMomentum();
            final double angle = Vector3D.angle(sunPos, normal);
            if (!Double.isNaN(this.previous)) {
                Assert.assertEquals(this.previous, angle, 0.0013);
            }
            this.previous = angle;
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    @Test
    public void testEcksteinHechlerReference() throws ParseException, FileNotFoundException,
                                              PatriusException {

        // Definition of initial conditions with position and velocity
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        final Transform itrfToEME2000 = this.ITRF2005.getTransformTo(FramesFactory.getEME2000(), date);
        final Vector3D pole = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        final Frame poleAligned = new Frame(FramesFactory.getEME2000(), new Transform(date,
            new Rotation(Vector3D.PLUS_K, pole)), "pole aligned", true);

        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            poleAligned, date, this.mu);

        this.propagator.addForceModel(new CunninghamAttractionModel(this.ITRF2005, this.ae, this.mu, new double[][] {
            { 0.0 }, { 0.0 }, { this.c20 }, { this.c30 }, { this.c40 }, { this.c50 }, { this.c60 }, }, new double[][] {
            { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, }));

        // let the step handler perform the test
        this.propagator.setInitialState(new SpacecraftState(initialOrbit));
        this.propagator.setMasterMode(20, new EckStepHandler(initialOrbit, this.ae, this.c20, this.c30, this.c40,
            this.c50, this.c60));
        this.propagator.propagate(date.shiftedBy(50000));
        Assert.assertTrue(this.propagator.getCalls() < 1300);

    }

    private static class EckStepHandler implements PatriusFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 6132817809836153771L;

        /** Body mu */
        private static final double mu = 3.986004415e+14;

        private EckStepHandler(final Orbit initialOrbit, final double ae, final double c20,
            final double c30, final double c40, final double c50, final double c60)
            throws FileNotFoundException, PatriusException {
            this.referencePropagator = new EcksteinHechlerPropagator(initialOrbit, ae, mu,
                initialOrbit.getFrame(), c20, c30, c40, c50, c60, ParametersType.OSCULATING);
        }

        private final EcksteinHechlerPropagator referencePropagator;

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            try {

                final SpacecraftState EHPOrbit = this.referencePropagator.propagate(currentState
                    .getDate());
                final Vector3D posEHP = EHPOrbit.getPVCoordinates().getPosition();
                final Vector3D posDROZ = currentState.getPVCoordinates().getPosition();
                final Vector3D velEHP = EHPOrbit.getPVCoordinates().getVelocity();
                final Vector3D dif = posEHP.subtract(posDROZ);

                final Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
                final Vector3D W = EHPOrbit.getPVCoordinates().getMomentum().normalize();
                final Vector3D N = Vector3D.crossProduct(W, T);

                Assert.assertTrue(dif.getNorm() < 111);
                Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(dif, T)) < 111);
                Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(dif, N)) < 54);
                Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(dif, W)) < 12);

            } catch (final PropagationException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * test the difference with the Cunningham model
     * 
     * @throws PatriusException
     * @throws ParseException
     */
    @Test
    public void testZonalWithDrozinerReference() throws PatriusException, ParseException {
        // initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
            new TimeComponents(13, 59, 27.816), TimeScalesFactory.getUTC());
        final double i = MathLib.toRadians(98.7);
        final double omega = MathLib.toRadians(93.0);
        final double OMEGA = MathLib.toRadians(15.0 * 22.5);
        final Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i, omega, OMEGA, 0,
            PositionAngle.MEAN, FramesFactory.getEME2000(), date, this.mu);

        this.propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(1000));
        this.propagator.addForceModel(new CunninghamAttractionModel(this.ITRF2005, this.ae, this.mu, new double[][] {
            { 0.0 }, { 0.0 }, { this.c20 }, { this.c30 }, { this.c40 }, { this.c50 }, { this.c60 }, }, new double[][] {
            { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, }));

        this.propagator.setInitialState(new SpacecraftState(orbit));
        final SpacecraftState cunnOrb = this.propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        this.propagator.removeForceModels();

        this.propagator.addForceModel(new DrozinerAttractionModel(this.ITRF2005, this.ae, this.mu, new double[][] {
            { 0.0 }, { 0.0 }, { this.c20 }, { this.c30 }, { this.c40 }, { this.c50 }, { this.c60 }, }, new double[][] {
            { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, }));

        this.propagator.setInitialState(new SpacecraftState(orbit));
        final SpacecraftState drozOrb = this.propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        final Vector3D dif = cunnOrb.getPVCoordinates().getPosition()
            .subtract(drozOrb.getPVCoordinates().getPosition());
        Assert.assertEquals(0, dif.getNorm(), 3.1e-7);
        Assert.assertTrue(this.propagator.getCalls() < 400);
    }

    /**
     * Test the partial derivatives computation.
     * 
     * @throws PatriusException when an error occurs.
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public final void testAddDAccDState() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("potential");

        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader(
            "GRGS_EIGEN_GL04S.txt", true));

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // Here, we get the data as extracted from the file.
        final int n = 100;
        final double[][] C = provider.getC(n, 100, false);
        final double[][] S = provider.getS(n, 100, false);

        Utils.setDataRoot("regular-data");

        final CunninghamAttractionModel grav = new CunninghamAttractionModel(
            FramesFactory.getITRF(), provider.getAe(), provider.getMu(), C, S);

        double[][] dAdP = new double[3][3];
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        final double[][] expDAccdPos = new double[3][3];

        final AbsoluteDate date = new AbsoluteDate(1970, 3, 5, 0, 24, 0, TimeScalesFactory.getTAI());

        Frame referenceFrame = FramesFactory.getITRF();

        // PV coordinates in GCRF frame
        Vector3D pos = new Vector3D(6.4688587830467382E+06, -1.8805091845627432E+06,
            -1.3293159229471583E+04);
        Vector3D vel = new Vector3D(2.1471807451962504E+03, 7.3823935125280523E+03,
            -1.1409758242487955E+01);
        final PVCoordinates pv = new PVCoordinates(pos, vel);

        dAdP = GravityToolbox.computeDAccDPos(pv, date, provider.getAe(), provider.getMu(),
                grav.getC(), grav.getS());

        // compute partial derivatives by finite differences
        // with respect to spacecraft position

        // compute nominal acceleration
        Vector3D nominalAcc = grav.computeAcceleration(pv, date);

        // step
        double hPos = MathLib.sqrt(Precision.EPSILON) * pos.getNorm();

        // shifted position (dX)
        PVCoordinates shiftedPVdX = new PVCoordinates(new Vector3D(pos.getX() + hPos, pos.getY(),
            pos.getZ()), pv.getVelocity());
        // compute shifted acceleration
        Vector3D shiftedAcc = grav.computeAcceleration(shiftedPVdX, date);

        expDAccdPos[0][0] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][0] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][0] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // shifted position (dY)
        PVCoordinates shiftedPVdY = new PVCoordinates(new Vector3D(pos.getX(), pos.getY() + hPos,
            pos.getZ()), pv.getVelocity());
        // compute shifted acceleration
        shiftedAcc = grav.computeAcceleration(shiftedPVdY, date);

        expDAccdPos[0][1] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][1] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][1] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // shifted position (dZ)
        PVCoordinates shiftedPVdZ = new PVCoordinates(new Vector3D(pos.getX(), pos.getY(),
            pos.getZ() + hPos), pv.getVelocity());
        // compute shifted acceleration
        shiftedAcc = grav.computeAcceleration(shiftedPVdZ, date);

        expDAccdPos[0][2] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][2] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][2] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // compare with expected acceleration
        for (int i = 0; i < dAdP.length; i++) {
            for (int j = 0; j < dAdP.length; j++) {
                Assert.assertEquals(expDAccdPos[i][j], dAdP[i][j],
                    Precision.DOUBLE_COMPARISON_EPSILON);
            }
        }

        // compute partial derivatives with addDAccDState method
        referenceFrame = FramesFactory.getGCRF();
        final Frame frame = FramesFactory.getITRF();

        final Transform t = frame.getTransformTo(referenceFrame, date);

        final PVCoordinates pvGcrf = t.transformPVCoordinates(pv);
        pos = pvGcrf.getPosition();
        vel = pvGcrf.getVelocity();

        final Orbit orbit = new CartesianOrbit(pvGcrf, FramesFactory.getGCRF(), date, this.mu);
        final SpacecraftState scr = new SpacecraftState(orbit);
        grav.addDAccDState(scr, dAccdPos, dAccdVel);

        // compute nominal acceleration
        nominalAcc = grav.computeAcceleration(scr);

        // step
        hPos = MathLib.sqrt(Precision.EPSILON) * pos.getNorm();

        // shifted position (dX)
        shiftedPVdX = new PVCoordinates(new Vector3D(pos.getX() + hPos, pos.getY(), pos.getZ()),
            pv.getVelocity());
        // compute shifted acceleration
        shiftedAcc = grav.computeAcceleration(scr);

        expDAccdPos[0][0] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][0] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][0] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // shifted position (dY)
        shiftedPVdY = new PVCoordinates(new Vector3D(pos.getX(), pos.getY() + hPos, pos.getZ()),
            pv.getVelocity());
        // compute shifted acceleration
        shiftedAcc = grav.computeAcceleration(scr);

        expDAccdPos[0][1] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][1] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][1] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // shifted position (dZ)
        shiftedPVdZ = new PVCoordinates(new Vector3D(pos.getX(), pos.getY(), pos.getZ() + hPos),
            pv.getVelocity());
        // compute shifted acceleration
        shiftedAcc = grav.computeAcceleration(scr);

        expDAccdPos[0][2] = (shiftedAcc.getX() - nominalAcc.getX()) / hPos;
        expDAccdPos[1][2] = (shiftedAcc.getY() - nominalAcc.getY()) / hPos;
        expDAccdPos[2][2] = (shiftedAcc.getZ() - nominalAcc.getZ()) / hPos;

        // compare with expected acceleration
        for (int i = 0; i < dAccdPos.length; i++) {
            for (int j = 0; j < dAccdPos.length; j++) {
                if (expDAccdPos[i][j] == 0) {
                    Assert.assertEquals(expDAccdPos[i][j], dAccdPos[i][j], 5e-7);
                } else {
                    Assert.assertEquals(0,
                        (expDAccdPos[i][j] - dAccdPos[i][j]) / expDAccdPos[i][j], 5e-7);
                }
            }
        }
    }

    /**
     * Test the partial derivatives computation.
     * 
     * @throws PatriusException when an error occurs
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public final void testAddDAccDParam() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("potential");

        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader(
            "GRGS_EIGEN_GL04S.txt", true));

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // Here, we get the data as extracted from the file.
        final int n = 100;
        final double[][] C = provider.getC(n, 100, false);
        final double[][] S = provider.getS(n, 100, false);

        Utils.setDataRoot("regular-data");
        final CunninghamAttractionModel grav = new CunninghamAttractionModel(
            FramesFactory.getITRF(), provider.getAe(), provider.getMu(), C, S);

        final double[] dAccdParam = new double[3];

        try {
            grav.addDAccDParam(null, new Parameter("null", .0), dAccdParam);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        } catch (final RuntimeException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedMethod {@link CunninghamAttractionModel#supportsJacobianParameter(Parameter)}
     * 
     * @description Test created for coverage purpose
     * 
     * @input CunninghamAttractionModel
     * 
     * @output a boolean
     * 
     * @testPassCriteria CunninghamAttractionModel#supportsJacobianParameter(Parameter) should
     *                   return false
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    public final void testSupportsJacobianParameter() throws IOException, ParseException,
                                                     PatriusException {
        Utils.setDataRoot("potential");

        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader(
            "GRGS_EIGEN_GL04S.txt", true));

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // Here, we get the data as extracted from the file.
        final int n = 100;
        final double[][] C = provider.getC(n, 100, false);
        final double[][] S = provider.getS(n, 100, false);

        Utils.setDataRoot("regular-data");
        final CunninghamAttractionModel grav = new CunninghamAttractionModel(
            FramesFactory.getITRF(), provider.getAe(), provider.getMu(), C, S);
        final Parameter p = new Parameter("name", 0.);
        Assert.assertFalse(grav.supportsJacobianParameter(p));
    }

    @Test
    public void testIssue97() throws IOException, ParseException, PatriusException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06,
            -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03,
            -1.14097953925384523e+01);
        final SpacecraftState spacecraftState = new SpacecraftState(new CartesianOrbit(
            new PVCoordinates(pos, vel), FramesFactory.getGCRF(), new AbsoluteDate(2005, 3, 5,
                0, 24, 0.0, TimeScalesFactory.getTAI()), provider.getMu()));

        final AccelerationRetriever accelerationRetriever = new AccelerationRetriever();
        for (int i = 2; i <= 69; i++) {
            // we get the data as extracted from the file
            final double[][] C = provider.getC(i, i, false);
            final double[][] S = provider.getS(i, i, false);
            // perturbing force (ITRF2008 central body frame)
            final ForceModel cunModel = new CunninghamAttractionModel(FramesFactory.getITRF(),
                provider.getAe(), provider.getMu(), C, S);
            final ForceModel droModel = new DrozinerAttractionModel(FramesFactory.getITRF(),
                provider.getAe(), provider.getMu(), C, S);

            /**
             * Compute acceleration
             */
            cunModel.addContribution(spacecraftState, accelerationRetriever);
            final Vector3D cunGamma = accelerationRetriever.getAcceleration();
            droModel.addContribution(spacecraftState, accelerationRetriever);
            final Vector3D droGamma = accelerationRetriever.getAcceleration();
            Assert.assertEquals(0.0, cunGamma.subtract(droGamma).getNorm(),
                2.2e-9 * droGamma.getNorm());

        }

    }

    @Test
    public void testNewtonianAttraction() throws IllegalArgumentException, PatriusException {
        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06,
            -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03,
            -1.14097953925384523e+01);
        final SpacecraftState spacecraftState = new SpacecraftState(new CartesianOrbit(
            new PVCoordinates(pos, vel), FramesFactory.getGCRF(), new AbsoluteDate(2005, 3, 5,
                0, 24, 0.0, TimeScalesFactory.getTAI()), this.mu));
        // coverage tests:
        final NewtonianAttraction model = new NewtonianAttraction(this.muParam);
        this.muParam.setValue(5.);
        Assert.assertEquals(5., model.getMu(), 0.0);
        Assert.assertNotNull(model.computeAcceleration(spacecraftState));
        final double[] dAccdParam = new double[3];
        model.addDAccDParam(spacecraftState, this.muParam, dAccdParam);
        Assert.assertNotNull(dAccdParam);

        Assert.assertTrue(model.getParameters().size() == 1);
        Assert.assertTrue(model.getParameters().contains(this.muParam));
    }

    private static class AccelerationRetriever implements TimeDerivativesEquations {

        private static final long serialVersionUID = -4616792058307814184L;
        private Vector3D acceleration;

        @Override
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit) {
        }

        @Override
        public void addKeplerContribution(final double mu) {
        }

        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            this.acceleration = new Vector3D(x, y, z);
        }

        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame) {
        }

        public Vector3D getAcceleration() {
            return this.acceleration;
        }

        @Override
        public void addAdditionalStateDerivative(final String name, final double[] pDot) {
        }

    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link CunninghamAttractionModel #CunninghamAttractionModel(Frame, double, double, double[][], double[][], double[][], double[][], boolean) }
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link CunninghamAttractionModelAttractionModel}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at
     *                   construction : instantiation is done with null tabs of normalized
     *                   coefficients used for partial derivatives computation
     * 
     * @throws PatriusException when an Orekit error occurs
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("potential");

        // Gravity
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader(
            "GRGS_EIGEN_GL04S.txt", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        // Spacecraft
        final AbsoluteDate date = new AbsoluteDate(1970, 3, 5, 0, 24, 0, TimeScalesFactory.getTAI());
        final Frame referenceFrame = FramesFactory.getITRF();
        final Vector3D pos = new Vector3D(6.4688587830467382E+06, -1.8805091845627432E+06,
            -1.3293159229471583E+04);
        final Vector3D vel = new Vector3D(2.1471807451962504E+03, 7.3823935125280523E+03,
            -1.1409758242487955E+01);
        final PVCoordinates pv = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pv, referenceFrame, date, this.mu);
        final SpacecraftState scr = new SpacecraftState(orbit);

        // Coefficients
        final int n = 100;
        final double[][] C = provider.getC(n, 100, false);
        final double[][] S = provider.getS(n, 100, false);

        // Instantiation
        final CunninghamAttractionModel grav = new CunninghamAttractionModel(
            FramesFactory.getITRF(), provider.getAe(), provider.getMu(), C, S, 0, 0);

        // Check partial derivatives computation is well deactivated
        Assert.assertFalse(grav.computeGradientPosition());
        // Partial derivatives wrt velocity are always null in Cunningham model
        Assert.assertFalse(grav.computeGradientVelocity());

        // Partial derivatives
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        grav.addDAccDState(scr, dAccdPos, dAccdVel);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
            }
        }

    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link CunninghamAttractionModel#addDAccDState(SpacecraftState, double[][], double[][])}
     * 
     * @description This test checks that:
     *              <ul>
     *              <li>The numerical propagation of a given orbit using instances of CunninghamAttractionModel with
     *              fixed degree/order (60, 60) for acceleration but different degree/order (60, 60) and (59, 59) for
     *              partial derivatives lead to the same [position, velocity] state but slighty different state
     *              transition matrix.</li>
     *              <li>The partial derivatives of model (60, 60) for acceleration and (59, 59) for partial derivatives
     *              are the same than of model (59, 59) for acceleration and (59, 59) for partial derivatives.</li>
     *              <ul>
     * 
     * @input instances of {@link CunninghamAttractionModel}
     * 
     * @output positions, velocities of final orbits, partials derivatives
     * 
     * @testPassCriteria the [positions, velocities] must be equals, state transition matrix
     *                   "almost" the same (relative difference < 1E-6)
     * 
     * @throws PatriusException when an Orekit error occurs
     * @throws ParseException
     * @throws IOException
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationDifferentDegreeOrder() throws PatriusException, IOException,
                                                     ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // date
        final AbsoluteDate date = new AbsoluteDate();

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // SpacecraftState
        final KeplerianOrbit orbit = new KeplerianOrbit(7E6, 0.001, 0.93, 0, 0, 0,
            PositionAngle.TRUE, gcrf, date, mu);
        SpacecraftState state1 = new SpacecraftState(orbit);
        SpacecraftState state2 = new SpacecraftState(orbit);
        final double t = orbit.getKeplerianPeriod();

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        // Create 3 instances of CunninghamAttractionModel with different degrees/orders
        final double[][] c1 = data.getC(60, 60, false);
        final double[][] s1 = data.getS(60, 60, false);
        final double[][] c2 = data.getC(59, 59, false);
        final double[][] s2 = data.getS(59, 59, false);
        final CunninghamAttractionModel model1 = new CunninghamAttractionModel(itrf, ae, mu, c1,
            s1, 60, 60);
        final CunninghamAttractionModel model2 = new CunninghamAttractionModel(itrf, ae, mu, c1,
            s1, 59, 59);
        final CunninghamAttractionModel model3 = new CunninghamAttractionModel(itrf, ae, mu, c2,
            s2, 59, 59);

        // Propagators
        final double step = 60;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);
        final NumericalPropagator prop1 = new NumericalPropagator(integrator);
        final NumericalPropagator prop2 = new NumericalPropagator(integrator);

        final PartialDerivativesEquations eq1 = new PartialDerivativesEquations("partial", prop1);
        state1 = eq1.setInitialJacobians(state1);
        prop1.setInitialState(state1);
        prop1.addForceModel(model1);
        final PartialDerivativesEquations eq2 = new PartialDerivativesEquations("partial", prop2);
        state2 = eq2.setInitialJacobians(state2);
        prop2.setInitialState(state2);
        prop2.addForceModel(model2);

        // Propagation : final state
        final SpacecraftState FinalState1 = prop1.propagate(date.shiftedBy(t));
        final SpacecraftState FinalState2 = prop2.propagate(date.shiftedBy(t));

        // Positions and velocities must be the same whereas degrees/orders are different for each
        // model
        final Vector3D pos1 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D pos2 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D vel1 = FinalState2.getPVCoordinates().getVelocity();
        final Vector3D vel2 = FinalState2.getPVCoordinates().getVelocity();

        Assert.assertEquals(0., pos1.distance(pos2), 0.);
        Assert.assertEquals(0., vel1.distance(vel2), 0.);

        // Check that partial derivatives are different, but "nearly" the same
        final double epsilon = 6.0E-5;
        final double[] stm1 = FinalState1.getAdditionalState("partial");
        final double[] stm2 = FinalState2.getAdditionalState("partial");
        for (int i = 0; i < stm1.length; i++) {
            Assert.assertEquals(0., (stm1[i] - stm2[i]) / stm1[i], epsilon);
            Assert.assertFalse(stm1[i] == stm2[i]);
        }

        // Check that different instances of CunninghamAttractionModel returns same partial
        // derivatives
        final double[][] dAccdPos = new double[6][6];
        final double[][] dAccdVel = new double[6][6];
        final double[][] dAccdPos2 = new double[6][6];
        final double[][] dAccdVel2 = new double[6][6];
        model2.addDAccDState(state1, dAccdPos, dAccdVel);
        model3.addDAccDState(state1, dAccdPos2, dAccdVel2);

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
                Assert.assertEquals(dAccdPos[j][k], dAccdPos2[j][k], 0);
                Assert.assertEquals(dAccdVel[j][k], dAccdVel2[j][k], 0);
            }
        }

        // Check degree and order upper limit
        try {
            new CunninghamAttractionModel(itrf, ae, mu, c1, s1, 61, 60);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new CunninghamAttractionModel(itrf, ae, mu, c1, s1, 60, 61);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * FA 648 : Raise an exception if input order or degree for partial derivatives computation is
     * negative.
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * 
     * @testType UT
     * @testedMethod {@link CunninghamAttractionModel#CunninghamAttractionModel(Frame, Parameter, Parameter, double[][], double[][], int, int)}
     *               .
     * 
     * @description Test to cover the case of a CunninghamAttractionModel creation with either a
     *              negative order for partial derivatives computation or a negative degree : an
     *              exception should be raised.
     * 
     * @testPassCriteria An exception should be raised when calling the constructor with a bad
     *                   input.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCunninghamNegativeOrderOrDegree() throws IOException, ParseException,
                                                     PatriusException {
        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Non inertial frame
        final Frame nonInertialEarthFrame = FramesFactory.getITRF();

        // Date
        final AbsoluteDate date = new AbsoluteDate();

        // Constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // Orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE,
            nonInertialEarthFrame, date, mu);
        new SpacecraftState(orbit);

        // Gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // A provider for the GRGS data is created
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        // Degree
        final int degree = 50;

        // Order
        final int order = 50;

        // unnormalized Cosine coefficients
        final double[][] unnormalizedC = provider.getC(degree, order, false);

        // unnormalized Sine coefficients
        final double[][] unnormalizedS = provider.getS(degree, order, false);

        // Balmino model : order for partial derivatives < 0, degree > 0
        final int degreePD = degree;
        final int orderPDNeg = -1;

        // An exception should be raised here !
        try {
            new CunninghamAttractionModel(
                nonInertialEarthFrame, new Parameter("ae", provider.getAe()), new Parameter(
                    "mu", provider.getMu()), unnormalizedC, unnormalizedS, degreePD,
                orderPDNeg);
        } catch (final IllegalArgumentException exc) {
            Assert.assertTrue(true);
        }

        // Balmino model : order for partial derivatives > 0, degree < 0
        final int orderPD = order;
        final int degreePDNeg = -1;

        new CunninghamAttractionModel(
            nonInertialEarthFrame, new Parameter("ae", provider.getAe()), new Parameter("mu",
                provider.getMu()), unnormalizedC, unnormalizedS, degreePDNeg, orderPD);
    }

    @Before
    public void setUp() throws PatriusException {
        this.ITRF2005 = null;
        this.propagator = null;
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        try {
            // Eigen c1 model truncated to degree 6
            this.mu = 3.986004415e+14;
            this.muParam = new Parameter("mu", this.mu);
            this.ae = 6378136.460;
            this.aeParam = new Parameter("ae", this.ae);
            this.c20 = -1.08262631303e-3;
            this.c30 = 2.53248017972e-6;
            this.c40 = 1.61994537014e-6;
            this.c50 = 2.27888264414e-7;
            this.c60 = -5.40618601332e-7;

            this.ITRF2005 = FramesFactory.getITRF();
            final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
            final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
            final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001,
                1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            this.propagator = new NumericalPropagator(integrator);
        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() throws PatriusException {
        this.ITRF2005 = null;
        this.propagator = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    @Test
    public void testCSTables() {
        // Tabs creation
        final double[][] cCoefsT = new double[2][3];
        cCoefsT[0][0] = 9;
        cCoefsT[0][1] = 6;
        cCoefsT[1][0] = 4;
        cCoefsT[1][1] = 3;
        cCoefsT[0][2] = 10;
        cCoefsT[1][2] = 11;
        final double[][] sCoefsT = new double[2][3];
        sCoefsT[0][0] = 9;
        sCoefsT[0][1] = 3;
        sCoefsT[1][0] = 31;
        sCoefsT[1][1] = 1;
        sCoefsT[0][2] = 6;
        sCoefsT[1][2] = 2;

        final CunninghamAttractionModel model = new CunninghamAttractionModel(FramesFactory.getGCRF(),
            Constants.CNES_STELA_AE, Constants.CNES_STELA_MU, cCoefsT, sCoefsT);
        // Get values
        Assert.assertEquals(3, model.getC().length, 0);
        Assert.assertEquals(2, model.getC()[0].length, 0);
        Assert.assertEquals(4, model.getC()[0][1], 0);
        Assert.assertEquals(3, model.getC()[1][1], 0);
        Assert.assertEquals(6, model.getC()[1][0], 0);
        Assert.assertEquals(11, model.getC()[2][1], 0);

        Assert.assertEquals(2, model.getS()[0].length, 0);
        Assert.assertEquals(9, model.getS()[0][0], 0);
        Assert.assertEquals(3, model.getS()[1][0], 0);
        Assert.assertEquals(6, model.getS()[2][0], 0);
        Assert.assertEquals(1, model.getS()[1][1], 0);
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {
        // Tabs creation
        final double[][] cCoefs = new double[2][2];
        cCoefs[0][0] = 1;
        cCoefs[0][1] = 2;
        cCoefs[1][0] = 3;
        cCoefs[1][1] = 4;

        final double[][] sCoefs = new double[2][2];
        sCoefs[0][0] = 5;
        sCoefs[0][1] = 6;
        sCoefs[1][0] = 7;
        sCoefs[1][1] = 8;
        final Frame frame = FramesFactory.getGCRF();
        final CunninghamAttractionModel model = new CunninghamAttractionModel(frame,
            Constants.CNES_STELA_AE, Constants.CNES_STELA_MU, cCoefs, sCoefs);
        Assert.assertEquals(true, model.getBodyFrame().getName().equals(frame.getName()));

    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link CunninghamAttractionModel#computeAcceleration(SpacecraftState)}
     * 
     * @description compute acceleration with multiplicative factor k
     * 
     * @testPassCriteria acceleration with k = 5 = 5 * acceleration with k = 1
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public final void testMultiplicativeFactor() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("normalized");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();
        final double[][] c = pot.getC(4, 4, false);
        final double[][] s = pot.getS(4, 4, false);
        final double ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.WGS84_EARTH_MU;
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000000, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,mu));
        final CunninghamAttractionModel actualModel = new CunninghamAttractionModel(FramesFactory.getGCRF(), new Parameter("", ae),
                new Parameter("", mu), c, s, 4, 4);
        actualModel.setMultiplicativeFactor(5.);
        final CunninghamAttractionModel expectedModel = new CunninghamAttractionModel(FramesFactory.getGCRF(), new Parameter("", ae),
                new Parameter("", mu), c, s, 4, 4);
        
        // Acceleration
        final Vector3D actual = actualModel.computeAcceleration(state);
        final Vector3D expected = expectedModel.computeAcceleration(state).scalarMultiply(5.);
        Assert.assertEquals(expected, actual);
        // Partial derivatives
        final double[][] dAccdPosActual = new double[3][3];
        final double[][] dAccdVelActual = new double[3][3];
        actualModel.addDAccDState(state, dAccdPosActual, dAccdVelActual);
        final double[][] dAccdPosExpected = new double[3][3];
        final double[][] dAccdVelExpected = new double[3][3];
        expectedModel.addDAccDState(state, dAccdPosExpected, dAccdVelExpected);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(dAccdPosActual[i][j], dAccdPosExpected[i][j] * 5., 0.);
                Assert.assertEquals(dAccdVelActual[i][j], dAccdVelExpected[i][j] * 5., 0.);
            }
        }
        // K value
        Assert.assertEquals(5., actualModel.getMultiplicativeFactor(), 0.);
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedMethod {@link ForceModel#enrichParameterDescriptors()}
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws IOException, ParseException,
            PatriusException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");

        // gravity
        GravityFieldFactory
                .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);
        final Frame itrf = FramesFactory.getITRF();

        final CunninghamAttractionModel forceModel = new CunninghamAttractionModel(itrf,
                this.aeParam, this.muParam, c, s);

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }

    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;
    private double mu;
    private double ae;
    private Parameter muParam;
    private Parameter aeParam;

    private Frame ITRF2005;
    private NumericalPropagator propagator;

}
