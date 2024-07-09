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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:287:21/10/2014:Bug in frame transformation when changing order of two following transformation
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class InterpolatingTransformProviderTest {

    @Test
    public void testCacheHitWithDerivatives() throws PatriusException {

        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final CirclingProvider referenceProvider = new CirclingProvider(t0, 0.2);
        final CirclingProvider rawProvider = new CirclingProvider(t0, 0.2);
        final InterpolatingTransformProvider interpolatingProvider =
            new InterpolatingTransformProvider(rawProvider, true, true,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                5, 0.8, 10, 60.0, 60.0, true);

        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            final Transform reference = referenceProvider.getTransform(t0.shiftedBy(dt));
            final Transform interpolated = interpolatingProvider.getTransform(t0.shiftedBy(dt), false);
            final Transform error = new Transform(reference.getDate(), reference, interpolated.getInverse());
            Assert.assertEquals(0.0, error.getCartesian().getPosition().getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(), 3.0e-14);
            Assert.assertEquals(0.0, error.getAngular().getRotation().getAngle(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

        }
        Assert.assertEquals(10, rawProvider.getCount());
        Assert.assertEquals(3001, referenceProvider.getCount());

    }

    @Test
    public void testCacheHitWithoutDerivatives() throws PatriusException {

        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final CirclingProvider referenceProvider = new CirclingProvider(t0, 0.2);
        final CirclingProvider rawProvider = new CirclingProvider(t0, 0.2);
        final InterpolatingTransformProvider interpolatingProvider =
            new InterpolatingTransformProvider(rawProvider, false, false,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                5, 0.8, 10, 60.0, 60.0);

        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            final Transform reference = referenceProvider.getTransform(t0.shiftedBy(dt));
            final Transform interpolated = interpolatingProvider.getTransform(t0.shiftedBy(dt));
            final Transform error = new Transform(reference.getDate(), reference, interpolated.getInverse());
            Assert.assertEquals(0.0, error.getCartesian().getPosition().getNorm(), 1.3e-6);
            Assert.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(), 7.0e-6);
            Assert.assertEquals(0.0, error.getAngular().getRotation().getAngle(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

        }
        Assert.assertEquals(10, rawProvider.getCount());
        Assert.assertEquals(3001, referenceProvider.getCount());

    }

    @Test(expected = PatriusException.class)
    public void testForwardException() throws PatriusException {
        final InterpolatingTransformProvider interpolatingProvider =
            new InterpolatingTransformProvider(new TransformProvider(){
                private static final long serialVersionUID = -3126512810306982868L;

                @Override
                public Transform getTransform(final AbsoluteDate date) throws PatriusException {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }

                @Override
                public Transform
                        getTransform(final AbsoluteDate date, final FramesConfiguration config)
                                                                                               throws PatriusException {
                    return this.getTransform(date);
                }

                @Override
                public
                        Transform
                        getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives)
                                                                                                   throws PatriusException {
                    return this.getTransform(date);
                }

                @Override
                public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                              final boolean computeSpinDerivatives) throws PatriusException {
                    return this.getTransform(date);
                }
            }, true, true,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                5, 0.8, 10, 60.0, 60.0);
        interpolatingProvider.getTransform(AbsoluteDate.J2000_EPOCH);
    }

    @Test
    public void testSerialization() throws PatriusException, IOException, ClassNotFoundException {

        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final CirclingProvider rawProvider = new CirclingProvider(t0, 0.2);
        final InterpolatingTransformProvider interpolatingProvider =
            new InterpolatingTransformProvider(rawProvider, true, true,
                AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                5, 0.8, 10, 60.0, 60.0);

        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            interpolatingProvider.getTransform(t0.shiftedBy(dt));
        }
        Assert.assertEquals(10, rawProvider.getCount());

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(interpolatingProvider);

        Assert.assertTrue(bos.size() > 500);
        Assert.assertTrue(bos.size() < 700);

        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final InterpolatingTransformProvider deserialized =
            (InterpolatingTransformProvider) ois.readObject();
        Assert.assertEquals(0, ((CirclingProvider) deserialized.getRawProvider()).getCount());
        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            final Transform t1 = interpolatingProvider.getTransform(t0.shiftedBy(dt));
            final Transform t2 = deserialized.getTransform(t0.shiftedBy(dt));
            final Transform error = new Transform(t1.getDate(), t1, t2.getInverse());
            // both interpolators should give the same results
            Assert.assertEquals(0.0, error.getCartesian().getPosition().getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.0, error.getAngular().getRotation().getAngle(), Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);
        }

        // the original interpolator should not have triggered any new calls
        Assert.assertEquals(10, rawProvider.getCount());

        // the deserialized interpolator should have triggered new calls
        Assert.assertEquals(10, ((CirclingProvider) deserialized.getRawProvider()).getCount());

    }

    private static class CirclingProvider implements TransformProvider {

        private static final long serialVersionUID = 473784183299281612L;
        private int count;
        private final AbsoluteDate t0;
        private final double omega;

        public CirclingProvider(final AbsoluteDate t0, final double omega) {
            this.count = 0;
            this.t0 = t0;
            this.omega = omega;
        }

        @Override
        public Transform getTransform(final AbsoluteDate date) {
            // the following transform corresponds to a frame moving along the circle r = 1
            // with its x axis always pointing to the reference frame center
            ++this.count;
            final double dt = date.durationFrom(this.t0);
            final double cos = MathLib.cos(this.omega * dt);
            final double sin = MathLib.sin(this.omega * dt);
            return new Transform(date,
                new Transform(date, new Vector3D(-cos, -sin, 0), new Vector3D(this.omega * sin, -this.omega * cos, 0)),
                new Transform(date,
                    new Rotation(Vector3D.PLUS_K, -FastMath.PI + this.omega * dt),
                    new Vector3D(this.omega, Vector3D.PLUS_K)));
        }

        public int getCount() {
            return this.count;
        }

        private Object readResolve() {
            this.count = 0;
            return this;
        }

        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config)
                                                                                                throws PatriusException {
            return this.getTransform(date);
        }

        @Override
        public Transform
                getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives) throws PatriusException {
            return this.getTransform(date);
        }

        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                      final boolean computeSpinDerivatives)
                                                                           throws PatriusException {
            return this.getTransform(date);
        }
    }

}
