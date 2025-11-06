package fr.cnes.sirius.patrius.stela.forces.solaractivity.constant;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.forces.drag.AbstractStelaDragCoef;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaDragCoefType;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.AbstractStelaSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.stela.spaceobject.StelaSpaceObject;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Mean constant model of solar activity. This solar activity uses calculated constant F107
 * depending on the orbit and the ballistic coefficient (see User guide for more information).
 * This computation is performed using the method {@link #updateF107(StelaSpaceObject, SpacecraftState)}.
 * Resulting value is stored in {@link #losF107}.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaLOSConstantSolarActivity extends AbstractStelaSolarActivity {

    /** Serializable UID. */
    private static final long serialVersionUID = 7687384102040077559L;

    /** Logger for this class */
    private static final Logger LOGGER = Logger.getLogger(StelaLOSConstantSolarActivity.class.getName());

    /* ****************************** ATTRIBUTES ****************************** */

    /** Solar flux. */
    private double losF107;

    /** Geomagnetic activity coefficient. */
    private final double constantAP;

    /* ****************************** CONSTRUCTOR ****************************** */

    /**
     * Constructor of a MEAN_CONSTANT solar activity model.
     */
    public StelaLOSConstantSolarActivity() {
        super(StelaSolarActivityType.MEAN_CONSTANT);
        this.losF107 = Constants.STELA_LOS_F107;
        this.constantAP = Constants.STELA_LOS_AP;
    }

    /* ****************************** CLASS METHODS ****************************** */

    /** {@inheritDoc} */
    @Override
    public double getInstantFluxValue(final AbsoluteDate date) {
        return this.losF107;
    }

    /** {@inheritDoc} */
    @Override
    public double getAp(final AbsoluteDate date) {
        return this.constantAP;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getSolarActivity(final AbsoluteDate date) {
        return new double[] { this.losF107, this.losF107, this.constantAP, this.constantAP, this.constantAP,
            this.constantAP, this.constantAP, this.constantAP,
            this.constantAP };
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // Get the number format in order to properly write the results.
        final DecimalFormat rez = getStelaDoubleFormat();

        final String CR = System.lineSeparator();

        return "[ Solar Activity ]" + CR + " Solar Activity Type : " + StelaSolarActivityType.MEAN_CONSTANT + CR
                + " AP Constant Equivalent Solar Activity : " + rez.format(
                    this.constantAP)
                + CR + " F10.7 Constant Equivalent Solar Activity : " + rez.format(this.losF107) + CR;
    }

    /**
     * Updates the F107 coefficient for MEAN_CONSTANT solar activity.
     * Call the {@link StelaLOSConstantSolarActivity#computeLosF107(StelaSpaceObject, SpacecraftState)} method
     *
     * @param spaceObject
     *        the SpaceObject.
     * @param state
     *        the Bulletin.
     */
    public void updateF107(final StelaSpaceObject spaceObject, final SpacecraftState state) throws PatriusException {
        // Computation for the F10.7 update
        computeLosF107(spaceObject, state);

        final NumberFormat nf = getStelaDoubleFormat();
        final String f107str = nf.format(this.losF107);
        final String apstr = nf.format(this.constantAP);
        final String message = "STELA_LOS_SOLAR_ACTIVITY_UPDATED : f107Value = " + f107str +
                " constantAP = " + apstr;
        // Log the update (file + GUI)
        LOGGER.info(message);
    }

    /**
     * Updates the F107 coefficient for MEAN_CONSTANT solar activity.
     * 
     * @param spaceObject
     *        the SpaceObject.
     * @param state
     *        the Bulletin.
     */
    public void computeLosF107(final StelaSpaceObject spaceObject, final SpacecraftState state)
        throws PatriusException {

        // SpaceObject features
        final double dragArea = spaceObject.getDragArea();
        final double mass = spaceObject.getMass();

        // Drag coefficient
        final AbstractStelaDragCoef dragCoefObj = spaceObject.getDragCoef();
        final StelaDragCoefType stelaDragCoefType = dragCoefObj.getDragCoefType();
        final double dragCoef;
        if (stelaDragCoefType == StelaDragCoefType.CONSTANT) {
            dragCoef = dragCoefObj.getDragCoef(null);
        } else {
            dragCoef = Constants.STELA_DEFAULT_LOS_CX;
        }

        final double apogee = state.getOrbit().getParameters().getApsisRadiusParameters().getApoapsis();
        final double apogeeAltitudeKM = (apogee - Constants.CNES_STELA_AE) / 1000;

        // Computation of F107 coefficient
        double f107 = Constants.STELA_LOS_K0 + Constants.STELA_LOS_K1 * FastMath.log(dragCoef * dragArea / mass)
                + Constants.STELA_LOS_K2 * FastMath.log(apogeeAltitudeKM);
        if (Double.compare(dragCoef, 0.) == 0) {
            // Random value, as this won't affect final result
            f107 = this.losF107;
        }

        // Update F10.7
        this.losF107 = f107;
    }

    /** {@inheritDoc} */
    @Override
    public StelaLOSConstantSolarActivity copy() throws PatriusException {
        // Create new instance of StelaLOSConstantSolarActivity with default values
        return new StelaLOSConstantSolarActivity();
    }

}
