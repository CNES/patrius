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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:271:05/09/2014:Anomalies definitions LVLH et VVLH
 * VERSION::DM:344:15/04/2015:Construction of an attitude law from a Local Orbital Frame
 * VERSION::DM:342:05/03/2015:No exceptions thrown for singular Euler or Cardan angles: testSingularities() replaced
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class LofOffsetTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF 2005C
    private Frame frameITRF2005;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    // Satellite position
    CircularOrbit orbit;
    PVCoordinates pvSatEME2000;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(LofOffsetTest.class.getSimpleName(), "Lof offset attitude provider");
    }

    /**
     * Test if the lof offset is the one expected
     */
    @Test
    public void testZero() throws PatriusException {

        // Satellite position

        // Lof aligned attitude provider
        final LofOffset lofAlignedLaw = new LofOffset(this.orbit.getFrame(), LOFType.LVLH);
        final Rotation lofOffsetRot =
            lofAlignedLaw.getAttitude(this.orbit, this.date, this.orbit.getFrame()).getRotation();

        // Check that
        final Vector3D momentumEME2000 = this.pvSatEME2000.getMomentum();
        final Vector3D momentumLof = lofOffsetRot.applyInverseTo(momentumEME2000);
        final double cosinus = MathLib.cos(Vector3D.dotProduct(momentumLof, Vector3D.PLUS_K));
        Assert.assertEquals(1., cosinus, Utils.epsilonAngle);

    }

    /**
     * Test if the lof offset is the one expected
     */
    @Test
    public void testOffset() throws PatriusException {

        Report.printMethodHeader("testOffset", "Rotation computation", "Math", Utils.epsilonAngle,
            ComparisonType.ABSOLUTE);

        // Satellite position
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(0.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Create target pointing attitude provider
        // ************************************
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF2005);
        final GeodeticPoint geoTargetITRF2005 = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26),
            600.);

        // Attitude law definition from geodetic point target
        final TargetPointing targetLaw = new TargetPointing(geoTargetITRF2005, earthShape);
        final Rotation targetRot = targetLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Create lof aligned attitude provider
        // *******************************
        final LofOffset lofAlignedLaw = new LofOffset(circ.getFrame(), LOFType.LVLH);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Get rotation from LOF to target pointing attitude
        final Rotation rollPitchYaw = lofAlignedRot.applyInverseTo(targetRot);
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create lof offset attitude provider with computed roll, pitch, yaw
        // **************************************************************
        final LofOffset lofOffsetLaw =
            new LofOffset(circ.getFrame(), LOFType.LVLH, RotationOrder.ZYX, yaw, pitch, roll);
        final Rotation lofOffsetRot = lofOffsetLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Compose rotations : target pointing attitudes
        final double angleCompo = targetRot.applyInverseTo(lofOffsetRot).getAngle();
        Assert.assertEquals(0., angleCompo, Utils.epsilonAngle);

        Report.printToReport("Target pointing angle at date at date", 0., angleCompo);
    }

    /**
     * Test is the target pointed is the one expected
     * Test the use of constructor without an inertial frame as parameter.
     * {@link fr.cnes.sirius.patrius.attitudes.LofOffset#LofOffset(Frame, LOFType)
     * LofOffset(Frame, LOFType)}
     */
    @Test
    public void testOffsetGCRF() throws PatriusException {

        // Satellite position
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(0.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Create target pointing attitude provider
        // ************************************
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF2005);
        final GeodeticPoint geoTargetITRF2005 = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26),
            600.);

        // Attitude law definition from geodetic point target
        final TargetPointing targetLaw = new TargetPointing(geoTargetITRF2005, earthShape);
        final Rotation targetRot = targetLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Create lof aligned attitude provider
        // *******************************
        final LofOffset lofAlignedLaw = new LofOffset(LOFType.LVLH);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Get rotation from LOF to target pointing attitude
        final Rotation rollPitchYaw = lofAlignedRot.applyInverseTo(targetRot);
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create lof offset attitude provider with computed roll, pitch, yaw
        // **************************************************************
        final LofOffset lofOffsetLaw =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH, RotationOrder.ZYX, yaw, pitch,
                roll);
        final Rotation lofOffsetRot = lofOffsetLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Compose rotations : target pointing attitudes
        final double angleCompo = targetRot.applyInverseTo(lofOffsetRot).getAngle();
        Assert.assertEquals(0., angleCompo, Utils.epsilonAngle);

    }

    /**
     * Test is the target pointed is the one expected
     */
    @Test
    public void testTarget()
                            throws PatriusException {

        // Create target point and target pointing law towards that point
        final GeodeticPoint targetDef = new GeodeticPoint(MathLib.toRadians(5.), MathLib.toRadians(-40.), 0.);
        final TargetPointing targetLaw = new TargetPointing(targetDef, this.earthSpheric);

        // Get roll, pitch, yaw angles corresponding to this pointing law
        final LofOffset lofAlignedLaw = new LofOffset(this.orbit.getFrame(), LOFType.LVLH);
        final Rotation lofAlignedRot =
            lofAlignedLaw.getAttitude(this.orbit, this.date, this.orbit.getFrame()).getRotation();
        final Rotation targetAttitudeRot =
            targetLaw.getAttitude(this.orbit, this.date, this.orbit.getFrame()).getRotation();
        final Rotation rollPitchYaw = lofAlignedRot.applyInverseTo(targetAttitudeRot);
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create a lof offset law from those values
        final LofOffset lofOffsetLaw =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH, RotationOrder.ZYX, yaw, pitch,
                roll);
        final LofOffsetPointing lofOffsetPtLaw =
            new LofOffsetPointing(this.earthSpheric, lofOffsetLaw, Vector3D.PLUS_K);

        // Check target pointed by this law : shall be the same as defined
        final Vector3D pTargetRes =
            lofOffsetPtLaw.getTargetPoint(this.orbit, this.date, this.earthSpheric.getBodyFrame());
        final GeodeticPoint targetRes =
            this.earthSpheric.transform(pTargetRes, this.earthSpheric.getBodyFrame(), this.date);

        Assert.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(targetDef.getLatitude(), targetRes.getLatitude(), Utils.epsilonAngle);

    }

    /**
     * Test is the target pointed is the one expected
     * Test the use of constructor without an inertial frame as parameter.
     * {@link fr.cnes.sirius.patrius.attitudes.LofOffset#LofOffset(Frame, LOFType, RotationOrder, double, double, double)
     * LofOffset(Frame, LOFType, RotationOrder, double, double, double)}
     */
    @Test
    public void testTargetGCRF()
                                throws PatriusException {

        // Create target point and target pointing law towards that point
        final GeodeticPoint targetDef = new GeodeticPoint(MathLib.toRadians(5.), MathLib.toRadians(-40.), 0.);
        final TargetPointing targetLaw = new TargetPointing(targetDef, this.earthSpheric);

        // Get roll, pitch, yaw angles corresponding to this pointing law
        final LofOffset lofAlignedLaw = new LofOffset(this.orbit.getFrame(), LOFType.LVLH);
        final Rotation lofAlignedRot =
            lofAlignedLaw.getAttitude(this.orbit, this.date, this.orbit.getFrame()).getRotation();
        final Rotation targetAttitudeRot =
            targetLaw.getAttitude(this.orbit, this.date, this.orbit.getFrame()).getRotation();
        final Rotation rollPitchYaw = lofAlignedRot.applyInverseTo(targetAttitudeRot);
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create a lof offset law from those values
        final LofOffset lofOffsetLaw = new LofOffset(LOFType.LVLH, RotationOrder.ZYX, yaw, pitch, roll);
        final LofOffsetPointing lofOffsetPtLaw =
            new LofOffsetPointing(this.earthSpheric, lofOffsetLaw, Vector3D.PLUS_K);

        // Check target pointed by this law : shall be the same as defined
        final Vector3D pTargetRes =
            lofOffsetPtLaw.getTargetPoint(this.orbit, this.date, this.earthSpheric.getBodyFrame());
        final GeodeticPoint targetRes =
            this.earthSpheric.transform(pTargetRes, this.earthSpheric.getBodyFrame(), this.date);

        Assert.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(targetDef.getLatitude(), targetRes.getLatitude(), Utils.epsilonAngle);

    }

    @Test
    public void testSpin() throws PatriusException {

        Report
            .printMethodHeader("testSpin", "Spin computation", "Finite differences", 1.0e-10, ComparisonType.ABSOLUTE);

        final AbstractAttitudeLaw law =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH, RotationOrder.XYX, 0.1, 0.2, 0.3);

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 0.01;
        final SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        final SpacecraftState sPlus = propagator.propagate(date.shiftedBy(h));

        final Rotation rMinus = law.getAttitude(orbit, date.shiftedBy(-h), orbit.getFrame()).getRotation();
        final Rotation r0 = law.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        final Rotation rPlus = law.getAttitude(orbit, date.shiftedBy(h), orbit.getFrame()).getRotation();

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(), r0);
        final double evolutionAngleMinus = Rotation.distance(rMinus, r0);
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(r0, sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(r0, rPlus);
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        final Vector3D spin0 = law.getAttitude(orbit, date, orbit.getFrame()).getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
            sPlus.getAttitude().getRotation(), 2 * h);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-10);

        Report.printToReport("Spin at date", reference, spin0);
    }

    @Test
    public void testAnglesSign() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-8, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(0.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final double alpha = 0.1;
        final double cos = MathLib.cos(alpha);
        final double sin = MathLib.sin(alpha);

        // Roll
        Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH, RotationOrder.XYZ, alpha, 0.0, 0.0)
            .getAttitude(orbit, date, orbit.getFrame());
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_I, 1.0, 0.0, 0.0, 1.0e-8);
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_J, 0.0, cos, sin, 1.0e-8);
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_K, 0.0, -sin, cos, 1.0e-8);

        // Pitch
        attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH, RotationOrder.XYZ, 0.0, alpha, 0.0).getAttitude(orbit,
            date, orbit.getFrame());
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_I, cos, 0.0, -sin, 1.0e-8);
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_J, 0.0, 1.0, 0.0, 1.0e-8);
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_K, sin, 0.0, cos, 1.0e-8);

        // Yaw
        attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH, RotationOrder.XYZ, 0.0, 0.0, alpha).getAttitude(orbit,
            date, orbit.getFrame());
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_I, cos, sin, 0.0, 1.0e-8);
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_J, -sin, cos, 0.0, 1.0e-8);
        this.checkSatVector(orbit, attitude, Vector3D.PLUS_K, 0.0, 0.0, 1.0, 1.0e-8);

    }

    @Test
    public void testRetrieveAngles() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final RotationOrder order = RotationOrder.ZXY;
        final double alpha1 = 0.123;
        final double alpha2 = 0.456;
        final double alpha3 = 0.789;
        final LofOffset law = new LofOffset(orbit.getFrame(), LOFType.LVLH, order, alpha1, alpha2, alpha3);
        final Rotation offsetAttRot = law.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        final Rotation alignedAttRot = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, date,
            orbit.getFrame()).getRotation();
        final Rotation offsetProper = alignedAttRot.applyInverseTo(offsetAttRot);
        final double[] angles = offsetProper.getAngles(order);
        Assert.assertEquals(alpha1, angles[0], 1.0e-11);
        Assert.assertEquals(alpha2, angles[1], 1.0e-11);
        Assert.assertEquals(alpha3, angles[2], 1.0e-11);
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            1E-13, ComparisonType.ABSOLUTE);

        // Satellite position
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(0.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Create target pointing attitude provider
        // ************************************
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF2005);
        final GeodeticPoint geoTargetITRF2005 = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26),
            600.);

        // Attitude law definition from geodetic point target
        final TargetPointing targetLaw = new TargetPointing(geoTargetITRF2005, earthShape);
        final Rotation targetRot = targetLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Create lof aligned attitude provider
        // *******************************
        final LofOffset lofAlignedLaw = new LofOffset(LOFType.LVLH);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Get rotation from LOF to target pointing attitude
        final Rotation rollPitchYaw = lofAlignedRot.applyInverseTo(targetRot);
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create lof offset attitude provider with computed roll, pitch, yaw
        // **************************************************************
        final LofOffset lofOffsetLaw =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH, RotationOrder.ZYX, yaw, pitch,
                roll);
        lofOffsetLaw.setSpinDerivativesComputation(true);

        // Check that derivation of spin with finite difference method is closed to acceleration
        final Frame frameToCompute = FramesFactory.getITRF();

        for (int i = 1; i < 10000; i += 100) {
            final Vector3D acc = lofOffsetLaw.getAttitude(this.orbit, this.date.shiftedBy(i), frameToCompute)
                .getRotationAcceleration();
            final Vector3D accDerivateSpin =
                this.getSpinFunction(lofOffsetLaw, this.orbit, frameToCompute, this.date.shiftedBy(i))
                    .nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 1e-13);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        lofOffsetLaw.setSpinDerivativesComputation(false);
        Assert.assertNull(lofOffsetLaw.getAttitude(this.orbit).getRotationAcceleration());
    }

    @Test
    public void testRotationNoAcceleration() throws PatriusException {

        // Satellite position
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(0.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Create target pointing attitude provider
        // ************************************
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF2005);
        final GeodeticPoint geoTargetITRF2005 = new GeodeticPoint(MathLib.toRadians(43.36), MathLib.toRadians(1.26),
            600.);

        // Attitude law definition from geodetic point target
        final TargetPointing targetLaw = new TargetPointing(geoTargetITRF2005, earthShape);
        final Rotation targetRot = targetLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Create lof aligned attitude provider
        // *******************************
        final LofOffset lofAlignedLaw = new LofOffset(LOFType.LVLH);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Get rotation from LOF to target pointing attitude
        final Rotation rollPitchYaw = lofAlignedRot.applyInverseTo(targetRot);
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create lof offset attitude provider with computed roll, pitch, yaw
        // with no acceleration (we don't call setSpinDerivativesComputation())
        // **************************************************************
        final LofOffset lofOffsetLaw =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH, RotationOrder.ZYX, yaw, pitch,
                roll);

        // Check that derivation of spin with finite difference method is closed to acceleration
        final Frame frameToCompute = FramesFactory.getITRF();

        for (int i = 1; i < 10000; i += 100) {
            final Vector3D acc = lofOffsetLaw.getAttitude(this.orbit, this.date.shiftedBy(i), frameToCompute)
                .getRotationAcceleration();
            Assert.assertNull(acc);
        }
    }

    /**
     * Local function to provide spin function.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @param law
     *        law
     * @return spin function of date relative
     */
    public Vector3DFunction getSpinFunction(final LofOffset law, final PVCoordinatesProvider pvProv, final Frame frame,
                                            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return law.getAttitude(pvProv, date, frame).getSpin();
            }
        };
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
        final Frame pInertialFrame = FramesFactory.getGCRF();
        final LOFType typeIn = LOFType.LVLH;
        final RotationOrder order = RotationOrder.XYZ;
        final double alpha1 = 0;
        final double alpha2 = 1;
        final double alpha3 = 2;
        final Rotation rotation = new Rotation(order, alpha1, alpha2, alpha3);
        final LofOffset lofOffset = new LofOffset(pInertialFrame, typeIn, order, alpha1, alpha2, alpha3);

        Assert.assertEquals(rotation.getAngle(), lofOffset.getRotation().getAngle(), 0);
        Assert.assertEquals(pInertialFrame.getName(), lofOffset.getPseudoInertialFrame().getName());
        Assert.assertEquals(typeIn.name(), lofOffset.getLofType().name());
        Assert.assertNotNull(lofOffset.toString());
    }

    private void checkSatVector(final Orbit o, final Attitude a, final Vector3D satVector,
                                final double expectedX, final double expectedY, final double expectedZ,
                                final double threshold) {
        final Vector3D zLof = o.getPVCoordinates().getPosition().normalize().negate();
        final Vector3D yLof = o.getPVCoordinates().getMomentum().normalize().negate();
        final Vector3D xLof = Vector3D.crossProduct(yLof, zLof);
        Assert.assertTrue(Vector3D.dotProduct(xLof, o.getPVCoordinates().getVelocity()) > 0);
        final Vector3D v = a.getRotation().applyTo(satVector);
        Assert.assertEquals(expectedX, Vector3D.dotProduct(v, xLof), 1.0e-8);
        Assert.assertEquals(expectedY, Vector3D.dotProduct(v, yLof), 1.0e-8);
        Assert.assertEquals(expectedZ, Vector3D.dotProduct(v, zLof), 1.0e-8);
    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Computation date
            this.date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            // Body mu
            this.mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

            // Elliptic earth shape
            this.earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

            // Satellite position
            this.orbit =
                new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, MathLib.toRadians(50.), MathLib.toRadians(150.),
                    MathLib.toRadians(5.300), PositionAngle.MEAN,
                    FramesFactory.getEME2000(), this.date, this.mu);
            this.pvSatEME2000 = this.orbit.getPVCoordinates();

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.frameITRF2005 = null;
        this.earthSpheric = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
