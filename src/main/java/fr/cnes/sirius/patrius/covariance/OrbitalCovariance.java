/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2021 CNES
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
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.util.List;
import java.util.Objects;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.SymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Orbital covariance associated with a single orbit.
 * <p>
 * This class associates a {@linkplain Covariance} instance with a given orbit, its date being the
 * date of definition of the orbital covariance. The frame, orbit type (Cartesian, Keplerian, etc)
 * and position angle type (mean, true, eccentric) in which it is expressed can also be specified at
 * construction if they are not the frame, orbit type and position angle type of the associated
 * orbit.
 * </p>
 * <p>
 * The covariance matrix must be at least six by six, where the first six rows/columns represent the
 * uncertainty on the orbital parameters and the remaining ones represent the uncertainty on the
 * additional parameters. The parameter descriptors of these first six rows/columns must be
 * associated to an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate
 * descriptor}, and this descriptor must be mapped to a valid {@linkplain OrbitalCoordinate} (one
 * with the expected orbit type and state vector index).
 * </p>
 *
 * @author Hugo Veuillez (CNES)
 * @author Pierre Seimandi (GMV)
 */
public class OrbitalCovariance extends AbstractOrbitalCovariance<OrbitalCovariance> {

    /** Serial version UID. */
    private static final long serialVersionUID = -2579866942627262828L;

    /** Short argument description for the orbits. */
    private static final String ORBIT = "orbit";

    /** Orbit associated with the covariance. */
    private final Orbit associatedOrbit;

    /**
     * Creates a new instance that associates a covariance matrix with a given orbit, the covariance
     * being defined in the orbit's frame and type, and a {@link PositionAngle#TRUE TRUE} position
     * angle type.
     * <p>
     * The covariance matrix must be at least six by six, where the first six rows/columns represent
     * the uncertainty on the orbital parameters and the remaining ones represent the uncertainty on
     * the additional parameters. The parameter descriptors of these first six rows/columns are
     * automatically associated with an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE
     * orbital coordinate descriptor}, mapped to a valid {@linkplain OrbitalCoordinate}.
     * </p>
     *
     * @param covarianceMatrix
     *        the covariance matrix
     * @param orbit
     *        the orbit associated with the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the dimension of the
     *         covariance matrix is less than 6
     */
    public OrbitalCovariance(final SymmetricPositiveMatrix covarianceMatrix, final Orbit orbit) {
        this(covarianceMatrix, orbit, requireNonNull(orbit, ORBIT).getFrame(), requireNonNull(
                orbit, ORBIT).getType(), PositionAngle.TRUE);
    }

    /**
     * Creates a new instance that associates a covariance matrix with a given orbit, the covariance
     * being defined in the orbit's frame and type, and the specified position angle type.
     * <p>
     * The covariance matrix must be at least six by six, where the first six rows/columns represent
     * the uncertainty on the orbital parameters and the remaining ones represent the uncertainty on
     * the additional parameters. The parameter descriptors of these first six rows/columns are
     * automatically associated with an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE
     * orbital coordinate descriptor}, mapped to a valid {@linkplain OrbitalCoordinate}.
     * </p>
     *
     * @param covarianceMatrix
     *        the covariance matrix
     * @param orbit
     *        the orbit associated with the covariance
     * @param positionAngle
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the dimension of the
     *         covariance matrix is less than 6
     */
    public OrbitalCovariance(final SymmetricPositiveMatrix covarianceMatrix, final Orbit orbit,
            final PositionAngle positionAngle) {
        this(covarianceMatrix, orbit, requireNonNull(orbit, ORBIT).getFrame(), requireNonNull(
                orbit, ORBIT).getType(), positionAngle);
    }

    /**
     * Creates a new instance that associates a covariance matrix with a given orbit, the covariance
     * being defined in the specified frame, orbit type and position angle type.
     * <p>
     * The covariance matrix must be at least six by six, where the first six rows/columns represent
     * the uncertainty on the orbital parameters and the remaining ones represent the uncertainty on
     * the additional parameters. The parameter descriptors of these first six rows/columns are
     * automatically associated with an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE
     * orbital coordinate descriptor}, mapped to a valid {@linkplain OrbitalCoordinate}.
     * </p>
     *
     * @param covarianceMatrix
     *        the covariance matrix
     * @param orbit
     *        the orbit associated with the covariance
     * @param frame
     *        the frame of the covariance
     * @param orbitType
     *        the orbit type of the covariance
     * @param positionAngle
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the dimension of the
     *         covariance matrix is less than 6
     */
    public OrbitalCovariance(final SymmetricPositiveMatrix covarianceMatrix, final Orbit orbit,
            final Frame frame, final OrbitType orbitType, final PositionAngle positionAngle) {
        super(covarianceMatrix, frame, orbitType, positionAngle);
        this.associatedOrbit = requireNonNull(orbit, ORBIT);
        this.checkCovarianceMatrixDimension();
        this.initParameterDescriptors(0, false);
    }

    /**
     * Creates a new instance that associates a covariance matrix with a given orbit, the covariance
     * being defined in the orbit's frame and type, and a {@link PositionAngle#TRUE TRUE} position
     * angle type.
     * <p>
     * The covariance matrix must be at least six by six, where the first six rows/columns represent
     * the uncertainty on the orbital parameters and the remaining ones represent the uncertainty on
     * the additional parameters. The parameter descriptors of these first six rows/columns must be
     * associated to an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate
     * descriptor}, and this descriptor must be mapped to a valid {@linkplain OrbitalCoordinate}
     * (one with the expected orbit type and state vector index).
     * </p>
     *
     * @param covariance
     *        the covariance
     * @param orbit
     *        the orbit associated with the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the dimension of the
     *         covariance matrix is less than 6, or if the parameter descriptors associated with the
     *         orbital parameters are not valid
     */
    public OrbitalCovariance(final Covariance covariance, final Orbit orbit) {
        this(covariance, orbit, requireNonNull(orbit, ORBIT).getFrame(), requireNonNull(orbit,
                ORBIT).getType(), PositionAngle.TRUE);
    }

    /**
     * Creates a new instance that associates a covariance matrix with a given orbit, the covariance
     * being defined in the orbit's frame and type, and the specified position angle type.
     * <p>
     * The covariance matrix must be at least six by six, where the first six rows/columns represent
     * the uncertainty on the orbital parameters and the remaining ones represent the uncertainty on
     * the additional parameters. The parameter descriptors of these first six rows/columns must be
     * associated to an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate
     * descriptor}, and this descriptor must be mapped to a valid {@linkplain OrbitalCoordinate}
     * (one with the expected orbit type and state vector index).
     * </p>
     *
     * @param covariance
     *        the covariance
     * @param orbit
     *        the orbit associated with the covariance
     * @param positionAngle
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the dimension of the
     *         covariance matrix is less than 6, or if the parameter descriptors associated with the
     *         orbital parameters are not valid
     */
    public OrbitalCovariance(final Covariance covariance, final Orbit orbit,
            final PositionAngle positionAngle) {
        this(covariance, orbit, requireNonNull(orbit, ORBIT).getFrame(), requireNonNull(orbit,
                ORBIT).getType(), positionAngle);
    }

    /**
     * Creates a new instance that associates a covariance matrix with a given orbit, the covariance
     * being defined in the specified frame, orbit type and position angle type.
     * <p>
     * The covariance matrix must be at least six by six, where the first six rows/columns represent
     * the uncertainty on the orbital parameters and the remaining ones represent the uncertainty on
     * the additional parameters. The parameter descriptors of these first six rows/columns must be
     * associated to an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate
     * descriptor}, and this descriptor must be mapped to a valid {@linkplain OrbitalCoordinate}
     * (one with the expected orbit type and state vector index).
     * </p>
     *
     * @param covariance
     *        the covariance
     * @param orbit
     *        the orbit associated with the covariance
     * @param frame
     *        the frame of the covariance
     * @param orbitType
     *        the orbit type of the covariance
     * @param positionAngle
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the dimension of the
     *         covariance matrix is less than 6, or if the parameter
     *         descriptors associated with the orbital parameters are not valid
     */
    public OrbitalCovariance(final Covariance covariance, final Orbit orbit, final Frame frame,
            final OrbitType orbitType, final PositionAngle positionAngle) {
        super(covariance, frame, orbitType, positionAngle);
        this.associatedOrbit = requireNonNull(orbit, ORBIT);
        this.checkCovarianceMatrixDimension();
        this.checkParameterDescriptors();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.associatedOrbit.getDate();
    }

    /**
     * Gets the orbit associated with the covariance.
     *
     * @return the orbit associated with the covariance
     */
    public Orbit getOrbit() {
        return this.associatedOrbit;
    }

    /**
     * Shifts the orbital covariance in time by a given duration.
     * <p>
     * The shift of the orbital covariance is based on a simple Keplerian model. It is not intended
     * as a replacement for proper covariance propagation, but it should be sufficient for small
     * time shifts or coarse accuracy.
     * </p>
     *
     * @param dt
     *        the time shift
     * @return the time-shifted orbital covariance
     * @throws PatriusException
     *         if the Keplerian transition matrix cannot be computed
     */
    public OrbitalCovariance shiftedBy(final double dt) throws PatriusException {
        // Associated frame, orbit type, position angle type and covariance
        final Frame frame = this.getFrame();
        final OrbitType orbitType = this.getOrbitType();
        final PositionAngle positionAngle = this.getPositionAngle();
        final Covariance covariance = this.getCovariance();

        // Compute the Keplerian transition matrix
        RealMatrix jacobian = this.associatedOrbit.getJacobian(dt, frame, frame, orbitType,
                orbitType, positionAngle, positionAngle);

        // Extends the transition matrix if the size of the covariance matrix is greater than 6
        final int nbAdditionalParameters = covariance.getSize() - ORBIT_DIMENSION;
        if (nbAdditionalParameters > 0) {
            final RealMatrix identity = MatrixUtils.createRealIdentityMatrix(
                    nbAdditionalParameters, true);
            jacobian = jacobian.concatenateDiagonally(identity);
        }

        // Return the shifted orbital covariance
        // (the parameter descriptors remain unchanged)
        final Orbit shiftedOrbit = this.associatedOrbit.shiftedBy(dt);
        final Covariance shiftedCovariance = covariance.quadraticMultiplication(jacobian,
                covariance.getParameterDescriptors());
        return new OrbitalCovariance(shiftedCovariance, shiftedOrbit, frame, orbitType,
                positionAngle);
    }

    /** {@inheritDoc} */
    @Override
    public OrbitalCovariance transformTo(final Frame destFrame, final OrbitType destOrbitType,
            final PositionAngle destPositionAngle) throws PatriusException {
        final OrbitalCovariance out;
        if (this.getFrame().equals(destFrame) && this.getOrbitType().equals(destOrbitType)
                && this.getPositionAngle().equals(destPositionAngle)) {
            out = this.copy();
        } else {
            final Covariance covariance = this.getCovariance(destFrame, destOrbitType,
                    destPositionAngle);
            out = new OrbitalCovariance(covariance, this.associatedOrbit, destFrame, destOrbitType,
                    destPositionAngle);
        }
        return out;
    }

    /**
     * Transforms this orbital covariance to a local orbital frame centered on the associated orbit.
     * <p>
     * <b>Important:</b><br>
     * The returned covariance is defined in {@linkplain OrbitType#CARTESIAN Cartesian coordinates},
     * in the specified {@linkplain LOFType}. Note that the local orbital frame uses the associated
     * orbit as its {@linkplain PVCoordinatesProvider}, which relies on a simple Keplerian model for
     * the propagation. The LOF built is therefore only valid at the date of the associated orbit,
     * unless it is frozen at this date (the LOF then becomes an inertial frame which can be used at
     * other dates).
     * </p>
     *
     * @param lofType
     *        the type of the local orbital frame
     * @param frozenLof
     *        whether or not the local orbital frame built should be frozen at the date of the
     *        reference orbit
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified local orbital frame
     */
    public OrbitalCovariance transformTo(final LOFType lofType, final boolean frozenLof)
            throws PatriusException {
        return transformTo(this.associatedOrbit, lofType, frozenLof);
    }

    /**
     * Transforms this orbital covariance to a local orbital frame centered on a given orbit.
     * <p>
     * <em>The reference orbit must be defined at the same date as the covariance.</em>
     * </p>
     * <p>
     * <b>Important:</b><br>
     * The returned covariance is defined in {@linkplain OrbitType#CARTESIAN Cartesian coordinates},
     * in the specified {@linkplain LOFType}. Note that the local orbital frame uses the reference
     * orbit as its {@linkplain PVCoordinatesProvider}, which relies on a simple Keplerian model for
     * the propagation. The LOF built is therefore only valid at the date of the reference orbit,
     * unless it is frozen at this date (the LOF then becomes an inertial frame which can be used at
     * other dates).
     * </p>
     *
     * @param referenceOrbit
     *        the orbit used to build the local orbital frame
     * @param lofType
     *        the type of the local orbital frame
     * @param frozenLof
     *        whether or not the local orbital frame built should be frozen at the date of the
     *        reference orbit
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified local orbital frame
     */
    public OrbitalCovariance transformTo(final Orbit referenceOrbit, final LOFType lofType,
            final boolean frozenLof) throws PatriusException {
        // Ensure the reference orbit is not null and defined at the same date as the covariance
        checkOrbit(referenceOrbit);

        // Initialize the reference pseudo inertial frame
        final Frame pseudoInertialFrame;
        if (this.getFrame().isPseudoInertial()) {
            pseudoInertialFrame = this.getFrame();
        } else if (referenceOrbit.getFrame().isPseudoInertial()) {
            pseudoInertialFrame = referenceOrbit.getFrame();
        } else {
            pseudoInertialFrame = FramesFactory.getGCRF();
        }

        // Check if the reference orbit frame is pseudo-inertial, otherwise convert the orbit in the
        // pseudo-inertial frame
        final Orbit orbit;
        if (referenceOrbit.getFrame().isPseudoInertial()) {
            orbit = referenceOrbit;
        } else {
            orbit = referenceOrbit.getType().convertOrbit(referenceOrbit, pseudoInertialFrame);
        }

        // Build the LOF frame and freeze it if requested
        Frame lofFrame = new LocalOrbitalFrame(pseudoInertialFrame, lofType, orbit, lofType.name());
        if (frozenLof) {
            lofFrame = lofFrame.getFrozenFrame(pseudoInertialFrame, orbit.getDate(), "Frozen_"
                    + lofType.name());
        }

        // Return the orbital covariance transformed in the expected frame
        return this.transformTo(lofFrame, OrbitType.CARTESIAN, this.getPositionAngle());
    }

    /**
     * Gets the covariance transformed to the specified frame, orbit type and position angle type.
     * <p>
     * <b>Important:</b><br>
     * The use of non-Cartesian coordinates types may be incompatible with some frames (e.g. local
     * orbital frames).
     * </p>
     *
     * @param destFrame
     *        the destination frame
     * @param destOrbitType
     *        the destination orbit type
     * @param destPositionAngle
     *        the destination position angle type
     * @return the covariance expressed in the specified frame, orbit type and position angle type
     * @throws PatriusException
     *         if the covariance cannot be transformed to the specified frame, orbit type and
     *         position angle type
     */
    private Covariance getCovariance(final Frame destFrame, final OrbitType destOrbitType,
            final PositionAngle destPositionAngle) throws PatriusException {
        // Associated frame, orbit type, position angle type and covariance
        final Frame frame = this.getFrame();
        final OrbitType orbitType = this.getOrbitType();
        final PositionAngle positionAngle = this.getPositionAngle();
        final Covariance covariance = this.getCovariance();

        // Compute the Jacobians matrix
        RealMatrix jacobians = this.associatedOrbit.getJacobian(frame, destFrame, orbitType,
                destOrbitType, positionAngle, destPositionAngle);

        // Extends the transition matrix if the size of the covariance matrix is greater than 6
        final int nbAdditionalParameters = covariance.getSize() - ORBIT_DIMENSION;
        if (nbAdditionalParameters > 0) {
            final RealMatrix identity = MatrixUtils.createRealIdentityMatrix(
                    nbAdditionalParameters, true);
            jacobians = jacobians.concatenateDiagonally(identity);
        }

        // Update the parameter descriptors
        final List<ParameterDescriptor> updatedParameterDescriptors = convertParameterDescriptors(
                this.getParameterDescriptors(), destOrbitType, destPositionAngle);
        return covariance.quadraticMultiplication(jacobians, updatedParameterDescriptors);
    }

    /**
     * Gets a copy of this orbital covariance.
     * <p>
     * This method performs a shallow copy of the associated covariance (shallow copy of the
     * parameter descriptors list, deep copy of the covariance matrix). The orbit, frame, orbit type
     * and position angle type are all passed by reference since they are immutable.
     * </p>
     *
     * @return a copy of this orbital covariance
     * @see Covariance#copy()
     */
    public OrbitalCovariance copy() {
        return new OrbitalCovariance(this.getCovariance().copy(), this.associatedOrbit,
                this.getFrame(), this.getOrbitType(), this.getPositionAngle());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if (object == this) {
            // Same instance
            isEqual = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final OrbitalCovariance other = (OrbitalCovariance) object;

            isEqual = super.equals(other);
            isEqual &= Objects.equals(this.associatedOrbit, other.associatedOrbit);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.associatedOrbit);
    }
}
