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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.FrameAncestorException;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class UpdatableFrameTest {

    @Test
    public void testUpdateTransform() throws PatriusException {
        final Random random = new Random(0x2f6769c23e53e96el);
        final UpdatableFrame f0 = new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "dummy");
        final AbsoluteDate date = new AbsoluteDate();

        final UpdatableFrame f1 = new UpdatableFrame(f0, randomTransform(random), "f1");
        final UpdatableFrame f2 = new UpdatableFrame(f1, randomTransform(random), "f2");
        final UpdatableFrame f3 = new UpdatableFrame(f2, randomTransform(random), "f3");
        final UpdatableFrame f4 = new UpdatableFrame(f2, randomTransform(random), "f4");
        final UpdatableFrame f5 = new UpdatableFrame(f4, randomTransform(random), "f5");
        final UpdatableFrame f6 = new UpdatableFrame(f0, randomTransform(random), "f6");
        final UpdatableFrame f7 = new UpdatableFrame(f6, randomTransform(random), "f7");
        final UpdatableFrame f8 = new UpdatableFrame(f6, randomTransform(random), "f8");
        final UpdatableFrame f9 = new UpdatableFrame(f7, randomTransform(random), "f9");

        checkFrameAncestorException(f6, f8, f9, randomTransform(random), date);
        checkFrameAncestorException(f6, f9, f8, randomTransform(random), date);
        checkFrameAncestorException(f6, f3, f5, randomTransform(random), date);
        checkFrameAncestorException(f6, f5, f3, randomTransform(random), date);
        checkFrameAncestorException(f0, f5, f9, randomTransform(random), date);
        checkFrameAncestorException(f0, f9, f5, randomTransform(random), date);
        checkFrameAncestorException(f3, f0, f6, randomTransform(random), date);
        checkFrameAncestorException(f3, f6, f0, randomTransform(random), date);

        checkUpdateTransform(f1, f5, f9, date, random);
        checkUpdateTransform(f7, f6, f9, date, random);
        checkUpdateTransform(f6, f0, f7, date, random);

        checkUpdateTransform(f6, f6.getParent(), f6, date, random);

    }

    private static void checkFrameAncestorException(final UpdatableFrame f0, final Frame f1,
            final Frame f2,
                                             final Transform transform, final AbsoluteDate date) {
        try {
            f0.updateTransform(f1, f2, transform, date);
            Assert.fail("Should raise a FrameAncestorException");
        } catch (final FrameAncestorException expected) {
            // expected behavior
        } catch (final Exception e) {
            Assert.fail("wrong exception caught");
        }
    }

    private static void checkUpdateTransform(final UpdatableFrame f0, final Frame f1,
            final Frame f2,
                                      final AbsoluteDate date, final Random random)
                                                                                   throws PatriusException {
        final Transform f1ToF2 = randomTransform(random);

        f0.updateTransform(f1, f2, f1ToF2, date);
        final Transform obtained12 = f1.getTransformTo(f2, date);
        checkNoTransform(new Transform(date, f1ToF2, obtained12.getInverse()), random);

        f0.updateTransform(f2, f1, f1ToF2.getInverse(), date);
        final Transform obtained21 = f2.getTransformTo(f1, date);
        checkNoTransform(new Transform(date, f1ToF2.getInverse(), obtained21.getInverse()), random);

        checkNoTransform(new Transform(date, obtained12, obtained21), random);
    }

    private static Transform randomTransform(final Random random) {
        Transform transform = Transform.IDENTITY;
        for (int i = random.nextInt(10); i > 0; --i) {
            if (random.nextBoolean()) {
                final Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                    random.nextDouble() * 1000.0,
                    random.nextDouble() * 1000.0);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), u));
            } else {
                final double q0 = random.nextDouble() * 2 - 1;
                final double q1 = random.nextDouble() * 2 - 1;
                final double q2 = random.nextDouble() * 2 - 1;
                final double q3 = random.nextDouble() * 2 - 1;
                final double q = MathLib.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
                final Rotation r = new Rotation(false, q0 / q, q1 / q, q2 / q, q3 / q);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), r));
            }
        }
        return transform;
    }

    private static void checkNoTransform(final Transform transform, final Random random) {
        for (int i = 0; i < 100; ++i) {
            final Vector3D a = new Vector3D(random.nextDouble(),
                random.nextDouble(),
                random.nextDouble());
            final Vector3D b = transform.transformVector(a);
            Assert.assertEquals(0, a.subtract(b).getNorm(), 1.0e-10);
            final Vector3D c = transform.transformPosition(a);
            Assert.assertEquals(0, a.subtract(c).getNorm(), 1.0e-10);
        }
    }

    @Test
    public void seriaTest() {

        final Random random = new Random(0x2f6769c23e53e96el);
        final UpdatableFrame f0 = new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "dummy");

        final UpdatableFrame f1 = new UpdatableFrame(f0, randomTransform(random), "f1");
        final UpdatableFrame f2 = new UpdatableFrame(f1, randomTransform(random), "f2");
        final UpdatableFrame f3 = new UpdatableFrame(f2, randomTransform(random), "f3");
        final UpdatableFrame f4 = new UpdatableFrame(f2, randomTransform(random), "f4");
        final UpdatableFrame f5 = new UpdatableFrame(f4, randomTransform(random), "f5");
        final UpdatableFrame f6 = new UpdatableFrame(f0, randomTransform(random), "f6");
        final UpdatableFrame f7 = new UpdatableFrame(f6, randomTransform(random), "f7");
        final UpdatableFrame f8 = new UpdatableFrame(f6, randomTransform(random), "f8");
        final UpdatableFrame f9 = new UpdatableFrame(f7, randomTransform(random), "f9");

        final Frame[] frames = { f0, f1, f2, f3, f4, f5, f6, f7, f8, f9 };
        for (final Frame f : frames) {
            final Frame frameRecover = TestUtils.serializeAndRecover(f);
            FrameTest.frameEq(f, frameRecover);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }
}
