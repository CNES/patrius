/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This interface is used to compute meteorological conditions at a given date, allowing to adapt the computation of
 * atmospheric effects to the moment when a signal propagates through the atmosphere.
 * 
 * @author William POLYCARPE (TSN)
 */
@FunctionalInterface
public interface MeteorologicalConditionsProvider extends Serializable {

    /**
     * Returns the meteorological conditions at a given date.
     * 
     * @param date
     *        date of meteo conditions
     * @return MeteorologicalConditions (temperature, pressure, humidity) at date
     */
    MeteorologicalConditions getMeteorologicalConditions(final AbsoluteDate date);
}
