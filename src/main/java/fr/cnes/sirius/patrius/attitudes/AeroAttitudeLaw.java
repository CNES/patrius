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
 * @history created 23/01/17
 *
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:595:23/01/2017:Creation of AeroAttitudeLaw class
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.attitudes.kinematics.AbstractOrientationFunction;
import fr.cnes.sirius.patrius.attitudes.kinematics.OrientationFunction;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.analysis.differentiation.FiniteDifferencesDifferentiator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParameterizableFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ReentryParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class defining an aerodynamic attitude law by angle of attack, sideslip and velocity roll.
 *
 * @author Emmanuel Bignon
 * @version $Id: AeroAttitudeLaw.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 3.4
 */
public class AeroAttitudeLaw extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -5371693012337184284L;

    /** Default step for finite differences spin computation. */
    private static final double DEFAULT_STEP_VEL = 0.2;

    /** Default step for finite differences spin computation. */
    private static final double DEFAULT_STEP_ACC = 0.001;

    /** Angle of attack. */
    private final IParameterizableFunction angleOfAttack;

    /** Sideslip. */
    private final IParameterizableFunction sideSlip;

    /** Roll velocity. */
    private final IParameterizableFunction rollVelocity;

    /** Earth shape. */
    private final OneAxisEllipsoid earthBodyShape;

    /** Step for finite differences spin computation. */
    private final double stepSpin;

    /** Step for finite differences spin derivative computation. */
    private final double stepAcc;

    /**
     * Constructor.
     *
     * @param angleofattack
     *        angle of attack in [-Pi;Pi]
     * @param sideslip
     *        sideslip angle in ]-Pi/2; Pi/2[
     * @param rollVel
     *        roll velocity in [-Pi;Pi]
     * @param earthShape
     *        Earth shape
     * @throws PatriusException
     *         if some specific error occurs
     */
    public AeroAttitudeLaw(final double angleofattack, final double sideslip, final double rollVel,
                           final OneAxisEllipsoid earthShape)
        throws PatriusException {
        this(angleofattack, sideslip, rollVel, earthShape, DEFAULT_STEP_VEL, DEFAULT_STEP_ACC);
    }

    /**
     * Constructor.
     *
     * @param angleofattack
     *        angle of attack
     * @param sideslip
     *        sideslip angle
     * @param rollVel
     *        roll velocity
     * @param earthShape
     *        Earth shape
     */
    public AeroAttitudeLaw(final IParameterizableFunction angleofattack, final IParameterizableFunction sideslip,
                           final IParameterizableFunction rollVel, final OneAxisEllipsoid earthShape) {
        this(angleofattack, sideslip, rollVel, earthShape, DEFAULT_STEP_VEL, DEFAULT_STEP_ACC);
    }

    /**
     * Constructor with parameterizable delta-time for spin and acceleration computation.
     *
     * @param angleofattack
     *        angle of attack
     * @param sideslip
     *        sideslip angle
     * @param rollVel
     *        roll velocity
     * @param earthShape
     *        Earth shape
     * @param dtSpin
     *        step for finite differences spin computation
     * @param dtAcc
     *        step for finite differences spin derivative computation
     */
    public AeroAttitudeLaw(final double angleofattack, final double sideslip, final double rollVel,
                           final OneAxisEllipsoid earthShape, final double dtSpin, final double dtAcc) {
        this(new ConstantFunction(angleofattack), new ConstantFunction(sideslip), new ConstantFunction(rollVel),
                earthShape, dtSpin, dtAcc);
    }

    /**
     * Constructor with parameterizable delta-time for spin and acceleration computation.
     *
     * @param angleofattack
     *        angle of attack
     * @param sideslip
     *        sideslip angle
     * @param rollVel
     *        roll velocity
     * @param earthShape
     *        Earth shape
     * @param dtSpin
     *        step for finite differences spin computation
     * @param dtAcc
     *        step for finite differences spin derivative computation
     */
    public AeroAttitudeLaw(final IParameterizableFunction angleofattack, final IParameterizableFunction sideslip,
                           final IParameterizableFunction rollVel, final OneAxisEllipsoid earthShape,
                           final double dtSpin, final double dtAcc) {
        super();
        this.angleOfAttack = angleofattack;
        this.sideSlip = sideslip;
        this.rollVelocity = rollVel;
        this.earthBodyShape = earthShape;
        this.stepSpin = dtSpin;
        this.stepAcc = dtAcc;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
        throws PatriusException {

        // Get rotation
        final Rotation rot = computeOrientation(pvProv, date, frame);

        // Get spin (using finite differences)
        final Vector3DFunction spinFunction = getSpinFunction(pvProv, frame, date);
        final Vector3D spin = spinFunction.getVector3D(date);

        // Get spin derivative
        Vector3D spinDerivative = null;
        if (getSpinDerivativesComputation()) {
            spinDerivative = spinFunction.nthDerivative(1).getVector3D(date);
        }
        return new Attitude(frame, new TimeStampedAngularCoordinates(date, rot, spin, spinDerivative));
    }

    /**
     * Compute orientation.
     *
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which orientation function is computed
     * @param date
     *        date
     * @return orientation at date
     * @throws PatriusException
     *         if direction could not be computed
     */
    private Rotation computeOrientation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
        throws PatriusException {

        // PV coordinates expressed in ITRF
        final PVCoordinates pvCoordinates = pvProv.getPVCoordinates(date, this.earthBodyShape.getBodyFrame());

        // Cartesian Parameters
        final CartesianParameters cartParam = new CartesianParameters(pvCoordinates, Constants.EGM96_EARTH_MU);
        final Orbit orbit = new CartesianOrbit(cartParam, this.earthBodyShape.getBodyFrame(), date);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Get reentry Parameters
        final ReentryParameters reentryParam = cartParam.getReentryParameters(
            this.earthBodyShape.getEquatorialRadius(), this.earthBodyShape.getFlattening());
        final EllipsoidPoint point = new EllipsoidPoint(this.earthBodyShape,
            this.earthBodyShape.getLLHCoordinatesSystem(),
            reentryParam.getLatitude(), reentryParam.getLongitude(), reentryParam.getAltitude(), "");

        // Topocentric frame
        final Frame topocentricFrame = new TopocentricFrame(point, 0, "TopoFrame");

        // Rotation 1: from reference Frame to Topocentric frame
        final Rotation fromRefFrameToTopoFrame = frame.getTransformTo(topocentricFrame, date).getRotation();

        // Rotation 2: from Topocentric frame to Aircraft-carried normal earth
        // axis system frame
        final Rotation fromTopoFrameToAircraftNormalEarthAxisFrame = new Rotation(RotationOrder.XYZ, FastMath.PI, 0, 0);

        // Rotation 3: from Aircraft-carried normal earth axis system frame to
        // Aircraft orientation
        // angles

        // Compute yaw, pitch and Roll
        final double[] aircraftAngles = computeYawPitchRoll(state, reentryParam);

        final Rotation fromAircraftNormalEarthAxisFrameToAircraftFrame =
            new Rotation(RotationOrder.ZYX, aircraftAngles[0], aircraftAngles[1], aircraftAngles[2]);

        // Build global rotation (reference frame to aircraft frame)
        return fromRefFrameToTopoFrame.applyTo(fromTopoFrameToAircraftNormalEarthAxisFrame)
            .applyTo(fromAircraftNormalEarthAxisFrameToAircraftFrame);
    }

    /**
     * Method to compute aircraft orientation angles with respect the Aircraft-carried normal earth axis system frame:
     * OX towards local North and OY towards local East.
     *
     * @param state
     *        state
     * @param reentryParam
     *        reentry parameters
     * @return yaw, pitch and roll
     */
    private double[] computeYawPitchRoll(final SpacecraftState state, final ReentryParameters reentryParam) {

        // Rotation 1: Aircraft-carried normal earth axis system frame ->
        // Aerodynamic frame
        final Rotation earthToAero =
            new Rotation(RotationOrder.ZYX, reentryParam.getAzimuth(), reentryParam.getSlope(),
                this.rollVelocity.value(state));

        // Rotation 2: Aircraft frame -> Aerodynamic frame
        final Rotation aircraftToAero = new Rotation(RotationOrder.YZX, -this.angleOfAttack.value(state),
            this.sideSlip.value(state), 0);

        // Rotation 3: Aerodynamic frame -> Aircraft frame
        final Rotation aeroToAircraft = aircraftToAero.revert();

        // Rotation 4: Aircraft-carried normal earth axis system frame ->
        // Aircraft frame
        final Rotation earthToAircraft = earthToAero.applyTo(aeroToAircraft);

        // Aircraft yaw, pitch and roll
        final double[] angles = earthToAircraft.getAngles(RotationOrder.ZYX);

        // If pitch = +/-PI/2.. yaw set to 0 (arbitrarily)
        if (Precision.equals(angles[1], FastMath.PI / 2.) || Precision.equals(angles[1], -FastMath.PI / 2.)) {
            angles[0] = 0.;
        }

        // Yaw adjustment
        if (angles[0] > FastMath.PI) {
            angles[0] = angles[0] - 2. * FastMath.PI;
        } else if (angles[0] < -FastMath.PI) {
            angles[0] = angles[0] + 2. * FastMath.PI;
        }

        // Roll adjustment
        if (angles[2] > FastMath.PI) {
            angles[2] = angles[2] - 2. * FastMath.PI;
        } else if (angles[2] < -FastMath.PI) {
            angles[2] = angles[2] + 2. * FastMath.PI;
        }

        // Return angles
        return angles;
    }

    /**
     * Build orientation function.
     *
     * @param pvProv
     *        position-velocity provider around current date
     * @param frame
     *        reference frame from which orientation function is computed
     * @param zeroAbscissa
     *        the date for which x=0 for orientation function of date
     * @return the orientation (quaternion) function of the date
     */
    private OrientationFunction getOrientationFunction(final PVCoordinatesProvider pvProv, final Frame frame,
                                                       final AbsoluteDate zeroAbscissa) {
        return new AbstractOrientationFunction(zeroAbscissa){

            /** {@inheritDoc} */
            @Override
            public Rotation getOrientation(final AbsoluteDate date) throws PatriusException {
                return AeroAttitudeLaw.this.computeOrientation(pvProv, date, frame);
            }
        };
    }

    /**
     * Build spin function.
     *
     * @param pvProv
     *        position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @return spin function of date relative
     */
    private Vector3DFunction getSpinFunction(final PVCoordinatesProvider pvProv, final Frame frame,
                                             final AbsoluteDate zeroAbscissa) {

        return new AbstractVector3DFunction(zeroAbscissa, new FiniteDifferencesDifferentiator(4, this.stepAcc)){
            /** {@inheritDoc} */
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {

                final OrientationFunction rot = AeroAttitudeLaw.this.getOrientationFunction(pvProv, frame, date);
                return rot.estimateRateFunction(AeroAttitudeLaw.this.stepSpin, AbsoluteDateInterval.INFINITY)
                    .getVector3D(date);
            }
        };
    }

    /**
     * Method to compute aerodynamic frame orientation angles with respect aircraft frame.
     *
     * @param slopeVel
     *        Slope velocity
     * @param azimuthVel
     *        Azimuth velocity
     * @param yaw
     *        Aircraft yaw
     * @param pitch
     *        Aircraft pitch
     * @param roll
     *        Aircraft roll
     * @return Angle of Attack, Side slip angle, Bank angle
     * @throws PatriusException
     *         side slip angle doesn't permit to compute bank angle
     */
    public static double[] aircraftToAero(final double slopeVel, final double azimuthVel, final double yaw,
                                          final double pitch, final double roll)
        throws PatriusException {

        // Velocity direction expressed in Aircraft-carried normal earth axis
        // system frame
        final double[] sincosSlope = MathLib.sinAndCos(slopeVel);
        final double sinSlope = sincosSlope[0];
        final double cosSlope = sincosSlope[1];
        final double[] sincosAzimut = MathLib.sinAndCos(azimuthVel);
        final double sinAzimut = sincosAzimut[0];
        final double cosAzimut = sincosAzimut[1];
        final Vector3D velEarthVec = new Vector3D(cosSlope * cosAzimut, cosSlope * sinAzimut, -sinSlope);

        // Rotation 1: Aircraft-carried normal earth axis system frame ->
        // Aircraft frame
        final Rotation earthToAircraft = new Rotation(RotationOrder.ZYX, yaw, pitch, roll);

        // Velocity direction expressed in Aircraft frame
        final Vector3D velAircraftVec = earthToAircraft.applyInverseTo(velEarthVec);

        // Side slip angle
        final double sideSlipAngle = MathLib.asin(velAircraftVec.getY() / velAircraftVec.getNorm());

        // Exception
        if (Comparators.equals(sideSlipAngle, FastMath.PI / 2) || Comparators.equals(sideSlipAngle, -FastMath.PI / 2)) {
            throw new PatriusException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);

        }

        // Angle of Attack
        final double angleOfAttack = MathLib.atan2(velAircraftVec.getZ(), velAircraftVec.getX());

        // Rotation 2: Aicraft frame -> Aerodynamic frame
        final Rotation aircraftToAero = new Rotation(RotationOrder.YZX, -angleOfAttack, sideSlipAngle, 0);

        // Rotation 3: Aircraft-carried normal earth axis system frame ->
        // Aerodynamic frame
        final Rotation earthToAero = earthToAircraft.applyTo(aircraftToAero);

        // Compute bank angle
        final double bankAngle = earthToAero.getAngles(RotationOrder.ZYX)[2];

        // Results
        // AoA, Sidesplip, Bank angle
        final double[] velocityAngles = new double[3];
        velocityAngles[0] = angleOfAttack;
        velocityAngles[1] = sideSlipAngle;
        velocityAngles[2] = bankAngle;

        return velocityAngles;

    }

    /**
     * Getter for the angle of attack.
     *
     * @param state
     *        state
     * @return the angle of attack.
     */
    public double getAngleOfAttack(final SpacecraftState state) {
        return this.angleOfAttack.value(state);
    }

    /**
     * Getter for the side slip angle.
     *
     * @param state
     *        state
     * @return the side slip angle.
     */
    public double getSideSlipAngle(final SpacecraftState state) {
        return this.sideSlip.value(state);
    }

    /**
     * Getter for the roll velocity.
     *
     * @param state
     *        state
     * @return the roll velocity.
     */
    public double getRollVelocity(final SpacecraftState state) {
        return this.rollVelocity.value(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s: angleOfAttack=%s, sideSlip=%s, rollVelocity=%s", this.getClass().getSimpleName(),
            this.angleOfAttack, this.sideSlip, this.rollVelocity);
    }
}
