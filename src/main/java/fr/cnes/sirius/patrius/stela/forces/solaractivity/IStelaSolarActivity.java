package fr.cnes.sirius.patrius.stela.forces.solaractivity;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;

/**
 * Interface for Stela solar activity models.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public interface IStelaSolarActivity extends Serializable {

    /**
     * Get Solar activity flux at a specified date.
     * 
     * @param date a date
     * 
     * @return solar activity daily flux
     * 
     * @throws PatriusException thrown if a problem occurs while reading the solar activity file
     */
    double getInstantFluxValue(AbsoluteDate date) throws PatriusException;

    /**
     * Get Geomagnetic activity at a specified date.
     * 
     * @param date a date
     * 
     * @return geomagnetic activity
     * 
     * @throws PatriusException thrown if a problem occurs while reading the solar activity file
     */
    double getAp(AbsoluteDate date) throws PatriusException;

    /**
     * Get Solar activity coefficients at a specified date, in the following order: daily flux, mean flux, Ap1, Ap2,
     * Ap3, Ap4, Ap5, Ap6, Ap7.
     *
     * @param date a date
     * 
     * @return solar activity coefficients (solar flux and geomagnetic activity)
     * 
     * @throws PatriusException thrown if a problem occurs while reading the solar activity file
     */
    double[] getSolarActivity(AbsoluteDate date) throws PatriusException;

    /**
     * Set geomagnetic activity coefficient to the input value. It should be used only if instantiated solar activity is
     * a constant one.
     * 
     * @param inputAP geomagnetic activity coefficient
     * 
     * @throws PatriusException thrown if solar activity is variable
     */
    default void setConstantAP(final double inputAP) throws PatriusException {
        throw new PatriusException(PatriusMessages.STELA_CANNOT_SET_AP);
    }

    /**
     * Set solar flux value to the input parameter's value. It should be used only if instantiated solar activity is a
     * constant one.
     * 
     * @param inputF107 constant solar flux value
     * 
     * @throws PatriusException Exceptions thrown if solar activity is variable.
     */
    default void setConstantF107(final double inputF107) throws PatriusException {
        throw new PatriusException(PatriusMessages.STELA_CANNOT_SET_F107);
    }

    /**
     * Get solar activity type. It can be either constant or variable.
     * 
     * @return solar activity type
     */
    StelaSolarActivityType getSolActType();

    /**
     * Get information of Solar Activity.
     * 
     * @return a string with all solar activity
     */
    String toString();

    /**
     * Copy the solar activity model.
     * 
     * @return copied solar activity model
     */
    IStelaSolarActivity copy() throws PatriusException, IOException, ParseException;
}
