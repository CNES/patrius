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
 * StrictMath wrapper. This class encapsulates {@link StrictMath} methods.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.3
 */
public class StrictMathWrapper implements MathLibrary {

    /** {@inheritDoc} */
    @Override
    public double sqrt(final double a) {
        return StrictMath.sqrt(a);
    }

    /** {@inheritDoc} */
    @Override
    public double cosh(final double x) {
        return StrictMath.cosh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double sinh(final double x) {
        return StrictMath.sinh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tanh(final double x) {
        return StrictMath.tanh(x);
    }

    /**
     * {@inheritDoc}.
     * This function is not available in StrictMath library, hence FastMath implementation is used.
     */
    @Override
    public double acosh(final double a) {
        return FastMath.acosh(a);
    }

    /**
     * {@inheritDoc}.
     * This function is not available in StrictMath library, hence FastMath implementation is used.
     */
    @Override
    public double asinh(final double a) {
        return FastMath.asinh(a);
    }

    /**
     * {@inheritDoc}.
     * This function is not available in StrictMath library, hence FastMath implementation is used.
     */
    @Override
    public double atanh(final double a) {
        return FastMath.atanh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double signum(final double a) {
        return StrictMath.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public float signum(final float a) {
        return StrictMath.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public double nextUp(final double a) {
        return StrictMath.nextUp(a);
    }

    /** {@inheritDoc} */
    @Override
    public float nextUp(final float a) {
        return StrictMath.nextUp(a);
    }

    /** {@inheritDoc} */
    @Override
    public double random() {
        return StrictMath.random();
    }

    /** {@inheritDoc} */
    @Override
    public double exp(final double x) {
        return StrictMath.exp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double expm1(final double x) {
        return StrictMath.expm1(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log(final double x) {
        return StrictMath.log(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log1p(final double x) {
        return StrictMath.log1p(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log10(final double x) {
        return StrictMath.log10(x);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double x, final double y) {
        return StrictMath.pow(x, y);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double d, final int e) {
        final double res;
        if (Double.isInfinite(d) && e > 0) {
            // JAFAMA result
            // StrictMath returns NaN which is not appropriate since value exist
            res = d;
        } else {
            res = StrictMath.pow(d, e);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double sin(final double x) {
        return StrictMath.sin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cos(final double x) {
        return StrictMath.cos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tan(final double x) {
        return StrictMath.tan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan(final double x) {
        return StrictMath.atan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan2(final double y, final double x) {
        return StrictMath.atan2(y, x);
    }

    /** {@inheritDoc} */
    @Override
    public double asin(final double x) {
        return StrictMath.asin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double acos(final double x) {
        return StrictMath.acos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cbrt(final double x) {
        return StrictMath.cbrt(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toRadians(final double x) {
        return StrictMath.toRadians(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toDegrees(final double x) {
        return StrictMath.toDegrees(x);
    }

    /** {@inheritDoc} */
    @Override
    public int abs(final int x) {
        return StrictMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public long abs(final long x) {
        return StrictMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public float abs(final float x) {
        return StrictMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double abs(final double x) {
        return StrictMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double ulp(final double x) {
        return StrictMath.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public float ulp(final float x) {
        return StrictMath.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double scalb(final double d, final int n) {
        return StrictMath.scalb(d, n);
    }

    /** {@inheritDoc} */
    @Override
    public float scalb(final float f, final int n) {
        return StrictMath.scalb(f, n);
    }

    /** {@inheritDoc} */
    @Override
    public double nextAfter(final double d, final double direction) {
        return StrictMath.nextAfter(d, direction);
    }

    /** {@inheritDoc} */
    @Override
    public float nextAfter(final float f, final double direction) {
        return StrictMath.nextAfter(f, direction);
    }

    /** {@inheritDoc} */
    @Override
    public double floor(final double x) {
        return StrictMath.floor(x);
    }

    /** {@inheritDoc} */
    @Override
    public double ceil(final double x) {
        return StrictMath.ceil(x);
    }

    /** {@inheritDoc} */
    @Override
    public double rint(final double x) {
        return StrictMath.rint(x);
    }

    /** {@inheritDoc} */
    @Override
    public long round(final double x) {
        return StrictMath.round(x);
    }

    /** {@inheritDoc} */
    @Override
    public int round(final float x) {
        return StrictMath.round(x);
    }

    /** {@inheritDoc} */
    @Override
    public int min(final int a, final int b) {
        return StrictMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long min(final long a, final long b) {
        return StrictMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float min(final float a, final float b) {
        return StrictMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double min(final double a, final double b) {
        return StrictMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public int max(final int a, final int b) {
        return StrictMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long max(final long a, final long b) {
        return StrictMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float max(final float a, final float b) {
        return StrictMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double max(final double a, final double b) {
        return StrictMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double hypot(final double x, final double y) {
        return StrictMath.hypot(x, y);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodName
    // Reason: name kept as such
    @Override
    public double IEEEremainder(final double dividend, final double divisor) {
        return StrictMath.IEEEremainder(dividend, divisor);
    }

    /** {@inheritDoc} */
    @Override
    public double copySign(final double magnitude, final double sign) {
        return StrictMath.copySign(magnitude, sign);
    }

    /** {@inheritDoc} */
    @Override
    public float copySign(final float magnitude, final float sign) {
        return StrictMath.copySign(magnitude, sign);
    }

    /** {@inheritDoc} */
    @Override
    public int getExponent(final double d) {
        return StrictMath.getExponent(d);
    }

    /** {@inheritDoc} */
    @Override
    public int getExponent(final float f) {
        return StrictMath.getExponent(f);
    }

    /** {@inheritDoc} */
    @Override
    public double[] sinAndCos(final double x) {
        return new double[] { StrictMath.sin(x), StrictMath.cos(x) };
    }

    /** {@inheritDoc} */
    @Override
    public void sinAndCos(final double x, final double[] sincos) {
        sincos[0] = StrictMath.sin(x);
        sincos[1] = StrictMath.cos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double[] sinhAndCosh(final double x) {
        return new double[] { StrictMath.sinh(x), StrictMath.cosh(x) };
    }

    /** {@inheritDoc} */
    @Override
    public void sinhAndCosh(final double x, final double[] sinhcosh) {
        sinhcosh[0] = StrictMath.sinh(x);
        sinhcosh[1] = StrictMath.cosh(x);
    }
}
