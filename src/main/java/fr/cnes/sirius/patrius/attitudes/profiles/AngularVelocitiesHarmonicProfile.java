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
 * @history Created 20/03/2013
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:FA:FA-2084:15/05/2019:[PATRIUS] Suppression de la classe AbstractGuidanceProfile
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:98:12/07/2013:Fixed wrong date parameter given to att and DynamicsElements const.
 * VERSION::FA:180:27/03/2014:Removed DynamicsElements - frames transformations derivatives unknown
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * VERSION::DM:1951:10/12/2018:add getRotationAcceleration(...) and methods for truncation
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.polynomials.FourierSeries;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * An attitude angular velocities profile, whose x-y-z components are represented with Fourier series.
 * <p>
 *
 * @author Rami Houdroge, Pierre Brechard
 *
 * @since 2.0
 */
public class AngularVelocitiesHarmonicProfile extends AbstractAngularVelocitiesAttitudeProfile {

    /** Serialization UID. */
    private static final long serialVersionUID = 1345017247221207645L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ANGULAR_VELOCITIES_HARMONIC_PROFILE";

    /**
     * Create a harmonic, angular velocities guidance profile.
     *
     * @param xAngle
     *        x angular velocity Fourier decomposition
     * @param yAngle
     *        y angular velocity Fourier decomposition
     * @param zAngle
     *        z angular velocity Fourier decomposition
     * @param frame
     *        frame where the initial rotation and xAngle, yAngle and zAngle Fourier Series functions are expressed
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date
     * @param integType
     *        integration type
     * @param integStep
     *        integration step
     * @throws PatriusException
     *         thrown if the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesHarmonicProfile(final FourierSeries xAngle, final FourierSeries yAngle,
            final FourierSeries zAngle, final Frame frame, final AbsoluteDateInterval timeInterval,
            final Rotation rotationRef, final AbsoluteDate dateRef, final AngularVelocityIntegrationType integType,
            final double integStep) throws PatriusException {
        this(xAngle, yAngle, zAngle, frame, timeInterval, rotationRef, dateRef, integType, integStep, DEFAULT_NATURE);
    }

    /**
     * Create a harmonic, angular velocities guidance profile.
     *
     * @param xAngle
     *        x angular velocity Fourier decomposition
     * @param yAngle
     *        y angular velocity Fourier decomposition
     * @param zAngle
     *        z angular velocity Fourier decomposition
     * @param frame
     *        frame where the initial rotation and xAngle, yAngle and zAngle Fourier Series functions are expressed
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date
     * @param integType
     *        integration type
     * @param integStep
     *        integration step
     * @param cacheFreq
     *        Number of integration steps performed between two values stored in the underlying cache
     * @throws PatriusException
     *         thrown if the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesHarmonicProfile(final FourierSeries xAngle, final FourierSeries yAngle,
            final FourierSeries zAngle, final Frame frame, final AbsoluteDateInterval timeInterval,
            final Rotation rotationRef, final AbsoluteDate dateRef, final AngularVelocityIntegrationType integType,
            final double integStep, final int cacheFreq) throws PatriusException {
        this(xAngle, yAngle, zAngle, frame, timeInterval, rotationRef, dateRef, integType, integStep, cacheFreq,
                DEFAULT_NATURE);
    }

    /**
     * Create a harmonic, angular velocities guidance profile.
     *
     * @param frame
     *        frame where initialRotation and xAngle, yAngle and zAngle are expressed
     * @param xAngle
     *        x angular velocity Fourier decomposition
     * @param yAngle
     *        y angular velocity Fourier decomposition
     * @param zAngle
     *        z angular velocity Fourier decomposition
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date
     * @param integType
     *        integration type
     * @param integStep
     *        integration step
     * @param nature
     *        Nature
     * @throws PatriusException
     *         thrown if the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesHarmonicProfile(final FourierSeries xAngle, final FourierSeries yAngle,
            final FourierSeries zAngle, final Frame frame, final AbsoluteDateInterval timeInterval,
            final Rotation rotationRef, final AbsoluteDate dateRef, final AngularVelocityIntegrationType integType,
            final double integStep, final String nature) throws PatriusException {
        super(new FourierSeries3DFunction(xAngle, yAngle, zAngle, dateRef), frame, timeInterval, rotationRef, dateRef,
                integType, integStep, nature);
    }

    /**
     * Create a harmonic, angular velocities guidance profile.
     *
     * @param frame
     *        frame where initialRotation and xAngle, yAngle and zAngle are expressed
     * @param xAngle
     *        x angular velocity Fourier decomposition
     * @param yAngle
     *        y angular velocity Fourier decomposition
     * @param zAngle
     *        z angular velocity Fourier decomposition
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date
     * @param integType
     *        integration type
     * @param integStep
     *        integration step
     * @param cacheFreq
     *        Number of integration steps performed between two values stored in the underlying cache
     * @param nature
     *        Nature
     * @throws PatriusException
     *         thrown if the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesHarmonicProfile(final FourierSeries xAngle, final FourierSeries yAngle,
            final FourierSeries zAngle, final Frame frame, final AbsoluteDateInterval timeInterval,
            final Rotation rotationRef, final AbsoluteDate dateRef, final AngularVelocityIntegrationType integType,
            final double integStep, final int cacheFreq, final String nature) throws PatriusException {
        super(new FourierSeries3DFunction(xAngle, yAngle, zAngle, dateRef), frame, timeInterval, rotationRef, dateRef,
                integType, integStep, cacheFreq, nature);
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesHarmonicProfile copy(final AbsoluteDateInterval newInterval) {
        // Check new interval is included in old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        try {
            // Retrieve data
            final AbsoluteDateInterval oldInt = getTimeInterval();
            final AbsoluteDate dateI = newInterval.getLowerData();
            final Rotation rotationIn = getOrientation(dateI);
            final double[] freqs = getAngularFrequencies();
            final double[] cnsts = getConstants();
            final double delta = dateI.durationFrom(oldInt.getLowerData());

            // Redefine coefficients of the Fourier Series
            final double[][] newCos = redefineCosArray(delta);
            final double[][] newSin = redefineSinArray(delta);

            final FourierSeries xfsNew = new FourierSeries(freqs[0], cnsts[0], newCos[0], newSin[0]);
            final FourierSeries yfsNew = new FourierSeries(freqs[1], cnsts[1], newCos[1], newSin[1]);
            final FourierSeries zfsNew = new FourierSeries(freqs[2], cnsts[2], newCos[2], newSin[2]);

            // Build new instance
            final AngularVelocitiesHarmonicProfile res = new AngularVelocitiesHarmonicProfile(xfsNew, yfsNew, zfsNew,
                this.refFrame, newInterval, rotationIn, dateI, this.type, this.integStep, this.cacheFreq, getNature());
            res.setSpinDerivativesComputation(this.spinDerivativesComputation);
            return res;
        } catch (final PatriusException e) {
            // Should not happen since law is valid on interval
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /**
     * Redefine Fourier Series Cosine coefficients.
     *
     * @param delta
     *        difference between newLowerDate e oldLowerDate
     * @return the new Fourier Series Cosine coefficients
     */
    private double[][] redefineCosArray(final double delta) {

        // Get data
        final double[] w = getAngularFrequencies();
        final double[][] c = getCosArrays();
        final double[][] s = getSinArrays();
        final double[][] newCosArray = c.clone();

        // New Cosine Coefficients Ak'
        // Ak’ = ak cos(kw0 (t0’-t0)) + bk sin(kw0(t0’-t0))
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < c[i].length; j++) {
                final double[] sincos = MathLib.sinAndCos((j + 1) * w[i] * delta);
                final double sin = sincos[0];
                final double cos = sincos[1];
                final double value = (c[i][j] * cos) + (s[i][j] * sin);
                newCosArray[i][j] = value;
            }
        }
        // Return result
        return newCosArray;
    }

    /**
     * Redefine Fourier Series Sine coefficients.
     *
     * @param delta
     *        difference between newLowerDate e oldLowerDate
     * @return the new Fourier Series Sine coefficients
     */
    private double[][] redefineSinArray(final double delta) {

        // Get data
        final double[] w = getAngularFrequencies();
        final double[][] c = getCosArrays();
        final double[][] s = getSinArrays();
        final double[][] newSinArray = s.clone();

        // New Cosine Coefficients Bk'
        // Bk’ = bk cos(kw0 (t0’-t0)) - ak sin(kw0(t0’-t0))
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < s[i].length; j++) {
                final double[] sincos = MathLib.sinAndCos((j + 1) * w[i] * delta);
                final double sin = sincos[0];
                final double cos = sincos[1];
                final double value = (s[i][j] * cos) - (c[i][j] * sin);
                newSinArray[i][j] = value;
            }
        }
        // Return result
        return newSinArray;
    }

    /**
     * Gets the angular frequencies of the three Fourier series representing x, y and z.
     *
     * @return the angular frequencies
     */
    public double[] getAngularFrequencies() {
        return ((FourierSeries3DFunction) this.spinVectorFunction).getAngularFrequencies();
    }

    /**
     * Gets the <code>a0</code> coefficients of the three Fourier series representing x, y and z.
     *
     * @return the <code>a0</code> coefficients
     */
    public double[] getConstants() {
        return ((FourierSeries3DFunction) this.spinVectorFunction).getConstants();
    }

    /**
     * Gets the <code>a</code> coefficients of the three Fourier series representing x, y and z.
     *
     * @return the <code>a</code> coefficients of the three Fourier series representing x, y and z.
     */
    public double[][] getCosArrays() {
        return ((FourierSeries3DFunction) this.spinVectorFunction).getCosArrays();
    }

    /**
     * Gets the <code>b</code> coefficients of the three Fourier series representing x, y and z.
     *
     * @return the <code>b</code> coefficients of the three Fourier series representing x, y and z.
     */
    public double[][] getSinArrays() {
        return ((FourierSeries3DFunction) this.spinVectorFunction).getSinArrays();
    }

    /**
     * Gets the size of the Fourierseries3DFunction, ie 3.
     *
     * @return 3.
     */
    public int getSize() {
        return ((FourierSeries3DFunction) this.spinVectorFunction).getSize();
    }

    /**
     * Fourier Series {@link Vector3DFunction}.
     *
     * @author Rami Houdroge, Pierre Brechard
     *
     * @since 2.0
     */
    private static final class FourierSeries3DFunction implements Vector3DFunction, Serializable {

        /** Serialization UID. */
        private static final long serialVersionUID = -1655067902166792760L;

        /** Harmonic representation of angular velocity x-componenet : <code>d&theta;<sub>x</sub>/dt</code>. */
        private final FourierSeries xFunction;

        /** Harmonic representation of angular velocity y-componenet : <code>d&theta;<sub>y</sub>/dt</code>. */
        private final FourierSeries yFunction;

        /** Harmonic representation of angular velocity z-componenet : <code>d&theta;<sub>z</sub>/dt</code>. */
        private final FourierSeries zFunction;

        /** Initial reference date. */
        private final AbsoluteDate date0;

        /**
         * Constructor.
         * @param xfs
         *        angular velocity x-componenet function : <code>d&theta;<sub>x</sub>/dt</code>
         * @param yfs
         *        angular velocity y-componenet function : <code>d&theta;<sub>y</sub>/dt</code>
         * @param zfs
         *        angular velocity z-componenet function : <code>d&theta;<sub>z</sub>/dt</code>
         * @param date0
         *        Initial reference date
         */
        public FourierSeries3DFunction(final FourierSeries xfs, final FourierSeries yfs, final FourierSeries zfs,
                final AbsoluteDate date0) {
            this.xFunction = xfs;
            this.yFunction = yfs;
            this.zFunction = zfs;
            this.date0 = date0;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final double t) {
            return new double[] { this.xFunction.value(t), this.yFunction.value(t), this.zFunction.value(t) };
        }

        /** {@inheritDoc} */
        @Override
        public Vector3DFunction nthDerivative(final int order) {
            // Derivatives are available through the FourierSeries components
            return new FourierSeries3DFunction((FourierSeries) this.xFunction.derivative(order),
                    (FourierSeries) this.yFunction.derivative(order), (FourierSeries) this.zFunction.derivative(order),
                    this.date0);
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D integral(final double t0, final double tf) {

            // trigonometric part
            final FourierSeries xfI = this.xFunction.polynomialPrimitive();
            final FourierSeries yfI = this.yFunction.polynomialPrimitive();
            final FourierSeries zfI = this.zFunction.polynomialPrimitive();

            // linear part
            final double cx = this.xFunction.getConstant() * (tf - t0);
            final double cy = this.yFunction.getConstant() * (tf - t0);
            final double cz = this.zFunction.getConstant() * (tf - t0);

            // final value
            final double x = (xfI.value(tf) - xfI.value(t0)) + cx;
            final double y = (yfI.value(tf) - yfI.value(t0)) + cy;
            final double z = (zfI.value(tf) - zfI.value(t0)) + cz;

            return new Vector3D(x, y, z);
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getVector3D(final AbsoluteDate userDate) throws PatriusException {
            return new Vector3D(value(userDate.durationFrom(this.date0)));
        }

        /**
         * Gets the angular frequencies of the three Fourier series representing x, y and z.
         *
         * @return the angular frequencies
         */
        public double[] getAngularFrequencies() {
            final double[] frequencies = new double[3];
            frequencies[0] = this.xFunction.getAngularFrequency();
            frequencies[1] = this.yFunction.getAngularFrequency();
            frequencies[2] = this.zFunction.getAngularFrequency();
            return frequencies;
        }

        /**
         * Gets the <code>a0</code> coefficients of the three Fourier series representing x, y and z.
         *
         * @return the <code>a0</code> coefficients
         */
        public double[] getConstants() {
            final double[] a0 = new double[3];
            a0[0] = this.xFunction.getConstant();
            a0[1] = this.yFunction.getConstant();
            a0[2] = this.zFunction.getConstant();
            return a0;
        }

        /**
         * Gets the <code>a</code> coefficients of the three Fourier series representing x, y and z.
         *
         * @return the <code>a</code> coefficients of the three Fourier series representing x, y and z.
         */
        public double[][] getCosArrays() {
            final double[][] cosArray = new double[3][];
            cosArray[0] = this.xFunction.getCosArray().clone();
            cosArray[1] = this.yFunction.getCosArray().clone();
            cosArray[2] = this.zFunction.getCosArray().clone();
            return cosArray;
        }

        /**
         * Gets the <code>b<c/ode> coefficients of the three Fourier series representing x, y and z.
         *
         * @return the <code>b</code> coefficients of the three Fourier series representing x, y and z.
         */
        public double[][] getSinArrays() {
            final double[][] sinArray = new double[3][];
            sinArray[0] = this.xFunction.getSinArray().clone();
            sinArray[1] = this.yFunction.getSinArray().clone();
            sinArray[2] = this.zFunction.getSinArray().clone();
            return sinArray;
        }

        /** {@inheritDoc} */
        @Override
        public int getSize() {
            return 3;
        }
    }
}
