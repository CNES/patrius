/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:426:30/10/2015: Add asbtract method convertType implemented in each item of the enum
 * VERSION::DM:1798:10/12/2018: Changes after AlternateEquinoctialParameters creation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.AlternateEquinoctialCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ApsisRadiusCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CircularCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquatorialCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquinoctialCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Enumerate for {@link Orbit orbital} parameters types.
 */
public enum OrbitType {

    /** Type for propagation in {@link CartesianOrbit Cartesian parameters}. */
    CARTESIAN {
        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CartesianOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit convertOrbit(final Orbit initOrbit, final Frame frame) throws PatriusException {
            return new CartesianOrbit(initOrbit.getPVCoordinates(frame), frame,
                    initOrbit.getDate(), initOrbit.getMu());
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                final double[] stateVector) {
            final PVCoordinates pv = orbit.getPVCoordinates();
            final Vector3D p = pv.getPosition();
            final Vector3D v = pv.getVelocity();

            stateVector[0] = p.getX();
            stateVector[1] = p.getY();
            stateVector[2] = p.getZ();
            stateVector[3] = v.getX();
            stateVector[4] = v.getY();
            stateVector[5] = v.getZ();
        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                final AbsoluteDate date, final double mu, final Frame frame) {
            final Vector3D p = new Vector3D(stateVector[0], stateVector[1], stateVector[2]);
            final Vector3D v = new Vector3D(stateVector[3], stateVector[4], stateVector[5]);
            return new CartesianOrbit(new PVCoordinates(p, v), frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public CartesianCoordinate getCoordinateType(final int stateVectorIndex,
                final PositionAngle positionAngle) {
            return CartesianCoordinate.valueOf(stateVectorIndex);
        }
    },

    /** Type for propagation in {@link CircularOrbit circular parameters}. */
    CIRCULAR {
        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CircularOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit convertOrbit(final Orbit initOrbit, final Frame frame) throws PatriusException {
            return new CircularOrbit(initOrbit.getPVCoordinates(frame), frame, initOrbit.getDate(),
                    initOrbit.getMu());
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                final double[] stateVector) {
            final CircularOrbit circularOrbit = (CircularOrbit) OrbitType.CIRCULAR
                    .convertType(orbit);

            stateVector[0] = circularOrbit.getA();
            stateVector[1] = circularOrbit.getCircularEx();
            stateVector[2] = circularOrbit.getCircularEy();
            stateVector[3] = circularOrbit.getI();
            stateVector[4] = circularOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = circularOrbit.getAlpha(type);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                final AbsoluteDate date, final double mu, final Frame frame) {
            return new CircularOrbit(stateVector[0], stateVector[1], stateVector[2],
                    stateVector[3], stateVector[4], stateVector[5], type, frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public CircularCoordinate getCoordinateType(final int stateVectorIndex,
                final PositionAngle positionAngle) {
            return CircularCoordinate.valueOf(stateVectorIndex, positionAngle);
        }
    },

    /** Type for propagation in {@link EquinoctialOrbit equinoctial parameters}. */
    EQUINOCTIAL {
        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new EquinoctialOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit convertOrbit(final Orbit initOrbit, final Frame frame) throws PatriusException {
            return new EquinoctialOrbit(initOrbit.getPVCoordinates(frame), frame,
                    initOrbit.getDate(), initOrbit.getMu());
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                final double[] stateVector) {
            final EquinoctialOrbit equinoctialOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL
                    .convertType(orbit);

            stateVector[0] = equinoctialOrbit.getA();
            stateVector[1] = equinoctialOrbit.getEquinoctialEx();
            stateVector[2] = equinoctialOrbit.getEquinoctialEy();
            stateVector[3] = equinoctialOrbit.getHx();
            stateVector[4] = equinoctialOrbit.getHy();
            stateVector[5] = equinoctialOrbit.getL(type);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                final AbsoluteDate date, final double mu, final Frame frame) {
            return new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2],
                    stateVector[3], stateVector[4], stateVector[5], type, frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public EquinoctialCoordinate getCoordinateType(final int stateVectorIndex,
                final PositionAngle positionAngle) {
            return EquinoctialCoordinate.valueOf(stateVectorIndex, positionAngle);
        }

    },

    /**
     * Type for propagation in {@link AlternateEquinoctialOrbit Alternate equinoctial parameters}.
     */
    ALTERNATE_EQUINOCTIAL {
        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            Orbit result = orbit;
            if (orbit.getType() != this) {
                result = new AlternateEquinoctialOrbit(orbit);
            }
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public Orbit convertOrbit(final Orbit initOrbit, final Frame frame) throws PatriusException {
            return new AlternateEquinoctialOrbit(initOrbit.getPVCoordinates(frame), frame,
                    initOrbit.getDate(), initOrbit.getMu());
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                final double[] stateVector) {
            final AlternateEquinoctialOrbit alteternateEquinoctialOrbit = 
                    (AlternateEquinoctialOrbit) OrbitType.ALTERNATE_EQUINOCTIAL.convertType(orbit);

            stateVector[0] = alteternateEquinoctialOrbit.getN();
            stateVector[1] = alteternateEquinoctialOrbit.getEquinoctialEx();
            stateVector[2] = alteternateEquinoctialOrbit.getEquinoctialEy();
            stateVector[3] = alteternateEquinoctialOrbit.getHx();
            stateVector[4] = alteternateEquinoctialOrbit.getHy();
            stateVector[5] = alteternateEquinoctialOrbit.getL(type);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                final AbsoluteDate date, final double mu, final Frame frame) {
            return new AlternateEquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2],
                    stateVector[3], stateVector[4], stateVector[5], type, frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public AlternateEquinoctialCoordinate getCoordinateType(final int stateVectorIndex,
                final PositionAngle positionAngle) {
            return AlternateEquinoctialCoordinate.valueOf(stateVectorIndex, positionAngle);
        }
    },

    /** Type for propagation in {@link ApsisOrbit Apsis parameters}. */
    APSIS {
        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new ApsisOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit convertOrbit(final Orbit initOrbit, final Frame frame) throws PatriusException {
            return new ApsisOrbit(initOrbit.getPVCoordinates(frame), frame, initOrbit.getDate(),
                    initOrbit.getMu());
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                final double[] stateVector) {
            final ApsisOrbit apsidesOrbit = (ApsisOrbit) OrbitType.APSIS.convertType(orbit);

            stateVector[0] = apsidesOrbit.getPeriapsis();
            stateVector[1] = apsidesOrbit.getApoapsis();
            stateVector[2] = apsidesOrbit.getI();
            stateVector[3] = apsidesOrbit.getPerigeeArgument();
            stateVector[4] = apsidesOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = apsidesOrbit.getAnomaly(type);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                final AbsoluteDate date, final double mu, final Frame frame) {
            return new ApsisOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                    stateVector[4], stateVector[5], type, frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public ApsisRadiusCoordinate getCoordinateType(final int stateVectorIndex,
                final PositionAngle positionAngle) {
            return ApsisRadiusCoordinate.valueOf(stateVectorIndex, positionAngle);
        }
    },

    /** Type for propagation in {@link EquatorialOrbit Equatorial parameters}. */
    EQUATORIAL {
        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new EquatorialOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit convertOrbit(final Orbit initOrbit, final Frame frame) throws PatriusException {
            return new EquatorialOrbit(initOrbit.getPVCoordinates(frame), frame,
                    initOrbit.getDate(), initOrbit.getMu());
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                final double[] stateVector) {
            final EquatorialOrbit equatorialOrbit = (EquatorialOrbit) OrbitType.EQUATORIAL
                    .convertType(orbit);

            stateVector[0] = equatorialOrbit.getA();
            stateVector[1] = equatorialOrbit.getE();
            stateVector[2] = equatorialOrbit.getPomega();
            stateVector[3] = equatorialOrbit.getIx();
            stateVector[4] = equatorialOrbit.getIy();
            stateVector[5] = equatorialOrbit.getAnomaly(type);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                final AbsoluteDate date, final double mu, final Frame frame) {
            return new EquatorialOrbit(stateVector[0], stateVector[1], stateVector[2],
                    stateVector[3], stateVector[4], stateVector[5], type, frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public EquatorialCoordinate getCoordinateType(final int stateVectorIndex,
                final PositionAngle positionAngle) {
            return EquatorialCoordinate.valueOf(stateVectorIndex, positionAngle);
        }
    },

    /** Type for propagation in {@link KeplerianOrbit Keplerian parameters}. */
    KEPLERIAN {
        /** {@inheritDoc} */
        @Override
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new KeplerianOrbit(orbit);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit convertOrbit(final Orbit initOrbit, final Frame frame) throws PatriusException {
            return new KeplerianOrbit(initOrbit.getPVCoordinates(frame), frame,
                    initOrbit.getDate(), initOrbit.getMu());
        }

        /** {@inheritDoc} */
        @Override
        public void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
                final double[] stateVector) {
            final KeplerianOrbit keplerianOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN
                    .convertType(orbit);

            stateVector[0] = keplerianOrbit.getA();
            stateVector[1] = keplerianOrbit.getE();
            stateVector[2] = keplerianOrbit.getI();
            stateVector[3] = keplerianOrbit.getPerigeeArgument();
            stateVector[4] = keplerianOrbit.getRightAscensionOfAscendingNode();
            stateVector[5] = keplerianOrbit.getAnomaly(type);
        }

        /** {@inheritDoc} */
        @Override
        public Orbit mapArrayToOrbit(final double[] stateVector, final PositionAngle type,
                final AbsoluteDate date, final double mu, final Frame frame) {
            return new KeplerianOrbit(stateVector[0], stateVector[1], stateVector[2],
                    stateVector[3], stateVector[4], stateVector[5], type, frame, date, mu);
        }

        /** {@inheritDoc} */
        @Override
        public KeplerianCoordinate getCoordinateType(final int stateVectorIndex,
                final PositionAngle positionAngle) {
            return KeplerianCoordinate.valueOf(stateVectorIndex, positionAngle);
        }
    };

    /**
     * Convert an orbit to the instance type.
     * <p>
     * The returned orbit is the specified instance itself if its type already matches, otherwise a
     * new orbit of the proper type created
     * </p>
     *
     * @param orbit
     *        orbit to convert
     * @return converted orbit with type guaranteed to match (so it can be cast safely)
     */
    public abstract Orbit convertType(final Orbit orbit);

    /**
     * Convert orbit to state array.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the
     * {@link Orbit#getJacobianWrtCartesian(fr.cnes.sirius.patrius.orbits.PositionAngle, double[][])
     * Orbit.getJacobianWrtCartesian} method for the corresponding orbit type in terms of parameters
     * order and meaning.
     * </p>
     *
     * @param orbit
     *        orbit to map
     * @param type
     *        type of the angle
     * @param stateVector
     *        flat array into which the state vector should be mapped
     */
    public abstract void mapOrbitToArray(final Orbit orbit, final PositionAngle type,
            final double[] stateVector);

    /**
     * Convert state array to orbital parameters.
     * <p>
     * Note that all implementations of this method <em>must</em> be consistent with the
     * implementation of the
     * {@link Orbit#getJacobianWrtCartesian(fr.cnes.sirius.patrius.orbits.PositionAngle, double[][])
     * Orbit.getJacobianWrtCartesian} method for the corresponding orbit type in terms of parameters
     * order and meaning.
     * </p>
     *
     * @param array
     *        state as a flat array
     * @param type
     *        type of the angle
     * @param date
     *        integration date
     * @param mu
     *        central attraction coefficient used for propagation (m<sup>3</sup>/s<sup>2</sup>)
     * @param frame
     *        frame in which integration is performed
     * @return orbit corresponding to the flat array as a space dynamics object
     */
    public abstract Orbit mapArrayToOrbit(final double[] array, final PositionAngle type,
            final AbsoluteDate date, final double mu, final Frame frame);

    /**
     * Convert an orbit from a given orbit type to an other in a wished frame.
     *
     * @param initOrbit
     *        the input orbit
     * @param frame
     *        the frame where the input orbit is converted it is not necessary inertial or
     *        pseudo-inertial
     * @return the converted orbit
     * @throws PatriusException
     *         if transformation between frames cannot be computed
     */
    public abstract Orbit convertOrbit(final Orbit initOrbit, final Frame frame)
            throws PatriusException;

    /**
     * Gets the coordinate type associated with a given state vector index.
     *
     * @param stateVectorIndex
     *        the state vector index
     * @param positionAngle
     *        the position angle type
     * @return the coordinate type associated with the provided state vector index
     * @throws IllegalArgumentException
     *         if the provided state vector index is not between 0 and 5 (included)
     */
    public abstract OrbitalCoordinate getCoordinateType(final int stateVectorIndex,
            final PositionAngle positionAngle);
}
