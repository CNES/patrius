/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.random.UnitSphereRandomVectorGenerator;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Interpolating function that implements the
 * <a href="http://www.dudziak.com/microsphere.php">Microsphere Projection</a>.
 * 
 * @version $Id: MicrosphereInterpolatingFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.NullAssignment")
public class MicrosphereInterpolatingFunction
    implements MultivariateFunction {

    /**
     * Internal accounting data for the interpolation algorithm.
     * Each element of the list corresponds to one surface element of
     * the microsphere.
     */
    private final List<MicrosphereSurfaceElement> microsphere;
    /**
     * Exponent used in the power law that computes the weights of the
     * sample data.
     */
    private final double brightnessExponent;
    /**
     * Sample data.
     */
    private final Map<RealVector, Double> samples;

    /**
     * @param xval
     *        Arguments for the interpolation points. {@code xval[i][0]} is the first component of interpolation
     *        point {@code i}, {@code xval[i][1]} is the second component, and so on
     *        until {@code xval[i][d-1]}, the last component of that interpolation
     *        point (where {@code dimension} is thus the dimension of the sampled
     *        space).
     * @param yval
     *        Values for the interpolation points.
     * @param brightnessExponentIn
     *        Brightness dimming factor.
     * @param microsphereElements
     *        Number of surface elements of the
     *        microsphere.
     * @param rand
     *        Unit vector generator for creating the microsphere.
     * @throws DimensionMismatchException
     *         if the lengths of {@code yval} and {@code xval} (equal to {@code n}, the number of interpolation
     *         points)
     *         do not match, or the the arrays {@code xval[0]} ... {@code xval[n]},
     *         have lengths different from {@code dimension}.
     * @throws NoDataException
     *         if there an array has zero-length.
     * @throws NullArgumentException
     *         if an argument is {@code null}.
     */
    public MicrosphereInterpolatingFunction(final double[][] xval,
        final double[] yval,
        final int brightnessExponentIn,
        final int microsphereElements,
        final UnitSphereRandomVectorGenerator rand) {
        if (xval == null ||
            yval == null) {
            throw new NullArgumentException();
        }
        if (xval.length == 0) {
            throw new NoDataException();
        }
        if (xval.length != yval.length) {
            throw new DimensionMismatchException(xval.length, yval.length);
        }
        if (xval[0] == null) {
            throw new NullArgumentException();
        }

        final int dimension = xval[0].length;
        this.brightnessExponent = brightnessExponentIn;

        // Copy data samples.
        this.samples = new HashMap<RealVector, Double>(yval.length);
        for (int i = 0; i < xval.length; ++i) {
            final double[] xvalI = xval[i];
            if (xvalI == null) {
                throw new NullArgumentException();
            }
            if (xvalI.length != dimension) {
                throw new DimensionMismatchException(xvalI.length, dimension);
            }

            this.samples.put(new ArrayRealVector(xvalI), yval[i]);
        }

        this.microsphere = new ArrayList<MicrosphereSurfaceElement>(microsphereElements);
        // Generate the microsphere, assuming that a fairly large number of
        // randomly generated normals will represent a sphere.
        for (int i = 0; i < microsphereElements; i++) {
            this.microsphere.add(new MicrosphereSurfaceElement(rand.nextVector()));
        }
    }

    /**
     * @param point
     *        Interpolation point.
     * @return the interpolated value.
     */
    @Override
    public double value(final double[] point) {
        final RealVector p = new ArrayRealVector(point);

        // Reset.
        for (final MicrosphereSurfaceElement md : this.microsphere) {
            md.reset();
        }

        // Compute contribution of each sample points to the microsphere elements illumination
        for (final Map.Entry<RealVector, Double> sd : this.samples.entrySet()) {

            // Vector between interpolation point and current sample point.
            final RealVector diff = sd.getKey().subtract(p);
            final double diffNorm = diff.getNorm();

            if (MathLib.abs(diffNorm) < MathLib.ulp(1d)) {
                // No need to interpolate, as the interpolation point is
                // actually (very close to) one of the sampled points.
                return sd.getValue();
            }

            for (final MicrosphereSurfaceElement md : this.microsphere) {
                final double w = MathLib.pow(diffNorm, -this.brightnessExponent);
                md.store(this.cosAngle(diff, md.normal()) * w, sd);
            }

        }

        // Interpolation calculation.
        double value = 0;
        double totalWeight = 0;
        for (final MicrosphereSurfaceElement md : this.microsphere) {
            final double iV = md.illumination();
            final Map.Entry<RealVector, Double> sd = md.sample();
            if (sd != null) {
                value += iV * sd.getValue();
                totalWeight += iV;
            }
        }

        return value / totalWeight;
    }

    /**
     * Compute the cosine of the angle between 2 vectors.
     * 
     * @param v
     *        Vector.
     * @param w
     *        Vector.
     * @return the cosine of the angle between {@code v} and {@code w}.
     */
    private double cosAngle(final RealVector v, final RealVector w) {
        return v.dotProduct(w) / (v.getNorm() * w.getNorm());
    }

    /**
     * Class for storing the accounting data needed to perform the
     * microsphere projection.
     */
    private static class MicrosphereSurfaceElement {

        /** Normal vector characterizing a surface element. */
        private final RealVector normalVector;

        /** Illumination received from the brightest sample. */
        private double brightestIllumination;
        
        /** Brightest sample. */
        private Map.Entry<RealVector, Double> brightestSample;

        /**
         * @param n
         *        Normal vector characterizing a surface element
         *        of the microsphere.
         */
        MicrosphereSurfaceElement(final double[] n) {
            this.normalVector = new ArrayRealVector(n);
        }

        /**
         * Return the normal vector.
         * 
         * @return the normal vector
         */
        private RealVector normal() {
            return this.normalVector;
        }

        /**
         * Reset "illumination" and "sampleIndex".
         */
        private void reset() {
            this.brightestIllumination = 0;
            this.brightestSample = null;
        }

        /**
         * Store the illumination and index of the brightest sample.
         * 
         * @param illuminationFromSample
         *        illumination received from sample
         * @param sample
         *        current sample illuminating the element
         */
        private void store(final double illuminationFromSample,
                           final Map.Entry<RealVector, Double> sample) {
            if (illuminationFromSample > this.brightestIllumination) {
                this.brightestIllumination = illuminationFromSample;
                this.brightestSample = sample;
            }
        }

        /**
         * Get the illumination of the element.
         * 
         * @return the illumination.
         */
        private double illumination() {
            return this.brightestIllumination;
        }

        /**
         * Get the sample illuminating the element the most.
         * 
         * @return the sample.
         */
        private Map.Entry<RealVector, Double> sample() {
            return this.brightestSample;
        }
    }
}
