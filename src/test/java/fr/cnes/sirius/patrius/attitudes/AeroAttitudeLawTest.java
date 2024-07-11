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
 * @history created 23/01/17
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:595:23/01/2017:Creation of AeroAttitudeLaw class
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.PolarMotion;
import fr.cnes.sirius.patrius.frames.configuration.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ReentryParameters;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the AeroAttitudeLaw class.
 *              </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: AeroAttitudeLawTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 3.4
 */
public class AeroAttitudeLawTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Aerodynamic attitude law
         * 
         * @featureDescription object describing an attitude law in a
         *                     time interval
         * 
         * @coveredRequirements
         */
        AERO_ATTITE_LAW
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AeroAttitudeLawTest.class.getSimpleName(), "Aerodynamic attitude provider");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_ATTITE_LAW}
     * 
     * @testedMethod {@link AeroAttitudeLaw#getAttitude(Orbit)}
     * 
     * @description Compute attitude law given angle of attack, sideslip and velocity roll. Check that returned attitude
     *              has right
     *              angle of attack, sideslip and velocity roll.
     * 
     * @input angle of attack, sideslip and velocity roll, AeroAttitudeLaw
     * 
     * @output angle of attack, sideslip and velocity roll
     * 
     * @testPassCriteria angle of attack, sideslip and velocity roll are as expected (threshold: 1E-14)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testRotation() throws PatriusException {

        Report.printMethodHeader("testRotation", "Rotation computation", "Math", 1E-14, ComparisonType.ABSOLUTE);

        // Initialization
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "earth");
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final Vector3D velocity = orbit.getPVCoordinates(FramesFactory.getITRF()).getVelocity().normalize();

        final Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getITRF(), date);

        // Check angle of attack, sideslip and velocity roll
        final double angleOfAttack = 0.1;
        final double sideSlip = 0.2;
        final double velocityRoll = 0.3;
        final AeroAttitudeLaw attitudeLaw = new AeroAttitudeLaw(angleOfAttack, sideSlip, velocityRoll, earth);
        final Rotation rotation = attitudeLaw.getAttitude(orbit).getRotation();

        // Local frame vectors expressed in local frame
        final Vector3D oxSat = Vector3D.PLUS_I;
        final Vector3D ozSat = Vector3D.PLUS_K;
        // Local frame vectors expressed in Earth frame
        final Vector3D oxRef = t.transformVector(rotation.applyTo(oxSat));
        final Vector3D ozRef = t.transformVector(rotation.applyTo(ozSat));
        // Velocity frame vectors expressed in Earth frame
        final Vector3D oxVel = velocity;
        final Vector3D oxVelProjInOxOzPlane = new Vector3D(Vector3D.dotProduct(oxVel, oxRef), oxRef,
            Vector3D.dotProduct(oxVel, ozRef), ozRef);
        final Vector3D ozVel = Vector3D.crossProduct(oxVel, oxVelProjInOxOzPlane).normalize();
        final Vector3D oyVel = Vector3D.crossProduct(ozVel, oxVel);

        // Angle of attack
        final Vector3D oxRefProjInOxvOyvPlane = new Vector3D(Vector3D.dotProduct(oxRef, oxVel), oxVel,
            Vector3D.dotProduct(oxRef, oyVel), oyVel);
        final double actualAngleofAttack = Vector3D.angle(oxRef, oxRefProjInOxvOyvPlane);
        Report.printToReport("Angle of attack", angleOfAttack, actualAngleofAttack);
        Assert.assertEquals(angleOfAttack, actualAngleofAttack, 1E-14);

        // Side slip
        final Vector3D velProjInOxOzPlane = new Vector3D(Vector3D.dotProduct(velocity, ozRef), ozRef,
            Vector3D.dotProduct(velocity, oxRef), oxRef);
        final double actualSideSlip = Vector3D.angle(velocity, velProjInOxOzPlane);
        Report.printToReport("Side slip", sideSlip, actualSideSlip);
        Assert.assertEquals(sideSlip, actualSideSlip, 1E-14);

        // Velocity roll (on simplified case to align topocentric frame with GCRF reference frame)
        final EllipsoidBodyShape earthGCRF = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getGCRF(),
            "earth");
        final AeroAttitudeLaw attitudeLaw2 = new AeroAttitudeLaw(0., 0., velocityRoll, earthGCRF);
        final Orbit orbit2 = new KeplerianOrbit(7000000, 0.0, FastMath.PI / 2., 0.0, 0.0, 0.0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final Rotation rotation2 = attitudeLaw2.getAttitude(orbit2).getRotation();

        final Vector3D ozRef2 = rotation2.applyTo(ozSat);
        final double actualVelocityRoll = MathLib.acos(Vector3D.dotProduct(ozRef2.negate(), Vector3D.PLUS_I));
        Report.printToReport("Velocity roll", velocityRoll, actualVelocityRoll);
        Assert.assertEquals(velocityRoll, actualVelocityRoll, 1E-14);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_ATTITE_LAW}
     * 
     * @testedMethod {@link AeroAttitudeLaw#getAttitude(Orbit)}
     * 
     * @description Compute attitude law given variable angle of attack, sideslip and velocity roll. Check that returned
     *              attitude has right
     *              angle of attack, sideslip and velocity roll (which vary through time).
     * 
     * @input variable angle of attack, sideslip and velocity roll, AeroAttitudeLaw
     * 
     * @output variable angle of attack, sideslip and velocity roll
     * 
     * @testPassCriteria variable angle of attack, sideslip and velocity roll are as expected (threshold: 1E-11 due to
     *                   numerical quality issues when computing reference angles)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testRotationVariable() throws PatriusException {

        // Initialization
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "earth");
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);

        // Check angle of attack, sideslip and velocity roll over all range
        final IParamDiffFunction angleOfAttack = new LinearFunction(date, 0., 0.01);
        final IParamDiffFunction sideSlip = new LinearFunction(date, 0., 0.01);
        final IParamDiffFunction velocityRoll = new LinearFunction(date, 0., 0.02);
        final AeroAttitudeLaw attitudeLaw = new AeroAttitudeLaw(angleOfAttack, sideSlip, velocityRoll, earth);

        for (int i = -156; i <= 156; i++) {
            final SpacecraftState state = new SpacecraftState(orbit.shiftedBy(i));
            final Rotation rotation = attitudeLaw.getAttitude(state.getOrbit()).getRotation();

            final Vector3D velocity = state.getOrbit().getPVCoordinates(FramesFactory.getITRF()).getVelocity()
                .normalize();
            final Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getITRF(), state.getDate());

            // Local frame vectors expressed in local frame
            final Vector3D oxSat = Vector3D.PLUS_I;
            final Vector3D ozSat = Vector3D.PLUS_K;
            // Local frame vectors expressed in Earth frame
            final Vector3D oxRef = t.transformVector(rotation.applyTo(oxSat));
            final Vector3D ozRef = t.transformVector(rotation.applyTo(ozSat));
            final Vector3D oyRef = Vector3D.crossProduct(ozRef, oxRef);
            // Velocity frame vectors expressed in Earth frame
            final Vector3D oxVel = velocity;
            final Vector3D oxVelProjInOxOzPlane = new Vector3D(Vector3D.dotProduct(oxVel, oxRef), oxRef,
                Vector3D.dotProduct(oxVel, ozRef), ozRef);
            Vector3D ozVel = Vector3D.crossProduct(oxVel, oxVelProjInOxOzPlane).normalize();
            if (sideSlip.value(state) > 0) {
                // In that case, cross product leads to the opposite vector
                ozVel = ozVel.negate();
            }
            final Vector3D oyVel = Vector3D.crossProduct(ozVel, oxVel);

            // Angle of attack (negative if Ox and Oz in the same half-plane defined by (Oxv, Oyv))
            final Vector3D oxRefProjInOxvOyvPlane = new Vector3D(Vector3D.dotProduct(oxRef, oxVel), oxVel,
                Vector3D.dotProduct(oxRef, oyVel), oyVel);
            double actualAngleofAttack = Vector3D.angle(oxRef, oxRefProjInOxvOyvPlane);
            if (Vector3D.dotProduct(oxRef, ozVel) * Vector3D.dotProduct(ozRef, ozVel) > 0) {
                actualAngleofAttack = -actualAngleofAttack;
            }
            Assert.assertEquals(angleOfAttack.value(state), actualAngleofAttack, 1E-11);

            // Side slip (negative if Oxv and Oyv are not in the same half-plane defined by (Ox, Oz))
            final Vector3D velProjInOxOzPlane = new Vector3D(Vector3D.dotProduct(velocity, ozRef), ozRef,
                Vector3D.dotProduct(velocity, oxRef), oxRef);
            double actualSideSlip = Vector3D.angle(velocity, velProjInOxOzPlane);
            if (Vector3D.dotProduct(oxVel, oyRef) * Vector3D.dotProduct(oyVel, oyRef) < 0) {
                actualSideSlip = -actualSideSlip;
            }
            Assert.assertEquals(sideSlip.value(state), actualSideSlip, 1E-11);

            // Velocity roll (on simplified case to align topocentric frame with GCRF reference frame)
            final EllipsoidBodyShape earthGCRF = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getGCRF(),
                "earth");
            final AeroAttitudeLaw attitudeLaw2 = new AeroAttitudeLaw(new ConstantFunction(0.),
                new ConstantFunction(0.), velocityRoll, earthGCRF);
            final Orbit orbit2 = new KeplerianOrbit(7000000, 0.0, FastMath.PI / 2., 0.0, 0.0, 0.0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), date.shiftedBy(i), Constants.EGM96_EARTH_MU);
            final SpacecraftState state2 = new SpacecraftState(orbit2);
            final Rotation rotation2 = attitudeLaw2.getAttitude(state2.getOrbit()).getRotation();
            final Vector3D ozRef2 = rotation2.applyTo(ozSat);
            double actualVelocityRoll = MathLib.acos(Vector3D.dotProduct(ozRef2.negate(), Vector3D.PLUS_I));
            if (Vector3D.dotProduct(ozRef2, Vector3D.PLUS_J) > 0) {
                actualVelocityRoll = -actualVelocityRoll;
            }
            Assert.assertEquals(velocityRoll.value(state2), actualVelocityRoll, 1E-14);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_ATTITE_LAW}
     * 
     * @testedMethod {@link AeroAttitudeLaw#getAttitude(Orbit)}
     * 
     * @description Compute spin
     * 
     * @input angle of attack, sideslip and velocity roll, AeroAttitudeLaw
     * 
     * @output spin
     * 
     * @testPassCriteria spin is as expected (reference: centered finite differences, threshold: 0)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testSpin() throws PatriusException {

        Report.printMethodHeader("testSpin", "Spin computation", "Finite differences", 0., ComparisonType.ABSOLUTE);

        // Initialization
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "earth");
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final double dtSpin = 1.;
        final double dtAcc = 2.;
        final AeroAttitudeLaw attitudeLaw = new AeroAttitudeLaw(0.1, 0.2, 0.3, earth, dtSpin, dtAcc);
        final Vector3D actual = attitudeLaw.getAttitude(orbit).getSpin();

        // Check
        final Vector3D expected = AngularCoordinates.estimateRate(
            attitudeLaw.getAttitude(orbit.shiftedBy(-dtSpin / 2.)).getRotation(),
            attitudeLaw.getAttitude(orbit.shiftedBy(dtSpin / 2.)).getRotation(),
            dtSpin);

        Report.printToReport("Spin at date", expected, actual);
        Assert.assertEquals(0., expected.subtract(actual).getNorm(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_ATTITE_LAW}
     * 
     * @testedMethod {@link AeroAttitudeLaw#getAttitude(Orbit)}
     * 
     * @description Compute spin
     * 
     * @input angle of attack, sideslip and velocity roll, AeroAttitudeLaw
     * 
     * @output spin
     * 
     * @testPassCriteria acceleration is as expected (reference: centered finite differences, threshold: 1E-13)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testAcceleration() throws PatriusException {

        Report.printMethodHeader("testSpin", "Rotation acceleration computation", "Finite differences", 1E-13,
            ComparisonType.ABSOLUTE);

        // Initialization
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "earth");
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(7000000, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final double dtSpin = 1.;
        final double dtAcc = 2.;
        final AeroAttitudeLaw attitudeLaw = new AeroAttitudeLaw(0.1, 0.2, 0.3, earth, dtSpin, dtAcc);
        attitudeLaw.setSpinDerivativesComputation(true);
        final Vector3D actual = attitudeLaw.getAttitude(orbit).getRotationAcceleration();

        // Check
        final Vector3D spin1 = attitudeLaw.getAttitude(orbit.shiftedBy(-dtAcc / 2.)).getSpin();
        final Vector3D spin2 = attitudeLaw.getAttitude(orbit.shiftedBy(dtAcc / 2.)).getSpin();
        final Vector3D expected = new Vector3D(-0.5, spin1, 0.5, spin2);

        Assert.assertEquals(0., expected.subtract(actual).getNorm(), 1E-13);
        Report.printToReport("Rotation acceleration at date", expected, actual);
    }

    // Data initialization attributes
    private double psi2000Ref;
    private double teta2000Ref;
    private double phi2000Ref;
    private double alt;
    private double lat;
    private double lon;
    private double vit;
    private double pen;
    private double azi;
    private double alfa;
    private double beta;
    private double gite;
    private double flat;
    private String label;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_ATTITE_LAW}
     * 
     * @testedMethod {@link AeroAttitudeLaw#getAttitude(Orbit)}
     * 
     * @description Compute attitude law given angle of attack, sideslip and velocity roll. Check that returned angles
     *              in J2000
     *              frame are correct.
     * 
     * @input angle of attack, sideslip and velocity roll, AeroAttitudeLaw
     * 
     * @output angles in J2000 frame
     * 
     * @testPassCriteria angles in J2000 frame are as expected (reference: MSLIB, threshold: 1E-3 rad, report shows
     *                   however than inaccuracy comes
     *                   from MSLIB, PATRIUS results being better than 1E-7 rad)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testMSLIB() throws PatriusException {

        Report.printMethodHeader("testRotation", "Rotation computation", "MSLIB", 1E-3, ComparisonType.ABSOLUTE);

        // Initialization
        // Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(getSimplifiedConfiguration());

        // Loop on all the case
        for (int numCase = 0; numCase < 7; numCase++) {
            // Init test case data
            this.inidata(numCase);

            // Initial orbit
            final AbsoluteDate date = new AbsoluteDate("2010-01-01T00:00:00.000", TimeScalesFactory.getUTC());
            final ReentryParameters ren =
                new ReentryParameters(this.alt, this.lat, this.lon, this.vit, this.pen, this.azi,
                    Constants.WGS84_EARTH_EQUATORIAL_RADIUS, this.flat, Constants.WGS84_EARTH_MU);
            final Orbit iniOrbit = new CartesianOrbit(ren, FramesFactory.getEME2000(), date);

            final EllipsoidBodyShape earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS, this.flat, FramesFactory.getITRF(), "EARTH");
            final Attitude attitude = new AeroAttitudeLaw(this.alfa, this.beta, this.gite, earth).getAttitude(iniOrbit);

            // Check results (angle in J2000)
            final double psi2000 = attitude.getRotation().getAngles(RotationOrder.ZYX)[0];
            final double teta2000 = attitude.getRotation().getAngles(RotationOrder.ZYX)[1];
            final double phi2000 = attitude.getRotation().getAngles(RotationOrder.ZYX)[2];
            Report.printToReport("Psi (" + this.label + ")", MathLib.toDegrees(this.psi2000Ref),
                MathLib.toDegrees(psi2000));
            Report.printToReport("Teta (" + this.label + ")", MathLib.toDegrees(this.teta2000Ref),
                MathLib.toDegrees(teta2000));
            Report.printToReport("Phi (" + this.label + ")", MathLib.toDegrees(this.phi2000Ref),
                MathLib.toDegrees(phi2000));
            Assert.assertEquals(this.psi2000Ref, psi2000, 6E-7);
            Assert.assertEquals(this.teta2000Ref, teta2000, 3E-6);
            Assert.assertEquals(this.phi2000Ref, phi2000, 4E-3);
        }
    }

    /**
     * Method to configure frames configuration.
     * 
     * @return frame configuration object
     */
    private static FramesConfiguration getSimplifiedConfiguration() {
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;
        builder.setDiurnalRotation(new DiurnalRotation(TidalCorrectionModelFactory.NO_TIDE,
            LibrationCorrectionModelFactory.NO_LIBRATION));
        builder.setPolarMotion(new PolarMotion(false, tides, lib, SPrimeModelFactory.NO_SP));
        builder.setPrecessionNutation(new PrecessionNutation(false, PrecessionNutationModelFactory.NO_PN));
        builder.setEOPHistory(new NoEOP2000History());
        return builder.getConfiguration();
    }

    /**
     * Private method to initialize data.
     * 
     * @throws PatriusException
     */
    private void inidata(final int numCase) throws PatriusException {

        // By default values ...
        this.alt = 120.e+03;
        this.lat = 0.;
        this.lon = 0.;
        this.vit = 7500.;
        this.pen = 0.;
        this.azi = MathLib.toRadians(90.);
        this.alfa = 0.;
        this.beta = 0;
        this.gite = 0.;
        this.flat = Constants.WGS84_EARTH_FLATTENING;

        switch (numCase) {
            case 0:
                this.label = "basic case";
                this.psi2000Ref = MathLib.toRadians(89.99999999519);
                this.teta2000Ref = MathLib.toRadians(-4.8348493663253e-05);
                this.phi2000Ref = MathLib.toRadians(270.) - MathUtils.TWO_PI;
                break;
            case 1:
                this.label = "90 deg bank angle";
                this.gite = MathLib.toRadians(90.);
                this.psi2000Ref = MathLib.toRadians(89.99999999519);
                this.teta2000Ref = MathLib.toRadians(-4.8348493685533e-05);
                this.phi2000Ref = MathLib.toRadians(0.);
                break;
            case 2:
                this.label = "180 deg bank angle";
                this.gite = MathLib.toRadians(180.);
                this.psi2000Ref = MathLib.toRadians(89.99999999519);
                this.teta2000Ref = MathLib.toRadians(-4.8348493668024e-05);
                this.phi2000Ref = MathLib.toRadians(90.);
                break;
            case 3:
                this.label = "non null attitude angles";
                this.alfa = MathLib.toRadians(45.);
                this.beta = MathLib.toRadians(10.);
                this.gite = MathLib.toRadians(90.);
                this.psi2000Ref = MathLib.toRadians(80.000008390808);
                this.teta2000Ref = MathLib.toRadians(44.999952386028);
                this.phi2000Ref = MathLib.toRadians(1.1873200846572e-05);
                break;
            case 4:
                this.label = "non null FPA";
                this.pen = MathLib.toRadians(-10.);
                this.psi2000Ref = MathLib.toRadians(100.67027516176);
                this.teta2000Ref = MathLib.toRadians(-0.00011870467194537);
                this.phi2000Ref = MathLib.toRadians(269.9999776343) - MathUtils.TWO_PI;
                break;
            case 5:
                this.label = "non null latitude";
                this.lat = MathLib.toRadians(45.);
                this.psi2000Ref = MathLib.toRadians(90.000030880941);
                this.teta2000Ref = MathLib.toRadians(-3.6037356984774e-05);
                this.phi2000Ref = MathLib.toRadians(225.1888639432) - MathUtils.TWO_PI;
                break;
            case 6:
                this.label = "non null spherical latitude";
                this.lat = MathLib.toRadians(45.);
                this.flat = 0.;
                this.psi2000Ref = MathLib.toRadians(90.00003104808);
                this.teta2000Ref = MathLib.toRadians(-3.5991548931475e-05);
                this.phi2000Ref = MathLib.toRadians(224.99999999999) - MathUtils.TWO_PI;
                break;
            default:
                break;
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
        final double angleofattack = MathLib.toRadians(10);
        final double sideslip = MathLib.toRadians(12);
        final double rollVel = MathLib.toRadians(25);
        final AeroAttitudeLaw aeroAttitudeLaw = new AeroAttitudeLaw(angleofattack, sideslip, rollVel,
            new OneAxisEllipsoid(Constants.CNES_STELA_AE,
                Constants.GRIM5C1_EARTH_FLATTENING,
                FramesFactory.getGCRF(), "earth"));

        Assert.assertEquals(angleofattack, aeroAttitudeLaw.getAngleOfAttack(null), 0);
        Assert.assertEquals(sideslip, aeroAttitudeLaw.getSideSlipAngle(null), 0);
        Assert.assertEquals(rollVel, aeroAttitudeLaw.getRollVelocity(null), 0);
        Assert.assertNotNull(aeroAttitudeLaw.toString());
    }

    /**
     * Setup.
     */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-dataCNES-2003");
    }
}
