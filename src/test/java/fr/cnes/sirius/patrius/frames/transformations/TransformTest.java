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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3093:10/05/2022:[PATRIUS] Mise en Oeuvre PM2973 , gestion coordonnees et referentiel 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Added a transformWrench method
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::FA:356:20/03/2015: Performance degradation in conversions
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.FactoryManagedFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937a;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PVCoordinatesTest;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.wrenches.Wrench;

public class TransformTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TransformTest.class.getSimpleName(), "Transform");
    }

    @Test
    public void testIdentityTranslation() {
        checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(0, 0, 0)),
            new Random(0xfd118eac6b5ec136l));
    }

    @Test
    public void testIdentityRotation() {
        checkNoTransform(new Transform(AbsoluteDate.J2000_EPOCH, new Rotation(false, 1, 0, 0, 0)),
                new Random(
            0xfd118eac6b5ec136l));
    }

    @Test
    public void testSimpleComposition() {
        Report.printMethodHeader("testSimpleComposition", "Composition", "Math", 1.0e-15, ComparisonType.ABSOLUTE);
        final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, new Transform(AbsoluteDate.J2000_EPOCH,
            new Rotation(Vector3D.PLUS_K, 0.5 * FastMath.PI)), new Transform(AbsoluteDate.J2000_EPOCH,
            Vector3D.PLUS_I));
        final Vector3D u = transform.transformPosition(new Vector3D(1.0, 1.0, 1.0));
        final Vector3D v = new Vector3D(0.0, -1.0, 1.0);
        Assert.assertEquals(0, u.subtract(v).getNorm(), 1.0e-15);
        Report.printToReport("Transformed position", v, u);
    }

    @Test
    public void testAcceleration() {
        Report.printMethodHeader("testAcceleration", "Position/Velocity/Acceleration", "Math", 9.0e-11,
            ComparisonType.RELATIVE);

        final PVCoordinates initPV =
            new PVCoordinates(new Vector3D(9, 8, 7), new Vector3D(6, 5, 4), new Vector3D(3, 2, 1));
        for (double dt = 0; dt < 1; dt += 0.01) {
            final PVCoordinates basePV = initPV.shiftedBy(dt);
            final PVCoordinates transformedPV = evolvingTransform(AbsoluteDate.J2000_EPOCH, dt)
                .transformPVCoordinates(basePV);

            // rebuild transformed acceleration, relying only on transformed position and velocity
            final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
            final double h = 1.0e-2;
            for (int i = -3; i < 4; ++i) {
                final Transform t = evolvingTransform(AbsoluteDate.J2000_EPOCH, dt + i * h);
                final PVCoordinates pv = t.transformPVCoordinates(initPV.shiftedBy(dt + i * h));
                sample
                    .add(new TimeStampedPVCoordinates(t.getDate(), pv.getPosition(), pv.getVelocity(), Vector3D.ZERO));
            }
            final PVCoordinates rebuiltPV =
                TimeStampedPVCoordinates.interpolate(AbsoluteDate.J2000_EPOCH.shiftedBy(dt),
                    CartesianDerivativesFilter.USE_PV,
                    sample);

            checkVector(rebuiltPV.getPosition(), transformedPV.getPosition(), 4.0e-16);
            checkVector(rebuiltPV.getVelocity(), transformedPV.getVelocity(), 2.0e-15);
            checkVector(rebuiltPV.getAcceleration(), transformedPV.getAcceleration(), 9.0e-11);

            if (dt == 0.01) {
                Report.printToReport("Transformed position", rebuiltPV.getPosition(), transformedPV.getPosition());
                Report.printToReport("Transformed velocity", rebuiltPV.getVelocity(), transformedPV.getVelocity());
                Report.printToReport("Transformed acceleration", rebuiltPV.getAcceleration(),
                    transformedPV.getAcceleration());
            }
        }

    }

    @Test
    public void testAccelerationComposition() {
        final RandomGenerator random = new Well19937a(0x41fdd07d6c9e9f65l);

        final Transform t1 = buildRandomTransform(random);
        final Transform t2 = buildRandomTransform(random);
        final Transform t12 = new Transform(AbsoluteDate.J2000_EPOCH, t1, t2, true);

        final Vector3D q = randomVector(1.0e3, random);
        final Vector3D qDot = randomVector(1.0, random);
        final Vector3D qDotDot = randomVector(1.0e-3, random);

        final PVCoordinates pva0 = new PVCoordinates(q, qDot, qDotDot);
        final PVCoordinates pva1 = t1.transformPVCoordinates(pva0);
        final PVCoordinates pva2 = t2.transformPVCoordinates(pva1);
        final PVCoordinates pvac = t12.transformPVCoordinates(pva0);

        checkVector(pva2.getPosition(), pvac.getPosition(), 1.0e-15);
        checkVector(pva2.getVelocity(), pvac.getVelocity(), 1.0e-15);
        checkVector(pva2.getAcceleration(), pvac.getAcceleration(), 1.0e-15);

        // despite neither raw transforms have angular acceleration,
        // the combination does have an angular acceleration,
        // it is due to the cross product Ω₁ ⨯ Ω₂
        Assert.assertEquals(0.0, t1.getAngular().getRotationAcceleration().getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, t2.getAngular().getRotationAcceleration().getNorm(), 1.0e-15);
        Assert.assertTrue(t12.getAngular().getRotationAcceleration().getNorm() > 0.01);

    }

    @Test
    public void testProjectVelocityAndAcceleration() {
        final RandomGenerator random = new Well19937a(0x41fdd07d6c9e9f65l);
        final Transform t1 = buildRandomTransform(random);
        final Transform t2 = buildRandomTransform(random);
        final AbsoluteDate date = new AbsoluteDate();
        final boolean computeSpinDerivatives = true;
        // build the generic transform (i.e. not by simply projecting velocity and acceleration)
        final Transform transformGeneric = new Transform(date, t1, t2, computeSpinDerivatives, false);
        // build the transform by simply projecting velocity and acceleration
        final Transform transformWithProjection = new Transform(date, t1, t2, computeSpinDerivatives, true);
        final PVCoordinates cartesianTransformGeneric = transformGeneric.getCartesian();
        final PVCoordinates cartesianTransformWithProjection = transformWithProjection.getCartesian();
        // check that the velocities are different
        Assert.assertFalse(cartesianTransformWithProjection.getVelocity().equals(
            cartesianTransformGeneric.getVelocity()));
        // check that the accelerations are different
        Assert.assertFalse(cartesianTransformWithProjection.getAcceleration().equals(
            cartesianTransformGeneric.getAcceleration()));
    }

    @Test
    public void testProjectVelocityAndAccelerationCelestLab() throws PatriusException {

        // case 1 (without projection of velocity and acceleration): both referentials coincide with their corresponding
        // frames

        // define the origin frame
        final Frame originFrame = FramesFactory.getGCRF();
        // define the destination frame
        final FactoryManagedFrame destinationFrame = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 06, 11, 14, 51.704);
        // build the transform needed to pass from the GCRF frame to the EME2000 frame
        final Transform transformGcrfToEme2000 = originFrame.getTransformTo(destinationFrame, date,
            FramesFactory.getConfiguration(), true);
        // initialize pv in GCRF
        final Vector3D posGcrf = new Vector3D(2563886.8779688897, -985608.501054303, 6236976.976623426);
        final Vector3D velGcrf = new Vector3D(-7090.259917801003, -448.8883791950612, 2840.586787876285);
        final PVCoordinates pvCoordGcrf = new PVCoordinates(posGcrf, velGcrf);
        // compute pv in EME2000
        final PVCoordinates pvCoordEme2000Patrius = transformGcrfToEme2000.transformPVCoordinates(pvCoordGcrf);
        final Vector3D posEme2000Patrius = pvCoordEme2000Patrius.getPosition();
        final Vector3D velEme2000Patrius = pvCoordEme2000Patrius.getVelocity();
        // use CelestLab pv values in EME2000 as reference
        final Vector3D posEme2000CelestLab = new Vector3D(2563887.450198092032, -985608.1133819842944,
            6236976.802654840983);
        final Vector3D velEme2000CelestLab = new Vector3D(-7090.259657183235504, -448.8887871615972358,
            2840.587373922796360);
        // fix the tolerance
        final double tol = 3.9e-6;
        // check that the positions in EME2000 are the same
        Assert.assertEquals(posEme2000CelestLab.getX(), posEme2000Patrius.getX(), tol);
        Assert.assertEquals(posEme2000CelestLab.getY(), posEme2000Patrius.getY(), tol);
        Assert.assertEquals(posEme2000CelestLab.getZ(), posEme2000Patrius.getZ(), tol);
        // check that the velocities in EME2000 are the same
        Assert.assertEquals(velEme2000CelestLab.getX(), velEme2000Patrius.getX(), tol);
        Assert.assertEquals(velEme2000CelestLab.getY(), velEme2000Patrius.getY(), tol);
        Assert.assertEquals(velEme2000CelestLab.getZ(), velEme2000Patrius.getZ(), tol);

        // case 2 (without projection of velocity and acceleration): only the referential of the destination frame
        // coincides with its corresponding (destination) frame and the referential of the origin frame is not on the
        // path from the origin frame to the destination frame

        // change the referential of the origin frame
        originFrame.setReferential(FramesFactory.getMOD(true));
        // build the transform needed to pass from the GCRF frame to the EME2000 frame
        final Transform transformGcrfToEme2000ModifOrigRef = originFrame.getTransformTo(destinationFrame, date,
            FramesFactory.getConfiguration(), true);
        // compute pv in EME2000
        final PVCoordinates pvCoordEme2000PatriusModifOrigRef = transformGcrfToEme2000ModifOrigRef
            .transformPVCoordinates(pvCoordGcrf);
        final Vector3D posEme2000PatriusModifOrigRef = pvCoordEme2000PatriusModifOrigRef.getPosition();
        final Vector3D velEme2000PatriusModifOrigRef = pvCoordEme2000PatriusModifOrigRef.getVelocity();
        // check that the positions in EME2000 are the same
        Assert.assertEquals(posEme2000CelestLab.getX(), posEme2000PatriusModifOrigRef.getX(), tol);
        Assert.assertEquals(posEme2000CelestLab.getY(), posEme2000PatriusModifOrigRef.getY(), tol);
        Assert.assertEquals(posEme2000CelestLab.getZ(), posEme2000PatriusModifOrigRef.getZ(), tol);
        // check that the velocities in EME2000 are the same
        Assert.assertEquals(velEme2000CelestLab.getX(), velEme2000PatriusModifOrigRef.getX(), tol);
        Assert.assertEquals(velEme2000CelestLab.getY(), velEme2000PatriusModifOrigRef.getY(), tol);
        Assert.assertEquals(velEme2000CelestLab.getZ(), velEme2000PatriusModifOrigRef.getZ(), tol);

        // case 3 (without projection of velocity and acceleration): only the referential of the origin frame coincides
        // with its corresponding (origin) frame and the referential of the destination frame is not on the path from
        // the origin frame to the destination frame

        // reset back the referential of the origin frame to the original one
        originFrame.setReferential(FramesFactory.getGCRF());
        // change the referential of the destination frame
        destinationFrame.setReferential(FramesFactory.getMOD(true));
        // build the transform needed to pass from the GCRF frame to the EME2000 frame
        final Transform transformGcrfToEme2000ModifDestRef = originFrame.getTransformTo(destinationFrame, date,
            FramesFactory.getConfiguration(), true);
        // compute pv in EME2000
        final PVCoordinates pvCoordEme2000PatriusModifDestRef = transformGcrfToEme2000ModifDestRef
            .transformPVCoordinates(pvCoordGcrf);
        final Vector3D posEme2000PatriusModifDestRef = pvCoordEme2000PatriusModifDestRef.getPosition();
        final Vector3D velEme2000PatriusModifDestRef = pvCoordEme2000PatriusModifDestRef.getVelocity();
        // check that the positions in EME2000 are the same
        Assert.assertEquals(posEme2000CelestLab.getX(), posEme2000PatriusModifDestRef.getX(), tol);
        Assert.assertEquals(posEme2000CelestLab.getY(), posEme2000PatriusModifDestRef.getY(), tol);
        Assert.assertEquals(posEme2000CelestLab.getZ(), posEme2000PatriusModifDestRef.getZ(), tol);
        // check that the velocities in EME2000 are the same
        Assert.assertEquals(velEme2000CelestLab.getX(), velEme2000PatriusModifDestRef.getX(), tol);
        Assert.assertEquals(velEme2000CelestLab.getY(), velEme2000PatriusModifDestRef.getY(), tol);
        Assert.assertEquals(velEme2000CelestLab.getZ(), velEme2000PatriusModifDestRef.getZ(), tol);

        // case 4 (without projection of velocity and acceleration): no referential coincides with its corresponding
        // frame and no referential is on the path from the origin frame to the destination frame

        // change the referential of the origin frame
        originFrame.setReferential(FramesFactory.getMOD(true));
        // build the transform needed to pass from the GCRF frame to the EME2000 frame
        final Transform transformGcrfToEme2000ModifOrigRefAndDestRef = originFrame.getTransformTo(destinationFrame,
            date, FramesFactory.getConfiguration(), true);
        // compute pv in EME2000
        final PVCoordinates pvCoordEme2000PatriusModifOrigRefAndDestRef = transformGcrfToEme2000ModifOrigRefAndDestRef
            .transformPVCoordinates(pvCoordGcrf);
        final Vector3D posEme2000PatriusModifOrigRefAndDestRef = pvCoordEme2000PatriusModifOrigRefAndDestRef
            .getPosition();
        final Vector3D velEme2000PatriusModifOrigRefAndDestRef = pvCoordEme2000PatriusModifOrigRefAndDestRef
            .getVelocity();
        // check that the positions in EME2000 are the same
        Assert.assertEquals(posEme2000CelestLab.getX(), posEme2000PatriusModifOrigRefAndDestRef.getX(), tol);
        Assert.assertEquals(posEme2000CelestLab.getY(), posEme2000PatriusModifOrigRefAndDestRef.getY(), tol);
        Assert.assertEquals(posEme2000CelestLab.getZ(), posEme2000PatriusModifOrigRefAndDestRef.getZ(), tol);
        // check that the velocities in EME2000 are the same
        Assert.assertEquals(velEme2000CelestLab.getX(), velEme2000PatriusModifOrigRefAndDestRef.getX(), tol);
        Assert.assertEquals(velEme2000CelestLab.getY(), velEme2000PatriusModifOrigRefAndDestRef.getY(), tol);
        Assert.assertEquals(velEme2000CelestLab.getZ(), velEme2000PatriusModifOrigRefAndDestRef.getZ(), tol);

        // case 5 (without projection of velocity and acceleration): no referential coincides with its corresponding
        // frame and the referential of the origin frame is on the path from the origin frame to the destination frame

        // change the referential of the origin frame
        originFrame.setReferential(FramesFactory.getEME2000());
        // define a new destination frame
        final FactoryManagedFrame modifDestinationFrame = FramesFactory.getMOD(false);
        // change the referential of the destination frame
        modifDestinationFrame.setReferential(FramesFactory.getTOD(false));
        // build the transform needed to pass from the GCRF frame to the MOD frame
        final Transform transformGcrfToMod = originFrame.getTransformTo(modifDestinationFrame, date,
            FramesFactory.getConfiguration(), true);
        // compute pv in MOD
        final PVCoordinates pvCoordModPatrius = transformGcrfToMod.transformPVCoordinates(pvCoordGcrf);
        final Vector3D posModPatrius = pvCoordModPatrius.getPosition();
        final Vector3D velModPatrius = pvCoordModPatrius.getVelocity();
        // use CelestLab pv values in MOD as reference
        final Vector3D posModCelestLab = new Vector3D(2563882.203363891225, -985600.3132453040453, 6236980.192136811092);
        final Vector3D velModCelestLab = new Vector3D(-7090.262058990344485, -448.9103394367006672,
            2840.578008473581122);
        // adapt the tolerance
        final double modifTol = 3.1e-4;
        // check that the positions in MOD are the same
        Assert.assertEquals(posModCelestLab.getX(), posModPatrius.getX(), modifTol);
        Assert.assertEquals(posModCelestLab.getY(), posModPatrius.getY(), modifTol);
        Assert.assertEquals(posModCelestLab.getZ(), posModPatrius.getZ(), modifTol);
        // check that the velocities in MOD are the same
        Assert.assertEquals(velModCelestLab.getX(), velModPatrius.getX(), modifTol);
        Assert.assertEquals(velModCelestLab.getY(), velModPatrius.getY(), modifTol);
        Assert.assertEquals(velModCelestLab.getZ(), velModPatrius.getZ(), modifTol);

        // case 6 (with projection of velocity and acceleration): only the referential of the origin frame coincides
        // with its corresponding (origin) frame and the referential of the destination frame is on the path from the
        // origin frame to the destination frame (so the velocity and the acceleration are simply projected from the
        // referential of the destination frame to its corresponding (destination) frame)

        // change the referential of the origin frame
        originFrame.setReferential(FramesFactory.getGCRF());
        // change the referential of the destination frame
        modifDestinationFrame.setReferential(FramesFactory.getEME2000());
        // build the transform needed to pass from the GCRF frame to the MOD frame
        final Transform transformGcrfToModModifDestRef = originFrame.getTransformTo(modifDestinationFrame, date,
            FramesFactory.getConfiguration(), true);
        // compute pv in MOD
        final PVCoordinates pvCoordModPatriusModifDestRef = transformGcrfToModModifDestRef
            .transformPVCoordinates(pvCoordGcrf);
        final Vector3D posModPatriusModifDestRef = pvCoordModPatriusModifDestRef.getPosition();
        final Vector3D velModPatriusModifDestRef = pvCoordModPatriusModifDestRef.getVelocity();
        // check that the positions in MOD are the same
        Assert.assertEquals(posModCelestLab.getX(), posModPatriusModifDestRef.getX(), modifTol);
        Assert.assertEquals(posModCelestLab.getY(), posModPatriusModifDestRef.getY(), modifTol);
        Assert.assertEquals(posModCelestLab.getZ(), posModPatriusModifDestRef.getZ(), modifTol);
        // check that the velocities in MOD are the same
        Assert.assertEquals(velModCelestLab.getX(), velModPatriusModifDestRef.getX(), modifTol);
        Assert.assertEquals(velModCelestLab.getY(), velModPatriusModifDestRef.getY(), modifTol);
        Assert.assertEquals(velModCelestLab.getZ(), velModPatriusModifDestRef.getZ(), modifTol);

    }

    @Test
    public void testRandomComposition() {

        final RandomGenerator random = new Well19937a(0x171c79e323a1123l);
        for (int i = 0; i < 20; ++i) {

            // build a complex transform by composing primitive ones
            final int n = random.nextInt(20);
            final Transform[] transforms = new Transform[n];
            Transform combined = Transform.IDENTITY;
            for (int k = 0; k < n; ++k) {
                transforms[k] =
                    random.nextBoolean()
 ? new Transform(AbsoluteDate.J2000_EPOCH,
                        randomVector(1.0e3, random), randomVector(1.0, random), randomVector(
                                1.0e-3, random)) : new Transform(AbsoluteDate.J2000_EPOCH,
                        randomRotation(random), randomVector(0.01,
                            random),
 randomVector(1.0e-4,
                                random));
                combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, transforms[k], true);
            }

            // check the composition
            for (int j = 0; j < 10; ++j) {
                final Vector3D a = randomVector(1.0, random);
                final Vector3D b = randomVector(1.0e3, random);
                final PVCoordinates c =
 new PVCoordinates(randomVector(1.0e3, random),
                        randomVector(1.0, random), randomVector(1.0e-3, random));
                Vector3D aRef = a;
                Vector3D bRef = b;
                PVCoordinates cRef = c;
                for (int k = 0; k < n; ++k) {
                    aRef = transforms[k].transformVector(aRef);
                    bRef = transforms[k].transformPosition(bRef);
                    cRef = transforms[k].transformPVCoordinates(cRef);
                }

                final Vector3D aCombined = combined.transformVector(a);
                final Vector3D bCombined = combined.transformPosition(b);
                final PVCoordinates cCombined = combined.transformPVCoordinates(c);
                checkVector(aRef, aCombined, 3.0e-15);
                checkVector(bRef, bCombined, 5.0e-14);
                checkVector(cRef.getPosition(), cCombined.getPosition(), 1.0e-14);
                checkVector(cRef.getVelocity(), cCombined.getVelocity(), 1.0e-14);
                checkVector(cRef.getAcceleration(), cCombined.getAcceleration(), 1.0e-14);

            }
        }

    }

    @Test
    public void testReverse() {
        final Random random = new Random(0x9f82ba2b2c98dac5l);
        for (int i = 0; i < 20; ++i) {
            final Transform combined = randomTransform(random);

            checkNoTransform(
                    new Transform(AbsoluteDate.J2000_EPOCH, combined, combined.getInverse()),
                    random);
            checkNoTransform(Transform.IDENTITY.getInverse(), random);

        }

    }

    @Test
    public void testDecomposeAndRebuild() {
        final RandomGenerator random = new Well19937a(0xb8ee9da1b05198c9l);
        for (int i = 0; i < 20; ++i) {
            final Transform combined = randomTransform(random);
            final Transform rebuilt = new Transform(combined.getDate(),
                new Transform(combined.getDate(), combined.getTranslation(),
                    combined.getVelocity(), combined.getAcceleration()),
                new Transform(combined.getDate(), combined.getRotation(),
                    combined.getRotationRate(), combined.getRotationAcceleration()), true);

            checkNoTransform(
                    new Transform(AbsoluteDate.J2000_EPOCH, combined, rebuilt.getInverse(true),
                            true),
                random);

        }
    }

    @Test
    public void testTranslation() {
        final Random rnd = new Random(0x7e9d737ba4147787l);
        for (int i = 0; i < 10; ++i) {
            final Vector3D delta = randomVector(rnd);
            final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, delta);
            for (int j = 0; j < 10; ++j) {
                final Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                final Vector3D b = transform.transformVector(a);
                Assert.assertEquals(0, b.subtract(a).getNorm(), 1.0e-10);
                final Vector3D c = transform.transformPosition(a);
                Assert.assertEquals(0, a.subtract(c).subtract(delta).getNorm(), 1.0e-13);
            }
        }
    }

    @Test
    public void testRoughTransPV() {

        final PVCoordinates pointP1 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.PLUS_I);

        // translation transform test
        final PVCoordinates pointP2 = new PVCoordinates(new Vector3D(0, 0, 0), new Vector3D(0, 0, 0), new Vector3D(0,
            0, 0));
        final Transform R1toR2 =
            new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.PLUS_I);
        final PVCoordinates result1 = R1toR2.transformPVCoordinates(pointP1);
        checkVector(pointP2.getPosition(), result1.getPosition(), 1.0e-15);
        checkVector(pointP2.getVelocity(), result1.getVelocity(), 1.0e-15);
        checkVector(pointP2.getAcceleration(), result1.getAcceleration(), 1.0e-15);

        // test inverse translation
        final Transform R2toR1 = R1toR2.getInverse();
        final PVCoordinates invResult1 = R2toR1.transformPVCoordinates(pointP2);
        checkVector(pointP1.getPosition(), invResult1.getPosition(), 1.0e-15);
        checkVector(pointP1.getVelocity(), invResult1.getVelocity(), 1.0e-15);
        checkVector(pointP1.getAcceleration(), invResult1.getAcceleration(), 1.0e-15);

        // rotation transform test
        final PVCoordinates pointP3 =
            new PVCoordinates(Vector3D.PLUS_J, new Vector3D(-2, 1, 0), new Vector3D(-4, -3, -1));
        final Rotation R = new Rotation(Vector3D.PLUS_K, -FastMath.PI / 2);
        final Transform R1toR3 =
            new Transform(AbsoluteDate.J2000_EPOCH, R, new Vector3D(0, 0, -2), new Vector3D(1, 0, 0));
        final PVCoordinates result2 = R1toR3.transformPVCoordinates(pointP1);
        checkVector(pointP3.getPosition(), result2.getPosition(), 1.0e-15);
        checkVector(pointP3.getVelocity(), result2.getVelocity(), 1.0e-15);
        checkVector(pointP3.getAcceleration(), result2.getAcceleration(), 1.0e-15);

        // test inverse rotation
        final Transform R3toR1 = R1toR3.getInverse(true);
        final PVCoordinates invResult2 = R3toR1.transformPVCoordinates(pointP3);
        checkVector(pointP1.getPosition(), invResult2.getPosition(), 1.0e-15);
        checkVector(pointP1.getVelocity(), invResult2.getVelocity(), 1.0e-15);
        checkVector(pointP1.getAcceleration(), invResult2.getAcceleration(), 1.0e-15);

        // combine 2 velocity transform
        final Transform R1toR4 =
            new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(-2, 0, 0), new Vector3D(-2, 0, 0),
                new Vector3D(-2, 0, 0));
        final PVCoordinates pointP4 =
            new PVCoordinates(new Vector3D(3, 0, 0), new Vector3D(3, 0, 0), new Vector3D(3, 0, 0));
        final Transform R2toR4 = new Transform(AbsoluteDate.J2000_EPOCH, R2toR1, R1toR4);
        final PVCoordinates compResult = R2toR4.transformPVCoordinates(pointP2);
        checkVector(pointP4.getPosition(), compResult.getPosition(), 1.0e-15);
        checkVector(pointP4.getVelocity(), compResult.getVelocity(), 1.0e-15);
        checkVector(pointP4.getAcceleration(), compResult.getAcceleration(), 1.0e-15);

        // combine 2 rotation tranform
        final PVCoordinates pointP5 =
            new PVCoordinates(new Vector3D(-1, 0, 0), new Vector3D(-1, 0, 3), new Vector3D(8, 0, 6));
        final Rotation R2 = new Rotation(new Vector3D(0, 0, 1), -FastMath.PI);
        final Transform R1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R2, new Vector3D(0, -3, 0));
        final Transform R3toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R3toR1, R1toR5, true);
        final PVCoordinates combResult = R3toR5.transformPVCoordinates(pointP3);
        checkVector(pointP5.getPosition(), combResult.getPosition(), 1.0e-15);
        checkVector(pointP5.getVelocity(), combResult.getVelocity(), 1.0e-15);
        checkVector(pointP5.getAcceleration(), combResult.getAcceleration(), 1.0e-15);

        // combine translation and rotation
        final Transform R2toR3 = new Transform(AbsoluteDate.J2000_EPOCH, R2toR1, R1toR3, true);
        PVCoordinates result = R2toR3.transformPVCoordinates(pointP2);
        checkVector(pointP3.getPosition(), result.getPosition(), 1.0e-15);
        checkVector(pointP3.getVelocity(), result.getVelocity(), 1.0e-15);
        checkVector(pointP3.getAcceleration(), result.getAcceleration(), 1.0e-15);

        final Transform R3toR2 = new Transform(AbsoluteDate.J2000_EPOCH, R3toR1, R1toR2, true);
        result = R3toR2.transformPVCoordinates(pointP3);
        checkVector(pointP2.getPosition(), result.getPosition(), 1.0e-15);
        checkVector(pointP2.getVelocity(), result.getVelocity(), 1.0e-15);
        checkVector(pointP2.getAcceleration(), result.getAcceleration(), 1.0e-15);

        Transform newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR2, R2toR3, true);
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, newR1toR5, R3toR5, true);
        result = newR1toR5.transformPVCoordinates(pointP1);
        checkVector(pointP5.getPosition(), result.getPosition(), 1.0e-15);
        checkVector(pointP5.getVelocity(), result.getVelocity(), 1.0e-15);
        checkVector(pointP5.getAcceleration(), result.getAcceleration(), 1.0e-15);

        // more tests
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR2, R2toR3, true);
        final Transform R3toR4 = new Transform(AbsoluteDate.J2000_EPOCH, R3toR1, R1toR4, true);
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, newR1toR5, R3toR4, true);
        final Transform R4toR5 = new Transform(AbsoluteDate.J2000_EPOCH, R1toR4.getInverse(), R1toR5, true);
        newR1toR5 = new Transform(AbsoluteDate.J2000_EPOCH, newR1toR5, R4toR5, true);
        result = newR1toR5.transformPVCoordinates(pointP1);
        checkVector(pointP5.getPosition(), result.getPosition(), 1.0e-15);
        checkVector(pointP5.getVelocity(), result.getVelocity(), 1.0e-15);
        checkVector(pointP5.getAcceleration(), result.getAcceleration(), 1.0e-15);

    }

    @Test
    public void testRotPV() {

        final Random rnd = new Random(0x73d5554d99427af0l);

        // Instant Rotation only

        for (int i = 0; i < 10; ++i) {

            // Random instant rotation

            final Rotation instantRot = randomRotation(rnd);
            final Vector3D normAxis = instantRot.getAxis();
            final double w = MathLib.abs(instantRot.getAngle()) / Constants.JULIAN_DAY;

            // random rotation
            final Rotation rot = randomRotation(rnd);

            // so we have a transform
            final Transform tr = new Transform(AbsoluteDate.J2000_EPOCH, rot, new Vector3D(w, normAxis));

            // random position and velocity
            final Vector3D pos = randomVector(rnd);
            final Vector3D vel = randomVector(rnd);

            final PVCoordinates pvOne = new PVCoordinates(pos, vel);

            // we obtain

            final PVCoordinates pvTwo = tr.transformPVCoordinates(pvOne);

            // test inverse

            final Vector3D resultvel = tr.getInverse().transformPVCoordinates(pvTwo).getVelocity();

            checkVectors(resultvel, vel);

        }

    }

    @Test
    public void testTransPV() {

        final Random rnd = new Random(0x73d5554d99427af0l);

        // translation velocity only :

        for (int i = 0; i < 10; ++i) {

            // random position and velocity
            final Vector3D pos = randomVector(rnd);
            final Vector3D vel = randomVector(rnd);
            final PVCoordinates pvOne = new PVCoordinates(pos, vel);

            // random transform
            final Vector3D trans = randomVector(rnd);
            final Vector3D transVel = randomVector(rnd);
            final Transform tr = new Transform(AbsoluteDate.J2000_EPOCH, trans, transVel);

            final double dt = 1;

            // we should obtain :

            final Vector3D good = tr.transformPosition(pos.add(new Vector3D(dt, vel))
                .subtract(new Vector3D(dt, tr.getVelocity())));

            // we have :

            final PVCoordinates pvTwo = tr.transformPVCoordinates(pvOne);
            final Vector3D result = (pvTwo.getPosition().add(new Vector3D(dt, pvTwo.getVelocity())));
            checkVectors(good, result);

            // test inverse

            final Vector3D resultvel = tr.getInverse().transformPVCoordinates(pvTwo).getVelocity();
            checkVectors(resultvel, vel);

        }

    }

    @Test
    public void testRotation() {
        final Random rnd = new Random(0x73d5554d99427af0l);
        for (int i = 0; i < 10; ++i) {

            final Rotation r = randomRotation(rnd);
            final Vector3D axis = r.getAxis();
            final double angle = r.getAngle();

            final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, r);
            for (int j = 0; j < 10; ++j) {
                final Vector3D a = new Vector3D(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                final Vector3D b = transform.transformVector(a);
                Assert.assertEquals(Vector3D.angle(axis, a), Vector3D.angle(axis, b), 1.0e-13);
                final Vector3D aOrtho = Vector3D.crossProduct(axis, a);
                final Vector3D bOrtho = Vector3D.crossProduct(axis, b);
                Assert.assertEquals(angle, Vector3D.angle(aOrtho, bOrtho), 1.0e-13);
                final Vector3D c = transform.transformPosition(a);
                Assert.assertEquals(0, c.subtract(b).getNorm(), 1.0e-13);
            }

        }
    }

    @Test
    public void testJacobian() {

        // base directions for finite differences
        final PVCoordinates[] directions = new PVCoordinates[] { new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO),
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO), new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO),
            new PVCoordinates(Vector3D.ZERO, Vector3D.PLUS_I), new PVCoordinates(Vector3D.ZERO, Vector3D.PLUS_J),
            new PVCoordinates(Vector3D.ZERO, Vector3D.PLUS_K) };
        final double h = 0.01;

        final Random random = new Random(0xce2bfddfbb9796bel);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            final Transform combined = randomTransform(random);

            // compute Jacobian
            final double[][] jacobian = new double[6][6];
            combined.getJacobian(jacobian);

            for (int j = 0; j < 100; ++j) {

                final PVCoordinates pv0 = new PVCoordinates(randomVector(random),
                        randomVector(random));
                final double epsilonP = 1.0e-11 * pv0.getPosition().getNorm();
                final double epsilonV = 1.0e-7 * pv0.getVelocity().getNorm();

                for (int l = 0; l < directions.length; ++l) {

                    // eight points finite differences estimation of a Jacobian column
                    final PVCoordinates pvm4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -4 * h,
                        directions[l]));
                    final PVCoordinates pvm3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -3 * h,
                        directions[l]));
                    final PVCoordinates pvm2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -2 * h,
                        directions[l]));
                    final PVCoordinates pvm1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, -1 * h,
                        directions[l]));
                    final PVCoordinates pvp1h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +1 * h,
                        directions[l]));
                    final PVCoordinates pvp2h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +2 * h,
                        directions[l]));
                    final PVCoordinates pvp3h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +3 * h,
                        directions[l]));
                    final PVCoordinates pvp4h = combined.transformPVCoordinates(new PVCoordinates(1.0, pv0, +4 * h,
                        directions[l]));
                    final PVCoordinates d4 = new PVCoordinates(pvm4h, pvp4h);
                    final PVCoordinates d3 = new PVCoordinates(pvm3h, pvp3h);
                    final PVCoordinates d2 = new PVCoordinates(pvm2h, pvp2h);
                    final PVCoordinates d1 = new PVCoordinates(pvm1h, pvp1h);
                    final double c = 1.0 / (840 * h);
                    final PVCoordinates estimatedColumn =
                        new PVCoordinates(-3 * c, d4, 32 * c, d3, -168 * c, d2, 672 * c, d1);

                    // check finite analytical Jacobian against finite difference reference
                    Assert.assertEquals(estimatedColumn.getPosition().getX(), jacobian[0][l], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getY(), jacobian[1][l], epsilonP);
                    Assert.assertEquals(estimatedColumn.getPosition().getZ(), jacobian[2][l], epsilonP);
                    Assert.assertEquals(estimatedColumn.getVelocity().getX(), jacobian[3][l], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getY(), jacobian[4][l], epsilonV);
                    Assert.assertEquals(estimatedColumn.getVelocity().getZ(), jacobian[5][l], epsilonV);

                }

            }
        }

    }

    @Test
    public void testLine() {
        final Random random = new Random(0x4a5ff67426c5731fl);
        for (int i = 0; i < 100; ++i) {
            final Transform transform = randomTransform(random);
            for (int j = 0; j < 20; ++j) {
                final Vector3D p0 = randomVector(random);
                final Vector3D p1 = randomVector(random);
                final Line l = new Line(p0, p1);
                final Line transformed = transform.transformLine(l);
                for (int k = 0; k < 10; ++k) {
                    final Vector3D p = l.pointAt(random.nextDouble() * 1.0e6);
                    Assert.assertEquals(0.0, transformed.distance(transform.transformPosition(p)), 1.0e-6);
                }
            }
        }
    }

    @Test
    public void testLinear() {

        final Random random = new Random(0x14f6411217b148d8l);
        for (int n = 0; n < 100; ++n) {
            final Transform t = randomTransform(random);

            // build an equivalent linear transform by extracting raw translation/rotation
            final RealMatrix linearA = MatrixUtils.createRealMatrix(3, 4);
            linearA.setSubMatrix(t.getRotation().revert().getMatrix(), 0, 0);
            final Vector3D rt = t.getRotation().applyInverseTo(t.getTranslation().negate());
            linearA.setEntry(0, 3, rt.getX());
            linearA.setEntry(1, 3, rt.getY());
            linearA.setEntry(2, 3, rt.getZ());

            // build an equivalent linear transform by observing transformed points
            final RealMatrix linearB = MatrixUtils.createRealMatrix(3, 4);
            final Vector3D p0 = t.transformPosition(Vector3D.ZERO);
            final Vector3D pI = t.transformPosition(Vector3D.PLUS_I).subtract(p0);
            final Vector3D pJ = t.transformPosition(Vector3D.PLUS_J).subtract(p0);
            final Vector3D pK = t.transformPosition(Vector3D.PLUS_K).subtract(p0);
            linearB.setColumn(0, new double[] { pI.getX(), pI.getY(), pI.getZ() });
            linearB.setColumn(1, new double[] { pJ.getX(), pJ.getY(), pJ.getZ() });
            linearB.setColumn(2, new double[] { pK.getX(), pK.getY(), pK.getZ() });
            linearB.setColumn(3, new double[] { p0.getX(), p0.getY(), p0.getZ() });

            // both linear transforms should be equal
            Assert.assertEquals(0.0, linearB.subtract(linearA).getNorm(), 1.0e-9 * linearA.getNorm());

            for (int i = 0; i < 100; ++i) {
                final Vector3D p = randomVector(random);
                final Vector3D q = t.transformPosition(p);

                final double[] qA = linearA.operate(new double[] { p.getX(), p.getY(), p.getZ(), 1.0 });
                Assert.assertEquals(q.getX(), qA[0], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getY(), qA[1], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getZ(), qA[2], 1.0e-9 * p.getNorm());

                final double[] qB = linearB.operate(new double[] { p.getX(), p.getY(), p.getZ(), 1.0 });
                Assert.assertEquals(q.getX(), qB[0], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getY(), qB[1], 1.0e-9 * p.getNorm());
                Assert.assertEquals(q.getZ(), qB[2], 1.0e-9 * p.getNorm());

            }

        }

    }

    @Test
    public void testShift() {

        Report.printMethodHeader("testShift", "Shift", "Math", 1.0e-14, ComparisonType.RELATIVE);

        // the following transform corresponds to a frame moving along the line x=1 and rotating around its -z axis
        // the linear motion velocity is (0, +1, 0), the angular rate is PI/2
        // at t = -1 the frame origin is at (1, -1, 0), its X axis is equal to Xref and its Y axis is equal to Yref
        // at t = 0 the frame origin is at (1, 0, 0), its X axis is equal to -Yref and its Y axis is equal to Xref
        // at t = +1 the frame origin is at (1, +1, 0), its X axis is equal to -Xref and its Y axis is equal to -Yref
        final AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        final double alpha0 = 0.5 * FastMath.PI;
        final double omega = 0.5 * FastMath.PI;
        final Transform t = new Transform(date,
            new Transform(date, Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.ZERO),
            new Transform(date,
                new Rotation(Vector3D.PLUS_K, -alpha0),
                new Vector3D(omega, Vector3D.MINUS_K)));

        for (double dt = -10.0; dt < 10.0; dt += 0.125) {

            final Transform shifted = t.shiftedBy(dt);

            // the following point should always remain at moving frame origin
            final PVCoordinates expectedFixedPoint =
                shifted.transformPVCoordinates(new PVCoordinates(new Vector3D(1, dt, 0), Vector3D.PLUS_J,
                    Vector3D.ZERO));
            checkVector(expectedFixedPoint.getPosition(), Vector3D.ZERO, 1.0e-14);
            checkVector(expectedFixedPoint.getVelocity(), Vector3D.ZERO, 1.0e-14);
            checkVector(expectedFixedPoint.getAcceleration(), Vector3D.ZERO, 1.0e-14);

            // fixed frame origin apparent motion in moving frame
            final PVCoordinates expectedApparentMotion = shifted.transformPVCoordinates(PVCoordinates.ZERO);
            final double c = MathLib.cos(alpha0 + omega * dt);
            final double s = MathLib.sin(alpha0 + omega * dt);
            final Vector3D referencePosition = new Vector3D(-c + dt * s,
                -s - dt * c,
                0);
            final Vector3D referenceVelocity = new Vector3D((1 + omega) * s + dt * omega * c,
                -(1 + omega) * c + dt * omega * s,
                0);
            final Vector3D referenceAcceleration = new Vector3D(omega * (2 + omega) * c - dt * omega * omega * s,
                omega * (2 + omega) * s + dt * omega * omega * c,
                0);
            checkVector(expectedApparentMotion.getPosition(), referencePosition, 1.0e-14);
            checkVector(expectedApparentMotion.getVelocity(), referenceVelocity, 1.0e-14);
            checkVector(expectedApparentMotion.getAcceleration(), referenceAcceleration, 1.0e-14);

            if (dt == -10) {
                Report.printToReport("Transformed position", referencePosition, expectedApparentMotion.getPosition());
                Report.printToReport("Transformed velocity", referenceVelocity, expectedApparentMotion.getVelocity());
                Report.printToReport("Transformed acceleration", referenceAcceleration,
                    expectedApparentMotion.getAcceleration());
            }
        }

    }

    @Test
    public void testShiftDerivatives() {

        final RandomGenerator random = new Well19937a(0x5acda4f605aadce7l);
        for (int i = 0; i < 10; ++i) {
            final Transform t = randomTransform(random);

            for (double dt = -10.0; dt < 10.0; dt += 0.125) {

                final Transform t0 = t.shiftedBy(dt, true);
                final double v = t0.getVelocity().getNorm();
                final double a = t0.getAcceleration().getNorm();
                final double omega = t0.getRotationRate().getNorm();
                final double omegaDot = t0.getRotationAcceleration().getNorm();

                // numerical derivatives
                final double h = 0.01 / omega;
                final Transform tm4h = t.shiftedBy(dt - 4 * h, true);
                final Transform tm3h = t.shiftedBy(dt - 3 * h, true);
                final Transform tm2h = t.shiftedBy(dt - 2 * h, true);
                final Transform tm1h = t.shiftedBy(dt - 1 * h, true);
                final Transform tp1h = t.shiftedBy(dt + 1 * h, true);
                final Transform tp2h = t.shiftedBy(dt + 2 * h, true);
                final Transform tp3h = t.shiftedBy(dt + 3 * h, true);
                final Transform tp4h = t.shiftedBy(dt + 4 * h, true);
                final double numXDot = derivative(h,
                    tm4h.getTranslation().getX(), tm3h.getTranslation().getX(),
                    tm2h.getTranslation().getX(), tm1h.getTranslation().getX(),
                    tp1h.getTranslation().getX(), tp2h.getTranslation().getX(),
                    tp3h.getTranslation().getX(), tp4h.getTranslation().getX());
                final double numYDot = derivative(h,
                    tm4h.getTranslation().getY(), tm3h.getTranslation().getY(),
                    tm2h.getTranslation().getY(), tm1h.getTranslation().getY(),
                    tp1h.getTranslation().getY(), tp2h.getTranslation().getY(),
                    tp3h.getTranslation().getY(), tp4h.getTranslation().getY());
                final double numZDot = derivative(h,
                    tm4h.getTranslation().getZ(), tm3h.getTranslation().getZ(),
                    tm2h.getTranslation().getZ(), tm1h.getTranslation().getZ(),
                    tp1h.getTranslation().getZ(), tp2h.getTranslation().getZ(),
                    tp3h.getTranslation().getZ(), tp4h.getTranslation().getZ());
                final double numXDot2 = derivative(h,
                    tm4h.getVelocity().getX(), tm3h.getVelocity().getX(),
                    tm2h.getVelocity().getX(), tm1h.getVelocity().getX(),
                    tp1h.getVelocity().getX(), tp2h.getVelocity().getX(),
                    tp3h.getVelocity().getX(), tp4h.getVelocity().getX());
                final double numYDot2 = derivative(h,
                    tm4h.getVelocity().getY(), tm3h.getVelocity().getY(),
                    tm2h.getVelocity().getY(), tm1h.getVelocity().getY(),
                    tp1h.getVelocity().getY(), tp2h.getVelocity().getY(),
                    tp3h.getVelocity().getY(), tp4h.getVelocity().getY());
                final double numZDot2 = derivative(h,
                    tm4h.getVelocity().getZ(), tm3h.getVelocity().getZ(),
                    tm2h.getVelocity().getZ(), tm1h.getVelocity().getZ(),
                    tp1h.getVelocity().getZ(), tp2h.getVelocity().getZ(),
                    tp3h.getVelocity().getZ(), tp4h.getVelocity().getZ());
                final double numQ0Dot = derivative(h,
                    tm4h.getRotation().getQuaternion().getQ0(), tm3h.getRotation().getQuaternion().getQ0(),
                    tm2h.getRotation().getQuaternion().getQ0(), tm1h.getRotation().getQuaternion().getQ0(),
                    tp1h.getRotation().getQuaternion().getQ0(), tp2h.getRotation().getQuaternion().getQ0(),
                    tp3h.getRotation().getQuaternion().getQ0(), tp4h.getRotation().getQuaternion().getQ0());
                final double numQ1Dot = derivative(h,
                    tm4h.getRotation().getQuaternion().getQ1(), tm3h.getRotation().getQuaternion().getQ1(),
                    tm2h.getRotation().getQuaternion().getQ1(), tm1h.getRotation().getQuaternion().getQ1(),
                    tp1h.getRotation().getQuaternion().getQ1(), tp2h.getRotation().getQuaternion().getQ1(),
                    tp3h.getRotation().getQuaternion().getQ1(), tp4h.getRotation().getQuaternion().getQ1());
                final double numQ2Dot = derivative(h,
                    tm4h.getRotation().getQuaternion().getQ2(), tm3h.getRotation().getQuaternion().getQ2(),
                    tm2h.getRotation().getQuaternion().getQ2(), tm1h.getRotation().getQuaternion().getQ2(),
                    tp1h.getRotation().getQuaternion().getQ2(), tp2h.getRotation().getQuaternion().getQ2(),
                    tp3h.getRotation().getQuaternion().getQ2(), tp4h.getRotation().getQuaternion().getQ2());
                final double numQ3Dot = derivative(h,
                    tm4h.getRotation().getQuaternion().getQ3(), tm3h.getRotation().getQuaternion().getQ3(),
                    tm2h.getRotation().getQuaternion().getQ3(), tm1h.getRotation().getQuaternion().getQ3(),
                    tp1h.getRotation().getQuaternion().getQ3(), tp2h.getRotation().getQuaternion().getQ3(),
                    tp3h.getRotation().getQuaternion().getQ3(), tp4h.getRotation().getQuaternion().getQ3());
                final double numOxDot = derivative(h,
                    tm4h.getRotationRate().getX(), tm3h.getRotationRate().getX(),
                    tm2h.getRotationRate().getX(), tm1h.getRotationRate().getX(),
                    tp1h.getRotationRate().getX(), tp2h.getRotationRate().getX(),
                    tp3h.getRotationRate().getX(), tp4h.getRotationRate().getX());
                final double numOyDot = derivative(h,
                    tm4h.getRotationRate().getY(), tm3h.getRotationRate().getY(),
                    tm2h.getRotationRate().getY(), tm1h.getRotationRate().getY(),
                    tp1h.getRotationRate().getY(), tp2h.getRotationRate().getY(),
                    tp3h.getRotationRate().getY(), tp4h.getRotationRate().getY());
                final double numOzDot = derivative(h,
                    tm4h.getRotationRate().getZ(), tm3h.getRotationRate().getZ(),
                    tm2h.getRotationRate().getZ(), tm1h.getRotationRate().getZ(),
                    tp1h.getRotationRate().getZ(), tp2h.getRotationRate().getZ(),
                    tp3h.getRotationRate().getZ(), tp4h.getRotationRate().getZ());

                // theoretical derivatives
                final double theXDot = t0.getVelocity().getX();
                final double theYDot = t0.getVelocity().getY();
                final double theZDot = t0.getVelocity().getZ();
                final double theXDot2 = t0.getAcceleration().getX();
                final double theYDot2 = t0.getAcceleration().getY();
                final double theZDot2 = t0.getAcceleration().getZ();
                final Rotation r0 = t0.getRotation();
                final Vector3D w = t0.getRotationRate();
                final Vector3D q =
                    new Vector3D(r0.getQuaternion().getQ1(), r0.getQuaternion().getQ2(), r0.getQuaternion()
                        .getQ3());
                final Vector3D qw = Vector3D.crossProduct(q, w);
                final double theQ0Dot = -0.5 * Vector3D.dotProduct(q, w);
                final double theQ1Dot = 0.5 * (r0.getQuaternion().getQ0() * w.getX() + qw.getX());
                final double theQ2Dot = 0.5 * (r0.getQuaternion().getQ0() * w.getY() + qw.getY());
                final double theQ3Dot = 0.5 * (r0.getQuaternion().getQ0() * w.getZ() + qw.getZ());
                final double theOxDot2 = t0.getRotationAcceleration().getX();
                final double theOyDot2 = t0.getRotationAcceleration().getY();
                final double theOzDot2 = t0.getRotationAcceleration().getZ();

                // check consistency
                Assert.assertEquals(theXDot, numXDot, 1.0e-8 * v);
                Assert.assertEquals(theYDot, numYDot, 1.0e-8 * v);
                Assert.assertEquals(theZDot, numZDot, 1.0e-8 * v);

                Assert.assertEquals(theXDot2, numXDot2, 1.0e-13 * a);
                Assert.assertEquals(theYDot2, numYDot2, 1.0e-13 * a);
                Assert.assertEquals(theZDot2, numZDot2, 1.0e-13 * a);

                Assert.assertEquals(theQ0Dot, numQ0Dot, 1.0e-13 * omega);
                Assert.assertEquals(theQ1Dot, numQ1Dot, 1.0e-13 * omega);
                Assert.assertEquals(theQ2Dot, numQ2Dot, 1.0e-13 * omega);
                Assert.assertEquals(theQ3Dot, numQ3Dot, 1.0e-13 * omega);

                Assert.assertEquals(theOxDot2, numOxDot, 1.0e-12 * omegaDot);
                Assert.assertEquals(theOyDot2, numOyDot, 1.0e-12 * omegaDot);
                Assert.assertEquals(theOzDot2, numOzDot, 1.0e-12 * omegaDot);

            }
        }
    }

    @Test
    public void testInterpolation() throws PatriusException {

        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final List<Transform> sample = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            sample.add(evolvingTransform(t0, i * 0.8));
        }

        for (double dt = 0.1; dt <= 3.1; dt += 0.01) {
            final Transform reference = evolvingTransform(t0, dt);
            final Transform interpolated = sample.get(0).interpolate(reference.getDate(), sample);
            final Transform error = new Transform(reference.getDate(), reference, interpolated.getInverse(), true);
            Assert.assertEquals(0.0, error.getCartesian().getPosition().getNorm(), 2.0e-12);
            Assert.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(), 6.0e-12);
            Assert.assertEquals(0.0, error.getCartesian().getAcceleration().getNorm(), 4.0e-12);
            Assert.assertEquals(0.0, error.getAngular().getRotation().getAngle(), 2.0e-15);
            Assert.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(), 6.0e-15);
            Assert.assertEquals(0.0, error.getAngular().getRotationAcceleration().getNorm(), 4.0e-14);
            if (dt == 0.11) {
                Report.printMethodHeader("testInterpolation", "Cartesian Interpolation", "Math", 6.0e-12,
                    ComparisonType.RELATIVE);
                Report.printToReport("Position", Vector3D.ZERO, error.getCartesian().getPosition());
                Report.printToReport("Velocity", Vector3D.ZERO, error.getCartesian().getVelocity());
                Report.printToReport("Acceleration", Vector3D.ZERO, error.getCartesian().getAcceleration());
                Report.printMethodHeader("testInterpolation", "Angular Interpolation", "Math", 4.0e-14,
                    ComparisonType.RELATIVE);
                Report.printToReport("Rotation", Rotation.IDENTITY, error.getRotation());
                Report.printToReport("Spin", Vector3D.ZERO, error.getRotationRate());
                Report.printToReport("Rotation acceleration", Vector3D.ZERO, error.getRotationAcceleration());
            }
        }
    }

    @Test
    public void testInterpolationRotationOnly() throws PatriusException {

        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final List<Transform> sample = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            sample.add(evolvingTransform2(t0, i * 0.8));
        }

        for (double dt = 0.1; dt <= 3.1; dt += 0.01) {
            final Transform reference = evolvingTransform2(t0, dt);
            final Transform interpolated = sample.get(0).interpolate(reference.getDate(), sample);
            final Transform error = new Transform(reference.getDate(), reference, interpolated.getInverse());
            Assert.assertEquals(0.0, error.getCartesian().getPosition().getNorm(), 0);
            Assert.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(), 0);
            Assert.assertEquals(0.0, error.getAngular().getRotation().getAngle(), 8.0e-9);
            Assert.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(), 5.0e-8);

        }

    }

    private static Transform evolvingTransform(final AbsoluteDate t0, final double dt) {
        // the following transform corresponds to a frame moving along the circle r = 1
        // with its x axis always pointing to the reference frame center
        final double omega = 0.2;
        final AbsoluteDate date = t0.shiftedBy(dt);
        final double cos = MathLib.cos(omega * dt);
        final double sin = MathLib.sin(omega * dt);
        return new Transform(date,
            new Transform(date,
                new Vector3D(-cos, -sin, 0),
                new Vector3D(omega * sin, -omega * cos, 0),
                new Vector3D(omega * omega * cos, omega * omega * sin, 0)),
            new Transform(date,
                new Rotation(Vector3D.PLUS_K, FastMath.PI + omega * dt),
                new Vector3D(omega, Vector3D.PLUS_K)));
    }

    private static Transform evolvingTransform2(final AbsoluteDate t0, final double dt) {
        final double omega = 0.2;
        final AbsoluteDate date = t0.shiftedBy(dt);
        return new Transform(date, new Rotation(Vector3D.PLUS_K, FastMath.PI + omega * dt), new Vector3D(
            omega, Vector3D.PLUS_K));
    }

    private static double derivative(final double h,
                              final double ym4h, final double ym3h, final double ym2h, final double ym1h,
                              final double yp1h, final double yp2h, final double yp3h, final double yp4h) {
        return (-3 * (yp4h - ym4h) + 32 * (yp3h - ym3h) - 168 * (yp2h - ym2h) + 672 * (yp1h - ym1h)) /
                (840 * h);
    }

    private static Transform randomTransform(final Random random) {
        // generate a random transform
        Transform combined = Transform.IDENTITY;
        for (int k = 0; k < 20; ++k) {
            final Transform t =
 random.nextBoolean() ? new Transform(AbsoluteDate.J2000_EPOCH,
                    randomVector(random), randomVector(random), randomVector(random))
                    : new Transform(AbsoluteDate.J2000_EPOCH, randomRotation(random),
                            randomVector(random), randomVector(random));
            combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, t);
        }
        return combined;
    }

    private static Transform randomTransform(final RandomGenerator random) {
        // generate a random transform
        Transform combined = Transform.IDENTITY;
        for (int k = 0; k < 20; ++k) {
            final Transform t =
                random.nextBoolean() ?
 new Transform(AbsoluteDate.J2000_EPOCH,
                    randomVector(1.0e3, random), randomVector(1.0,
                        random),
 randomVector(1.0e-3,
                            random)) : new Transform(AbsoluteDate.J2000_EPOCH,
                    randomRotation(random), randomVector(0.01, random),
                    randomVector(1.0e-4, random));
            combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, t, true);
        }
        return combined;
    }

    private static void checkVectors(final Vector3D v1, final Vector3D v2) {

        final Vector3D d = v1.subtract(v2);

        Assert.assertEquals(0, d.getX(), 1.0e-8);
        Assert.assertEquals(0, d.getY(), 1.0e-8);
        Assert.assertEquals(0, d.getZ(), 1.0e-8);

        Assert.assertEquals(0, d.getNorm(), 1.0e-8);

        if ((v1.getNorm() > 1.0e-10) && (v2.getNorm() > 1.0e-10)) {
            final Rotation r = new Rotation(v1, v2);
            Assert.assertEquals(0, r.getAngle(), 1.0e-8);
        }
    }

    private static Vector3D randomVector(final Random random) {
        return new Vector3D(random.nextDouble() * 1.0, random.nextDouble() * 1.0, random.nextDouble() * 1.0);
    }

    private static Vector3D randomVector(final double scale, final RandomGenerator random) {
        return new Vector3D(random.nextDouble() * scale,
            random.nextDouble() * scale,
            random.nextDouble() * scale);
    }

    private static Rotation randomRotation(final Random random) {
        final double q0 = random.nextDouble() * 2 - 1;
        final double q1 = random.nextDouble() * 2 - 1;
        final double q2 = random.nextDouble() * 2 - 1;
        final double q3 = random.nextDouble() * 2 - 1;
        final double q = MathLib.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(false, q0 / q, q1 / q, q2 / q, q3 / q);
    }

    private static Rotation randomRotation(final RandomGenerator random) {
        final double q0 = random.nextDouble() * 2 - 1;
        final double q1 = random.nextDouble() * 2 - 1;
        final double q2 = random.nextDouble() * 2 - 1;
        final double q3 = random.nextDouble() * 2 - 1;
        final double q = MathLib.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(false, q0 / q, q1 / q, q2 / q, q3 / q);
    }

    private static void checkNoTransform(final Transform transform, final Random random) {
        for (int i = 0; i < 100; ++i) {
            final Vector3D a = randomVector(random);
            final Vector3D tA = transform.transformVector(a);
            Assert.assertEquals(0, a.subtract(tA).getNorm(), 1.0e-10 * a.getNorm());
            final Vector3D b = randomVector(random);
            final Vector3D tB = transform.transformPosition(b);
            Assert.assertEquals(0, b.subtract(tB).getNorm(), 1.0e-10 * a.getNorm());
            final PVCoordinates pv = new PVCoordinates(randomVector(random), randomVector(random));
            final PVCoordinates tPv = transform.transformPVCoordinates(pv);
            Assert.assertEquals(0, pv.getPosition().subtract(tPv.getPosition()).getNorm(), 1.0e-10 * pv.getPosition()
                .getNorm());
            Assert.assertEquals("" + (1.0e-9 * pv.getVelocity().getNorm()), 0,
                pv.getVelocity().subtract(tPv.getVelocity()).getNorm(), 3.0e-9 * pv.getVelocity().getNorm());
        }
    }

    private static void checkNoTransform(final Transform transform, final RandomGenerator random) {
        for (int i = 0; i < 100; ++i) {
            final Vector3D a = randomVector(1.0e3, random);
            final Vector3D tA = transform.transformVector(a);
            Assert.assertEquals(0, a.subtract(tA).getNorm(), 1.0e-10 * a.getNorm());
            final Vector3D b = randomVector(1.0e3, random);
            final Vector3D tB = transform.transformPosition(b);
            Assert.assertEquals(0, b.subtract(tB).getNorm(), 1.0e-10 * a.getNorm());
            final PVCoordinates pv =
 new PVCoordinates(randomVector(1.0e3, random), randomVector(
                    1.0, random), randomVector(
                    1.0e-3, random));
            final PVCoordinates tPv = transform.transformPVCoordinates(pv);
            checkVector(pv.getPosition(), tPv.getPosition(), 1.0e-10);
            checkVector(pv.getVelocity(), tPv.getVelocity(), 3.0e-9);
            checkVector(pv.getAcceleration(), tPv.getAcceleration(), 3.0e-9);
        }
    }

    @Test
    public void testCoverageCompletion()
        throws IllegalArgumentException {

        // shift tranform
        final Transform t0 = Transform.IDENTITY;
        final Transform t = Transform.IDENTITY.shiftedBy(10.);
        Assert.assertTrue(t0.equals(t));

        // get jacobian
        final double[][] a = new double[6][6];
        t0.getJacobian(a);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assert.assertTrue(i == j ? a[i][j] == 1 : a[i][j] == 0);
            }
        }
    }

    @Test
    public void testTransformWrench() {
        final Vector3D origin = Vector3D.ZERO;
        final Vector3D force = Vector3D.PLUS_J;
        final Vector3D torque = Vector3D.PLUS_K;
        final Wrench wrench = new Wrench(origin, force, torque);

        final Rotation rot = new Rotation(Vector3D.MINUS_J, FastMath.PI / 2);
        final Transform t = new Transform(AbsoluteDate.J2000_EPOCH, rot);

        final Wrench transformed = t.transformWrench(wrench);

        checkVectors(force, transformed.getForce());
        checkVectors(Vector3D.PLUS_I, transformed.getTorque());
    }

    @Test
    public void testSerialization() {

        // random tests
        final RandomGenerator random = new Well19937a(0xa0ee1db1b05288d0l);
        for (int i = 0; i < 20; ++i) {
            final Transform transform = randomTransform(random);
            final Transform transform2 = TestUtils.serializeAndRecover(transform);
            assertEqualsTransform(transform, transform2);
        }

        // constants test
        final Transform transform = Transform.IDENTITY;
        final Transform transform2 = TestUtils.serializeAndRecover(transform);
        assertEqualsTransform(transform, transform2);

    }

    public static void assertEqualsTransform(final Transform transform1, final Transform transform2) {
        Assert.assertEquals(transform1.getDate(), transform2.getDate());
        PVCoordinatesTest.assertEqualsPVCoordinates(transform1.getCartesian(), transform2.getCartesian());
        assertEqualsAngularCoordinates(transform1.getAngular(), transform2.getAngular());
    }

    public static void assertEqualsAngularCoordinates(final AngularCoordinates c1, final AngularCoordinates c2) {
        Assert.assertTrue(c1.getRotation().isEqualTo(c2.getRotation()));
        Assert.assertEquals(c1.getRotationRate(), c2.getRotationRate());
        Assert.assertEquals(c1.getRotationAcceleration(), c2.getRotationAcceleration());
    }

    private static void checkVector(final Vector3D reference, final Vector3D result,
            final double relativeTolerance) {
        if (reference == null) {
            Assert.assertNull(result);
        } else {
            final double refNorm = reference.getNorm();
            final double resNorm = result.getNorm();
            final double tolerance = relativeTolerance * (1 + MathLib.max(refNorm, resNorm));
            Assert.assertEquals("ref = " + reference + ", res = " + result + " -> " +
                    (Vector3D.distance(reference, result) / (1 + MathLib.max(refNorm, resNorm))),
                0, Vector3D.distance(reference, result), tolerance);
        }
    }

    private static Transform buildRandomTransform(final RandomGenerator random) {

        final Vector3D p = randomVector(1.0e3, random);
        final Vector3D v = randomVector(1.0, random);
        final Vector3D a = randomVector(1.0e-3, random);
        final Rotation r = randomRotation(random);
        final Vector3D o = randomVector(0.1, random);

        return new Transform(AbsoluteDate.J2000_EPOCH, new Transform(AbsoluteDate.J2000_EPOCH, p, v, a), new Transform(
            AbsoluteDate.J2000_EPOCH, r, o), true);
    }
}
