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
 * @history created 26/03/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.6:DM:DM-2656:27/01/2021:[PATRIUS] delTa parametrable utilise pour le calcul de vitesse dans
 * QuaternionPolynomialProfile
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:FA:FA-2084:15/05/2019:[PATRIUS] Suppression de la classe AbstractGuidanceProfile
 * VERSION::FA:180:27/03/2014:Removed DynamicsElements - frames transformations derivatives unknown
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.kinematics.AbstractOrientationFunction;
import fr.cnes.sirius.patrius.attitudes.kinematics.OrientationFunction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.FourierSeries;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Represents a quaternion guidance profile, calculated with Fourier series.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment thread-safe if the Frame attribute is thread-safe.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class QuaternionHarmonicProfile extends AbstractAttitudeProfile {

    /** Serializable UID. */
    private static final long serialVersionUID = 4343338300700455854L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "QUATERNION_HARMONIC_PROFILE";

    /** Initial date. */
    private final AbsoluteDate date0;

    /** The reference frame of the Fourier series. */
    private final Frame referenceFrame;

    /** Fourier series representing the q0 component of the quaternion. */
    private final FourierSeries q0fs;

    /** Fourier series representing the q1 component of the quaternion. */
    private final FourierSeries q1fs;

    /** Fourier series representing the q2 component of the quaternion. */
    private final FourierSeries q2fs;

    /** Fourier series representing the q3 component of the quaternion. */
    private final FourierSeries q3fs;

    /** Nature. */
    private final String nature;
    
    /**
     * Create a harmonic, quaternion guidance profile.
     * 
     * @param origin
     *        origin of date
     * @param frame
     *        the reference frame of the Fourier series
     * @param q0
     *        q0 quaternion component Fourier decomposition
     * @param q1
     *        q1 quaternion component Fourier decomposition
     * @param q2
     *        q2 quaternion component Fourier decomposition
     * @param q3
     *        q3 quaternion component Fourier decomposition
     * @param timeInterval
     *        interval of validity of the guidance profile
     */
    public QuaternionHarmonicProfile(final AbsoluteDate origin,
            final Frame frame,
            final FourierSeries q0,
            final FourierSeries q1,
            final FourierSeries q2,
            final FourierSeries q3,
            final AbsoluteDateInterval timeInterval) {
        super(timeInterval);
        this.date0 = origin;
        this.referenceFrame = frame;
        this.q0fs = q0;
        this.q1fs = q1;
        this.q2fs = q2;
        this.q3fs = q3;
        this.nature = DEFAULT_NATURE;
    }

    /**
     * Create a harmonic, quaternion guidance profile.
     * 
     * @param origin
     *        origin of date
     * @param frame
     *        the reference frame of the Fourier series
     * @param q0
     *        q0 quaternion component Fourier decomposition
     * @param q1
     *        q1 quaternion component Fourier decomposition
     * @param q2
     *        q2 quaternion component Fourier decomposition
     * @param q3
     *        q3 quaternion component Fourier decomposition
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param spinDeltaT delta-t used for spin computation by finite differences
     */
    public QuaternionHarmonicProfile(final AbsoluteDate origin,
            final Frame frame,
            final FourierSeries q0,
            final FourierSeries q1,
            final FourierSeries q2,
            final FourierSeries q3,
            final AbsoluteDateInterval timeInterval,
            final double spinDeltaT) {
        this(origin, frame, q0, q1, q2, q3, timeInterval, DEFAULT_NATURE, spinDeltaT);
    }

    /**
     * Create a harmonic, quaternion guidance profile.
     * 
     * @param origin
     *        origin of date
     * @param frame
     *        the reference frame of the Fourier series
     * @param q0
     *        q0 quaternion component Fourier decomposition
     * @param q1
     *        q1 quaternion component Fourier decomposition
     * @param q2
     *        q2 quaternion component Fourier decomposition
     * @param q3
     *        q3 quaternion component Fourier decomposition
     * @param timeInterval
     *        interval of validity of the guidance profile
     * @param nature nature
     * @param spinDeltaT delta-t used for spin computation by finite differences
     */
    public QuaternionHarmonicProfile(final AbsoluteDate origin,
            final Frame frame,
            final FourierSeries q0,
            final FourierSeries q1,
            final FourierSeries q2,
            final FourierSeries q3,
            final AbsoluteDateInterval timeInterval,
            final String nature,
            final double spinDeltaT) {
        super(timeInterval, spinDeltaT);
        this.date0 = origin;
        this.referenceFrame = frame;
        this.q0fs = q0;
        this.q1fs = q1;
        this.q2fs = q2;
        this.q3fs = q3;
        this.nature = nature;
    }

    /**
     * @return the Fourier series representing the q0 quaternion component.
     */
    public FourierSeries getQ0FourierSeries() {
        return this.q0fs;
    }

    /**
     * @return the Fourier series representing the q1 quaternion component.
     */
    public FourierSeries getQ1FourierSeries() {
        return this.q1fs;
    }

    /**
     * @return the Fourier series representing the q2 quaternion component.
     */
    public FourierSeries getQ2FourierSeries() {
        return this.q2fs;
    }

    /**
     * @return the Fourier series representing the q3 quaternion component.
     */
    public FourierSeries getQ3FourierSeries() {
        return this.q3fs;
    }

    /**
     * @return the angular frequencies of the four Fourier series representing q0, q1, q2 qnd q3.
     */
    public double[] getAngularFrequencies() {
        final double[] frequencies = new double[4];
        frequencies[0] = this.q0fs.getAngularFrequency();
        frequencies[1] = this.q1fs.getAngularFrequency();
        frequencies[2] = this.q2fs.getAngularFrequency();
        frequencies[3] = this.q3fs.getAngularFrequency();
        return frequencies;
    }

    /**
     * @return the a0 coefficients of the four Fourier series representing q0, q1, q2 qnd q3.
     */
    public double[] getConstants() {
        final double[] a0 = new double[4];
        a0[0] = this.q0fs.getConstant();
        a0[1] = this.q1fs.getConstant();
        a0[2] = this.q2fs.getConstant();
        a0[3] = this.q3fs.getConstant();
        return a0;
    }

    /**
     * @return the a coefficients of the four Fourier series representing q0, q1, q2 qnd q3.
     */
    public double[][] getCosArrays() {
        final double[][] cosArray = new double[4][];
        cosArray[0] = this.q0fs.getCosArray();
        cosArray[1] = this.q1fs.getCosArray();
        cosArray[2] = this.q2fs.getCosArray();
        cosArray[3] = this.q3fs.getCosArray();
        return cosArray;
    }

    /**
     * @return the b coefficients of the four Fourier series representing q0, q1, q2 qnd q3.
     */
    public double[][] getSinArrays() {
        final double[][] sinArray = new double[4][];
        sinArray[0] = this.q0fs.getSinArray();
        sinArray[1] = this.q1fs.getSinArray();
        sinArray[2] = this.q2fs.getSinArray();
        sinArray[3] = this.q3fs.getSinArray();
        return sinArray;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        checkDate(date);

        // Calculate rotation in reference frame
        final double x = date.durationFrom(this.date0);
        final Rotation rotation = new Rotation(false, this.q0fs.value(x), this.q1fs.value(x), this.q2fs.value(x),
                this.q3fs.value(x));

        // transform the rotation from the reference frame to the new frame:
        final Transform toReferenceFrame = frame.getTransformTo(this.referenceFrame, date);
        final Rotation rotationInUserFrame = rotation.applyTo(toReferenceFrame.getRotation());

        final double h = getSpinDeltaT();
        final OrientationFunction rot = new AbstractOrientationFunction(date) {
            /** {@inheritDoc} */
            @Override
            public Rotation getOrientation(final AbsoluteDate date) throws PatriusException {
                return rotationInUserFrame;
            }
        };
        final Vector3DFunction spin = new AbstractVector3DFunction(date) {
            /** {@inheritDoc} */
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return rot.estimateRateFunction(h, getTimeInterval()).getVector3D(date);
            }
        };
        return new Attitude(date, frame, rotationInUserFrame, spin.getVector3D(date));
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        throw new PatriusRuntimeException(PatriusMessages.ATTITUDE_SPIN_DERIVATIVES_NOT_AVAILABLE, null);
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return nature;
    }

    /** {@inheritDoc} */
    @Override
    public QuaternionHarmonicProfile copy(final AbsoluteDateInterval newInterval) {
        return new QuaternionHarmonicProfile(date0, referenceFrame, q0fs, q1fs, q2fs, q3fs, newInterval,
                getSpinDeltaT());
    }
}
