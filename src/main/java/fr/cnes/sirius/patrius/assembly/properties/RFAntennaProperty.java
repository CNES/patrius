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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:567:04/04/2016:Link budget correction
 * END-HISTORY
 * 
 * @history created 03/09/2012
 */
package fr.cnes.sirius.patrius.assembly.properties;

import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.RFLinkBudgetModel;
import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.LinearInterpolator;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents an RF antenna property for a part of the assembly. This property
 * is used when calculating the RF link budget.
 * 
 * @concurrency immutable
 * 
 * @see IPartProperty
 * @see RFLinkBudgetModel
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RFAntennaProperty implements IPartProperty {

    /** Serial UID. */
    private static final long serialVersionUID = -8792178238795884106L;

    /**
     * Amplifier output power [dB].
     */
    private final double outputPower;

    /**
     * Antenna pattern values of the polar angle [rad]; polar angle is 0
     * on antenna boresight, and its values are between 0 and PI.
     */
    private final double[] patternPolarAngle;

    /**
     * Antenna pattern values of the azimuthal angle [rad]; this angle is 0
     * on antenna XZ meridian, and its values are between 0 and 2PI.
     */
    private final double[] patternAzimuth;

    /**
     * The function interpolating the gain diagram values.
     */
    private final BivariateFunction interpFunctionGain;

    /**
     * The function interpolating the ellipticity factor values.
     */
    private final BivariateFunction interpFunctionEllipticity;

    /**
     * Technological losses by the satellite transmitter [dB].
     */
    private final double technoLoss;

    /**
     * Losses between TX and antenna [dB].
     */
    private final double circuitLoss;

    /**
     * Bit rate for nominal mode [bps].
     */
    private final double bitRate;

    /**
     * Emission frequency [Hz].
     */
    private final double frequency;

    /**
     * Constructor of this property.<br>
     * 
     * @param inOutputPower
     *        amplifier output power.
     * @param inPatternPolarAngle
     *        antenna pattern values of the polar angle (1 x N); this array can't contain 2 values (but it can
     *        contain only one value) because of the spline interpolation: at least 3 values are required.<br>
     *        Polar angle values must be between 0 and PI.
     * @param inPatternAzimuth
     *        antenna pattern values of the azimuthal angle (1 x M); this array can't contain 2 values (but it can
     *        contain only one value) because of the spline interpolation: at least 3 values are required.<br>
     *        Azimuthal angle values must be between 0 and 2PI.
     * @param inGainPattern
     *        antenna gain diagram values: Gain = F(polar angle, azimuth) (N x M).
     * @param inEllipticityFactor
     *        factor of ellipticity: F(polar angle, azimuth) (N x M).
     * @param inTechnoLoss
     *        technological losses by the satellite transmitter.
     * @param inCircuitLoss
     *        losses between TX and antenna.
     * @param inBitRate
     *        bit rate for nominal mode.
     * @param inFrequency
     *        emission frequency.
     */
    public RFAntennaProperty(final double inOutputPower, final double[] inPatternPolarAngle,
        final double[] inPatternAzimuth, final double[][] inGainPattern,
        final double[][] inEllipticityFactor, final double inTechnoLoss, final double inCircuitLoss,
        final double inBitRate, final double inFrequency) {
        // check vectors and matrices dimensions:
        final int polLength = inPatternPolarAngle.length;
        final int azLength = inPatternAzimuth.length;
        this.checkInputArrays(inGainPattern, inEllipticityFactor, polLength, azLength);

        // check the polar angles and the azimuthal angle:
        if ((inPatternPolarAngle[0] < 0 || inPatternPolarAngle[polLength - 1] > FastMath.PI)
            || (inPatternAzimuth[0] < 0 || inPatternAzimuth[azLength - 1] > 2. * FastMath.PI)) {
            throw new MathIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        this.outputPower = inOutputPower;
        // gain pattern matrix:
        this.patternPolarAngle = inPatternPolarAngle.clone();
        this.patternAzimuth = inPatternAzimuth.clone();
        this.interpFunctionGain =
            this.computeInterpolatingFunction(this.patternPolarAngle, this.patternAzimuth, inGainPattern.clone());
        this.interpFunctionEllipticity =
            this.computeInterpolatingFunction(this.patternPolarAngle, this.patternAzimuth,
                    inEllipticityFactor.clone());
        this.technoLoss = inTechnoLoss;
        this.circuitLoss = inCircuitLoss;
        this.bitRate = inBitRate;
        this.frequency = inFrequency;
    }

    /**
     * Check if the size of the input arrays are coherent.
     * 
     * @param gainPatternIn
     *        antenna gain diagram values: Gain = F(polar angle, azimuth) (N x M).
     * @param ellipticityFactorIn
     *        factor of ellipticity: F(polar angle, azimuth) (N x M).
     * @param polLength
     *        the length of the polar angles vector.
     * @param azLength
     *        the length of the azimuthal angles vector.
     */
    private void checkInputArrays(final double[][] gainPatternIn, final double[][] ellipticityFactorIn,
                                  final int polLength, final int azLength) {
        if ((azLength == 2) || (polLength == 2)) {
            //
            throw new MathIllegalArgumentException(PatriusMessages.PDB_UNSUPPORTED_ARRAY_DIMENSION);
        }
        final int gainLengthX = gainPatternIn.length;
        final int ellLengthX = ellipticityFactorIn.length;
        if ((polLength != gainLengthX) || (polLength != ellLengthX)) {
            // the x-dimensions do not correspond:
            if (polLength == gainLengthX) {
                throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH, polLength, ellLengthX);
            } else {
                throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH, polLength, gainLengthX);
            }
        }
        // check the rows dimension of the matrices:
        for (int j = 0; j < polLength; j++) {
            if ((gainPatternIn[j].length != azLength) || (ellipticityFactorIn[j].length != azLength)) {
                // the y-dimensions do not correspond:
                if (azLength == gainPatternIn[j].length) {
                    throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH,
                        azLength, ellipticityFactorIn[j].length);
                } else {
                    throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH,
                        azLength, gainPatternIn[j].length);
                }
            }
        }
    }

    /**
     * Computes the bivariate function interpolating the input values. The interpolation is done using splines.
     * 
     * @param polarAngle
     *        the polar angle
     * @param azimuth
     *        the azimuthal angle
     * @param values
     *        the values = F(polarAngle, azimuth)
     * @return the interpolating function of the input data.
     */
    private BivariateFunction computeInterpolatingFunction(final double[] polarAngle, final double[] azimuth,
                                                           final double[][] values) {
        final int np = polarAngle.length;
        final int na = azimuth.length;

        final BivariateFunction function;
        if (na >= 2 && np >= 2) {
            // Bi-linear interpolation
            function = new BiLinearIntervalsInterpolator().interpolate(polarAngle, azimuth, values);
        } else if (np == 1 && na >= 2) {
            // only one polar angle value - interpolate over the one row of the values matrix:
            final double[] vector = values[0];
            final UnivariateFunction univariateFunction = new LinearInterpolator().interpolate(azimuth, vector);
            function = new BivariateFunction(){
                /** {@inheritDoc} */
                @Override
                public double value(final double x, final double y) {
                    return univariateFunction.value(y);
                }
            };
        } else if (np >= 2 && na == 1) {
            // only one azimuth value - interpolate over the one column of the values matrix:
            final double[] vector = new double[np];
            for (int row = 0; row < np; row++) {
                vector[row] = values[row][0];
            }
            final UnivariateFunction univariateFunction = new LinearInterpolator().interpolate(polarAngle, vector);
            function = new BivariateFunction(){
                /** {@inheritDoc} */
                @Override
                public double value(final double x, final double y) {
                    return univariateFunction.value(x);
                }
            };

        } else if (na == 1 && np == 1) {
            // only one polar angle and azimuth - no interpolation:
            function = new BivariateFunction(){
                /** {@inheritDoc} */
                @Override
                public double value(final double x, final double y) {
                    return values[0][0];
                }
            };
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.PDB_UNSUPPORTED_ARRAY_DIMENSION);
        }
        return function;
    }

    /**
     * @return the amplifier output power [dB].
     */
    public final double getOutputPower() {
        return this.outputPower;
    }

    /**
     * Gets the antenna gain using a spline interpolation. <br>
     * The antenna gain is a function of the station direction (gain = F(polarAngle, azimuth)).
     * 
     * @param polarAngle
     *        the polar angle of the ground station direction in the antenna frame [0, PI].
     * @param azimuth
     *        the azimuth of the ground station direction in the antenna frame [0, 2PI].
     * 
     * @return the antenna gain for the specified ground station direction [dB].
     */
    public final double getGain(final double polarAngle, final double azimuth) {
        // checks the input angles:
        if ((polarAngle < 0 || polarAngle > FastMath.PI) || (azimuth < 0 || azimuth > 2 * FastMath.PI)) {
            throw new MathIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        final double pol = this.checkInputAngles(polarAngle, this.patternPolarAngle);
        final double az = this.checkInputAngles(azimuth, this.patternAzimuth);
        return this.interpFunctionGain.value(pol, az);
    }

    /**
     * Gets the factor of ellipticity using a spline interpolation. <br>
     * The ellipticity factor is a function of the station direction (ellipticity = F(polarAngle, azimuth))
     * and is used to calculate the polarisation losses of the antenna.
     * 
     * @param polarAngle
     *        the polar angle of the ground station direction in the antenna frame [0, PI].
     * @param azimuth
     *        the azimuth of the ground station direction in the antenna frame [0, 2PI].
     * 
     * @return the ellipticity factor for the specified ground station direction [dB].
     */
    public final double getEllipticity(final double polarAngle, final double azimuth) {
        // checks the input angles:
        if ((polarAngle < 0 || polarAngle > FastMath.PI) || (azimuth < 0 || azimuth > 2 * FastMath.PI)) {
            throw new MathIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        final double pol = this.checkInputAngles(polarAngle, this.patternPolarAngle);
        final double az = this.checkInputAngles(azimuth, this.patternAzimuth);
        return this.interpFunctionEllipticity.value(pol, az);
    }

    /**
     * Verifies if the input angle is contained in the array of angles; if not, returns the value of the
     * first or last angle of the array, otherwise returns itself.
     * 
     * @param angle
     *        the input angle
     * @param array
     *        the array of angles
     * 
     * @return the input angle or the first/last angle of the input array.
     */
    private double checkInputAngles(final double angle, final double[] array) {
        double currentAngle = angle;
        if (angle <= array[0]) {
            // the smallest angle of the pattern is bigger than the input angle:
            currentAngle = array[0];
        } else if (angle > array[array.length - 1]) {
            // the biggest angle of the pattern is smaller than the input angle:
            currentAngle = array[array.length - 1];
        }
        return currentAngle;
    }

    /**
     * @return the technological losses by the satellite transmitter [dB].
     */
    public final double getTechnoLoss() {
        return this.technoLoss;
    }

    /**
     * @return the losses between TX and antenna [dB].
     */
    public final double getCircuitLoss() {
        return this.circuitLoss;
    }

    /**
     * @return the bit rate for nominal mode [bps].
     */
    public final double getBitRate() {
        return this.bitRate;
    }

    /**
     * @return the emission frequency [Hz].
     */
    public final double getFrequency() {
        return this.frequency;
    }

    /** {@inheritDoc} */
    @Override
    public final PropertyType getType() {
        return PropertyType.RF;
    }
}
