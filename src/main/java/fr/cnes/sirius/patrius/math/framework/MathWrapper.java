/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.framework;

import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * Math wrapper. This class encapsulates {@link Math} methods.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.3
 */
public class MathWrapper implements MathLibrary {

    /** {@inheritDoc} */
    @Override
    public double sqrt(final double a) {
        return Math.sqrt(a);
    }

    /** {@inheritDoc} */
    @Override
    public double cosh(final double x) {
        return Math.cosh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double sinh(final double x) {
        return Math.sinh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tanh(final double x) {
        return Math.tanh(x);
    }

    /**
     * {@inheritDoc}.
     * This function is not available in Math library, hence FastMath implementation is used.
     */
    @Override
    public double acosh(final double a) {
        return FastMath.acosh(a);
    }

    /**
     * {@inheritDoc}.
     * This function is not available in Math library, hence FastMath implementation is used.
     */
    @Override
    public double asinh(final double a) {
        return FastMath.asinh(a);
    }

    /**
     * {@inheritDoc}.
     * This function is not available in Math library, hence FastMath implementation is used.
     */
    @Override
    public double atanh(final double a) {
        return FastMath.atanh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double signum(final double a) {
        return Math.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public float signum(final float a) {
        return Math.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public double nextUp(final double a) {
        return Math.nextUp(a);
    }

    /** {@inheritDoc} */
    @Override
    public float nextUp(final float a) {
        return Math.nextUp(a);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Note:</b> this implementation currently delegates to {@link Math#random()}.
     */
    @Override
    public double random() {
        return Math.random();
    }

    /** {@inheritDoc} */
    @Override
    public double exp(final double x) {
        return Math.exp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double expm1(final double x) {
        return Math.expm1(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log(final double x) {
        return Math.log(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log1p(final double x) {
        return Math.log1p(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log10(final double x) {
        return Math.log10(x);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double x, final double y) {
        return Math.pow(x, y);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double d, final int e) {
        final double res;
        if (Double.isInfinite(d) && e > 0) {
            // JAFAMA result
            // Math returns NaN which is not appropriate since value exist
            res = d;
        } else {
            res = Math.pow(d, e);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double sin(final double x) {
        return Math.sin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cos(final double x) {
        return Math.cos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tan(final double x) {
        return Math.tan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan(final double x) {
        return Math.atan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan2(final double y, final double x) {
        return Math.atan2(y, x);
    }

    /** {@inheritDoc} */
    @Override
    public double asin(final double x) {
        return Math.asin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double acos(final double x) {
        return Math.acos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cbrt(final double x) {
        return Math.cbrt(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toRadians(final double x) {
        return Math.toRadians(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toDegrees(final double x) {
        return Math.toDegrees(x);
    }

    /** {@inheritDoc} */
    @Override
    public int abs(final int x) {
        return Math.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public long abs(final long x) {
        return Math.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public float abs(final float x) {
        return Math.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double abs(final double x) {
        return Math.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double ulp(final double x) {
        return Math.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public float ulp(final float x) {
        return Math.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double scalb(final double d, final int n) {
        return Math.scalb(d, n);
    }

    /** {@inheritDoc} */
    @Override
    public float scalb(final float f, final int n) {
        return Math.scalb(f, n);
    }

    /** {@inheritDoc} */
    @Override
    public double nextAfter(final double d, final double direction) {
        return Math.nextAfter(d, direction);
    }

    /** {@inheritDoc} */
    @Override
    public float nextAfter(final float f, final double direction) {
        return Math.nextAfter(f, direction);
    }

    /** {@inheritDoc} */
    @Override
    public double floor(final double x) {
        return Math.floor(x);
    }

    /** {@inheritDoc} */
    @Override
    public double ceil(final double x) {
        return Math.ceil(x);
    }

    /** {@inheritDoc} */
    @Override
    public double rint(final double x) {
        return Math.rint(x);
    }

    /** {@inheritDoc} */
    @Override
    public long round(final double x) {
        return Math.round(x);
    }

    /** {@inheritDoc} */
    @Override
    public int round(final float x) {
        return Math.round(x);
    }

    /** {@inheritDoc} */
    @Override
    public int min(final int a, final int b) {
        return Math.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long min(final long a, final long b) {
        return Math.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float min(final float a, final float b) {
        return Math.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double min(final double a, final double b) {
        return Math.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public int max(final int a, final int b) {
        return Math.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long max(final long a, final long b) {
        return Math.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float max(final float a, final float b) {
        return Math.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double max(final double a, final double b) {
        return Math.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double hypot(final double x, final double y) {
        return Math.hypot(x, y);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodName
    // Reason: name kept as such
    @Override
    public double IEEEremainder(final double dividend, final double divisor) {
        return Math.IEEEremainder(dividend, divisor);
    }

    /** {@inheritDoc} */
    @Override
    public double copySign(final double magnitude, final double sign) {
        return Math.copySign(magnitude, sign);
    }

    /** {@inheritDoc} */
    @Override
    public float copySign(final float magnitude, final float sign) {
        return Math.copySign(magnitude, sign);
    }

    /** {@inheritDoc} */
    @Override
    public int getExponent(final double d) {
        return Math.getExponent(d);
    }

    /** {@inheritDoc} */
    @Override
    public int getExponent(final float f) {
        return Math.getExponent(f);
    }

    /** {@inheritDoc} */
    @Override
    public double[] sinAndCos(final double x) {
        return new double[] { Math.sin(x), Math.cos(x) };
    }

    /** {@inheritDoc} */
    @Override
    public void sinAndCos(final double x, final double[] sincos) {
        sincos[0] = Math.sin(x);
        sincos[1] = Math.cos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double[] sinhAndCosh(final double x) {
        return new double[] { Math.sinh(x), Math.cosh(x) };
    }

    /** {@inheritDoc} */
    @Override
    public void sinhAndCosh(final double x, final double[] sinhcosh) {
        sinhcosh[0] = Math.sinh(x);
        sinhcosh[1] = Math.cosh(x);
    }
}
