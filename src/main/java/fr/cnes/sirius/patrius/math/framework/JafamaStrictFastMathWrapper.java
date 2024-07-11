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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.framework;

import fr.cnes.sirius.patrius.math.util.JafamaFastMath;
import net.jafama.StrictFastMath;

/**
 * JAFAMA StrictFastMath wrapper. This class encapsulates {@link StrictFastMath} methods.
 * <p>
 * Note that only common methods with {@link fr.cnes.sirius.patrius.math.util.FastMath} are encapsulated.
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.2
 */
public class JafamaStrictFastMathWrapper implements MathLibrary {

    /** {@inheritDoc} */
    @Override
    public double sqrt(final double a) {
        return StrictFastMath.sqrt(a);
    }

    /** {@inheritDoc} */
    @Override
    public double cosh(final double x) {
        return StrictFastMath.cosh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double sinh(final double x) {
        return StrictFastMath.sinh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tanh(final double x) {
        return StrictFastMath.tanh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double acosh(final double a) {
        return StrictFastMath.acosh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double asinh(final double a) {
        return StrictFastMath.asinh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double atanh(final double a) {
        return StrictFastMath.atanh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double signum(final double a) {
        return StrictFastMath.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public float signum(final float a) {
        return StrictFastMath.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public double nextUp(final double a) {
        return StrictFastMath.nextUp(a);
    }

    /** {@inheritDoc} */
    @Override
    public float nextUp(final float a) {
        return StrictFastMath.nextUp(a);
    }

    /** {@inheritDoc} */
    @Override
    public double random() {
        return StrictFastMath.random();
    }

    /** {@inheritDoc} */
    @Override
    public double exp(final double x) {
        return StrictFastMath.exp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double expm1(final double x) {
        return StrictFastMath.expm1(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log(final double x) {
        return StrictFastMath.log(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log1p(final double x) {
        return StrictFastMath.log1p(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log10(final double x) {
        return StrictFastMath.log10(x);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double x, final double y) {
        return StrictFastMath.pow(x, y);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double d, final int e) {
        return StrictFastMath.pow(d, e);
    }

    /** {@inheritDoc} */
    @Override
    public double sin(final double x) {
        return StrictFastMath.sin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cos(final double x) {
        return StrictFastMath.cos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tan(final double x) {
        return StrictFastMath.tan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan(final double x) {
        return StrictFastMath.atan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan2(final double y, final double x) {
        return StrictFastMath.atan2(y, x);
    }

    /** {@inheritDoc} */
    @Override
    public double asin(final double x) {
        return StrictFastMath.asin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double acos(final double x) {
        return StrictFastMath.acos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cbrt(final double x) {
        return StrictFastMath.cbrt(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toRadians(final double x) {
        return StrictFastMath.toRadians(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toDegrees(final double x) {
        return StrictFastMath.toDegrees(x);
    }

    /** {@inheritDoc} */
    @Override
    public int abs(final int x) {
        return StrictFastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public long abs(final long x) {
        return StrictFastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public float abs(final float x) {
        return StrictFastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double abs(final double x) {
        return StrictFastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double ulp(final double x) {
        return StrictFastMath.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public float ulp(final float x) {
        return StrictFastMath.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double scalb(final double d, final int n) {
        return StrictFastMath.scalb(d, n);
    }

    /** {@inheritDoc} */
    @Override
    public float scalb(final float f, final int n) {
        return StrictFastMath.scalb(f, n);
    }

    /** {@inheritDoc} */
    @Override
    public double nextAfter(final double d, final double direction) {
        return StrictFastMath.nextAfter(d, direction);
    }

    /** {@inheritDoc} */
    @Override
    public float nextAfter(final float f, final double direction) {
        return StrictFastMath.nextAfter(f, direction);
    }

    /** {@inheritDoc} */
    @Override
    public double floor(final double x) {
        return StrictFastMath.floor(x);
    }

    /** {@inheritDoc} */
    @Override
    public double ceil(final double x) {
        return StrictFastMath.ceil(x);
    }

    /** {@inheritDoc} */
    @Override
    public double rint(final double x) {
        return StrictFastMath.rint(x);
    }

    /** {@inheritDoc} */
    @Override
    public long round(final double x) {
        return StrictFastMath.round(x);
    }

    /** {@inheritDoc} */
    @Override
    public int round(final float x) {
        return StrictFastMath.round(x);
    }

    /** {@inheritDoc} */
    @Override
    public int min(final int a, final int b) {
        return StrictFastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long min(final long a, final long b) {
        return StrictFastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float min(final float a, final float b) {
        return StrictFastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double min(final double a, final double b) {
        return StrictFastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public int max(final int a, final int b) {
        return StrictFastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long max(final long a, final long b) {
        return StrictFastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float max(final float a, final float b) {
        return StrictFastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double max(final double a, final double b) {
        return StrictFastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double hypot(final double x, final double y) {
        return StrictFastMath.hypot(x, y);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodName
    // Reason: name kept as such
    @Override
    public double IEEEremainder(final double dividend, final double divisor) {
        return StrictFastMath.IEEEremainder(dividend, divisor);
    }

    /** {@inheritDoc} */
    @Override
    public double copySign(final double magnitude, final double sign) {
        return StrictFastMath.copySign(magnitude, sign);
    }

    /** {@inheritDoc} */
    @Override
    public float copySign(final float magnitude, final float sign) {
        return StrictFastMath.copySign(magnitude, sign);
    }

    /** {@inheritDoc} */
    @Override
    public int getExponent(final double d) {
        return StrictFastMath.getExponent(d);
    }

    /** {@inheritDoc} */
    @Override
    public int getExponent(final float f) {
        return StrictFastMath.getExponent(f);
    }

    /** {@inheritDoc} */
    @Override
    public double[] sinAndCos(final double x) {
        final double[] sincos = { 0., 0.};
        sinAndCos(x, sincos);
        return sincos;
    }

    /** {@inheritDoc} */
    @Override
    public void sinAndCos(final double x, final double[] sincos) {
        // Deported in another class for sake of clarity
        // This class if 850 lines long
        JafamaFastMath.sinAndCos(x, sincos);
    }

    /** {@inheritDoc} */
    @Override
    public double[] sinhAndCosh(final double x) {
        final double[] sinhcosh = { 0., 0.};
        sinhAndCosh(x, sinhcosh);
        return sinhcosh;
    }

    /** {@inheritDoc} */
    @Override
    public void sinhAndCosh(final double x, final double[] sinhcosh) {
        // Deported in another class for sake of clarity
        // This class if more than 800 lines long
        JafamaFastMath.sinhAndCosh(x, sinhcosh);
    }
}
