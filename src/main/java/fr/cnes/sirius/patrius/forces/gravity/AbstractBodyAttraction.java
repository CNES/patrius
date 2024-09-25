/**
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
 * VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le
 * propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de
 * AbstractEOPHistory
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Abstract body attraction force model.
 */
public abstract class AbstractBodyAttraction extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Parameter name for central attraction coefficient. */
    public static final String K_FACTOR = "gravitationalMultiplicativeFactor";

    /** Serializable UID. */
    private static final long serialVersionUID = 2281191845337825042L;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    protected final boolean computePartialDerivativesWrtPosition;

    /** The gravitational attraction model to consider. */
    private final GravityModel gravityModel;

    /** Multiplicative coefficient. */
    private final Parameter k;

    /**
     * <p>
     * Simple constructor. Only central term of direct body is considered.
     * </p>
     *
     * @param gravityModelIn
     *        the gravitational attraction model to consider
     * @param computePD
     *        true if partial derivatives have to be computed
     * @param k multiplicative factor
     * @throws NullArgumentException
     *         if {@code gravityModelIn} is {@code null}
     */
    public AbstractBodyAttraction(final GravityModel gravityModelIn, final boolean computePD, final double k) {
        this(gravityModelIn, computePD, new Parameter(K_FACTOR, k));
    }

    /**
     * <p>
     * Simple constructor. Only central term of direct body is considered.
     * </p>
     *
     * @param gravityModelIn
     *        the gravitational attraction model to consider
     * @param computePD
     *        true if partial derivatives have to be computed
     * @param k multiplicative factor
     * @throws NullArgumentException
     *         if {@code gravityModelIn} is {@code null}
     */
    public AbstractBodyAttraction(final GravityModel gravityModelIn, final boolean computePD, final Parameter k) {
        super(k);
        // Check if the given gravity model is null
        if (gravityModelIn == null) {
            throw new NullArgumentException(PatriusMessages.NULL_NOT_ALLOWED_DESCRIPTION, "gravityModel");
        }
        this.k = k;
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL,
            this.getClass());
        this.gravityModel = gravityModelIn;
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState state, final TimeDerivativesEquations adder)
        throws PatriusException {
        final Vector3D acceleration = this.computeAcceleration(state);
        adder.addXYZAcceleration(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        final AbsoluteDate date = s.getDate();

        // Get position in body frame
        final Transform scFrameToBodyFrame = s.getFrame().getTransformTo(getGravityModel().getBodyFrame(), date);
        final Vector3D positionInBodyFrame = scFrameToBodyFrame.transformPosition(s.getPVCoordinates().getPosition());

        // Compute acceleration in body frame
        final Vector3D gammaInBodyFrame = computeAcceleration(positionInBodyFrame, date, scFrameToBodyFrame);

        // Convert acceleration in spacecraft state frame
        return scFrameToBodyFrame.getRotation().applyTo(gammaInBodyFrame);
    }

    /**
     * Abstract method to compute the acceleration of the implementation
     *
     * @param positionInBodyFrame position in body frame
     * @param date date
     * @param scFrameToBodyFrame transform from spacecraft frame to body frame
     * @return acceleration in body frame
     * @throws PatriusException thrown if computation failed
     */
    protected abstract Vector3D computeAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date,
                                                    final Transform scFrameToBodyFrame) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState state,
                              final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {
        this.addDAccDState(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(), dAccdPos);
    }

    /**
     * Compute acceleration derivatives with respect to the position of the spacecraft.
     *
     * @param pos position of the spacecraft
     * @param frame frame in which the acceleration derivatives are computed
     * @param date date
     * @param dAccdPos acceleration derivatives with respect to position
     * @exception PatriusException if derivatives cannot be computed
     */
    public void
        addDAccDState(final Vector3D pos, final Frame frame, final AbsoluteDate date, final double[][] dAccdPos)
            throws PatriusException {
        // Check if compute partial derivatives with respect to position are supposed to be computed
        if (this.computePartialDerivativesWrtPosition) {
            // Retrieve the body frame
            final Frame bodyFrame = this.gravityModel.getBodyFrame();
            // Get position in body frame
            final Transform transformFromScFrameToBodyFrame = frame.getTransformTo(bodyFrame, date);
            final Vector3D positionInBodyFrame = transformFromScFrameToBodyFrame.transformPosition(pos);
            // Compute derivative in the body frame
            final double[][] dAccDPositionInBodyFrame = this.gravityModel.computeDAccDPos(positionInBodyFrame, date);
            // Compute derivative in the spacecraft frame
            final double[][] dAccDPositionInScFrame = convertMatrixFromAToB(dAccDPositionInBodyFrame,
                transformFromScFrameToBodyFrame);
            // The only non-null contribution for this force is dAcc/dPos
            final double kValue = this.k.getValue();
            dAccdPos[0][0] += kValue * dAccDPositionInScFrame[0][0];
            dAccdPos[0][1] += kValue * dAccDPositionInScFrame[0][1];
            dAccdPos[0][2] += kValue * dAccDPositionInScFrame[0][2];
            dAccdPos[1][0] += kValue * dAccDPositionInScFrame[1][0];
            dAccdPos[1][1] += kValue * dAccDPositionInScFrame[1][1];
            dAccdPos[1][2] += kValue * dAccDPositionInScFrame[1][2];
            dAccdPos[2][0] += kValue * dAccDPositionInScFrame[2][0];
            dAccdPos[2][1] += kValue * dAccDPositionInScFrame[2][1];
            dAccdPos[2][2] += kValue * dAccDPositionInScFrame[2][2];
        }
    }

    /**
     * Convert a 3x3 matrix from a frame A to a frame B.
     *
     * @param matrixInFrameA the matrix expressed in the frame A
     * @param transformFromFrameBToFrameA the transform needed to pass from the frame B to the frame A
     * @return the matrix expressed in the frame B
     */
    private static double[][] convertMatrixFromAToB(final double[][] matrixInFrameA,
                                                    final Transform transformFromFrameBToFrameA) {
        switch (PatriusConfiguration.getPatriusCompatibilityMode()) {
            case OLD_MODELS: {
                if (transformFromFrameBToFrameA.equals(Transform.IDENTITY)) {
                    // Immediate return if transform is identity
                    return matrixInFrameA;
                }

                // Jacobian matrix from B to A
                final double[][] jac = new double[6][6];
                transformFromFrameBToFrameA.getJacobian(jac);
                // Keep the useful part (3x3 for position)
                final double[][] jac33Transposed = new double[3][3];
                for (int i = 0; i < jac33Transposed.length; i++) {
                    for (int j = 0; j < jac33Transposed[i].length; j++) {
                        jac33Transposed[i][j] = jac[j][i];
                    }
                }
                // Build real matrix for multiplication
                final Array2DRowRealMatrix matrixInARealMatrix = new Array2DRowRealMatrix(matrixInFrameA, false);
                final Array2DRowRealMatrix jacFromAToBMatrix = new Array2DRowRealMatrix(jac33Transposed, false);
                // Transformation
                final RealMatrix matrixInB =
                    jacFromAToBMatrix.multiply(matrixInARealMatrix).multiply(jacFromAToBMatrix, true);

                return matrixInB.getData(true);
            }
            case NEW_MODELS:
            case MIXED_MODELS:
                if (transformFromFrameBToFrameA.equals(Transform.IDENTITY)) {
                    // Immediate return if transform is identity
                    return matrixInFrameA;
                }

                final double[][] matrixInB = new double[3][3];
                final Rotation rotFromAToB = transformFromFrameBToFrameA.getRotation();

                // First rotation (rotate lines)
                final double[][] intermediateMatrix = new double[3][3];
                rotFromAToB.applyTo(matrixInFrameA[0], intermediateMatrix[0]);
                rotFromAToB.applyTo(matrixInFrameA[1], intermediateMatrix[1]);
                rotFromAToB.applyTo(matrixInFrameA[2], intermediateMatrix[2]);

                // Second rotation (rotate columns)
                final double[] intermediateVectorIn = new double[3];
                final double[] intermediateVectorOut = new double[3];

                // Rotation for index 0
                extractColumnInMatrix(intermediateMatrix, intermediateVectorIn, 0);
                rotFromAToB.applyTo(intermediateVectorIn, intermediateVectorOut);
                copyVectorInMatrixColumn(matrixInB, intermediateVectorOut, 0);

                // Rotation for index 1
                extractColumnInMatrix(intermediateMatrix, intermediateVectorIn, 1);
                rotFromAToB.applyTo(intermediateVectorIn, intermediateVectorOut);
                copyVectorInMatrixColumn(matrixInB, intermediateVectorOut, 1);

                // Rotation for index 2
                extractColumnInMatrix(intermediateMatrix, intermediateVectorIn, 2);
                rotFromAToB.applyTo(intermediateVectorIn, intermediateVectorOut);
                copyVectorInMatrixColumn(matrixInB, intermediateVectorOut, 2);

                return matrixInB;
                
            default:
                throw new IllegalArgumentException(
                    "Unsupported compatibility mode : " + PatriusConfiguration.getPatriusCompatibilityMode());
        }
    }

    /**
     * Utility function to extract the column of a 3x3 matrix.
     *
     * @param matrix
     *        The 3x3 matrix
     * @param vector
     *        The vector to extract
     * @param index
     *        The index of the column
     */
    private static void extractColumnInMatrix(final double[][] matrix, final double[] vector, final int index) {
        vector[0] = matrix[0][index];
        vector[1] = matrix[1][index];
        vector[2] = matrix[2][index];
    }

    /**
     * Utility function to copy a vector into a matrix.
     *
     * @param matrix
     *        The matrix where the vector is copied
     * @param vector
     *        The vector to copy
     * @param index
     *        The index of the column
     */
    private static void copyVectorInMatrixColumn(final double[][] matrix, final double[] vector, final int index) {
        matrix[0][index] = vector[0];
        matrix[1][index] = vector[1];
        matrix[2][index] = vector[2];
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDParam(final SpacecraftState state, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        this.addDAccDParam(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(), param,
            dAccdParam);
    }

    /**
     * Compute acceleration derivatives with respect to additional parameters.
     *
     * @param pos position of the spacecraft
     * @param frame frame in which the acceleration derivatives are computed
     * @param date date
     * @param param the parameter with respect to which derivatives are required
     * @param dAccdParam acceleration derivatives with respect to specified parameters
     * @exception PatriusException if derivatives cannot be computed
     */
    public void addDAccDParam(final Vector3D pos, final Frame frame,
                              final AbsoluteDate date, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        if (this.k.equals(param)) {
            // Get position in body frame
            final Transform scFrameToBodyFrame = frame.getTransformTo(this.gravityModel.getBodyFrame(), date);
            final Vector3D positionInBodyFrame = scFrameToBodyFrame.transformPosition(pos);

            final double kValue = this.k.getValue();
            final Vector3D acc = this.computeAcceleration(positionInBodyFrame, date, scFrameToBodyFrame);
            dAccdParam[0] += acc.getX() / kValue;
            dAccdParam[1] += acc.getY() / kValue;
            dAccdParam[2] += acc.getZ() / kValue;
        } else {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /**
     * Get the force multiplicative factor.
     *
     * @return the force multiplicative factor
     */
    public double getMultiplicativeFactor() {
        return this.k.getValue();
    }

    /**
     * Get the force multiplicative factor parameter
     *
     * @return the force multiplicative factor parameter
     */
    public Parameter getMultiplicativeFactorParameter() {
        return this.k;
    }

    /**
     * Set the multiplicative factor.
     *
     * @param factor the force multiplicative factor to set.
     */
    public void setMultiplicativeFactor(final double factor) {
        this.k.setValue(factor);
    }

    /**
     * Get the gravitational attraction model.
     *
     * @return the gravitational attraction model
     */
    public GravityModel getGravityModel() {
        return this.gravityModel;
    }

}
