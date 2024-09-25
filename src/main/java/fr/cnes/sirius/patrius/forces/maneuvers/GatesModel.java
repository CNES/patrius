/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.7:DM:DM-2818:18/05/2021:[PATRIUS|COLOSUS] Classe GatesModel
 * VERSION:4.7:DM:DM-2914:18/05/2021:Ajout d'un attribut reducedTimes à la classe QuaternionPolynomialSegment
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.SymmetricMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * This class implements a variation of the model proposed by C.R. Gates to represent the
 * uncertainty on a maneuver's magnitude and direction.
 *
 * <p>
 * The model implemented here differs from the one proposed by Gates in a few ways. Although no mention of it is made in
 * the paper, the formulation proposed by Gates seems to rely on the small angles hypothesis. This is not the case of
 * the formulation used here. In addition, this class only implements the shutoff error and the pointing error, that is,
 * the errors on the maneuver's magnitude and direction which are proportional to &Delta;V. The uncertainty on the
 * maneuver's direction is modeled as a spherical Gaussian distribution around the &Delta;V vector, while the
 * uncertainty on the maneuver's magnitude is assumed to follow an uncorrelated Gaussian distribution.
 * </p>
 *
 * <p>
 * Note that although the uncertainty on the maneuver's magnitude and direction is defined using the target &Delta;V
 * vector, the latter is generally not the mean of the distribution (this is only the case if there is no uncertainty on
 * the maneuver's direction). The mean perturbed maneuver can be computed using {@linkplain #getMeanDeltaV(Vector3D)} or
 * {@linkplain #getMeanDeltaV(Vector3D, double)}.
 * </p>
 *
 * <p>
 * <b>See:</b><br>
 * <i>A Simplified Model of Midcourse Maneuver Execution Errors</i><br>
 * by C.R. Gates<br>
 * Technical Report n°32-504, October 15, 1963<br>
 * Jet Propulsion Laboratory (JPL)<br>
 * California Institute of Technology, Pasadena, California.
 * </p>
 *
 * @author GMV
 * 
 * @since 4.7
 */
public class GatesModel {

    /**
     * Standard deviation &sigma;<sub>M</sub> on the magnitude (in percent).
     *
     * <p>
     * This parameter defines the shutoff error, which is in the direction of &Delta;V and is proportional to its
     * norm.<br>
     * Such an error would result from scale-factor errors in the shutoff system.
     * </p>
     */
    private final double sigmaMagnitude;

    /**
     * Standard deviation &sigma;<sub>D</sub> on the direction (in radians).
     *
     * <p>
     * This parameter defines the pointing error, which is orthogonal to &Delta;V and proportional to its norm.<br>
     * Such an error would result from imperfect angular orientation of the thrust vector.
     * </p>
     */
    private final double sigmaDirection;

    /**
     * Constructor.
     *
     * <p>
     * The parameter &sigma;<sub>M</sub> defines the shutoff error, which is in the direction of &Delta;V and is
     * proportional to its norm.<br>
     * Such an error would result from scale-factor errors in the shutoff system.
     * </p>
     *
     * <p>
     * The parameter &sigma;<sub>D</sub> defines the pointing error, which is proportional to the norm of &Delta;V.<br>
     * Such an error would result from imperfect angular orientation of the thrust vector.
     * </p>
     *
     * @param sigmaMagnitudeIn
     *        the standard deviation &sigma;<sub>M</sub> on the magnitude (in percent)
     * @param sigmaDirectionIn
     *        the standard deviation &sigma;<sub>D</sub> on the direction (in radians)
     */
    public GatesModel(final double sigmaMagnitudeIn,
            final double sigmaDirectionIn) {
        this.sigmaMagnitude = sigmaMagnitudeIn;
        this.sigmaDirection = sigmaDirectionIn;
    }

    /**
     * Gets the standard deviation &sigma;<sub>M</sub> on the magnitude (in percent).
     *
     * <p>
     * This parameter defines the shutoff error, which is in the direction of &Delta;V and is proportional to its
     * norm.<br>
     * Such an error would result from scale-factor errors in the shutoff system.
     * </p>
     *
     * @return the standard deviation on the magnitude (in percent)
     */
    public double getSigmaMagnitude() {
        return this.sigmaMagnitude;
    }

    /**
     * Gets the standard deviation &sigma;<sub>D</sub> on the direction (in radians).
     *
     * <p>
     * This parameter defines the pointing error, which is orthogonal to &Delta;V and proportional to its norm.<br>
     * Such an error would result from imperfect angular orientation of the thrust vector.
     * </p>
     *
     * @return the standard deviation on the direction (in radians)
     */
    public double getSigmaDirection() {
        return this.sigmaDirection;
    }

    /**
     * Computes the covariance matrix modeling the uncertainty on a maneuver's magnitude and
     * direction.
     *
     * <p>
     * This methods assumes the uncertainty on the maneuver's direction follows a spherical Gaussian distribution around
     * the provided &Delta;V vector. The uncertainty on the maneuver's magnitude is assumed to follow an uncorrelated
     * Gaussian distribution. Both uncertainties are proportional to the norm of &Delta;V.
     * </p>
     *
     * @param deltaV
     *        the maneuver's &Delta;V (in any frame)
     *
     * @return a 3-by-3 covariance matrix modeling the uncertainty on the maneuver's magnitude and
     *         direction (in the same frame as &Delta;V)
     *
     * @throws MathArithmeticException
     *         if the norm of the provided &Delta;V vector is close to zero
     *         (&lt;{@link Precision#EPSILON &epsilon;}), but not exactly equal to zero
     */
    public SymmetricMatrix getCovarianceMatrix3x3(final Vector3D deltaV) {
        return getCovarianceMatrix3x3(deltaV, this.sigmaMagnitude, this.sigmaDirection);
    }

    /**
     * Computes the covariance matrix modeling the uncertainty on an object's position and velocity
     * induced by the uncertainty on a maneuver's magnitude and direction.
     *
     * <p>
     * This methods assumes the uncertainty on the maneuver's direction follows a spherical Gaussian distribution around
     * the provided &Delta;V vector. The uncertainty on the maneuver's magnitude is assumed to follow an uncorrelated
     * Gaussian distribution. Both uncertainties are proportional to the norm of &Delta;V.
     * </p>
     *
     * @param deltaV
     *        the maneuver's &Delta;V (in any frame)
     *
     * @return a 6-by-6 covariance matrix modeling the uncertainty on an object's position and
     *         velocity induced by the uncertainty on the maneuver's magnitude and direction (in the
     *         same frame as &Delta;V)
     *
     * @throws MathArithmeticException
     *         if the norm of the provided &Delta;V vector is close to zero
     *         (&lt;{@link Precision#EPSILON &epsilon;}), but not exactly equal to zero
     */
    public SymmetricMatrix getCovarianceMatrix6x6(final Vector3D deltaV) {
        return getCovarianceMatrix6x6(deltaV, this.sigmaMagnitude, this.sigmaDirection);
    }

    /**
     * Computes the mean &Delta;V vector of the distribution modeling the uncertainty on a
     * maneuver's magnitude and direction.
     *
     * <p>
     * This methods assumes the uncertainty on the maneuver's direction follows a spherical Gaussian distribution around
     * the provided &Delta;V vector. The uncertainty on the maneuver's magnitude is assumed to follow an uncorrelated
     * Gaussian distribution. Both uncertainties are proportional to the norm of &Delta;V.
     * </p>
     *
     * @param deltaV
     *        the maneuver's &Delta;V (in any frame)
     *
     * @return the mean &Delta;V vector of the distribution modeling the uncertainty on a maneuver's
     *         magnitude and direction.
     *
     * @throws MathArithmeticException
     *         if the norm of the provided &Delta;V vector is close to zero
     *         (&lt;{@link Precision#EPSILON &epsilon;}), but not exactly equal to zero
     */
    public Vector3D getMeanDeltaV(final Vector3D deltaV) {
        return getMeanDeltaV(deltaV, this.sigmaDirection);
    }

    /**
     * Computes the covariance matrix modeling the uncertainty on a maneuver's magnitude and
     * direction.
     *
     * <p>
     * This methods assumes the uncertainty on the maneuver's direction follows a spherical Gaussian distribution around
     * the provided &Delta;V vector. The uncertainty on the maneuver's magnitude is assumed to follow an uncorrelated
     * Gaussian distribution. Both uncertainties are proportional to the norm of &Delta;V.
     * </p>
     *
     * @param deltaV
     *        the maneuver's &Delta;V (in any frame)
     * @param sigmaMagnitude
     *        the standard deviation on the magnitude (in percents)
     * @param sigmaDirection
     *        the standard deviation on the direction (in radians)
     *
     *
     * @return a 3-by-3 covariance matrix modeling the uncertainty on the maneuver's magnitude and
     *         direction (in the same frame as &Delta;V)
     *
     * @throws MathArithmeticException
     *         if the norm of the provided &Delta;V vector is close to zero
     *         (&lt;{@link Precision#EPSILON &epsilon;}), but not exactly equal to zero
     */
    public static SymmetricMatrix getCovarianceMatrix3x3(final Vector3D deltaV,
            final double sigmaMagnitude,
            final double sigmaDirection) {
        final SymmetricMatrix out;
        final double magnitude2 = deltaV.getNormSq();

        if (magnitude2 == 0) {
            out = new ArrayRowSymmetricMatrix(3);
        } else {
            // Build the covariance matrix in (magnitude, pitch and yaw)
            final double sigmaMagnitude2 = sigmaMagnitude * sigmaMagnitude;
            final double sigmaDirection2 = sigmaDirection * sigmaDirection;
            final double cst1 = 1. + sigmaMagnitude2;
            final double exp1 = MathLib.exp(-sigmaDirection2);
            final double exp2 = exp1 * exp1;

            final double[][] data = new double[3][3];
            data[0][0] = magnitude2 * (cst1 / 2. * (1 + exp2) - exp1);
            data[1][1] = magnitude2 * (cst1 / 4. * (1 - exp2));
            data[2][2] = magnitude2 * (cst1 / 4. * (1 - exp2));

            final SymmetricMatrix covariance = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, data);

            // Build the rotation matrix used to transform the
            // covariance matrix back to the initial frame.
            final Rotation rotation = new Rotation(Vector3D.PLUS_I, deltaV);
            final RealMatrix transformation = new Array2DRowRealMatrix(rotation.getMatrix(), false);

            // Transform the covariance matrix back to the initial frame
            out = covariance.quadraticMultiplication(transformation);
        }

        return out;
    }

    /**
     * Computes the covariance matrix modeling the uncertainty on an object's position and velocity
     * induced by the uncertainty on a maneuver's magnitude and direction.
     *
     * <p>
     * This methods assumes the uncertainty on the maneuver's direction follows a spherical Gaussian distribution around
     * the provided &Delta;V vector. The uncertainty on the maneuver's magnitude is assumed to follow an uncorrelated
     * Gaussian distribution. Both uncertainties are proportional to the norm of &Delta;V.
     * </p>
     *
     * @param deltaV
     *        the maneuver's &Delta;V (in any frame)
     * @param sigmaMagnitude
     *        the standard deviation on the magnitude (in percents)
     * @param sigmaDirection
     *        the standard deviation on the direction (in radians)
     *
     * @return a 6-by-6 covariance matrix modeling the uncertainty on an object's position and
     *         velocity induced by the uncertainty on the maneuver's magnitude and direction (in the
     *         same frame as &Delta;V)
     *
     * @throws MathArithmeticException
     *         if the norm of the provided &Delta;V vector is close to zero
     *         (&lt;{@link Precision#EPSILON &epsilon;}), but not exactly equal to zero
     */
    public static SymmetricMatrix getCovarianceMatrix6x6(final Vector3D deltaV,
            final double sigmaMagnitude,
            final double sigmaDirection) {
        final SymmetricMatrix covariance = getCovarianceMatrix3x3(deltaV, sigmaMagnitude, sigmaDirection);

        final RealMatrix matrix = new Array2DRowRealMatrix(6, 6);
        matrix.setSubMatrix(covariance.getData(), 3, 3);
        return new ArrayRowSymmetricMatrix(SymmetryType.LOWER, matrix, null, null);
    }

    /**
     * Computes the mean &Delta;V vector of the distribution modeling the uncertainty on a
     * maneuver's magnitude and direction.
     *
     * <p>
     * This methods assumes the uncertainty on the maneuver's direction follows a spherical Gaussian distribution around
     * the provided &Delta;V vector. The uncertainty on the maneuver's magnitude is assumed to follow an uncorrelated
     * Gaussian distribution. Both uncertainties are proportional to the norm of &Delta;V.
     * </p>
     *
     * @param deltaV
     *        the maneuver's &Delta;V (in any frame)
     * @param sigmaDirection
     *        the standard deviation on the direction (in radians)
     *
     * @return the mean &Delta;V vector of the distribution modeling the uncertainty on a maneuver's
     *         magnitude and direction
     *
     * @throws MathArithmeticException
     *         if the norm of the provided &Delta;V vector is close to zero
     *         (&lt;{@link Precision#EPSILON &epsilon;}), but not exactly equal to zero
     */
    public static Vector3D getMeanDeltaV(final Vector3D deltaV,
            final double sigmaDirection) {
        final Vector3D out;
        final double magnitude = deltaV.getNorm();

        if (magnitude == 0.) {
            out = deltaV;
        } else {
            // Mean delta V in the maneuver frame
            final double sigmaDirection2 = MathLib.pow(sigmaDirection, 2);
            final Vector3D meanDeltaV = new Vector3D(magnitude * MathLib.exp(-sigmaDirection2 / 2.0), 0.0, 0.0);

            // Transform the computed delta V back to the initial frame
            final Rotation rotation = new Rotation(Vector3D.PLUS_I, deltaV);
            out = rotation.applyTo(meanDeltaV);
        }

        return out;
    }
}
