/**
 * 
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
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.framework;

import net.jafama.StrictFastMath;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.JafamaFastMath;

/**
 * This implementation encapsulates for each math method the fastest methods among:
 * <ul>
 * <li>{@link FastMath}</li>
 * <li>{@link net.jafama.FastMath}</li>
 * <li>{@link StrictFastMath}</li>
 * </ul>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.2
 */
public class FastestMathLibWrapper implements MathLibrary {

    /** {@inheritDoc} */
    @Override
    public double sqrt(final double a) {
        return net.jafama.FastMath.sqrt(a);
    }

    /** {@inheritDoc} */
    @Override
    public double cosh(final double x) {
        return net.jafama.FastMath.cosh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double sinh(final double x) {
        return net.jafama.FastMath.sinh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tanh(final double x) {
        return net.jafama.FastMath.tanh(x);
    }

    /** {@inheritDoc} */
    @Override
    public double acosh(final double a) {
        return net.jafama.StrictFastMath.acosh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double asinh(final double a) {
        return net.jafama.StrictFastMath.asinh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double atanh(final double a) {
        return net.jafama.FastMath.atanh(a);
    }

    /** {@inheritDoc} */
    @Override
    public double signum(final double a) {
        return FastMath.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public float signum(final float a) {
        return FastMath.signum(a);
    }

    /** {@inheritDoc} */
    @Override
    public double nextUp(final double a) {
        return net.jafama.FastMath.nextUp(a);
    }

    /** {@inheritDoc} */
    @Override
    public float nextUp(final float a) {
        return net.jafama.FastMath.nextUp(a);
    }

    /** {@inheritDoc} */
    @Override
    public double random() {
        return net.jafama.FastMath.random();
    }

    /** {@inheritDoc} */
    @Override
    public double exp(final double x) {
        return net.jafama.FastMath.exp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double expm1(final double x) {
        return net.jafama.FastMath.expm1(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log(final double x) {
        return net.jafama.StrictFastMath.log(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log1p(final double x) {
        return net.jafama.FastMath.log1p(x);
    }

    /** {@inheritDoc} */
    @Override
    public double log10(final double x) {
        return net.jafama.StrictFastMath.log10(x);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double x, final double y) {
        return net.jafama.FastMath.pow(x, y);
    }

    /** {@inheritDoc} */
    @Override
    public double pow(final double d, final int e) {
        final double res;
        if (Double.isInfinite(d) && e > 0) {
            // JAFAMA result
            // net.jafama.FastMath returns NaN which is not appropriate since value exist
            res = d;
        } else {
            res = FastMath.pow(d, e);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double sin(final double x) {
        return net.jafama.FastMath.sin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cos(final double x) {
        return net.jafama.FastMath.cos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double tan(final double x) {
        return net.jafama.FastMath.tan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan(final double x) {
        return net.jafama.FastMath.atan(x);
    }

    /** {@inheritDoc} */
    @Override
    public double atan2(final double y, final double x) {
        return net.jafama.FastMath.atan2(y, x);
    }

    /** {@inheritDoc} */
    @Override
    public double asin(final double x) {
        return net.jafama.FastMath.asin(x);
    }

    /** {@inheritDoc} */
    @Override
    public double acos(final double x) {
        return net.jafama.FastMath.acos(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cbrt(final double x) {
        return net.jafama.FastMath.cbrt(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toRadians(final double x) {
        return net.jafama.FastMath.toRadians(x);
    }

    /** {@inheritDoc} */
    @Override
    public double toDegrees(final double x) {
        return net.jafama.FastMath.toDegrees(x);
    }

    /** {@inheritDoc} */
    @Override
    public int abs(final int x) {
        return FastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public long abs(final long x) {
        return FastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public float abs(final float x) {
        return FastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double abs(final double x) {
        return FastMath.abs(x);
    }

    /** {@inheritDoc} */
    @Override
    public double ulp(final double x) {
        return net.jafama.FastMath.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public float ulp(final float x) {
        return net.jafama.FastMath.ulp(x);
    }

    /** {@inheritDoc} */
    @Override
    public double scalb(final double d, final int n) {
        return net.jafama.FastMath.scalb(d, n);
    }

    /** {@inheritDoc} */
    @Override
    public float scalb(final float f, final int n) {
        return net.jafama.FastMath.scalb(f, n);
    }

    /** {@inheritDoc} */
    @Override
    public double nextAfter(final double d, final double direction) {
        return net.jafama.FastMath.nextAfter(d, direction);
    }

    /** {@inheritDoc} */
    @Override
    public float nextAfter(final float f, final double direction) {
        return net.jafama.FastMath.nextAfter(f, direction);
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
        return FastMath.rint(x);
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
        return FastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long min(final long a, final long b) {
        return net.jafama.FastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float min(final float a, final float b) {
        return net.jafama.FastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double min(final double a, final double b) {
        return net.jafama.FastMath.min(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public int max(final int a, final int b) {
        return FastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public long max(final long a, final long b) {
        return net.jafama.FastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public float max(final float a, final float b) {
        return net.jafama.FastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double max(final double a, final double b) {
        return net.jafama.FastMath.max(a, b);
    }

    /** {@inheritDoc} */
    @Override
    public double hypot(final double x, final double y) {
        return net.jafama.FastMath.hypot(x, y);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop MethodName
    // Reason: name kept as such
    @Override
    public double IEEEremainder(final double dividend, final double divisor) {
        return net.jafama.FastMath.IEEEremainder(dividend, divisor);
    }

    /** {@inheritDoc} */
    @Override
    public double copySign(final double magnitude, final double sign) {
        return FastMath.copySign(magnitude, sign);
    }

    /** {@inheritDoc} */
    @Override
    public float copySign(final float magnitude, final float sign) {
        return FastMath.copySign(magnitude, sign);
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