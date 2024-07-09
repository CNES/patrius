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
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.FiniteDifferencesDifferentiator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.FieldVector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937a;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class PVCoordinatesTest {

    @Test
    public void testDefaultConstructor() {
        Assert.assertEquals("{P(0.0, 0.0, 0.0), V(0.0, 0.0, 0.0), A(0.0, 0.0, 0.0)}", new PVCoordinates().toString());
    }

    @Test
    public void testSimpleConstructor() {
        Assert.assertEquals("{P(0.1, 0.2, 0.3), V(-1.2, -34.2, 1.0)}", new PVCoordinates(0.1, 0.2,
                0.3, -1.2, -34.2, 1.).toString());
    }

    @Test
    public void testLinearConstructors() {
        final PVCoordinates pv1 = new PVCoordinates(new Vector3D(1, 0.1, 10),
            new Vector3D(-1, -0.1, -10));
        final PVCoordinates pv2 = new PVCoordinates(new Vector3D(2, 0.2, 20),
            new Vector3D(-2, -0.2, -20));
        final PVCoordinates pv3 = new PVCoordinates(new Vector3D(3, 0.3, 30),
            new Vector3D(-3, -0.3, -30));
        final PVCoordinates pv4 = new PVCoordinates(new Vector3D(4, 0.4, 40),
            new Vector3D(-4, -0.4, -40));
        this.checkPV(pv4, new PVCoordinates(4, pv1), 1.0e-15);
        this.checkPV(pv2, new PVCoordinates(pv1, pv3), 1.0e-15);
        this.checkPV(pv3, new PVCoordinates(1, pv1, 1, pv2), 1.0e-15);
        this.checkPV(new PVCoordinates(2, pv4), new PVCoordinates(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        this.checkPV(new PVCoordinates(3, pv3), new PVCoordinates(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        this.checkPV(new PVCoordinates(5, pv4), new PVCoordinates(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVectorNeg() throws PatriusException {
        try {
            PVCoordinates.ZERO.toDerivativeStructureVector(-1);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException oe) {
            Assert.assertEquals(PatriusMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector3() throws PatriusException {
        try {
            PVCoordinates.ZERO.toDerivativeStructureVector(3);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException oe) {
            Assert.assertEquals(PatriusMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(3, ((Integer) (oe.getParts()[0])).intValue());
        }
    }

    @Test
    public void testToDerivativeStructureVector0() throws PatriusException {
        final FieldVector3D<DerivativeStructure> fv =
            new PVCoordinates(new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(0);
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(0, fv.getX().getOrder());
        Assert.assertEquals(1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(10.0, fv.getZ().getReal(), 1.0e-10);
        this.checkPV(new PVCoordinates(new Vector3D(1, 0.1, 10),
            Vector3D.ZERO,
            Vector3D.ZERO),
            new PVCoordinates(fv), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector1() throws PatriusException {
        final FieldVector3D<DerivativeStructure> fv =
            new PVCoordinates(new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(1);
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(1, fv.getX().getOrder());
        Assert.assertEquals(1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(10.0, fv.getZ().getReal(), 1.0e-10);
        Assert.assertEquals(-1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(-0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(-10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        this.checkPV(new PVCoordinates(new Vector3D(1, 0.1, 10),
            new Vector3D(-1, -0.1, -10),
            Vector3D.ZERO),
            new PVCoordinates(fv), 1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector2() throws PatriusException {
        final FieldVector3D<DerivativeStructure> fv =
            new PVCoordinates(new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(2);
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(2, fv.getX().getOrder());
        Assert.assertEquals(1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(10.0, fv.getZ().getReal(), 1.0e-10);
        Assert.assertEquals(-1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(-0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(-10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(10.0, fv.getX().getPartialDerivative(2), 1.0e-15);
        Assert.assertEquals(-1.0, fv.getY().getPartialDerivative(2), 1.0e-15);
        Assert.assertEquals(-100.0, fv.getZ().getPartialDerivative(2), 1.0e-15);
        this.checkPV(new PVCoordinates(new Vector3D(1, 0.1, 10),
            new Vector3D(-1, -0.1, -10),
            new Vector3D(10, -1.0, -100)),
            new PVCoordinates(fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            final Vector3D p = new PVCoordinates(new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assert.assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            Assert.assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            Assert.assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }
    }

    @Test
    public void testShift() {
        final Vector3D p1 = new Vector3D(1, 0.1, 10);
        final Vector3D p2 = new Vector3D(2, 0.2, 20);
        final Vector3D v = new Vector3D(-1, -0.1, -10);
        this.checkPV(new PVCoordinates(p2, v), new PVCoordinates(p1, v).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(0.0, PVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(v).getNorm(), 1.0e-15);
        // check with acceleration :
        final Vector3D p3 = new Vector3D(1., 2., 3.);
        final Vector3D v2 = new Vector3D(4., 5., 6.);
        final Vector3D a = new Vector3D(7., 8., 9.);
        final double dt = 0.1;
        final PVCoordinates pvaShifted = new PVCoordinates(p3, v2, a).shiftedBy(dt);
        final Vector3D posExp = p3.add(v2.scalarMultiply(dt)).add(a.scalarMultiply(0.5 * dt * dt));
        final Vector3D vExp = v2.add(a.scalarMultiply(dt));
        final PVCoordinates pvExp = new PVCoordinates(posExp, vExp, a);
        this.checkPV(pvaShifted, pvExp, 1e-15);
    }

    @Test
    public void testToArray() {
        final PVCoordinates pvWithAcc = new PVCoordinates(new Vector3D(4, 0.4, 40), new Vector3D(
                -4, -0.4,
                -40), new Vector3D(0.1, 0.2, 0.3));
        final PVCoordinates pvWithoutAcc = new PVCoordinates(new Vector3D(4, 0.4, 40),
                new Vector3D(-4, -0.4, -40));

        double[] array = pvWithAcc.toArray(true);

        // Acceleration should be included
        Assert.assertEquals(9, array.length);
        Assert.assertEquals(pvWithAcc.getPosition().getX(), array[0], 0.);
        Assert.assertEquals(pvWithAcc.getPosition().getY(), array[1], 0.);
        Assert.assertEquals(pvWithAcc.getPosition().getZ(), array[2], 0.);
        Assert.assertEquals(pvWithAcc.getVelocity().getX(), array[3], 0.);
        Assert.assertEquals(pvWithAcc.getVelocity().getY(), array[4], 0.);
        Assert.assertEquals(pvWithAcc.getVelocity().getZ(), array[5], 0.);
        Assert.assertEquals(pvWithAcc.getAcceleration().getX(), array[6], 0.);
        Assert.assertEquals(pvWithAcc.getAcceleration().getY(), array[7], 0.);
        Assert.assertEquals(pvWithAcc.getAcceleration().getZ(), array[8], 0.);

        // Try to extract acceleration beside the fact it isn't initialized (should fail)
        final String expectedMessage = "The acceleration is not initialized";
        try {
            pvWithoutAcc.toArray(true);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        array = pvWithAcc.toArray(false);

        // Acceleration shouldn't be included
        Assert.assertEquals(6, array.length);
        Assert.assertEquals(pvWithAcc.getPosition().getX(), array[0], 0.);
        Assert.assertEquals(pvWithAcc.getPosition().getY(), array[1], 0.);
        Assert.assertEquals(pvWithAcc.getPosition().getZ(), array[2], 0.);
        Assert.assertEquals(pvWithAcc.getVelocity().getX(), array[3], 0.);
        Assert.assertEquals(pvWithAcc.getVelocity().getY(), array[4], 0.);
        Assert.assertEquals(pvWithAcc.getVelocity().getZ(), array[5], 0.);

        // Acceleration shouldn't be included (without error)
        final double[] arrayBis = pvWithoutAcc.toArray(false);
        Assert.assertTrue(Arrays.equals(array, arrayBis));
    }

    @Test
    public void testToString() {
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(1, 0.1, 10), new Vector3D(-1, -0.1, -10));
        Assert.assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0)}", pv.toString());
    }

    @Test
    public void testGetMomentum() {
        // setup
        final Vector3D p = new Vector3D(1, -2, 3);
        final Vector3D v = new Vector3D(-9, 8, -7);

        // action + verify
        Assert.assertEquals(new PVCoordinates(p, v).getMomentum(), p.crossProduct(v));
        // check simple cases
        Assert.assertEquals(
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.MINUS_I).getMomentum(),
            Vector3D.ZERO);
        Assert.assertEquals(
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J).getMomentum(),
            Vector3D.PLUS_K);
    }

    @Test
    public void testGetAngularVelocity() {
        // setup
        final Vector3D p = new Vector3D(1, -2, 3);
        final Vector3D v = new Vector3D(-9, 8, -7);

        // action + verify
        Assert.assertEquals(
            new PVCoordinates(p, v).getAngularVelocity(),
            p.crossProduct(v).scalarMultiply(1.0 / p.getNormSq()));
        // check extra simple cases
        Assert.assertEquals(
            new PVCoordinates(Vector3D.PLUS_I, Vector3D.MINUS_I).getAngularVelocity(),
            Vector3D.ZERO);
        Assert.assertEquals(
            new PVCoordinates(new Vector3D(2, 0, 0), Vector3D.PLUS_J).getAngularVelocity(),
            Vector3D.PLUS_K.scalarMultiply(0.5));
    }

    @Test
    public void testNormalize() {
        final RandomGenerator generator = new Well19937a(0xb2011ffd25412067l);
        final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final PVCoordinates pv = this.randomPVCoordinates(generator, 1e6, 1e3, 1.0);
            final DerivativeStructure x =
                differentiator.differentiate(new UnivariateFunction(){
                    @Override
                    public double value(final double t) {
                        return pv.shiftedBy(t).getPosition().normalize().getX();
                    }
                }).value(new DerivativeStructure(1, 2, 0, 0.0));
            final DerivativeStructure y =
                differentiator.differentiate(new UnivariateFunction(){
                    @Override
                    public double value(final double t) {
                        return pv.shiftedBy(t).getPosition().normalize().getY();
                    }
                }).value(new DerivativeStructure(1, 2, 0, 0.0));
            final DerivativeStructure z =
                differentiator.differentiate(new UnivariateFunction(){
                    @Override
                    public double value(final double t) {
                        return pv.shiftedBy(t).getPosition().normalize().getZ();
                    }
                }).value(new DerivativeStructure(1, 2, 0, 0.0));
            final PVCoordinates normalized = pv.normalize();
            Assert.assertEquals(x.getValue(), normalized.getPosition().getX(), 1.0e-16);
            Assert.assertEquals(y.getValue(), normalized.getPosition().getY(), 1.0e-16);
            Assert.assertEquals(z.getValue(), normalized.getPosition().getZ(), 1.0e-16);
            Assert.assertEquals(x.getPartialDerivative(1), normalized.getVelocity().getX(), 3.0e-13);
            Assert.assertEquals(y.getPartialDerivative(1), normalized.getVelocity().getY(), 3.0e-13);
            Assert.assertEquals(z.getPartialDerivative(1), normalized.getVelocity().getZ(), 3.0e-13);
            Assert.assertEquals(x.getPartialDerivative(2), normalized.getAcceleration().getX(), 6.0e-10);
            Assert.assertEquals(y.getPartialDerivative(2), normalized.getAcceleration().getY(), 6.0e-10);
            Assert.assertEquals(z.getPartialDerivative(2), normalized.getAcceleration().getZ(), 6.0e-10);
        }
    }

    @Test
    public void testCrossProduct() {
        final RandomGenerator generator = new Well19937a(0x85c592b3be733d23l);
        final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0e-3);
        for (int i = 0; i < 200; ++i) {
            final PVCoordinates pv1 = this.randomPVCoordinates(generator, 1.0, 1.0, 1.0);
            final PVCoordinates pv2 = this.randomPVCoordinates(generator, 1.0, 1.0, 1.0);
            final DerivativeStructure x =
                differentiator.differentiate(new UnivariateFunction(){
                    @Override
                    public double value(final double t) {
                        return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                            pv2.shiftedBy(t).getPosition()).getX();
                    }
                }).value(new DerivativeStructure(1, 2, 0, 0.0));
            final DerivativeStructure y =
                differentiator.differentiate(new UnivariateFunction(){
                    @Override
                    public double value(final double t) {
                        return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                            pv2.shiftedBy(t).getPosition()).getY();
                    }
                }).value(new DerivativeStructure(1, 2, 0, 0.0));
            final DerivativeStructure z =
                differentiator.differentiate(new UnivariateFunction(){
                    @Override
                    public double value(final double t) {
                        return Vector3D.crossProduct(pv1.shiftedBy(t).getPosition(),
                            pv2.shiftedBy(t).getPosition()).getZ();
                    }
                }).value(new DerivativeStructure(1, 2, 0, 0.0));
            final PVCoordinates product = PVCoordinates.crossProduct(pv1, pv2);
            Assert.assertEquals(x.getValue(), product.getPosition().getX(), 1.0e-16);
            Assert.assertEquals(y.getValue(), product.getPosition().getY(), 1.0e-16);
            Assert.assertEquals(z.getValue(), product.getPosition().getZ(), 1.0e-16);
            Assert.assertEquals(x.getPartialDerivative(1), product.getVelocity().getX(), 9.0e-10);
            Assert.assertEquals(y.getPartialDerivative(1), product.getVelocity().getY(), 9.0e-10);
            Assert.assertEquals(z.getPartialDerivative(1), product.getVelocity().getZ(), 9.0e-10);
            Assert.assertEquals(x.getPartialDerivative(2), product.getAcceleration().getX(), 3.0e-9);
            Assert.assertEquals(y.getPartialDerivative(2), product.getAcceleration().getY(), 3.0e-9);
            Assert.assertEquals(z.getPartialDerivative(2), product.getAcceleration().getZ(), 3.0e-9);
        }
    }

    @Test
    public void testNegate() {
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(1, 0.1, 10), new Vector3D(-1, -0.1, -10), new Vector3D(1, 2, -3));
        final PVCoordinates pvNegate =
            new PVCoordinates(new Vector3D(-1, -0.1, -10), new Vector3D(1, 0.1, 10), new Vector3D(-1, -2, 3));
        this.checkPV(pv.negate(), pvNegate, 1.0e-15);
    }

    @Test
    public void testSerialization() {

        // Random test
        final RandomGenerator generator = new Well19937a(0x95b592c3be613d21l);
        for (int i = 0; i < 500; ++i) {
            // nextInt/Double exclusive of the top value, add one if needed
            PVCoordinates pvC1 = this.randomPVCoordinates(generator, 1.0, 1.0, 1.0);
            PVCoordinates pvC2 = TestUtils.serializeAndRecover(pvC1);
            assertEqualsPVCoordinates(pvC2, pvC1);

            pvC1 = this.randomPVCoordinates(generator, 1e6, 1e3, 1.0);
            pvC2 = TestUtils.serializeAndRecover(pvC1);
            assertEqualsPVCoordinates(pvC2, pvC1);
        }

        // Test constants
        final PVCoordinates pvC = TestUtils.serializeAndRecover(PVCoordinates.ZERO);
        assertEqualsPVCoordinates(PVCoordinates.ZERO, pvC);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link PVCoordinates#equals(Object)}
     * @testedMethod {@link PVCoordinates#hashCode()}
     * 
     * @description test equals() and hashcode() method for different case:
     * - params1 vs params1
     * - params1 vs params2 with same attributes
     * - params 1 vs params3 with different attributes
     * 
     * @input ReentryParameters
     * 
     * @output boolean and int
     * 
     * @testPassCriteria output is as expected
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testEquals() throws PatriusException {

        // Initialization
        final PVCoordinates params1 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final PVCoordinates params2 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final PVCoordinates params3 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_J);

        // Check
        Assert.assertTrue(params1.equals(params1));
        Assert.assertTrue(params1.equals(params2));
        Assert.assertFalse(params1.equals(params3));

        Assert.assertTrue(params1.hashCode() == params2.hashCode());
        Assert.assertFalse(params1.hashCode() == params3.hashCode());
    }

    public static void assertEqualsPVCoordinates(final PVCoordinates pvC1, final PVCoordinates pvC2) {
        Assert.assertEquals(pvC1.getPosition(), pvC2.getPosition());
        Assert.assertEquals(pvC1.getVelocity(), pvC2.getVelocity());
        Assert.assertEquals(pvC1.getAcceleration(), pvC2.getAcceleration());
    }

    private Vector3D randomVector(final RandomGenerator random, final double norm) {
        final double n = random.nextDouble() * norm;
        final double x = random.nextDouble();
        final double y = random.nextDouble();
        final double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private PVCoordinates randomPVCoordinates(final RandomGenerator random,
                                              final double norm0, final double norm1, final double norm2) {
        final Vector3D p0 = this.randomVector(random, norm0);
        final Vector3D p1 = this.randomVector(random, norm1);
        final Vector3D p2 = this.randomVector(random, norm2);
        return new PVCoordinates(p0, p1, p2);
    }

    private void checkPV(final PVCoordinates expected, final PVCoordinates real, final double epsilon) {
        Assert.assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        Assert.assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
        if (expected.getAcceleration() == null) {
            Assert.assertNull(real.getAcceleration());
        } else {
            Assert.assertEquals(expected.getAcceleration().getX(), real.getAcceleration().getX(), epsilon);
            Assert.assertEquals(expected.getAcceleration().getY(), real.getAcceleration().getY(), epsilon);
            Assert.assertEquals(expected.getAcceleration().getZ(), real.getAcceleration().getZ(), epsilon);
        }
    }

}
