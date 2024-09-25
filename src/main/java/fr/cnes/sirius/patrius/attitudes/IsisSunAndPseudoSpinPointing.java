/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles SunIsis and PseudoSpin attitude provider.
 *
 * This law is composed of a base frame (SUN ISIS) defined as<br>
 * - Z_sun = direction Sun->Sat<br>
 * - X_sun = Z_sun^orbitalMomentum with sign so that X_sun(3) is negative (FLAG 1 if negated else 0)<br>
 *
 * followed by a rotation about the Z_sun axis of angle:<br>
 * bias = - BETA_SIGN * alpha – PHI + FLAG * PI + (1 – BETA_SIGN) * PI/2<br>
 * where alpha is the Sat->Sun angle with Z in the LVLH XZ plane
 *
 * Note that due to simplifications in the computation (eg fixed sun, keplerian motion, fixed orbital plane) the angular
 * velocity has an approximate error of O(E-7) rad/sec (real value is O(E-3) as driven by orbital period) and angular
 * acceleration is not computed (zero), likely O(E-9) rad/s2
 *
 */
public class IsisSunAndPseudoSpinPointing extends AbstractAttitudeLaw {

    /** serial ID */
    private static final long serialVersionUID = -7480050673887223290L;

    /** sun direction to use */
    private final IDirection sunDirection;
    /** phase adjustment */
    private final double pseudoSpinPhase;
    /** desired beta sign */
    private final double betaSign;

    /**
     * Constructor.
     *
     * @param dirSun
     *            sun direction to use
     * @param pseudoSpinPhase
     *            phase adjustment
     * @param positiveBetaSign
     *            desired beta sign (true positive, false negative)
     */
    public IsisSunAndPseudoSpinPointing(final IDirection dirSun, final double pseudoSpinPhase,
            final boolean positiveBetaSign) {
        super();
        this.sunDirection = dirSun;
        this.pseudoSpinPhase = pseudoSpinPhase;
        if (positiveBetaSign) {
            this.betaSign = 1.;
        } else {
            this.betaSign = -1.;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
        throws PatriusException {

        // ISIS inertial frame : GCRF
        final Frame gcrf = FramesFactory.getGCRF();

        // Satellite position/velocity
        final PVCoordinates pvSat = pvProv.getPVCoordinates(date, gcrf);

        // Normal to the orbit (-W)
        final Vector3D nOrb = pvSat.getMomentum().negate().normalize();

        // (Earth/Sat)->Sun unit vector (pvProv should not be needed for BasicBoard)
        final Vector3D uSun = this.sunDirection.getVector(pvProv, date, gcrf).normalize();
        // Sun->Sat unit vector
        final Vector3D zSun = uSun.negate();

        // Compute Sun axis in inertial frame
        Vector3D xSun = nOrb.crossProduct(zSun);

        // ySun is computed by zSun ^ xSun : throw orekit exception if xSun is null,
        // meaning the Sun is orthogonal to the orbit plane so ySun could not be computed
        if (xSun.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw new PatriusException(PatriusMessages.ISIS_SUN_FRAME_UNDEFINED);
        }

        // Test a condition on xSun
        double flag = 0.;
        if (xSun.getZ() > 0.) {
            flag = 1.;
            xSun = xSun.negate();
        }
        xSun = xSun.normalize();
        // not needed ySun = zSun.crossProduct(xSun)

        // Compute the wanted attitude : align satellite axis with (xSun, ySun, zSun)
        // assuming sun direction is "fixed" and satellite position irrelevant and orbit normal fixed,
        // angular velocity is set to zero, but real is O(E-7)
        final TimeStampedAngularCoordinates sunIsis = new TimeStampedAngularCoordinates(date,
                new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_I, zSun, xSun), Vector3D.ZERO, Vector3D.ZERO);

        // solar position angle ----------------
        final Transform lof = LOFType.LVLH.transformFromInertial(date, pvSat);
        final Vector3D sunLof = lof.transformVector(uSun);
        final double alpha = MathLib.atan2(sunLof.getX(), sunLof.getZ());
        // assuming sun direction is "fixed" and satellite position irrelevant,
        // the only thing contributing to this is the actual LOF angular velocity on Y
        // Note though that this is likely just a keplerian rate estimation
        final double alphaRate = -lof.getRotationRate().getY();
        // lof.getRotationAcceleration() is zero so no Rate2

        // Compute bias rotation around ZSun ----------------
        final double bias =
            FastMath.PI * (flag + (1. - this.betaSign) / 2.) - this.betaSign * alpha + this.pseudoSpinPhase;
        final double biasRate = -this.betaSign * alphaRate;
        final Rotation biasRot = new Rotation(Vector3D.PLUS_K, bias);
        final Vector3D biasRotRate = Vector3D.PLUS_K.scalarMultiply(biasRate);
        final TimeStampedAngularCoordinates rotBiasZ =
            new TimeStampedAngularCoordinates(date, biasRot, biasRotRate, Vector3D.ZERO);

        // compose rotations GCRF->SunIsis->BiasZ ------------------
        final TimeStampedAngularCoordinates ac = sunIsis.addOffset(rotBiasZ, getSpinDerivativesComputation());

        // Return the attitude in the input frame
        return new Attitude(gcrf, ac).withReferenceFrame(frame, getSpinDerivativesComputation());
    }
}
