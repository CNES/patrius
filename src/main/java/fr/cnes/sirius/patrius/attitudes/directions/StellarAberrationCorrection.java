/**
 * HISTORY
 * VERSION:4.11:DM:DM-3303:22/05/2023:[PATRIUS] Modifications mineures dans UserCelestialBody 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans ITargetDirection
 * END-HISTORY
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements the stellar aberration correction for both reception and transmission cases.
 * <p>
 * <u>Nota:</u> The light-time aberration is out of the scope of this class.
 * </p>
 *
 * @author Alice Latourte
 *
 * @since 4.10
 */
public final class StellarAberrationCorrection {

    /**
     * Private constructor
     */
    private StellarAberrationCorrection() {

    }

    /**
     * Return the Target apparent direction from Observer position due to stellar aberration.<br>
     * The Observer receives the signal emitted by the Target.
     *
     * <p>
     * Relativistic effects are not taken into account here.
     * </p>
     *
     * <pre>
     *              target    target(corrected)
     *                T          T'
     *               ^          ^
     *              /         .
     *             /        .  dir(corrected)
     *            /__ Δθ  .
     *       dir /   \  .
     *          /     .
     *         /    .
     *        /   .
     *       /__.
     *      / . \  θ
     *     /.    |
     *     -----------&gt; _
     *   O              V
     *  observer     velocity
     * 
     *                  (V/c)sin(θ)
     *  sin(Δθ) = ------------------------- ≈ (V/c)sin(θ) + 1/2(V/c)²sin(2θ)
     *            sqrt(1 + 2(V/c) + (V/c)²)
     * 
     * (non-relativistic case)
     * 
     * Note:
     * In the solar system
     *      V/c       ≈ 1e-4 or 1e-2 deg
     *      1/2(V/c)² ≈ 1e-8 or 1e-6 deg
     * Only the first term is taken into account here
     *
     * </pre>
     *
     * @param observer
     *        provider of Observer PV coordinates
     * @param observerToTargetDir
     *        light-time corrected direction from Observer to Target
     * @param frame
     *        expression frame of the entered observerToTargetDir
     * @param date
     *        computation date of the entered observerToTargetDir (observer date)
     *
     * @return Apparent Observer-Target direction corrected for stellar aberration
     * @exception PatriusException
     *            if solar system ephemerides cannot be loaded
     */
    public static Vector3D applyTo(final PVCoordinatesProvider observer, final Vector3D observerToTargetDir,
                                   final Frame frame, final AbsoluteDate date)
        throws PatriusException {

        // ICRF frame
        final Frame icrf = FramesFactory.getICRF();

        // inertial velocity of observer in ICRF frame
        final Vector3D observerVelocityInIcrf = observer.getPVCoordinates(date, icrf).getVelocity();

        // observer velocity projected in frame
        final Vector3D observerVelocityProjInFrame = icrf.getTransformTo(frame, date).transformVector(
            observerVelocityInIcrf);

        return applyTo(observerToTargetDir, observerVelocityProjInFrame);
    }

    /**
     * Return the Target apparent direction from Observer position due to stellar aberration.<br>
     * The Observer receives the signal emitted by the Target.
     *
     * <p>
     * Relativistic effects are not taken into account here.
     * </p>
     *
     * <pre>
     *              target    target(corrected)
     *                T          T'
     *               ^          ^
     *              /         .
     *             /        .  dir(corrected)
     *            /__ Δθ  .
     *       dir /   \  .
     *          /     .
     *         /    .
     *        /   .
     *       /__.
     *      / . \  θ
     *     /.    |
     *     -----------&gt; _
     *   O              V
     *  observer     velocity
     * 
     *                  (V/c)sin(θ)
     *  sin(Δθ) = ------------------------- ≈ (V/c)sin(θ) + 1/2(V/c)²sin(2θ)
     *            sqrt(1 + 2(V/c) + (V/c)²)
     * 
     * (non-relativistic case)
     * 
     * Note:
     * In the solar system
     *      V/c       ≈ 1e-4 or 1e-2 deg
     *      1/2(V/c)² ≈ 1e-8 or 1e-6 deg
     * Only the first term is taken into account here
     *
     * </pre>
     *
     * @param observerToTargetDir
     *        light-time corrected direction from Observer to Target
     * @param velocity
     *        Velocity of the Observer wrt. solar system barycenter, projected in the frame used to express
     *        observerToTargetDir
     *
     * @return Apparent Observer-Target direction corrected for stellar aberration
     */
    private static Vector3D applyTo(final Vector3D observerToTargetDir, final Vector3D velocity) {

        final double theta = Vector3D.angle(observerToTargetDir, velocity);
        final double deltaTheta = MathLib.asin((MathLib.sin(theta) * velocity.getNorm()) / Constants.SPEED_OF_LIGHT);
        final Vector3D perp = Vector3D.crossProduct(observerToTargetDir, velocity);
        final Rotation rot = new Rotation(perp, deltaTheta);

        return rot.applyTo(observerToTargetDir);
    }

    /**
     * Return the direction in which the signal should be emitted to reach the Target due to stellar aberration.<br>
     * <b><u>Warning:</u> The Observer transmits</b> the signal to the target.
     *
     * <p>
     * Relativistic effects are not taken into account here.
     * </p>
     *
     * <pre>
     *
     *     target(corrected)  target
     *                   T'    T
     *                   ^    ^
     *                  .    /
     *                  __Δθ/
     *  dir(corrected) .  \/
     *                    /
     *                .  / dir
     *                  /
     *               . /
     *                /__
     *              ./   \  θ
     *              /     |
     *              -----------&gt; _
     *            O              V
     *        transmitter        velocity
     * 
     *                  (V/c)sin(θ)
     *  sin(Δθ) = ------------------------- ≈ (V/c)sin(θ) + 1/2(V/c)²sin(2θ)
     *            sqrt(1 + 2(V/c) + (V/c)²)
     * 
     * (non-relativistic case)
     * 
     * Note:
     * In the solar system
     *      V/c       ≈ 1e-4 or 1e-2 deg
     *      1/2(V/c)² ≈ 1e-8 or 1e-6 deg
     * Only the first term is taken into account here
     *
     * </pre>
     *
     * @param transmitter
     *        provider of Transmitter PV coordinates
     * @param transmitterToTargetDir
     *        light-time corrected direction from Transmitter to Target
     * @param frame
     *        expression frame of the entered transmitterToTargetDir
     * @param date
     *        computation date of the entered transmitterToTargetDir (transmitter date)
     *
     * @return Transmitter-Target transmission direction corrected for stellar aberration
     * @throws PatriusException
     *         if solar system ephemerides cannot be loaded
     */
    public static Vector3D applyInverseTo(final PVCoordinatesProvider transmitter,
                                          final Vector3D transmitterToTargetDir, final Frame frame,
                                          final AbsoluteDate date)
        throws PatriusException {

        // ICRF frame
        final Frame icrf = FramesFactory.getICRF();

        // inertial velocity of transmitter in ICRF frame
        final Vector3D transmitterVelocityInIcrf = transmitter.getPVCoordinates(date, icrf).getVelocity();

        // transmitter velocity projected in frame
        final Vector3D transmitterVelocityProjInFrame = icrf.getTransformTo(frame, date).transformVector(
            transmitterVelocityInIcrf);

        return applyInverseTo(transmitterToTargetDir, transmitterVelocityProjInFrame);
    }

    /**
     * Return the direction in which the signal should be emitted to reach the Target due to stellar aberration.<br>
     * <b><u>Warning:</u> The Observer transmits</b> the signal to the target.
     *
     * <p>
     * Relativistic effects are not taken into account here.
     * </p>
     *
     * <pre>
     *
     *     target(corrected)  target
     *                   T'    T
     *                   ^    ^
     *                  .    /
     *                  __Δθ/
     *  dir(corrected) .  \/
     *                    /
     *                .  / dir
     *                  /
     *               . /
     *                /__
     *              ./   \  θ
     *              /     |
     *              -----------&gt; _
     *            O              V
     *        transmitter        velocity
     * 
     *                  (V/c)sin(θ)
     *  sin(Δθ) = ------------------------- ≈ (V/c)sin(θ) + 1/2(V/c)²sin(2θ)
     *            sqrt(1 + 2(V/c) + (V/c)²)
     * 
     * (non-relativistic case)
     * 
     * Note:
     * In the solar system
     *      V/c       ≈ 1e-4 or 1e-2 deg
     *      1/2(V/c)² ≈ 1e-8 or 1e-6 deg
     * Only the first term is taken into account here
     *
     * </pre>
     *
     * @param transmitterToTargetDir
     *        light-time corrected direction from Transmitter to Target
     * @param transmitterVelocity
     *        Velocity of the Transmitter wrt. solar system barycenter, projected in the frame used to express
     *        transmitterToTargetDir
     *
     * @return Transmitter-Target transmission direction corrected for stellar aberration
     */
    private static Vector3D applyInverseTo(final Vector3D transmitterToTargetDir, final Vector3D transmitterVelocity) {

        final double theta = Vector3D.angle(transmitterToTargetDir, transmitterVelocity);
        final double deltaTheta = MathLib.asin((MathLib.sin(theta) * transmitterVelocity.getNorm())
                / Constants.SPEED_OF_LIGHT);
        final Vector3D perp = Vector3D.crossProduct(transmitterToTargetDir, transmitterVelocity);
        final Rotation rot = new Rotation(perp, -deltaTheta);

        return rot.applyTo(transmitterToTargetDir);
    }

}
