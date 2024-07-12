/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
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
package fr.cnes.sirius.patrius.forces.maneuvers.orbman;

import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class defining an impulsive maneuver with a semi-major axis increment as input.
 * 
 * @author JFG
 * @since 4.4
 */
public class ImpulseDaManeuver extends ImpulseManeuver implements ImpulseParKepManeuver {

    /** Serializable UID. */
    private static final long serialVersionUID = -3287083592430088075L;

    /** Forced LOF */
    private static final LOFType LOFTYPE = LOFType.TNW;

    /** Increment of semi major axis (m) */
    private final double da;

    /**
     * Build a new instance.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param da semi major axis increment (m)
     * @param isp engine specific impulse (s)
     * @param massModel mass model
     * @param part part of the mass model that provides the propellant
     * @throws PatriusException thrown if mass from mass provider is negative
     */
    public ImpulseDaManeuver(final EventDetector inTrigger, final double da,
                             final double isp, final MassProvider massModel, final String part)
        throws PatriusException {

        super(inTrigger, Vector3D.ZERO, isp, massModel, part, LOFTYPE);
        this.da = da;
    }

    /**
     * Build a new instance using propulsive and engine property.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param da semi major axis increment (m)
     * @param engine engine property (specific impulse)
     * @param massModel mass model
     * @param tank tank property gathering mass and part name information
     */
    public ImpulseDaManeuver(final EventDetector inTrigger, final double da,
            final PropulsiveProperty engine, final MassProvider massModel,
            final TankProperty tank) {
        super(inTrigger, Vector3D.ZERO, engine, massModel, tank, LOFTYPE);
        this.da = da;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        // DeltaV computation
        computeDV(oldState);

        return super.resetState(oldState);
    }

    /** {@inheritDoc} */
    @Override
    public void computeDV(final SpacecraftState state) {

        // Getting Keplerian parameters
        final KeplerianParameters param = state.getOrbit().getParameters().getKeplerianParameters();

        // Intermediate variables
        final double sma = param.getA();
        final double exc = param.getE();
        final double ane = param.getAnomaly(PositionAngle.ECCENTRIC);
        final double mu = param.getMu();

        final double ray = sma * (1. - exc * MathLib.cos(ane));
        final double twoOnR = 2. / ray;

        // DV computation

        final double vBefore = MathLib.sqrt(mu * (twoOnR - (1. / sma)));
        final double vAfter = MathLib.sqrt(mu * (twoOnR - (1. / (sma + da))));

        deltaVSat = new Vector3D(vAfter - vBefore, 0., 0.);
    }
    
    /**
     * Getter for semi-major axis increment.
     * @return semi-major axis increment
     */
    public double getDa() {
        return da;
    }
}
